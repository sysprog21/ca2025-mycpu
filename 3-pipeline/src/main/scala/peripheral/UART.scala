// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import chisel3.util._

class UartIO extends DecoupledIO(UInt(8.W)) {}

/**
 * UART Transmitter Module
 *
 * Implements 8-N-1 serial transmission (8 data bits, no parity, 1 stop bit)
 * with ready/valid handshaking protocol.
 *
 * @param frequency System clock frequency in Hz
 * @param baudRate Target baud rate in bits/second
 *
 * Features:
 * - Minimal design without FIFO buffering
 * - Two stop bits for reliable transmission
 * - Automatic bit rate generation from frequency/baudRate
 *
 * Timing:
 * - BIT_CNT = (frequency + baudRate/2) / baudRate - 1
 * - Ready when idle (cntReg == 0 && bitsReg == 0)
 *
 * Protocol:
 * - 11-bit frame: 1 start + 8 data + 2 stop bits
 * - LSB first transmission
 * - Idle state: TXD = 1
 */
class Tx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd     = Output(UInt(1.W))
    val channel = Flipped(new UartIO())

  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).U

  val shiftReg = RegInit(0x7ff.U)
  val cntReg   = RegInit(0.U(20.W))
  val bitsReg  = RegInit(0.U(4.W))

  io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.txd           := shiftReg(0)

  when(cntReg === 0.U) {

    cntReg := BIT_CNT
    when(bitsReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := Cat(1.U, shift(9, 0))
      bitsReg  := bitsReg - 1.U
    }.otherwise {
      when(io.channel.valid) {
        shiftReg := Cat(Cat(3.U, io.channel.bits), 0.U) // two stop bits, data, one start bit
        bitsReg  := 11.U
      }.otherwise {
        shiftReg := 0x7ff.U
      }
    }

  }.otherwise {
    cntReg := cntReg - 1.U
  }
}

/**
 * UART Receiver Module
 *
 * Implements 8-N-1 serial reception with automatic start bit detection
 * and ready/valid handshaking protocol.
 *
 * @param frequency System clock frequency in Hz
 * @param baudRate Target baud rate in bits/second
 *
 * Features:
 * - Two-stage synchronizer for async RX signal (metastability protection)
 * - Start bit detection with 1.5 bit delay for mid-bit sampling
 * - Automatic data capture with valid flag generation
 *
 * Timing:
 * - BIT_CNT = (frequency + baudRate/2) / baudRate - 1 (bit period)
 * - START_CNT = (3*frequency/2 + baudRate/2) / baudRate - 1 (1.5 bits)
 * - Mid-bit sampling ensures reliable capture despite clock tolerance
 *
 * Protocol:
 * - 10-bit frame: 1 start + 8 data + 1 stop bit
 * - LSB first reception
 * - Valid flag set after 8th bit captured
 *
 * Attribution:
 * Inspired by Tommy Thorn's YARVI UART receiver
 * https://github.com/tommythorn/yarvi
 */
class Rx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val rxd     = Input(UInt(1.W))
    val channel = new UartIO()

  })

  val BIT_CNT   = ((frequency + baudRate / 2) / baudRate - 1).U
  val START_CNT = ((3 * frequency / 2 + baudRate / 2) / baudRate - 1).U

  // Sync in the asynchronous RX data, reset to 1 to not start reading after a reset
  val rxReg = RegNext(RegNext(io.rxd, 1.U), 1.U)

  val shiftReg = RegInit(0.U(8.W))
  val cntReg   = RegInit(0.U(20.W))
  val bitsReg  = RegInit(0.U(4.W))
  val valReg   = RegInit(false.B)

  when(cntReg =/= 0.U) {
    cntReg := cntReg - 1.U
  }.elsewhen(bitsReg =/= 0.U) {
    cntReg   := BIT_CNT
    shiftReg := Cat(rxReg, shiftReg >> 1)
    bitsReg  := bitsReg - 1.U
    // the last shifted in
    when(bitsReg === 1.U) {
      valReg := true.B
    }
  }.elsewhen(rxReg === 0.U) { // wait 1.5 bits after falling edge of start
    cntReg  := START_CNT
    bitsReg := 8.U
  }

  when(valReg && io.channel.ready) {
    valReg := false.B
  }

  io.channel.bits  := shiftReg
  io.channel.valid := valReg
}

/**
 * A single byte buffer with a ready/valid interface
 */
class Buffer extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(new UartIO())
    val out = new UartIO()
  })

  val empty :: full :: Nil = Enum(2)
  val stateReg             = RegInit(empty)
  val dataReg              = RegInit(0.U(8.W))

  io.in.ready  := stateReg === empty
  io.out.valid := stateReg === full

  when(stateReg === empty) {
    when(io.in.valid) {
      dataReg  := io.in.bits
      stateReg := full
    }
  }.otherwise { // full
    when(io.out.ready) {
      stateReg := empty
    }
  }
  io.out.bits := dataReg
}

/**
 * A transmitter with a single buffer.
 */
class BufferedTx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd     = Output(UInt(1.W))
    val channel = Flipped(new UartIO())

  })
  val tx  = Module(new Tx(frequency, baudRate))
  val buf = Module(new Buffer)

  buf.io.in <> io.channel
  tx.io.channel <> buf.io.out
  io.txd <> tx.io.txd
}

class Uart(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val bundle = new RAMBundle
    val rxd    = Input(UInt(1.W))
    val txd    = Output(UInt(1.W))

    val signal_interrupt = Output(Bool())
  })
  val interrupt = RegInit(false.B)
  val rxData    = RegInit(0.U)

  val tx = Module(new BufferedTx(frequency, baudRate))
  val rx = Module(new Rx(frequency, baudRate))

  io.bundle.read_data := 0.U
  when(io.bundle.address === 0x4.U) {
    io.bundle.read_data := baudRate.U
  }.elsewhen(io.bundle.address === 0xc.U) {
    io.bundle.read_data := rxData
    interrupt           := false.B
  }

  tx.io.channel.valid := false.B
  tx.io.channel.bits  := 0.U
  when(io.bundle.write_enable) {
    when(io.bundle.address === 0x8.U) {
      interrupt := io.bundle.write_data =/= 0.U
    }.elsewhen(io.bundle.address === 0x10.U) {
      tx.io.channel.valid := true.B
      tx.io.channel.bits  := io.bundle.write_data
    }
  }

  io.txd    := tx.io.txd
  rx.io.rxd := io.rxd

  io.signal_interrupt := interrupt
  rx.io.channel.ready := false.B
  when(rx.io.channel.valid) {
    rx.io.channel.ready := true.B
    rxData              := rx.io.channel.bits
    interrupt           := true.B
  }
}
