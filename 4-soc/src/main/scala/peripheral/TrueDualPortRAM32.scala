// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import chisel3.experimental._
import chisel3.util._

/**
 * True dual-port, dual-clock RAM with 32-bit data width
 *
 * Port A: Write port (CPU clock domain)
 * Port B: Read port (Pixel clock domain)
 *
 * For synthesis: vendor-specific implementation
 * For Verilator: behavioral model with separate clocks
 *
 * Parameters:
 * - depth: Number of 32-bit words (e.g., 6144 for 12 frames of 64×64 pixels, 8 pixels per word)
 * - addrWidth: Address width in bits
 */
class TrueDualPortRAM32(depth: Int, addrWidth: Int)
    extends BlackBox(
      Map(
        "DEPTH"      -> depth,
        "ADDR_WIDTH" -> addrWidth
      )
    )
    with HasBlackBoxResource {

  val io = IO(new Bundle {
    // Port A: Write port (CPU clock domain)
    val clka  = Input(Clock())
    val wea   = Input(Bool())
    val addra = Input(UInt(addrWidth.W))
    val dina  = Input(UInt(32.W))

    // Port B: Read port (Pixel clock domain)
    val clkb  = Input(Clock())
    val addrb = Input(UInt(addrWidth.W))
    val doutb = Output(UInt(32.W))
  })

  // Add Verilog resource for behavioral model
  addResource("/vsrc/TrueDualPortRAM32.v")
}
