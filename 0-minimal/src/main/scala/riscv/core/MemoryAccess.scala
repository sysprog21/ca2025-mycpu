// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.peripheral.RAMBundle
import riscv.Parameters

// Minimal MemoryAccess - only supports word-aligned LW/SW
class MemoryAccess extends Module {
  val io = IO(new Bundle() {
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val memory_read_enable  = Input(Bool())
    val memory_write_enable = Input(Bool())

    val wb_memory_read_data = Output(UInt(Parameters.DataWidth))

    val memory_bundle = Flipped(new RAMBundle)
  })

  io.memory_bundle.write_enable := false.B
  io.memory_bundle.write_data   := 0.U
  io.memory_bundle.address      := io.alu_result
  io.memory_bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  io.wb_memory_read_data        := 0.U

  when(io.memory_read_enable) {
    // Simple LW - word-aligned only
    io.wb_memory_read_data := io.memory_bundle.read_data
  }.elsewhen(io.memory_write_enable) {
    // Simple SW - word-aligned only
    io.memory_bundle.write_enable := true.B
    io.memory_bundle.write_data   := io.reg2_data
    io.memory_bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(true.B))
  }
}
