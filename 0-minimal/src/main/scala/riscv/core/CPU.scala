// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.CPUBundle

// Minimal CPU: single-cycle RISC-V processor supporting only jit.asmbin instructions
// Instructions supported: AUIPC, ADDI, LW, SW, JALR (5 instructions total)
// Testing uses debug interface for register verification (no ECALL needed)
class CPU extends Module {
  val io = IO(new CPUBundle)

  // Pipeline stage modules
  val regs       = Module(new RegisterFile)
  val inst_fetch = Module(new InstructionFetch)
  val id         = Module(new InstructionDecode)
  val ex         = Module(new Execute)
  val mem        = Module(new MemoryAccess)
  val wb         = Module(new WriteBack)

  // Instruction Fetch Stage (IF)
  inst_fetch.io.jump_address_id       := ex.io.if_jump_address
  inst_fetch.io.jump_flag_id          := ex.io.if_jump_flag
  inst_fetch.io.instruction_valid     := io.instruction_valid
  inst_fetch.io.instruction_read_data := io.instruction
  io.instruction_address              := inst_fetch.io.instruction_address

  // Instruction Decode Stage (ID)
  id.io.instruction := inst_fetch.io.instruction

  // Register File
  regs.io.write_enable       := id.io.reg_write_enable
  regs.io.write_address      := id.io.reg_write_address
  regs.io.write_data         := wb.io.regs_write_data
  regs.io.read_address1      := id.io.regs_reg1_read_address
  regs.io.read_address2      := id.io.regs_reg2_read_address
  regs.io.debug_read_address := io.debug_read_address
  io.debug_read_data         := regs.io.debug_read_data

  // Execute Stage (EX)
  ex.io.aluop1_source       := id.io.ex_aluop1_source
  ex.io.immediate           := id.io.ex_immediate
  ex.io.instruction         := inst_fetch.io.instruction
  ex.io.instruction_address := inst_fetch.io.instruction_address
  ex.io.reg1_data           := regs.io.read_data1

  // Memory Access Stage (MEM)
  mem.io.alu_result          := ex.io.mem_alu_result
  mem.io.reg2_data           := regs.io.read_data2
  mem.io.memory_read_enable  := id.io.memory_read_enable
  mem.io.memory_write_enable := id.io.memory_write_enable
  io.memory_bundle <> mem.io.memory_bundle

  // Write Back Stage (WB)
  wb.io.instruction_address := inst_fetch.io.instruction_address
  wb.io.alu_result          := ex.io.mem_alu_result
  wb.io.memory_read_data    := mem.io.wb_memory_read_data
  wb.io.regs_write_source   := id.io.wb_reg_write_source
}
