// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.ByteBuffer
import java.nio.ByteOrder

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import riscv.Parameters

class InstructionROM(instructionFilename: String) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(Parameters.AddrWidth))
    val data    = Output(UInt(Parameters.InstructionWidth))
  })

  val (instructionsInitFile, capacity) = readAsmBinary(instructionFilename)
  val mem                              = Mem(capacity, UInt(Parameters.InstructionWidth))
  loadMemoryFromFileInline(mem, instructionsInitFile.toString.replaceAll("\\\\", "/"))
  // Convert byte address to word index: subtract entry address offset, then divide by 4
  // PC starts at 0x1000, but ROM instructions start at index 0
  // Use combinational read mem(addr) instead of synchronous mem.read(addr) for immediate instruction fetch
  val wordAddress = (io.address - Parameters.EntryAddress) >> 2
  io.data := mem(wordAddress)

  def readAsmBinary(filename: String) = {
    val inputStream = if (Files.exists(Paths.get(filename))) {
      Files.newInputStream(Paths.get(filename))
    } else {
      getClass.getClassLoader.getResourceAsStream(filename)
    }
    var instructions = new Array[BigInt](0)
    val arr          = new Array[Byte](4)
    while (inputStream.read(arr) == 4) {
      val instBuf = ByteBuffer.wrap(arr)
      instBuf.order(ByteOrder.LITTLE_ENDIAN)
      val inst = BigInt(instBuf.getInt() & 0xffffffffL)
      instructions = instructions :+ inst
    }
    instructions = instructions :+ BigInt(0x00000013L)
    instructions = instructions :+ BigInt(0x00000013L)
    instructions = instructions :+ BigInt(0x00000013L)
    val currentDir = System.getProperty("user.dir")
    // Extract just the filename from instructionFilename (handles absolute paths)
    val baseName   = Paths.get(instructionFilename).getFileName.toString
    val exeTxtPath = Paths.get(currentDir, "verilog", f"${baseName}.txt")
    // Create verilog directory if it doesn't exist
    Files.createDirectories(exeTxtPath.getParent)
    val writer = new FileWriter(exeTxtPath.toString)
    for (i <- instructions.indices) {
      // Use @address\ndata format for loadMemoryFromFileInline compatibility
      writer.write(f"@$i%x\n${instructions(i)}%08x\n")
    }
    writer.close()
    (exeTxtPath, instructions.length)
  }
}
