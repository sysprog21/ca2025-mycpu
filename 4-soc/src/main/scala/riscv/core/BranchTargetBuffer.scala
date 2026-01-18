// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * Branch Target Buffer with 2-bit Saturating Counter Predictor
 *
 * Architecture:
 * - Direct-mapped cache indexed by PC bits (configurable size, default 16 entries)
 * - Each entry stores: valid bit, tag, target address, 2-bit counter
 * - Prediction: taken on hit AND counter >= 2 (weakly/strongly taken)
 *
 * 2-bit Saturating Counter States:
 * - 0: Strongly Not Taken (SNT)
 * - 1: Weakly Not Taken (WNT)
 * - 2: Weakly Taken (WT)
 * - 3: Strongly Taken (ST)
 *
 * Counter Transitions:
 * - Branch taken: increment (saturate at 3)
 * - Branch not taken: decrement (saturate at 0)
 * - New entry: initialize to Weakly Taken (2)
 *
 * Operation:
 * - IF stage: Look up BTB using current PC, predict taken if hit && counter >= 2
 * - ID stage: Update BTB when branch/jump resolves, adjust counter
 *
 * Performance:
 * - Better than static "always taken" for alternating branch patterns
 * - Hysteresis prevents single misprediction from flipping direction
 *
 * @param entries Number of BTB entries (must be power of 2)
 */
class BranchTargetBuffer(entries: Int = 16) extends Module {
  require(isPow2(entries), "BTB entries must be power of 2")

  val indexBits = log2Ceil(entries)
  val tagBits   = Parameters.AddrBits - indexBits - 2 // -2 for 4-byte alignment

  val io = IO(new Bundle {
    // Prediction interface (IF stage) - combinational lookup
    val pc              = Input(UInt(Parameters.AddrWidth))
    val predicted_pc    = Output(UInt(Parameters.AddrWidth))
    val predicted_taken = Output(Bool())

    // Update interface (ID stage) - registered update
    val update_valid  = Input(Bool())
    val update_pc     = Input(UInt(Parameters.AddrWidth))
    val update_target = Input(UInt(Parameters.AddrWidth))
    val update_taken  = Input(Bool()) // Whether branch was actually taken
  })

  // BTB entry structure
  val valid   = RegInit(VecInit(Seq.fill(entries)(false.B)))
  val tags    = Reg(Vec(entries, UInt(tagBits.W)))
  val targets = Reg(Vec(entries, UInt(Parameters.AddrBits.W)))

  // 2-bit saturating counters for direction prediction
  // States: 0=SNT, 1=WNT, 2=WT, 3=ST (predict taken when >= 2)
  val counters = RegInit(VecInit(Seq.fill(entries)(2.U(2.W)))) // Initialize to Weakly Taken

  // Index and tag extraction (index/tag bits computed from entry count)
  def getIndex(pc: UInt): UInt = pc(indexBits + 1, 2)
  def getTag(pc: UInt): UInt   = pc(Parameters.AddrBits - 1, indexBits + 2)

  // Prediction logic (combinational - available same cycle)
  val pred_index = getIndex(io.pc)
  val pred_tag   = getTag(io.pc)
  val hit        = valid(pred_index) && (tags(pred_index) === pred_tag)

  // Predict taken only if BTB hit AND counter indicates taken (>= 2)
  val predict_taken = hit && (counters(pred_index) >= 2.U)
  io.predicted_taken := predict_taken
  // Only redirect to target when predicting taken; otherwise fall through to pc+4
  io.predicted_pc := Mux(predict_taken, targets(pred_index), io.pc + 4.U)

  // Update logic (registered - takes effect next cycle)
  when(io.update_valid) {
    val upd_index = getIndex(io.update_pc)
    val upd_tag   = getTag(io.update_pc)
    val entry_hit = valid(upd_index) && (tags(upd_index) === upd_tag)

    when(io.update_taken) {
      // Branch taken: allocate/update entry, saturating increment counter
      valid(upd_index)   := true.B
      tags(upd_index)    := upd_tag
      targets(upd_index) := io.update_target
      // Saturating increment: cap at 3 (ST)
      when(entry_hit) {
        counters(upd_index) := Mux(counters(upd_index) === 3.U, 3.U, counters(upd_index) + 1.U)
      }.otherwise {
        // New entry: initialize to Weakly Taken (2)
        counters(upd_index) := 2.U
      }
    }.otherwise {
      // Branch not taken: decrement counter if entry exists
      // Invalidate entry when counter reaches 0 (Strongly Not Taken) to free slot
      when(entry_hit) {
        when(counters(upd_index) === 1.U) {
          // Counter will reach 0: invalidate entry instead of keeping dead weight
          valid(upd_index) := false.B
        }.elsewhen(counters(upd_index) > 1.U) {
          counters(upd_index) := counters(upd_index) - 1.U
        }
        // counter === 0.U: already at SNT, keep saturated (shouldn't happen if we invalidate at 0)
      }
    }
  }
}
