// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.core.ALU
import riscv.core.ALUControl
import riscv.Parameters

class Execute extends Module {
  val io = IO(new Bundle {
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val reg1_data           = Input(UInt(Parameters.DataWidth))
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val immediate           = Input(UInt(Parameters.DataWidth))
    val aluop1_source       = Input(UInt(1.W))
    val aluop2_source       = Input(UInt(1.W))
    val csr_read_data       = Input(UInt(Parameters.DataWidth))
    val forward_from_mem    = Input(UInt(Parameters.DataWidth))
    val forward_from_wb     = Input(UInt(Parameters.DataWidth))
    val reg1_forward        = Input(UInt(2.W))
    val reg2_forward        = Input(UInt(2.W))

    val mem_alu_result = Output(UInt(Parameters.DataWidth))
    val mem_reg2_data  = Output(UInt(Parameters.DataWidth))
    val csr_write_data = Output(UInt(Parameters.DataWidth))
  })

  val opcode = io.instruction(6, 0)
  val funct3 = io.instruction(14, 12)
  val funct7 = io.instruction(31, 25)
  val uimm   = io.instruction(19, 15)

  val alu      = Module(new ALU)
  val alu_ctrl = Module(new ALUControl)

  alu_ctrl.io.opcode := opcode
  alu_ctrl.io.funct3 := funct3
  alu_ctrl.io.funct7 := funct7
  alu.io.func        := alu_ctrl.io.alu_funct

  val reg1_data = MuxLookup(
    io.reg1_forward,
    io.reg1_data
  )(
    IndexedSeq(
      ForwardingType.ForwardFromMEM -> io.forward_from_mem,
      ForwardingType.ForwardFromWB  -> io.forward_from_wb
    )
  )
  alu.io.op1 := Mux(
    io.aluop1_source === ALUOp1Source.InstructionAddress,
    io.instruction_address,
    reg1_data
  )

  val reg2_data = MuxLookup(
    io.reg2_forward,
    io.reg2_data
  )(
    IndexedSeq(
      ForwardingType.ForwardFromMEM -> io.forward_from_mem,
      ForwardingType.ForwardFromWB  -> io.forward_from_wb
    )
  )
  alu.io.op2 := Mux(
    io.aluop2_source === ALUOp2Source.Immediate,
    io.immediate,
    reg2_data
  )
  io.mem_alu_result := alu.io.result
  io.mem_reg2_data  := reg2_data
  io.csr_write_data := MuxLookup(
    funct3,
    0.U
  )(
    IndexedSeq(
      InstructionsTypeCSR.csrrw  -> reg1_data,
      InstructionsTypeCSR.csrrc  -> io.csr_read_data.&((~reg1_data).asUInt),
      InstructionsTypeCSR.csrrs  -> io.csr_read_data.|(reg1_data),
      InstructionsTypeCSR.csrrwi -> Cat(0.U(27.W), uimm),
      InstructionsTypeCSR.csrrci -> io.csr_read_data.&((~Cat(0.U(27.W), uimm)).asUInt),
      InstructionsTypeCSR.csrrsi -> io.csr_read_data.|(Cat(0.U(27.W), uimm)),
    )
  )
}
