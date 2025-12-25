// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import bus.AXI4LiteChannels
import bus.AXI4LiteMasterBundle
import chisel3._
import riscv.Parameters

class CPUBundle extends Bundle {
  // Instruction fetch interface
  val instruction_address = Output(UInt(Parameters.AddrWidth))
  val instruction         = Input(UInt(Parameters.InstructionWidth))
  val instruction_valid   = Input(Bool())

  // Memory/Bus interface
  val memory_bundle = new BusBundle
  val device_select = Output(UInt(Parameters.SlaveDeviceCountBits.W))

  // AXI4-Lite interface (for top-level integration)
  val axi4_channels = new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits)

  // Interrupt
  val interrupt_flag = Input(UInt(Parameters.InterruptFlagWidth))

  // Debug interfaces
  val debug_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
  val debug_read_data    = Output(UInt(Parameters.DataWidth))

  val csr_debug_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))
  val csr_debug_read_data    = Output(UInt(Parameters.DataWidth))

  // Bus address and write strobes for BusSwitch/arbiter AXI4-Lite routing
  val bus_address            = Output(UInt(Parameters.AddrWidth))
  val debug_bus_write_enable = Output(Bool())
  val debug_bus_write_data   = Output(UInt(Parameters.DataWidth))
}
