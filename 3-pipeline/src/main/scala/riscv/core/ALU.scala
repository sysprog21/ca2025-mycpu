// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// ALU operation types supported by the processor
object ALUFunctions extends ChiselEnum {
  val zero, add, sub, sll, slt, xor, or, and, srl, sra, sltu = Value
}

// Arithmetic Logic Unit: performs arithmetic and logical operations
//
// Implements all RV32I ALU operations:
// - Arithmetic: ADD, SUB
// - Logical: AND, OR, XOR
// - Shift: SLL (logical left), SRL (logical right), SRA (arithmetic right)
// - Comparison: SLT (signed), SLTU (unsigned)
// - Special: ZERO (always outputs 0, used for non-ALU instructions)
//
// All operations are combinational, producing results in the same cycle.
class ALU extends Module {
  val io = IO(new Bundle {
    val func = Input(ALUFunctions())

    val op1 = Input(UInt(Parameters.DataWidth))
    val op2 = Input(UInt(Parameters.DataWidth))

    val result = Output(UInt(Parameters.DataWidth))
  })

  // ============================================================
  // [CA25: Exercise 16] ALU Operation Implementation - Basic Arithmetic and Logic
  // ============================================================
  // Hint: Implement all RV32I ALU operations
  //
  // Completed examples:
  // - add: Addition (op1 + op2)
  // - sub: Subtraction (op1 - op2)
  //
  // Students need to complete:
  // - Logical operations: xor, or, and
  // - Shift operations: sll, srl, sra
  // - Comparison operations: slt, sltu
  io.result := 0.U
  switch(io.func) {
    is(ALUFunctions.add) {
      io.result := io.op1 + io.op2
    }
    is(ALUFunctions.sub) {
      io.result := io.op1 - io.op2
    }

    // Shift Operations
    // Hint: RISC-V specifies that shift amount uses only low 5 bits (max shift 31 bits)
    is(ALUFunctions.sll) {
      // TODO: Implement logical left shift (Shift Left Logical)
      // Hint: Use shift left operator, only use low 5 bits of second operand
      io.result := io.op1 << io.op2(4, 0)
    }
    is(ALUFunctions.srl) {
      // TODO: Implement logical right shift (Shift Right Logical)
      // Hint: Use shift right operator, fill high bits with 0, only use low 5 bits
      io.result := io.op1 >> io.op2(4, 0)
    }
    is(ALUFunctions.sra) {
      // TODO: Implement arithmetic right shift (Shift Right Arithmetic)
      // Hint: Need to preserve sign bit, steps:
      //   1. Convert operand to signed type
      //   2. Perform arithmetic right shift
      //   3. Convert back to unsigned type
      io.result := (io.op1.asSInt >> io.op2(4, 0)).asUInt
    }

    // Comparison Operations
    //
    is(ALUFunctions.slt) {
      // TODO: Implement signed comparison (Set Less Than)
      // Hint: Convert both operands to signed type then compare
      //   If op1 < op2 (signed), result is 1, otherwise 0
      io.result := Mux(io.op1.asSInt < io.op2.asSInt, 1.U, 0.U)
    }
    is(ALUFunctions.sltu) {
      // TODO: Implement unsigned comparison (Set Less Than Unsigned)
      // Hint: Directly compare unsigned values
      //   If op1 < op2 (unsigned), result is 1, otherwise 0
      io.result := Mux(io.op1 < io.op2, 1.U, 0.U)
    }

    // Logical Operations
    //
    is(ALUFunctions.xor) {
      // TODO: Implement XOR operation
      io.result := io.op1 ^ io.op2
    }
    is(ALUFunctions.or) {
      // TODO: Implement OR operation
      io.result := io.op1 | io.op2
    }
    is(ALUFunctions.and) {
      // TODO: Implement AND operation
      io.result := io.op1 & io.op2
    }
  }
}
