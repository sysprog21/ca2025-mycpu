// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package board.verilator

import bus.AXI4LiteSlave
import bus.AXI4LiteSlaveBundle
import bus.BusArbiter
import bus.BusSwitch
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.Mux1H
import peripheral.DummySlave
import peripheral.Uart
import peripheral.VGA
import riscv.core.CPU
import riscv.Parameters

class Top extends Module {
  val io = IO(new Bundle {
    val signal_interrupt = Input(Bool())

    // Instruction interface (external ROM in testbench)
    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_valid   = Input(Bool())

    val mem_slave = new AXI4LiteSlaveBundle(Parameters.AddrBits, Parameters.DataBits)

    // VGA peripheral outputs
    val vga_pixclk      = Input(Clock())     // VGA pixel clock (31.5 MHz)
    val vga_hsync       = Output(Bool())     // Horizontal sync
    val vga_vsync       = Output(Bool())     // Vertical sync
    val vga_rrggbb      = Output(UInt(6.W))  // 6-bit color output
    val vga_activevideo = Output(Bool())     // Active display region
    val vga_x_pos       = Output(UInt(10.W)) // Current pixel X position
    val vga_y_pos       = Output(UInt(10.W)) // Current pixel Y position

    // UART peripheral outputs
    val uart_txd       = Output(UInt(1.W)) // UART TX data
    val uart_rxd       = Input(UInt(1.W))  // UART RX data
    val uart_interrupt = Output(Bool())    // UART interrupt signal

    val cpu_debug_read_address     = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val cpu_debug_read_data        = Output(UInt(Parameters.DataWidth))
    val cpu_csr_debug_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val cpu_csr_debug_read_data    = Output(UInt(Parameters.DataWidth))
  })

  // AXI4-Lite memory model provided by Verilator C++ harness (sim.cpp)
  val mem_slave = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))
  io.mem_slave <> mem_slave.io.bundle

  // VGA peripheral
  val vga = Module(new VGA)

  // UART peripheral (115200 baud standard rate)
  val uart = Module(new Uart(frequency = 50000000, baudRate = 115200))

  val cpus        = Seq.fill(Parameters.MasterDeviceCount)(Module(new CPU))
  val dummy       = Module(new DummySlave)
  val bus_arbiter = Module(new BusArbiter)
  val bus_switch  = Module(new BusSwitch)

  // Instruction fetch (external ROM in testbench)
  io.instruction_address := cpus.head.io.instruction_address
  for (i <- 0 until Parameters.MasterDeviceCount) {
    val cpu = cpus(i)
    if (i == 0) {
      cpu.io.instruction       := io.instruction
      cpu.io.instruction_valid := io.instruction_valid
    } else {
      cpu.io.instruction       := 0.U
      cpu.io.instruction_valid := false.B
    }
  }

  // Terminate unused memory_bundle inputs with explicit values
  // These signals are not used because memory access goes through AXI4-Lite channels,
  // but Chisel requires all bundle inputs to be driven. Using explicit zeros instead
  // of DontCare for deterministic simulation behavior and cleaner waveforms.
  for (cpu <- cpus) {
    cpu.io.memory_bundle.read_data           := 0.U
    cpu.io.memory_bundle.read_valid          := false.B
    cpu.io.memory_bundle.write_valid         := false.B
    cpu.io.memory_bundle.write_data_accepted := false.B
    cpu.io.memory_bundle.busy                := false.B
    cpu.io.memory_bundle.granted             := false.B
  }

  // Bus arbiter
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

  // Bus switch
  bus_switch.io.master <> muxed_master
  bus_switch.io.address := Mux1H(bus_arbiter.io.bus_granted, cpus.map(_.io.bus_address))
  bus_switch.io.slaves(0) <> mem_slave.io.channels
  bus_switch.io.slaves(1) <> vga.io.channels
  bus_switch.io.slaves(2) <> uart.io.channels
  for (i <- 3 until Parameters.SlaveDeviceCount) {
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

  // VGA connections
  vga.io.pixClock    := io.vga_pixclk
  io.vga_hsync       := vga.io.hsync
  io.vga_vsync       := vga.io.vsync
  io.vga_rrggbb      := vga.io.rrggbb
  io.vga_activevideo := vga.io.activevideo
  io.vga_x_pos       := vga.io.x_pos
  io.vga_y_pos       := vga.io.y_pos

  // UART connections
  io.uart_txd       := uart.io.txd
  uart.io.rxd       := io.uart_rxd
  io.uart_interrupt := uart.io.signal_interrupt

  // Interrupt
  for (cpu <- cpus) {
    cpu.io.interrupt_flag := io.signal_interrupt
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

  // Debug interfaces
  cpus.head.io.debug_read_address     := io.cpu_debug_read_address
  io.cpu_debug_read_data              := cpus.head.io.debug_read_data
  cpus.head.io.csr_debug_read_address := io.cpu_csr_debug_read_address
  io.cpu_csr_debug_read_data          := cpus.head.io.csr_debug_read_data
  for (i <- 1 until Parameters.MasterDeviceCount) {
    cpus(i).io.debug_read_address     := 0.U
    cpus(i).io.csr_debug_read_address := 0.U
  }
}

object VerilogGenerator extends App {
  (new ChiselStage).emitVerilog(
    new Top(),
    Array("--target-dir", "4-soc/verilog/verilator")
  )
}
