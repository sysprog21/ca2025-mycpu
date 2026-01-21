// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

// Program counter reset value
object ProgramCounter {
  val EntryAddress = Parameters.EntryAddress
}

// Instruction Fetch stage: maintains PC, fetches instructions, and handles interrupts
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    val jump_flag_id              = Input(Bool())
    val jump_address_id           = Input(UInt(Parameters.AddrWidth))
    val interrupt_assert          = Input(Bool())
    val interrupt_handler_address = Input(UInt(Parameters.AddrWidth))
    val instruction_read_data     = Input(UInt(Parameters.DataWidth))
    val instruction_valid         = Input(Bool())

    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val instruction         = Output(UInt(Parameters.InstructionWidth))
  })

  // Program counter register (PC)
  val pc = RegInit(ProgramCounter.EntryAddress)

  // ============================================================
  // [CA25: Exercise 13] PC Update Logic - Sequential vs Control Flow with Interrupts
  // ============================================================
  // Hint: Implement program counter (PC) update logic for sequential execution,
  // control flow changes, and interrupt handling
  //
  // PC update rules:
  // 1. Interrupt asserted: PC = interrupt handler address (highest priority)
  //    - When interrupt is asserted, vector to trap handler
  //    - Saves current PC to mepc before jump (handled by CLINT)
  // 2. Control flow (jump/branch taken): PC = jump target address
  //    - When jump flag is asserted, use jump address
  //    - Covers: JAL, JALR, taken branches, and MRET
  // 3. Sequential execution: PC = PC + 4
  //    - When no interrupt/jump/branch, increment PC by 4 bytes (next instruction)
  //    - RISC-V instructions are 4 bytes (32 bits) in RV32I
  // 4. Invalid instruction: PC = PC (hold current value)
  //    - When instruction is invalid, don't update PC
  //    - Insert NOP to prevent illegal instruction execution
  //
  // Priority: Interrupt > Jump/Branch > Sequential
  //
  // Examples:
  // - Normal ADD: PC = 0x1000 → next PC = 0x1004 (sequential)
  // - JAL offset: PC = 0x1000, target = 0x2000 → next PC = 0x2000 (control flow)
  // - Timer interrupt: PC = 0x1000, handler = 0x8000 → next PC = 0x8000 (interrupt)
  when(io.instruction_valid) {
    io.instruction := io.instruction_read_data

    // TODO: Complete PC update logic with interrupt priority
    // Hint: Use nested multiplexer to implement priority: interrupt > jump > sequential
    // - Outermost multiplexer: Check interrupt condition
    //   - True: Use interrupt handler address
    //   - False: Check jump/branch condition
    // - Inner multiplexer: Check jump flag
    //   - True: Use jump target address
    //   - False: Sequential execution
    pc := ?

  }.otherwise {
    // When instruction is invalid, hold PC and insert NOP (ADDI x0, x0, 0)
    // NOP = 0x00000013 allows pipeline to continue safely without side effects
    pc             := pc
    io.instruction := 0x00000013.U // NOP: prevents illegal instruction execution
  }
  io.instruction_address := pc
}
