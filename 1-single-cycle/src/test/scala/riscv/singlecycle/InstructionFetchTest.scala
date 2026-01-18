// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.singlecycle

import scala.math.pow
import scala.util.Random

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.ALUOp1Source
import riscv.core.ALUOp2Source
import riscv.core.InstructionFetch
import riscv.core.InstructionTypes
import riscv.core.ProgramCounter
import riscv.Parameters
import riscv.TestAnnotations

class InstructionFetchTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("InstructionFetch")
  it should "correctly update PC and handle jumps" in {
    test(new InstructionFetch).withAnnotations(TestAnnotations.annos) { c =>
      val entry = 0x1000
      var pre   = entry
      var cur   = pre
      c.io.instruction_valid.poke(true.B)
      var x = 0
      for (x <- 0 to 100) {
        Random.nextInt(2) match {
          case 0 => // no jump
            cur = pre + 4
            c.io.jump_flag_id.poke(false.B)
            c.clock.step()
            c.io.instruction_address.expect(cur)
            pre = pre + 4
          case 1 => // jump
            c.io.jump_flag_id.poke(true.B)
            c.io.jump_address_id.poke(entry)
            c.clock.step()
            c.io.instruction_address.expect(entry)
            pre = entry
        }
      }

    }
  }
}
