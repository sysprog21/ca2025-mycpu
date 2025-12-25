// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * Indirect Branch Target Buffer for JALR Target Prediction
 *
 * Purpose:
 * - Regular BTB predicts branch/jump targets based on PC alone
 * - RAS predicts return addresses for JALR rs1=ra patterns
 * - IndirectBTB fills the gap: JALR with computed targets (function pointers, vtables)
 *
 * Key Insight:
 * JALR target = rs1 + imm. Same PC can produce different targets based on rs1 value.
 * IndirectBTB tracks (PC, rs1_hash) → target mappings to predict computed jumps.
 *
 * Use Cases:
 * - Function pointers: JALR rd=ra, rs1=t0 (target varies based on pointer value)
 * - Virtual method calls: JALR where rs1 contains vtable entry
 * - Switch statements compiled to jump tables
 * - Indirect calls through PLT/GOT
 *
 * Architecture:
 * - 8-entry fully-associative table (small but effective for hot indirect jumps)
 * - Each entry: valid, pc_tag (upper PC bits), rs1_hash (8-bit hash), target, age
 * - Lookup: Match PC AND rs1_hash for high-confidence prediction
 * - Replacement: LRU-approximated via age counter
 *
 * Integration Priority (in InstructionFetch):
 * 1. RAS prediction for returns (JALR rs1=link, rd=x0)
 * 2. IndirectBTB prediction for other JALR (when RAS doesn't match)
 * 3. Regular BTB (fallback, less accurate for JALR)
 *
 * Performance Impact:
 * - Reduces indirect jump misprediction penalty for repetitive patterns
 * - Complements RAS (handles non-return JALR)
 * - Small area overhead (8 x ~72 bits = 576 bits storage)
 *
 * Bergamot Reference:
 * This addresses a gap noted in Bergamot documentation where indirect branch
 * prediction was identified as a limitation. The design tracks PC + rs1_hash
 * to capture correlation between register state and jump targets.
 *
 * @param entries Number of IndirectBTB entries (default 8)
 */
class IndirectBTB(entries: Int = 8) extends Module {
  require(isPow2(entries) && entries >= 4, "IndirectBTB entries must be power of 2 and >= 4")

  val indexBits = log2Ceil(entries)
  val pcTagBits = Parameters.AddrBits - 2 // Full PC except 2 LSBs (word alignment)
  val hashBits  = 8                       // rs1 value hash width

  val io = IO(new Bundle {
    // Prediction interface (IF stage) - combinational lookup
    // Note: rs1_hash not used for prediction lookup (not available in IF stage).
    // Prediction uses PC-only matching - less accurate but available early.
    // Multiple entries with same PC will return most recently used target.
    val pc               = Input(UInt(Parameters.AddrWidth))
    val predicted_target = Output(UInt(Parameters.AddrWidth))
    val hit              = Output(Bool())

    // Update interface (ID stage) - registered update
    // Full (PC, rs1_hash) used for update to disambiguate different callers.
    val update_valid    = Input(Bool())
    val update_pc       = Input(UInt(Parameters.AddrWidth))
    val update_rs1_hash = Input(UInt(hashBits.W))
    val update_target   = Input(UInt(Parameters.AddrWidth))
  })

  // Entry structure: valid + pc_tag + rs1_hash + target + age
  val valid     = RegInit(VecInit(Seq.fill(entries)(false.B)))
  val pc_tags   = Reg(Vec(entries, UInt(pcTagBits.W)))
  val rs1_hashs = Reg(Vec(entries, UInt(hashBits.W)))
  val targets   = Reg(Vec(entries, UInt(Parameters.AddrBits.W)))
  val ages      = RegInit(VecInit(Seq.fill(entries)(0.U(log2Ceil(entries).W))))

  // Tag extraction (PC[31:2] for full address match)
  def getPcTag(pc: UInt): UInt = pc(Parameters.AddrBits - 1, 2)

  // Prediction logic - PC-only associative search
  // Note: rs1_hash not used for lookup (not available in IF stage).
  // If multiple entries match same PC (different rs1_hash), use lowest age (MRU).
  val pred_pc_tag = getPcTag(io.pc)

  // Find all entries matching PC (ignoring rs1_hash)
  val pc_hit_vec = VecInit((0 until entries).map { i =>
    valid(i) && (pc_tags(i) === pred_pc_tag)
  })
  val hit_any = pc_hit_vec.asUInt.orR

  // Select entry with lowest age (most recently used) among PC matches
  // This gives best prediction when same JALR has multiple targets
  val hit_ages = VecInit((0 until entries).map { i =>
    Mux(pc_hit_vec(i), ages(i), (entries - 1).U) // Non-matching entries get max age
  })
  val min_age   = hit_ages.reduce((a, b) => Mux(a < b, a, b))
  val mru_vec   = VecInit((0 until entries).map(i => pc_hit_vec(i) && (ages(i) === min_age)))
  val hit_index = PriorityEncoder(mru_vec.asUInt)

  io.hit              := hit_any
  io.predicted_target := Mux(hit_any, targets(hit_index), 0.U)

  // Update age counters on hit (move-to-front approximation)
  // Skip if update is also happening this cycle (update path handles age management)
  when(hit_any && !io.update_valid) {
    // Reset age of hit entry, increment others
    for (i <- 0 until entries) {
      when(mru_vec(i)) {
        ages(i) := 0.U
      }.elsewhen(valid(i) && ages(i) < (entries - 1).U) {
        ages(i) := ages(i) + 1.U
      }
    }
  }

  // Update logic - find entry to replace or update
  when(io.update_valid) {
    val upd_pc_tag = getPcTag(io.update_pc)

    // Check if entry already exists (update case)
    val existing_vec = VecInit((0 until entries).map { i =>
      valid(i) && (pc_tags(i) === upd_pc_tag) && (rs1_hashs(i) === io.update_rs1_hash)
    })
    val existing_any   = existing_vec.asUInt.orR
    val existing_index = PriorityEncoder(existing_vec.asUInt)

    // Find invalid entry for new allocation
    val invalid_vec   = VecInit((0 until entries).map(i => !valid(i)))
    val invalid_any   = invalid_vec.asUInt.orR
    val invalid_index = PriorityEncoder(invalid_vec.asUInt)

    // Find oldest entry for eviction (LRU approximation)
    val oldest_age   = ages.reduce((a, b) => Mux(a > b, a, b))
    val oldest_vec   = VecInit((0 until entries).map(i => valid(i) && (ages(i) === oldest_age)))
    val oldest_index = PriorityEncoder(oldest_vec.asUInt)

    // Select target index: existing > invalid > oldest
    val target_index = Mux(
      existing_any,
      existing_index,
      Mux(invalid_any, invalid_index, oldest_index)
    )

    // Update entry
    valid(target_index)     := true.B
    pc_tags(target_index)   := upd_pc_tag
    rs1_hashs(target_index) := io.update_rs1_hash
    targets(target_index)   := io.update_target

    // Reset age of updated entry, age others
    for (i <- 0 until entries) {
      when(i.U === target_index) {
        ages(i) := 0.U
      }.elsewhen(valid(i) && ages(i) < (entries - 1).U) {
        ages(i) := ages(i) + 1.U
      }
    }
  }
}

/**
 * Hash function for rs1 value compression
 *
 * Reduces 32-bit register value to 8-bit hash for storage efficiency.
 * XOR-fold provides reasonable distribution for pointer values.
 */
object IndirectBTBHash {
  def apply(value: UInt): UInt = {
    // XOR fold 32 bits to 8 bits: bytes XORed together
    val b0 = value(7, 0)
    val b1 = value(15, 8)
    val b2 = value(23, 16)
    val b3 = value(31, 24)
    b0 ^ b1 ^ b2 ^ b3
  }
}
