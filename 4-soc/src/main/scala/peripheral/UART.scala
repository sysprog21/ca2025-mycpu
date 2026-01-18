// SPDX-License-Identifier: MIT
package peripheral

import chisel3._
import chisel3.util._
import bus._
import riscv.Parameters

class Uart(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val channels = Flipped(new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
    val txd = Output(UInt(1.W))
    val rxd = Input(UInt(1.W))
    val signal_interrupt = Output(Bool())
  })

  val tx_reg = RegInit(0.U(8.W))
  val rx_reg = RegInit(0.U(8.W))
  val status_reg = RegInit(0.U(3.W))

  val cyclesPerBit = (frequency + baudRate / 2) / baudRate
  val timer_tx = RegInit(0.U(32.W))
  val bit_index_tx = RegInit(0.U(4.W))
  val tx_state = RegInit(0.U(2.W))
  val tx_data_shifter = RegInit(0.U(8.W))

  val slave = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))
  slave.io.channels <> io.channels

  // [FIX] Address Decoding: Mask upper bits to get local offset
  val offset = slave.io.bundle.address(3, 0)

  // Write Logic
  when(slave.io.bundle.write) {
    // Write to TX Buffer (Offset 0x00)
    when(offset === 0.U) {
      tx_reg := slave.io.bundle.write_data(7, 0)
      when(tx_state === 0.U) {
        tx_state := 1.U
        timer_tx := 0.U
        tx_data_shifter := slave.io.bundle.write_data(7, 0)
      }
    }
  }

  // Read Logic
  slave.io.bundle.read_valid := false.B
  slave.io.bundle.read_data := 0.U
  when(slave.io.bundle.read) {
    slave.io.bundle.read_valid := true.B
    switch(offset) {
      is(0.U) { slave.io.bundle.read_data := rx_reg }
      is(4.U) { slave.io.bundle.read_data := status_reg }
    }
  }

  // TX State Machine
  io.txd := 1.U
  status_reg := Cat(0.U(1.W), 0.U(1.W), (tx_state =/= 0.U))

  switch(tx_state) {
    is(0.U) { // Idle
      io.txd := 1.U
      when(slave.io.bundle.write && offset === 0.U) {
        tx_state := 1.U
        timer_tx := 0.U
      }
    }
    is(1.U) { // Start
      io.txd := 0.U
      when(timer_tx === cyclesPerBit.U) {
        timer_tx := 0.U
        tx_state := 2.U
        bit_index_tx := 0.U
      } .otherwise { timer_tx := timer_tx + 1.U }
    }
    is(2.U) { // Data
      io.txd := tx_data_shifter(0)
      when(timer_tx === cyclesPerBit.U) {
        timer_tx := 0.U
        tx_data_shifter := tx_data_shifter >> 1
        when(bit_index_tx === 7.U) { tx_state := 3.U }
        .otherwise { bit_index_tx := bit_index_tx + 1.U }
      } .otherwise { timer_tx := timer_tx + 1.U }
    }
    is(3.U) { // Stop
      io.txd := 1.U
      when(timer_tx === cyclesPerBit.U) { tx_state := 0.U }
      .otherwise { timer_tx := timer_tx + 1.U }
    }
  }
  io.signal_interrupt := false.B
}