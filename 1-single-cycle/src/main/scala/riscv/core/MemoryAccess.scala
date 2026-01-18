// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import peripheral.RAMBundle
import riscv.Parameters

// Memory Access stage: handles load/store operations with proper byte/halfword/word alignment
//
// This module implements RV32I memory access operations:
// - Load operations (LB, LH, LW, LBU, LHU): extract and sign/zero-extend data
// - Store operations (SB, SH, SW): write with byte-level strobes
//
// Memory alignment:
// - Addresses are byte-addressable but memory is organized as 32-bit words
// - mem_address_index (bits 1:0) selects byte/halfword position within word
// - Byte stores use individual byte strobes for precise writes
// - Loads extract and extend data based on address alignment
class MemoryAccess extends Module {
  val io = IO(new Bundle() {
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val memory_read_enable  = Input(Bool())
    val memory_write_enable = Input(Bool())
    val funct3              = Input(UInt(3.W))

    val wb_memory_read_data = Output(UInt(Parameters.DataWidth))

    val memory_bundle = Flipped(new RAMBundle)
  })
  val mem_address_index = io.alu_result(log2Up(Parameters.WordSize) - 1, 0).asUInt

  io.memory_bundle.write_enable := false.B
  io.memory_bundle.write_data   := 0.U
  io.memory_bundle.address      := io.alu_result
  io.memory_bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  io.wb_memory_read_data        := 0.U

  // ============================================================
  // [CA25: Exercise 6] Load Data Extension - Sign and Zero Extension
  // ============================================================
  // Hint: Implement proper sign extension and zero extension for load operations
  //
  // RISC-V Load instruction types:
  // - LB (Load Byte): Load 8-bit value and sign-extend to 32 bits
  // - LBU (Load Byte Unsigned): Load 8-bit value and zero-extend to 32 bits
  // - LH (Load Halfword): Load 16-bit value and sign-extend to 32 bits
  // - LHU (Load Halfword Unsigned): Load 16-bit value and zero-extend to 32 bits
  // - LW (Load Word): Load full 32-bit value, no extension needed
  //
  // Sign extension: Replicate the sign bit (MSB) to fill upper bits
  //   Example: LB loads 0xFF → sign-extended to 0xFFFFFFFF
  // Zero extension: Fill upper bits with zeros
  //   Example: LBU loads 0xFF → zero-extended to 0x000000FF
  when(io.memory_read_enable) {
    // Optimized load logic: extract bytes/halfwords based on address alignment
    val data  = io.memory_bundle.read_data
    val bytes = Wire(Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
    for (i <- 0 until Parameters.WordSize) {
      bytes(i) := data((i + 1) * Parameters.ByteBits - 1, i * Parameters.ByteBits)
    }
    // Select byte based on lower 2 address bits (mem_address_index)
    val byte = bytes(mem_address_index)
    // Select halfword based on bit 1 of address (word-aligned halfwords)
    val half = Mux(mem_address_index(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))

    // TODO: Complete sign/zero extension for load operations
    // Hint:
    // - Use Fill to replicate a bit multiple times
    // - For sign extension: Fill with the sign bit (MSB)
    // - For zero extension: Fill with zeros
    // - Use Cat to concatenate extension bits with loaded data
    io.wb_memory_read_data := MuxLookup(io.funct3, 0.U)(
      Seq(
        // TODO: Complete LB (sign-extend byte)
        // Hint: Replicate sign bit, then concatenate with byte
        InstructionsTypeL.lb  -> Cat(Fill(24, byte(7)), byte),

        // TODO: Complete LBU (zero-extend byte)
        // Hint: Fill upper bits with zero, then concatenate with byte
        InstructionsTypeL.lbu -> Cat(0.U(24.W), byte),

        // TODO: Complete LH (sign-extend halfword)
        // Hint: Replicate sign bit, then concatenate with halfword
        InstructionsTypeL.lh  -> Cat(Fill(16, half(15)), half),

        // TODO: Complete LHU (zero-extend halfword)
        // Hint: Fill upper bits with zero, then concatenate with halfword
        InstructionsTypeL.lhu -> Cat(0.U(16.W), half),

        // LW: Load full word, no extension needed (completed example)
        InstructionsTypeL.lw  -> data
      )
    )
  // ============================================================
  // [CA25: Exercise 7] Store Data Alignment - Byte Strobes and Shifting
  // ============================================================
  // Hint: Implement proper data alignment and byte strobes for store operations
  //
  // RISC-V Store instruction types:
  // - SB (Store Byte): Write 8-bit value to memory at byte-aligned address
  // - SH (Store Halfword): Write 16-bit value to memory at halfword-aligned address
  // - SW (Store Word): Write 32-bit value to memory at word-aligned address
  //
  // Key concepts:
  // 1. Byte strobes: Control which bytes in a 32-bit word are written
  //    - SB: 1 strobe active (at mem_address_index position)
  //    - SH: 2 strobes active (based on address bit 1)
  //    - SW: All 4 strobes active
  // 2. Data shifting: Align data to correct byte position in 32-bit word
  //    - mem_address_index (bits 1:0) indicates byte position
  //    - Left shift by (mem_address_index * 8) bits for byte operations
  //    - Left shift by 16 bits for upper halfword
  //
  // Examples:
  // - SB to address 0x1002 (index=2): data[7:0] → byte 2, strobe[2]=1
  // - SH to address 0x1002 (index=2): data[15:0] → bytes 2-3, strobes[2:3]=1
  }.elsewhen(io.memory_write_enable) {
    io.memory_bundle.write_enable := true.B
    io.memory_bundle.address      := io.alu_result

    val data = io.reg2_data
    // Optimized store logic: reduce combinational depth by simplifying shift operations
    // mem_address_index is already computed from address alignment (bits 1:0)
    val strobeInit   = VecInit(Seq.fill(Parameters.WordSize)(false.B))
    val defaultData  = 0.U(Parameters.DataWidth)
    val writeStrobes = WireInit(strobeInit)
    val writeData    = WireDefault(defaultData)

    switch(io.funct3) {
      is(InstructionsTypeS.sb) {
        // TODO: Complete store byte logic
        // Hint:
        // 1. Enable single byte strobe at appropriate position
        // 2. Shift byte data to correct position based on address
        writeStrobes(mem_address_index) := true.B
        writeData := data(7, 0) << (mem_address_index << 3)
      }
      is(InstructionsTypeS.sh) {
        // TODO: Complete store halfword logic
        // Hint: Check address to determine lower/upper halfword position
        when(mem_address_index(1) === 0.U) {
          // Lower halfword (bytes 0-1)
          // TODO: Enable strobes for lower two bytes, no shifting needed
          writeStrobes(0) := true.B
          writeStrobes(1) := true.B
          writeData := data(15, 0)
        }.otherwise {
          // Upper halfword (bytes 2-3)
          // TODO: Enable strobes for upper two bytes, apply appropriate shift
          writeStrobes(2) := true.B
          writeStrobes(3) := true.B
          writeData := data(15, 0) << 16
        }
      }
      is(InstructionsTypeS.sw) {
        // Store word: enable all byte strobes, no shifting needed (completed example)
        writeStrobes := VecInit(Seq.fill(Parameters.WordSize)(true.B))
        writeData    := data
      }
    }
    io.memory_bundle.write_data   := writeData
    io.memory_bundle.write_strobe := writeStrobes
  }
}
