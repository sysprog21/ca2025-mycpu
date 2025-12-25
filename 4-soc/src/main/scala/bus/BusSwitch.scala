// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package bus

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * AXI4-Lite Bus Switch (Address Decoder)
 *
 * Routes AXI4-Lite transactions from a single master to multiple slaves
 * based on address decoding. Uses upper address bits [31:29] for routing,
 * supporting up to 8 slave devices.
 *
 * Memory Map (3-bit decode from address[31:29]):
 *   Slave 0: 0x0000_0000 - 0x1FFF_FFFF (Main Memory)
 *   Slave 1: 0x2000_0000 - 0x3FFF_FFFF (VGA Controller)
 *   Slave 2: 0x4000_0000 - 0x5FFF_FFFF (UART Controller)
 *   Slave 3: 0x6000_0000 - 0x7FFF_FFFF (Reserved/DummySlave)
 *   Slave 4: 0x8000_0000 - 0x9FFF_FFFF (Reserved/DummySlave)
 *   Slave 5: 0xA000_0000 - 0xBFFF_FFFF (Reserved/DummySlave)
 *   Slave 6: 0xC000_0000 - 0xDFFF_FFFF (Reserved/DummySlave)
 *   Slave 7: 0xE000_0000 - 0xFFFF_FFFF (Reserved/DummySlave)
 *
 * CRITICAL: Address Stability Requirement
 *   The io.address input MUST be stable for the duration of each AXI transaction.
 *   This switch uses combinational decode - if the address changes mid-transaction,
 *   routing will glitch and corrupt the transfer.
 *
 *   The CPU wrapper (CPU.scala) handles this by latching bus_address_reg when a
 *   new transaction starts and holding it while the AXI master is busy.
 *
 * Design Features:
 *   - Combinational one-hot decode: Zero-cycle latency for address routing
 *   - VALID gating: Only selected slave sees VALID assertions
 *   - Mux1H response: Fast one-hot multiplexer for slave responses
 *   - DummySlave support: Unmapped regions respond with DECERR (no deadlock)
 */
class BusSwitch extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(Parameters.AddrWidth))
    val slaves  = Vec(Parameters.SlaveDeviceCount, new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
    val master  = Flipped(new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
  })

  val index = io.address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)
  val sel   = UIntToOH(index, Parameters.SlaveDeviceCount)

  // Latched slave selection for response channels (hardening fix).
  // This ensures stable response routing even if io.address changes mid-transaction.
  // Without this, a timing race could cause RDATA/BRESP to be muxed from wrong slave.
  val read_sel  = RegInit(0.U(Parameters.SlaveDeviceCount.W))
  val write_sel = RegInit(0.U(Parameters.SlaveDeviceCount.W))

  // Latch read selection when ARVALID is asserted (start of read transaction).
  // Priority: New transaction latching takes precedence over clearing to handle
  // back-to-back transactions where completion and new start occur same cycle.
  when(io.master.read_address_channel.ARVALID) {
    read_sel := sel
  }.elsewhen(io.master.read_data_channel.RVALID && io.master.read_data_channel.RREADY) {
    read_sel := 0.U
  }

  // Latch write selection when AWVALID or WVALID is asserted (start of write transaction).
  // Priority: New transaction latching takes precedence over clearing.
  when(io.master.write_address_channel.AWVALID || io.master.write_data_channel.WVALID) {
    write_sel := sel
  }.elsewhen(io.master.write_response_channel.BVALID && io.master.write_response_channel.BREADY) {
    write_sel := 0.U
  }

  // Drive slaves: only the selected slave sees VALID/READY handshakes.
  // Use combinational sel for address/data phases, latched sel for response phases.
  for (i <- 0 until Parameters.SlaveDeviceCount) {
    val hit = sel(i)

    // Write address channel: combinational sel (transaction start)
    io.slaves(i).write_address_channel.AWVALID := io.master.write_address_channel.AWVALID && hit
    io.slaves(i).write_address_channel.AWADDR  := io.master.write_address_channel.AWADDR
    io.slaves(i).write_address_channel.AWPROT  := io.master.write_address_channel.AWPROT

    // Write data channel: combinational sel (transaction start)
    io.slaves(i).write_data_channel.WVALID := io.master.write_data_channel.WVALID && hit
    io.slaves(i).write_data_channel.WDATA  := io.master.write_data_channel.WDATA
    io.slaves(i).write_data_channel.WSTRB  := io.master.write_data_channel.WSTRB

    // Write response channel: latched sel (response phase)
    io.slaves(i).write_response_channel.BREADY := io.master.write_response_channel.BREADY && write_sel(i)

    // Read address channel: combinational sel (transaction start)
    io.slaves(i).read_address_channel.ARVALID := io.master.read_address_channel.ARVALID && hit
    io.slaves(i).read_address_channel.ARADDR  := io.master.read_address_channel.ARADDR
    io.slaves(i).read_address_channel.ARPROT  := io.master.read_address_channel.ARPROT

    // Read data channel: latched sel (response phase)
    io.slaves(i).read_data_channel.RREADY := io.master.read_data_channel.RREADY && read_sel(i)
  }

  // Multiplex slave responses back to the master
  // Use combinational sel for address handshake (AWREADY, ARREADY)
  // Use latched sel for response muxing (BVALID/BRESP, RVALID/RDATA/RRESP)
  io.master.write_address_channel.AWREADY := Mux1H(sel, io.slaves.map(_.write_address_channel.AWREADY))
  io.master.write_data_channel.WREADY     := Mux1H(sel, io.slaves.map(_.write_data_channel.WREADY))
  io.master.write_response_channel.BVALID := Mux1H(write_sel, io.slaves.map(_.write_response_channel.BVALID))
  io.master.write_response_channel.BRESP  := Mux1H(write_sel, io.slaves.map(_.write_response_channel.BRESP))

  io.master.read_address_channel.ARREADY := Mux1H(sel, io.slaves.map(_.read_address_channel.ARREADY))
  io.master.read_data_channel.RVALID     := Mux1H(read_sel, io.slaves.map(_.read_data_channel.RVALID))
  io.master.read_data_channel.RDATA      := Mux1H(read_sel, io.slaves.map(_.read_data_channel.RDATA))
  io.master.read_data_channel.RRESP      := Mux1H(read_sel, io.slaves.map(_.read_data_channel.RRESP))
}
