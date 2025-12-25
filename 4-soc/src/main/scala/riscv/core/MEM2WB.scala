// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

/**
 * MEM/WB Pipeline Register: Memory Access to Write Back boundary.
 *
 * Buffers the final data source selection inputs: ALU result, memory read data,
 * CSR read data, or PC+4 (for JAL/JALR link address). The WriteBack stage
 * selects among these based on regs_write_source.
 *
 * Key signals buffered:
 * - alu_result: Forwarded for non-memory ALU operations
 * - memory_read_data: Load result after byte/half extraction and sign-extension
 * - instruction_address: Used to compute PC+4 for JAL/JALR writeback
 * - regs_write_*: Register file write control signals
 *
 * Critical timing note: The inputs to this register come from MemoryAccess's
 * latched outputs (wb_*), not from ex2mem, to preserve correct values across
 * multi-cycle AXI transactions. See MemoryAccess.scala for details.
 */
class MEM2WB extends Module {
  val io = IO(new Bundle() {
    val stall               = Input(Bool())
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val regs_write_enable   = Input(Bool())
    val regs_write_source   = Input(UInt(2.W))
    val regs_write_address  = Input(UInt(Parameters.AddrWidth))
    val memory_read_data    = Input(UInt(Parameters.DataWidth))
    val csr_read_data       = Input(UInt(Parameters.DataWidth))

    val output_instruction_address = Output(UInt(Parameters.AddrWidth))
    val output_alu_result          = Output(UInt(Parameters.DataWidth))
    val output_regs_write_enable   = Output(Bool())
    val output_regs_write_source   = Output(UInt(2.W))
    val output_regs_write_address  = Output(UInt(Parameters.AddrWidth))
    val output_memory_read_data    = Output(UInt(Parameters.DataWidth))
    val output_csr_read_data       = Output(UInt(Parameters.DataWidth))
  })
  val stall = io.stall
  val flush = false.B

  val alu_result = Module(new PipelineRegister())
  alu_result.io.in     := io.alu_result
  alu_result.io.stall  := stall
  alu_result.io.flush  := flush
  io.output_alu_result := alu_result.io.out

  val memory_read_data = Module(new PipelineRegister())
  memory_read_data.io.in     := io.memory_read_data
  memory_read_data.io.stall  := stall
  memory_read_data.io.flush  := flush
  io.output_memory_read_data := memory_read_data.io.out

  val regs_write_enable = Module(new PipelineRegister(1))
  regs_write_enable.io.in     := io.regs_write_enable
  regs_write_enable.io.stall  := stall
  regs_write_enable.io.flush  := flush
  io.output_regs_write_enable := regs_write_enable.io.out

  val regs_write_source = Module(new PipelineRegister(2))
  regs_write_source.io.in     := io.regs_write_source
  regs_write_source.io.stall  := stall
  regs_write_source.io.flush  := flush
  io.output_regs_write_source := regs_write_source.io.out

  val regs_write_address = Module(new PipelineRegister(Parameters.PhysicalRegisterAddrBits))
  regs_write_address.io.in     := io.regs_write_address
  regs_write_address.io.stall  := stall
  regs_write_address.io.flush  := flush
  io.output_regs_write_address := regs_write_address.io.out

  val instruction_address = Module(new PipelineRegister(Parameters.InstructionBits))
  instruction_address.io.in     := io.instruction_address
  instruction_address.io.stall  := stall
  instruction_address.io.flush  := flush
  io.output_instruction_address := instruction_address.io.out

  val csr_read_data = Module(new PipelineRegister())
  csr_read_data.io.in     := io.csr_read_data
  csr_read_data.io.stall  := stall
  csr_read_data.io.flush  := flush
  io.output_csr_read_data := csr_read_data.io.out
}
