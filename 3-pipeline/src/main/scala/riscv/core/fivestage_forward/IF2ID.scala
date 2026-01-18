// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_forward

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

/**
 * IF/ID Pipeline Register: optimized for forwarding pipeline
 *
 * Pipeline register for forwarding-enabled processor. Supports both
 * stall and flush for efficient hazard handling with minimal performance impact.
 *
 * Optimization Features:
 * - Works with forwarding to minimize stalls
 * - Only stalls for true load-use hazards
 * - Flushes for control hazards (branches/jumps)
 *
 * Control Signal Interaction:
 * - `stall`: Used only for load-use hazards (rare with forwarding)
 * - `flush`: Used for control hazards (branch misprediction)
 *
 * Performance Benefits:
 * - Stalls occur ~10-20% of time (vs 40-50% without forwarding)
 * - Most RAW hazards resolved by forwarding
 *
 * @note Forwarding reduces stall frequency significantly
 * @note Load-use is primary cause of stalls in this configuration
 */
class IF2ID extends Module {
  val io = IO(new Bundle {
    val stall               = Input(Bool())
    val flush               = Input(Bool())
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val interrupt_flag      = Input(UInt(Parameters.InterruptFlagWidth))

    val output_instruction         = Output(UInt(Parameters.DataWidth))
    val output_instruction_address = Output(UInt(Parameters.AddrWidth))
    val output_interrupt_flag      = Output(UInt(Parameters.InterruptFlagWidth))
  })

  val instruction = Module(new PipelineRegister(defaultValue = InstructionsNop.nop))
  instruction.io.in     := io.instruction
  instruction.io.stall  := io.stall
  instruction.io.flush  := io.flush
  io.output_instruction := instruction.io.out

  val instruction_address = Module(new PipelineRegister(defaultValue = ProgramCounter.EntryAddress))
  instruction_address.io.in     := io.instruction_address
  instruction_address.io.stall  := io.stall
  instruction_address.io.flush  := io.flush
  io.output_instruction_address := instruction_address.io.out

  val interrupt_flag = Module(new PipelineRegister(Parameters.InterruptFlagBits))
  interrupt_flag.io.in     := io.interrupt_flag
  interrupt_flag.io.stall  := io.stall
  interrupt_flag.io.flush  := io.flush
  io.output_interrupt_flag := interrupt_flag.io.out
}
