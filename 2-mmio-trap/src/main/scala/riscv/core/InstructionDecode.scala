// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import chisel3.ChiselEnum
import riscv.Parameters

// RV32I opcode groupings (RV32I + CSR)
object InstructionTypes {
  val Load    = "b0000011".U(7.W)
  val OpImm   = "b0010011".U(7.W)
  val Store   = "b0100011".U(7.W)
  val Op      = "b0110011".U(7.W)
  val Lui     = "b0110111".U(7.W)
  val Auipc   = "b0010111".U(7.W)
  val Jal     = "b1101111".U(7.W)
  val Jalr    = "b1100111".U(7.W)
  val Branch  = "b1100011".U(7.W)
  val MiscMem = "b0001111".U(7.W)
  val System  = "b1110011".U(7.W)
}

object Instructions {
  val jal   = InstructionTypes.Jal
  val jalr  = InstructionTypes.Jalr
  val lui   = InstructionTypes.Lui
  val auipc = InstructionTypes.Auipc
}

object InstructionsTypeL {
  val lb  = "b000".U(3.W)
  val lh  = "b001".U(3.W)
  val lw  = "b010".U(3.W)
  val lbu = "b100".U(3.W)
  val lhu = "b101".U(3.W)
}

object InstructionsTypeS {
  val sb = "b000".U(3.W)
  val sh = "b001".U(3.W)
  val sw = "b010".U(3.W)
}

object InstructionsTypeI {
  val addi  = "b000".U(3.W)
  val slli  = "b001".U(3.W)
  val slti  = "b010".U(3.W)
  val sltiu = "b011".U(3.W)
  val xori  = "b100".U(3.W)
  val sri   = "b101".U(3.W)
  val ori   = "b110".U(3.W)
  val andi  = "b111".U(3.W)
}

object InstructionsTypeR {
  val add_sub = "b000".U(3.W)
  val sll     = "b001".U(3.W)
  val slt     = "b010".U(3.W)
  val sltu    = "b011".U(3.W)
  val xor     = "b100".U(3.W)
  val sr      = "b101".U(3.W)
  val or      = "b110".U(3.W)
  val and     = "b111".U(3.W)
}

object InstructionsTypeB {
  val beq  = "b000".U(3.W)
  val bne  = "b001".U(3.W)
  val blt  = "b100".U(3.W)
  val bge  = "b101".U(3.W)
  val bltu = "b110".U(3.W)
  val bgeu = "b111".U(3.W)
}

object InstructionsTypeCSR {
  val csrrw  = "b001".U(3.W)
  val csrrs  = "b010".U(3.W)
  val csrrc  = "b011".U(3.W)
  val csrrwi = "b101".U(3.W)
  val csrrsi = "b110".U(3.W)
  val csrrci = "b111".U(3.W)
}

object InstructionsEnv {
  val ecall  = 0x00000073L.U(Parameters.DataWidth)
  val ebreak = 0x00100073L.U(Parameters.DataWidth)
}

object InstructionsRet {
  val mret = 0x30200073L.U(Parameters.DataWidth)
}

object InstructionsNop {
  val nop = 0x00000013L.U(Parameters.DataWidth)
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
  val CSR                    = 2.U(2.W)
  val NextInstructionAddress = 3.U(2.W)
}

object ImmediateKind extends ChiselEnum {
  val None, I, S, B, U, J = Value
}

/**
 * InstructionDecode: Instruction field extraction and control signal generation
 *
 * Pipeline Stage: ID (Instruction Decode)
 *
 * Key Responsibilities:
 * - Extract instruction fields (opcode, rd, rs1, rs2, funct3, funct7, CSR address)
 * - Generate control signals for Execute, Memory, and WriteBack stages
 * - Decode and sign-extend immediate values based on instruction format
 * - Determine ALU operand sources (register vs PC, register vs immediate)
 * - Identify register file and CSR read/write operations
 * - Configure memory access (read/write enable signals)
 *
 * RV32I Instruction Formats Decoded:
 * - R-type: Register-register operations (ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU)
 * - I-type: Immediate operations and loads (ADDI, SLTI, ANDI, ORI, XORI, LB, LH, LW, JALR)
 * - S-type: Store operations (SB, SH, SW)
 * - B-type: Branch operations (BEQ, BNE, BLT, BGE, BLTU, BGEU)
 * - U-type: Upper immediate (LUI, AUIPC)
 * - J-type: Jump (JAL)
 *
 * Zicsr Extension (CSR Instructions):
 * - CSRRW/CSRRWI: Atomic read-write CSR
 * - CSRRS/CSRRSI: Atomic read and set bits in CSR
 * - CSRRC/CSRRCI: Atomic read and clear bits in CSR
 * - CSR address extracted from inst[31:20] (12-bit CSR space)
 * - Immediate variant (I-suffix) uses zimm from inst[19:15] (5-bit zero-extended)
 *
 * Control Signal Generation:
 * - reg_write_enable: Enable writing to rd (false for branches, stores, CSR ops without rd)
 * - csr_reg_write_enable: Enable CSR write operation
 * - memory_read_enable: Asserted for load instructions
 * - memory_write_enable: Asserted for store instructions
 * - ex_aluop1_source: Select PC vs rs1 for ALU operand 1
 * - ex_aluop2_source: Select immediate vs rs2 for ALU operand 2
 * - wb_reg_write_source: Select ALU result, memory data, CSR data, or PC+4 for rd
 *
 * Interface:
 * - Input: 32-bit instruction from IF stage
 * - Outputs: Control signals to EX/MEM/WB, immediate value, register addresses, CSR address
 */
class InstructionDecode extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(Parameters.InstructionWidth))

    val regs_reg1_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val regs_reg2_read_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val ex_immediate           = Output(UInt(Parameters.DataBits.W))
    val ex_aluop1_source       = Output(UInt(1.W))
    val ex_aluop2_source       = Output(UInt(1.W))
    val memory_read_enable     = Output(Bool())
    val memory_write_enable    = Output(Bool())
    val wb_reg_write_source    = Output(UInt(2.W))
    val reg_write_enable       = Output(Bool())
    val reg_write_address      = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val csr_reg_address        = Output(UInt(Parameters.CSRRegisterAddrWidth))
    val csr_reg_write_enable   = Output(Bool())
  })

  val instruction = io.instruction
  val opcode      = instruction(6, 0)
  val funct3      = instruction(14, 12)
  val rs1         = instruction(19, 15)
  val rs2         = instruction(24, 20)
  val rd          = instruction(11, 7)

  val isLoad    = opcode === InstructionTypes.Load
  val isStore   = opcode === InstructionTypes.Store
  val isOpImm   = opcode === InstructionTypes.OpImm
  val isOp      = opcode === InstructionTypes.Op
  val isLui     = opcode === InstructionTypes.Lui
  val isAuipc   = opcode === InstructionTypes.Auipc
  val isJal     = opcode === InstructionTypes.Jal
  val isJalr    = opcode === InstructionTypes.Jalr
  val isBranch  = opcode === InstructionTypes.Branch
  val isMiscMem = opcode === InstructionTypes.MiscMem
  val isSystem  = opcode === InstructionTypes.System
  val isCsr     = isSystem && (funct3 =/= "b000".U)

  val csrImm           = instruction(19, 15)
  val csrImmIsZero     = csrImm === 0.U
  val csrRs1IsZero     = rs1 === 0.U
  val csrUsesImmediate = funct3(2) && isCsr
  val csrSourceZero    = csrUsesImmediate && csrImmIsZero || (!csrUsesImmediate && csrRs1IsZero)

  val usesRs1 = isLoad || isStore || isOpImm || isOp || isBranch || isJalr || (isCsr && !csrUsesImmediate)
  val usesRs2 = isStore || isOp || isBranch

  val regWrite = (isLoad || isOpImm || isOp || isLui || isAuipc || isJal || isJalr || isCsr) && (rd =/= 0.U)

  // ============================================================
  // [CA25: Exercise 2] Control Signal Generation
  // ============================================================
  // Hint: Generate correct control signals based on instruction type
  //
  // Need to determine three key multiplexer selections:
  // 1. WriteBack data source selection (wbSource)
  // 2. ALU operand 1 selection (aluOp1Sel)
  // 3. ALU operand 2 selection (aluOp2Sel)

  // WriteBack data source selection:
  // - Default: ALU result
  // - Load instructions: Read from Memory
  // - CSR instructions: Read from CSR
  // - JAL/JALR: Save PC+4 (return address)
  val wbSource = WireDefault(RegWriteSource.ALUResult)
  // TODO: Determine when to write back from Memory
  when(?) {
    wbSource := RegWriteSource.Memory
  }
  // TODO: Determine when to write back from CSR
  .elsewhen(?) {
    wbSource := RegWriteSource.CSR
  }
  // TODO: Determine when to write back PC+4
  .elsewhen(?) {
    wbSource := RegWriteSource.NextInstructionAddress
  }

  // ALU operand 1 selection:
  // - Default: Register rs1
  // - Branch/AUIPC/JAL: Use PC (for calculating target address or PC+offset)
  //
  val aluOp1Sel = WireDefault(ALUOp1Source.Register)
  // TODO: Determine when to use PC as first operand
  // Hint: Consider instructions that need PC-relative addressing
  when(?) {
    aluOp1Sel := ALUOp1Source.InstructionAddress
  }

  // ALU operand 2 selection:
  // - Default: Register rs2 (for R-type instructions)
  // - I-type/S-type/B-type/U-type/J-type: Use immediate
  val needsImmediate =
    isLoad || isStore || isOpImm || isBranch || isLui || isAuipc || isJal || isJalr
  val aluOp2Sel = WireDefault(ALUOp2Source.Register)
  // TODO: Determine when to use immediate as second operand
  // Hint: Most instruction types except R-type use immediate
  when(?) {
    aluOp2Sel := ALUOp2Source.Immediate
  }

  val immKind = WireDefault(ImmediateKind.None)
  when(isLoad || isOpImm || isJalr) {
    immKind := ImmediateKind.I
  }
  when(isStore) {
    immKind := ImmediateKind.S
  }
  when(isBranch) {
    immKind := ImmediateKind.B
  }
  when(isLui || isAuipc) {
    immKind := ImmediateKind.U
  }
  when(isJal) {
    immKind := ImmediateKind.J
  }

  io.regs_reg1_read_address := Mux(usesRs1, rs1, 0.U)
  io.regs_reg2_read_address := Mux(usesRs2, rs2, 0.U)
  io.ex_aluop1_source       := aluOp1Sel
  io.ex_aluop2_source       := aluOp2Sel
  io.memory_read_enable     := isLoad
  io.memory_write_enable    := isStore
  io.wb_reg_write_source    := wbSource
  io.reg_write_enable       := regWrite
  io.reg_write_address      := rd

  io.csr_reg_address := instruction(31, 20)
  val csrWrites = isCsr && (
    (funct3 === InstructionsTypeCSR.csrrw) ||
      (funct3 === InstructionsTypeCSR.csrrwi) ||
      (!csrSourceZero && (funct3 === InstructionsTypeCSR.csrrs ||
        funct3 === InstructionsTypeCSR.csrrc ||
        funct3 === InstructionsTypeCSR.csrrsi ||
        funct3 === InstructionsTypeCSR.csrrci))
  )
  io.csr_reg_write_enable := csrWrites

  // ============================================================
  // [CA25: Exercise 1] Immediate Extension - RISC-V Instruction Encoding
  // ============================================================
  // Hint: RISC-V has five immediate formats, requiring correct bit-field
  // extraction and sign extension
  //
  // I-type (12-bit): Used for ADDI, LW, JALR, etc.
  //   Immediate located at inst[31:20]
  //   Requires sign extension to 32 bits
  //   Hint: Use Fill() to replicate sign bit instruction(31)
  //
  val immI = Cat(
    Fill(Parameters.DataBits - 12, instruction(31)),  // Sign extension: replicate bit 31 twenty times
    instruction(31, 20)                                // Immediate: bits [31:20]
  )

  // S-type (12-bit): Used for SW, SH, SB store instructions
  //   Immediate split into two parts: inst[31:25] and inst[11:7]
  //   Need to concatenate these parts then sign extend
  //   Hint: High bits at upper field, low bits at lower field
  //
  // TODO: Complete S-type immediate extension
  val immS = Cat(
    Fill(Parameters.DataBits - 12, instruction(?)),  // Sign extension
    instruction(?),                                  // High 7 bits
    instruction(?)                                   // Low 5 bits
  )

  // B-type (13-bit): Used for BEQ, BNE, BLT branch instructions
  //   Immediate requires reordering: {sign, bit11, bits[10:5], bits[4:1], 0}
  //   Note: LSB is always 0 (2-byte alignment)
  //   Requires sign extension to 32 bits
  //   Hint: B-type bit order is scrambled, must reorder per specification
  //
  // TODO: Complete B-type immediate extension
  val immB = Cat(
    Fill(Parameters.DataBits - 13, instruction(31)), // Sign extension
    instruction(?),                                  // bit [12]
    instruction(?),                                  // bit [11]
    instruction(?),                                  // bits [10:5]
    instruction(?),                                  // bits [4:1]
    ?                                                // bit [0] = 0 (alignment)
  )

  // U-type (20-bit): Used for LUI, AUIPC
  //   Immediate located at inst[31:12], low 12 bits filled with zeros
  //   No sign extension needed (placed directly in upper 20 bits)
  //   Hint: U-type places 20 bits in result's upper bits, fills low 12 bits with 0
  val immU = Cat(instruction(31, 12), 0.U(12.W))

  // J-type (21-bit): Used for JAL
  //   Immediate requires reordering: {sign, bits[19:12], bit11, bits[10:1], 0}
  //   Note: LSB is always 0 (2-byte alignment)
  //   Requires sign extension to 32 bits
  //   Hint: J-type bit order is scrambled, similar to B-type
  //
  // TODO: Complete J-type immediate extension
  val immJ = Cat(
    Fill(Parameters.DataBits - 21, instruction(31)), // Sign extension
    instruction(?),                                  // bit [20]
    instruction(?),                                  // bits [19:12]
    instruction(?),                                  // bit [11]
    instruction(?),                                  // bits [10:1]
    ?                                                // bit [0] = 0 (alignment)
  )

  val immediate = MuxLookup(immKind.asUInt, 0.U(Parameters.DataBits.W))(
    Seq(
      ImmediateKind.I.asUInt -> immI,
      ImmediateKind.S.asUInt -> immS,
      ImmediateKind.B.asUInt -> immB,
      ImmediateKind.U.asUInt -> immU,
      ImmediateKind.J.asUInt -> immJ
    )
  )
  io.ex_immediate := immediate
}
