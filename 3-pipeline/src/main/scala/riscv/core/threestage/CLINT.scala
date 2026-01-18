// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.threestage

import chisel3._
import chisel3.util.MuxLookup
import riscv.Parameters

object InterruptStatus {
  val None   = 0x0.U(8.W)
  val Timer0 = 0x1.U(8.W)
  val Ret    = 0xff.U(8.W)
}

class CSRDirectAccessBundle extends Bundle {
  val mstatus = Input(UInt(Parameters.DataWidth))
  val mepc    = Input(UInt(Parameters.DataWidth))
  val mcause  = Input(UInt(Parameters.DataWidth))
  val mtvec   = Input(UInt(Parameters.DataWidth))
  val mie     = Input(UInt(Parameters.DataWidth))

  val mstatus_write_data = Output(UInt(Parameters.DataWidth))
  val mepc_write_data    = Output(UInt(Parameters.DataWidth))
  val mcause_write_data  = Output(UInt(Parameters.DataWidth))

  val direct_write_enable = Output(Bool())
}

// Core Local Interrupt Controller
class CLINT extends Module {
  val io = IO(new Bundle {
    val interrupt_flag = Input(UInt(Parameters.InterruptFlagWidth))

    val instruction_ex         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address_if = Input(UInt(Parameters.AddrWidth))
    val instruction_address_id = Input(UInt(Parameters.AddrWidth))

    val jump_flag    = Input(Bool())
    val jump_address = Input(UInt(Parameters.AddrWidth))

    val ex_interrupt_handler_address = Output(UInt(Parameters.AddrWidth))
    val ex_interrupt_assert          = Output(Bool())

    val csr_bundle = new CSRDirectAccessBundle
  })
  val interrupt_enable_global   = io.csr_bundle.mstatus(3) // MIE bit (global enable)
  val interrupt_enable_timer    = io.csr_bundle.mie(7)     // MTIE bit (timer enable)
  val interrupt_enable_external = io.csr_bundle.mie(11)    // MEIE bit (external enable)

  val jumpping = RegNext(io.jump_flag || io.ex_interrupt_assert)
  val instruction_address = Mux(
    io.jump_flag,
    io.jump_address,
    Mux(jumpping, io.instruction_address_if, io.instruction_address_id)
  )
  // Trap entry: Set MPP=0b11 (Machine mode), MPIE=MIE (save), MIE=0 (disable)
  val mstatus_disable_interrupt =
    io.csr_bundle.mstatus(31, 13) ## 3.U(2.W) ## io.csr_bundle.mstatus(10, 8) ## io.csr_bundle.mstatus(
      3
    ) ## io.csr_bundle.mstatus(6, 4) ## 0.U(1.W) ## io.csr_bundle.mstatus(2, 0)
  // Trap return: Set MPP=0b11 (Machine mode), MPIE=1, MIE=MPIE (restore)
  val mstatus_recover_interrupt =
    io.csr_bundle.mstatus(31, 13) ## 3.U(2.W) ## io.csr_bundle.mstatus(10, 8) ## 1
      .U(1.W) ## io.csr_bundle.mstatus(6, 4) ## io.csr_bundle.mstatus(7) ## io.csr_bundle.mstatus(2, 0)

  // Check individual interrupt source enable based on interrupt type
  val interrupt_source_enabled = Mux(
    io.interrupt_flag(0), // Timer interrupt (bit 0)
    interrupt_enable_timer,
    interrupt_enable_external
  )

  // Sequential trap handling logic to prevent glitches
  when(io.instruction_ex === InstructionsEnv.ecall || io.instruction_ex === InstructionsEnv.ebreak) {
    // Exception handling (ecall/ebreak)
    io.csr_bundle.mstatus_write_data := mstatus_disable_interrupt
    io.csr_bundle.mepc_write_data    := instruction_address
    io.csr_bundle.mcause_write_data := MuxLookup(
      io.instruction_ex,
      10.U
    )(
      IndexedSeq(
        InstructionsEnv.ecall  -> 11.U,
        InstructionsEnv.ebreak -> 3.U,
      )
    )
    io.csr_bundle.direct_write_enable := true.B
    io.ex_interrupt_assert            := true.B
    io.ex_interrupt_handler_address   := io.csr_bundle.mtvec
  }.elsewhen(io.interrupt_flag =/= InterruptStatus.None && interrupt_enable_global && interrupt_source_enabled) {
    // Interrupt handling (timer/external)
    io.csr_bundle.mstatus_write_data  := mstatus_disable_interrupt
    io.csr_bundle.mepc_write_data     := instruction_address
    io.csr_bundle.mcause_write_data   := Mux(io.interrupt_flag(0), 0x80000007L.U, 0x8000000bL.U)
    io.csr_bundle.direct_write_enable := true.B
    io.ex_interrupt_assert            := true.B
    io.ex_interrupt_handler_address   := io.csr_bundle.mtvec
  }.elsewhen(io.instruction_ex === InstructionsRet.mret) {
    // Return from trap (mret)
    io.csr_bundle.mstatus_write_data  := mstatus_recover_interrupt
    io.csr_bundle.mepc_write_data     := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data   := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := true.B
    io.ex_interrupt_assert            := true.B
    io.ex_interrupt_handler_address   := io.csr_bundle.mepc
  }.otherwise {
    // No trap - preserve existing CSR values
    io.csr_bundle.mstatus_write_data  := io.csr_bundle.mstatus
    io.csr_bundle.mepc_write_data     := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data   := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := false.B
    io.ex_interrupt_assert            := false.B
    io.ex_interrupt_handler_address   := 0.U
  }
}
