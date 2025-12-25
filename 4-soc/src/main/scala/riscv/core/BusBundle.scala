// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

class BusBundle extends Bundle {
  val address             = Output(UInt(Parameters.AddrWidth))
  val read                = Output(Bool())
  val read_data           = Input(UInt(Parameters.DataWidth))
  val read_valid          = Input(Bool())
  val write               = Output(Bool())
  val write_data          = Output(UInt(Parameters.DataWidth))
  val write_strobe        = Output(Vec(Parameters.WordSize, Bool()))
  val write_valid         = Input(Bool())
  val write_data_accepted = Input(Bool()) // Posted-write: data accepted, BRESP pending
  val busy                = Input(Bool())
  val request             = Output(Bool())
  val granted             = Input(Bool())
}
