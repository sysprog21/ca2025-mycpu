// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.Cat
import chisel3.util.MuxLookup
import riscv.Parameters

// Interrupt cause codes for mcause register
object InterruptCode {
  val None   = 0x0.U(8.W)
  val Timer0 = 0x1.U(8.W)
  val Ret    = 0xff.U(8.W)
}

object InterruptEntry {
  val Timer0 = 0x4.U(8.W)
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

// Core Local Interrupt Controller: manages interrupt entry/exit and CSR state transitions
//
// Responsibilities:
// - Handle external interrupts from peripherals (timer, UART, etc.)
// - Manage trap entry for exceptions (ECALL, EBREAK)
// - Execute MRET (machine return) for trap exit
// - Update CSR state atomically during trap handling
//
// State Transitions:
// - Interrupt entry: Save PC to mepc, set mcause, disable interrupts (MIE=0), jump to mtvec
// - Exception entry: Same as interrupt, but mcause bit 31 = 0 (not interrupt)
// - MRET: Restore PC from mepc, re-enable interrupts (MIE=MPIE), set MPIE=1
//
// CSR Updates (mstatus):
// - Trap entry: MPIE←MIE, MIE←0 (save and disable interrupts)
// - MRET: MIE←MPIE, MPIE←1 (restore interrupts and set MPIE)
class CLINT extends Module {
  val io = IO(new Bundle {
    // Interrupt signals from peripherals
    val interrupt_flag = Input(UInt(Parameters.InterruptFlagWidth))

    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))

    val jump_flag    = Input(Bool())
    val jump_address = Input(UInt(Parameters.AddrWidth))

    val interrupt_handler_address = Output(UInt(Parameters.AddrWidth))
    val interrupt_assert          = Output(Bool())

    val csr_bundle = new CSRDirectAccessBundle
  })
  val interrupt_enable_global   = io.csr_bundle.mstatus(3) // MIE bit (global enable)
  val interrupt_enable_timer    = io.csr_bundle.mie(7)     // MTIE bit (timer enable)
  val interrupt_enable_external = io.csr_bundle.mie(11)    // MEIE bit (external enable)

  val instruction_address = Mux(
    io.jump_flag,
    io.jump_address,
    io.instruction_address + 4.U,
  )
  val mpie = io.csr_bundle.mstatus(7)
  val mie  = io.csr_bundle.mstatus(3)
  // val mpp = io.csr_bundle.mstatus(12, 11)  // Not used in M-mode only implementation

  // ============================================================
  // [CA25: Exercise 11] Interrupt Entry - mstatus State Transition
  // ============================================================
  // Hint: Implement mstatus register update during interrupt/trap entry
  //
  // mstatus bit positions (RISC-V Privileged Spec):
  // - Bit 3 (MIE): Machine Interrupt Enable (global interrupt enable)
  // - Bit 7 (MPIE): Machine Previous Interrupt Enable (saved MIE)
  //
  // Trap entry state transition:
  // 1. Save current interrupt enable: MPIE ← MIE (save before disabling)
  // 2. Disable interrupts: MIE ← 0 (prevent nested interrupts)
  // 3. Save return address: mepc ← instruction_address
  // 4. Record cause: mcause ← interrupt code (bit 31=1 for interrupts)
  // 5. Jump to handler: PC ← mtvec
  //
  // Example:
  // - Before: mstatus.MIE=1, mstatus.MPIE=? (don't care)
  // - After:  mstatus.MIE=0, mstatus.MPIE=1 (saved previous enable state)

  // Check individual interrupt source enable based on interrupt type
  val interrupt_source_enabled = Mux(
    io.interrupt_flag === InterruptCode.Timer0,
    interrupt_enable_timer,
    interrupt_enable_external
  )

  when(io.interrupt_flag =/= InterruptCode.None && interrupt_enable_global && interrupt_source_enabled) { // interrupt
    io.interrupt_assert          := true.B
    io.interrupt_handler_address := io.csr_bundle.mtvec
    // TODO: Complete mstatus update logic for interrupt entry
    // Hint: mstatus bit layout (showing only relevant bits):
    //   [31:13] | [12:11:MPP] | [10:8] | [7:MPIE] | [6:4] | [3:MIE] | [2:0]
    // Need to:
    // 1. Set MPP to 0b11 (Machine mode)
    // 2. Save current MIE to MPIE (bit 7)
    // 3. Clear MIE (bit 3) to disable interrupts
    io.csr_bundle.mstatus_write_data :=
      Cat(
        io.csr_bundle.mstatus(31, 13),
        3.U(2.W), // mpp ← 0b11 (Machine mode)
        io.csr_bundle.mstatus(10, 8),
        mie, // mpie ← mie (save current interrupt enable)
        io.csr_bundle.mstatus(6, 4),
        0.U(1.W), // mie ← 0 (disable interrupts)
        io.csr_bundle.mstatus(2, 0)
      )
    io.csr_bundle.mepc_write_data := instruction_address
    io.csr_bundle.mcause_write_data :=
      Cat(
        1.U,
        MuxLookup(
          io.interrupt_flag,
          11.U(31.W) // machine external interrupt
        )(
          IndexedSeq(
            InterruptCode.Timer0 -> 7.U(31.W),
          )
        )
      )
    io.csr_bundle.direct_write_enable := true.B
  }.elsewhen(io.instruction === InstructionsEnv.ebreak || io.instruction === InstructionsEnv.ecall) { // exception
    io.interrupt_assert          := true.B
    io.interrupt_handler_address := io.csr_bundle.mtvec
    io.csr_bundle.mstatus_write_data :=
      Cat(
        io.csr_bundle.mstatus(31, 13),
        3.U(2.W), // mpp ← 0b11 (Machine mode)
        io.csr_bundle.mstatus(10, 8),
        mie, // mpie
        io.csr_bundle.mstatus(6, 4),
        0.U(1.W), // mie
        io.csr_bundle.mstatus(2, 0)
      )
    io.csr_bundle.mepc_write_data := instruction_address
    io.csr_bundle.mcause_write_data := Cat(
      0.U,
      MuxLookup(io.instruction, 0.U)(
        IndexedSeq(
          InstructionsEnv.ebreak -> 3.U(31.W),
          InstructionsEnv.ecall  -> 11.U(31.W),
        )
      )
    )
    io.csr_bundle.direct_write_enable := true.B
    // ============================================================
    // [CA25: Exercise 12] Trap Return (MRET) - mstatus State Restoration
    // ============================================================
    // Hint: Implement mstatus register update during trap return (MRET instruction)
    //
    // MRET (Machine Return) state transition:
    // 1. Restore interrupt enable: MIE ← MPIE (restore saved state)
    // 2. Set MPIE to 1: MPIE ← 1 (spec requires MPIE=1 after MRET)
    // 3. Return to saved PC: PC ← mepc
    //
    // This is the inverse of trap entry:
    // - Trap entry: MPIE←MIE, MIE←0 (save and disable)
    // - MRET: MIE←MPIE, MPIE←1 (restore and reset)
    //
    // Example:
    // - Before MRET: mstatus.MIE=0, mstatus.MPIE=1 (in trap handler)
    // - After MRET:  mstatus.MIE=1, mstatus.MPIE=1 (interrupts re-enabled)
  }.elsewhen(io.instruction === InstructionsRet.mret) { // ret
    io.interrupt_assert          := true.B
    io.interrupt_handler_address := io.csr_bundle.mepc
    // TODO: Complete mstatus update logic for MRET
    // Hint: mstatus bit layout (showing only relevant bits):
    //   [31:13] | [12:11:MPP] | [10:8] | [7:MPIE] | [6:4] | [3:MIE] | [2:0]
    // Need to:
    // 1. Set MPP to 0b11 (Machine mode, for M-mode only systems)
    // 2. Set MPIE to 1 (bit 7)
    // 3. Restore MIE from MPIE (bit 3 ← bit 7)
    io.csr_bundle.mstatus_write_data :=
      Cat(
        io.csr_bundle.mstatus(31, 13),
        3.U(2.W), // mpp ← 0b11 (Machine mode)
        io.csr_bundle.mstatus(10, 8),
        1.U(1.W), // mpie ← 1 (reset MPIE)
        io.csr_bundle.mstatus(6, 4),
        mpie, // mie ← mpie (restore interrupt enable)
        io.csr_bundle.mstatus(2, 0)
      )
    io.csr_bundle.mepc_write_data     := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data   := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := true.B
  }.otherwise {
    io.interrupt_assert               := false.B
    io.interrupt_handler_address      := io.csr_bundle.mtvec
    io.csr_bundle.mstatus_write_data  := io.csr_bundle.mstatus
    io.csr_bundle.mepc_write_data     := io.csr_bundle.mepc
    io.csr_bundle.mcause_write_data   := io.csr_bundle.mcause
    io.csr_bundle.direct_write_enable := false.B
  }
  // io.interrupt_handler_address := io.csr_bundle.mepc
}
