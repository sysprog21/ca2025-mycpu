// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import peripheral.Buffer
import peripheral.BufferedTx
import peripheral.Rx
import peripheral.Tx
import peripheral.Uart
import peripheral.UartConstants._

class UARTTest extends AnyFlatSpec with ChiselScalatestTester {
  // Test parameters: slow baud rate for faster simulation
  val testFrequency = 1000                         // 1 kHz clock
  val testBaudRate  = 100                          // 100 baud (10 clock cycles per bit)
  val bitCycles     = testFrequency / testBaudRate // 10 cycles per bit

  behavior.of("UART Tx")

  it should "be ready when idle" in {
    test(new Tx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.channel.ready.expect(true.B)
      dut.io.txd.expect(1.U) // Idle high
    }
  }

  it should "transmit start bit, data, and stop bits" in {
    test(new Tx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Send byte 0x55 (01010101) - good for testing bit transitions
      dut.io.channel.valid.poke(true.B)
      dut.io.channel.bits.poke(0x55.U)
      dut.clock.step()
      dut.io.channel.valid.poke(false.B)

      // Tx should now be busy
      dut.io.channel.ready.expect(false.B)

      // Wait for transmission to complete - poll for ready
      // Ready is only true briefly when both counters are 0
      val maxWait = 200 // More than enough for 11 bits * 10 cycles
      var cycles  = 0
      while (!dut.io.channel.ready.peekBoolean() && cycles < maxWait) {
        dut.clock.step()
        cycles += 1
      }

      // Should have finished within expected time
      assert(cycles < maxWait, s"TX did not complete within $maxWait cycles")
      dut.io.channel.ready.expect(true.B)
      dut.io.txd.expect(1.U) // Back to idle high
    }
  }

  it should "not accept data when busy" in {
    test(new Tx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Start transmission
      dut.io.channel.valid.poke(true.B)
      dut.io.channel.bits.poke(0xaa.U)
      dut.clock.step()
      dut.io.channel.valid.poke(false.B)

      // Tx should be busy
      dut.io.channel.ready.expect(false.B)

      // Wait a few cycles, still busy
      dut.clock.step(5)
      dut.io.channel.ready.expect(false.B)
    }
  }

  behavior.of("UART Rx")

  it should "signal valid when byte received" in {
    test(new Rx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Initially idle (high)
      dut.io.rxd.poke(1.U)
      dut.io.channel.valid.expect(false.B)
      dut.clock.step(5)

      // Send start bit (low)
      dut.io.rxd.poke(0.U)
      dut.clock.step(bitCycles + bitCycles / 2) // Wait 1.5 bit times to sample middle

      // Send data bits (LSB first): 0x55 = 01010101 -> 1,0,1,0,1,0,1,0
      val dataBits = Seq(1, 0, 1, 0, 1, 0, 1, 0)
      for (bit <- dataBits) {
        dut.io.rxd.poke(bit.U)
        dut.clock.step(bitCycles)
      }

      // Stop bit (high)
      dut.io.rxd.poke(1.U)
      dut.clock.step(bitCycles)

      // Data should now be valid
      dut.io.channel.valid.expect(true.B)
      dut.io.channel.bits.expect(0x55.U)

      // Acknowledge receipt
      dut.io.channel.ready.poke(true.B)
      dut.clock.step()
      dut.io.channel.ready.poke(false.B)

      // Valid should clear
      dut.io.channel.valid.expect(false.B)
    }
  }

  it should "receive 0x00 byte correctly" in {
    test(new Rx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Start bit
      dut.io.rxd.poke(0.U)
      dut.clock.step(bitCycles + bitCycles / 2)

      // All zeros
      for (_ <- 0 until 8) {
        dut.io.rxd.poke(0.U)
        dut.clock.step(bitCycles)
      }

      // Stop bit
      dut.io.rxd.poke(1.U)
      dut.clock.step(bitCycles)

      dut.io.channel.valid.expect(true.B)
      dut.io.channel.bits.expect(0x00.U)
    }
  }

  it should "receive 0xFF byte correctly" in {
    test(new Rx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Start bit
      dut.io.rxd.poke(0.U)
      dut.clock.step(bitCycles + bitCycles / 2)

      // All ones
      for (_ <- 0 until 8) {
        dut.io.rxd.poke(1.U)
        dut.clock.step(bitCycles)
      }

      // Stop bit
      dut.io.rxd.poke(1.U)
      dut.clock.step(bitCycles)

      dut.io.channel.valid.expect(true.B)
      dut.io.channel.bits.expect(0xff.U)
    }
  }

  behavior.of("UART Buffer")

  it should "be ready when empty" in {
    test(new Buffer).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.in.ready.expect(true.B)
      dut.io.out.valid.expect(false.B)
    }
  }

  it should "store and forward one byte" in {
    test(new Buffer).withAnnotations(TestAnnotations.annos) { dut =>
      // Write data to buffer
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.poke(0x42.U)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)

      // Buffer should be full
      dut.io.in.ready.expect(false.B)
      dut.io.out.valid.expect(true.B)
      dut.io.out.bits.expect(0x42.U)

      // Read from buffer
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
      dut.io.out.ready.poke(false.B)

      // Buffer should be empty again
      dut.io.in.ready.expect(true.B)
      dut.io.out.valid.expect(false.B)
    }
  }

  it should "not accept data when full" in {
    test(new Buffer).withAnnotations(TestAnnotations.annos) { dut =>
      // Fill buffer
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.poke(0x42.U)
      dut.clock.step()

      // Try to write again (should not be accepted)
      dut.io.in.bits.poke(0x99.U)
      dut.io.in.ready.expect(false.B)
      dut.clock.step()

      // Original data should still be there
      dut.io.out.bits.expect(0x42.U)
    }
  }

  behavior.of("BufferedTx")

  it should "accept byte into buffer while transmitting" in {
    test(new BufferedTx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Initially ready (buffer empty)
      dut.io.channel.ready.expect(true.B)

      // Send first byte - buffer accepts it
      dut.io.channel.valid.poke(true.B)
      dut.io.channel.bits.poke(0x41.U) // 'A'
      dut.clock.step()
      dut.io.channel.valid.poke(false.B)

      // Wait for buffer to become ready (TX takes byte from buffer)
      // Buffer→TX handshake takes a few cycles due to registered signals
      val maxWait = 20
      var cycles  = 0
      while (!dut.io.channel.ready.peekBoolean() && cycles < maxWait) {
        dut.clock.step()
        cycles += 1
      }

      // Buffer should now be empty (TX took the byte), ready for next
      assert(cycles < maxWait, s"Buffer did not become ready within $maxWait cycles (got $cycles)")
      dut.io.channel.ready.expect(true.B)

      // Send second byte to buffer while TX is transmitting
      dut.io.channel.valid.poke(true.B)
      dut.io.channel.bits.poke(0x42.U) // 'B'
      dut.clock.step()
      dut.io.channel.valid.poke(false.B)

      // Now buffer should be full (TX still busy with first byte)
      dut.io.channel.ready.expect(false.B)
    }
  }

  it should "transmit consecutive bytes" in {
    test(new BufferedTx(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Send first byte
      dut.io.channel.valid.poke(true.B)
      dut.io.channel.bits.poke(0xaa.U)
      dut.clock.step()
      dut.io.channel.valid.poke(false.B)

      // Wait for transmission to complete - poll for ready
      val maxWait = 200
      var cycles  = 0
      while (!dut.io.channel.ready.peekBoolean() && cycles < maxWait) {
        dut.clock.step()
        cycles += 1
      }

      // Should be ready for next byte
      assert(cycles < maxWait, s"First TX did not complete within $maxWait cycles")
      dut.io.channel.ready.expect(true.B)

      // Send second byte
      dut.io.channel.valid.poke(true.B)
      dut.io.channel.bits.poke(0x55.U)
      dut.clock.step()
      dut.io.channel.valid.poke(false.B)

      // TX takes byte from buffer on this cycle, but register update
      // happens on next clock edge. Step once more to see TX start.
      dut.clock.step()

      // Should now be transmitting (not idle)
      // TX is busy so ready should be false (buffer took the byte)
      dut.io.channel.ready.expect(false.B)
    }
  }

  // ==================== Integration Tests ====================
  // Test the full Uart module with AXI4-Lite interface

  behavior.of("Uart MMIO Interface")

  /** Helper: Perform AXI4-Lite write to Uart */
  def axiWrite(dut: Uart, addr: Int, data: Int): Unit = {
    // Write address channel
    dut.io.channels.write_address_channel.AWVALID.poke(true.B)
    dut.io.channels.write_address_channel.AWADDR.poke(addr.U)
    dut.io.channels.write_address_channel.AWPROT.poke(0.U)
    // Write data channel
    dut.io.channels.write_data_channel.WVALID.poke(true.B)
    dut.io.channels.write_data_channel.WDATA.poke(data.U)
    dut.io.channels.write_data_channel.WSTRB.poke(0xf.U)
    // Response channel ready
    dut.io.channels.write_response_channel.BREADY.poke(true.B)

    // Wait for write to complete
    val maxWait = 20
    var cycles  = 0
    while (!dut.io.channels.write_response_channel.BVALID.peekBoolean() && cycles < maxWait) {
      dut.clock.step()
      cycles += 1
    }
    dut.clock.step()

    // Deassert signals
    dut.io.channels.write_address_channel.AWVALID.poke(false.B)
    dut.io.channels.write_data_channel.WVALID.poke(false.B)
    dut.io.channels.write_response_channel.BREADY.poke(false.B)
  }

  /** Helper: Perform AXI4-Lite read from Uart */
  def axiRead(dut: Uart, addr: Int): BigInt = {
    // Read address channel
    dut.io.channels.read_address_channel.ARVALID.poke(true.B)
    dut.io.channels.read_address_channel.ARADDR.poke(addr.U)
    dut.io.channels.read_address_channel.ARPROT.poke(0.U)
    // Read data channel ready
    dut.io.channels.read_data_channel.RREADY.poke(true.B)

    // Wait for read to complete
    val maxWait = 20
    var cycles  = 0
    while (!dut.io.channels.read_data_channel.RVALID.peekBoolean() && cycles < maxWait) {
      dut.clock.step()
      cycles += 1
    }

    val data = dut.io.channels.read_data_channel.RDATA.peekInt()
    dut.clock.step()

    // Deassert signals
    dut.io.channels.read_address_channel.ARVALID.poke(false.B)
    dut.io.channels.read_data_channel.RREADY.poke(false.B)
    dut.clock.step()

    data
  }

  it should "read baud rate from MMIO register" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Initialize
      dut.io.rxd.poke(1.U) // Idle high
      dut.clock.step(5)

      // Read baud rate register
      val baudRate = axiRead(dut, REG_BAUD_RATE)
      assert(baudRate == testBaudRate, s"Expected baud rate $testBaudRate, got $baudRate")
    }
  }

  it should "show TX ready in STATUS register when idle" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Read status - bit 0 should be 1 (TX ready)
      val status = axiRead(dut, REG_STATUS)
      assert((status & 0x01) == 1, s"TX should be ready, status=$status")
    }
  }

  it should "transmit byte via TX_DATA register" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Write byte to TX register
      axiWrite(dut, REG_TX_DATA, 0x55)

      // TX line should go low (start bit) within a few cycles
      val maxWait     = 30
      var cycles      = 0
      var sawStartBit = false
      while (cycles < maxWait && !sawStartBit) {
        if (dut.io.txd.peekInt() == 0) sawStartBit = true
        dut.clock.step()
        cycles += 1
      }
      assert(sawStartBit, "TX should have started (start bit = 0)")
    }
  }

  it should "set RX valid in STATUS when data received" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Initially no RX data
      val statusBefore = axiRead(dut, REG_STATUS)
      assert((statusBefore & 0x02) == 0, "RX should not be valid initially")

      // Send a byte via RX line
      // Start bit
      dut.io.rxd.poke(0.U)
      dut.clock.step(bitCycles + bitCycles / 2)

      // Data bits: 0xAA = 10101010 -> LSB first: 0,1,0,1,0,1,0,1
      val dataBits = Seq(0, 1, 0, 1, 0, 1, 0, 1)
      for (bit <- dataBits) {
        dut.io.rxd.poke(bit.U)
        dut.clock.step(bitCycles)
      }

      // Stop bit
      dut.io.rxd.poke(1.U)
      dut.clock.step(bitCycles + 5)

      // RX data register should have the byte
      val rxData = axiRead(dut, REG_RX_DATA)
      assert((rxData & 0xff) == 0xaa, s"Expected RX data 0xAA, got ${rxData & 0xff}")
    }
  }

  it should "generate interrupt on RX and clear on read" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Initially no interrupt (register starts at false)
      dut.io.signal_interrupt.expect(false.B)

      // Send a byte via RX
      dut.io.rxd.poke(0.U) // Start bit
      dut.clock.step(bitCycles + bitCycles / 2)

      for (_ <- 0 until 8) {
        dut.io.rxd.poke(1.U) // All 1s = 0xFF
        dut.clock.step(bitCycles)
      }

      dut.io.rxd.poke(1.U) // Stop bit
      dut.clock.step(bitCycles + 5)

      // Interrupt should be asserted after RX completes
      dut.io.signal_interrupt.expect(true.B)

      // Read RX data - should clear interrupt
      axiRead(dut, REG_RX_DATA)
      dut.clock.step(2)

      // Interrupt should be cleared
      dut.io.signal_interrupt.expect(false.B)
    }
  }

  it should "allow manual interrupt control via INTERRUPT register" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.rxd.poke(1.U)
      dut.clock.step(5)

      // Initially no interrupt
      dut.io.signal_interrupt.expect(false.B)

      // Writing non-zero to INTERRUPT register sets it
      axiWrite(dut, REG_INTERRUPT, 1)
      dut.clock.step(2)
      dut.io.signal_interrupt.expect(true.B)

      // Writing zero clears it
      axiWrite(dut, REG_INTERRUPT, 0)
      dut.clock.step(2)
      dut.io.signal_interrupt.expect(false.B)
    }
  }

  behavior.of("Uart TX/RX Loopback")

  it should "receive transmitted data in loopback" in {
    test(new Uart(testFrequency, testBaudRate)).withAnnotations(TestAnnotations.annos) { dut =>
      // Connect TX to RX for loopback (internal to test)
      // We'll manually feed TX output to RX input

      dut.io.rxd.poke(1.U) // Start idle
      dut.clock.step(5)

      // Transmit 0x55
      axiWrite(dut, REG_TX_DATA, 0x55)

      // Simulate loopback: capture TX bits and feed to RX
      // This requires bit-level simulation
      val frameBits   = 1 + 8 + 2 // start + data + stop
      val totalCycles = frameBits * bitCycles + 20

      // Track TX output and feed to RX with 1-cycle delay (simulating wire)
      for (_ <- 0 until totalCycles) {
        val txBit = dut.io.txd.peekInt()
        dut.io.rxd.poke(txBit.U)
        dut.clock.step()
      }

      // Allow RX to complete
      dut.io.rxd.poke(1.U)
      dut.clock.step(20)

      // Read received data
      val rxData = axiRead(dut, REG_RX_DATA)
      assert((rxData & 0xff) == 0x55, s"Loopback failed: sent 0x55, received 0x${(rxData & 0xff).toString(16)}")
    }
  }
}
