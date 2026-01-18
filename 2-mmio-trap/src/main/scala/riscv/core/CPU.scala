// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.Cat
import riscv.CPUBundle
import riscv.Parameters

/**
 * CPU: Single-cycle RISC-V RV32I processor with CSR and interrupt support
 *
 * Extends the basic single-cycle design (1-single-cycle) with privileged
 * architecture features for trap handling and interrupt processing.
 *
 * Pipeline Stages:
 * - IF (InstructionFetch): Fetches instructions and handles interrupt vectoring
 * - ID (InstructionDecode): Decodes instructions including CSR instructions
 * - EX (Execute): Performs ALU operations and CSR read/write operations
 * - MEM (MemoryAccess): Handles load/store and memory-mapped I/O
 * - WB (WriteBack): Selects writeback source (ALU, memory, CSR, or PC+4)
 *
 * Privileged Architecture Components:
 * - CSR (Control and Status Registers): Machine-mode CSRs per RISC-V spec
 *   - mstatus, mie, mtvec, mepc, mcause, mtval, mip (subset)
 * - CLINT (Core-Local Interrupt Controller): Timer and external interrupts
 *   - mtime, mtimecmp registers for timer interrupts
 *   - Interrupt assertion and vectoring logic
 *
 * Key Architectural Features:
 * - Single-cycle execution with interrupt support
 * - Trap handling: Interrupts cause PC redirect to mtvec handler
 * - CSR instructions: CSRRW, CSRRS, CSRRC and immediate variants
 * - Memory-mapped I/O via deviceSelect for peripherals
 * - Priority arbitration: CLINT writes to CSRs take priority over CPU writes
 *
 * Interrupt Handling Flow:
 * 1. External interrupt arrives (io.interrupt_flag)
 * 2. CLINT checks mstatus.MIE and mie enable bits
 * 3. If interrupt taken: save PC to mepc, set mcause, jump to mtvec
 * 4. Handler executes, uses MRET to restore PC from mepc
 *
 * Interface:
 * - instruction_address/instruction: Instruction memory interface
 * - memory_bundle: Data memory and MMIO access
 * - interrupt_flag: External interrupt input
 * - deviceSelect: Peripheral address decoding
 * - regs_debug/csr_regs_debug: Register inspection for testing
 *
 * Compliance:
 * - Implements RV32I base + Zicsr (CSR) extension
 * - Machine mode only (no user/supervisor modes)
 * - Passes RISC-V compliance tests including trap handling
 */
class CPU extends Module {
  val io = IO(new CPUBundle)

  // Pipeline stage modules
  val regs       = Module(new RegisterFile)
  val inst_fetch = Module(new InstructionFetch)
  val id         = Module(new InstructionDecode)
  val ex         = Module(new Execute)
  val mem        = Module(new MemoryAccess)
  val wb         = Module(new WriteBack)

  // Privileged architecture components
  val csr_regs = Module(new CSR)
  val clint    = Module(new CLINT)

  io.regs_debug_read_data            := regs.io.debug_read_data
  io.csr_regs_debug_read_data        := csr_regs.io.debug_reg_read_data
  regs.io.debug_read_address         := io.regs_debug_read_address
  csr_regs.io.debug_reg_read_address := io.csr_regs_debug_read_address

  io.deviceSelect := mem.io.memory_bundle
    .address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)

  inst_fetch.io.jump_address_id           := ex.io.if_jump_address
  inst_fetch.io.jump_flag_id              := ex.io.if_jump_flag
  inst_fetch.io.interrupt_assert          := clint.io.interrupt_assert
  inst_fetch.io.interrupt_handler_address := clint.io.interrupt_handler_address
  inst_fetch.io.instruction_valid         := io.instruction_valid
  inst_fetch.io.instruction_read_data     := io.instruction
  io.instruction_address                  := inst_fetch.io.instruction_address

  regs.io.write_enable  := id.io.reg_write_enable
  regs.io.write_address := id.io.reg_write_address
  regs.io.write_data    := wb.io.regs_write_data
  regs.io.read_address1 := id.io.regs_reg1_read_address
  regs.io.read_address2 := id.io.regs_reg2_read_address

  id.io.instruction := inst_fetch.io.instruction

  csr_regs.io.clint_access_bundle <> clint.io.csr_bundle
  csr_regs.io.reg_read_address_id  := id.io.csr_reg_address
  csr_regs.io.reg_write_data_ex    := ex.io.csr_reg_write_data
  csr_regs.io.reg_write_address_id := id.io.csr_reg_address
  csr_regs.io.reg_write_enable_id  := id.io.csr_reg_write_enable

  ex.io.instruction         := inst_fetch.io.instruction
  ex.io.instruction_address := inst_fetch.io.instruction_address
  ex.io.reg1_data           := regs.io.read_data1
  ex.io.reg2_data           := regs.io.read_data2
  ex.io.immediate           := id.io.ex_immediate
  ex.io.aluop1_source       := id.io.ex_aluop1_source
  ex.io.aluop2_source       := id.io.ex_aluop2_source
  ex.io.csr_reg_read_data   := csr_regs.io.reg_read_data

  mem.io.alu_result          := ex.io.mem_alu_result
  mem.io.reg2_data           := regs.io.read_data2
  mem.io.memory_read_enable  := id.io.memory_read_enable
  mem.io.memory_write_enable := id.io.memory_write_enable
  mem.io.funct3              := inst_fetch.io.instruction(14, 12)

  io.memory_bundle.address := Cat(
    0.U(Parameters.SlaveDeviceCountBits.W),
    mem.io.memory_bundle.address(Parameters.AddrBits - 1 - Parameters.SlaveDeviceCountBits, 0)
  )
  io.memory_bundle.write_enable := mem.io.memory_bundle.write_enable
  io.memory_bundle.write_data   := mem.io.memory_bundle.write_data
  io.memory_bundle.write_strobe := mem.io.memory_bundle.write_strobe
  mem.io.memory_bundle.read_data := io.memory_bundle.read_data

  wb.io.instruction_address := inst_fetch.io.instruction_address
  wb.io.alu_result          := ex.io.mem_alu_result
  wb.io.memory_read_data    := mem.io.wb_memory_read_data
  wb.io.regs_write_source   := id.io.wb_reg_write_source
  wb.io.csr_read_data       := csr_regs.io.reg_read_data

  clint.io.instruction         := inst_fetch.io.instruction
  clint.io.instruction_address := inst_fetch.io.instruction_address
  clint.io.interrupt_flag      := io.interrupt_flag
  clint.io.jump_flag           := ex.io.if_jump_flag
  clint.io.jump_address        := ex.io.if_jump_address
}
