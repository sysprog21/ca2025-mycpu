// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

/**
 * Pipeline Register with stall and flush control
 *
 * Standard pipeline register for holding data between pipeline stages.
 * Supports stalling (hold current value) and flushing (reset to default).
 *
 * Control signal priority (high to low):
 *   1. flush - Reset to default (highest priority)
 *   2. stall - Hold current value
 *   3. normal - Capture input
 *
 * Timing: One-cycle delay from input to output (registered).
 */
class PipelineRegister(width: Int = Parameters.DataBits, defaultValue: UInt = 0.U) extends Module {
  val io = IO(new Bundle {
    val stall = Input(Bool())
    val flush = Input(Bool())
    val in    = Input(UInt(width.W))
    val out   = Output(UInt(width.W))
  })

  val reg = RegInit(UInt(width.W), defaultValue)

  when(io.flush) {
    reg := defaultValue
  }.elsewhen(!io.stall) {
    reg := io.in
  }
  // When stalled: reg retains its value (no assignment needed)

  io.out := reg
}
