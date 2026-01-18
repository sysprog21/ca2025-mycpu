// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.fivestage_forward

import chisel3._
import chisel3.util._
import riscv.core.ALU
import riscv.core.ALUControl
import riscv.Parameters

class Execute extends Module {
  val io = IO(new Bundle {
    val instruction                     = Input(UInt(Parameters.InstructionWidth))
    val instruction_address             = Input(UInt(Parameters.AddrWidth))
    val reg1_data                       = Input(UInt(Parameters.DataWidth))
    val reg2_data                       = Input(UInt(Parameters.DataWidth))
    val immediate_id                    = Input(UInt(Parameters.DataWidth))
    val aluop1_source_id                = Input(UInt(1.W))
    val aluop2_source_id                = Input(UInt(1.W))
    val csr_read_data_id                = Input(UInt(Parameters.DataWidth))
    val forward_from_mem                = Input(UInt(Parameters.DataWidth))
    val forward_from_wb                 = Input(UInt(Parameters.DataWidth))
    val reg1_forward                    = Input(UInt(2.W)) // forwarding.io.reg1_forward_ex
    val reg2_forward                    = Input(UInt(2.W)) // forwarding.io.reg2_forward_ex
    val interrupt_assert_clint          = Input(Bool())
    val interrupt_handler_address_clint = Input(UInt(Parameters.AddrWidth))

    val mem_alu_result     = Output(UInt(Parameters.DataWidth))
    val mem_reg2_data      = Output(UInt(Parameters.DataWidth))
    val csr_write_data     = Output(UInt(Parameters.DataWidth))
    val if_jump_flag       = Output(Bool())
    val if_jump_address    = Output(UInt(Parameters.AddrWidth))
    val clint_jump_flag    = Output(Bool())
    val clint_jump_address = Output(UInt(Parameters.AddrWidth))
  })

  val opcode = io.instruction(6, 0)
  val funct3 = io.instruction(14, 12)
  val funct7 = io.instruction(31, 25)
  val uimm   = io.instruction(19, 15)

  // ALU compute
  val alu      = Module(new ALU)
  val alu_ctrl = Module(new ALUControl)

  alu_ctrl.io.opcode := opcode
  alu_ctrl.io.funct3 := funct3
  alu_ctrl.io.funct7 := funct7
  alu.io.func        := alu_ctrl.io.alu_funct

  // 在执行单元中根据旁路单元的控制信号使用对应的旁路数据
//  var reg1_data = io.reg1_data
//  var reg2_data = io.reg2_data
//  when(io.reg1_forward === ForwardingType.ForwardFromMEM) {
//    reg1_data = io.forward_from_mem
//  }.elsewhen(io.reg1_forward === ForwardingType.ForwardFromWB){
//    reg1_data = io.forward_from_wb
//  }
//  when(io.reg2_forward === ForwardingType.ForwardFromMEM){
//    reg2_data = io.forward_from_mem
//  }.elsewhen(io.reg2_forward === ForwardingType.ForwardFromWB){
//    reg2_data = io.forward_from_wb
//  }
  val reg1_data = Mux(
    io.reg1_forward === ForwardingType.ForwardFromMEM,
    io.forward_from_mem,
    Mux(io.reg1_forward === ForwardingType.ForwardFromWB, io.forward_from_wb, io.reg1_data)
  )
  val reg2_data = Mux(
    io.reg2_forward === ForwardingType.ForwardFromMEM,
    io.forward_from_mem,
    Mux(io.reg2_forward === ForwardingType.ForwardFromWB, io.forward_from_wb, io.reg2_data)
  )

  alu.io.op1 := Mux(
    io.aluop1_source_id === ALUOp1Source.InstructionAddress,
    io.instruction_address,
    reg1_data
  )
  alu.io.op2 := Mux(
    io.aluop2_source_id === ALUOp2Source.Immediate,
    io.immediate_id,
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
      InstructionsTypeCSR.csrrc  -> io.csr_read_data_id.&((~reg1_data).asUInt),
      InstructionsTypeCSR.csrrs  -> io.csr_read_data_id.|(reg1_data),
      InstructionsTypeCSR.csrrwi -> Cat(0.U(27.W), uimm),
      InstructionsTypeCSR.csrrci -> io.csr_read_data_id.&((~Cat(0.U(27.W), uimm)).asUInt),
      InstructionsTypeCSR.csrrsi -> io.csr_read_data_id.|(Cat(0.U(27.W), uimm)),
    )
  )

  // jump and interrupt
  val instruction_jump_flag = (opcode === Instructions.jal) ||
    (opcode === Instructions.jalr) ||
    (opcode === InstructionTypes.B) && MuxLookup(
      funct3,
      false.B
    )(
      IndexedSeq(
        InstructionsTypeB.beq  -> (reg1_data === reg2_data),
        InstructionsTypeB.bne  -> (reg1_data =/= reg2_data),
        InstructionsTypeB.blt  -> (reg1_data.asSInt < reg2_data.asSInt),
        InstructionsTypeB.bge  -> (reg1_data.asSInt >= reg2_data.asSInt),
        InstructionsTypeB.bltu -> (reg1_data.asUInt < reg2_data.asUInt),
        InstructionsTypeB.bgeu -> (reg1_data.asUInt >= reg2_data.asUInt)
      )
    )
  io.clint_jump_flag    := instruction_jump_flag
  io.clint_jump_address := alu.io.result
  io.if_jump_flag       := io.interrupt_assert_clint || instruction_jump_flag
  io.if_jump_address    := Mux(io.interrupt_assert_clint, io.interrupt_handler_address_clint, alu.io.result)
}
