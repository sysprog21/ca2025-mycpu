// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.threestage

import chisel3._

class Control extends Module {
  val io = IO(new Bundle {
    val JumpFlag = Input(Bool())
    val Flush    = Output(Bool())
  })
  io.Flush := io.JumpFlag
  // lab3 end
}
