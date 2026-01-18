// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import chisel3.util._
import riscv.Parameters

class RAMBundle extends Bundle {
  val address      = Input(UInt(Parameters.AddrWidth))
  val write_data   = Input(UInt(Parameters.DataWidth))
  val write_enable = Input(Bool())
  val write_strobe = Input(Vec(Parameters.WordSize, Bool()))
  val read_data    = Output(UInt(Parameters.DataWidth))
}
// The purpose of this module is to help the synthesis tool recognize
// our memory as a Block RAM template
class BlockRAM(capacity: Int) extends Module {
  val io = IO(new Bundle {
    val read_address  = Input(UInt(Parameters.AddrWidth))
    val write_address = Input(UInt(Parameters.AddrWidth))
    val write_data    = Input(UInt(Parameters.DataWidth))
    val write_enable  = Input(Bool())
    val write_strobe  = Input(Vec(Parameters.WordSize, Bool()))

    val debug_read_address = Input(UInt(Parameters.AddrWidth))

    val read_data       = Output(UInt(Parameters.DataWidth))
    val debug_read_data = Output(UInt(Parameters.DataWidth))
  })
  val mem = SyncReadMem(capacity, Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
  when(io.write_enable) {
    val write_data_vec = Wire(Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
    for (i <- 0 until Parameters.WordSize) {
      write_data_vec(i) := io.write_data((i + 1) * Parameters.ByteBits - 1, i * Parameters.ByteBits)
    }
    mem.write((io.write_address >> 2.U).asUInt, write_data_vec, io.write_strobe)
  }
  io.read_data       := mem.read((io.read_address >> 2.U).asUInt, true.B).asUInt
  io.debug_read_data := mem.read((io.debug_read_address >> 2.U).asUInt, true.B).asUInt
}

// Memory module: unified instruction and data memory with bounds checking
//
// Features:
// - Synchronous read memory (1-cycle latency)
// - Byte-addressable with byte-level write strobes
// - Separate ports for instruction fetch, data access, and debug
// - Memory bounds validation prevents out-of-bounds corruption
//
// Address mapping:
// - Byte addresses divided by 4 (>> 2) to get word addresses
// - capacity parameter specifies number of 32-bit words
// - Out-of-bounds addresses clamped to 0 for safe reads
class Memory(capacity: Int) extends Module {
  val io = IO(new Bundle {
    val bundle = new RAMBundle

    val instruction         = Output(UInt(Parameters.DataWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))

    val debug_read_address = Input(UInt(Parameters.AddrWidth))
    val debug_read_data    = Output(UInt(Parameters.DataWidth))
  })

  val mem = SyncReadMem(capacity, Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))

  // Memory bounds checking: capacity is in words, addresses are word-aligned (>> 2)
  val max_word_address = (capacity - 1).U

  when(io.bundle.write_enable) {
    val write_data_vec = Wire(Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
    for (i <- 0 until Parameters.WordSize) {
      write_data_vec(i) := io.bundle.write_data((i + 1) * Parameters.ByteBits - 1, i * Parameters.ByteBits)
    }
    val write_word_addr = (io.bundle.address >> 2.U).asUInt
    // Only write if address is within bounds
    when(write_word_addr <= max_word_address) {
      mem.write(write_word_addr, write_data_vec, io.bundle.write_strobe)
    }
  }

  // Clamp read addresses to valid range to prevent out-of-bounds access
  val read_word_addr = Mux(
    (io.bundle.address >> 2.U).asUInt <= max_word_address,
    (io.bundle.address >> 2.U).asUInt,
    0.U
  )
  val debug_word_addr = Mux(
    (io.debug_read_address >> 2.U).asUInt <= max_word_address,
    (io.debug_read_address >> 2.U).asUInt,
    0.U
  )
  val inst_word_addr = Mux(
    (io.instruction_address >> 2.U).asUInt <= max_word_address,
    (io.instruction_address >> 2.U).asUInt,
    0.U
  )

  io.bundle.read_data := mem.read(read_word_addr, true.B).asUInt
  io.debug_read_data  := mem.read(debug_word_addr, true.B).asUInt
  io.instruction      := mem.read(inst_word_addr, true.B).asUInt
}
