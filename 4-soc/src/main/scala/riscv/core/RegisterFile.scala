// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

object Registers extends Enumeration {
  type Register = Value
  val zero, ra, sp, gp, tp, t0, t1, t2, fp, s1, a0, a1, a2, a3, a4, a5, a6, a7, s2, s3, s4, s5, s6, s7, s8, s9, s10,
      s11, t3, t4, t5, t6 = Value
}

// Register File: 32 general-purpose registers with pipeline write forwarding
// x0 is architecturally constant zero per RISC-V spec
// Only 31 physical registers allocated (x1-x31), saving 3% resources
// Write forwarding allows reading currently-being-written value (pipeline optimization)
class RegisterFile extends Module {
  val io = IO(new Bundle {
    val write_enable  = Input(Bool())
    val write_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val write_data    = Input(UInt(Parameters.DataWidth))

    val read_address1 = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val read_address2 = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val read_data1    = Output(UInt(Parameters.DataWidth))
    val read_data2    = Output(UInt(Parameters.DataWidth))

    val debug_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val debug_read_data    = Output(UInt(Parameters.DataWidth))
  })
  // Allocate only 31 physical registers (x1-x31), x0 is constant zero
  // This saves 3% of register file resources (992 vs 1024 flip-flops)
  val registers = Reg(Vec(Parameters.PhysicalRegisters - 1, UInt(Parameters.DataWidth)))

  when(!reset.asBool) {
    when(io.write_enable && io.write_address =/= 0.U) {
      // Map x1-x31 to indices 0-30 in physical storage
      registers(io.write_address - 1.U) := io.write_data
    }
  }

  // Read ports with x0 hardwired to zero and write forwarding for pipeline optimization
  // Timing optimization: nested Mux guards x0 first to avoid subtract on critical path
  // Priority: x0 check (fastest) → write forwarding → register read (with address mapping)
  io.read_data1 := Mux(
    io.read_address1 === 0.U,
    0.U, // x0 always zero - fastest path
    Mux(
      io.write_enable && io.write_address === io.read_address1,
      io.write_data,                    // Forward write data
      registers(io.read_address1 - 1.U) // Physical storage (mapped)
    )
  )

  io.read_data2 := Mux(
    io.read_address2 === 0.U,
    0.U, // x0 always zero - fastest path
    Mux(
      io.write_enable && io.write_address === io.read_address2,
      io.write_data,                    // Forward write data
      registers(io.read_address2 - 1.U) // Physical storage (mapped)
    )
  )

  io.debug_read_data := Mux(
    io.debug_read_address === 0.U,
    0.U, // x0 always zero - fastest path
    Mux(
      io.write_enable && io.write_address === io.debug_read_address,
      io.write_data,                         // Forward write data
      registers(io.debug_read_address - 1.U) // Physical storage (mapped)
    )
  )

}
