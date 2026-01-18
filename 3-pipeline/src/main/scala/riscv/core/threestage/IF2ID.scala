// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.threestage

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

class IF2ID extends Module {
  val io = IO(new Bundle {
    val flush               = Input(Bool())
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val interrupt_flag      = Input(UInt(Parameters.InterruptFlagWidth))

    val output_instruction         = Output(UInt(Parameters.DataWidth))
    val output_instruction_address = Output(UInt(Parameters.AddrWidth))
    val output_interrupt_flag      = Output(UInt(Parameters.InterruptFlagWidth))
  })

  val stall = false.B

  val instruction = Module(new PipelineRegister(defaultValue = InstructionsNop.nop))
  instruction.io.in     := io.instruction
  instruction.io.stall  := stall
  instruction.io.flush  := io.flush
  io.output_instruction := instruction.io.out

  val instruction_address = Module(new PipelineRegister(defaultValue = ProgramCounter.EntryAddress))
  instruction_address.io.in     := io.instruction_address
  instruction_address.io.stall  := stall
  instruction_address.io.flush  := io.flush
  io.output_instruction_address := instruction_address.io.out

  val interrupt_flag = Module(new PipelineRegister(Parameters.InterruptFlagBits))
  interrupt_flag.io.in     := io.interrupt_flag
  interrupt_flag.io.stall  := stall
  interrupt_flag.io.flush  := io.flush
  io.output_interrupt_flag := interrupt_flag.io.out
}
