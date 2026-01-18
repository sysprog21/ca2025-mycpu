// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.core.threestage.CSRDirectAccessBundle
import riscv.Parameters

object CSRRegister {
  // Refer to Spec. Vol.II Page 8-10
  val MSTATUS  = 0x300.U(Parameters.CSRRegisterAddrWidth)
  val MIE      = 0x304.U(Parameters.CSRRegisterAddrWidth)
  val MTVEC    = 0x305.U(Parameters.CSRRegisterAddrWidth)
  val MSCRATCH = 0x340.U(Parameters.CSRRegisterAddrWidth)
  val MEPC     = 0x341.U(Parameters.CSRRegisterAddrWidth)
  val MCAUSE   = 0x342.U(Parameters.CSRRegisterAddrWidth)
  val CycleL   = 0xc00.U(Parameters.CSRRegisterAddrWidth)
  val CycleH   = 0xc80.U(Parameters.CSRRegisterAddrWidth)
}

class CSR extends Module {
  val io = IO(new Bundle {
    val reg_read_address_id    = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val reg_write_enable_ex    = Input(Bool())
    val reg_write_address_ex   = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val reg_write_data_ex      = Input(UInt(Parameters.DataWidth))
    val debug_reg_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))

    val id_reg_read_data    = Output(UInt(Parameters.DataWidth))
    val debug_reg_read_data = Output(UInt(Parameters.DataWidth))

    val clint_access_bundle = Flipped(new CSRDirectAccessBundle)
  })

  val mstatus  = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mie      = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mtvec    = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mscratch = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mepc     = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mcause   = RegInit(UInt(Parameters.DataWidth), 0.U)
  val cycles   = RegInit(UInt(64.W), 0.U)
  val regLUT =
    IndexedSeq(
      CSRRegister.MSTATUS  -> mstatus,
      CSRRegister.MIE      -> mie,
      CSRRegister.MTVEC    -> mtvec,
      CSRRegister.MSCRATCH -> mscratch,
      CSRRegister.MEPC     -> mepc,
      CSRRegister.MCAUSE   -> mcause,
      CSRRegister.CycleL   -> cycles(31, 0),
      CSRRegister.CycleH   -> cycles(63, 32),
    )
  cycles := cycles + 1.U

  // If the pipeline and the CLINT are going to read and write the CSR at the same time, let the pipeline write first.
  // This is implemented in a single cycle by passing reg_write_data_ex to clint and writing the data from the CLINT to the CSR.
  io.id_reg_read_data    := MuxLookup(io.reg_read_address_id, 0.U)(regLUT)
  io.debug_reg_read_data := MuxLookup(io.debug_reg_read_address, 0.U)(regLUT)

  io.clint_access_bundle.mstatus := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MSTATUS,
    io.reg_write_data_ex,
    mstatus
  )
  io.clint_access_bundle.mtvec := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MTVEC,
    io.reg_write_data_ex,
    mtvec
  )
  io.clint_access_bundle.mcause := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MCAUSE,
    io.reg_write_data_ex,
    mcause
  )
  io.clint_access_bundle.mepc := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MEPC,
    io.reg_write_data_ex,
    mepc
  )
  io.clint_access_bundle.mie := Mux(
    io.reg_write_enable_ex && io.reg_write_address_ex === CSRRegister.MIE,
    io.reg_write_data_ex,
    mie
  )

  when(io.clint_access_bundle.direct_write_enable) {
    mstatus := io.clint_access_bundle.mstatus_write_data
    mepc    := io.clint_access_bundle.mepc_write_data
    mcause  := io.clint_access_bundle.mcause_write_data
  }.elsewhen(io.reg_write_enable_ex) {
    when(io.reg_write_address_ex === CSRRegister.MSTATUS) {
      mstatus := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MEPC) {
      mepc := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MCAUSE) {
      mcause := io.reg_write_data_ex
    }
  }

  when(io.reg_write_enable_ex) {
    when(io.reg_write_address_ex === CSRRegister.MIE) {
      mie := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MTVEC) {
      mtvec := io.reg_write_data_ex
    }.elsewhen(io.reg_write_address_ex === CSRRegister.MSCRATCH) {
      mscratch := io.reg_write_data_ex
    }
  }
}
