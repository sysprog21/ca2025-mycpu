// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._

// ALU Control: decodes instruction opcode/funct fields to determine ALU operation
//
// Maps RV32I instruction encoding to ALU functions:
// - OpImm (I-type): Immediate arithmetic/logical operations
// - Op (R-type): Register-register operations
// - Branch/Load/Store/JAL/JALR/LUI/AUIPC: Use ADD for address calculation
//
// Optimizations:
// - Default assignment to ADD reduces redundant case statements
// - Only OpImm and Op require complex decoding via funct3/funct7
class ALUControl extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val funct3 = Input(UInt(3.W))
    val funct7 = Input(UInt(7.W))

    val alu_funct = Output(ALUFunctions())
  })

  // ============================================================
  // [CA25: Exercise 3] ALU Control Logic - Opcode/Funct3/Funct7 Decoding
  // ============================================================
  // Hint: Determine which ALU operation to execute based on instruction's
  // opcode, funct3, and funct7
  //
  // RISC-V instruction encoding rules:
  // - OpImm (I-type): funct3 determines operation type
  //   - Special case: SRLI/SRAI distinguished by funct7[5]
  // - Op (R-type): funct3 determines operation type
  //   - Special cases: ADD/SUB and SRL/SRA distinguished by funct7[5]
  // - Other types: All use ADD (for address calculation or immediate loading)

  // Default ALU function for address calculation (Branch, Load, Store, JAL, JALR, LUI, AUIPC)
  io.alu_funct := ALUFunctions.add

  switch(io.opcode) {
    is(InstructionTypes.OpImm) {
      // I-type immediate operation instructions (ADDI, SLTI, XORI, ORI, ANDI, SLLI, SRLI, SRAI)
      io.alu_funct := MuxLookup(io.funct3, ALUFunctions.zero)(
        Seq(
          // TODO: Map funct3 to corresponding ALU operation
          // Hint: Refer to definitions in InstructionsTypeI object
          InstructionsTypeI.addi  -> ALUFunctions.add,  // Completed example
          InstructionsTypeI.slli  -> ALUFunctions.sll,
          InstructionsTypeI.slti  -> ALUFunctions.slt,
          InstructionsTypeI.sltiu -> ALUFunctions.sltu,

          // TODO: Complete the following mappings
          InstructionsTypeI.xori  -> ALUFunctions.xor,
          InstructionsTypeI.ori   -> ALUFunctions.or,
          InstructionsTypeI.andi  -> ALUFunctions.and,

          // SRLI/SRAI distinguished by funct7[5]:
          //   funct7[5] = 0 → SRLI (logical right shift)
          //   funct7[5] = 1 → SRAI (arithmetic right shift)
          // TODO: Complete Mux selection logic
          InstructionsTypeI.sri   -> Mux(io.funct7(5), ALUFunctions.sra, ALUFunctions.srl)
        )
      )
    }

    is(InstructionTypes.Op) {
      // R-type register operation instructions (ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND)
      io.alu_funct := MuxLookup(io.funct3, ALUFunctions.zero)(
        Seq(
          // ADD/SUB distinguished by funct7[5]:
          //   funct7[5] = 0 → ADD
          //   funct7[5] = 1 → SUB
          // TODO: Complete Mux selection logic
          InstructionsTypeR.add_sub -> Mux(io.funct7(5), ALUFunctions.sub, ALUFunctions.add),

          InstructionsTypeR.sll     -> ALUFunctions.sll,
          InstructionsTypeR.slt     -> ALUFunctions.slt,
          InstructionsTypeR.sltu    -> ALUFunctions.sltu,

          // TODO: Complete the following mappings
          InstructionsTypeR.xor     -> ALUFunctions.xor,
          InstructionsTypeR.or      -> ALUFunctions.or,
          InstructionsTypeR.and     -> ALUFunctions.and,

          // SRL/SRA distinguished by funct7[5]:
          //   funct7[5] = 0 → SRL (logical right shift)
          //   funct7[5] = 1 → SRA (arithmetic right shift)
          // TODO: Complete Mux selection logic
          InstructionsTypeR.sr      -> Mux(io.funct7(5), ALUFunctions.sra, ALUFunctions.srl)
        )
      )
    }
    // All other instruction types use ADD for address/immediate calculation
    // (Branch, Load, Store, JAL, JALR, LUI, AUIPC) - handled by default assignment above
  }
}
