// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import peripheral.Timer

class TimerTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Timer Peripheral")

  it should "not signal interrupt when count < limit" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(false.B)

      dut.clock.step()

      dut.io.signal_interrupt.expect(false.B)
    }
  }

  it should "signal interrupt when count >= limit and enabled" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      // Set limit=5, counter increments each cycle
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_data.poke(5.U)
      dut.clock.step() // count: 0->1, limit: ->5
      dut.io.bundle.write_enable.poke(false.B)

      dut.clock.step() // 1->2
      dut.io.signal_interrupt.expect(false.B)
      dut.clock.step() // 2->3
      dut.io.signal_interrupt.expect(false.B)
      dut.clock.step() // 3->4
      dut.io.signal_interrupt.expect(false.B)
      dut.clock.step() // 4->5, interrupt fires
      dut.io.signal_interrupt.expect(true.B)
    }
  }

  it should "reset counter when reaching limit" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_data.poke(3.U)
      dut.clock.step() // count: 0->1, limit: ->3
      dut.io.bundle.write_enable.poke(false.B)

      dut.clock.step() // 1->2
      dut.io.signal_interrupt.expect(false.B)
      dut.clock.step() // 2->3, interrupt fires
      dut.io.signal_interrupt.expect(true.B)
      dut.clock.step() // 3->0 (reset)
      dut.io.signal_interrupt.expect(false.B)
    }
  }

  it should "not signal interrupt when disabled" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_data.poke(2.U)
      dut.clock.step()

      dut.io.bundle.address.poke(0x8.U) // Disable
      dut.io.bundle.write_data.poke(0.U)
      dut.clock.step()
      dut.io.bundle.write_enable.poke(false.B)

      dut.clock.step(5)
      dut.io.signal_interrupt.expect(false.B)
    }
  }

  it should "read limit register correctly" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_data.poke(12345.U)
      dut.clock.step()
      dut.io.bundle.write_enable.poke(false.B)

      dut.io.bundle.address.poke(0x4.U)
      dut.clock.step()
      dut.io.bundle.read_data.expect(12345.U)
    }
  }

  it should "read enable register correctly" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.address.poke(0x8.U)
      dut.io.bundle.write_enable.poke(false.B)
      dut.clock.step()
      dut.io.bundle.read_data.expect(1.U) // Default enabled

      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.write_data.poke(0.U)
      dut.clock.step()
      dut.io.bundle.write_enable.poke(false.B)

      dut.clock.step()
      dut.io.bundle.read_data.expect(0.U)
    }
  }

  it should "expose debug signals correctly" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_data.poke(999.U)
      dut.clock.step()
      dut.io.bundle.write_enable.poke(false.B)

      dut.io.debug_limit.expect(999.U)
      dut.io.debug_enabled.expect(true.B)
    }
  }

  it should "generate periodic interrupts" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_data.poke(4.U)
      dut.clock.step()
      dut.io.bundle.write_enable.poke(false.B)

      var interruptCount = 0
      for (_ <- 0 until 15) {
        if (dut.io.signal_interrupt.peekBoolean()) interruptCount += 1
        dut.clock.step()
      }
      assert(interruptCount >= 2, s"Expected periodic interrupts, got $interruptCount")
    }
  }

  it should "return 0 for unknown addresses" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.write_enable.poke(false.B)
      dut.io.bundle.address.poke(0x10.U)
      dut.clock.step()
      dut.io.bundle.read_data.expect(0.U)
    }
  }

  it should "ignore writes to unknown addresses" in {
    test(new Timer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.bundle.address.poke(0x4.U)
      dut.io.bundle.write_enable.poke(false.B)
      dut.clock.step()
      val initialLimit = dut.io.bundle.read_data.peekInt()

      dut.io.bundle.address.poke(0x10.U)
      dut.io.bundle.write_enable.poke(true.B)
      dut.io.bundle.write_data.poke(0xdead.U)
      dut.clock.step()
      dut.io.bundle.write_enable.poke(false.B)

      dut.io.bundle.address.poke(0x4.U)
      dut.clock.step()
      dut.io.bundle.read_data.expect(initialLimit.U)
    }
  }
}
