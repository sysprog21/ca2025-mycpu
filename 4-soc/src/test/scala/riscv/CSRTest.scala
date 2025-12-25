// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.CSR
import riscv.core.CSRRegister

class CSRTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("CSR Performance Counters")

  it should "increment mcycle each clock cycle when not inhibited" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      // Ensure mcountinhibit bit 0 is clear (default)
      dut.io.reg_write_enable_ex.poke(false.B)
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)

      // Read initial cycle count
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleL)
      dut.clock.step()
      val cycle0 = dut.io.id_reg_read_data.peekInt()

      // Run 10 cycles
      dut.clock.step(10)

      // Read cycle count again
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleL)
      dut.clock.step()
      val cycle1 = dut.io.id_reg_read_data.peekInt()

      // mcycle should have incremented by ~11 cycles (10 steps + 1 for read)
      assert(cycle1 > cycle0, s"mcycle should increment: was $cycle0, now $cycle1")
      assert(cycle1 - cycle0 >= 10, s"mcycle should increment by at least 10: diff=${cycle1 - cycle0}")
    }
  }

  it should "inhibit mcycle when mcountinhibit bit 0 is set" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)

      // Set mcountinhibit bit 0 to inhibit mcycle
      dut.io.reg_write_enable_ex.poke(true.B)
      dut.io.reg_write_address_ex.poke(CSRRegister.MCOUNTINHIBIT)
      dut.io.reg_write_data_ex.poke(1.U) // Bit 0 = inhibit mcycle
      dut.clock.step()
      dut.io.reg_write_enable_ex.poke(false.B)

      // Read mcycle
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleL)
      dut.clock.step()
      val cycle0 = dut.io.id_reg_read_data.peekInt()

      // Run 10 cycles
      dut.clock.step(10)

      // Read mcycle again - should not have changed
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleL)
      dut.clock.step()
      val cycle1 = dut.io.id_reg_read_data.peekInt()

      assert(cycle1 == cycle0, s"mcycle should be inhibited: was $cycle0, now $cycle1")
    }
  }

  it should "count instruction retirements in minstret" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.reg_write_enable_ex.poke(false.B)
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)
      dut.io.instruction_retired.poke(false.B)

      // Read initial instret
      dut.io.reg_read_address_id.poke(CSRRegister.MInstretL)
      dut.clock.step()
      val instret0 = dut.io.id_reg_read_data.peekInt()

      // Simulate 5 instruction retirements
      for (_ <- 0 until 5) {
        dut.io.instruction_retired.poke(true.B)
        dut.clock.step()
      }
      dut.io.instruction_retired.poke(false.B)

      // Read instret again
      dut.io.reg_read_address_id.poke(CSRRegister.MInstretL)
      dut.clock.step()
      val instret1 = dut.io.id_reg_read_data.peekInt()

      assert(instret1 - instret0 == 5, s"minstret should increment by 5: diff=${instret1 - instret0}")
    }
  }

  it should "count branch mispredictions in mhpmcounter3" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.reg_write_enable_ex.poke(false.B)
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)
      dut.io.branch_misprediction.poke(false.B)

      // Read initial counter
      dut.io.reg_read_address_id.poke(CSRRegister.MHPMCounter3L)
      dut.clock.step()
      val cnt0 = dut.io.id_reg_read_data.peekInt()

      // Simulate 3 branch mispredictions
      for (_ <- 0 until 3) {
        dut.io.branch_misprediction.poke(true.B)
        dut.clock.step()
      }
      dut.io.branch_misprediction.poke(false.B)

      // Read counter again
      dut.io.reg_read_address_id.poke(CSRRegister.MHPMCounter3L)
      dut.clock.step()
      val cnt1 = dut.io.id_reg_read_data.peekInt()

      assert(cnt1 - cnt0 == 3, s"mhpmcounter3 should increment by 3: diff=${cnt1 - cnt0}")
    }
  }

  it should "count hazard stall cycles in mhpmcounter4" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.reg_write_enable_ex.poke(false.B)
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)
      dut.io.hazard_stall.poke(false.B)

      // Read initial counter
      dut.io.reg_read_address_id.poke(CSRRegister.MHPMCounter4L)
      dut.clock.step()
      val cnt0 = dut.io.id_reg_read_data.peekInt()

      // Simulate 4 hazard stall cycles
      dut.io.hazard_stall.poke(true.B)
      dut.clock.step(4)
      dut.io.hazard_stall.poke(false.B)

      // Read counter again
      dut.io.reg_read_address_id.poke(CSRRegister.MHPMCounter4L)
      dut.clock.step()
      val cnt1 = dut.io.id_reg_read_data.peekInt()

      assert(cnt1 - cnt0 == 4, s"mhpmcounter4 should increment by 4: diff=${cnt1 - cnt0}")
    }
  }

  it should "latch shadow register on low word read for atomic 64-bit access" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)

      // Write a known value to mcycle: high=0x12345678, low=0xABCD0000
      // This gives us control over the high word value
      dut.io.reg_write_enable_ex.poke(true.B)
      dut.io.reg_write_address_ex.poke(CSRRegister.MCycleH)
      dut.io.reg_write_data_ex.poke("h12345678".U)
      dut.clock.step()
      dut.io.reg_write_address_ex.poke(CSRRegister.MCycleL)
      dut.io.reg_write_data_ex.poke("hABCD0000".U)
      dut.clock.step()
      dut.io.reg_write_enable_ex.poke(false.B)

      // Read low word - this should latch high word (0x12345678) into shadow
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleL)
      dut.clock.step()
      val low = dut.io.id_reg_read_data.peekInt()

      // Now change the high word to something different (0xDEADBEEF)
      dut.io.reg_write_enable_ex.poke(true.B)
      dut.io.reg_write_address_ex.poke(CSRRegister.MCycleH)
      dut.io.reg_write_data_ex.poke("hDEADBEEF".U)
      dut.clock.step()
      dut.io.reg_write_enable_ex.poke(false.B)

      // Read high word - should return LATCHED shadow value (0x12345678),
      // not the newly written value (0xDEADBEEF)
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleH)
      dut.clock.step()
      val high = dut.io.id_reg_read_data.peekInt()

      // Verify atomic read: high should be the latched shadow (0x12345678)
      assert(high == 0x12345678L, f"Shadow should be latched: expected 0x12345678, got 0x$high%08X")
    }
  }

  it should "allow M-mode writes to mcycle" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)

      // Write a specific value to mcycle low
      dut.io.reg_write_enable_ex.poke(true.B)
      dut.io.reg_write_address_ex.poke(CSRRegister.MCycleL)
      dut.io.reg_write_data_ex.poke("hDEADBEEF".U)
      dut.clock.step()
      dut.io.reg_write_enable_ex.poke(false.B)

      // Read back
      dut.io.reg_read_address_id.poke(CSRRegister.MCycleL)
      dut.clock.step()
      val readback = dut.io.id_reg_read_data.peekInt()

      // Value should be DEADBEEF + some increment (mcycle keeps running)
      assert(
        (readback & 0xffff0000L) == 0xdead0000L || readback >= 0xdeadbeefL,
        f"mcycle should contain written value: 0x$readback%08X"
      )
    }
  }

  it should "respect mcountinhibit mask (only bits 0,2,3-9 writable)" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)

      // Try to write all bits
      dut.io.reg_write_enable_ex.poke(true.B)
      dut.io.reg_write_address_ex.poke(CSRRegister.MCOUNTINHIBIT)
      dut.io.reg_write_data_ex.poke("hFFFFFFFF".U)
      dut.clock.step()
      dut.io.reg_write_enable_ex.poke(false.B)

      // Read back
      dut.io.reg_read_address_id.poke(CSRRegister.MCOUNTINHIBIT)
      dut.clock.step()
      val readback = dut.io.id_reg_read_data.peekInt()

      // Only bits 0, 2, 3-9 should be set (mask 0x3fd)
      assert(readback == 0x3fdL, f"mcountinhibit should mask to 0x3fd: got 0x$readback%08X")
    }
  }

  it should "inhibit minstret when mcountinhibit bit 2 is set" in {
    test(new CSR).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.clint_access_bundle.direct_write_enable.poke(false.B)
      dut.io.instruction_retired.poke(false.B)

      // Set mcountinhibit bit 2 to inhibit minstret
      dut.io.reg_write_enable_ex.poke(true.B)
      dut.io.reg_write_address_ex.poke(CSRRegister.MCOUNTINHIBIT)
      dut.io.reg_write_data_ex.poke(4.U) // Bit 2 = inhibit minstret
      dut.clock.step()
      dut.io.reg_write_enable_ex.poke(false.B)

      // Read minstret
      dut.io.reg_read_address_id.poke(CSRRegister.MInstretL)
      dut.clock.step()
      val instret0 = dut.io.id_reg_read_data.peekInt()

      // Simulate 5 instruction retirements (should be ignored)
      for (_ <- 0 until 5) {
        dut.io.instruction_retired.poke(true.B)
        dut.clock.step()
      }
      dut.io.instruction_retired.poke(false.B)

      // Read minstret again - should not have changed
      dut.io.reg_read_address_id.poke(CSRRegister.MInstretL)
      dut.clock.step()
      val instret1 = dut.io.id_reg_read_data.peekInt()

      assert(instret1 == instret0, s"minstret should be inhibited: was $instret0, now $instret1")
    }
  }
}
