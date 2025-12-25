// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

/**
 * Forwarding type enumeration for bypass path selection
 *
 * Defines the source of forwarded data:
 * - NoForward: Use register file value (no forwarding needed)
 * - ForwardFromMEM: Forward from EX/MEM pipeline register (1 cycle old)
 * - ForwardFromWB: Forward from MEM/WB pipeline register (2 cycles old)
 */
object ForwardingType {
  val NoForward      = 0.U(2.W)
  val ForwardFromMEM = 1.U(2.W)
  val ForwardFromWB  = 2.U(2.W)
}

/**
 * Enhanced Data Forwarding Unit: resolves data hazards with dual-stage forwarding
 *
 * Advanced forwarding unit that provides bypass paths to both ID and EX stages,
 * enabling early branch resolution and reducing control hazard penalties.
 *
 * Key Enhancements over Basic Forwarding:
 * - ID stage forwarding: Enables early branch comparison in decode stage
 * - Dual-stage support: Simultaneous forwarding to both ID and EX stages
 * - Reduced branch penalty: Branch decisions made 1 cycle earlier
 *
 * Forwarding Paths:
 * ID Stage Forwarding (for branch comparison):
 * - EX/MEM → ID: Forward for immediate branch operand resolution
 * - MEM/WB → ID: Forward for 2-cycle old branch operands
 *
 * EX Stage Forwarding (for ALU operations):
 * - EX/MEM → EX: Forward ALU result from previous instruction
 * - MEM/WB → EX: Forward memory or writeback value
 *
 * Performance Benefits:
 * - Branch penalty reduced from 2 cycles to 1 cycle
 * - Earlier hazard resolution for control flow instructions
 * - Improved CPI for branch-heavy code
 *
 * Example - Early branch resolution with ID forwarding:
 * ```
 * ADD  x1, x2, x3   # EX stage, result available
 * BEQ  x1, x4, label # ID stage, x1 forwarded from EX/MEM
 * NOP               # Only 1 bubble needed (vs. 2 without ID forwarding)
 * ```
 *
 * @note This is the most optimized forwarding configuration
 * @note ID forwarding requires additional bypass paths in decode stage
 */
class Forwarding extends Module {
  val io = IO(new Bundle() {
    val rs1_id               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg1_read_address             //
    val rs2_id               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg2_read_address             //
    val rs1_ex               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_reg1_read_address
    val rs2_ex               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_reg2_read_address
    val rd_mem               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // ex2mem.io.output_regs_write_address
    val reg_write_enable_mem = Input(Bool())                                     // ex2mem.io.output_regs_write_enable
    val rd_wb                = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // mem2wb.io.output_regs_write_address
    val reg_write_enable_wb  = Input(Bool())                                     // mem2wb.io.output_regs_write_enable

    val reg1_forward_id = Output(UInt(2.W)) // id.io.reg1_forward                       //
    val reg2_forward_id = Output(UInt(2.W)) // id.io.reg2_forward                       //
    val reg1_forward_ex = Output(UInt(2.W)) // ex.io.reg1_forward
    val reg2_forward_ex = Output(UInt(2.W)) // ex.io.reg2_forward
  })

  // ==================== EX Stage Forwarding Logic ====================
  // Forwarding for ALU operations and memory address calculation

  // EX stage rs1 forwarding: Resolve RAW hazards for first ALU operand
  when(io.reg_write_enable_mem && io.rs1_ex === io.rd_mem && io.rd_mem =/= 0.U) {
    // Priority 1: Forward from EX/MEM stage (1-cycle RAW hazard)
    // Most recent result takes precedence
    io.reg1_forward_ex := ForwardingType.ForwardFromMEM
  }.elsewhen(io.reg_write_enable_wb && io.rs1_ex === io.rd_wb && io.rd_wb =/= 0.U) {
    // Priority 2: Forward from MEM/WB stage (2-cycle RAW hazard)
    // Older result if no newer hazard exists
    io.reg1_forward_ex := ForwardingType.ForwardFromWB
  }.otherwise {
    // No hazard: Use register file value
    io.reg1_forward_ex := ForwardingType.NoForward
  }

  // EX stage rs2 forwarding: Resolve RAW hazards for second ALU operand
  when(io.reg_write_enable_mem && io.rs2_ex === io.rd_mem && io.rd_mem =/= 0.U) {
    // Priority 1: Forward from EX/MEM stage
    // Example: ADD x1, x2, x3; SUB x4, x5, x1 (forward x1 to rs2)
    io.reg2_forward_ex := ForwardingType.ForwardFromMEM
  }.elsewhen(io.reg_write_enable_wb && io.rs2_ex === io.rd_wb && io.rd_wb =/= 0.U) {
    // Priority 2: Forward from MEM/WB stage
    io.reg2_forward_ex := ForwardingType.ForwardFromWB
  }.otherwise {
    // No hazard: Use register file value
    io.reg2_forward_ex := ForwardingType.NoForward
  }

  // ==================== ID Stage Forwarding Logic ====================
  // Forwarding for branch comparison and early hazard resolution
  // Enables branch decision in ID stage instead of EX stage

  // ID stage rs1 forwarding: Enable early branch operand resolution
  when(io.reg_write_enable_mem && io.rs1_id === io.rd_mem && io.rd_mem =/= 0.U) {
    // Forward from EX/MEM to ID for branch comparison
    // Example: ADD x1, x2, x3 (in EX); BEQ x1, x4, label (in ID)
    // Without this: 2-cycle branch penalty
    // With this: 1-cycle branch penalty
    io.reg1_forward_id := ForwardingType.ForwardFromMEM
  }.elsewhen(io.reg_write_enable_wb && io.rs1_id === io.rd_wb && io.rd_wb =/= 0.U) {
    // Forward from MEM/WB to ID for older dependencies
    io.reg1_forward_id := ForwardingType.ForwardFromWB
  }.otherwise {
    // No forwarding needed for ID stage rs1
    io.reg1_forward_id := ForwardingType.NoForward
  }

  // ID stage rs2 forwarding: Enable early branch operand resolution
  when(io.reg_write_enable_mem && io.rs2_id === io.rd_mem && io.rd_mem =/= 0.U) {
    // Forward from EX/MEM to ID for second branch operand
    // Critical for instructions like: BEQ x1, x2, label
    // where both operands may have pending writes
    io.reg2_forward_id := ForwardingType.ForwardFromMEM
  }.elsewhen(io.reg_write_enable_wb && io.rs2_id === io.rd_wb && io.rd_wb =/= 0.U) {
    // Forward from MEM/WB to ID
    io.reg2_forward_id := ForwardingType.ForwardFromWB
  }.otherwise {
    // No forwarding needed for ID stage rs2
    io.reg2_forward_id := ForwardingType.NoForward
  }
}
