// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.singlecycle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.Execute
import riscv.TestAnnotations

class ExecuteTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("[Execute] CSR write-back")
  it should "produce correct data for csr write" in {
    test(new Execute).withAnnotations(TestAnnotations.annos) { c =>
      c.io.instruction.poke(0x30047073L.U) // csrc mstatus,3
      c.io.csr_reg_read_data.poke(0x1888L.U)
      c.io.reg1_data.poke(0x1880L.U)
      c.io.csr_reg_write_data.expect(0x1880.U)
      c.clock.step()
      c.io.instruction.poke(0x30046073L.U) // csrs mastatus,3
      c.io.csr_reg_read_data.poke(0x1880L.U)
      c.io.reg1_data.poke(0x1880L.U)
      c.io.csr_reg_write_data.expect(0x1888.U)
      c.clock.step()
      c.io.instruction.poke(0x30051073L.U) // csrw mstatus, a0
      c.io.csr_reg_read_data.poke(0.U)
      c.io.reg1_data.poke(0x1888L.U)
      c.io.csr_reg_write_data.expect(0x1888.U)
      c.clock.step()
      c.io.instruction.poke(0x30002573L.U) // csrr a0, mstatus
      c.io.csr_reg_read_data.poke(0x1888.U)
      c.io.reg1_data.poke(0x0L.U)
      c.io.csr_reg_write_data.expect(0x1888.U)
      c.clock.step()
    }
  }
}
