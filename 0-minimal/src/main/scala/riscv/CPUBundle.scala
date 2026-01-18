// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import riscv.peripheral.RAMBundle

class CPUBundle extends Bundle {
  val instruction_address = Output(UInt(Parameters.AddrWidth))
  val instruction         = Input(UInt(Parameters.DataWidth))
  val memory_bundle       = Flipped(new RAMBundle)
  val instruction_valid   = Input(Bool())
  val debug_read_address  = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
  val debug_read_data     = Output(UInt(Parameters.DataWidth))
}
