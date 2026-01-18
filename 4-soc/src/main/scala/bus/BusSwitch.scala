package bus

import chisel3._
import chisel3.util._
import riscv.Parameters

class BusSwitch extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(Parameters.AddrWidth))
    val slaves  = Vec(Parameters.SlaveDeviceCount, new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
    val master  = Flipped(new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
  })

  // ---------------------------------------------------------
  // 1. Address Decoding Logic
  // ---------------------------------------------------------
  
  // Default Decoding (Legacy bit-slicing [31:29])
  val defaultIndex = io.address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)
  val defaultSel   = UIntToOH(defaultIndex, Parameters.SlaveDeviceCount)

  // Address Overrides for Peripherals
  // UART: 0x1000_0000 - 0x1000_0FFF (Slave 2)
  val isUART = io.address >= 0x10000000.U && io.address < 0x10001000.U
  
  // DMA:  0x1000_2000 - 0x1000_2FFF (Slave 3)
  val isDMA  = io.address >= 0x10002000.U && io.address < 0x10003000.U

  // Final Selection Mux (Priority: DMA > UART > Default)
  val sel = Mux(isDMA,  UIntToOH(3.U, Parameters.SlaveDeviceCount), 
            Mux(isUART, UIntToOH(2.U, Parameters.SlaveDeviceCount), 
            defaultSel))                                            

  // ---------------------------------------------------------
  // 2. Latch Logic for Stability
  // ---------------------------------------------------------
  val read_sel  = RegInit(0.U(Parameters.SlaveDeviceCount.W))
  val write_sel = RegInit(0.U(Parameters.SlaveDeviceCount.W))

  // Latch read selection
  when(io.master.read_address_channel.ARVALID) {
    read_sel := sel
  }.elsewhen(io.master.read_data_channel.RVALID && io.master.read_data_channel.RREADY) {
    read_sel := 0.U
  }

  // Latch write selection
  when(io.master.write_address_channel.AWVALID || io.master.write_data_channel.WVALID) {
    write_sel := sel
  }.elsewhen(io.master.write_response_channel.BVALID && io.master.write_response_channel.BREADY) {
    write_sel := 0.U
  }

  // ---------------------------------------------------------
  // 3. Slave Drive Logic
  // ---------------------------------------------------------
  for (i <- 0 until Parameters.SlaveDeviceCount) {
    val hit = sel(i)

    // Write Channel
    io.slaves(i).write_address_channel.AWVALID := io.master.write_address_channel.AWVALID && hit
    io.slaves(i).write_address_channel.AWADDR  := io.master.write_address_channel.AWADDR
    io.slaves(i).write_address_channel.AWPROT  := io.master.write_address_channel.AWPROT
    io.slaves(i).write_data_channel.WVALID := io.master.write_data_channel.WVALID && hit
    io.slaves(i).write_data_channel.WDATA  := io.master.write_data_channel.WDATA
    io.slaves(i).write_data_channel.WSTRB  := io.master.write_data_channel.WSTRB
    io.slaves(i).write_response_channel.BREADY := io.master.write_response_channel.BREADY && write_sel(i)

    // Read Channel
    io.slaves(i).read_address_channel.ARVALID := io.master.read_address_channel.ARVALID && hit
    io.slaves(i).read_address_channel.ARADDR  := io.master.read_address_channel.ARADDR
    io.slaves(i).read_address_channel.ARPROT  := io.master.read_address_channel.ARPROT
    io.slaves(i).read_data_channel.RREADY := io.master.read_data_channel.RREADY && read_sel(i)
  }

  // ---------------------------------------------------------
  // 4. Response Multiplexing
  // ---------------------------------------------------------
  io.master.write_address_channel.AWREADY := Mux1H(sel, io.slaves.map(_.write_address_channel.AWREADY))
  io.master.write_data_channel.WREADY     := Mux1H(sel, io.slaves.map(_.write_data_channel.WREADY))
  io.master.write_response_channel.BVALID := Mux1H(write_sel, io.slaves.map(_.write_response_channel.BVALID))
  io.master.write_response_channel.BRESP  := Mux1H(write_sel, io.slaves.map(_.write_response_channel.BRESP))

  io.master.read_address_channel.ARREADY := Mux1H(sel, io.slaves.map(_.read_address_channel.ARREADY))
  io.master.read_data_channel.RVALID     := Mux1H(read_sel, io.slaves.map(_.read_data_channel.RVALID))
  io.master.read_data_channel.RDATA      := Mux1H(read_sel, io.slaves.map(_.read_data_channel.RDATA))
  io.master.read_data_channel.RRESP      := Mux1H(read_sel, io.slaves.map(_.read_data_channel.RRESP))
}