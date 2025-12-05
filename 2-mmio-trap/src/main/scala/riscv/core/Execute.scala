// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.Cat
import chisel3.util.MuxLookup
import riscv.Parameters

/**
 * Execute: ALU operations, CSR access, and branch resolution for RV32I+Zicsr
 *
 * Pipeline Stage: EX (Execute)
 *
 * Key Responsibilities:
 * - Select ALU operands from register data, PC, or immediate values
 * - Perform arithmetic and logical operations via ALU submodule
 * - Execute CSR read-modify-write operations (CSRRW, CSRRS, CSRRC variants)
 * - Evaluate branch conditions for all six RV32I branch types
 * - Calculate branch and jump target addresses
 * - Generate control signals for instruction fetch (jump flag and address)
 *
 * CSR Instruction Handling:
 * - CSRRW: Write rs1/zimm to CSR, read old value to rd
 * - CSRRS: Set bits in CSR using rs1/zimm, read old value to rd
 * - CSRRC: Clear bits in CSR using rs1/zimm, read old value to rd
 * - CSR writes occur in parallel with normal ALU operations
 * - CSR read data comes from CSR module (csr_reg_read_data input)
 * - CSR write data computed here (csr_reg_write_data output)
 *
 * Control Flow Handling:
 * - Branch (B-type): Compare rs1 and rs2, compute PC + immediate if taken
 *   - BEQ/BNE: Equality comparison (signed or unsigned agnostic)
 *   - BLT/BGE: Signed comparison using .asSInt
 *   - BLTU/BGEU: Unsigned comparison using .asUInt
 * - JAL (J-type): Unconditional PC-relative jump, save PC+4 to rd
 * - JALR (I-type): Indirect jump to (rs1 + immediate) & ~1, save PC+4 to rd
 * - MRET: Return from trap, jump to mepc (handled by CLINT)
 *
 * ALU Operand Selection:
 * - Operand 1: Register (rs1) or PC (for branches, JAL, AUIPC)
 * - Operand 2: Register (rs2) or immediate (for I-type, S-type, U-type)
 *
 * Interface:
 * - Inputs: instruction, PC, register values, immediate, CSR read data
 * - Outputs: ALU result, CSR write data, jump flag, jump address
 *
 * Branch Penalty:
 * - Taken branches/jumps cause 1-cycle penalty (IF must restart)
 * - Interrupts cause 1-cycle penalty to vector to handler
 */
class Execute extends Module {
  val io = IO(new Bundle {
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))
    val reg1_data           = Input(UInt(Parameters.DataWidth))
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val immediate           = Input(UInt(Parameters.DataWidth))
    val aluop1_source       = Input(UInt(1.W))
    val aluop2_source       = Input(UInt(1.W))
    val csr_reg_read_data   = Input(UInt(Parameters.DataWidth))

    val mem_alu_result     = Output(UInt(Parameters.DataWidth))
    val csr_reg_write_data = Output(UInt(Parameters.DataWidth))
    val if_jump_flag       = Output(Bool())
    val if_jump_address    = Output(UInt(Parameters.DataWidth))
  })

  // Decode instruction fields
  val opcode = io.instruction(6, 0)
  val funct3 = io.instruction(14, 12)

  // Instantiate ALU and control logic
  val alu      = Module(new ALU)
  val alu_ctrl = Module(new ALUControl)

  alu_ctrl.io.opcode := opcode
  alu_ctrl.io.funct3 := funct3
  alu_ctrl.io.funct7 := io.instruction(31, 25)

  // Select ALU operands based on instruction type
  alu.io.func := alu_ctrl.io.alu_funct
  val aluOp1 = Mux(io.aluop1_source === ALUOp1Source.InstructionAddress, io.instruction_address, io.reg1_data)
  val aluOp2 = Mux(io.aluop2_source === ALUOp2Source.Immediate, io.immediate, io.reg2_data)
  alu.io.op1 := aluOp1
  alu.io.op2 := aluOp2

  io.mem_alu_result := alu.io.result

  // CSR write data: implements read-modify-write semantics for RISC-V CSR instructions
  val csrSourceImmediate = Cat(0.U((Parameters.DataBits - 5).W), io.instruction(19, 15))
  val csrSource          = Mux(funct3(2), csrSourceImmediate, io.reg1_data)
  val csrResult = MuxLookup(funct3, csrSource)(
    Seq(
      InstructionsTypeCSR.csrrw  -> csrSource,
      InstructionsTypeCSR.csrrwi -> csrSource,
      InstructionsTypeCSR.csrrs  -> (io.csr_reg_read_data | csrSource),
      InstructionsTypeCSR.csrrsi -> (io.csr_reg_read_data | csrSource),
      InstructionsTypeCSR.csrrc  -> (io.csr_reg_read_data & (~csrSource).asUInt),
      InstructionsTypeCSR.csrrci -> (io.csr_reg_read_data & (~csrSource).asUInt)
    )
  )
  io.csr_reg_write_data := csrResult

  // ============================================================
  // [CA25: Exercise 4] Branch Comparison Logic
  // ============================================================
  // Hint: Implement all six RV32I branch conditions
  //
  // Branch types:
  // - BEQ/BNE: Equality/inequality comparison (sign-agnostic)
  // - BLT/BGE: Signed comparison (requires type conversion)
  // - BLTU/BGEU: Unsigned comparison (direct comparison)
  val branchCondition = MuxLookup(funct3, false.B)(
    Seq(
      // TODO: Implement six branch conditions
      // Hint: Compare two register data values based on branch type
      InstructionsTypeB.beq  -> (io.reg1_data === io.reg2_data),
      InstructionsTypeB.bne  -> (io.reg1_data =/= io.reg2_data),

      // Signed comparison (need conversion to signed type)
      InstructionsTypeB.blt  -> (io.reg1_data.asSInt < io.reg2_data.asSInt),
      InstructionsTypeB.bge  -> (io.reg1_data.asSInt >= io.reg2_data.asSInt),

      // Unsigned comparison
      InstructionsTypeB.bltu -> (io.reg1_data < io.reg2_data),
      InstructionsTypeB.bgeu -> (io.reg1_data >= io.reg2_data)
    )
  )
  val isBranch = opcode === InstructionTypes.Branch
  val isJal    = opcode === Instructions.jal
  val isJalr   = opcode === Instructions.jalr

  // ============================================================
  // [CA25: Exercise 5] Jump Target Address Calculation
  // ============================================================
  // Hint: Calculate branch and jump target addresses
  //
  // Address calculation rules:
  // - Branch: PC + immediate (PC-relative)
  // - JAL: PC + immediate (PC-relative)
  // - JALR: (rs1 + immediate) & ~1 (register base, clear LSB for alignment)
  //
  // TODO: Complete the following address calculations
  val branchTarget = io.instruction_address + io.immediate

  val jalTarget    = branchTarget  // JAL and Branch use same calculation method

  // JALR address calculation:
  //   1. Add register value and immediate
  //   2. Clear LSB (2-byte alignment)
  val jalrSum      = io.reg1_data + io.immediate

  // TODO: Clear LSB using bit concatenation
  // Hint: Extract upper bits and append zero
  val jalrTarget   = Cat(jalrSum(31, 1), 0.U(1.W))

  val branchTaken = isBranch && branchCondition
  io.if_jump_flag    := branchTaken || isJal || isJalr
  io.if_jump_address := Mux(isJalr, jalrTarget, Mux(isJal, jalTarget, branchTarget))
}
