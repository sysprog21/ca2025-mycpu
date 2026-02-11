// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import bus.AXI4LiteSlave
import chisel3._
import chisel3.util.Mux1H
import peripheral.DummySlave
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
    val cpus = Seq.fill(Parameters.MasterDeviceCount)(Module(new CPU))

    // AXI4-Lite slave adapter for memory
    val mem_slave = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))

    for (i <- 0 until Parameters.MasterDeviceCount) {
      val cpu = cpus(i)
      cpu.io.debug_read_address     := 0.U
      cpu.io.csr_debug_read_address := 0.U
      if (i == 0) {
        cpu.io.instruction_valid   := rom_loader.io.load_finished
        mem.io.instruction_address := cpu.io.instruction_address
        cpu.io.instruction         := mem.io.instruction
      } else {
        cpu.io.instruction_valid := false.B
        cpu.io.instruction       := 0.U
      }
      cpu.io.interrupt_flag := io.interrupt_flag
    }

    val bus_arbiter = Module(new bus.BusArbiter)
    val bus_switch  = Module(new bus.BusSwitch)

    for (i <- 0 until Parameters.MasterDeviceCount) {
      bus_arbiter.io.bus_request(i) := cpus(i).io.memory_bundle.request
    }

    val muxed_master = Wire(new bus.AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
    muxed_master.write_address_channel.AWVALID := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_address_channel.AWVALID)
    )
    muxed_master.write_address_channel.AWADDR := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_address_channel.AWADDR)
    )
    muxed_master.write_address_channel.AWPROT := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_address_channel.AWPROT)
    )
    muxed_master.write_data_channel.WVALID := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_data_channel.WVALID)
    )
    muxed_master.write_data_channel.WDATA := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_data_channel.WDATA)
    )
    muxed_master.write_data_channel.WSTRB := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_data_channel.WSTRB)
    )
    muxed_master.write_response_channel.BREADY := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.write_response_channel.BREADY)
    )
    muxed_master.read_address_channel.ARVALID := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.read_address_channel.ARVALID)
    )
    muxed_master.read_address_channel.ARADDR := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.read_address_channel.ARADDR)
    )
    muxed_master.read_address_channel.ARPROT := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.read_address_channel.ARPROT)
    )
    muxed_master.read_data_channel.RREADY := Mux1H(
      bus_arbiter.io.bus_granted,
      cpus.map(_.io.axi4_channels.read_data_channel.RREADY)
    )

    bus_switch.io.master <> muxed_master
    bus_switch.io.address := Mux1H(bus_arbiter.io.bus_granted, cpus.map(_.io.bus_address))
    bus_switch.io.slaves(0) <> mem_slave.io.channels
    val dummy = Module(new DummySlave)
    for (i <- 1 until Parameters.SlaveDeviceCount) {
      bus_switch.io.slaves(i) <> dummy.io.channels
    }

    for (i <- 0 until Parameters.MasterDeviceCount) {
      cpus(i).io.axi4_channels.write_address_channel.AWREADY :=
        bus_switch.io.master.write_address_channel.AWREADY && bus_arbiter.io.bus_granted(i)
      cpus(i).io.axi4_channels.write_data_channel.WREADY :=
        bus_switch.io.master.write_data_channel.WREADY && bus_arbiter.io.bus_granted(i)
      cpus(i).io.axi4_channels.write_response_channel.BVALID :=
        bus_switch.io.master.write_response_channel.BVALID && bus_arbiter.io.bus_granted(i)
      cpus(i).io.axi4_channels.write_response_channel.BRESP := bus_switch.io.master.write_response_channel.BRESP
      cpus(i).io.axi4_channels.read_address_channel.ARREADY :=
        bus_switch.io.master.read_address_channel.ARREADY && bus_arbiter.io.bus_granted(i)
      cpus(i).io.axi4_channels.read_data_channel.RVALID :=
        bus_switch.io.master.read_data_channel.RVALID && bus_arbiter.io.bus_granted(i)
      cpus(i).io.axi4_channels.read_data_channel.RDATA := bus_switch.io.master.read_data_channel.RDATA
      cpus(i).io.axi4_channels.read_data_channel.RRESP := bus_switch.io.master.read_data_channel.RRESP
    }

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
    cpus.head.io.debug_read_address     := io.regs_debug_read_address
    io.regs_debug_read_data             := cpus.head.io.debug_read_data
    cpus.head.io.csr_debug_read_address := io.csr_debug_read_address
    io.csr_debug_read_data              := cpus.head.io.csr_debug_read_data
    for (i <- 1 until Parameters.MasterDeviceCount) {
      cpus(i).io.debug_read_address     := 0.U
      cpus(i).io.csr_debug_read_address := 0.U
    }

    // Drive memory_bundle INPUT signals (not used - actual memory goes through AXI4)
    // These must be driven to avoid FIRRTL RefNotInitializedException
    for (cpu <- cpus) {
      cpu.io.memory_bundle.read_data           := DontCare
      cpu.io.memory_bundle.read_valid          := false.B
      cpu.io.memory_bundle.write_valid         := false.B
      cpu.io.memory_bundle.write_data_accepted := false.B
      cpu.io.memory_bundle.busy                := false.B
      cpu.io.memory_bundle.granted             := true.B
    }

    val snoop_addr  = RegInit(0.U(Parameters.AddrWidth))
    val snoop_valid = RegInit(false.B)
    snoop_valid := false.B
    when(bus_switch.io.master.write_address_channel.AWVALID && bus_switch.io.master.write_address_channel.AWREADY) {
      snoop_addr := bus_switch.io.master.write_address_channel.AWADDR
    }
    when(bus_switch.io.master.write_response_channel.BVALID && bus_switch.io.master.write_response_channel.BREADY) {
      snoop_valid := true.B
    }
    for (cpu <- cpus) {
      cpu.io.reservation_snoop_valid := snoop_valid
      cpu.io.reservation_snoop_addr  := snoop_addr
    }
  }

  mem.io.debug_read_address := io.mem_debug_read_address
  io.mem_debug_read_data    := mem.io.debug_read_data
}
