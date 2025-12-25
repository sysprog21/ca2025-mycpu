// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import bus.AXI4LiteSlave
import chisel3._
import peripheral.InstructionROM
import peripheral.Memory
import peripheral.ROMLoader
import riscv.core.CPU

// Simplified test harness for RISCOF compliance tests
// Uses AXI4-Lite to connect CPU to Memory, matching the 4-soc architecture
class TestTopModule(exeFilename: String) extends Module {
  val io = IO(new Bundle {
    val regs_debug_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val mem_debug_read_address  = Input(UInt(Parameters.AddrWidth))
    val regs_debug_read_data    = Output(UInt(Parameters.DataWidth))
    val mem_debug_read_data     = Output(UInt(Parameters.DataWidth))
    val csr_debug_read_address  = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val csr_debug_read_data     = Output(UInt(Parameters.DataWidth))
    val interrupt_flag          = Input(UInt(Parameters.InterruptFlagWidth))
  })

  val mem             = Module(new Memory(8192))
  val instruction_rom = Module(new InstructionROM(exeFilename))
  val rom_loader      = Module(new ROMLoader(instruction_rom.capacity))

  rom_loader.io.rom_data     := instruction_rom.io.data
  rom_loader.io.load_address := Parameters.EntryAddress
  instruction_rom.io.address := rom_loader.io.rom_address

  // Clock divider for CPU (4:1 ratio for AXI4-Lite timing compatibility)
  val CPU_clkdiv = RegInit(UInt(2.W), 0.U)
  val CPU_tick   = Wire(Bool())
  val CPU_next   = Wire(UInt(2.W))
  CPU_next   := Mux(CPU_clkdiv === 3.U, 0.U, CPU_clkdiv + 1.U)
  CPU_tick   := CPU_clkdiv === 0.U
  CPU_clkdiv := CPU_next

  withClock(CPU_tick.asClock) {
    val cpu = Module(new CPU)

    // AXI4-Lite slave adapter for memory
    val mem_slave = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))

    cpu.io.debug_read_address     := 0.U
    cpu.io.csr_debug_read_address := 0.U
    cpu.io.instruction_valid      := rom_loader.io.load_finished

    // Instruction fetch from memory
    mem.io.instruction_address := cpu.io.instruction_address
    cpu.io.instruction         := mem.io.instruction

    cpu.io.interrupt_flag := io.interrupt_flag

    // Connect AXI4-Lite channels from CPU to memory slave
    mem_slave.io.channels <> cpu.io.axi4_channels

    // Memory connections using Mux to select between ROM loading and normal operation
    val loading = !rom_loader.io.load_finished

    // Memory bundle connections (select between ROMLoader and AXI slave)
    mem.io.bundle.address      := Mux(loading, rom_loader.io.bundle.address, mem_slave.io.bundle.address)
    mem.io.bundle.write_data   := Mux(loading, rom_loader.io.bundle.write_data, mem_slave.io.bundle.write_data)
    mem.io.bundle.write_enable := Mux(loading, rom_loader.io.bundle.write_enable, mem_slave.io.bundle.write)
    mem.io.bundle.write_strobe := Mux(loading, rom_loader.io.bundle.write_strobe, mem_slave.io.bundle.write_strobe)

    // ROMLoader read_data (always connect to memory, not used during loading)
    rom_loader.io.bundle.read_data := mem.io.bundle.read_data

    // AXI slave read responses
    // Memory is SyncReadMem with 1-cycle read latency.
    // read_valid must be delayed 1 cycle after the read request.
    val read_pending = RegNext(mem_slave.io.bundle.read && !loading, false.B)
    mem_slave.io.bundle.read_data  := mem.io.bundle.read_data
    mem_slave.io.bundle.read_valid := read_pending

    // Note: cpu.io.memory_bundle is connected internally by CPU wrapper to AXI4LiteMaster
    // DO NOT override these signals - they are NOT unused, they are internal to CPU

    // Debug interfaces
    cpu.io.debug_read_address     := io.regs_debug_read_address
    io.regs_debug_read_data       := cpu.io.debug_read_data
    cpu.io.csr_debug_read_address := io.csr_debug_read_address
    io.csr_debug_read_data        := cpu.io.csr_debug_read_data

    // Drive memory_bundle INPUT signals (not used - actual memory goes through AXI4)
    // These must be driven to avoid FIRRTL RefNotInitializedException
    cpu.io.memory_bundle.read_data           := DontCare
    cpu.io.memory_bundle.read_valid          := false.B
    cpu.io.memory_bundle.write_valid         := false.B
    cpu.io.memory_bundle.write_data_accepted := false.B
    cpu.io.memory_bundle.busy                := false.B
    cpu.io.memory_bundle.granted             := true.B
  }

  mem.io.debug_read_address := io.mem_debug_read_address
  io.mem_debug_read_data    := mem.io.debug_read_data
}
