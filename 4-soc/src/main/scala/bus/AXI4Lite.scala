// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package bus

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import riscv.Parameters

/**
 * AXI4-Lite Bus Protocol Implementation
 *
 * Implements ARM AMBA AXI4-Lite protocol for simple memory-mapped I/O.
 * AXI4-Lite is a subset of full AXI4, optimized for low-throughput
 * control/status register access with single-beat transactions only.
 *
 * Protocol Characteristics:
 * - Single-beat (non-burst) transactions only
 * - Fixed data width (32-bit in this implementation)
 * - Separate read and write channels (can operate independently)
 * - VALID/READY handshake on all channels
 *
 * Channel Summary:
 * - Write Address (AW): Master provides write address
 * - Write Data (W): Master provides write data with byte strobes
 * - Write Response (B): Slave acknowledges write completion
 * - Read Address (AR): Master provides read address
 * - Read Data (R): Slave returns read data with response
 */
object AXI4Lite {
  val protWidth = 3 // Protection type width (privileged, secure, instruction/data)
  val respWidth = 2 // Response width (OKAY=0, EXOKAY=1, SLVERR=2, DECERR=3)
}

class AXI4LiteWriteAddressChannel(addrWidth: Int) extends Bundle {
  val AWVALID = Output(Bool())
  val AWREADY = Input(Bool())
  val AWADDR  = Output(UInt(addrWidth.W))
  val AWPROT  = Output(UInt(AXI4Lite.protWidth.W))
}

class AXI4LiteWriteDataChannel(dataWidth: Int) extends Bundle {
  val WVALID = Output(Bool())
  val WREADY = Input(Bool())
  val WDATA  = Output(UInt(dataWidth.W))
  val WSTRB  = Output(UInt((dataWidth / 8).W))
}

class AXI4LiteWriteResponseChannel extends Bundle {
  val BVALID = Input(Bool())
  val BREADY = Output(Bool())
  val BRESP  = Input(UInt(AXI4Lite.respWidth.W))
}

class AXI4LiteReadAddressChannel(addrWidth: Int) extends Bundle {
  val ARVALID = Output(Bool())
  val ARREADY = Input(Bool())
  val ARADDR  = Output(UInt(addrWidth.W))
  val ARPROT  = Output(UInt(AXI4Lite.protWidth.W))
}

class AXI4LiteReadDataChannel(dataWidth: Int) extends Bundle {
  val RVALID = Input(Bool())
  val RREADY = Output(Bool())
  val RDATA  = Input(UInt(dataWidth.W))
  val RRESP  = Input(UInt(AXI4Lite.respWidth.W))
}

class AXI4LiteInterface(addrWidth: Int, dataWidth: Int) extends Bundle {
  val AWVALID = Output(Bool())
  val AWREADY = Input(Bool())
  val AWADDR  = Output(UInt(addrWidth.W))
  val AWPROT  = Output(UInt(AXI4Lite.protWidth.W))
  val WVALID  = Output(Bool())
  val WREADY  = Input(Bool())
  val WDATA   = Output(UInt(dataWidth.W))
  val WSTRB   = Output(UInt((dataWidth / 8).W))
  val BVALID  = Input(Bool())
  val BREADY  = Output(Bool())
  val BRESP   = Input(UInt(AXI4Lite.respWidth.W))
  val ARVALID = Output(Bool())
  val ARREADY = Input(Bool())
  val ARADDR  = Output(UInt(addrWidth.W))
  val ARPROT  = Output(UInt(AXI4Lite.protWidth.W))
  val RVALID  = Input(Bool())
  val RREADY  = Output(Bool())
  val RDATA   = Input(UInt(dataWidth.W))
  val RRESP   = Input(UInt(AXI4Lite.respWidth.W))
}

class AXI4LiteChannels(addrWidth: Int, dataWidth: Int) extends Bundle {
  val write_address_channel  = new AXI4LiteWriteAddressChannel(addrWidth)
  val write_data_channel     = new AXI4LiteWriteDataChannel(dataWidth)
  val write_response_channel = new AXI4LiteWriteResponseChannel()
  val read_address_channel   = new AXI4LiteReadAddressChannel(addrWidth)
  val read_data_channel      = new AXI4LiteReadDataChannel(dataWidth)
}

// Bundle for slave device to interact with AXI4-Lite bus
class AXI4LiteSlaveBundle(addrWidth: Int, dataWidth: Int) extends Bundle {
  val address      = Output(UInt(addrWidth.W))
  val read         = Output(Bool())           // tell slave device to read
  val read_data    = Input(UInt(dataWidth.W)) // data read from slave device
  val read_valid   = Input(Bool())            // indicates if read_data is valid
  val write        = Output(Bool())           // tell slave device to write
  val write_data   = Output(UInt(dataWidth.W))
  val write_strobe = Output(Vec(Parameters.WordSize, Bool()))
}

// Bundle for master device to interact with AXI4-Lite bus
class AXI4LiteMasterBundle(addrWidth: Int, dataWidth: Int) extends Bundle {
  val address             = Input(UInt(addrWidth.W))
  val read                = Input(Bool())  // request a read transaction
  val write               = Input(Bool())  // request a write transaction
  val read_data           = Output(UInt(dataWidth.W))
  val write_data          = Input(UInt(dataWidth.W))
  val write_strobe        = Input(Vec(Parameters.WordSize, Bool()))
  val busy                = Output(Bool()) // if busy, master is not ready
  val read_valid          = Output(Bool()) // read transaction complete
  val write_valid         = Output(Bool()) // write transaction complete (BRESP received)
  val write_data_accepted = Output(Bool()) // write data accepted by slave (WREADY && WVALID)
}

object AXI4LiteStates extends ChiselEnum {
  val Idle, ReadAddr, ReadData, WriteAddr, WriteData, WriteResp = Value
}

class AXI4LiteSlave(addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val channels = Flipped(new AXI4LiteChannels(addrWidth, dataWidth))
    val bundle   = new AXI4LiteSlaveBundle(addrWidth, dataWidth)
  })

  val state = RegInit(AXI4LiteStates.Idle)

  val addr = RegInit(0.U(addrWidth.W)) // Fixed: was dataWidth, should be addrWidth
  io.bundle.address := addr

  // Read signals
  val read = RegInit(false.B)
  io.bundle.read := read
  val read_data = RegInit(0.U(dataWidth.W))
  io.channels.read_data_channel.RDATA := read_data

  val ARREADY = RegInit(false.B)
  io.channels.read_address_channel.ARREADY := ARREADY
  val RVALID = RegInit(false.B)
  io.channels.read_data_channel.RVALID := RVALID
  val RRESP = RegInit(0.U(AXI4Lite.respWidth.W))
  io.channels.read_data_channel.RRESP := RRESP

  // Write signals
  val write = RegInit(false.B)
  io.bundle.write := write
  val write_data = RegInit(0.U(dataWidth.W))
  io.bundle.write_data := write_data
  val write_strobe = RegInit(VecInit(Seq.fill(Parameters.WordSize)(false.B)))
  io.bundle.write_strobe := write_strobe

  val AWREADY = RegInit(false.B)
  io.channels.write_address_channel.AWREADY := AWREADY
  val WREADY = RegInit(false.B)
  io.channels.write_data_channel.WREADY := WREADY
  val BVALID = RegInit(false.B)
  io.channels.write_response_channel.BVALID := BVALID
  val BRESP = WireInit(0.U(AXI4Lite.respWidth.W))
  io.channels.write_response_channel.BRESP := BRESP

  switch(state) {
    is(AXI4LiteStates.Idle) {
      read  := false.B
      write := false.B

      when(io.channels.read_address_channel.ARVALID) {
        // Read request
        state   := AXI4LiteStates.ReadAddr
        ARREADY := true.B
      }.elsewhen(io.channels.write_address_channel.AWVALID) {
        // Write request
        state   := AXI4LiteStates.WriteAddr
        AWREADY := true.B
      }
    }

    is(AXI4LiteStates.ReadAddr) {
      when(io.channels.read_address_channel.ARVALID && ARREADY) {
        // Capture address
        addr    := io.channels.read_address_channel.ARADDR
        ARREADY := false.B
        read    := true.B
        state   := AXI4LiteStates.ReadData
      }
    }

    is(AXI4LiteStates.ReadData) {
      when(io.bundle.read_valid) {
        // Data ready from slave device
        read_data := io.bundle.read_data
        RVALID    := true.B
        RRESP     := 0.U // OKAY response
        read      := false.B
      }

      when(RVALID && io.channels.read_data_channel.RREADY) {
        // Master acknowledged data
        RVALID := false.B
        state  := AXI4LiteStates.Idle
      }
    }

    is(AXI4LiteStates.WriteAddr) {
      when(io.channels.write_address_channel.AWVALID && AWREADY) {
        // Capture write address
        addr    := io.channels.write_address_channel.AWADDR
        AWREADY := false.B
        WREADY  := true.B
        state   := AXI4LiteStates.WriteData
      }
    }

    is(AXI4LiteStates.WriteData) {
      when(io.channels.write_data_channel.WVALID && WREADY) {
        // Capture write data
        write_data   := io.channels.write_data_channel.WDATA
        write_strobe := VecInit(io.channels.write_data_channel.WSTRB.asBools)
        WREADY       := false.B
        write        := true.B
        state        := AXI4LiteStates.WriteResp
      }
    }

    is(AXI4LiteStates.WriteResp) {
      write  := false.B
      BVALID := true.B
      BRESP  := 0.U // OKAY response

      when(BVALID && io.channels.write_response_channel.BREADY) {
        // Master acknowledged write completion
        BVALID := false.B
        state  := AXI4LiteStates.Idle
      }
    }
  }
}

class AXI4LiteMaster(addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val channels = new AXI4LiteChannels(addrWidth, dataWidth)
    val bundle   = new AXI4LiteMasterBundle(addrWidth, dataWidth)
  })

  val state = RegInit(AXI4LiteStates.Idle)
  io.bundle.busy := state =/= AXI4LiteStates.Idle

  val addr = RegInit(0.U(addrWidth.W)) // Fixed: use addrWidth, not dataWidth
  io.channels.read_address_channel.ARADDR  := addr
  io.channels.write_address_channel.AWADDR := addr

  // Read signals
  val read_valid = RegInit(false.B)
  io.bundle.read_valid := read_valid
  val read_data = RegInit(0.U(dataWidth.W))
  io.bundle.read_data := read_data

  val ARVALID = RegInit(false.B)
  io.channels.read_address_channel.ARVALID := ARVALID
  val RREADY = RegInit(false.B)
  io.channels.read_data_channel.RREADY := RREADY

  io.channels.read_address_channel.ARPROT := 0.U

  // Write signals
  val write_valid = RegInit(false.B)
  io.bundle.write_valid := write_valid
  val write_data = RegInit(0.U(dataWidth.W))
  io.channels.write_data_channel.WDATA := write_data
  val write_strobe = RegInit(VecInit(Seq.fill(Parameters.WordSize)(false.B)))
  io.channels.write_data_channel.WSTRB := write_strobe.asUInt

  val AWVALID = RegInit(false.B)
  io.channels.write_address_channel.AWVALID := AWVALID
  val WVALID = RegInit(false.B)
  io.channels.write_data_channel.WVALID := WVALID
  val BREADY = RegInit(false.B)
  io.channels.write_response_channel.BREADY := BREADY

  // Posted-write optimization: Signal when write data is accepted (before BRESP)
  val write_data_accepted = RegInit(false.B)
  io.bundle.write_data_accepted := write_data_accepted

  io.channels.write_address_channel.AWPROT := 0.U

  // Performance optimization: Assert ARVALID/AWVALID in Idle to save 1 cycle
  // per transaction. Both addr and ARVALID/AWVALID are registered, so they
  // update synchronously on the same clock edge - no address capture race.
  // The CPU already latches bus_address_reg, so BusSwitch routing is stable.
  switch(state) {
    is(AXI4LiteStates.Idle) {
      read_valid          := false.B
      write_valid         := false.B
      write_data_accepted := false.B

      when(io.bundle.read && !io.bundle.write) {
        // Start read transaction - assert ARVALID immediately (saves 1 cycle)
        addr    := io.bundle.address
        ARVALID := true.B
        RREADY  := true.B
        state   := AXI4LiteStates.ReadData
      }.elsewhen(io.bundle.write) {
        // Start write transaction - assert AWVALID/WVALID immediately (saves 1 cycle)
        addr         := io.bundle.address
        write_data   := io.bundle.write_data
        write_strobe := io.bundle.write_strobe
        AWVALID      := true.B
        WVALID       := true.B
        state        := AXI4LiteStates.WriteData
      }
    }

    is(AXI4LiteStates.ReadData) {
      // Deassert ARVALID after address handshake completes
      when(ARVALID && io.channels.read_address_channel.ARREADY) {
        ARVALID := false.B
      }

      when(io.channels.read_data_channel.RVALID && RREADY) {
        // Data received from slave
        read_data  := io.channels.read_data_channel.RDATA
        RREADY     := false.B
        read_valid := true.B
        state      := AXI4LiteStates.Idle
      }
    }

    is(AXI4LiteStates.WriteData) {
      // Deassert AWVALID after address handshake completes
      when(AWVALID && io.channels.write_address_channel.AWREADY) {
        AWVALID := false.B
      }

      when(WVALID && io.channels.write_data_channel.WREADY) {
        // Data accepted by slave - signal for posted-write optimization
        WVALID              := false.B
        BREADY              := true.B
        write_data_accepted := true.B // Pipeline can proceed before BRESP
        state               := AXI4LiteStates.WriteResp
      }
    }

    is(AXI4LiteStates.WriteResp) {
      // Fix AXI4-Lite protocol: ensure AWVALID is cleared after address handshake
      // If WREADY arrived before AWREADY in WriteData, AWVALID may still be high
      when(AWVALID && io.channels.write_address_channel.AWREADY) {
        AWVALID := false.B
      }

      when(io.channels.write_response_channel.BVALID && BREADY) {
        // Write acknowledged
        BREADY      := false.B
        write_valid := true.B
        state       := AXI4LiteStates.Idle
      }
    }
  }
}
