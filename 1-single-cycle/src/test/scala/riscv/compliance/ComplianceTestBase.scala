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

// RISCOF Compliance Test Framework for RV32I
//
// Memory Layout and Execution Flow:
// ┌────────────────────────────────────────────┐
// │ Address Range    │ Purpose                 │
// ├────────────────────────────────────────────┤
// │ 0x00000000-0x0FFF│ Instruction ROM         │
// │ 0x00001000-0x1FFF│ Data Memory (signature) │
// │ 0x00002000-0xFFFF│ Stack and heap          │
// └────────────────────────────────────────────┘
//
// Test Execution Flow:
// 1. Extract begin_signature/end_signature symbols from ELF file using readelf
// 2. Load compiled .asmbin test program into instruction ROM
// 3. Run simulation for 100K cycles (configurable timeout)
// 4. Read signature region via debug interface (mem_debug_read_address/data)
// 5. Write signature to file for RISCOF comparison
//
// Debug Interface Protocol:
// - mem_debug_read_address: Address to read from memory (input)
// - mem_debug_read_data: Data read from memory (output, available next cycle)
// - Single-cycle latency: poke address → step clock → peek data
//
object ElfSignatureExtractor {

  /**
   * Extracts begin_signature and end_signature addresses from RISC-V ELF file
   *
   * Uses RISC-V toolchain readelf to parse symbol table and locate the memory
   * region where RISCOF test will write results. The signature region is marked
   * by begin_signature and end_signature symbols in the compiled test.
   *
   * Toolchain Discovery:
   * 1. $RISCV environment variable
   * 2. $HOME/riscv/toolchain
   * 3. /opt/riscv (system default)
   *
   * @param elfFile Path to compiled RISC-V ELF test binary
   * @return Tuple of (begin_signature address, end_signature address) in bytes
   */
  def extractSignatureRange(elfFile: String): (BigInt, BigInt) = {
    // Try different RISC-V toolchain locations
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
      .getOrElse(
        throw new Exception("No RISC-V toolchain found. Set $RISCV or install to $HOME/riscv/toolchain")
      )

    // Get symbol table using readelf -s (symbol table dump)
    //
    // Example readelf output format:
    // Num:    Value  Size Type    Bind   Vis      Ndx Name
    //  42: 00001000     0 NOTYPE  GLOBAL DEFAULT    2 begin_signature
    //  43: 00002000     0 NOTYPE  GLOBAL DEFAULT    2 end_signature
    //
    // We parse column 1 (Value) which contains the address in hexadecimal
    val symbolOutput = s"${readelfCmd} -s ${elfFile}".!!

    var beginSig: Option[BigInt] = None
    var endSig: Option[BigInt]   = None

    // Parse symbol table line by line
    symbolOutput.split("\n").foreach { line =>
      if (line.contains("begin_signature")) {
        val parts = line.trim.split("\\s+")
        if (parts.length >= 2) {
          try {
            // parts(1) is the Value column (address in hex)
            beginSig = Some(BigInt(parts(1), 16))
          } catch {
            case _: Exception => // Ignore parsing errors
          }
        }
      } else if (line.contains("end_signature")) {
        val parts = line.trim.split("\\s+")
        if (parts.length >= 2) {
          try {
            endSig = Some(BigInt(parts(1), 16))
          } catch {
            case _: Exception => // Ignore parsing errors
          }
        }
      }
    }

    (beginSig.getOrElse(BigInt(0)), endSig.getOrElse(BigInt(0)))
  }
}

abstract class ComplianceTestBase extends AnyFlatSpec with ChiselScalatestTester {

  /**
   * Runs a single RISCOF compliance test
   *
   * Execution Steps:
   * 1. Extract signature address range from ELF file (begin_signature, end_signature symbols)
   * 2. Instantiate TestTopModule with preloaded instruction ROM from .asmbin file
   * 3. Run simulation for 100K cycles with infinite timeout (clock.setTimeout(0))
   * 4. Read signature region from memory via debug interface:
   *    - Poke debug_read_address with memory address
   *    - Step clock once (single-cycle read latency)
   *    - Peek debug_read_data to get word value
   * 5. Write signature to file in hexadecimal format (8 hex digits per line)
   * 6. RISCOF framework compares signature against golden reference
   *
   * Memory Access Pattern:
   * - Uses 4-byte aligned addresses (increment by 4)
   * - Reads words in little-endian format
   * - Signature typically located at 0x1000-0x2000 range
   *
   * Fallback Behavior:
   * - If ELF symbol extraction fails, defaults to reading 0x1000-0x2000
   * - Ensures tests can run even with malformed ELF files
   *
   * @param asmbinFile Path to compiled binary instruction file (.asmbin)
   * @param elfFile Path to ELF file (for extracting signature region symbols)
   * @param sigFile Output signature file path for RISCOF comparison
   * @param annos Firrtl annotations for simulation configuration
   */
  def runComplianceTest(
      asmbinFile: String,
      elfFile: String,
      sigFile: String,
      annos: firrtl.AnnotationSeq
  ): Unit = {

    // Extract signature region from ELF
    val (beginSig, endSig) = ElfSignatureExtractor.extractSignatureRange(elfFile)

    test(new TestTopModule(asmbinFile)).withAnnotations(annos) { c =>
      // Disable timeout to allow long-running tests (RISCOF tests can take many cycles)
      c.clock.setTimeout(0)

      // Run simulation for 100K cycles (100 steps × 1000 cycles each)
      // This is sufficient for most RV32I compliance tests to complete
      for (_ <- 1 to 100) {
        c.clock.step(1000)
      }

      // Read signature region from memory and write to file for RISCOF comparison
      val writer = new PrintWriter(new File(sigFile))
      try {
        if (beginSig != 0 && endSig != 0 && endSig > beginSig) {
          // Use signature range extracted from ELF symbols
          val startAddr = beginSig.toInt
          val endAddr   = endSig.toInt

          // Read memory word-by-word (4-byte aligned)
          for (addr <- startAddr until endAddr by 4) {
            c.io.mem_debug_read_address.poke(addr.asUInt) // Set address to read
            c.clock.step()                                // Wait 1 cycle for data
            val data = c.io.mem_debug_read_data.peekInt() // Read data value
            writer.println(f"${data.toInt}%08x") // Write as 8-digit hex
          }
        } else {
          // Fallback: ELF symbol extraction failed, use default signature region
          // Standard RISCOF test signature region: 0x1000-0x2000 (4KB)
          for (addr <- 0x1000 to 0x2000 by 4) {
            c.io.mem_debug_read_address.poke(addr.asUInt)
            c.clock.step()
            val data = c.io.mem_debug_read_data.peekInt()
            writer.println(f"${data.toInt}%08x")
          }
        }
      } finally {
        writer.close() // Ensure file is closed even if error occurs
      }

      println(s"✅ Test completed - signature: ${sigFile}")
    }
  }
}
