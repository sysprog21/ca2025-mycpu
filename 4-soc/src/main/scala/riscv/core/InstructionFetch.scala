// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.MuxCase
import riscv.Parameters

object ProgramCounter {
  val EntryAddress = Parameters.EntryAddress
}

/**
 * Instruction Fetch Stage with Branch Prediction (BTB + RAS)
 *
 * This module implements the IF stage of the 5-stage pipeline with integrated
 * branch prediction using two complementary predictors:
 *
 * Branch Target Buffer (BTB):
 * - 32-entry direct-mapped cache indexed by PC[6:2]
 * - Stores branch/jump targets with 2-bit saturating counters
 * - Predicts taken when: BTB hit AND counter >= 2 (weakly/strongly taken)
 * - Updated in ID stage when branches resolve
 *
 * Return Address Stack (RAS):
 * - 4-entry circular stack for JALR return prediction
 * - Push on call: JAL/JALR with rd=x1 (ra) or rd=x5 (t0)
 * - Pop on return: JALR with rs1=x1/x5, rd=x0
 * - Speculative pop in IF stage when return pattern detected
 *
 * Prediction Priority (highest to lowest):
 * 1. Pending jump (deferred from stall) - correctness requirement
 * 2. BTB misprediction correction - rollback to correct PC
 * 3. Jump from ID stage - actual resolved target
 * 4. RAS prediction - return address prediction (most specific for returns)
 * 5. BTB prediction - branch/jump target prediction
 * 6. Sequential PC+4 - default fall-through
 *
 * BTB vs RAS Selection:
 * For JALR instructions, both BTB and RAS may have predictions:
 * - RAS is preferred for return patterns (JALR rs1=ra/t0, rd=x0) because
 *   it specifically tracks call/return sequences with ~90%+ accuracy
 * - BTB may have stale JALR targets from previous executions
 * - When RAS predicts (speculative pop succeeded), RAS target is used
 * - BTB still updates on JALR resolution to capture non-return JALR targets
 *
 * Misprediction Handling:
 * - BTB wrong direction: Predicted taken, actually not taken → redirect to PC+4
 * - BTB wrong target: Predicted taken with wrong target → redirect to correct target
 * - BTB aliasing: BTB hit on non-branch instruction → redirect to PC+4, invalidate entry
 * - RAS wrong target: RAS predicted return address incorrect → redirect to correct target
 *
 * Performance Characteristics:
 * - Correct BTB/RAS prediction: 0 cycle penalty (flush suppressed)
 * - BTB/RAS misprediction: 1 cycle penalty (IF flush)
 * - No prediction (cold miss): 1 cycle penalty for taken branches
 *
 * CSR Counter Support:
 * - mhpmcounter3: Branch mispredictions (BTB/RAS wrong direction or target)
 * - mhpmcounter7: BTB miss penalty (cold misses + wrong target predictions)
 * - mhpmcounter8: Total branches resolved (accuracy denominator)
 * - mhpmcounter9: BTB predictions made (coverage numerator)
 */
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    val stall_flag_ctrl   = Input(Bool())
    val jump_flag_id      = Input(Bool())
    val jump_address_id   = Input(UInt(Parameters.AddrWidth))
    val rom_instruction   = Input(UInt(Parameters.DataWidth))
    val instruction_valid = Input(Bool())

    // BTB misprediction correction (from ID stage)
    val btb_mispredict         = Input(Bool())                     // BTB predicted wrong
    val btb_correction_addr    = Input(UInt(Parameters.AddrWidth)) // Correct PC
    val btb_correct_prediction = Input(Bool())                     // BTB predicted correctly - skip PC redirect

    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val id_instruction      = Output(UInt(Parameters.InstructionWidth))

    // BTB prediction info passed to ID stage
    val btb_predicted_taken  = Output(Bool())
    val btb_predicted_target = Output(UInt(Parameters.AddrWidth))

    // BTB update interface (from ID stage)
    val btb_update_valid  = Input(Bool())
    val btb_update_pc     = Input(UInt(Parameters.AddrWidth))
    val btb_update_target = Input(UInt(Parameters.AddrWidth))
    val btb_update_taken  = Input(Bool())

    // RAS prediction info passed to ID stage
    val ras_predicted_valid  = Output(Bool())
    val ras_predicted_target = Output(UInt(Parameters.AddrWidth))

    // RAS update interface (from ID stage)
    val ras_push      = Input(Bool()) // JAL with rd=ra detected
    val ras_push_addr = Input(UInt(Parameters.AddrWidth))
    val ras_pop       = Input(Bool()) // JALR with rs1=ra resolved (for misprediction handling)

    // RAS misprediction correction (from ID stage)
    val ras_restore       = Input(Bool())
    val ras_restore_addr  = Input(UInt(Parameters.AddrWidth))
    val ras_restore_valid = Input(Bool())

    // IndirectBTB prediction info passed to ID stage
    val ibtb_predicted_valid  = Output(Bool())
    val ibtb_predicted_target = Output(UInt(Parameters.AddrWidth))

    // IndirectBTB update interface (from ID stage)
    val ibtb_update_valid    = Input(Bool())
    val ibtb_update_pc       = Input(UInt(Parameters.AddrWidth))
    val ibtb_update_rs1_hash = Input(UInt(8.W))
    val ibtb_update_target   = Input(UInt(Parameters.AddrWidth))
  })
  val pc = RegInit(ProgramCounter.EntryAddress)

  // Branch Target Buffer for branch prediction (32 entries for better coverage)
  val btb = Module(new BranchTargetBuffer(entries = 32))
  btb.io.pc := pc

  // BTB prediction: use predicted target if BTB predicts taken
  val btb_next_pc = btb.io.predicted_pc
  io.btb_predicted_taken  := btb.io.predicted_taken
  io.btb_predicted_target := btb.io.predicted_pc

  // Return Address Stack for JALR return prediction
  val ras = Module(new ReturnAddressStack(depth = 4))

  // Indirect Branch Target Buffer for non-return JALR prediction
  // Handles function pointers, vtables, computed jumps that RAS doesn't cover
  val ibtb = Module(new IndirectBTB(entries = 8))
  ibtb.io.pc := pc

  // Detect JALR with rs1=ra (x1) or rs1=t0 (x5) in fetched instruction for speculative pop
  // JALR opcode = 0b1100111, rs1 is bits [19:15], rd is bits [11:7]
  val inst        = io.rom_instruction
  val is_jalr     = inst(6, 0) === "b1100111".U
  val jalr_rs1    = inst(19, 15)
  val jalr_rd     = inst(11, 7)
  val is_ra_or_t0 = jalr_rs1 === 1.U || jalr_rs1 === 5.U        // x1 (ra) or x5 (t0)
  val is_return   = is_jalr && is_ra_or_t0 && (jalr_rd === 0.U) // JALR rd=x0, rs1=ra/t0 is return pattern

  // Speculative RAS pop in IF stage when return detected and not stalled
  val speculative_ras_pop = is_return && io.instruction_valid && !io.stall_flag_ctrl

  // RAS connections
  ras.io.push          := io.ras_push
  ras.io.push_addr     := io.ras_push_addr
  ras.io.pop           := speculative_ras_pop || io.ras_pop
  ras.io.restore       := io.ras_restore
  ras.io.restore_addr  := io.ras_restore_addr
  ras.io.restore_valid := io.ras_restore_valid

  // RAS prediction output (for ID stage to detect misprediction)
  io.ras_predicted_valid  := ras.io.valid && speculative_ras_pop
  io.ras_predicted_target := ras.io.predicted_addr

  // IndirectBTB prediction: for non-return JALR (function pointers, vtables)
  // Only predict when instruction is JALR but not a return pattern (RAS handles returns)
  val is_indirect_jalr    = is_jalr && !is_return && io.instruction_valid && !io.stall_flag_ctrl
  val ibtb_prediction_hit = ibtb.io.hit && is_indirect_jalr

  // IndirectBTB prediction output (for ID stage to detect misprediction)
  io.ibtb_predicted_valid  := ibtb_prediction_hit
  io.ibtb_predicted_target := ibtb.io.predicted_target

  // Latch jump request when stall is active
  // Problem: When mem_stall releases, PipelineRegister's combinational bypass
  // immediately changes IF2ID output, causing jump_flag_id to become false.
  // Solution: Latch jump request during stall so it's taken when stall releases.
  val pending_jump      = RegInit(false.B)
  val pending_jump_addr = RegInit(0.U(Parameters.AddrWidth))

  // Shadow registers to capture jump info before flush clears ID stage
  // This solves the race condition where:
  // - Cycle N: Jump in ID triggers flush
  // - Cycle N+1: Jump moves to EX, ID flushed, stall begins
  // Without shadow registers, pending_jump can't capture the jump because
  // by the time stall activates, jump_flag_id is already 0 (ID was flushed).
  val prev_jump_flag = RegNext(io.jump_flag_id, false.B)
  val prev_jump_addr = RegNext(io.jump_address_id, 0.U)

  // Latch jump when stall blocks it
  // Check both current cycle (normal case) and previous cycle (flush race case)
  when(io.stall_flag_ctrl && (io.jump_flag_id || prev_jump_flag)) {
    pending_jump      := true.B
    pending_jump_addr := Mux(io.jump_flag_id, io.jump_address_id, prev_jump_addr)
  }.elsewhen(!io.stall_flag_ctrl) {
    // Clear pending jump when we can take it (or when there's no jump)
    pending_jump := false.B
  }

  // Take pending jump (priority) or current jump, respecting stall
  val take_pending = pending_jump && !io.stall_flag_ctrl
  // Skip current jump if BTB already predicted correctly - IF already at correct target
  val take_current = io.jump_flag_id && !io.stall_flag_ctrl && !pending_jump && !io.btb_correct_prediction
  // BTB misprediction correction: redirect to correct PC
  val take_btb_correction = io.btb_mispredict && !io.stall_flag_ctrl

  // RAS prediction: use RAS target for returns when valid
  val ras_prediction_valid = io.ras_predicted_valid

  // Default PC selection: RAS > IndirectBTB > BTB > sequential
  // Priority for JALR instructions:
  // 1. RAS prediction for returns (highest accuracy for call/return patterns)
  // 2. IndirectBTB for other JALR (function pointers, computed jumps)
  // 3. BTB (fallback, less accurate for JALR but may have stale entry)
  val default_next_pc = Mux(
    ras_prediction_valid,
    ras.io.predicted_addr, // RAS prediction for returns
    Mux(
      ibtb_prediction_hit,
      ibtb.io.predicted_target,                          // IndirectBTB prediction for non-return JALR
      Mux(btb.io.predicted_taken, btb_next_pc, pc + 4.U) // BTB prediction or sequential
    )
  )

  // Next PC selection priority:
  // 1. Pending jump (deferred from stall)
  // 2. BTB misprediction correction (rollback to sequential PC)
  // 3. Actual jump from ID stage (branch taken / jump)
  // 4. RAS prediction (speculative return address)
  // 5. BTB prediction (speculative branch target)
  // 6. Sequential PC+4 (default)
  val next_pc = MuxCase(
    default_next_pc,
    IndexedSeq(
      take_pending                                  -> pending_jump_addr,
      take_btb_correction                           -> io.btb_correction_addr,
      take_current                                  -> io.jump_address_id,
      (io.stall_flag_ctrl || !io.instruction_valid) -> pc
    )
  )

  pc := next_pc

  io.instruction_address := pc
  io.id_instruction      := Mux(io.instruction_valid, io.rom_instruction, InstructionsNop.nop)

  // BTB update interface - connect external update signals to BTB
  btb.io.update_valid  := io.btb_update_valid
  btb.io.update_pc     := io.btb_update_pc
  btb.io.update_target := io.btb_update_target
  btb.io.update_taken  := io.btb_update_taken

  // IndirectBTB update interface - connect external update signals
  ibtb.io.update_valid    := io.ibtb_update_valid
  ibtb.io.update_pc       := io.ibtb_update_pc
  ibtb.io.update_rs1_hash := io.ibtb_update_rs1_hash
  ibtb.io.update_target   := io.ibtb_update_target
}
