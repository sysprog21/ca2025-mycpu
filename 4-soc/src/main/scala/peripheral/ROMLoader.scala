// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import riscv.Parameters

class ROMLoader(capacity: Int) extends Module {
  val io = IO(new Bundle {
    val bundle = Flipped(new RAMBundle)

    val rom_address = Output(UInt(Parameters.AddrWidth))
    val rom_data    = Input(UInt(Parameters.InstructionWidth))

    val load_address  = Input(UInt(Parameters.AddrWidth))
    val load_finished = Output(Bool())
  })

  val address = RegInit(0.U(32.W))
  val valid   = RegInit(false.B)

  io.bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  io.bundle.address      := 0.U
  io.bundle.write_data   := 0.U
  io.bundle.write_enable := false.B
  when(address <= (capacity - 1).U) {
    io.bundle.write_enable := true.B
    io.bundle.write_data   := io.rom_data
    io.bundle.address      := (address << 2.U).asUInt + io.load_address
    io.bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(true.B))
    address                := address + 1.U
    when(address === (capacity - 1).U) {
      valid := true.B
    }
  }
  io.load_finished := valid
  // Convert word index to byte address that InstructionROM expects
  // InstructionROM converts byte addresses starting at EntryAddress (0x1000) to word indices
  io.rom_address := (address << 2.U) + io.load_address
}
