// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

class PipelineRegister(width: Int = Parameters.DataBits, defaultValue: UInt = 0.U) extends Module {
  val io = IO(new Bundle {
    val stall = Input(Bool())
    val flush = Input(Bool())
    val in    = Input(UInt(width.W))
    val out   = Output(UInt(width.W))
  })
  val reg = RegInit(UInt(width.W), defaultValue)

  when(io.flush) {
  // Flush : Clear register contents (Highest Priority)
    reg := defaultValue
  }
  .elsewhen(io.stall) {
  // Stall : Freeze register contents
    reg := reg
  }
  .otherwise {
  // Normal action: Update register contents with new input
    reg := io.in
  }
  // Connect register to output.
  // Being driven by a register breaks the combinational timing path.
  io.out := reg
}
