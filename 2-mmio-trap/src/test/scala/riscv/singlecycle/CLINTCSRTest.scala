// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.singlecycle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.ALUOp1Source
import riscv.core.ALUOp2Source
import riscv.core.CLINT
import riscv.core.CSR
import riscv.core.CSRRegister
import riscv.core.InstructionDecode
import riscv.core.InstructionsEnv
import riscv.core.InstructionsNop
import riscv.core.InstructionsRet
import riscv.core.InterruptCode
import riscv.Parameters
import riscv.TestAnnotations

class CLINTCSRTestTopModule extends Module {
  val io = IO(new Bundle {
    val csr_regs_debug_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val csr_regs_write_address      = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val csr_regs_write_data         = Input(UInt(Parameters.DataWidth))
    val csr_regs_write_enable       = Input(Bool())
    val interrupt_flag              = Input(UInt(Parameters.InterruptFlagWidth))
    val instruction                 = Input(UInt(Parameters.DataWidth))
    val instruction_address         = Input(UInt(Parameters.AddrWidth))

    val jump_flag    = Input(Bool())
    val jump_address = Input(UInt(Parameters.AddrWidth))

    val interrupt_assert          = Output(Bool())
    val interrupt_handler_address = Output(UInt(Parameters.DataWidth))
    val csr_regs_read_data        = Output(UInt(Parameters.DataWidth))
    val csr_regs_debug_read_data  = Output(UInt(Parameters.DataWidth))
  })
  val csr_regs = Module(new CSR)
  val clint    = Module(new CLINT)

  clint.io.instruction         := io.instruction
  clint.io.instruction_address := io.instruction_address
  clint.io.interrupt_flag      := io.interrupt_flag
  clint.io.jump_flag           := io.jump_flag
  clint.io.jump_address        := io.jump_address

  io.interrupt_handler_address       := clint.io.interrupt_handler_address
  io.interrupt_assert                := clint.io.interrupt_assert
  io.csr_regs_read_data              := csr_regs.io.reg_read_data
  csr_regs.io.reg_write_address_id   := io.csr_regs_write_address
  csr_regs.io.debug_reg_read_address := io.csr_regs_debug_read_address
  csr_regs.io.reg_write_data_ex      := io.csr_regs_write_data
  csr_regs.io.reg_write_enable_id    := io.csr_regs_write_enable
  csr_regs.io.reg_read_address_id    := io.csr_regs_write_address
  io.csr_regs_debug_read_data        := csr_regs.io.debug_reg_read_data

  csr_regs.io.clint_access_bundle <> clint.io.csr_bundle

}

class CLINTCSRTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("[CLINT] Machine-mode interrupt flow")
  it should "handle external interrupt" in {
    test(new CLINTCSRTestTopModule).withAnnotations(TestAnnotations.annos) { c =>
      c.io.jump_flag.poke(false.B)
      c.io.csr_regs_write_enable.poke(false.B)
      c.io.interrupt_flag.poke(0.U)
      c.clock.step()
      c.io.csr_regs_write_enable.poke(true.B)
      c.io.csr_regs_write_address.poke(CSRRegister.MTVEC)
      c.io.csr_regs_write_data.poke(0x1144L.U)
      c.clock.step()
      c.io.csr_regs_write_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_write_data.poke(0x1888L.U)
      c.clock.step()
      c.io.csr_regs_write_address.poke(CSRRegister.MIE)
      c.io.csr_regs_write_data.poke(0x888L.U) // MTIE (bit 7) | MEIE (bit 11)
      c.clock.step()
      c.io.csr_regs_write_enable.poke(false.B)

      // handle interrupt when not jumping
      c.io.jump_flag.poke(false.B)
      c.io.instruction_address.poke(0x1900L.U)
      c.io.instruction.poke(InstructionsNop.nop)
      c.io.interrupt_flag.poke(InterruptCode.Timer0)

      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(0x1144L.U)
      c.clock.step()
      c.io.interrupt_flag.poke(InterruptCode.None)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MEPC)
      c.io.csr_regs_debug_read_data.expect(0x1904L.U)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MCAUSE)
      c.io.csr_regs_debug_read_data.expect(0x80000007L.U)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_debug_read_data.expect(0x1880L.U)

      c.clock.step(25)

      // mret from interrupt handler
      c.io.instruction.poke(InstructionsRet.mret)
      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(0x1904L.U)
      c.clock.step()
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_debug_read_data.expect(0x1888L.U)

      // handle interrupt when jumping
      c.io.jump_flag.poke(true.B)
      c.io.jump_address.poke(0x1990L.U)
      c.io.interrupt_flag.poke(2.U)
      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(0x1144L.U)
      c.clock.step()
      c.io.interrupt_flag.poke(InterruptCode.None)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MEPC)
      c.io.csr_regs_debug_read_data.expect(0x1990L.U)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MCAUSE)
      c.io.csr_regs_debug_read_data.expect(0x8000000bL.U)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_debug_read_data.expect(0x1880L.U)

      c.clock.step(25)

      // mret from interrupt handler
      c.io.instruction.poke(InstructionsRet.mret)
      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(0x1990L.U)
      c.clock.step()
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_debug_read_data.expect(0x1888L.U)
      c.io.instruction.poke(InstructionsNop.nop)

      // don't handle interrupt under certain situation
      c.io.csr_regs_write_enable.poke(true.B)
      c.io.csr_regs_write_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_write_data.poke(0x1880L.U)
      c.io.interrupt_flag.poke(1.U)
      c.clock.step() // ? without this it wont work
      c.io.interrupt_assert.expect(false.B)
    }
  }

  it should "handle environmental instructions" in {
    test(new CLINTCSRTestTopModule).withAnnotations(TestAnnotations.annos) { c =>
      val interrupt_entry = 0x1144L.U

      c.io.jump_flag.poke(false.B)
      c.io.csr_regs_write_enable.poke(true.B)
      c.io.csr_regs_write_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_write_data.poke(0x1888L.U) // write mstatus MIE = 1, MPIE = 1
      c.clock.step()
      c.io.csr_regs_write_address.poke(CSRRegister.MTVEC)
      c.io.csr_regs_write_data.poke(interrupt_entry) // write mtvec to 0x1144
      c.clock.step()
      c.io.csr_regs_write_enable.poke(false.B)

      // ecall
      c.io.instruction.poke(InstructionsEnv.ecall)
      c.io.instruction_address.poke(0x2000L.U)
      c.io.interrupt_flag.poke(InterruptCode.None)
      c.clock.step() // poking ecall instruction at address 0x2000, expect CLINT to handle it

      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(interrupt_entry)

      c.io.csr_regs_debug_read_address.poke(CSRRegister.MEPC)
      c.io.csr_regs_debug_read_data.expect(0x2004L.U)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MCAUSE)
      c.io.csr_regs_debug_read_data.expect(0x0000_000bL.U) // ecall from M-mode
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_debug_read_data.expect(0x1880L.U)

      // mret from ecall handler
      c.io.instruction.poke(InstructionsRet.mret)
      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(0x2004L.U)
      c.clock.step()
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MSTATUS)
      c.io.csr_regs_debug_read_data.expect(0x1888L.U)

      // ebreak
      c.clock.step()
      c.io.instruction.poke(InstructionsEnv.ebreak)
      c.io.instruction_address.poke(0x2004L.U)
      c.clock.step()

      c.io.interrupt_assert.expect(true.B)
      c.io.interrupt_handler_address.expect(interrupt_entry)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MEPC)
      c.io.csr_regs_debug_read_data.expect(0x2008L.U)
      c.io.csr_regs_debug_read_address.poke(CSRRegister.MCAUSE)
      c.io.csr_regs_debug_read_data.expect(0x0000_0003L.U) // breakpoint
    }
  }

}
