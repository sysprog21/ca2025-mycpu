// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_stall

import chisel3._
import riscv.core.PipelineRegister
import riscv.Parameters

/**
 * MEM/WB Pipeline Register: buffers memory results for writeback stage
 *
 * Final pipeline register transferring results from Memory Access to
 * Write Back stage. Holds all possible result values for register file update.
 *
 * Data Stored:
 *
 * **Result Values (one will be selected for writeback):**
 * - ALU result (arithmetic/logical operations)
 * - Memory read data (load instructions)
 * - CSR read data (CSR instructions)
 *
 * **Register Write Control:**
 * - Write enable signal
 * - Destination register address
 * - Source selector (which result to write)
 *
 * **Debug Information:**
 * - Instruction address (PC) for tracing
 *
 * Forwarding Role:
 * - Provides ForwardFromWB path (priority 2)
 * - 2-cycle old results for forwarding
 * - Handles both ALU and load results
 *
 * Write Back Selection:
 * - Source 0: ALU result
 * - Source 1: Memory data (loads)
 * - Source 2: CSR data
 *
 * Pipeline Position:
 * ```
 * MEM Stage → [MEM2WB Register] → WB Stage → Register File
 *              ↓
 *        Forwarding to EX/ID
 * ```
 *
 * @note No stall/flush signals - WB stage always completes
 * @note Final stage before register file update
 * @note Both load and ALU results available for forwarding
 */
class MEM2WB extends Module {
  val io = IO(new Bundle() {
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
  val stall = false.B
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
