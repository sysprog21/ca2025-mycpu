// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

/**
 * ID/EX Pipeline Register: Instruction Decode to Execute boundary.
 *
 * Buffers decoded instruction fields, register operands (with ID-stage forwarding
 * already applied), immediate values, and all control signals needed for the
 * Execute stage. This is the widest pipeline register due to the large number
 * of decoded fields.
 *
 * Key signals buffered:
 * - reg1_data, reg2_data: Register operands (already forwarded in ID stage)
 * - immediate: Sign-extended immediate for I/S/B/U/J formats
 * - regs_write_*: Destination register metadata for forwarding detection
 * - memory_*_enable: Load/store indicators for hazard detection
 * - csr_*: CSR instruction control signals
 *
 * Flush behavior: Inserts NOP bubble on data hazards (load-use, JAL/JALR)
 *                 to allow forwarding paths to stabilize.
 */
class ID2EX extends Module {
  val io = IO(new Bundle {
    val stall                  = Input(Bool())
    val flush                  = Input(Bool())
    val instruction            = Input(UInt(Parameters.InstructionWidth))
    val instruction_address    = Input(UInt(Parameters.AddrWidth))
    val regs_reg1_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val regs_reg2_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val regs_write_enable      = Input(Bool())
    val regs_write_address     = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val regs_write_source      = Input(UInt(2.W))
    val reg1_data              = Input(UInt(Parameters.DataWidth))
    val reg2_data              = Input(UInt(Parameters.DataWidth))
    val immediate              = Input(UInt(Parameters.DataWidth))
    val aluop1_source          = Input(UInt(1.W))
    val aluop2_source          = Input(UInt(1.W))
    val csr_write_enable       = Input(Bool())
    val csr_address            = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val memory_read_enable     = Input(Bool())
    val memory_write_enable    = Input(Bool())
    val csr_read_data          = Input(UInt(Parameters.DataWidth))

    val output_instruction            = Output(UInt(Parameters.DataWidth))
    val output_instruction_address    = Output(UInt(Parameters.AddrWidth))
    val output_regs_reg1_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val output_regs_reg2_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val output_regs_write_enable      = Output(Bool())
    val output_regs_write_address     = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val output_regs_write_source      = Output(UInt(2.W))
    val output_reg1_data              = Output(UInt(Parameters.DataWidth))
    val output_reg2_data              = Output(UInt(Parameters.DataWidth))
    val output_immediate              = Output(UInt(Parameters.DataWidth))
    val output_aluop1_source          = Output(UInt(1.W))
    val output_aluop2_source          = Output(UInt(1.W))
    val output_csr_write_enable       = Output(Bool())
    val output_csr_address            = Output(UInt(Parameters.CSRRegisterAddrWidth))
    val output_memory_read_enable     = Output(Bool())
    val output_memory_write_enable    = Output(Bool())
    val output_csr_read_data          = Output(UInt(Parameters.DataWidth))
  })
  val stall = io.stall

  val instruction = Module(new PipelineRegister(defaultValue = InstructionsNop.nop))
  instruction.io.in     := io.instruction
  instruction.io.stall  := stall
  instruction.io.flush  := io.flush
  io.output_instruction := instruction.io.out

  val instruction_address = Module(new PipelineRegister(defaultValue = ProgramCounter.EntryAddress))
  instruction_address.io.in     := io.instruction_address
  instruction_address.io.stall  := stall
  instruction_address.io.flush  := io.flush
  io.output_instruction_address := instruction_address.io.out

  val regs_reg1_read_address = Module(new PipelineRegister(Parameters.PhysicalRegisterAddrBits))
  regs_reg1_read_address.io.in     := io.regs_reg1_read_address
  regs_reg1_read_address.io.stall  := stall
  regs_reg1_read_address.io.flush  := io.flush
  io.output_regs_reg1_read_address := regs_reg1_read_address.io.out

  val regs_reg2_read_address = Module(new PipelineRegister(Parameters.PhysicalRegisterAddrBits))
  regs_reg2_read_address.io.in     := io.regs_reg2_read_address
  regs_reg2_read_address.io.stall  := stall
  regs_reg2_read_address.io.flush  := io.flush
  io.output_regs_reg2_read_address := regs_reg2_read_address.io.out

  val regs_write_enable = Module(new PipelineRegister(1))
  regs_write_enable.io.in     := io.regs_write_enable
  regs_write_enable.io.stall  := stall
  regs_write_enable.io.flush  := io.flush
  io.output_regs_write_enable := regs_write_enable.io.out

  val regs_write_address = Module(new PipelineRegister(Parameters.PhysicalRegisterAddrBits))
  regs_write_address.io.in     := io.regs_write_address
  regs_write_address.io.stall  := stall
  regs_write_address.io.flush  := io.flush
  io.output_regs_write_address := regs_write_address.io.out

  val regs_write_source = Module(new PipelineRegister(2))
  regs_write_source.io.in     := io.regs_write_source
  regs_write_source.io.stall  := stall
  regs_write_source.io.flush  := io.flush
  io.output_regs_write_source := regs_write_source.io.out

  val reg1_data = Module(new PipelineRegister())
  reg1_data.io.in     := io.reg1_data
  reg1_data.io.stall  := stall
  reg1_data.io.flush  := io.flush
  io.output_reg1_data := reg1_data.io.out

  val reg2_data = Module(new PipelineRegister())
  reg2_data.io.in     := io.reg2_data
  reg2_data.io.stall  := stall
  reg2_data.io.flush  := io.flush
  io.output_reg2_data := reg2_data.io.out

  val immediate = Module(new PipelineRegister())
  immediate.io.in     := io.immediate
  immediate.io.stall  := stall
  immediate.io.flush  := io.flush
  io.output_immediate := immediate.io.out

  val aluop1_source = Module(new PipelineRegister(1))
  aluop1_source.io.in     := io.aluop1_source
  aluop1_source.io.stall  := stall
  aluop1_source.io.flush  := io.flush
  io.output_aluop1_source := aluop1_source.io.out

  val aluop2_source = Module(new PipelineRegister(1))
  aluop2_source.io.in     := io.aluop2_source
  aluop2_source.io.stall  := stall
  aluop2_source.io.flush  := io.flush
  io.output_aluop2_source := aluop2_source.io.out

  val csr_write_enable = Module(new PipelineRegister(1))
  csr_write_enable.io.in     := io.csr_write_enable
  csr_write_enable.io.stall  := stall
  csr_write_enable.io.flush  := io.flush
  io.output_csr_write_enable := csr_write_enable.io.out

  val csr_address = Module(new PipelineRegister(Parameters.CSRRegisterAddrBits))
  csr_address.io.in     := io.csr_address
  csr_address.io.stall  := stall
  csr_address.io.flush  := io.flush
  io.output_csr_address := csr_address.io.out

  val memory_read_enable = Module(new PipelineRegister(1))
  memory_read_enable.io.in     := io.memory_read_enable
  memory_read_enable.io.stall  := stall
  memory_read_enable.io.flush  := io.flush
  io.output_memory_read_enable := memory_read_enable.io.out

  val memory_write_enable = Module(new PipelineRegister(1))
  memory_write_enable.io.in     := io.memory_write_enable
  memory_write_enable.io.stall  := stall
  memory_write_enable.io.flush  := io.flush
  io.output_memory_write_enable := memory_write_enable.io.out

  val csr_read_data = Module(new PipelineRegister())
  csr_read_data.io.in     := io.csr_read_data
  csr_read_data.io.stall  := stall
  csr_read_data.io.flush  := io.flush
  io.output_csr_read_data := csr_read_data.io.out
}
