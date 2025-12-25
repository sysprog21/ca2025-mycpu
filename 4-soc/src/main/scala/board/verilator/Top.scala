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

  val cpu         = Module(new CPU)
  val dummy       = Module(new DummySlave)
  val bus_arbiter = Module(new BusArbiter)
  val bus_switch  = Module(new BusSwitch)

  // Instruction fetch (external ROM in testbench)
  io.instruction_address   := cpu.io.instruction_address
  cpu.io.instruction       := io.instruction
  cpu.io.instruction_valid := io.instruction_valid

  // Terminate unused memory_bundle inputs with explicit values
  // These signals are not used because memory access goes through AXI4-Lite channels,
  // but Chisel requires all bundle inputs to be driven. Using explicit zeros instead
  // of DontCare for deterministic simulation behavior and cleaner waveforms.
  cpu.io.memory_bundle.read_data           := 0.U
  cpu.io.memory_bundle.read_valid          := false.B
  cpu.io.memory_bundle.write_valid         := false.B
  cpu.io.memory_bundle.write_data_accepted := false.B
  cpu.io.memory_bundle.busy                := false.B
  cpu.io.memory_bundle.granted             := false.B

  // Bus arbiter
  bus_arbiter.io.bus_request(0) := true.B

  // Bus switch
  bus_switch.io.master <> cpu.io.axi4_channels
  bus_switch.io.address := cpu.io.bus_address
  bus_switch.io.slaves(0) <> mem_slave.io.channels
  bus_switch.io.slaves(1) <> vga.io.channels
  bus_switch.io.slaves(2) <> uart.io.channels
  for (i <- 3 until Parameters.SlaveDeviceCount) {
    bus_switch.io.slaves(i) <> dummy.io.channels
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
  cpu.io.interrupt_flag := io.signal_interrupt

  // Debug interfaces
  cpu.io.debug_read_address     := io.cpu_debug_read_address
  io.cpu_debug_read_data        := cpu.io.debug_read_data
  cpu.io.csr_debug_read_address := io.cpu_csr_debug_read_address
  io.cpu_csr_debug_read_data    := cpu.io.csr_debug_read_data
}

object VerilogGenerator extends App {
  (new ChiselStage).emitVerilog(
    new Top(),
    Array("--target-dir", "4-soc/verilog/verilator")
  )
}
