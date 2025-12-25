// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import bus.AXI4LiteChannels
import bus.AXI4LiteSlave
import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * UART protocol constants
 */
object UartConstants {
  // Frame format: 1 start bit + 8 data bits + 2 stop bits = 11 bits
  val DATA_BITS      = 8
  val STOP_BITS      = 2
  val START_BITS     = 1
  val TX_FRAME_BITS  = START_BITS + DATA_BITS + STOP_BITS // 11
  val TX_IDLE        = (1 << TX_FRAME_BITS) - 1           // 0x7ff (all ones = idle high)
  val TX_SHIFT_WIDTH = TX_FRAME_BITS                      // 11-bit shift register
  val COUNTER_WIDTH  = 16                                 // Supports clock/baud up to 65535 (100MHz @ 1526 baud minimum)

  // Register offsets from UART base address
  val REG_STATUS    = 0x00 // TX ready (bit 0), RX valid (bit 1) - read-only
  val REG_BAUD_RATE = 0x04 // Configured baud rate - read-only
  val REG_INTERRUPT = 0x08 // Interrupt enable - write-only
  val REG_RX_DATA   = 0x0c // Received data - read clears interrupt
  val REG_TX_DATA   = 0x10 // Transmit data - write-only
}

class UartIO extends DecoupledIO(UInt(8.W))

/**
 * Transmit part of the UART.
 * A minimal version without any additional buffering.
 * Use a ready/valid handshaking.
 */
class Tx(frequency: Int, baudRate: Int) extends Module {
  import UartConstants._

  val io = IO(new Bundle {
    val txd     = Output(UInt(1.W))
    val channel = Flipped(new UartIO())
  })

  // Clocks per bit: rounds to nearest integer for baud rate accuracy
  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).U

  val shiftReg = RegInit(TX_IDLE.U(TX_SHIFT_WIDTH.W))
  val cntReg   = RegInit(0.U(COUNTER_WIDTH.W))
  val bitsReg  = RegInit(0.U(4.W))

  io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.txd           := shiftReg(0)

  when(cntReg === 0.U) {
    cntReg := BIT_CNT
    when(bitsReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := Cat(1.U, shift(TX_SHIFT_WIDTH - 2, 0))
      bitsReg  := bitsReg - 1.U
    }.otherwise {
      when(io.channel.valid) {
        // Frame: [stop1][stop0][data7..data0][start] (LSB first)
        // Stop bits are 1s (high), start bit is 0 (low)
        shiftReg := Cat(Cat(3.U(STOP_BITS.W), io.channel.bits), 0.U(START_BITS.W))
        bitsReg  := TX_FRAME_BITS.U
      }.otherwise {
        shiftReg := TX_IDLE.U
      }
    }
  }.otherwise {
    cntReg := cntReg - 1.U
  }
}

/**
 * Receive part of the UART.
 * A minimal version without any additional buffering.
 * Use a ready/valid handshaking.
 */
class Rx(frequency: Int, baudRate: Int) extends Module {
  import UartConstants._

  val io = IO(new Bundle {
    val rxd     = Input(UInt(1.W))
    val channel = new UartIO()
  })

  // Clocks per bit (same formula as Tx)
  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).U
  // Sample at middle of start bit: 1.5 bit times from falling edge
  val START_CNT = ((3 * frequency / 2 + baudRate / 2) / baudRate - 1).U

  // Sync in the asynchronous RX data (2-stage synchronizer)
  val rxReg = RegNext(RegNext(io.rxd, 1.U), 1.U)

  val shiftReg = RegInit(0.U(DATA_BITS.W))
  val cntReg   = RegInit(0.U(COUNTER_WIDTH.W))
  val bitsReg  = RegInit(0.U(4.W))
  val valReg   = RegInit(false.B)

  when(cntReg =/= 0.U) {
    cntReg := cntReg - 1.U
  }.elsewhen(bitsReg =/= 0.U) {
    cntReg   := BIT_CNT
    shiftReg := Cat(rxReg, shiftReg >> 1)
    bitsReg  := bitsReg - 1.U
    when(bitsReg === 1.U) {
      valReg := true.B
    }
  }.elsewhen(rxReg === 0.U && !valReg) {
    // Falling edge detected - start bit
    // Only start receiving if previous byte was consumed (backpressure)
    cntReg  := START_CNT
    bitsReg := DATA_BITS.U
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
  }.otherwise {
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

/**
 * UART peripheral with AXI4-Lite interface
 *
 * Memory map (offset from base address):
 *   0x00: STATUS    - TX ready (bit 0), RX FIFO non-empty (bit 1) (read-only)
 *   0x04: BAUD_RATE - Configured baud rate (read-only)
 *   0x08: INTERRUPT - Interrupt enable/status (write: enable, read: N/A)
 *   0x0C: RX_DATA   - Received data from FIFO (read dequeues one byte)
 *   0x10: TX_DATA   - Transmit data (write-only)
 *
 * Features:
 *   - 4-entry RX FIFO: Buffers incoming bytes to prevent character loss
 *     when CPU is busy. STATUS.rx_valid reflects FIFO non-empty status.
 *   - Backpressure: RX module won't start receiving new byte if FIFO is full.
 *
 * Limitations:
 *   - Single-byte TX buffer: If TX buffer is full when CPU writes to TX_DATA,
 *     the write is silently dropped. Software should poll STATUS.tx_ready
 *     before writing. For production use, consider adding a TX FIFO.
 *
 * @param frequency System clock frequency in Hz
 * @param baudRate  Serial baud rate (e.g., 115200)
 */
class Uart(frequency: Int, baudRate: Int) extends Module {
  import UartConstants._

  val io = IO(new Bundle {
    val channels         = Flipped(new AXI4LiteChannels(8, Parameters.DataBits))
    val rxd              = Input(UInt(1.W))
    val txd              = Output(UInt(1.W))
    val signal_interrupt = Output(Bool())
  })

  val interrupt = RegInit(false.B)
  val slave     = Module(new AXI4LiteSlave(8, Parameters.DataBits))
  slave.io.channels <> io.channels

  val tx = Module(new BufferedTx(frequency, baudRate))
  val rx = Module(new Rx(frequency, baudRate))

  // RX FIFO: 4-entry buffer to absorb bursts and prevent character loss
  // pipe=true allows same-cycle enqueue/dequeue, flow=false requires explicit ready
  val rxFifo = Module(new Queue(UInt(8.W), entries = 4, pipe = true, flow = false))

  // MMIO address decode (mask to get offset within peripheral)
  // UART registers at 0x00-0x1F, base address 0x40000000
  val addr           = slave.io.bundle.address & 0xff.U
  val addr_status    = addr === REG_STATUS.U
  val addr_baud_rate = addr === REG_BAUD_RATE.U
  val addr_interrupt = addr === REG_INTERRUPT.U
  val addr_rx_data   = addr === REG_RX_DATA.U
  val addr_tx_data   = addr === REG_TX_DATA.U

  // AXI4-Lite Read handling
  // Only assert read_valid when there's an active read request
  val read_data_prepared = WireDefault(0.U(32.W))
  slave.io.bundle.read_valid := slave.io.bundle.read
  slave.io.bundle.read_data  := read_data_prepared

  when(addr_status) {
    // Status register: bit 0 = TX ready, bit 1 = RX data valid (FIFO non-empty)
    read_data_prepared := Cat(0.U(30.W), rxFifo.io.deq.valid, tx.io.channel.ready)
  }.elsewhen(addr_baud_rate) {
    read_data_prepared := baudRate.U
  }.elsewhen(addr_rx_data) {
    read_data_prepared := rxFifo.io.deq.bits
  }

  // RX FIFO connections: RX module -> FIFO -> CPU read
  rxFifo.io.enq <> rx.io.channel
  // Dequeue from FIFO when CPU reads UART_RECV
  rxFifo.io.deq.ready := slave.io.bundle.read && addr_rx_data

  // Interrupt handling:
  // - Set when FIFO receives data
  // - Clear when reading RX_DATA makes FIFO empty, OR software writes 0 to INTERRUPT
  // - Software can manually set/clear via INTERRUPT register
  when(slave.io.bundle.write && addr_interrupt) {
    // Manual interrupt control takes priority
    interrupt := slave.io.bundle.write_data =/= 0.U
  }.elsewhen(rxFifo.io.enq.fire) {
    // Set interrupt when new data arrives in FIFO
    interrupt := true.B
  }.elsewhen(slave.io.bundle.read && addr_rx_data && rxFifo.io.deq.valid) {
    // Clear interrupt when reading RX_DATA and this is the last byte in FIFO
    // (count will be 1 before dequeue, 0 after)
    when(rxFifo.io.count === 1.U) {
      interrupt := false.B
    }
  }

  // TX channel: only write when buffer is ready (backpressure handling)
  tx.io.channel.valid := false.B
  tx.io.channel.bits  := 0.U
  when(slave.io.bundle.write) {
    when(addr_tx_data && tx.io.channel.ready) {
      // Only write to TX when buffer has space (prevents silent data loss)
      tx.io.channel.valid := true.B
      // Explicit 8-bit slice: UART TX is 8 bits, write_data is 32 bits
      // Standard UART practice: use lower byte, ignore upper bits
      tx.io.channel.bits := slave.io.bundle.write_data(7, 0)
    }
    // Note: If TX buffer full (ready=false), AXI write completes but data is dropped.
    // Production implementation should stall AXI response until buffer ready.
  }

  io.txd              := tx.io.txd
  rx.io.rxd           := io.rxd
  io.signal_interrupt := interrupt
}
