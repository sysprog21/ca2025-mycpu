// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_stall

import chisel3._
import chisel3.util._
import peripheral.RAMBundle
import riscv.Parameters

class MemoryAccess extends Module {
  val io = IO(new Bundle() {
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val memory_read_enable  = Input(Bool())
    val memory_write_enable = Input(Bool())
    val funct3              = Input(UInt(3.W))
    val regs_write_source   = Input(UInt(2.W))
    val csr_read_data       = Input(UInt(Parameters.DataWidth))

    val wb_memory_read_data = Output(UInt(Parameters.DataWidth))

    val bundle = Flipped(new RAMBundle)
  })
  val mem_address_index = io.alu_result(log2Up(Parameters.WordSize) - 1, 0).asUInt

  io.bundle.write_enable := false.B
  io.bundle.write_data   := 0.U
  io.bundle.address      := io.alu_result
  io.bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  io.wb_memory_read_data := 0.U

  when(io.memory_read_enable) {
    val data  = io.bundle.read_data
    val bytes = Wire(Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
    for (i <- 0 until Parameters.WordSize) {
      bytes(i) := data((i + 1) * Parameters.ByteBits - 1, i * Parameters.ByteBits)
    }
    val byte = bytes(mem_address_index)
    val half = Mux(mem_address_index(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))

    io.wb_memory_read_data := MuxLookup(io.funct3, 0.U)(
      Seq(
        InstructionsTypeL.lb  -> Cat(Fill(24, byte(7)), byte),
        InstructionsTypeL.lbu -> Cat(Fill(24, 0.U), byte),
        InstructionsTypeL.lh  -> Cat(Fill(16, half(15)), half),
        InstructionsTypeL.lhu -> Cat(Fill(16, 0.U), half),
        InstructionsTypeL.lw  -> data
      )
    )
  }.elsewhen(io.memory_write_enable) {
    io.bundle.write_enable := true.B
    val data         = io.reg2_data
    val strobeInit   = VecInit(Seq.fill(Parameters.WordSize)(false.B))
    val defaultData  = 0.U(Parameters.DataWidth)
    val writeStrobes = WireInit(strobeInit)
    val writeData    = WireDefault(defaultData)
    switch(io.funct3) {
      is(InstructionsTypeS.sb) {
        writeStrobes(mem_address_index) := true.B
        writeData                       := data(7, 0) << (mem_address_index << 3)
      }
      is(InstructionsTypeS.sh) {
        when(mem_address_index(1) === 0.U) {
          writeStrobes(0) := true.B
          writeStrobes(1) := true.B
          writeData       := data(15, 0)
        }.otherwise {
          writeStrobes(2) := true.B
          writeStrobes(3) := true.B
          writeData       := data(15, 0) << 16
        }
      }
      is(InstructionsTypeS.sw) {
        writeStrobes := VecInit(Seq.fill(Parameters.WordSize)(true.B))
        writeData    := data
      }
    }
    io.bundle.write_data   := writeData
    io.bundle.write_strobe := writeStrobes
  }
}
