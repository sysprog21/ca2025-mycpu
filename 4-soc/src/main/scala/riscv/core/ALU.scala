// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * ALU function encoding. Maps to RV32I instruction funct3/funct7 combinations.
 *
 * The ALUControl module translates instruction encoding to these operations.
 * Special case 'zero' produces constant 0 for non-ALU instructions that still
 * flow through the execute stage (e.g., stores use ALU for address calculation
 * but don't need a separate zero output).
 */
object ALUFunctions extends ChiselEnum {
  val zero, add, sub, sll, slt, xor, or, and, srl, sra, sltu = Value
}

/**
 * Arithmetic Logic Unit: combinational compute engine for the Execute stage.
 *
 * Implements all RV32I integer operations in a single cycle:
 * - Arithmetic: ADD (also used for address calculation), SUB
 * - Logical: AND, OR, XOR (bitwise operations)
 * - Shift: SLL (left), SRL (logical right), SRA (arithmetic right, sign-extends)
 * - Compare: SLT (signed), SLTU (unsigned) - output 1 if op1 < op2, else 0
 *
 * Shift amounts use only lower 5 bits of op2 (RISC-V spec: shamt[4:0]).
 * Comparison results are 1-bit values zero-extended to 32 bits.
 *
 * Critical path: SLT/SLTU comparisons and SRA (requires sign extension logic).
 */
class ALU extends Module {
  val io = IO(new Bundle {
    val func = Input(ALUFunctions())

    val op1 = Input(UInt(Parameters.DataWidth))
    val op2 = Input(UInt(Parameters.DataWidth))

    val result = Output(UInt(Parameters.DataWidth))
  })

  io.result := 0.U
  switch(io.func) {
    is(ALUFunctions.add) {
      io.result := io.op1 + io.op2
    }
    is(ALUFunctions.sub) {
      io.result := io.op1 - io.op2
    }
    is(ALUFunctions.sll) {
      io.result := io.op1 << io.op2(4, 0)
    }
    is(ALUFunctions.slt) {
      io.result := io.op1.asSInt < io.op2.asSInt
    }
    is(ALUFunctions.xor) {
      io.result := io.op1 ^ io.op2
    }
    is(ALUFunctions.or) {
      io.result := io.op1 | io.op2
    }
    is(ALUFunctions.and) {
      io.result := io.op1 & io.op2
    }
    is(ALUFunctions.srl) {
      io.result := io.op1 >> io.op2(4, 0)
    }
    is(ALUFunctions.sra) {
      io.result := (io.op1.asSInt >> io.op2(4, 0)).asUInt
    }
    is(ALUFunctions.sltu) {
      io.result := io.op1 < io.op2
    }
  }
}
