// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_forward

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
 * Data Forwarding Unit: resolves data hazards through register forwarding
 *
 * Implements bypass paths to forward results from later pipeline stages back
 * to the Execute stage, avoiding pipeline stalls when possible.
 *
 * Forwarding Paths:
 * - EX/MEM → EX: Forward ALU result from previous instruction (priority 1)
 * - MEM/WB → EX: Forward memory data or writeback value (priority 2)
 * - No forwarding: Use register file value (default)
 *
 * Forwarding Priority:
 * 1. EX/MEM stage (most recent result) - handles immediate RAW hazard
 * 2. MEM/WB stage (older result) - handles 2-cycle RAW hazard
 * 3. No forwarding (use register file) - no hazard exists
 *
 * Hazard Detection:
 * - RAW (Read After Write) dependencies between instructions
 * - Checks both rs1 and rs2 source registers independently
 * - Register x0 is never forwarded (hardwired to zero)
 *
 * Example RAW hazard resolved by forwarding:
 * ```
 * ADD x1, x2, x3  # EX/MEM stage, writes x1
 * SUB x4, x1, x5  # EX stage, reads x1 → forward from EX/MEM
 * ```
 *
 * Example with multiple hazards:
 * ```
 * ADD x1, x2, x3  # MEM/WB stage, writes x1
 * SUB x4, x5, x6  # EX/MEM stage, writes x4
 * MUL x7, x1, x4  # EX stage, reads x1 from WB, x4 from MEM
 * ```
 *
 * @note Load-use hazards still require a 1-cycle stall even with forwarding
 * @note This unit handles forwarding control signals only; actual data muxing
 *       happens in the Execute stage based on these control signals
 */
class Forwarding extends Module {
  val io = IO(new Bundle() {
    val rs1_ex               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_reg1_read_address
    val rs2_ex               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_reg2_read_address
    val rd_mem               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // ex2mem.io.output_regs_write_address
    val reg_write_enable_mem = Input(Bool())                                     // ex2mem.io.output_regs_write_enable
    val rd_wb                = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // mem2wb.io.output_regs_write_address
    val reg_write_enable_wb  = Input(Bool())                                     // mem2wb.io.output_regs_write_enable

    // Forwarding Type
    val reg1_forward_ex = Output(UInt(2.W)) // ex.io.reg1_forward
    val reg2_forward_ex = Output(UInt(2.W)) // ex.io.reg2_forward
  })

  // Forwarding logic for source register rs1:
  // Priority-based forwarding to resolve RAW hazards for the first source operand
  // Check conditions in priority order: MEM stage has priority over WB stage
  when(io.reg_write_enable_mem && io.rs1_ex === io.rd_mem && io.rd_mem =/= 0.U) {
    // Priority 1 (ForwardFromMEM): Forward from EX/MEM pipeline register if:
    //   - EX/MEM stage will write to register file (reg_write_enable_mem)
    //   - EX/MEM destination register matches current rs1 (rs1_ex === rd_mem)
    //   - Destination is not x0 register (rd_mem =/= 0) - x0 is always zero
    // This handles immediate RAW hazard from the previous instruction
    io.reg1_forward_ex := ForwardingType.ForwardFromMEM
  }.elsewhen(io.reg_write_enable_wb && io.rs1_ex === io.rd_wb && io.rd_wb =/= 0.U) {
    // Priority 2 (ForwardFromWB): Forward from MEM/WB pipeline register if:
    //   - MEM/WB stage will write to register file (reg_write_enable_wb)
    //   - MEM/WB destination register matches current rs1 (rs1_ex === rd_wb)
    //   - Destination is not x0 register (rd_wb =/= 0)
    // This handles 2-cycle RAW hazard from instruction two cycles ago
    io.reg1_forward_ex := ForwardingType.ForwardFromWB
  }.otherwise {
    // No forwarding needed: Use value from register file
    // Either no hazard exists or source is x0 (always zero)
    io.reg1_forward_ex := ForwardingType.NoForward
  }

  // Forwarding logic for source register rs2:
  // Identical priority-based logic for the second source operand
  // Independent from rs1 forwarding - both can forward simultaneously
  when(io.reg_write_enable_mem && io.rs2_ex === io.rd_mem && io.rd_mem =/= 0.U) {
    // Priority 1: Forward from EX/MEM for immediate RAW hazard
    // Example: ADD x1, x2, x3; SUB x4, x5, x1 (x1 forwarded to rs2)
    io.reg2_forward_ex := ForwardingType.ForwardFromMEM
  }.elsewhen(io.reg_write_enable_wb && io.rs2_ex === io.rd_wb && io.rd_wb =/= 0.U) {
    // Priority 2: Forward from MEM/WB for 2-cycle RAW hazard
    io.reg2_forward_ex := ForwardingType.ForwardFromWB
  }.otherwise {
    // No forwarding: Use register file value
    io.reg2_forward_ex := ForwardingType.NoForward
  }

//  io.reg1_forward_ex := ForwardingType.NoForward
//  io.reg2_forward_ex := ForwardingType.NoForward
//  when(io.reg_write_enable_mem && (io.rs1_ex === io.rd_mem || io.rs2_ex === io.rd_mem) && io.rd_mem =/= 0.U){
//    when(io.rs1_ex === io.rd_mem){
//      io.reg1_forward_ex := ForwardingType.ForwardFromMEM
//    }.elsewhen(io.rs2_ex === io.rd_mem){
//      io.reg2_forward_ex := ForwardingType.ForwardFromMEM
//    }
//  }.elsewhen(io.reg_write_enable_wb && (io.rs1_ex === io.rd_wb || io.rs2_ex === io.rd_wb) && io.rd_wb =/= 0.U){
//    when(io.rs1_ex === io.rd_wb){
//      io.reg1_forward_ex := ForwardingType.ForwardFromWB
//    }.elsewhen(io.rs2_ex === io.rd_wb){
//      io.reg2_forward_ex := ForwardingType.ForwardFromWB
//    }
//  }
}
