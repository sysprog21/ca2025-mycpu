// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package board.verilator

import chisel3._
import chisel3.stage.ChiselStage
import peripheral._
import riscv.core.CPU
import riscv.CPUBundle
import riscv.Parameters

class TopBundle extends CPUBundle {
  // VGA peripheral outputs
  val vga_pixclk      = Input(Clock())     // VGA pixel clock (31.5 MHz)
  val vga_hsync       = Output(Bool())     // Horizontal sync
  val vga_vsync       = Output(Bool())     // Vertical sync
  val vga_rrggbb      = Output(UInt(6.W))  // 6-bit color output
  val vga_activevideo = Output(Bool())     // Active display region
  val vga_x_pos       = Output(UInt(10.W)) // Current pixel X position
  val vga_y_pos       = Output(UInt(10.W)) // Current pixel Y position
}

class Top extends Module {
  val io = IO(new TopBundle)

  val cpu = Module(new CPU)
  val vga = Module(new VGA)

  // Debug interface
  cpu.io.regs_debug_read_address     := io.regs_debug_read_address
  cpu.io.csr_regs_debug_read_address := io.csr_regs_debug_read_address
  io.csr_regs_debug_read_data        := cpu.io.csr_regs_debug_read_data
  io.regs_debug_read_data            := cpu.io.regs_debug_read_data

  // Export deviceSelect for external MMIO routing
  io.deviceSelect := cpu.io.deviceSelect

  // Instruction interface
  io.instruction_address   := cpu.io.instruction_address
  cpu.io.instruction       := io.instruction
  cpu.io.instruction_valid := io.instruction_valid
  cpu.io.interrupt_flag    := io.interrupt_flag

  // Memory/MMIO routing: deviceSelect determines routing
  // deviceSelect=1: VGA (0x20000000-0x2FFFFFFF)
  // Other values: External peripherals (Timer/UART/Memory - handled by testbench)
  io.memory_bundle.address      := cpu.io.memory_bundle.address
  io.memory_bundle.write_enable := cpu.io.memory_bundle.write_enable
  io.memory_bundle.write_data   := cpu.io.memory_bundle.write_data
  io.memory_bundle.write_strobe := cpu.io.memory_bundle.write_strobe

  // Mux read data based on deviceSelect
  cpu.io.memory_bundle.read_data := Mux(
    cpu.io.deviceSelect === 1.U,
    vga.io.bundle.read_data,
    io.memory_bundle.read_data
  )

  // VGA peripheral connections
  vga.io.pixClock            := io.vga_pixclk
  io.vga_hsync               := vga.io.hsync
  io.vga_vsync               := vga.io.vsync
  io.vga_rrggbb              := vga.io.rrggbb
  io.vga_activevideo         := vga.io.activevideo
  io.vga_x_pos               := vga.io.x_pos
  io.vga_y_pos               := vga.io.y_pos

  // VGA MMIO routing
  vga.io.bundle.address      := cpu.io.memory_bundle.address
  vga.io.bundle.write_data   := cpu.io.memory_bundle.write_data
  vga.io.bundle.write_strobe := cpu.io.memory_bundle.write_strobe
  vga.io.bundle.write_enable := Mux(
    cpu.io.deviceSelect === 1.U,
    cpu.io.memory_bundle.write_enable,
    false.B
  )
}

object VerilogGenerator extends App {
  (new ChiselStage).emitVerilog(
    new Top(),
    Array("--target-dir", "2-mmio-trap/verilog/verilator")
  )
}
