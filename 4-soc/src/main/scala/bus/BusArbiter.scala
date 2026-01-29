// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package bus

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * Bus arbiter for multi-master AXI4-Lite bus access.
 *
 * NOTE: This module is currently unused in 4-soc as the system operates with
 * a single bus master (CPU). It exists as infrastructure for future multi-master
 * configurations (e.g., DMA controller, debug interface, or additional cores).
 *
 * Implementation: Static priority arbitration where higher-numbered masters have
 * higher priority. Master 0 (CPU) is lowest priority, which would allow DMA or
 * debug access to preempt normal CPU bus transactions when needed.
 *
 * To enable multi-master operation:
 *   1. Set Parameters.MasterDeviceCount > 1
 *   2. Instantiate BusArbiter in Top.scala
 *   3. Connect arbiter between masters and BusSwitch
 *   4. Gate master bus signals through bus_granted
 */
class BusArbiter extends Module {
  val io = IO(new Bundle {
    val bus_request     = Input(Vec(Parameters.MasterDeviceCount, Bool()))
    val bus_granted     = Output(Vec(Parameters.MasterDeviceCount, Bool()))
    val ctrl_stall_flag = Output(Bool())
    val granted_index   = Output(UInt(log2Up(Parameters.MasterDeviceCount).W))
  })

  val active_grant  = RegInit(false.B)
  val current_grant = RegInit(0.U(log2Up(Parameters.MasterDeviceCount).W))

  val next_grant = Wire(UInt(log2Up(Parameters.MasterDeviceCount).W))
  next_grant := 0.U
  for (i <- 0 until Parameters.MasterDeviceCount) {
    when(io.bus_request(i.U)) {
      next_grant := i.U
    }
  }

  when(active_grant) {
    when(!io.bus_request(current_grant)) {
      active_grant := false.B
    }
  }.otherwise {
    when(io.bus_request.asUInt.orR) {
      active_grant  := true.B
      current_grant := next_grant
    }
  }

  for (i <- 0 until Parameters.MasterDeviceCount) {
    io.bus_granted(i.U) := active_grant && (current_grant === i.U)
  }
  io.ctrl_stall_flag := !active_grant
  io.granted_index   := current_grant
}
