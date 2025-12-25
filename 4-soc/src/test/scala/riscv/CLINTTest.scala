// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.CLINT
import riscv.core.InstructionsEnv
import riscv.core.InstructionsRet
import riscv.core.InterruptStatus

class CLINTTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Core Local Interrupt Controller")

  def setupDefaultCSR(dut: CLINT, mie: Int = 0x888, mstatus: Int = 0x8): Unit = {
    dut.io.csr_bundle.mstatus.poke(mstatus.U) // MIE=1 (global enable)
    dut.io.csr_bundle.mie.poke(mie.U)
    dut.io.csr_bundle.mtvec.poke(0x100.U)
    dut.io.csr_bundle.mepc.poke(0x200.U)
    dut.io.csr_bundle.mcause.poke(0.U)
  }

  it should "not assert interrupt when no interrupt flag" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut)
      dut.io.interrupt_flag.poke(InterruptStatus.None)
      dut.io.instruction_id.poke(0.U)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(false.B)
      dut.io.csr_bundle.direct_write_enable.expect(false.B)
    }
  }

  it should "assert timer interrupt when flag set and enabled" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut, mie = 0x80) // MTIE (bit 7) set
      dut.io.interrupt_flag.poke(InterruptStatus.Timer0)
      dut.io.instruction_id.poke(0.U)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(true.B)
      dut.io.id_interrupt_handler_address.expect(0x100.U) // mtvec
      dut.io.csr_bundle.direct_write_enable.expect(true.B)
      dut.io.csr_bundle.mcause_write_data.expect(0x80000007L.U) // Timer interrupt
    }
  }

  it should "not assert timer interrupt when global interrupt disabled" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut, mstatus = 0) // MIE (bit 3) clear
      dut.io.interrupt_flag.poke(InterruptStatus.Timer0)
      dut.io.instruction_id.poke(0.U)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(false.B)
    }
  }

  it should "not assert timer interrupt when timer interrupt disabled in MIE" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut, mie = 0) // MTIE clear
      dut.io.interrupt_flag.poke(InterruptStatus.Timer0)
      dut.io.instruction_id.poke(0.U)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(false.B)
    }
  }

  it should "handle ECALL instruction correctly" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut)
      dut.io.interrupt_flag.poke(InterruptStatus.None)
      dut.io.instruction_id.poke(InstructionsEnv.ecall)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(true.B)
      dut.io.id_interrupt_handler_address.expect(0x100.U) // mtvec
      dut.io.csr_bundle.direct_write_enable.expect(true.B)
      dut.io.csr_bundle.mcause_write_data.expect(11.U) // ECALL from M-mode
      dut.io.csr_bundle.mepc_write_data.expect(0x1000.U)
    }
  }

  it should "handle EBREAK instruction correctly" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut)
      dut.io.interrupt_flag.poke(InterruptStatus.None)
      dut.io.instruction_id.poke(InstructionsEnv.ebreak)
      dut.io.instruction_address_if.poke(0x2000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(true.B)
      dut.io.csr_bundle.mcause_write_data.expect(3.U)
      dut.io.csr_bundle.mepc_write_data.expect(0x2000.U)
    }
  }

  it should "handle MRET instruction correctly" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut)
      dut.io.csr_bundle.mepc.poke(0x3000.U)
      dut.io.interrupt_flag.poke(InterruptStatus.None)
      dut.io.instruction_id.poke(InstructionsRet.mret)
      dut.io.instruction_address_if.poke(0x100.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.id_interrupt_assert.expect(true.B)
      dut.io.id_interrupt_handler_address.expect(0x3000.U)
      dut.io.csr_bundle.direct_write_enable.expect(true.B)
    }
  }

  it should "use jump_address when jump_flag is set for mepc" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut)
      dut.io.interrupt_flag.poke(InterruptStatus.None)
      dut.io.instruction_id.poke(InstructionsEnv.ecall)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(true.B)
      dut.io.jump_address.poke(0x5000.U)

      dut.clock.step()

      dut.io.csr_bundle.mepc_write_data.expect(0x5000.U)
    }
  }

  it should "disable interrupts in mstatus when handling interrupt" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.csr_bundle.mstatus.poke(0x8.U) // MIE=1
      dut.io.csr_bundle.mie.poke(0x80.U)
      dut.io.csr_bundle.mtvec.poke(0x100.U)
      dut.io.csr_bundle.mepc.poke(0.U)
      dut.io.csr_bundle.mcause.poke(0.U)
      dut.io.interrupt_flag.poke(InterruptStatus.Timer0)
      dut.io.instruction_id.poke(0.U)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      val mstatus_written = dut.io.csr_bundle.mstatus_write_data.peekInt()
      assert((mstatus_written & 0x8) == 0, s"MIE should be cleared: $mstatus_written")
    }
  }

  it should "prioritize ECALL over pending interrupt" in {
    test(new CLINT).withAnnotations(TestAnnotations.annos) { dut =>
      setupDefaultCSR(dut, mie = 0x80)
      dut.io.interrupt_flag.poke(InterruptStatus.Timer0)
      dut.io.instruction_id.poke(InstructionsEnv.ecall)
      dut.io.instruction_address_if.poke(0x1000.U)
      dut.io.jump_flag.poke(false.B)
      dut.io.jump_address.poke(0.U)

      dut.clock.step()

      dut.io.csr_bundle.mcause_write_data.expect(11.U) // ECALL priority
    }
  }
}
