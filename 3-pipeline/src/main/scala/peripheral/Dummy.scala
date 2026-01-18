// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import riscv.Parameters

// A dummy master that never initiates reads or writes
class Dummy extends Module {
  val io = IO(new Bundle {
    val bundle = Flipped(new RAMBundle)
  })
  io.bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  io.bundle.write_data   := 0.U
  io.bundle.write_enable := false.B
  io.bundle.address      := 0.U
}
