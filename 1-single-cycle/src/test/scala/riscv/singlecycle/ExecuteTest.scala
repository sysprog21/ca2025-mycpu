// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.singlecycle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.ALUOp1Source
import riscv.core.ALUOp2Source
import riscv.core.Execute
import riscv.core.InstructionTypes
import riscv.TestAnnotations

class ExecuteTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Execute")
  it should "execute ALU operations and branch logic correctly" in {
    test(new Execute).withAnnotations(TestAnnotations.annos) { c =>
      // add test
      c.io.instruction.poke(0x001101b3L.U) // x3 =  x2 + x1
      // c.io.immediate.poke(0.U)
      // c.io.aluop1_source.poke(0.U)
      // c.io.aluop2_source.poke(0.U)

      var x = 0
      for (x <- 0 to 100) {
        val op1    = scala.util.Random.nextInt(429496729)
        val op2    = scala.util.Random.nextInt(429496729)
        val result = op1 + op2
        val addr   = scala.util.Random.nextInt(32)

        c.io.reg1_data.poke(op1.U)
        c.io.reg2_data.poke(op2.U)

        c.clock.step()
        c.io.mem_alu_result.expect(result.U)
        c.io.if_jump_flag.expect(0.U)
      }

      // beq test
      c.io.instruction.poke(0x00208163L.U) // pc + 2 if x1 === x2
      c.io.instruction_address.poke(2.U)
      c.io.immediate.poke(2.U)
      c.io.aluop1_source.poke(1.U)
      c.io.aluop2_source.poke(1.U)
      c.clock.step()

      // equ
      c.io.reg1_data.poke(9.U)
      c.io.reg2_data.poke(9.U)
      c.clock.step() // add
      c.io.if_jump_flag.expect(1.U)
      c.io.if_jump_address.expect(4.U)

      // not equ
      c.io.reg1_data.poke(9.U)
      c.io.reg2_data.poke(19.U)
      c.clock.step()
      c.io.if_jump_flag.expect(0.U)
      c.io.if_jump_address.expect(4.U)
    }
  }
}
