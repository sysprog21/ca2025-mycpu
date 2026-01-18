// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_forward

import chisel3._
import riscv.core.CPUBundle
import riscv.core.CSR
import riscv.core.RegisterFile
import riscv.Parameters

// Five-Stage Pipelined CPU with Forwarding
//
// Pipeline stages: IF → ID → EX → MEM → WB
//
// Architecture:
// - Classic five-stage RISC pipeline with full hazard mitigation
// - Forwarding network bypasses pipeline registers for RAW dependencies
// - Selective stalling only for unavoidable load-use hazards
//
// Hazard handling:
// - Data hazards: Forwarding paths from MEM and WB stages to EX inputs
//   * EX→EX forwarding: Bypass from ALU result (MEM stage) directly to EX
//   * MEM→EX forwarding: Bypass from memory/ALU result (WB stage) to EX
//   * Load-use stall: 1-cycle bubble when load result needed immediately
// - Control hazards: Flush IF and ID stages on taken branches
// - Forwarding priority: MEM-stage (newer) takes precedence over WB-stage
//
// Performance characteristics:
// - CPI: ~1.2-1.5 (reduced stalls via forwarding)
// - Complexity: Medium-High (forwarding network + selective stalling)
// - Educational value: Demonstrates forwarding optimization technique
//
// Forwarding paths:
// - reg1_data_ex: Mux(NoForward, ForwardFromMEM, ForwardFromWB)
// - reg2_data_ex: Mux(NoForward, ForwardFromMEM, ForwardFromWB)
// - Zero register protection: No forwarding when rd = x0
class CPU extends Module {
  val io = IO(new CPUBundle)

  val ctrl       = Module(new Control)
  val regs       = Module(new RegisterFile)
  val inst_fetch = Module(new InstructionFetch)
  val if2id      = Module(new IF2ID)
  val id         = Module(new InstructionDecode)
  val id2ex      = Module(new ID2EX)
  val ex         = Module(new Execute)
  val ex2mem     = Module(new EX2MEM)
  val mem        = Module(new MemoryAccess)
  val mem2wb     = Module(new MEM2WB)
  val wb         = Module(new WriteBack)
  val forwarding = Module(new Forwarding)
  val clint      = Module(new CLINT)
  val csr_regs   = Module(new CSR)

  ctrl.io.jump_flag             := ex.io.if_jump_flag
  ctrl.io.rs1_id                := id.io.regs_reg1_read_address
  ctrl.io.rs2_id                := id.io.regs_reg2_read_address
  ctrl.io.memory_read_enable_ex := id2ex.io.output_memory_read_enable
  ctrl.io.rd_ex                 := id2ex.io.output_regs_write_address

  regs.io.write_enable  := mem2wb.io.output_regs_write_enable
  regs.io.write_address := mem2wb.io.output_regs_write_address
  regs.io.write_data    := wb.io.regs_write_data
  regs.io.read_address1 := id.io.regs_reg1_read_address
  regs.io.read_address2 := id.io.regs_reg2_read_address

  regs.io.debug_read_address := io.debug_read_address
  io.debug_read_data         := regs.io.debug_read_data

  io.instruction_address          := inst_fetch.io.instruction_address
  inst_fetch.io.stall_flag_ctrl   := ctrl.io.pc_stall
  inst_fetch.io.jump_flag_id      := ex.io.if_jump_flag
  inst_fetch.io.jump_address_id   := ex.io.if_jump_address
  inst_fetch.io.rom_instruction   := io.instruction
  inst_fetch.io.instruction_valid := io.instruction_valid

  if2id.io.stall               := ctrl.io.if_stall
  if2id.io.flush               := ctrl.io.if_flush
  if2id.io.instruction         := inst_fetch.io.id_instruction
  if2id.io.instruction_address := inst_fetch.io.instruction_address
  if2id.io.interrupt_flag      := io.interrupt_flag

  id.io.instruction := if2id.io.output_instruction

  id2ex.io.flush                  := ctrl.io.id_flush
  id2ex.io.instruction            := if2id.io.output_instruction
  id2ex.io.instruction_address    := if2id.io.output_instruction_address
  id2ex.io.reg1_data              := regs.io.read_data1
  id2ex.io.reg2_data              := regs.io.read_data2
  id2ex.io.regs_reg1_read_address := id.io.regs_reg1_read_address
  id2ex.io.regs_reg2_read_address := id.io.regs_reg2_read_address
  id2ex.io.regs_write_enable      := id.io.ex_reg_write_enable
  id2ex.io.regs_write_address     := id.io.ex_reg_write_address
  id2ex.io.regs_write_source      := id.io.ex_reg_write_source
  id2ex.io.immediate              := id.io.ex_immediate
  id2ex.io.aluop1_source          := id.io.ex_aluop1_source
  id2ex.io.aluop2_source          := id.io.ex_aluop2_source
  id2ex.io.csr_write_enable       := id.io.ex_csr_write_enable
  id2ex.io.csr_address            := id.io.ex_csr_address
  id2ex.io.memory_read_enable     := id.io.ex_memory_read_enable
  id2ex.io.memory_write_enable    := id.io.ex_memory_write_enable
  id2ex.io.csr_read_data          := csr_regs.io.id_reg_read_data

  ex.io.instruction                     := id2ex.io.output_instruction
  ex.io.instruction_address             := id2ex.io.output_instruction_address
  ex.io.reg1_data                       := id2ex.io.output_reg1_data
  ex.io.reg2_data                       := id2ex.io.output_reg2_data
  ex.io.immediate_id                    := id2ex.io.output_immediate
  ex.io.aluop1_source_id                := id2ex.io.output_aluop1_source
  ex.io.aluop2_source_id                := id2ex.io.output_aluop2_source
  ex.io.csr_read_data_id                := id2ex.io.output_csr_read_data
  ex.io.forward_from_mem                := mem.io.forward_data
  ex.io.forward_from_wb                 := wb.io.regs_write_data
  ex.io.reg1_forward                    := forwarding.io.reg1_forward_ex
  ex.io.reg2_forward                    := forwarding.io.reg2_forward_ex
  ex.io.interrupt_assert_clint          := clint.io.ex_interrupt_assert
  ex.io.interrupt_handler_address_clint := clint.io.ex_interrupt_handler_address

  ex2mem.io.regs_write_enable   := id2ex.io.output_regs_write_enable
  ex2mem.io.regs_write_source   := id2ex.io.output_regs_write_source
  ex2mem.io.regs_write_address  := id2ex.io.output_regs_write_address
  ex2mem.io.instruction_address := id2ex.io.output_instruction_address
  ex2mem.io.funct3              := id2ex.io.output_instruction(14, 12)
  ex2mem.io.reg2_data           := ex.io.mem_reg2_data
  ex2mem.io.memory_read_enable  := id2ex.io.output_memory_read_enable
  ex2mem.io.memory_write_enable := id2ex.io.output_memory_write_enable
  ex2mem.io.alu_result          := ex.io.mem_alu_result
  ex2mem.io.csr_read_data       := id2ex.io.output_csr_read_data

  mem.io.alu_result          := ex2mem.io.output_alu_result
  mem.io.reg2_data           := ex2mem.io.output_reg2_data
  mem.io.memory_read_enable  := ex2mem.io.output_memory_read_enable
  mem.io.memory_write_enable := ex2mem.io.output_memory_write_enable
  mem.io.funct3              := ex2mem.io.output_funct3
  mem.io.regs_write_source   := ex2mem.io.output_regs_write_source
  mem.io.csr_read_data       := ex2mem.io.output_csr_read_data
  io.device_select := mem.io.bundle
    .address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)
  io.memory_bundle <> mem.io.bundle
  io.memory_bundle.address := 0.U(Parameters.SlaveDeviceCountBits.W) ## mem.io.bundle
    .address(Parameters.AddrBits - 1 - Parameters.SlaveDeviceCountBits, 0)

  mem2wb.io.instruction_address := ex2mem.io.output_instruction_address
  mem2wb.io.alu_result          := ex2mem.io.output_alu_result
  mem2wb.io.regs_write_enable   := ex2mem.io.output_regs_write_enable
  mem2wb.io.regs_write_source   := ex2mem.io.output_regs_write_source
  mem2wb.io.regs_write_address  := ex2mem.io.output_regs_write_address
  mem2wb.io.memory_read_data    := mem.io.wb_memory_read_data
  mem2wb.io.csr_read_data       := ex2mem.io.output_csr_read_data

  wb.io.instruction_address := mem2wb.io.output_instruction_address
  wb.io.alu_result          := mem2wb.io.output_alu_result
  wb.io.memory_read_data    := mem2wb.io.output_memory_read_data
  wb.io.regs_write_source   := mem2wb.io.output_regs_write_source
  wb.io.csr_read_data       := mem2wb.io.output_csr_read_data

  forwarding.io.rs1_ex               := id2ex.io.output_regs_reg1_read_address
  forwarding.io.rs2_ex               := id2ex.io.output_regs_reg2_read_address
  forwarding.io.rd_mem               := ex2mem.io.output_regs_write_address
  forwarding.io.reg_write_enable_mem := ex2mem.io.output_regs_write_enable
  forwarding.io.rd_wb                := mem2wb.io.output_regs_write_address
  forwarding.io.reg_write_enable_wb  := mem2wb.io.output_regs_write_enable

  clint.io.instruction_address_if := inst_fetch.io.instruction_address
  clint.io.instruction_address_id := if2id.io.output_instruction_address
  clint.io.instruction_ex         := id2ex.io.output_instruction
  clint.io.jump_flag              := ex.io.clint_jump_flag
  clint.io.jump_address           := ex.io.clint_jump_address
  clint.io.interrupt_flag         := io.interrupt_flag // Direct connection, bypass IF2ID pipeline delay
  clint.io.csr_bundle <> csr_regs.io.clint_access_bundle

  csr_regs.io.reg_read_address_id    := id.io.ex_csr_address
  csr_regs.io.reg_write_enable_ex    := id2ex.io.output_csr_write_enable
  csr_regs.io.reg_write_address_ex   := id2ex.io.output_csr_address
  csr_regs.io.reg_write_data_ex      := ex.io.csr_write_data
  csr_regs.io.debug_reg_read_address := io.csr_debug_read_address
  io.csr_debug_read_data             := csr_regs.io.debug_reg_read_data
}
