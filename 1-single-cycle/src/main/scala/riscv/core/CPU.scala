// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.Cat
import riscv.CPUBundle
import riscv.Parameters

/**
 * CPU: Top-level single-cycle RISC-V RV32I processor
 *
 * Implements a classic five-stage pipeline with all stages executing within
 * a single clock cycle. This design prioritizes simplicity and ease of
 * understanding over performance.
 *
 * Pipeline Stages:
 * - IF (InstructionFetch): Fetches instructions from memory using PC
 * - ID (InstructionDecode): Decodes instructions and reads register file
 * - EX (Execute): Performs ALU operations and resolves branches
 * - MEM (MemoryAccess): Handles load/store operations with proper alignment
 * - WB (WriteBack): Selects data to write back to register file
 *
 * Key Architectural Features:
 * - Single-cycle execution: CPI = 1 (no stalls, no forwarding needed)
 * - Direct signal wiring between stages (no pipeline registers)
 * - Branch resolution in EX stage causes 1-cycle penalty on taken branches
 * - Memory-mapped peripheral access via deviceSelect signal
 *
 * Interface:
 * - instruction_address: PC output to instruction memory
 * - instruction: Instruction data from memory (input)
 * - memory_bundle: AXI4-Lite-style interface for data memory access
 * - deviceSelect: Upper address bits for peripheral selection
 * - debug_read_address/data: Register file inspection for testing
 *
 * Compliance:
 * - Implements full RV32I base integer instruction set
 * - No support for: CSR, interrupts, privilege modes (see 2-mmio-trap)
 * - Passes RISC-V compliance test suite for RV32I
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

  io.deviceSelect := mem.io.memory_bundle
    .address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)

  inst_fetch.io.jump_address_id       := ex.io.if_jump_address
  inst_fetch.io.jump_flag_id          := ex.io.if_jump_flag
  inst_fetch.io.instruction_valid     := io.instruction_valid
  inst_fetch.io.instruction_read_data := io.instruction
  io.instruction_address              := inst_fetch.io.instruction_address

  regs.io.write_enable  := id.io.reg_write_enable
  regs.io.write_address := id.io.reg_write_address
  regs.io.write_data    := wb.io.regs_write_data
  regs.io.read_address1 := id.io.regs_reg1_read_address
  regs.io.read_address2 := id.io.regs_reg2_read_address

  regs.io.debug_read_address := io.debug_read_address
  io.debug_read_data         := regs.io.debug_read_data

  id.io.instruction := inst_fetch.io.instruction

  ex.io.aluop1_source       := id.io.ex_aluop1_source
  ex.io.aluop2_source       := id.io.ex_aluop2_source
  ex.io.immediate           := id.io.ex_immediate
  ex.io.instruction         := inst_fetch.io.instruction
  ex.io.instruction_address := inst_fetch.io.instruction_address
  ex.io.reg1_data           := regs.io.read_data1
  ex.io.reg2_data           := regs.io.read_data2

  mem.io.alu_result          := ex.io.mem_alu_result
  mem.io.reg2_data           := regs.io.read_data2
  mem.io.memory_read_enable  := id.io.memory_read_enable
  mem.io.memory_write_enable := id.io.memory_write_enable
  mem.io.funct3              := inst_fetch.io.instruction(14, 12)

  io.memory_bundle.address := Cat(
    0.U(Parameters.SlaveDeviceCountBits.W),
    mem.io.memory_bundle.address(Parameters.AddrBits - 1 - Parameters.SlaveDeviceCountBits, 0)
  )
  io.memory_bundle.write_enable  := mem.io.memory_bundle.write_enable
  io.memory_bundle.write_data    := mem.io.memory_bundle.write_data
  io.memory_bundle.write_strobe  := mem.io.memory_bundle.write_strobe
  mem.io.memory_bundle.read_data := io.memory_bundle.read_data

  wb.io.instruction_address := inst_fetch.io.instruction_address
  wb.io.alu_result          := ex.io.mem_alu_result
  wb.io.memory_read_data    := mem.io.wb_memory_read_data
  wb.io.regs_write_source   := id.io.wb_reg_write_source
}
