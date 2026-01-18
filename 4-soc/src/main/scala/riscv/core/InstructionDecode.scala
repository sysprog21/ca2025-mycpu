// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

class InstructionDecode extends Module {
  val io = IO(new Bundle {
    val instruction               = Input(UInt(Parameters.InstructionWidth))
    val instruction_address       = Input(UInt(Parameters.AddrWidth)) // if2id.io.output_instruction_address
    val reg1_data                 = Input(UInt(Parameters.DataWidth)) // regs.io.read_data1
    val reg2_data                 = Input(UInt(Parameters.DataWidth)) // regs.io.read_data2
    val forward_from_mem          = Input(UInt(Parameters.DataWidth)) // mem.io.forward_data
    val forward_from_wb           = Input(UInt(Parameters.DataWidth)) // wb.io.regs_write_data
    val reg1_forward              = Input(UInt(2.W))                  // forwarding.io.reg1_forward_id
    val reg2_forward              = Input(UInt(2.W))                  // forwarding.io.reg2_forward_id
    val interrupt_assert          = Input(Bool())                     // clint.io.id_interrupt_assert
    val interrupt_handler_address = Input(UInt(Parameters.AddrWidth)) // clint.io.id_interrupt_handler_address
    // Suppress branch decision when there's a RAW hazard with EX stage
    // Without this, branch would use wrong forwarded value from MEM (stale instruction)
    val branch_hazard = Input(Bool()) // ctrl.io.branch_hazard

    val regs_reg1_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val regs_reg2_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val ex_immediate           = Output(UInt(Parameters.DataWidth))
    val ex_aluop1_source       = Output(UInt(1.W))
    val ex_aluop2_source       = Output(UInt(1.W))
    val ex_memory_read_enable  = Output(Bool())
    val ex_memory_write_enable = Output(Bool())
    val ex_reg_write_source    = Output(UInt(2.W))
    val ex_reg_write_enable    = Output(Bool())
    val ex_reg_write_address   = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val ex_csr_address         = Output(UInt(Parameters.CSRRegisterAddrWidth))
    val ex_csr_write_enable    = Output(Bool())
    val ctrl_jump_instruction  = Output(Bool())                     // ctrl.io.jump_instruction_id
    val clint_jump_flag        = Output(Bool())                     // clint.io.jump_flag
    val clint_jump_address     = Output(UInt(Parameters.AddrWidth)) // clint.io.jump_address
    val if_jump_flag           = Output(Bool())                     // ctrl.io.jump_flag , inst_fetch.io.jump_flag_id
    val if_jump_address        = Output(UInt(Parameters.AddrWidth)) // inst_fetch.io.jump_address_id
  })
  val opcode = io.instruction(6, 0)
  val funct3 = io.instruction(14, 12)
  val funct7 = io.instruction(31, 25)
  val rd     = io.instruction(11, 7)
  val rs1    = io.instruction(19, 15)
  val rs2    = io.instruction(24, 20)

  // Track which operands are actually used to avoid false hazards/stalls on
  // encodings that reuse rs1/rs2 bits for immediates (JAL, CSR immediate, etc.).
  val csr_uses_uimm = opcode === Instructions.csr && (
    funct3 === InstructionsTypeCSR.csrrwi ||
      funct3 === InstructionsTypeCSR.csrrci ||
      funct3 === InstructionsTypeCSR.csrrsi
  )
  val uses_rs1 = (opcode === InstructionTypes.RM) || (opcode === InstructionTypes.I) ||
    (opcode === InstructionTypes.L) || (opcode === InstructionTypes.S) || (opcode === InstructionTypes.B) ||
    (opcode === Instructions.jalr) || (opcode === Instructions.csr && !csr_uses_uimm)
  val uses_rs2 = (opcode === InstructionTypes.RM) || (opcode === InstructionTypes.S) || (opcode === InstructionTypes.B)

  io.regs_reg1_read_address := Mux(uses_rs1, rs1, 0.U(Parameters.PhysicalRegisterAddrWidth))
  io.regs_reg2_read_address := Mux(uses_rs2, rs2, 0.U(Parameters.PhysicalRegisterAddrWidth))
  io.ex_immediate := MuxLookup(
    opcode,
    Cat(Fill(20, io.instruction(31)), io.instruction(31, 20))
  )(
    IndexedSeq(
      InstructionTypes.I -> Cat(Fill(21, io.instruction(31)), io.instruction(30, 20)),
      InstructionTypes.L -> Cat(Fill(21, io.instruction(31)), io.instruction(30, 20)),
      Instructions.jalr  -> Cat(Fill(21, io.instruction(31)), io.instruction(30, 20)),
      InstructionTypes.S -> Cat(Fill(21, io.instruction(31)), io.instruction(30, 25), io.instruction(11, 7)),
      InstructionTypes.B -> Cat(
        Fill(20, io.instruction(31)),
        io.instruction(7),
        io.instruction(30, 25),
        io.instruction(11, 8),
        0.U(1.W)
      ),
      Instructions.lui   -> Cat(io.instruction(31, 12), 0.U(12.W)),
      Instructions.auipc -> Cat(io.instruction(31, 12), 0.U(12.W)),
      Instructions.jal -> Cat(
        Fill(12, io.instruction(31)),
        io.instruction(19, 12),
        io.instruction(20),
        io.instruction(30, 21),
        0.U(1.W)
      )
    )
  )
  io.ex_aluop1_source := Mux(
    opcode === Instructions.auipc || opcode === InstructionTypes.B || opcode === Instructions.jal,
    ALUOp1Source.InstructionAddress,
    ALUOp1Source.Register
  )
  io.ex_aluop2_source := Mux(
    opcode === InstructionTypes.RM,
    ALUOp2Source.Register,
    ALUOp2Source.Immediate
  )
  io.ex_memory_read_enable  := opcode === InstructionTypes.L
  io.ex_memory_write_enable := opcode === InstructionTypes.S
  io.ex_reg_write_source := MuxLookup(
    opcode,
    RegWriteSource.ALUResult
  )(
    IndexedSeq(
      InstructionTypes.L -> RegWriteSource.Memory,
      Instructions.csr   -> RegWriteSource.CSR,
      Instructions.jal   -> RegWriteSource.NextInstructionAddress,
      Instructions.jalr  -> RegWriteSource.NextInstructionAddress
    )
  )
  io.ex_reg_write_enable := (opcode === InstructionTypes.RM) || (opcode === InstructionTypes.I) ||
    (opcode === InstructionTypes.L) || (opcode === Instructions.auipc) || (opcode === Instructions.lui) ||
    (opcode === Instructions.jal) || (opcode === Instructions.jalr) || (opcode === Instructions.csr)
  io.ex_reg_write_address := io.instruction(11, 7)
  io.ex_csr_address       := io.instruction(31, 20)
  io.ex_csr_write_enable := (opcode === Instructions.csr) && (
    funct3 === InstructionsTypeCSR.csrrw || funct3 === InstructionsTypeCSR.csrrwi ||
      funct3 === InstructionsTypeCSR.csrrs || funct3 === InstructionsTypeCSR.csrrsi ||
      funct3 === InstructionsTypeCSR.csrrc || funct3 === InstructionsTypeCSR.csrrci
  )

//  io.clint_jump_flag := io.interrupt_assert
//  io.clint_jump_address := io.interrupt_handler_address
  val reg1_data_forwarded = MuxLookup(io.reg1_forward, 0.U)(
    IndexedSeq(
      ForwardingType.NoForward      -> io.reg1_data,
      ForwardingType.ForwardFromWB  -> io.forward_from_wb,
      ForwardingType.ForwardFromMEM -> io.forward_from_mem
    )
  )
  val reg2_data_forwarded = MuxLookup(io.reg2_forward, 0.U)(
    IndexedSeq(
      ForwardingType.NoForward      -> io.reg2_data,
      ForwardingType.ForwardFromWB  -> io.forward_from_wb,
      ForwardingType.ForwardFromMEM -> io.forward_from_mem
    )
  )
  val reg1_data = Mux(uses_rs1, reg1_data_forwarded, 0.U)
  val reg2_data = Mux(uses_rs2, reg2_data_forwarded, 0.U)
  io.ctrl_jump_instruction := opcode === Instructions.jal ||
    (opcode === Instructions.jalr) ||
    (opcode === InstructionTypes.B)

  // Suppress branch/jump decision when there's a RAW hazard with EX stage
  // The branch_hazard signal indicates that the value needed for comparison is still
  // being computed in EX stage. Forwarding would get the wrong value from MEM stage
  // (which has a different instruction's result). We must NOT take the branch this cycle.
  // The pipeline will stall and re-evaluate next cycle when correct value is available.
  val branch_taken = !io.branch_hazard && (
    opcode === Instructions.jal ||
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
  )

  io.if_jump_flag := branch_taken || io.interrupt_assert

  val jalr_target = Cat((reg1_data + io.ex_immediate)(Parameters.AddrBits - 1, 1), 0.U(1.W))

  io.if_jump_address := Mux(
    io.interrupt_assert,
    io.interrupt_handler_address,
    MuxLookup(opcode, 0.U)(
      IndexedSeq(
        InstructionTypes.B -> (io.instruction_address + io.ex_immediate),
        Instructions.jal   -> (io.instruction_address + io.ex_immediate),
        Instructions.jalr  -> jalr_target
      )
    )
  )
  io.clint_jump_flag := io.ctrl_jump_instruction
  io.clint_jump_address := MuxLookup(
    opcode,
    0.U
  )(
    IndexedSeq(
      InstructionTypes.B -> (io.instruction_address + io.ex_immediate),
      Instructions.jal   -> (io.instruction_address + io.ex_immediate),
      Instructions.jalr  -> jalr_target
    )
  )
}
