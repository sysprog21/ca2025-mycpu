// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.MuxLookup
import riscv.Parameters

object InterruptStatus {
  val None   = 0x0.U(8.W)
  val Timer0 = 0x1.U(8.W)
  val Ret    = 0xff.U(8.W)
}

// Core Local Interrupt Controller
// CSRDirectAccessBundle is defined in CSR.scala
class CLINT extends Module {
  val io = IO(new Bundle {
    val interrupt_flag = Input(UInt(Parameters.InterruptFlagWidth))

    val instruction_id         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address_if = Input(UInt(Parameters.AddrWidth))

    val jump_flag    = Input(Bool())
    val jump_address = Input(UInt(Parameters.AddrWidth))

    val id_interrupt_handler_address = Output(UInt(Parameters.AddrWidth))
    val id_interrupt_assert          = Output(Bool())

    val csr_bundle = new CSRDirectAccessBundle
  })
  val interrupt_enable_global   = io.csr_bundle.mstatus(3) // MIE bit (global enable)
  val interrupt_enable_timer    = io.csr_bundle.mie(7)     // MTIE bit (timer enable)
  val interrupt_enable_external = io.csr_bundle.mie(11)    // MEIE bit (external enable)

  val instruction_address = Mux(
    io.jump_flag,
    io.jump_address,
    io.instruction_address_if,
  )
  // Trap entry: MIE (bit 3) -> MPIE (bit 7), then clear MIE
  val mstatus_disable_interrupt =
    io.csr_bundle.mstatus(31, 8) ## io.csr_bundle.mstatus(3) ## io.csr_bundle.mstatus(6, 4) ## 0.U(1.W) ## io.csr_bundle
      .mstatus(2, 0)
  // mret: MPIE (bit 7) -> MIE (bit 3), then set MPIE to 1
  val mstatus_recover_interrupt =
    io.csr_bundle.mstatus(31, 8) ## 1.U(1.W) ## io.csr_bundle.mstatus(6, 4) ## io.csr_bundle.mstatus(7) ## io.csr_bundle
      .mstatus(2, 0)

  // Check individual interrupt source enable based on interrupt type
  val interrupt_source_enabled = Mux(
    io.interrupt_flag(0), // Timer interrupt (bit 0)
    interrupt_enable_timer,
    interrupt_enable_external
  )

  when(io.instruction_id === InstructionsEnv.ecall || io.instruction_id === InstructionsEnv.ebreak) {
    io.csr_bundle.mstatus_write_data := mstatus_disable_interrupt
    io.csr_bundle.mepc_write_data    := instruction_address
    io.csr_bundle.mcause_write_data := MuxLookup(
      io.instruction_id,
      10.U
    )(
      IndexedSeq(
        InstructionsEnv.ecall  -> 11.U,
        InstructionsEnv.ebreak -> 3.U,
      )
    )
    io.csr_bundle.direct_write_enable := true.B
    io.id_interrupt_assert            := true.B
    io.id_interrupt_handler_address   := io.csr_bundle.mtvec
  }.elsewhen(io.interrupt_flag =/= InterruptStatus.None && interrupt_enable_global && interrupt_source_enabled) {
    io.csr_bundle.mstatus_write_data  := mstatus_disable_interrupt
    io.csr_bundle.mepc_write_data     := instruction_address
    io.csr_bundle.mcause_write_data   := Mux(io.interrupt_flag(0), 0x80000007L.U, 0x8000000bL.U)
    io.csr_bundle.direct_write_enable := true.B
    io.id_interrupt_assert            := true.B
    io.id_interrupt_handler_address   := io.csr_bundle.mtvec
  }.elsewhen(io.instruction_id === InstructionsRet.mret) {
    io.csr_bundle.mstatus_write_data  := mstatus_recover_interrupt
    io.csr_bundle.mepc_write_data     := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data   := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := true.B
    io.id_interrupt_assert            := true.B
    io.id_interrupt_handler_address   := io.csr_bundle.mepc
  }.otherwise {
    io.csr_bundle.mstatus_write_data  := io.csr_bundle.mstatus
    io.csr_bundle.mepc_write_data     := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data   := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := false.B
    io.id_interrupt_assert            := false.B
    io.id_interrupt_handler_address   := 0.U
  }
}
