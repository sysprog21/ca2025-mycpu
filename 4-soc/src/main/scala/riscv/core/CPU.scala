// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import bus.AXI4LiteMaster
import chisel3._
import riscv.ImplementationType
import riscv.Parameters
// PipelinedCPU is now in the same package (riscv.core)

class CPU(val implementation: Int = ImplementationType.FiveStageFinal) extends Module {
  val io = IO(new CPUBundle)

  implementation match {
    case ImplementationType.FiveStageFinal =>
      val cpu = Module(new PipelinedCPU)

      // Connect instruction fetch interface
      io.instruction_address   := cpu.io.instruction_address
      cpu.io.instruction       := io.instruction
      cpu.io.instruction_valid := io.instruction_valid

      // Connect memory/bus interface through AXI4-Lite master
      val axi_master = Module(new AXI4LiteMaster(Parameters.AddrBits, Parameters.DataBits))

      // Reconstruct full address from PipelinedCPU outputs
      val full_bus_address = cpu.io.device_select ## cpu.io.memory_bundle
        .address(Parameters.AddrBits - Parameters.SlaveDeviceCountBits - 1, 0)

      // BusBundle to AXI4LiteMasterBundle adapter
      axi_master.io.bundle.address      := full_bus_address
      axi_master.io.bundle.read         := cpu.io.memory_bundle.request && cpu.io.memory_bundle.read
      axi_master.io.bundle.write        := cpu.io.memory_bundle.request && cpu.io.memory_bundle.write
      axi_master.io.bundle.write_data   := cpu.io.memory_bundle.write_data
      axi_master.io.bundle.write_strobe := cpu.io.memory_bundle.write_strobe

      cpu.io.memory_bundle.read_data           := axi_master.io.bundle.read_data
      cpu.io.memory_bundle.read_valid          := axi_master.io.bundle.read_valid
      cpu.io.memory_bundle.write_valid         := axi_master.io.bundle.write_valid
      cpu.io.memory_bundle.write_data_accepted := axi_master.io.bundle.write_data_accepted
      cpu.io.memory_bundle.busy                := axi_master.io.bundle.busy
      cpu.io.memory_bundle.granted             := !axi_master.io.bundle.busy // Granted when not busy

      // Connect AXI4-Lite channels to top-level
      io.axi4_channels <> axi_master.io.channels

      // Initialize cpu's unused axi4_channels inputs (FiveStageCPUFinal doesn't use these)
      cpu.io.axi4_channels.read_address_channel.ARREADY  := false.B
      cpu.io.axi4_channels.read_data_channel.RVALID      := false.B
      cpu.io.axi4_channels.read_data_channel.RDATA       := 0.U
      cpu.io.axi4_channels.read_data_channel.RRESP       := 0.U
      cpu.io.axi4_channels.write_address_channel.AWREADY := false.B
      cpu.io.axi4_channels.write_data_channel.WREADY     := false.B
      cpu.io.axi4_channels.write_response_channel.BVALID := false.B
      cpu.io.axi4_channels.write_response_channel.BRESP  := 0.U

      // Connect device select and bus address from wrapper
      io.device_select := cpu.io.device_select

      // Latch bus address for the duration of each AXI transaction.
      // AXI4LiteMaster captures cpu.io.memory_bundle.address in the Idle state and
      // asserts ARVALID/AWVALID on the next cycle. If the CPU pipeline advances in
      // between, the combinational cpu.io.memory_bundle.address/device_select would
      // change and the BusSwitch could route the master's ARVALID/AWVALID to the
      // wrong slave. By registering bus_address when a new request starts and
      // holding it while the master is busy, we ensure stable routing.
      val bus_address_reg  = RegInit(0.U(Parameters.AddrWidth))
      val next_bus_address = full_bus_address

      // New transaction starts when master is idle (not busy) and CPU issues a
      // read or write request on the BusBundle.
      val start_bus_transaction =
        !axi_master.io.bundle.busy &&
          cpu.io.memory_bundle.request &&
          (cpu.io.memory_bundle.read || cpu.io.memory_bundle.write)

      when(start_bus_transaction) {
        bus_address_reg := next_bus_address
      }

      io.bus_address := bus_address_reg

      // Connect wrapper memory_bundle outputs (pass through from CPU)
      io.memory_bundle.address      := cpu.io.memory_bundle.address
      io.memory_bundle.read         := cpu.io.memory_bundle.read
      io.memory_bundle.write        := cpu.io.memory_bundle.write
      io.memory_bundle.write_data   := cpu.io.memory_bundle.write_data
      io.memory_bundle.write_strobe := cpu.io.memory_bundle.write_strobe
      io.memory_bundle.request      := cpu.io.memory_bundle.request

      // Note: io.memory_bundle inputs (read_data, read_valid, write_valid, busy, granted)
      // are not connected at Top level - they're for debugging/bypass only
      // The actual memory interface goes through axi4_channels

      // Connect interrupt
      cpu.io.interrupt_flag := io.interrupt_flag

      // Connect debug interfaces
      cpu.io.debug_read_address := io.debug_read_address
      io.debug_read_data        := cpu.io.debug_read_data

      cpu.io.csr_debug_read_address := io.csr_debug_read_address
      io.csr_debug_read_data        := cpu.io.csr_debug_read_data

      // Connect debug bus signals
      io.debug_bus_write_enable := cpu.io.memory_bundle.write
      io.debug_bus_write_data   := cpu.io.memory_bundle.write_data
  }
}
