// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class JITTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Minimal CPU - JIT Test")

  it should "correctly execute jit.asmbin and set a0 to 42" in {
    test(new TestTopModule("jit.asmbin")) { c =>
      // Run for enough cycles to complete JIT code execution
      // Program copies instructions to buffer, executes them, and sets a0=42
      for (i <- 1 to 50) {
        c.clock.step(1000)
        c.io.mem_debug_read_address.poke((i * 4).U)
      }

      // Verify a0 register (x10) = 42 via debug interface
      c.io.regs_debug_read_address.poke(10.U)
      c.clock.step()
      c.io.regs_debug_read_data.expect(42.U)
    }
  }
}
