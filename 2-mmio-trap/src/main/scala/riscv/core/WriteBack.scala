// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// Write Back stage: selects final result to write to register file
//
// Extended from single-cycle design with CSR support.
// This is the final stage of the processor pipeline, responsible for multiplexing
// the appropriate data source to be written back to the register file:
// - ALU result (default): Arithmetic/logical operation output
// - Memory data: Load instruction result
// - CSR data: CSR read instruction result (new in 2-mmio-trap)
// - Next instruction address (PC+4): Return address for JAL/JALR instructions
//
// The regs_write_source signal (from Decode stage) determines which source is selected.
class WriteBack extends Module {
  val io = IO(new Bundle() {
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val memory_read_data    = Input(UInt(Parameters.DataWidth))
    val csr_read_data       = Input(UInt(Parameters.DataWidth))
    val regs_write_source   = Input(UInt(2.W))
    val regs_write_data     = Output(UInt(Parameters.DataWidth))
  })

  // ============================================================
  // [CA25: Exercise 12] WriteBack Source Selection with CSR Support
  // ============================================================
  // Hint: Select the appropriate write-back data source based on instruction type
  //
  // WriteBack sources (extended from single-cycle design):
  // - ALU result (default): Used by arithmetic/logical/branch/jump instructions
  // - Memory read data: Used by load instructions (LB, LH, LW, LBU, LHU)
  // - CSR read data: Used by CSR read instructions (CSRRW, CSRRS, CSRRC) **NEW**
  // - Next instruction address (PC+4): Used by JAL/JALR for return address
  //
  // The control signal regs_write_source (from Decode stage) selects:
  // - RegWriteSource.ALUResult (0): Default, use ALU computation result
  // - RegWriteSource.Memory (1): Load instruction, use memory read data
  // - RegWriteSource.CSR (2): CSR instruction, use CSR read data **NEW**
  // - RegWriteSource.NextInstructionAddress (3): JAL/JALR, save return address
  //
  // Comparison with 1-single-cycle Exercise 8:
  // - Single-cycle: 3 sources (ALU, Memory, PC+4)
  // - MMIO-trap: 4 sources (ALU, Memory, CSR, PC+4) **Added CSR support**
  //
  // TODO: Complete MuxLookup to multiplex writeback sources with CSR support
  // Hint: Specify default value and cases for each source type, including CSR
  io.regs_write_data := MuxLookup(io.regs_write_source, io.alu_result)(
    Seq(
      RegWriteSource.Memory                 -> io.memory_read_data,
      RegWriteSource.CSR                    -> io.csr_read_data,
      RegWriteSource.NextInstructionAddress -> (io.instruction_address + 4.U)
    )
  )
}
