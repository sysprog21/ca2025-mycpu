// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.peripheral

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

// Harvard Architecture Memory: separate instruction and data memories
// This design uses dual-port block RAMs (synthesizable to FPGA)
// and supports JIT self-modifying code by synchronizing writes to both memories
class Memory(capacity: Int) extends Module {
  val io = IO(new Bundle {
    val bundle = new RAMBundle

    val instruction         = Output(UInt(Parameters.DataWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))

    val debug_read_address = Input(UInt(Parameters.AddrWidth))
    val debug_read_data    = Output(UInt(Parameters.DataWidth))
  })

  // Harvard Architecture: Separate instruction and data memories
  // - imem: Instruction memory (1 read port for instruction fetch)
  // - dmem: Data memory (1 read + 1 write port for load/store operations)
  // Both memories share the same address space to support JIT self-modifying code
  // Trade-off: Consumes 2× BRAM capacity (imem + dmem) vs. unified memory design
  val imem = SyncReadMem(capacity, Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
  val dmem = SyncReadMem(capacity, Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))

  val max_word_address = (capacity - 1).U

  // Memory writes: address truncated to word index (>>2), misaligned accesses write to aligned word
  // Out-of-bounds writes are silently dropped (no error signaling)
  // JIT support: Writes update both imem and dmem to enable self-modifying code execution
  // JIT caveat: Self-modifying code must not execute from the same word in the cycle it is written
  //             (read-during-write semantics are tool-dependent for SyncReadMem)
  when(io.bundle.write_enable) {
    val write_data_vec = Wire(Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
    for (i <- 0 until Parameters.WordSize) {
      write_data_vec(i) := io.bundle.write_data((i + 1) * Parameters.ByteBits - 1, i * Parameters.ByteBits)
    }
    val write_word_addr = (io.bundle.address >> 2.U).asUInt
    when(write_word_addr <= max_word_address) {
      // Write to both memories to support JIT (SW to code buffer, then execute via IF)
      dmem.write(write_word_addr, write_data_vec, io.bundle.write_strobe)
      imem.write(write_word_addr, write_data_vec, io.bundle.write_strobe)
    }
  }

  // Memory reads: address truncated to word index (>>2), misaligned accesses read from aligned word
  // Out-of-bounds reads return data from address 0 (no error signaling)
  //
  // Harvard Architecture Port Allocation (FPGA dual-port block RAM compliant):
  //
  // dmem (Data Memory): 1 read + 1 write = 2 ports ✓
  //   - Read:  Data load operations (LW via io.bundle.read_data)
  //   - Write: Data store operations (SW) + JIT synchronization
  //
  // imem (Instruction Memory): 2 reads + 1 write = 3 ports
  //   - Read 1: Instruction fetch (IF via io.instruction)
  //   - Read 2: Debug reads (via io.debug_read_data)
  //   - Write:  JIT synchronization (writes mirrored from dmem)
  //
  // Synthesis note: dmem maps to true dual-port block RAM. imem's 3 ports
  // will be emulated by FPGA tools (replicated BRAMs or distributed memory).
  // This is acceptable for this minimal educational design where:
  //   1. imem is read-only during normal operation (writes only for JIT)
  //   2. Debug reads happen when CPU is halted (low frequency)
  //   3. Total memory size is small (overhead is acceptable)
  //
  // For production: add debug_enable signal to multiplex imem reads, reducing to 2 ports.

  // Compute word addresses from byte addresses (factor out repeated calculations)
  val read_word_raw  = (io.bundle.address >> 2.U).asUInt
  val debug_word_raw = (io.debug_read_address >> 2.U).asUInt
  val inst_word_raw  = (io.instruction_address >> 2.U).asUInt

  // Bound-check addresses: out-of-bounds reads alias to word 0
  val read_word_addr  = Mux(read_word_raw <= max_word_address, read_word_raw, 0.U)
  val debug_word_addr = Mux(debug_word_raw <= max_word_address, debug_word_raw, 0.U)
  val inst_word_addr  = Mux(inst_word_raw <= max_word_address, inst_word_raw, 0.U)

  // Harvard architecture read ports
  // - Instruction fetch from imem (primary instruction path)
  // - Data loads from dmem (primary data path)
  // - Debug reads from imem (imem and dmem kept identical via synchronized writes)
  io.instruction      := imem.read(inst_word_addr, true.B).asUInt
  io.bundle.read_data := dmem.read(read_word_addr, true.B).asUInt
  io.debug_read_data  := imem.read(debug_word_addr, true.B).asUInt
}
