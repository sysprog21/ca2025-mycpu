// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_stall

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

/**
 * IF/ID Pipeline Register: buffers instruction fetch output for decode stage
 *
 * Holds fetched instruction and associated metadata between Instruction Fetch
 * and Instruction Decode stages. Supports stalling and flushing for hazard resolution.
 *
 * Data Stored:
 * - Fetched instruction (32-bit RISC-V instruction)
 * - Program counter value (address of this instruction)
 * - Interrupt flags (for interrupt handling)
 *
 * Control Signals:
 * - `stall`: Hold current values (pipeline freeze)
 *   - Used for: Data hazards, load-use stalls
 *   - Effect: Maintains same instruction for multiple cycles
 * - `flush`: Clear register contents (insert bubble/NOP)
 *   - Used for: Control hazards (branch/jump taken)
 *   - Effect: Replaces instruction with NOP
 *
 * Pipeline Behavior:
 * - Normal: Transfer new values on clock edge
 * - Stalled: Maintain current values (freeze)
 * - Flushed: Replace with NOP instruction
 *
 * Timing:
 * - Updated on positive clock edge
 * - Reset clears to NOP instruction and entry address
 *
 * @note Critical for implementing pipeline stalls and flushes
 * @note NOP insertion prevents invalid operations during hazards
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
