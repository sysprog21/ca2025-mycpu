// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// Write Back stage: selects final result to write to register file
//
// This is the final stage of the processor pipeline, responsible for multiplexing
// the appropriate data source to be written back to the register file:
// - ALU result (default): Arithmetic/logical operation output
// - Memory data: Load instruction result
// - Next instruction address (PC+4): Return address for JAL/JALR instructions
//
// The regs_write_source signal (from Decode stage) determines which source is selected.
class WriteBack extends Module {
  val io = IO(new Bundle() {
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val memory_read_data    = Input(UInt(Parameters.DataWidth))
    val regs_write_source   = Input(UInt(2.W))
    val regs_write_data     = Output(UInt(Parameters.DataWidth))
  })

  // ============================================================
  // [CA25: Exercise 8] WriteBack Source Selection
  // ============================================================
  // Hint: Select the appropriate write-back data source based on instruction type
  //
  // WriteBack sources:
  // - ALU result (default): Used by arithmetic/logical/branch/jump instructions
  // - Memory read data: Used by load instructions (LB, LH, LW, LBU, LHU)
  // - Next instruction address (PC+4): Used by JAL/JALR for return address
  //
  // The control signal regs_write_source (from Decode stage) selects:
  // - RegWriteSource.ALUResult (0): Default, use ALU computation result
  // - RegWriteSource.Memory (1): Load instruction, use memory read data
  // - RegWriteSource.NextInstructionAddress (2): JAL/JALR, save return address
  //
  // TODO: Complete MuxLookup to multiplex writeback sources
  // Hint: Specify default value and cases for each source type
  io.regs_write_data := MuxLookup(io.regs_write_source, io.alu_result)(
    Seq(
      RegWriteSource.Memory                 -> io.memory_read_data,
      RegWriteSource.NextInstructionAddress -> (io.instruction_address + 4.U)
    )
  )
}
