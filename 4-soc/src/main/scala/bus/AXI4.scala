// SPDX-License-Identifier: MIT

package bus

import chisel3._
import chisel3.util._

// Configuration parameters for AXI4 Bus
case class AXI4Config(
  addrWidth: Int = 32,
  dataWidth: Int = 32,
  idWidth:   Int = 4,
  userWidth: Int = 0,
  lenWidth:  Int = 8
)

object AXI4Parameters {
  // Burst types
  val BURST_FIXED = 0.U(2.W)
  val BURST_INCR  = 1.U(2.W)
  val BURST_WRAP  = 2.U(2.W)

  // Response types
  val RESP_OKAY   = 0.U(2.W)
  val RESP_EXOKAY = 1.U(2.W)
  val RESP_SLVERR = 2.U(2.W)
  val RESP_DECERR = 3.U(2.W)
}

// Full AXI4 Interface Bundle
class AXI4Bundle(val config: AXI4Config) extends Bundle {
  // Write Address Channel (AW)
  val aw = new Bundle {
    val id    = Output(UInt(config.idWidth.W))
    val addr  = Output(UInt(config.addrWidth.W))
    val len   = Output(UInt(config.lenWidth.W))
    val size  = Output(UInt(3.W))
    val burst = Output(UInt(2.W))
    val lock  = Output(Bool())
    val cache = Output(UInt(4.W))
    val prot  = Output(UInt(3.W))
    val qos   = Output(UInt(4.W))
    val valid = Output(Bool())
    val ready = Input(Bool())
  }

  // Write Data Channel (W)
  val w = new Bundle {
    val data  = Output(UInt(config.dataWidth.W))
    val strb  = Output(UInt((config.dataWidth / 8).W))
    val last  = Output(Bool())
    val valid = Output(Bool())
    val ready = Input(Bool())
  }

  // Write Response Channel (B)
  val b = new Bundle {
    val id    = Input(UInt(config.idWidth.W))
    val resp  = Input(UInt(2.W))
    val valid = Input(Bool())
    val ready = Output(Bool())
  }

  // Read Address Channel (AR)
  val ar = new Bundle {
    val id    = Output(UInt(config.idWidth.W))
    val addr  = Output(UInt(config.addrWidth.W))
    val len   = Output(UInt(config.lenWidth.W))
    val size  = Output(UInt(3.W))
    val burst = Output(UInt(2.W))
    val lock  = Output(Bool())
    val cache = Output(UInt(4.W))
    val prot  = Output(UInt(3.W))
    val qos   = Output(UInt(4.W))
    val valid = Output(Bool())
    val ready = Input(Bool())
  }

  // Read Data Channel (R)
  val r = new Bundle {
    val id    = Input(UInt(config.idWidth.W))
    val data  = Input(UInt(config.dataWidth.W))
    val resp  = Input(UInt(2.W))
    val last  = Input(Bool())
    val valid = Input(Bool())
    val ready = Output(Bool())
  }
}