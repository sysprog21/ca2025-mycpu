// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_forward

import chisel3._
import riscv.Parameters

/**
 * Hazard Detection and Control Unit: Optimized with forwarding support
 *
 * Implements hazard detection for a pipeline with forwarding capability.
 * Only stalls for true load-use hazards that cannot be resolved by forwarding.
 * Significantly improves performance compared to conservative stalling.
 *
 * Hazard Types and Resolution:
 * 1. **Control Hazards**: Branch/jump taken → flush pipeline
 * 2. **Data Hazards (RAW)**:
 *    - ALU-to-ALU dependencies → resolved by forwarding (no stall)
 *    - Load-use dependencies → require 1-cycle stall
 *
 * Load-Use Hazard:
 * Special case where forwarding cannot help because load data is not
 * available until MEM stage completes. Requires one bubble insertion.
 *
 * Example - Load-use hazard requiring stall:
 * ```
 * LW  x1, 0(x2)    # EX stage: load initiated, data not ready
 * ADD x3, x1, x4   # ID stage: needs x1 → STALL 1 cycle
 * ```
 *
 * Example - ALU dependency resolved by forwarding (no stall):
 * ```
 * ADD x1, x2, x3   # EX stage: result available for forwarding
 * SUB x4, x1, x5   # ID stage: x1 forwarded, no stall needed
 * ```
 *
 * Control Signals:
 * - `if_flush`: Clear IF/ID register on control hazard
 * - `id_flush`: Insert bubble for load-use hazard
 * - `pc_stall`: Freeze PC during load-use stall
 * - `if_stall`: Hold IF/ID register during stall
 *
 * Performance Impact:
 * - CPI ~1.1-1.3 (much better than conservative stalling)
 * - Only stalls for ~10-20% of instructions (loads followed by use)
 *
 * @note Works in conjunction with Forwarding unit to minimize stalls
 * @note Load-use is the only data hazard requiring a stall
 */
class Control extends Module {
  val io = IO(new Bundle {
    val jump_flag             = Input(Bool())                                     // ex.io.if_jump_flag
    val rs1_id                = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg1_read_address
    val rs2_id                = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg2_read_address
    val memory_read_enable_ex = Input(Bool())                                     // id2ex.io.output_memory_read_enable
    val rd_ex                 = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_write_address

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

  // Hazard detection with forwarding optimization
  when(io.jump_flag) {
    // =========================== Control Hazard ===========================
    // Jump/branch taken in EX stage - flush incorrectly fetched instructions
    io.if_flush := true.B // Flush IF/ID: discard wrong-path instruction
    io.id_flush := true.B // Flush ID/EX: discard wrong-path instruction

  }.elsewhen(
    // =========================== Load-Use Hazard ===========================
    // Special case: Load instruction followed immediately by dependent instruction
    // Forwarding CANNOT resolve this because load data isn't available until MEM stage
    //
    // Detection conditions (ALL must be true):
    io.memory_read_enable_ex &&                          // 1. EX stage has load instruction
      io.rd_ex =/= 0.U &&                                // 2. Load destination is not x0
      (io.rd_ex === io.rs1_id || io.rd_ex === io.rs2_id) // 3. ID stage uses load destination
      //
      // Example triggering this hazard:
      //   LW  x1, 0(x2)  [EX stage: memory_read_enable_ex=1, rd_ex=x1]
      //   ADD x3, x1, x4 [ID stage: rs1_id=x1] → STALL required
      //
      // Timeline without stall (WRONG):
      //   Cycle N:   LW in EX (initiates memory read)
      //   Cycle N+1: LW in MEM (data arrives), ADD in EX (needs x1 - NOT READY!)
      //
      // Timeline with stall (CORRECT):
      //   Cycle N:   LW in EX
      //   Cycle N+1: LW in MEM, bubble in EX (ADD stalled in ID)
      //   Cycle N+2: LW in WB, ADD in EX (x1 forwarded from MEM/WB)
  ) {
    // Insert one bubble to delay dependent instruction by 1 cycle
    io.id_flush := true.B // Insert NOP/bubble into ID/EX register
    io.pc_stall := true.B // Freeze PC (hold next instruction fetch)
    io.if_stall := true.B // Freeze IF/ID (hold current instruction)
    // After stall: forwarding unit will forward load result from MEM/WB stage
  }
}
