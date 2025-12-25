// Test to verify AXI4-Lite write path with clock domain crossing
package riscv

import bus.AXI4LiteMaster
import bus.AXI4LiteSlave
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import peripheral.Memory

class AXI4WritePathTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("AXI4-Lite Write Path with Clock Domain Crossing")

  it should "write data through AXI4-Lite to Memory with 4:1 clock ratio" in {
    test(new Module {
      val io = IO(new Bundle {
        val write_done  = Output(Bool())
        val read_data   = Output(UInt(32.W))
        val debug_write = Output(Bool())
        val debug_addr  = Output(UInt(32.W))
      })

      // Memory on main clock (outside withClock)
      val mem = Module(new Memory(256))
      mem.io.instruction_address := 0.U
      mem.io.debug_read_address  := 0x100.U // Read address 0x100

      // Clock divider
      val CPU_clkdiv = RegInit(0.U(2.W))
      val CPU_tick   = CPU_clkdiv === 0.U
      CPU_clkdiv := Mux(CPU_clkdiv === 3.U, 0.U, CPU_clkdiv + 1.U)

      // Track write completion
      val write_complete = RegInit(false.B)
      io.write_done := write_complete
      io.read_data  := mem.io.debug_read_data

      withClock(CPU_tick.asClock) {
        val master = Module(new AXI4LiteMaster(Parameters.AddrBits, Parameters.DataBits))
        val slave  = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))

        master.io.channels <> slave.io.channels

        // Write 0xDEADBEEF to address 0x100
        val started = RegInit(false.B)
        master.io.bundle.address      := 0x100.U
        master.io.bundle.write        := !started && !write_complete
        master.io.bundle.read         := false.B
        master.io.bundle.write_data   := "hDEADBEEF".U
        master.io.bundle.write_strobe := VecInit(Seq.fill(4)(true.B))

        when(master.io.bundle.write_valid) {
          write_complete := true.B
        }
        when(!started && !master.io.bundle.busy) {
          started := true.B
        }

        // Connect slave to memory
        mem.io.bundle.address      := slave.io.bundle.address
        mem.io.bundle.write_data   := slave.io.bundle.write_data
        mem.io.bundle.write_enable := slave.io.bundle.write
        mem.io.bundle.write_strobe := slave.io.bundle.write_strobe

        // Debug outputs
        io.debug_write := slave.io.bundle.write
        io.debug_addr  := slave.io.bundle.address

        // Slave read response (not used for write test)
        slave.io.bundle.read_data  := mem.io.bundle.read_data
        slave.io.bundle.read_valid := false.B
      }
    }).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(200)

      // Wait for write to complete
      var cycles = 0
      while (!dut.io.write_done.peek().litToBoolean && cycles < 100) {
        dut.clock.step()
        cycles += 1
      }

      // Read back data
      dut.clock.step(10) // Extra cycles to ensure write is committed
      val readData = dut.io.read_data.peekInt()

      assert(cycles < 100, s"Write did not complete within 100 cycles")
      assert(readData == 0xdeadbeefL, f"Expected 0xDEADBEEF, got 0x$readData%08X")
    }
  }
}
