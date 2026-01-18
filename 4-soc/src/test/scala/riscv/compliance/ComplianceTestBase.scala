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
import riscv.TestTopModule

// RISCOF Compliance Test Framework for MyCPU 4-soc
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

object ElfSignatureExtractor {

  /**
   * Extract signature memory region boundaries from compiled ELF file.
   *
   * RISC-V Architecture Tests define two special symbols in their linker scripts:
   * - begin_signature: Marks the start of the test output region
   * - end_signature:   Marks the end (exclusive) of the test output region
   *
   * @param elfFile Path to the compiled ELF file containing test program
   * @return Tuple of (beginAddress, endAddress) for signature region, or (0, 0) on failure
   */
  def extractSignatureRange(elfFile: String): (BigInt, BigInt) = {
    // Try different RISC-V toolchain locations and prefixes
    val toolchainPaths = Seq(
      sys.env.getOrElse("RISCV", ""),
      sys.env.get("HOME").map(_ + "/riscv/toolchain").getOrElse(""),
      "/opt/riscv"
    ).filter(_.nonEmpty)

    val prefixes = Seq("riscv32-unknown-elf", "riscv-none-elf", "riscv64-unknown-elf")

    // Find first working readelf command
    lazy val readelfCmd = toolchainPaths
      .flatMap { path =>
        prefixes.map(p => s"${path}/bin/${p}-readelf")
      }
      .find { cmd =>
        try {
          import scala.sys.process.ProcessLogger
          val logger = ProcessLogger(_ => (), _ => ())
          s"${cmd} --version".!(logger) == 0
        } catch {
          case _: Exception => false
        }
      }
      .orElse {
        // Fallback: search in PATH
        prefixes.map(p => s"${p}-readelf").find { cmd =>
          try {
            import scala.sys.process.ProcessLogger
            val logger = ProcessLogger(_ => (), _ => ())
            s"${cmd} --version".!(logger) == 0
          } catch {
            case _: Exception => false
          }
        }
      }
      .getOrElse("riscv-none-elf-readelf")

    // Get symbol table from ELF file
    val symbolOutput = s"${readelfCmd} -s ${elfFile}".!!

    var beginSig: Option[BigInt] = None
    var endSig: Option[BigInt]   = None

    // Parse symbol table looking for signature markers
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
   * Run a single RISCOF compliance test on the MyCPU 4-soc implementation.
   *
   * Test execution sequence:
   * 1. Extract signature region boundaries from ELF symbol table
   * 2. Instantiate TestTopModule with test binary
   * 3. Execute test for sufficient cycles (100K cycles with 4:1 clock ratio = 400K master cycles)
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
    val (beginSig, endSig) = ElfSignatureExtractor.extractSignatureRange(elfFile)

    // Construct absolute path to test.asmbin from elfFile's directory
    // The elfFile is like ".../dut/dut.elf" and test.asmbin is in the same directory
    val elfPath            = java.nio.file.Paths.get(elfFile)
    val testDir            = elfPath.getParent
    val absoluteAsmbinPath = testDir.resolve(asmbinFile).toAbsolutePath.toString

    // Instantiate 4-soc CPU (pipelined with AXI4-Lite)
    test(new TestTopModule(absoluteAsmbinPath)).withAnnotations(annos) { c =>
      // Disable clock timeout - some tests require many cycles
      c.clock.setTimeout(0)

      // Execute test program for sufficient cycles
      // 4-soc uses 4:1 clock divider, so 50K iterations * 4 = 200K CPU cycles
      for (_ <- 1 to 50) {
        c.clock.step(1000)
      }

      // Read signature memory region via debug interface and write to file
      val writer = new PrintWriter(new File(sigFile))
      try {
        if (beginSig != 0 && endSig != 0 && endSig > beginSig) {
          // Use extracted signature range from ELF symbols
          val startAddr = beginSig.toInt
          val endAddr   = endSig.toInt

          // Read each 32-bit word in signature region
          for (addr <- startAddr until endAddr by 4) {
            c.io.mem_debug_read_address.poke(addr.asUInt)
            c.clock.step()
            val data = c.io.mem_debug_read_data.peekInt()
            writer.println(f"${data.toInt}%08x")
          }
        } else {
          // Fallback: Use default region if symbol extraction failed
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
    }
  }
}
