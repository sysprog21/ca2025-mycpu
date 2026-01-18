// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

/**
 * EX/MEM Pipeline Register: Execute to Memory Access boundary.
 *
 * Buffers ALU computation results, store data, and memory control signals.
 * This register feeds the memory access stage and provides the MEM-stage
 * forwarding path (highest priority in the forwarding network).
 *
 * Key signals buffered:
 * - alu_result: Address for loads/stores, or computation result for ALU ops
 * - reg2_data: Store data (forwarded from EX stage)
 * - funct3: Memory access width (byte/half/word) and sign-extension mode
 * - memory_*_enable: Triggers AXI4-Lite bus transactions in MEM stage
 *
 * No flush input: EX2MEM never flushes because by the time an instruction
 * reaches this point, all control hazards have been resolved. Memory stalls
 * simply hold the register contents.
 */
class EX2MEM extends Module {
  val io = IO(new Bundle() {
    val stall               = Input(Bool())
    val regs_write_enable   = Input(Bool())
    val regs_write_source   = Input(UInt(2.W))
    val regs_write_address  = Input(UInt(Parameters.AddrWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val funct3              = Input(UInt(3.W))
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val memory_read_enable  = Input(Bool())
    val memory_write_enable = Input(Bool())
    val alu_result          = Input(UInt(Parameters.DataWidth))
    val csr_read_data       = Input(UInt(Parameters.DataWidth))

    val output_regs_write_enable   = Output(Bool())
    val output_regs_write_source   = Output(UInt(2.W))
    val output_regs_write_address  = Output(UInt(Parameters.AddrWidth))
    val output_instruction_address = Output(UInt(Parameters.AddrWidth))
    val output_funct3              = Output(UInt(Parameters.DataWidth))
    val output_reg2_data           = Output(UInt(Parameters.DataWidth))
    val output_memory_read_enable  = Output(Bool())
    val output_memory_write_enable = Output(Bool())
    val output_alu_result          = Output(UInt(Parameters.DataWidth))
    val output_csr_read_data       = Output(UInt(Parameters.DataWidth))
  })

  val stall = io.stall
  val flush = false.B

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

  val instruction_address = Module(new PipelineRegister(Parameters.AddrBits))
  instruction_address.io.in     := io.instruction_address
  instruction_address.io.stall  := stall
  instruction_address.io.flush  := flush
  io.output_instruction_address := instruction_address.io.out

  val funct3 = Module(new PipelineRegister(3))
  funct3.io.in     := io.funct3
  funct3.io.stall  := stall
  funct3.io.flush  := flush
  io.output_funct3 := funct3.io.out

  val reg2_data = Module(new PipelineRegister())
  reg2_data.io.in     := io.reg2_data
  reg2_data.io.stall  := stall
  reg2_data.io.flush  := flush
  io.output_reg2_data := reg2_data.io.out

  val alu_result = Module(new PipelineRegister())
  alu_result.io.in     := io.alu_result
  alu_result.io.stall  := stall
  alu_result.io.flush  := flush
  io.output_alu_result := alu_result.io.out

  val memory_read_enable = Module(new PipelineRegister(1))
  memory_read_enable.io.in     := io.memory_read_enable
  memory_read_enable.io.stall  := stall
  memory_read_enable.io.flush  := flush
  io.output_memory_read_enable := memory_read_enable.io.out

  val memory_write_enable = Module(new PipelineRegister(1))
  memory_write_enable.io.in     := io.memory_write_enable
  memory_write_enable.io.stall  := stall
  memory_write_enable.io.flush  := flush
  io.output_memory_write_enable := memory_write_enable.io.out

  val csr_read_data = Module(new PipelineRegister())
  csr_read_data.io.in     := io.csr_read_data
  csr_read_data.io.stall  := stall
  csr_read_data.io.flush  := flush
  io.output_csr_read_data := csr_read_data.io.out
}
