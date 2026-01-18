// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import chisel3.util._
import riscv.Parameters

class Timer extends Module {
  val io = IO(new Bundle {
    val bundle           = new RAMBundle
    val signal_interrupt = Output(Bool())

    val debug_limit   = Output(UInt(Parameters.DataWidth))
    val debug_enabled = Output(Bool())
  })

  val count = RegInit(0.U(32.W))
  val limit = RegInit(100000000.U(32.W))
  io.debug_limit := limit
  val enabled = RegInit(true.B)
  io.debug_enabled := enabled

  io.bundle.read_data := MuxLookup(
    io.bundle.address,
    0.U
  )(
    IndexedSeq(
      0x4.U -> limit,
      0x8.U -> enabled.asUInt,
    )
  )
  when(io.bundle.write_enable) {
    when(io.bundle.address === 0x4.U) {
      limit := io.bundle.write_data
      count := 0.U
    }.elsewhen(io.bundle.address === 0x8.U) {
      enabled := io.bundle.write_data =/= 0.U
    }
  }

  io.signal_interrupt := enabled && (count >= (limit - 10.U))

  when(count >= limit) {
    count := 0.U
  }.otherwise {
    count := count + 1.U
  }
}
