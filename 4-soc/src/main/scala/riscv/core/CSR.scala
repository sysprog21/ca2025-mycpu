// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

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

object CSRRegister {
  // CSR addresses per RISC-V Privileged Spec v1.12, Section 3.1-3.2
  // Machine Information Registers
  val MSTATUS  = 0x300.U(Parameters.CSRRegisterAddrWidth)
  val MIE      = 0x304.U(Parameters.CSRRegisterAddrWidth)
  val MTVEC    = 0x305.U(Parameters.CSRRegisterAddrWidth)
  val MSCRATCH = 0x340.U(Parameters.CSRRegisterAddrWidth)
  val MEPC     = 0x341.U(Parameters.CSRRegisterAddrWidth)
  val MCAUSE   = 0x342.U(Parameters.CSRRegisterAddrWidth)

  // Machine Counter/Timers (read-only shadows at 0xC00+)
  val CycleL   = 0xc00.U(Parameters.CSRRegisterAddrWidth) // Lower 32 bits of cycle counter
  val CycleH   = 0xc80.U(Parameters.CSRRegisterAddrWidth) // Upper 32 bits of cycle counter
  val InstretL = 0xc02.U(Parameters.CSRRegisterAddrWidth) // Lower 32 bits of instret counter
  val InstretH = 0xc82.U(Parameters.CSRRegisterAddrWidth) // Upper 32 bits of instret counter

  // Machine Counter/Timers (M-mode read/write at 0xB00+)
  val MCycleL   = 0xb00.U(Parameters.CSRRegisterAddrWidth) // Lower 32 bits of mcycle
  val MCycleH   = 0xb80.U(Parameters.CSRRegisterAddrWidth) // Upper 32 bits of mcycle
  val MInstretL = 0xb02.U(Parameters.CSRRegisterAddrWidth) // Lower 32 bits of minstret
  val MInstretH = 0xb82.U(Parameters.CSRRegisterAddrWidth) // Upper 32 bits of minstret

  // Hardware Performance Counters (M-mode read/write)
  val MHPMCounter3L = 0xb03.U(Parameters.CSRRegisterAddrWidth) // Branch mispredictions (BTB/RAS wrong)
  val MHPMCounter3H = 0xb83.U(Parameters.CSRRegisterAddrWidth)
  val MHPMCounter4L = 0xb04.U(Parameters.CSRRegisterAddrWidth) // Hazard stall cycles
  val MHPMCounter4H = 0xb84.U(Parameters.CSRRegisterAddrWidth)
  val MHPMCounter5L = 0xb05.U(Parameters.CSRRegisterAddrWidth) // Memory stall cycles
  val MHPMCounter5H = 0xb85.U(Parameters.CSRRegisterAddrWidth)
  val MHPMCounter6L = 0xb06.U(Parameters.CSRRegisterAddrWidth) // Control stall cycles (flush penalty)
  val MHPMCounter6H = 0xb86.U(Parameters.CSRRegisterAddrWidth)
  val MHPMCounter7L = 0xb07.U(Parameters.CSRRegisterAddrWidth) // BTB miss penalty (taken but not predicted)
  val MHPMCounter7H = 0xb87.U(Parameters.CSRRegisterAddrWidth)
  val MHPMCounter8L = 0xb08.U(Parameters.CSRRegisterAddrWidth) // Total branches resolved
  val MHPMCounter8H = 0xb88.U(Parameters.CSRRegisterAddrWidth)
  val MHPMCounter9L = 0xb09.U(Parameters.CSRRegisterAddrWidth) // BTB predictions (BTB said "taken")
  val MHPMCounter9H = 0xb89.U(Parameters.CSRRegisterAddrWidth)

  // Machine Counter-Inhibit Register (0x320)
  val MCOUNTINHIBIT = 0x320.U(Parameters.CSRRegisterAddrWidth)

  // Hardware Performance Counter Events (for documentation)
  // mhpmcounter3: Branch mispredictions (BTB/RAS predicted wrong direction or target)
  // mhpmcounter4: Hazard stall cycles (data hazards: load-use, RAW dependencies)
  // mhpmcounter5: Memory stall cycles (AXI bus wait states)
  // mhpmcounter6: Control stall cycles (branch/jump flush penalty)
  // mhpmcounter7: BTB miss penalty (branch taken but not in BTB)
  // mhpmcounter8: Total branches resolved (for accuracy = 1 - mhpmcounter3/mhpmcounter8)
  // mhpmcounter9: BTB predictions (BTB predicted "taken" for branch analysis)
}

/**
 * Control and Status Registers (CSR) with Performance Counters
 *
 * Implements RISC-V privileged architecture CSRs including:
 * - Machine trap setup/handling registers (mstatus, mtvec, mepc, mcause, etc.)
 * - Hardware performance counters (mcycle, minstret, mhpmcounter3-9)
 * - Counter inhibit register (mcountinhibit) for selective counter gating
 *
 * Performance Counter Mapping:
 * - mcycle/cycle (0xB00/0xC00): CPU clock cycles [CYCLES]
 * - minstret/instret (0xB02/0xC02): Instructions retired (completed in WB) [EVENTS]
 * - mhpmcounter3 (0xB03): Branch mispredictions (BTB/RAS wrong direction/target) [EVENTS]
 * - mhpmcounter4 (0xB04): Hazard stall cycles (data hazards) [CYCLES]
 * - mhpmcounter5 (0xB05): Memory stall cycles (AXI bus wait) [CYCLES]
 * - mhpmcounter6 (0xB06): Control flush events (branch/jump flush penalty) [EVENTS]
 * - mhpmcounter7 (0xB07): BTB miss/wrong-target events [EVENTS]
 * - mhpmcounter8 (0xB08): Total branches resolved [EVENTS] (accuracy denominator)
 * - mhpmcounter9 (0xB09): BTB predictions [EVENTS] (BTB predicted "taken")
 *
 * Counter Semantics (IMPORTANT):
 * - CYCLES counters: Increment once per clock cycle while condition is true
 * - EVENTS counters: Increment once per occurrence (regardless of duration)
 * - mhpmcounter6 counts FLUSH EVENTS, not cycles (each flush is 1 event)
 * - Counters 4,5,6 are MUTUALLY EXCLUSIVE with priority: memory(5) > control(6) > hazard(4)
 * - mhpmcounter3 and mhpmcounter7 may overlap (wrong-target counted in both)
 *
 * Key Metrics:
 * - CPI Calculation: mcycle / minstret
 * - Branch Accuracy: 1 - (mhpmcounter3 / mhpmcounter8)
 * - Stall Breakdown: mhpmcounter4 (hazard) + mhpmcounter5 (memory) cycles
 * - Control Overhead: mhpmcounter6 events (each flush = 1 cycle penalty)
 * - BTB Cold Miss Rate: mhpmcounter7 / mhpmcounter8
 * - BTB Coverage: mhpmcounter9 / mhpmcounter8 (how often BTB predicts)
 *
 * mcountinhibit (0x320) Bit Mapping:
 * - Bit 0: Inhibit mcycle
 * - Bit 1: Reserved (hardwired to 0)
 * - Bit 2: Inhibit minstret
 * - Bits 3-9: Inhibit mhpmcounter3-9
 * - Bits 10-31: Reserved (hardwired to 0)
 *
 * Features:
 * - Atomic 64-bit reads: Shadow registers latch high word when low word is read
 * - M-mode writes: Full read/write access to all counters via 0xB00+ addresses
 * - User-mode shadows: Read-only access via 0xC00+ (cycle/instret only)
 * - Counter inhibit: Selectively disable counters for profiling regions
 *
 * Usage Pattern for Atomic 64-bit Read:
 *   csrr t0, cyclel    // Reading low word latches high word into shadow
 *   csrr t1, cycleh    // Returns latched shadow value (atomic with t0)
 * Note: Reading high word without prior low word read returns stale shadow.
 *
 * CSR Write vs Counter Increment:
 * - When CSR write and counter increment occur in same cycle, write wins
 * - Written value will be incremented by 1 on next cycle (for mcycle)
 */
class CSR extends Module {
  val io = IO(new Bundle {
    val reg_read_address_id    = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val reg_write_enable_ex    = Input(Bool())
    val reg_write_address_ex   = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val reg_write_data_ex      = Input(UInt(Parameters.DataWidth))
    val debug_reg_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))

    val id_reg_read_data    = Output(UInt(Parameters.DataWidth))
    val debug_reg_read_data = Output(UInt(Parameters.DataWidth))

    val clint_access_bundle = Flipped(new CSRDirectAccessBundle)

    // Performance counter inputs (directly from pipeline stages)
    val instruction_retired  = Input(Bool()) // Instruction completed in WB stage
    val branch_misprediction = Input(Bool()) // BTB or RAS misprediction detected
    val hazard_stall         = Input(Bool()) // Data hazard stall (load-use, RAW)
    val memory_stall         = Input(Bool()) // Memory/AXI bus stall
    val control_stall        = Input(Bool()) // Control hazard (flush penalty)
    val btb_miss_taken       = Input(Bool()) // Branch taken but not in BTB
    val branch_resolved      = Input(Bool()) // Branch/jump resolved in ID stage
    val btb_predicted        = Input(Bool()) // BTB predicted "taken" for this branch
  })

  // Machine Trap Setup/Handling Registers
  val mstatus  = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mie      = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mtvec    = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mscratch = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mepc     = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mcause   = RegInit(UInt(Parameters.DataWidth), 0.U)

  // Machine Counter-Inhibit Register (mcountinhibit)
  // Bit 0: CY - inhibit mcycle, Bit 2: IR - inhibit minstret
  // Bits 3-9: HPM3-9 - inhibit mhpmcounter3-9
  val mcountinhibit = RegInit(0.U(32.W))

  // Hardware Performance Counters (64-bit)
  val mcycle       = RegInit(0.U(64.W)) // Clock cycles
  val minstret     = RegInit(0.U(64.W)) // Instructions retired
  val mhpmcounter3 = RegInit(0.U(64.W)) // Branch mispredictions (BTB/RAS wrong)
  val mhpmcounter4 = RegInit(0.U(64.W)) // Hazard stall cycles
  val mhpmcounter5 = RegInit(0.U(64.W)) // Memory stall cycles
  val mhpmcounter6 = RegInit(0.U(64.W)) // Control stall cycles
  val mhpmcounter7 = RegInit(0.U(64.W)) // BTB miss penalty
  val mhpmcounter8 = RegInit(0.U(64.W)) // Total branches resolved
  val mhpmcounter9 = RegInit(0.U(64.W)) // BTB predictions

  // Shadow registers for atomic 64-bit reads
  // When software reads the low 32 bits, we latch the high 32 bits into a shadow register.
  // This prevents torn reads when the counter increments between reading low and high words.
  // The shadow register is returned when reading the high word.
  val mcycle_shadow       = RegInit(0.U(32.W))
  val minstret_shadow     = RegInit(0.U(32.W))
  val mhpmcounter3_shadow = RegInit(0.U(32.W))
  val mhpmcounter4_shadow = RegInit(0.U(32.W))
  val mhpmcounter5_shadow = RegInit(0.U(32.W))
  val mhpmcounter6_shadow = RegInit(0.U(32.W))
  val mhpmcounter7_shadow = RegInit(0.U(32.W))
  val mhpmcounter8_shadow = RegInit(0.U(32.W))
  val mhpmcounter9_shadow = RegInit(0.U(32.W))

  // Latch high word when low word is read (for atomic 64-bit reads)
  val reading_cycle_low =
    io.reg_read_address_id === CSRRegister.CycleL || io.reg_read_address_id === CSRRegister.MCycleL
  val reading_instret_low =
    io.reg_read_address_id === CSRRegister.InstretL || io.reg_read_address_id === CSRRegister.MInstretL
  val reading_hpm3_low = io.reg_read_address_id === CSRRegister.MHPMCounter3L
  val reading_hpm4_low = io.reg_read_address_id === CSRRegister.MHPMCounter4L
  val reading_hpm5_low = io.reg_read_address_id === CSRRegister.MHPMCounter5L
  val reading_hpm6_low = io.reg_read_address_id === CSRRegister.MHPMCounter6L
  val reading_hpm7_low = io.reg_read_address_id === CSRRegister.MHPMCounter7L
  val reading_hpm8_low = io.reg_read_address_id === CSRRegister.MHPMCounter8L
  val reading_hpm9_low = io.reg_read_address_id === CSRRegister.MHPMCounter9L

  when(reading_cycle_low) {
    mcycle_shadow := mcycle(63, 32)
  }
  when(reading_instret_low) {
    minstret_shadow := minstret(63, 32)
  }
  when(reading_hpm3_low) {
    mhpmcounter3_shadow := mhpmcounter3(63, 32)
  }
  when(reading_hpm4_low) {
    mhpmcounter4_shadow := mhpmcounter4(63, 32)
  }
  when(reading_hpm5_low) {
    mhpmcounter5_shadow := mhpmcounter5(63, 32)
  }
  when(reading_hpm6_low) {
    mhpmcounter6_shadow := mhpmcounter6(63, 32)
  }
  when(reading_hpm7_low) {
    mhpmcounter7_shadow := mhpmcounter7(63, 32)
  }
  when(reading_hpm8_low) {
    mhpmcounter8_shadow := mhpmcounter8(63, 32)
  }
  when(reading_hpm9_low) {
    mhpmcounter9_shadow := mhpmcounter9(63, 32)
  }

  // Counter inhibit bits
  val inhibit_cy   = mcountinhibit(0) // Bit 0: mcycle
  val inhibit_ir   = mcountinhibit(2) // Bit 2: minstret
  val inhibit_hpm3 = mcountinhibit(3) // Bit 3: mhpmcounter3
  val inhibit_hpm4 = mcountinhibit(4) // Bit 4: mhpmcounter4
  val inhibit_hpm5 = mcountinhibit(5) // Bit 5: mhpmcounter5
  val inhibit_hpm6 = mcountinhibit(6) // Bit 6: mhpmcounter6
  val inhibit_hpm7 = mcountinhibit(7) // Bit 7: mhpmcounter7
  val inhibit_hpm8 = mcountinhibit(8) // Bit 8: mhpmcounter8
  val inhibit_hpm9 = mcountinhibit(9) // Bit 9: mhpmcounter9

  // Increment counters (after shadow latching to get consistent snapshot)
  // Each counter respects its mcountinhibit bit
  when(!inhibit_cy) {
    mcycle := mcycle + 1.U
  }
  when(io.instruction_retired && !inhibit_ir) {
    minstret := minstret + 1.U
  }
  when(io.branch_misprediction && !inhibit_hpm3) {
    mhpmcounter3 := mhpmcounter3 + 1.U
  }
  when(io.hazard_stall && !inhibit_hpm4) {
    mhpmcounter4 := mhpmcounter4 + 1.U
  }
  when(io.memory_stall && !inhibit_hpm5) {
    mhpmcounter5 := mhpmcounter5 + 1.U
  }
  when(io.control_stall && !inhibit_hpm6) {
    mhpmcounter6 := mhpmcounter6 + 1.U
  }
  when(io.btb_miss_taken && !inhibit_hpm7) {
    mhpmcounter7 := mhpmcounter7 + 1.U
  }
  when(io.branch_resolved && !inhibit_hpm8) {
    mhpmcounter8 := mhpmcounter8 + 1.U
  }
  when(io.btb_predicted && !inhibit_hpm9) {
    mhpmcounter9 := mhpmcounter9 + 1.U
  }

  // Register lookup table for CSR reads
  // High word reads use shadow registers for atomic 64-bit reads
  val regLUT =
    IndexedSeq(
      // Machine trap registers
      CSRRegister.MSTATUS  -> mstatus,
      CSRRegister.MIE      -> mie,
      CSRRegister.MTVEC    -> mtvec,
      CSRRegister.MSCRATCH -> mscratch,
      CSRRegister.MEPC     -> mepc,
      CSRRegister.MCAUSE   -> mcause,
      // Machine counter-inhibit register
      CSRRegister.MCOUNTINHIBIT -> mcountinhibit,
      // User-mode read-only shadows (0xC00+)
      // Low word: live value, High word: shadow (latched when low read)
      CSRRegister.CycleL   -> mcycle(31, 0),
      CSRRegister.CycleH   -> mcycle_shadow,
      CSRRegister.InstretL -> minstret(31, 0),
      CSRRegister.InstretH -> minstret_shadow,
      // Machine-mode read/write (0xB00+)
      CSRRegister.MCycleL   -> mcycle(31, 0),
      CSRRegister.MCycleH   -> mcycle_shadow,
      CSRRegister.MInstretL -> minstret(31, 0),
      CSRRegister.MInstretH -> minstret_shadow,
      // Hardware performance counters
      CSRRegister.MHPMCounter3L -> mhpmcounter3(31, 0),
      CSRRegister.MHPMCounter3H -> mhpmcounter3_shadow,
      CSRRegister.MHPMCounter4L -> mhpmcounter4(31, 0),
      CSRRegister.MHPMCounter4H -> mhpmcounter4_shadow,
      CSRRegister.MHPMCounter5L -> mhpmcounter5(31, 0),
      CSRRegister.MHPMCounter5H -> mhpmcounter5_shadow,
      CSRRegister.MHPMCounter6L -> mhpmcounter6(31, 0),
      CSRRegister.MHPMCounter6H -> mhpmcounter6_shadow,
      CSRRegister.MHPMCounter7L -> mhpmcounter7(31, 0),
      CSRRegister.MHPMCounter7H -> mhpmcounter7_shadow,
      CSRRegister.MHPMCounter8L -> mhpmcounter8(31, 0),
      CSRRegister.MHPMCounter8H -> mhpmcounter8_shadow,
      CSRRegister.MHPMCounter9L -> mhpmcounter9(31, 0),
      CSRRegister.MHPMCounter9H -> mhpmcounter9_shadow,
    )

  // If the pipeline and the CLINT are going to read and write the CSR at the same time, let the pipeline write first.
  // This is implemented in a single cycle by passing reg_write_data_ex to clint and writing the data from the CLINT to the CSR.
  io.id_reg_read_data    := MuxLookup(io.reg_read_address_id, 0.U)(regLUT)
  io.debug_reg_read_data := MuxLookup(io.debug_reg_read_address, 0.U)(regLUT)

  io.clint_access_bundle.mstatus := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MSTATUS,
    io.reg_write_data_ex,
    mstatus
  )
  io.clint_access_bundle.mtvec := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MTVEC,
    io.reg_write_data_ex,
    mtvec
  )
  io.clint_access_bundle.mcause := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MCAUSE,
    io.reg_write_data_ex,
    mcause
  )
  io.clint_access_bundle.mepc := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MEPC,
    io.reg_write_data_ex,
    mepc
  )
  io.clint_access_bundle.mie := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MIE,
    io.reg_write_data_ex,
    mie
  )

  when(io.clint_access_bundle.direct_write_enable) {
    mstatus := io.clint_access_bundle.mstatus_write_data
    mepc    := io.clint_access_bundle.mepc_write_data
    mcause  := io.clint_access_bundle.mcause_write_data
  }.elsewhen(io.reg_write_enable_ex) {
    when(io.reg_write_address_ex === CSRRegister.MSTATUS) {
      mstatus := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MEPC) {
      mepc := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MCAUSE) {
      mcause := io.reg_write_data_ex
    }
  }

  when(io.reg_write_enable_ex) {
    when(io.reg_write_address_ex === CSRRegister.MIE) {
      mie := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MTVEC) {
      mtvec := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MSCRATCH) {
      mscratch := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MCOUNTINHIBIT) {
      // Only bits 0, 2, 3-9 are writable (bit 1 is reserved, upper bits hardwired to 0)
      // Mask: 0x000003fd = bits 0,2,3,4,5,6,7,8,9 (skip bit 1, clear bits 10-31)
      mcountinhibit := io.reg_write_data_ex & "h000003fd".U
    }
  }

  // M-mode writes to performance counters (0xB00+)
  // Note: Counter increment happens unconditionally; CSR write overrides in same cycle.
  // The written value will be incremented by 1 on the next cycle (for mcycle).
  when(io.reg_write_enable_ex) {
    when(io.reg_write_address_ex === CSRRegister.MCycleL) {
      mcycle := Cat(mcycle(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MCycleH) {
      mcycle := Cat(io.reg_write_data_ex, mcycle(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MInstretL) {
      minstret := Cat(minstret(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MInstretH) {
      minstret := Cat(io.reg_write_data_ex, minstret(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter3L) {
      mhpmcounter3 := Cat(mhpmcounter3(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter3H) {
      mhpmcounter3 := Cat(io.reg_write_data_ex, mhpmcounter3(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter4L) {
      mhpmcounter4 := Cat(mhpmcounter4(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter4H) {
      mhpmcounter4 := Cat(io.reg_write_data_ex, mhpmcounter4(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter5L) {
      mhpmcounter5 := Cat(mhpmcounter5(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter5H) {
      mhpmcounter5 := Cat(io.reg_write_data_ex, mhpmcounter5(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter6L) {
      mhpmcounter6 := Cat(mhpmcounter6(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter6H) {
      mhpmcounter6 := Cat(io.reg_write_data_ex, mhpmcounter6(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter7L) {
      mhpmcounter7 := Cat(mhpmcounter7(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter7H) {
      mhpmcounter7 := Cat(io.reg_write_data_ex, mhpmcounter7(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter8L) {
      mhpmcounter8 := Cat(mhpmcounter8(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter8H) {
      mhpmcounter8 := Cat(io.reg_write_data_ex, mhpmcounter8(31, 0))
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter9L) {
      mhpmcounter9 := Cat(mhpmcounter9(63, 32), io.reg_write_data_ex)
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MHPMCounter9H) {
      mhpmcounter9 := Cat(io.reg_write_data_ex, mhpmcounter9(31, 0))
    }
  }
}
