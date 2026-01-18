// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * Return Address Stack (RAS) for JALR Return Prediction
 *
 * Purpose:
 * - BTB can predict branch targets (PC-relative) but not JALR targets (register-computed)
 * - JALR with rs1=ra (x1) is typically a function return
 * - RAS predicts return addresses by tracking call/return patterns
 *
 * Call/Return Detection (RISC-V conventions):
 * - CALL: JAL with rd=x1 (ra) or rd=x5 (t0) → push PC+4 to RAS
 * - RETURN: JALR with rs1=x1 (ra) or rs1=x5 (t0), rd=x0 → pop from RAS
 * - Note: Co-routine pattern (JAL rd=x5) also supported
 *
 * Architecture:
 * - 4-entry circular stack (configurable)
 * - Push on CALL, Pop on RETURN
 * - Stack pointer wraps on overflow/underflow (graceful degradation)
 * - Combinational pop for same-cycle prediction
 *
 * Integration Points:
 * - IF stage: Provides predicted return address when JALR rs1=ra detected
 * - ID stage: Push on JAL rd=ra, correction on misprediction
 *
 * Performance Impact:
 * - High accuracy for regular call/return patterns (~90%+ in typical code)
 * - Reduces JALR return misprediction penalty from 1-2 cycles to 0 cycles
 * - Minimal area overhead (4 x 32-bit registers + control logic)
 *
 * @param depth Number of RAS entries (typically 4-8)
 */
class ReturnAddressStack(depth: Int = 4) extends Module {
  require(depth >= 2 && isPow2(depth), "RAS depth must be power of 2 and >= 2")

  val io = IO(new Bundle {
    // Push interface (from ID stage on JAL rd=ra)
    val push      = Input(Bool())
    val push_addr = Input(UInt(Parameters.AddrWidth))

    // Pop interface (from IF stage on JALR rs1=ra prediction)
    val pop = Input(Bool())

    // Prediction output (combinational)
    val predicted_addr = Output(UInt(Parameters.AddrWidth))
    val valid          = Output(Bool())

    // Correction interface (from ID stage on misprediction)
    // Allows restoring RAS state after speculative pops
    val restore       = Input(Bool())
    val restore_addr  = Input(UInt(Parameters.AddrWidth))
    val restore_valid = Input(Bool())
  })

  // Stack storage and pointer
  val stack = Reg(Vec(depth, UInt(Parameters.AddrWidth)))
  val sp    = RegInit(0.U(log2Ceil(depth + 1).W)) // 0 = empty, depth = full

  // Calculate top of stack index (sp - 1, with wrap)
  val tos_index = (sp - 1.U)(log2Ceil(depth) - 1, 0)

  // Combinational prediction (available same cycle as pop request)
  io.valid          := sp > 0.U
  io.predicted_addr := Mux(io.valid, stack(tos_index), 0.U)

  // Stack operations (priority: restore > push > pop)
  when(io.restore) {
    // Restore after misprediction - push the correct return address back
    when(io.restore_valid && sp < depth.U) {
      stack(sp(log2Ceil(depth) - 1, 0)) := io.restore_addr
      sp                                := sp + 1.U
    }
  }.elsewhen(io.push && io.pop) {
    // Simultaneous push and pop (rare but possible with tail calls)
    // Replace TOS with new address, sp unchanged
    stack(tos_index) := io.push_addr
  }.elsewhen(io.push) {
    // Push: store at sp, increment (with saturation at depth)
    when(sp < depth.U) {
      stack(sp(log2Ceil(depth) - 1, 0)) := io.push_addr
      sp                                := sp + 1.U
    }.otherwise {
      // Overflow: shift stack down, push at top (oldest entry lost)
      for (i <- 0 until depth - 1) {
        stack(i) := stack(i + 1)
      }
      stack(depth - 1) := io.push_addr
      // sp stays at depth
    }
  }.elsewhen(io.pop) {
    // Pop: decrement sp (with saturation at 0)
    when(sp > 0.U) {
      sp := sp - 1.U
    }
    // Underflow: sp stays at 0, prediction invalid
  }
}
