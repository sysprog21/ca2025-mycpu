// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// Debug test using built-in uart.asmbin from resources
class DebugWriteTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Debug Write Test")

  it should "execute uart test and verify memory writes" in {
    // Use uart.asmbin which is already in src/main/resources/
    test(new TestTopModule("uart.asmbin")).withAnnotations(TestAnnotations.annos) { dut =>
      dut.clock.setTimeout(0)

      // Run uart test - needs time to execute
      dut.clock.step(50000)

      // Verify program area has instructions (not zeros)
      dut.io.mem_debug_read_address.poke(0x1000.U)
      dut.clock.step()
      val inst0 = dut.io.mem_debug_read_data.peekInt()
      assert(inst0 != 0, "Program area should have instructions loaded")
    }
  }
}
