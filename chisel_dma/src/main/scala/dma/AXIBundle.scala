package dma

import chisel3._
import chisel3.util._

// AXI4-Lite Bundle Definition (Skeleton)
class AXILiteSlave(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val aw = Flipped(Decoupled(UInt(addrWidth.W)))
  val w  = Flipped(Decoupled(UInt(dataWidth.W)))
  val b  = Decoupled(UInt(2.W)) // Write Response
  val ar = Flipped(Decoupled(UInt(addrWidth.W)))
  val r  = Decoupled(UInt(dataWidth.W))
}

// Main Module Placeholder
class DmaTop extends Module {
  val io = IO(new Bundle {
    val ctrl = new AXILiteSlave(32, 32)
    // TODO: Add AXI4 Master interface
  })

  // Basic connection to suppress "not driven" errors
  io.ctrl.aw.ready := false.B
  io.ctrl.w.ready  := false.B
  io.ctrl.b.valid  := false.B
  io.ctrl.b.bits   := 0.U
  io.ctrl.ar.ready := false.B
  io.ctrl.r.valid  := false.B
  io.ctrl.r.bits   := 0.U
}