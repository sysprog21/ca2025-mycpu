// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

import bus.AXI4LiteMaster
import bus.AXI4LiteSlave
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.Parameters

class AXI4LiteTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("AXI4LiteMaster and AXI4LiteSlave")

  it should "perform a simple read transaction" in {
    test(new Module {
      val io = IO(new Bundle {
        val done = Output(Bool())
      })

      val master = Module(new AXI4LiteMaster(Parameters.AddrBits, Parameters.DataBits))
      val slave  = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))

      master.io.channels <> slave.io.channels

      // Simple test: master reads from address 0x1000
      master.io.bundle.address      := 0x1000.U
      master.io.bundle.read         := true.B
      master.io.bundle.write        := false.B
      master.io.bundle.write_data   := 0.U
      master.io.bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))

      // Slave responds with test data
      slave.io.bundle.read_data  := "hDEADBEEF".U
      slave.io.bundle.read_valid := slave.io.bundle.read

      io.done := master.io.bundle.read_valid
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(100)

      // Wait for transaction to complete
      var cycles = 0
      while (!dut.io.done.peek().litToBoolean && cycles < 50) {
        dut.clock.step()
        cycles += 1
      }

      assert(dut.io.done.peek().litToBoolean, s"Read transaction did not complete in $cycles cycles")
    }
  }

  it should "perform a simple write transaction" in {
    test(new Module {
      val io = IO(new Bundle {
        val done = Output(Bool())
      })

      val master = Module(new AXI4LiteMaster(Parameters.AddrBits, Parameters.DataBits))
      val slave  = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))

      master.io.channels <> slave.io.channels

      // Master writes 0xCAFEBABE to address 0x2000
      master.io.bundle.address      := 0x2000.U
      master.io.bundle.read         := false.B
      master.io.bundle.write        := true.B
      master.io.bundle.write_data   := "hCAFEBABE".U
      master.io.bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(true.B))

      // Slave must drive read signals even for write test
      slave.io.bundle.read_data  := 0.U
      slave.io.bundle.read_valid := false.B

      io.done := master.io.bundle.write_valid
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(100)

      // Wait for transaction to complete
      var cycles = 0
      while (!dut.io.done.peek().litToBoolean && cycles < 50) {
        dut.clock.step()
        cycles += 1
      }

      assert(dut.io.done.peek().litToBoolean, s"Write transaction did not complete in $cycles cycles")
    }
  }
}
