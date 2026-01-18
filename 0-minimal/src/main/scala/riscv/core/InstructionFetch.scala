// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

object ProgramCounter {
  val EntryAddress = Parameters.EntryAddress
}

// Minimal Instruction Fetch: maintains PC and fetches instructions
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    val jump_flag_id          = Input(Bool())
    val jump_address_id       = Input(UInt(Parameters.AddrWidth))
    val instruction_read_data = Input(UInt(Parameters.DataWidth))
    val instruction_valid     = Input(Bool())

    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val instruction         = Output(UInt(Parameters.InstructionWidth))
  })

  val pc = RegInit(ProgramCounter.EntryAddress)

  when(io.instruction_valid) {
    io.instruction := io.instruction_read_data
    pc             := Mux(io.jump_flag_id, io.jump_address_id, pc + 4.U)
  }.otherwise {
    // PC is parked when instruction_valid is false, inject NOP to avoid decode errors
    pc             := pc
    io.instruction := 0x00000013.U // NOP (addi x0, x0, 0)
  }
  io.instruction_address := pc
}
