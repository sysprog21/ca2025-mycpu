// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.peripheral

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
  io.data := mem.read(io.address)

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
    val exeTxtPath = Paths.get("verilog", f"${instructionFilename}.txt")
    // Create parent directory if it doesn't exist
    Files.createDirectories(exeTxtPath.getParent())
    val writer = new FileWriter(exeTxtPath.toString)
    for (i <- instructions.indices) {
      writer.write(f"${instructions(i)}%08x\n")
    }
    writer.close()
    (exeTxtPath, instructions.length)
  }
}
