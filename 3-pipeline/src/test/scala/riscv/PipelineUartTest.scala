// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import peripheral.InstructionROM
import peripheral.Memory
import peripheral.ROMLoader
import riscv.core.CPU

class UartHarness(exeFilename: String, implementation: Int) extends Module {
  val io = IO(new Bundle {
    val mem_debug_read_address  = Input(UInt(Parameters.AddrWidth))
    val regs_debug_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val csr_debug_read_address  = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val interrupt_flag          = Input(UInt(Parameters.InterruptFlagWidth))

    val regs_debug_read_data = Output(UInt(Parameters.DataWidth))
    val mem_debug_read_data  = Output(UInt(Parameters.DataWidth))
    val csr_debug_read_data  = Output(UInt(Parameters.DataWidth))
    val pc_debug_read        = Output(UInt(Parameters.AddrWidth))
    val uart_tx_count        = Output(UInt(8.W))
    val uart_tx_last         = Output(UInt(8.W))
  })

  val mem             = Module(new Memory(8192))
  val instruction_rom = Module(new InstructionROM(exeFilename))
  val rom_loader      = Module(new ROMLoader(instruction_rom.capacity))

  rom_loader.io.rom_data     := instruction_rom.io.data
  rom_loader.io.load_address := Parameters.EntryAddress
  instruction_rom.io.address := rom_loader.io.rom_address

  val CPU_clkdiv = RegInit(UInt(2.W), 0.U)
  val CPU_tick   = Wire(Bool())
  val CPU_next   = Wire(UInt(2.W))
  CPU_next   := Mux(CPU_clkdiv === 3.U, 0.U, CPU_clkdiv + 1.U)
  CPU_tick   := CPU_clkdiv === 0.U
  CPU_clkdiv := CPU_next

  withClock(CPU_tick.asClock) {
    val cpu = Module(new CPU(implementation))
    cpu.io.debug_read_address     := 0.U
    cpu.io.csr_debug_read_address := 0.U
    cpu.io.instruction_valid      := rom_loader.io.load_finished
    mem.io.instruction_address    := cpu.io.instruction_address
    cpu.io.instruction            := mem.io.instruction
    cpu.io.interrupt_flag         := io.interrupt_flag

    when(!rom_loader.io.load_finished) {
      rom_loader.io.bundle <> mem.io.bundle
      cpu.io.memory_bundle.read_data := 0.U
    }.otherwise {
      rom_loader.io.bundle.read_data := 0.U
      cpu.io.memory_bundle <> mem.io.bundle
    }

    val cpuMemAddress     = cpu.io.memory_bundle.address
    val cpuMemWriteData   = cpu.io.memory_bundle.write_data
    val cpuMemWriteEnable = cpu.io.memory_bundle.write_enable
    val cpuMemWriteStrobe = cpu.io.memory_bundle.write_strobe
    val cpuMemReadData    = Wire(UInt(Parameters.DataWidth))
    cpu.io.memory_bundle.read_data := cpuMemReadData

    val memAddress     = Wire(UInt(Parameters.AddrWidth))
    val memWriteData   = Wire(UInt(Parameters.DataWidth))
    val memWriteEnable = Wire(Bool())
    val memWriteStrobe = Wire(Vec(Parameters.WordSize, Bool()))

    mem.io.bundle.address      := memAddress
    mem.io.bundle.write_data   := memWriteData
    mem.io.bundle.write_enable := memWriteEnable
    mem.io.bundle.write_strobe := memWriteStrobe

    val fullAddress = Cat(
      cpu.io.device_select,
      cpuMemAddress(Parameters.AddrBits - Parameters.SlaveDeviceCountBits - 1, 0)
    )

    val uartBaud     = RegInit(115200.U(32.W))
    val uartEnabled  = RegInit(false.B)
    val uartLastByte = RegInit(0.U(8.W))
    val uartCounter  = RegInit(0.U(8.W))

    val timerLimit  = RegInit(0.U(32.W))
    val timerEnable = RegInit(false.B)

    val addrHighNibble = fullAddress(31, 28)
    val isUart         = addrHighNibble === "h4".U
    val isTimer        = addrHighNibble === "h8".U
    val isMMIO         = isUart || isTimer
    val mmioOffset     = fullAddress(7, 0)

    when(cpuMemWriteEnable && isUart) {
      when(mmioOffset === 0x04.U) {
        uartBaud := cpuMemWriteData
      }.elsewhen(mmioOffset === 0x08.U) {
        uartEnabled := cpuMemWriteData.orR
      }.elsewhen(mmioOffset === 0x10.U) {
        when(uartEnabled) {
          uartLastByte := cpuMemWriteData(7, 0)
          uartCounter  := uartCounter + 1.U
        }
      }
    }.elsewhen(cpuMemWriteEnable && isTimer) {
      when(mmioOffset === 0x04.U) {
        timerLimit := cpuMemWriteData
      }.elsewhen(mmioOffset === 0x08.U) {
        timerEnable := cpuMemWriteData =/= 0.U
      }
    }

    val uartReadData = MuxCase(
      0.U(32.W),
      Seq(
        (mmioOffset === 0x04.U) -> uartBaud,
        (mmioOffset === 0x0c.U) -> Cat(0.U(24.W), uartLastByte)
      )
    )
    val timerReadData = MuxCase(
      0.U(32.W),
      Seq(
        (mmioOffset === 0x04.U) -> timerLimit,
        (mmioOffset === 0x08.U) -> timerEnable.asUInt
      )
    )
    val mmioReadData = Mux(isUart, uartReadData, Mux(isTimer, timerReadData, 0.U))

    when(!rom_loader.io.load_finished) {
      memAddress                     := rom_loader.io.bundle.address
      memWriteData                   := rom_loader.io.bundle.write_data
      memWriteEnable                 := rom_loader.io.bundle.write_enable
      memWriteStrobe                 := rom_loader.io.bundle.write_strobe
      rom_loader.io.bundle.read_data := mem.io.bundle.read_data
      cpuMemReadData                 := 0.U
    }.otherwise {
      rom_loader.io.bundle.read_data := 0.U
      memAddress                     := fullAddress
      memWriteData                   := cpuMemWriteData
      memWriteEnable                 := cpuMemWriteEnable && !isMMIO
      memWriteStrobe                 := cpuMemWriteStrobe
      cpuMemReadData                 := Mux(isMMIO, mmioReadData, mem.io.bundle.read_data)
    }

    cpu.io.debug_read_address     := io.regs_debug_read_address
    cpu.io.csr_debug_read_address := io.csr_debug_read_address
    io.regs_debug_read_data       := cpu.io.debug_read_data
    io.csr_debug_read_data        := cpu.io.csr_debug_read_data
    io.pc_debug_read              := cpu.io.instruction_address
    io.uart_tx_count              := uartCounter
    io.uart_tx_last               := uartLastByte
  }

  mem.io.debug_read_address := io.mem_debug_read_address
  io.mem_debug_read_data    := mem.io.debug_read_data
}

class PipelineUartTest extends AnyFlatSpec with ChiselScalatestTester {
  private val DoneValue = BigInt("CAFEF00D", 16)

  for (cfg <- PipelineConfigs.All) {
    behavior.of(s"${cfg.name} UART Comprehensive Test")
    it should "pass all TX and RX tests" in {
      test(new UartHarness("uart.asmbin", cfg.implementation)).withAnnotations(TestAnnotations.annos) { c =>
        c.io.interrupt_flag.poke(0.U)
        c.io.regs_debug_read_address.poke(0.U)
        c.io.csr_debug_read_address.poke(0.U)
        c.clock.setTimeout(0)

        // Run comprehensive test (TX + 3 RX tests)
        for (_ <- 0 until 30000) {
          c.clock.step()
        }

        // Check completion
        c.io.mem_debug_read_address.poke(0x100.U)
        c.clock.step()
        c.io.mem_debug_read_data.expect(0xcafef00dL.U) // DONE_FLAG

        // Check all tests passed: TX + Multi-byte RX + Binary RX + Timeout RX
        c.io.mem_debug_read_address.poke(0x104.U)
        c.clock.step()
        c.io.mem_debug_read_data.expect(0xf.U) // 0b1111 = all 4 tests passed
      }
    }
  }
}
