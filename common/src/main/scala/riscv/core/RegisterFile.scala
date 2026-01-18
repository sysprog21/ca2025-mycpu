// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

// RISC-V register ABI names
object Registers extends Enumeration {
  type Register = Value
  val zero, ra, sp, gp, tp, t0, t1, t2, fp, s1, a0, a1, a2, a3, a4, a5, a6, a7, s2, s3, s4, s5, s6, s7, s8, s9, s10,
      s11, t3, t4, t5, t6 = Value
}

// Register File: 32 general-purpose registers (x0 hardwired to zero)
// x0 is architecturally defined as constant zero per RISC-V spec
// We don't allocate storage for x0, saving resources and ensuring compliance
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
  // This saves 3% of register file resources and makes x0 handling explicit
  val registers = RegInit(VecInit(Seq.fill(Parameters.PhysicalRegisters - 1)(0.U(Parameters.DataWidth))))

  when(!reset.asBool) {
    when(io.write_enable && io.write_address =/= 0.U) {
      // Map x1-x31 to indices 0-30 in physical storage
      registers(io.write_address - 1.U) := io.write_data
    }
  }

  io.read_data1 := Mux(
    io.read_address1 === 0.U,
    0.U,
    registers(io.read_address1 - 1.U)
  )

  io.read_data2 := Mux(
    io.read_address2 === 0.U,
    0.U,
    registers(io.read_address2 - 1.U)
  )

  io.debug_read_data := Mux(
    io.debug_read_address === 0.U,
    0.U,
    registers(io.debug_read_address - 1.U)
  )
}
