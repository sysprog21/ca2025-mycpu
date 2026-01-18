// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.threestage

import chisel3._
import chisel3.util.MuxCase
import riscv.Parameters

object ProgramCounter {
  val EntryAddress = Parameters.EntryAddress
}

class InstructionFetch extends Module {
  val io = IO(new Bundle {
    val jump_flag_ex      = Input(Bool())
    val jump_address_ex   = Input(UInt(Parameters.AddrWidth))
    val rom_instruction   = Input(UInt(Parameters.DataWidth))
    val instruction_valid = Input(Bool())

    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val id_instruction      = Output(UInt(Parameters.InstructionWidth))
  })
  val pc = RegInit(ProgramCounter.EntryAddress)

  pc := MuxCase(
    pc,
    IndexedSeq(
      io.jump_flag_ex      -> io.jump_address_ex,
      io.instruction_valid -> (pc + 4.U)
    )
  )

  io.instruction_address := pc
  io.id_instruction      := Mux(io.instruction_valid, io.rom_instruction, InstructionsNop.nop)
}
