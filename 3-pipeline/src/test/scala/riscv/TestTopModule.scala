// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chisel3.util.Cat
import peripheral.InstructionROM
import peripheral.Memory
import peripheral.ROMLoader
import riscv.core.CPU

class TestTopModule(exeFilename: String, implementation: Int) extends Module {
  val io = IO(new Bundle {
    val regs_debug_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val mem_debug_read_address  = Input(UInt(Parameters.AddrWidth))
    val regs_debug_read_data    = Output(UInt(Parameters.DataWidth))
    val mem_debug_read_data     = Output(UInt(Parameters.DataWidth))
    val csr_debug_read_address  = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val csr_debug_read_data     = Output(UInt(Parameters.DataWidth))
    val interrupt_flag          = Input(UInt(Parameters.InterruptFlagWidth))
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

    // Detect MMIO address ranges (Timer: 0x8xxxxxxx, UART: 0x4xxxxxxx)
    val addrHighNibble = fullAddress(31, 28)
    val isTimer        = addrHighNibble === "h8".U
    val isUart         = addrHighNibble === "h4".U
    val isMMIO         = isTimer || isUart

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
      cpuMemReadData                 := mem.io.bundle.read_data
    }

    cpu.io.debug_read_address     := io.regs_debug_read_address
    io.regs_debug_read_data       := cpu.io.debug_read_data
    cpu.io.csr_debug_read_address := io.csr_debug_read_address
    io.csr_debug_read_data        := cpu.io.csr_debug_read_data
  }

  mem.io.debug_read_address := io.mem_debug_read_address
  io.mem_debug_read_data    := mem.io.debug_read_data
}
