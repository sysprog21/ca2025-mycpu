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

// Instruction Fetch stage: maintains PC and fetches instructions from memory
//
// This is the first stage of the processor pipeline, responsible for:
// - Maintaining the program counter (PC) register
// - Providing current PC to instruction memory
// - Handling control flow changes from Execute stage
//
// PC update logic:
// - Sequential: PC = PC + 4 (when no jump/branch)
// - Control flow: PC = jump_address_id (when jump_flag_id asserted by Execute stage)
//
// The instruction_valid signal gates PC updates to handle memory latency and stalls.
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    val jump_flag_id          = Input(Bool())
    val jump_address_id       = Input(UInt(Parameters.AddrWidth))
    val instruction_read_data = Input(UInt(Parameters.DataWidth))
    val instruction_valid     = Input(Bool())

    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val instruction         = Output(UInt(Parameters.InstructionWidth))
  })

  // Program counter register (PC)
  val pc = RegInit(ProgramCounter.EntryAddress)

  // ============================================================
  // [CA25: Exercise 9] PC Update Logic - Sequential vs Control Flow
  // ============================================================
  // Hint: Implement program counter (PC) update logic for sequential execution
  // and control flow changes
  //
  // PC update rules:
  // 1. Control flow (jump/branch taken): PC = jump target address
  //    - When jump flag is asserted, use jump address
  //    - Covers: JAL, JALR, and taken branches (BEQ, BNE, BLT, BGE, BLTU, BGEU)
  // 2. Sequential execution: PC = PC + 4
  //    - When no jump/branch, increment PC by 4 bytes (next instruction)
  //    - RISC-V instructions are 4 bytes (32 bits) in RV32I
  // 3. Invalid instruction: PC = PC (hold current value)
  //    - When instruction is invalid, don't update PC
  //    - Insert NOP to prevent illegal instruction execution
  //
  // Examples:
  // - Normal ADD: PC = 0x1000 → next PC = 0x1004 (sequential)
  // - JAL offset: PC = 0x1000, target = 0x2000 → next PC = 0x2000 (control flow)
  // - BEQ taken: PC = 0x1000, target = 0x0FFC → next PC = 0x0FFC (control flow)
  when(io.instruction_valid) {
    io.instruction := io.instruction_read_data

    // TODO: Complete PC update logic
    // Hint: Use multiplexer to select between jump target and sequential PC
    // - Check jump flag condition
    // - True case: Use jump target address
    // - False case: Sequential execution
    pc := Mux(io.jump_flag_id, io.jump_address_id, pc + 4.U)

  }.otherwise {
    // When instruction is invalid, hold PC and insert NOP (ADDI x0, x0, 0)
    // NOP = 0x00000013 allows pipeline to continue safely without side effects
    pc             := pc
    io.instruction := 0x00000013.U // NOP: prevents illegal instruction execution
  }
  io.instruction_address := pc
}
