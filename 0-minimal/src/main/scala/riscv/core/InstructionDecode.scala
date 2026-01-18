// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// Minimal instruction set - only what jit.asmbin needs
object InstructionTypes {
  val Load  = "b0000011".U(7.W)
  val OpImm = "b0010011".U(7.W)
  val Store = "b0100011".U(7.W)
  val Auipc = "b0010111".U(7.W)
  val Jalr  = "b1100111".U(7.W)
}

object ALUOp1Source {
  val Register           = 0.U(1.W)
  val InstructionAddress = 1.U(1.W)
}

object ALUOp2Source {
  val Register  = 0.U(1.W)
  val Immediate = 1.U(1.W)
}

object RegWriteSource {
  val ALUResult              = 0.U(2.W)
  val Memory                 = 1.U(2.W)
  val NextInstructionAddress = 2.U(2.W)
}

// Minimal InstructionDecode - only supports: AUIPC, ADDI, LW, SW, JALR
class InstructionDecode extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(Parameters.InstructionWidth))

    val regs_reg1_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val regs_reg2_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val ex_immediate           = Output(UInt(Parameters.DataBits.W))
    val ex_aluop1_source       = Output(UInt(1.W))
    val memory_read_enable     = Output(Bool())
    val memory_write_enable    = Output(Bool())
    val wb_reg_write_source    = Output(UInt(2.W))
    val reg_write_enable       = Output(Bool())
    val reg_write_address      = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
  })

  // Instruction Fields
  val instruction = io.instruction
  val opcode      = instruction(6, 0)
  val rs1         = instruction(19, 15)
  val rs2         = instruction(24, 20)
  val rd          = instruction(11, 7)

  // Instruction Type Decoding
  val isLoad  = opcode === InstructionTypes.Load  // LW
  val isStore = opcode === InstructionTypes.Store // SW
  val isOpImm = opcode === InstructionTypes.OpImm // ADDI
  val isAuipc = opcode === InstructionTypes.Auipc // AUIPC
  val isJalr  = opcode === InstructionTypes.Jalr  // JALR

  // Control Signal Generation

  // Register usage
  val usesRs1  = isLoad || isStore || isOpImm || isJalr
  val usesRs2  = isStore
  val regWrite = isLoad || isOpImm || isAuipc || isJalr

  // Writeback source
  val wbSource = WireDefault(RegWriteSource.ALUResult)
  when(isLoad) {
    wbSource := RegWriteSource.Memory
  }.elsewhen(isJalr) {
    wbSource := RegWriteSource.NextInstructionAddress
  }

  // ALU operand 1 source (PC for AUIPC, register otherwise)
  val aluOp1Sel = Mux(isAuipc, ALUOp1Source.InstructionAddress, ALUOp1Source.Register)

  // ALU operand 2 source (all supported instructions use an immediate)
  val aluOp2Sel = ALUOp2Source.Immediate

  // Immediate Extraction
  // I-type for Load, OpImm, Jalr
  val immI = Cat(Fill(Parameters.DataBits - 12, instruction(31)), instruction(31, 20))
  // S-type for Store
  val immS = Cat(Fill(Parameters.DataBits - 12, instruction(31)), instruction(31, 25), instruction(11, 7))
  // U-type for AUIPC
  val immU = Cat(instruction(31, 12), 0.U(12.W))

  val immediate = MuxLookup(opcode, immI)(
    Seq(
      InstructionTypes.Store -> immS,
      InstructionTypes.Auipc -> immU
    )
  )

  // Output Assignments
  io.regs_reg1_read_address := Mux(usesRs1, rs1, 0.U)
  io.regs_reg2_read_address := Mux(usesRs2, rs2, 0.U)
  io.ex_immediate           := immediate
  io.ex_aluop1_source       := aluOp1Sel
  io.memory_read_enable     := isLoad
  io.memory_write_enable    := isStore
  io.wb_reg_write_source    := wbSource
  io.reg_write_enable       := regWrite
  io.reg_write_address      := rd
}
