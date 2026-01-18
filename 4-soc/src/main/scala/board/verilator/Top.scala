// SPDX-License-Identifier: MIT
package board.verilator

import bus.AXI4LiteSlave
import bus.AXI4LiteSlaveBundle
import bus.BusArbiter
import bus.BusSwitch
import bus.AXI4Config
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import peripheral.DummySlave
import peripheral.Uart
import peripheral.VGA
import peripheral.DMA
import peripheral.DMAParameters
import riscv.core.CPU
import riscv.Parameters

class Top extends Module {
  val io = IO(new Bundle {
    val signal_interrupt = Input(Bool())
    val instruction_address = Output(UInt(Parameters.AddrWidth))
    val instruction         = Input(UInt(Parameters.InstructionWidth))
    val instruction_valid   = Input(Bool())
    val mem_slave = new AXI4LiteSlaveBundle(Parameters.AddrBits, Parameters.DataBits)
    val vga_pixclk      = Input(Clock())
    val vga_hsync       = Output(Bool())
    val vga_vsync       = Output(Bool())
    val vga_rrggbb      = Output(UInt(6.W))
    val vga_activevideo = Output(Bool())
    val vga_x_pos       = Output(UInt(10.W))
    val vga_y_pos       = Output(UInt(10.W))
    val uart_txd        = Output(UInt(1.W))
    val uart_rxd        = Input(UInt(1.W))
    val uart_interrupt  = Output(Bool())
    val cpu_debug_read_address      = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val cpu_debug_read_data         = Output(UInt(Parameters.DataWidth))
    val cpu_csr_debug_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val cpu_csr_debug_read_data     = Output(UInt(Parameters.DataWidth))
  })

  // 1. Memory Slave
  val mem_slave = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))
  io.mem_slave <> mem_slave.io.bundle

  // 2. Peripherals
  val vga  = Module(new VGA)
  val uart = Module(new Uart(frequency = 50000000, baudRate = 115200))
  
  // 3. DMA Controller
  val dma = Module(new DMA(DMAParameters(
    fifoDepth = 16,
    maxBurstLen = 1, 
    axiConfig = AXI4Config(
      addrWidth = Parameters.AddrBits,
      dataWidth = Parameters.DataBits
    )
  )))

  // 4. Core System
  val cpu         = Module(new CPU)
  val dummy       = Module(new DummySlave)
  val bus_switch  = Module(new BusSwitch)

  // CPU Connections
  io.instruction_address   := cpu.io.instruction_address
  cpu.io.instruction       := io.instruction
  cpu.io.instruction_valid := io.instruction_valid
  cpu.io.memory_bundle.read_data            := 0.U
  cpu.io.memory_bundle.read_valid           := false.B
  cpu.io.memory_bundle.write_valid          := false.B
  cpu.io.memory_bundle.write_data_accepted := false.B
  cpu.io.memory_bundle.busy                 := false.B
  cpu.io.memory_bundle.granted              := false.B
  cpu.io.interrupt_flag := io.signal_interrupt
  cpu.io.debug_read_address       := io.cpu_debug_read_address
  io.cpu_debug_read_data         := cpu.io.debug_read_data
  cpu.io.csr_debug_read_address := io.cpu_csr_debug_read_address
  io.cpu_csr_debug_read_data    := cpu.io.csr_debug_read_data

  // Bus Switch Connections
  bus_switch.io.master <> cpu.io.axi4_channels
  bus_switch.io.address := cpu.io.bus_address
  bus_switch.io.slaves(1) <> vga.io.channels
  bus_switch.io.slaves(2) <> uart.io.channels
  bus_switch.io.slaves(3) <> dma.io.slave
  for (i <- 4 until Parameters.SlaveDeviceCount) {
    bus_switch.io.slaves(i) <> dummy.io.channels
  }

  // =========================================================================
  // [FIX] Memory Arbitration with Locking
  // =========================================================================
  
  // Keep track of ongoing DMA transactions
  val dma_read_lock = RegInit(false.B)
  val dma_write_lock = RegInit(false.B)

  // [FIX] Replaced .fire with (.valid && .ready)
  when (dma.io.master.ar.valid && dma.io.master.ar.ready) { dma_read_lock := true.B }
  when (dma.io.master.r.valid && dma.io.master.r.ready && dma.io.master.r.last) { dma_read_lock := false.B }

  when (dma.io.master.aw.valid && dma.io.master.aw.ready) { dma_write_lock := true.B }
  when (dma.io.master.b.valid && dma.io.master.b.ready) { dma_write_lock := false.B }

  // DMA is active if it's requesting OR if it's waiting for a response
  val dma_active = dma.io.master.ar.valid || dma.io.master.aw.valid || dma.io.master.w.valid || dma_read_lock || dma_write_lock

  // Mux: Write Address
  mem_slave.io.channels.write_address_channel.AWVALID := Mux(dma_active, dma.io.master.aw.valid, bus_switch.io.slaves(0).write_address_channel.AWVALID)
  mem_slave.io.channels.write_address_channel.AWADDR  := Mux(dma_active, dma.io.master.aw.addr,  bus_switch.io.slaves(0).write_address_channel.AWADDR)
  mem_slave.io.channels.write_address_channel.AWPROT  := Mux(dma_active, dma.io.master.aw.prot,  bus_switch.io.slaves(0).write_address_channel.AWPROT)
  
  dma.io.master.aw.ready := dma_active && mem_slave.io.channels.write_address_channel.AWREADY
  bus_switch.io.slaves(0).write_address_channel.AWREADY := !dma_active && mem_slave.io.channels.write_address_channel.AWREADY

  // Mux: Write Data
  mem_slave.io.channels.write_data_channel.WVALID := Mux(dma_active, dma.io.master.w.valid, bus_switch.io.slaves(0).write_data_channel.WVALID)
  mem_slave.io.channels.write_data_channel.WDATA  := Mux(dma_active, dma.io.master.w.data,  bus_switch.io.slaves(0).write_data_channel.WDATA)
  mem_slave.io.channels.write_data_channel.WSTRB  := Mux(dma_active, dma.io.master.w.strb,  bus_switch.io.slaves(0).write_data_channel.WSTRB)
  
  dma.io.master.w.ready := dma_active && mem_slave.io.channels.write_data_channel.WREADY
  bus_switch.io.slaves(0).write_data_channel.WREADY := !dma_active && mem_slave.io.channels.write_data_channel.WREADY

  // Mux: Write Response
  dma.io.master.b.valid := dma_active && mem_slave.io.channels.write_response_channel.BVALID
  dma.io.master.b.resp  := mem_slave.io.channels.write_response_channel.BRESP
  dma.io.master.b.id    := 0.U
  
  bus_switch.io.slaves(0).write_response_channel.BVALID := !dma_active && mem_slave.io.channels.write_response_channel.BVALID
  bus_switch.io.slaves(0).write_response_channel.BRESP  := mem_slave.io.channels.write_response_channel.BRESP
  
  mem_slave.io.channels.write_response_channel.BREADY := Mux(dma_active, dma.io.master.b.ready, bus_switch.io.slaves(0).write_response_channel.BREADY)

  // Mux: Read Address
  mem_slave.io.channels.read_address_channel.ARVALID := Mux(dma_active, dma.io.master.ar.valid, bus_switch.io.slaves(0).read_address_channel.ARVALID)
  mem_slave.io.channels.read_address_channel.ARADDR  := Mux(dma_active, dma.io.master.ar.addr,  bus_switch.io.slaves(0).read_address_channel.ARADDR)
  mem_slave.io.channels.read_address_channel.ARPROT  := Mux(dma_active, dma.io.master.ar.prot,  bus_switch.io.slaves(0).read_address_channel.ARPROT)

  dma.io.master.ar.ready := dma_active && mem_slave.io.channels.read_address_channel.ARREADY
  bus_switch.io.slaves(0).read_address_channel.ARREADY := !dma_active && mem_slave.io.channels.read_address_channel.ARREADY

  // Mux: Read Data
  dma.io.master.r.valid := dma_active && mem_slave.io.channels.read_data_channel.RVALID
  dma.io.master.r.data  := mem_slave.io.channels.read_data_channel.RDATA
  dma.io.master.r.resp  := mem_slave.io.channels.read_data_channel.RRESP
  dma.io.master.r.last  := true.B 
  dma.io.master.r.id    := 0.U
  
  bus_switch.io.slaves(0).read_data_channel.RVALID := !dma_active && mem_slave.io.channels.read_data_channel.RVALID
  bus_switch.io.slaves(0).read_data_channel.RDATA  := mem_slave.io.channels.read_data_channel.RDATA
  bus_switch.io.slaves(0).read_data_channel.RRESP  := mem_slave.io.channels.read_data_channel.RRESP
  
  mem_slave.io.channels.read_data_channel.RREADY := Mux(dma_active, dma.io.master.r.ready, bus_switch.io.slaves(0).read_data_channel.RREADY)

  // Peripherals
  vga.io.pixClock     := io.vga_pixclk
  io.vga_hsync        := vga.io.hsync
  io.vga_vsync        := vga.io.vsync
  io.vga_rrggbb       := vga.io.rrggbb
  io.vga_activevideo := vga.io.activevideo
  io.vga_x_pos        := vga.io.x_pos
  io.vga_y_pos        := vga.io.y_pos
  io.uart_txd         := uart.io.txd
  uart.io.rxd         := io.uart_rxd
  io.uart_interrupt := uart.io.signal_interrupt
}

object VerilogGenerator extends App {
  (new ChiselStage).emitVerilog(
    new Top(),
    Array("--target-dir", "4-soc/verilog/verilator")
  )
}