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
  val myreg = RegInit(UInt(width.W), defaultValue)
  val out   = RegInit(UInt(width.W), defaultValue)
  when(io.flush) {
    out   := defaultValue
    myreg := defaultValue
  }
    .elsewhen(io.stall) {
      out := myreg
    }
    .otherwise {
      myreg := io.in
      out   := io.in
    }
  io.out := out // 在最后一步才给io.out赋值，是为了防止出现组合逻辑环路导致sbt "testOnly riscv.ThreeStageCPUTest"无法通过（sbt "testOnly riscv.PipelineRegisterTest"可以通过）
  // 踩了很多次坑猜测出来的，可能是因为如果在前面的条件判断中就给io.out赋值，硬件就不会理会后面代码对io.out的再次赋值
}
