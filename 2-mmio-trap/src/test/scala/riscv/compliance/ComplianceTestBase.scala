// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.compliance

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import chisel3._
import chiseltest._
import firrtl.annotations.Annotation
import org.scalatest.flatspec.AnyFlatSpec
import riscv.singlecycle.TestTopModule

// RISCOF Compliance Test Framework for MyCPU
//
// Memory Layout for Compliance Tests:
// =====================================
// The MyCPU architecture uses the following memory organization for RISCOF tests:
//
// Address Range    | Purpose                           | Notes
// -----------------|-----------------------------------|----------------------------------
// 0x0000 - 0x0FFF  | Reserved/Unused                   | Not accessed during tests
// 0x1000 - 0xNNNN  | Test program instructions         | Loaded from test.asmbin via ROMLoader
// 0xMMMM - 0xXXXX  | Test signature region             | Defined by begin_signature/end_signature symbols
//
// The signature region addresses are extracted from the compiled ELF file's symbol table:
// - begin_signature: Start address of test output memory region
// - end_signature:   End address (exclusive) of test output memory region
//
// Address Space Considerations:
// - All addresses are absolute in the 32-bit address space
// - Debug interface (mem_debug_read_address/mem_debug_read_data) uses these absolute addresses
// - No offset translation needed - ELF symbols provide physical addresses
//
// Test Execution Flow:
// 1. ROMLoader copies test.asmbin to memory starting at 0x1000 (Parameters.EntryAddress)
// 2. CPU executes instructions, writing results to signature region
// 3. After sufficient cycles, debug interface reads signature region
// 4. Signature data written to file for RISCOF comparison with reference model

object ElfSignatureExtractor {

  /**
   * Extract signature memory region boundaries from compiled ELF file.
   *
   * RISC-V Architecture Tests define two special symbols in their linker scripts:
   * - begin_signature: Marks the start of the test output region
   * - end_signature:   Marks the end (exclusive) of the test output region
   *
   * These symbols are embedded in the ELF symbol table and can be extracted using
   * riscv-*-readelf tool. The addresses are absolute physical addresses in the
   * 32-bit address space.
   *
   * @param elfFile Path to the compiled ELF file containing test program
   * @return Tuple of (beginAddress, endAddress) for signature region, or (0, 0) on failure
   */
  def extractSignatureRange(elfFile: String): (BigInt, BigInt) = {
    // Try different RISC-V toolchain locations and prefixes
    // Common toolchain installations use different naming conventions
    val toolchainPaths = Seq(
      sys.env.getOrElse("RISCV", ""),
      sys.env.get("HOME").map(_ + "/riscv/toolchain").getOrElse(""),
      "/opt/riscv"
    ).filter(_.nonEmpty)

    val prefixes = Seq("riscv32-unknown-elf", "riscv-none-elf", "riscv64-unknown-elf")

    // Find first working readelf command (lazy evaluation to avoid execution during test discovery)
    // Suppress stdout/stderr during version check to keep test output clean
    lazy val readelfCmd = toolchainPaths
      .flatMap { path =>
        prefixes.map(p => s"${path}/bin/${p}-readelf")
      }
      .find { cmd =>
        try {
          import scala.sys.process.ProcessLogger
          val logger = ProcessLogger(_ => (), _ => ()) // Discard stdout and stderr
          s"${cmd} --version".!(logger) == 0
        } catch {
          case _: Exception => false
        }
      }
      .orElse {
        // Final fallback: search in PATH
        prefixes.map(p => s"${p}-readelf").find { cmd =>
          try {
            import scala.sys.process.ProcessLogger
            val logger = ProcessLogger(_ => (), _ => ()) // Discard stdout and stderr
            s"${cmd} --version".!(logger) == 0
          } catch {
            case _: Exception => false
          }
        }
      }
      .getOrElse("riscv-none-elf-readelf")

    // Get symbol table from ELF file using readelf -s (symbols)
    // Output format: Num Value Size Type Bind Vis Ndx Name
    // Example: 123: 80001234 0 NOTYPE GLOBAL DEFAULT 1 begin_signature
    val symbolOutput = s"${readelfCmd} -s ${elfFile}".!!

    var beginSig: Option[BigInt] = None
    var endSig: Option[BigInt]   = None

    // Parse symbol table line by line looking for signature markers
    symbolOutput.split("\n").foreach { line =>
      if (line.contains("begin_signature")) {
        val parts = line.trim.split("\\s+")
        if (parts.length >= 2) {
          try {
            beginSig = Some(BigInt(parts(1), 16))
          } catch {
            case _: Exception =>
          }
        }
      } else if (line.contains("end_signature")) {
        val parts = line.trim.split("\\s+")
        if (parts.length >= 2) {
          try {
            endSig = Some(BigInt(parts(1), 16))
          } catch {
            case _: Exception =>
          }
        }
      }
    }

    (beginSig.getOrElse(BigInt(0)), endSig.getOrElse(BigInt(0)))
  }
}

abstract class ComplianceTestBase extends AnyFlatSpec with ChiselScalatestTester {

  /**
   * Run a single RISCOF compliance test on the MyCPU implementation.
   *
   * Test execution sequence:
   * 1. Extract signature region boundaries from ELF symbol table
   * 2. Instantiate TestTopModule with test binary (implementation=2 for 3-stage pipeline)
   * 3. Execute test for sufficient cycles to complete (100K cycles = 100 * 1000 step calls)
   * 4. Read signature memory region via debug interface
   * 5. Write signature data to file for RISCOF validation
   *
   * @param asmbinFile Path to test.asmbin (raw binary loaded by ROMLoader)
   * @param elfFile    Path to test.elf (ELF file containing symbol table)
   * @param sigFile    Output path for signature file (hex values, one per line)
   * @param annos      ChiselTest annotations for simulation control
   */
  def runComplianceTest(
      asmbinFile: String,
      elfFile: String,
      sigFile: String,
      annos: Seq[Annotation]
  ): Unit = {

    // Extract signature region from ELF symbol table
    // Returns (begin_signature_address, end_signature_address) as absolute addresses
    val (beginSig, endSig) = ElfSignatureExtractor.extractSignatureRange(elfFile)

    // Instantiate 2-mmio-trap CPU
    // TestTopModule parameters: (asmbinFile: String)
    test(new TestTopModule(asmbinFile)).withAnnotations(annos) { c =>
      // Disable clock timeout - some tests require many cycles
      // This allows tests to run as long as needed without ChiselTest timeout
      c.clock.setTimeout(0)

      // Execute test program for sufficient cycles
      // 100K total cycles = 100 iterations * 1000 cycles each
      // Conservative estimate ensures even slow tests complete
      // Note: External reference uses 50K cycles - could optimize if needed
      for (_ <- 1 to 100) {
        c.clock.step(1000)
      }

      // Read signature memory region via debug interface and write to file
      // Signature format: One 32-bit hex value per line (8 hex digits)
      val writer = new PrintWriter(new File(sigFile))
      try {
        if (beginSig != 0 && endSig != 0 && endSig > beginSig) {
          // Primary path: Use extracted signature range from ELF symbols
          // This is the normal case for properly compiled compliance tests
          val startAddr = beginSig.toInt
          val endAddr   = endSig.toInt

          // Read each 32-bit word in signature region (step by 4 bytes)
          // Debug interface: poke address, step clock, peek data
          for (addr <- startAddr until endAddr by 4) {
            c.io.mem_debug_read_address.poke(addr.asUInt)
            c.clock.step() // One cycle for memory read to stabilize
            val data = c.io.mem_debug_read_data.peekInt()
            writer.println(f"${data.toInt}%08x") // Format as 8-digit lowercase hex
          }
        } else {
          // Fallback path: Use default region if symbol extraction failed
          // This should rarely occur - indicates ELF processing issue
          // Default region: 0x1000-0x2000 (standard test program area)
          for (addr <- 0x1000 to 0x2000 by 4) {
            c.io.mem_debug_read_address.poke(addr.asUInt)
            c.clock.step()
            val data = c.io.mem_debug_read_data.peekInt()
            writer.println(f"${data.toInt}%08x")
          }
        }
      } finally {
        writer.close()
      }

      println(s"✅ Test completed - signature: ${sigFile}")
    }
  }
}
