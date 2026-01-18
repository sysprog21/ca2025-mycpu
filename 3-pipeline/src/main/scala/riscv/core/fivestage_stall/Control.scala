// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_stall

import chisel3._
import riscv.Parameters

/**
 * Hazard Detection and Control Unit: Conservative stalling strategy
 *
 * Implements a conservative hazard detection strategy that stalls the pipeline
 * for ANY data hazard, without forwarding support. This approach is simple
 * but results in lower performance due to frequent stalls.
 *
 * Hazard Types Detected:
 * 1. **Control Hazards**: Branch/jump instructions changing program flow
 * 2. **Data Hazards (RAW)**: Any register dependency between instructions
 *    - EX stage dependency: Instruction in EX writes to register needed by ID
 *    - MEM stage dependency: Instruction in MEM writes to register needed by ID
 *
 * Stalling Strategy:
 * - **Conservative approach**: Stall for ANY RAW dependency
 * - **No forwarding**: Cannot resolve hazards through bypass paths
 * - **Performance impact**: CPI ~1.5-2.0 due to frequent stalls
 *
 * Control Signals:
 * - `if_flush`: Clear IF/ID pipeline register (control hazard)
 * - `id_flush`: Clear ID/EX pipeline register (insert bubble/NOP)
 * - `pc_stall`: Freeze program counter (prevent new instruction fetch)
 * - `if_stall`: Freeze IF/ID pipeline register (hold current instruction)
 *
 * Example - RAW hazard requiring stall:
 * ```
 * ADD x1, x2, x3  # EX stage, will write x1
 * SUB x4, x1, x5  # ID stage, needs x1 → STALL 1 cycle
 * ```
 *
 * Example - Multiple cycle stall:
 * ```
 * LW  x1, 0(x2)   # MEM stage, will write x1
 * ADD x3, x1, x4  # ID stage, needs x1 → STALL 2 cycles
 * ```
 *
 * @note This is the simplest but least efficient hazard handling approach
 * @note Every data dependency causes a pipeline stall (no optimization)
 */
class Control extends Module {
  val io = IO(new Bundle {
    val jump_flag            = Input(Bool())                                     // ex.io.if_jump_flag
    val rs1_id               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg1_read_address
    val rs2_id               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg2_read_address
    val rd_ex                = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_write_address
    val reg_write_enable_ex  = Input(Bool())                                     // id2ex.io.output_regs_write_enable
    val rd_mem               = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // ex2mem.io.output_regs_write_address
    val reg_write_enable_mem = Input(Bool())                                     // ex2mem.io.output_regs_write_enable

    val if_flush = Output(Bool())
    val id_flush = Output(Bool())
    val pc_stall = Output(Bool())
    val if_stall = Output(Bool())
  })

  // Initialize control signals to default (no stall/flush) state
  io.if_flush := false.B
  io.id_flush := false.B
  io.pc_stall := false.B
  io.if_stall := false.B

  // Hazard detection priority logic
  when(io.jump_flag) {
    // =========================== Control Hazard ===========================
    // Jump/branch taken - must flush incorrectly fetched instructions
    // Instructions in IF and ID stages are on wrong execution path
    io.if_flush := true.B // Clear IF/ID register (discard fetched instruction)
    io.id_flush := true.B // Clear ID/EX register (discard decoded instruction)

  }.elsewhen(
    // =========================== Data Hazard (RAW) ===========================
    // Conservative stalling: ANY register dependency causes a stall
    // No forwarding capability, so must wait for register write to complete

    // Check EX stage dependency (1-cycle old instruction):
    (io.reg_write_enable_ex &&                              // EX stage will write a register
      (io.rd_ex === io.rs1_id || io.rd_ex === io.rs2_id) && // Destination matches ID source
      io.rd_ex =/= 0.U)                                     // Not writing to x0 (always zero)
      ||
        // Check MEM stage dependency (2-cycle old instruction):
        (io.reg_write_enable_mem &&                               // MEM stage will write a register
          (io.rd_mem === io.rs1_id || io.rd_mem === io.rs2_id) && // Destination matches ID source
          io.rd_mem =/= 0.U)                                      // Not writing to x0
  ) {
    // Stall action: Insert bubble (NOP) and freeze earlier stages
    io.id_flush := true.B // Insert NOP into ID/EX register (bubble)
    io.pc_stall := true.B // Freeze PC (don't fetch new instruction)
    io.if_stall := true.B // Freeze IF/ID register (hold current instruction)
    // Result: ID stage instruction waits until dependency resolved
  }
}
