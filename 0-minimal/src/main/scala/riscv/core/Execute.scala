// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.Cat
import riscv.Parameters

// Minimal Execute - only supports addition and JALR
class Execute extends Module {
  val io = IO(new Bundle {
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val reg1_data           = Input(UInt(Parameters.DataWidth))
    val immediate           = Input(UInt(Parameters.DataWidth))
    val aluop1_source       = Input(UInt(1.W))

    val mem_alu_result  = Output(UInt(Parameters.DataWidth))
    val if_jump_flag    = Output(Bool())
    val if_jump_address = Output(UInt(Parameters.DataWidth))
  })

  val opcode = io.instruction(6, 0)

  // ALU: simple addition only (all our instructions use ADD)
  val aluOp1    = Mux(io.aluop1_source === ALUOp1Source.InstructionAddress, io.instruction_address, io.reg1_data)
  val aluOp2    = io.immediate
  val aluResult = aluOp1 + aluOp2

  io.mem_alu_result := aluResult

  // JALR: jump to (rs1 + immediate) & ~1
  val isJalr     = opcode === InstructionTypes.Jalr
  val jalrTarget = Cat(aluResult(Parameters.DataBits - 1, 1), 0.U(1.W))

  io.if_jump_flag    := isJalr
  io.if_jump_address := jalrTarget
}
