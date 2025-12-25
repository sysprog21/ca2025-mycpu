// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import chisel3.util._
import riscv.Parameters

// Timer peripheral: memory-mapped timer with configurable interrupt generation
//
// Features:
// - 32-bit auto-reloading counter
// - Configurable interrupt threshold (limit register)
// - Enable/disable control for interrupt generation
// - Interrupts asserted when (count >= limit) and enabled
//
// Memory-mapped registers:
// - 0x4: limit - Interrupt threshold value (default: 100000000)
// - 0x8: enable - Interrupt enable flag (default: true)
//
// Behavior:
// - Counter increments every cycle
// - Counter resets to 0 when reaching limit
// - Interrupt signal remains high while (count >= limit) and enabled
class Timer extends Module {
  val io = IO(new Bundle {
    val bundle           = new RAMBundle
    val signal_interrupt = Output(Bool())

    val debug_limit   = Output(UInt(Parameters.DataWidth))
    val debug_enabled = Output(Bool())
  })

  val count = RegInit(0.U(32.W))
  val limit = RegInit(Parameters.TimerDefaultLimit.U(32.W)) // Default: 100M cycles (~1s at 100MHz)
  io.debug_limit := limit
  val enabled = RegInit(true.B)
  io.debug_enabled := enabled

  // Memory-mapped register addresses
  object DataAddr {
    val enable = 0x8.U
    val limit  = 0x4.U
  }

  io.signal_interrupt := enabled && (count >= limit)
  count               := Mux(count >= limit, 0.U, count + 1.U)

  io.bundle.read_data := MuxLookup(io.bundle.address, 0.U)(
    IndexedSeq(
      DataAddr.enable -> enabled,
      DataAddr.limit  -> limit,
    )
  )

  when(io.bundle.write_enable) {
    when(io.bundle.address === DataAddr.enable) {
      enabled := io.bundle.write_data
    }.elsewhen(io.bundle.address === DataAddr.limit) {
      limit := io.bundle.write_data
    }
  }
}
