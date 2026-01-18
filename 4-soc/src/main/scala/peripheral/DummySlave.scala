// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import bus.AXI4Lite
import bus.AXI4LiteChannels
import chisel3._
import riscv.Parameters

/**
 * Dummy AXI4-Lite slave that responds with DECERR (decode error)
 *
 * This is used for unmapped address regions. Instead of hanging the bus
 * (which would cause a deadlock), it properly completes transactions with
 * an error response. The CPU can then handle the error appropriately.
 *
 * AXI4-Lite response codes:
 *   OKAY   = 0 - Normal access success
 *   EXOKAY = 1 - Exclusive access success (not used in AXI4-Lite)
 *   SLVERR = 2 - Slave error (device exists but access failed)
 *   DECERR = 3 - Decode error (no device at this address)
 */
class DummySlave extends Module {
  val io = IO(new Bundle {
    val channels = Flipped(new AXI4LiteChannels(4, Parameters.DataBits))
  })

  val DECERR = 3.U(AXI4Lite.respWidth.W)

  // Read path: Accept address immediately, return DECERR
  val read_pending  = RegInit(false.B)
  val write_pending = RegInit(false.B)
  val write_data_ok = RegInit(false.B)

  // Read address channel - accept immediately
  io.channels.read_address_channel.ARREADY := !read_pending

  when(io.channels.read_address_channel.ARVALID && io.channels.read_address_channel.ARREADY) {
    read_pending := true.B
  }

  // Read data channel - respond with DECERR
  io.channels.read_data_channel.RVALID := read_pending
  io.channels.read_data_channel.RDATA  := "hDEADBEEF".U // Distinctive pattern for debugging
  io.channels.read_data_channel.RRESP  := DECERR

  when(read_pending && io.channels.read_data_channel.RREADY) {
    read_pending := false.B
  }

  // Write address channel - accept immediately
  io.channels.write_address_channel.AWREADY := !write_pending

  when(io.channels.write_address_channel.AWVALID && io.channels.write_address_channel.AWREADY) {
    write_pending := true.B
  }

  // Write data channel - accept immediately when write pending
  io.channels.write_data_channel.WREADY := write_pending && !write_data_ok

  when(write_pending && io.channels.write_data_channel.WVALID && io.channels.write_data_channel.WREADY) {
    write_data_ok := true.B
  }

  // Write response channel - respond with DECERR
  io.channels.write_response_channel.BVALID := write_data_ok
  io.channels.write_response_channel.BRESP  := DECERR

  when(write_data_ok && io.channels.write_response_channel.BREADY) {
    write_pending := false.B
    write_data_ok := false.B
  }
}
