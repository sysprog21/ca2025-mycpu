// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// RISC-V Machine-mode CSR addresses (Privileged Spec Vol.II)
object CSRRegister {
  val MSTATUS  = 0x300.U(Parameters.CSRRegisterAddrWidth)
  val MIE      = 0x304.U(Parameters.CSRRegisterAddrWidth)
  val MTVEC    = 0x305.U(Parameters.CSRRegisterAddrWidth)
  val MSCRATCH = 0x340.U(Parameters.CSRRegisterAddrWidth)
  val MEPC     = 0x341.U(Parameters.CSRRegisterAddrWidth)
  val MCAUSE   = 0x342.U(Parameters.CSRRegisterAddrWidth)
  val CycleL   = 0xc00.U(Parameters.CSRRegisterAddrWidth)
  val CycleH   = 0xc80.U(Parameters.CSRRegisterAddrWidth)
}

/**
 * CSR: Control and Status Registers for machine-mode privilege operations
 *
 * Implements RISC-V Privileged Specification v1.10 machine-mode CSRs for
 * trap handling, interrupt control, and status management.
 *
 * Implemented CSRs:
 * - mstatus (0x300): Machine status register
 *   - MIE bit (bit 3): Global machine interrupt enable
 *   - MPIE bit (bit 7): Prior interrupt enable (saved on trap entry)
 * - mie (0x304): Machine interrupt enable register
 *   - MTIE bit (bit 7): Timer interrupt enable
 *   - MEIE bit (bit 11): External interrupt enable
 * - mtvec (0x305): Machine trap vector base address
 *   - MODE bits [1:0]: Always 0 (direct mode, no vectoring)
 *   - BASE bits [31:2]: Trap handler address (4-byte aligned)
 * - mscratch (0x340): Machine scratch register for trap handler context
 * - mepc (0x341): Machine exception program counter (return address after trap)
 * - mcause (0x342): Machine trap cause
 *   - Interrupt bit (bit 31): 1=interrupt, 0=exception
 *   - Exception code bits [30:0]: Identifies specific trap cause
 * - cycle (0xC00): Lower 32 bits of 64-bit cycle counter (read-only)
 * - cycleh (0xC80): Upper 32 bits of 64-bit cycle counter (read-only)
 *
 * Write Priority and Atomic Operations:
 * When both CLINT and CPU pipeline attempt to write CSRs simultaneously,
 * CLINT writes take priority to ensure atomic trap entry.
 *
 * Implementation (lines 138-159):
 * 1. Trap-managed CSRs (mstatus, mepc, mcause) - lines 138-150:
 *    - when(CLINT direct_write_enable): CLINT writes during trap entry
 *    - elsewhen(CPU write_enable): CPU CSR instructions (only if CLINT idle)
 *    - CLINT priority ensures trap state cannot be corrupted
 *
 * 2. CPU-only CSRs (mie, mtvec, mscratch) - lines 152-159:
 *    - when(CPU write_enable): Always CPU-controlled
 *    - CLINT never writes these registers
 *    - Separate when block, no priority arbitration needed
 *
 * Atomic Guarantees:
 * - Single-cycle execution ensures no race conditions within module
 * - CLINT priority prevents partial trap state updates
 * - Read operations see consistent state due to combinational read paths
 *
 * Interrupt Handling Sequence:
 * 1. CLINT detects interrupt condition and asserts direct_write_enable
 * 2. CSR module atomically updates:
 *    - mstatus: Save MIE to MPIE, clear MIE (disable interrupts)
 *    - mepc: Save current PC (return address)
 *    - mcause: Record interrupt cause code
 * 3. InstructionFetch vectors PC to mtvec handler address
 * 4. Trap handler executes, eventually issues MRET
 * 5. MRET restores MIE from MPIE and returns to mepc
 *
 * Interface:
 * - reg_read_address_id: CSR address for read (ID stage)
 * - reg_write_enable_id: Enable CSR write (ID stage decode)
 * - reg_write_address_id: CSR address for write (ID stage)
 * - reg_write_data_ex: CSR write data (EX stage computed)
 * - clint_access_bundle: Bidirectional CLINT interface for trap handling
 * - debug_reg_read_address/data: Debug interface for testbench inspection
 *
 * Implementation Notes:
 * - Cycle counter increments every clock cycle (64-bit, no overflow handling)
 * - Read operations are combinational (MuxLookup from register values)
 * - Write operations are registered (take effect next cycle)
 * - Reset values: All CSRs initialized to 0 (interrupts disabled at reset)
 */
class CSR extends Module {
  val io = IO(new Bundle {
    val reg_read_address_id    = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val reg_write_enable_id    = Input(Bool())
    val reg_write_address_id   = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val reg_write_data_ex      = Input(UInt(Parameters.DataWidth))
    val debug_reg_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))

    val debug_reg_read_data = Output(UInt(Parameters.DataWidth))
    val reg_read_data       = Output(UInt(Parameters.DataWidth))

    val clint_access_bundle = Flipped(new CSRDirectAccessBundle)
  })

  val mstatus  = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mie      = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mtvec    = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mscratch = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mepc     = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mcause   = RegInit(UInt(Parameters.DataWidth), 0.U)
  val cycles   = RegInit(UInt(64.W), 0.U)
  // ============================================================
  // [CA25: Exercise 10] CSR Register Lookup Table - CSR Address Mapping
  // ============================================================
  // Hint: Map CSR addresses to corresponding registers
  //
  // CSR addresses defined in CSRRegister object:
  // - MSTATUS (0x300): Machine status register
  // - MIE (0x304): Machine interrupt enable register
  // - MTVEC (0x305): Machine trap vector base address
  // - MSCRATCH (0x340): Machine scratch register
  // - MEPC (0x341): Machine exception program counter
  // - MCAUSE (0x342): Machine trap cause
  // - CycleL (0xC00): Cycle counter low 32 bits
  // - CycleH (0xC80): Cycle counter high 32 bits
  val regLUT =
    IndexedSeq(
      // TODO: Complete CSR address to register mapping
      CSRRegister.MSTATUS  -> mstatus,
      CSRRegister.MIE      -> mie,
      CSRRegister.MTVEC    -> mtvec,
      CSRRegister.MSCRATCH -> mscratch,
      CSRRegister.MEPC     -> mepc,
      CSRRegister.MCAUSE   -> mcause,

      // 64-bit cycle counter split into high and low 32 bits
      // TODO: Extract low 32 bits and high 32 bits from cycles
      CSRRegister.CycleL   -> cycles(31, 0),
      CSRRegister.CycleH   -> cycles(63, 32),
    )
  cycles := cycles + 1.U

  // If the pipeline and the CLINT are going to write the CSR at the same time, CLINT writes take priority.
  // Interrupt entry (CLINT) must override normal CSR writes to properly handle traps.
  io.reg_read_data       := MuxLookup(io.reg_read_address_id, 0.U)(regLUT)
  io.debug_reg_read_data := MuxLookup(io.debug_reg_read_address, 0.U)(regLUT)

  // what data should be passed from csr to clint (Note: what should clint see is the next state of the CPU)
  io.clint_access_bundle.mstatus := mstatus
  io.clint_access_bundle.mtvec   := mtvec
  io.clint_access_bundle.mcause  := mcause
  io.clint_access_bundle.mepc    := mepc
  io.clint_access_bundle.mie     := mie

  // ============================================================
  // [CA25: Exercise 11] CSR Write Priority Logic
  // ============================================================
  // Hint: Handle priority when both CLINT and CPU write to CSRs simultaneously
  //
  // Write priority rules:
  // 1. CLINT direct write (interrupt handling): Highest priority
  // 2. CPU CSR instruction write: Secondary priority
  //
  // CSRs requiring atomic update (interrupt-related):
  // - mstatus: Save/restore interrupt enable state
  // - mepc: Save exception return address
  // - mcause: Record trap cause
  when(io.clint_access_bundle.direct_write_enable) {
    // Atomic update when CLINT triggers interrupt
    // TODO: Which CSRs does CLINT need to write?
    mstatus := io.clint_access_bundle.mstatus_write_data
    mepc := io.clint_access_bundle.mepc_write_data
    mcause := io.clint_access_bundle.mcause_write_data
  }.elsewhen(io.reg_write_enable_id) {
    // CPU CSR instruction write
    // TODO: Update corresponding CSR based on write address
    when(io.reg_write_address_id === CSRRegister.MSTATUS) {
      mstatus := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_id === CSRRegister.MEPC) {
      mepc := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_id === CSRRegister.MCAUSE) {
      mcause := io.reg_write_data_ex
    }
  }

  // CPU-exclusive CSRs (CLINT never writes these):
  // - mie: Machine interrupt enable bits
  // - mtvec: Machine trap vector base address
  // - mscratch: Machine scratch register for trap handlers
  when(io.reg_write_enable_id) {
    // TODO: Complete write logic for these CSRs
    when(io.reg_write_address_id === CSRRegister.MIE) {
      mie := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_id === CSRRegister.MTVEC) {
      mtvec := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_id === CSRRegister.MSCRATCH) {
      mscratch := io.reg_write_data_ex
    }
  }

}
