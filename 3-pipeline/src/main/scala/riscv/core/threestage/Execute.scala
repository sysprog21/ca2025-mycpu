// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core.threestage

import chisel3._
import chisel3.util._
import peripheral.RAMBundle
import riscv.core.ALU
import riscv.core.ALUControl
import riscv.Parameters

class Execute extends Module {
  val io = IO(new Bundle {
    val instruction                     = Input(UInt(Parameters.InstructionWidth))
    val instruction_address             = Input(UInt(Parameters.AddrWidth))
    val reg1_data                       = Input(UInt(Parameters.DataWidth))
    val reg2_data                       = Input(UInt(Parameters.DataWidth))
    val csr_read_data                   = Input(UInt(Parameters.DataWidth))
    val immediate_id                    = Input(UInt(Parameters.DataWidth))
    val aluop1_source_id                = Input(UInt(1.W))
    val aluop2_source_id                = Input(UInt(1.W))
    val memory_read_enable_id           = Input(Bool())
    val memory_write_enable_id          = Input(Bool())
    val regs_write_source_id            = Input(UInt(2.W))
    val interrupt_assert_clint          = Input(Bool())
    val interrupt_handler_address_clint = Input(UInt(Parameters.AddrWidth))

    val memory_bundle = Flipped(new RAMBundle)

    val csr_write_data     = Output(UInt(Parameters.DataWidth))
    val regs_write_data    = Output(UInt(Parameters.DataWidth))
    val if_jump_flag       = Output(Bool())
    val if_jump_address    = Output(UInt(Parameters.AddrWidth))
    val clint_jump_flag    = Output(Bool())
    val clint_jump_address = Output(UInt(Parameters.AddrWidth))
  })

  val opcode = io.instruction(6, 0)
  val funct3 = io.instruction(14, 12)
  val funct7 = io.instruction(31, 25)
  val uimm   = io.instruction(19, 15)

  // ALU compute
  val alu      = Module(new ALU)
  val alu_ctrl = Module(new ALUControl)

  alu_ctrl.io.opcode := opcode
  alu_ctrl.io.funct3 := funct3
  alu_ctrl.io.funct7 := funct7
  alu.io.func        := alu_ctrl.io.alu_funct
  alu.io.op1 := Mux(
    io.aluop1_source_id === ALUOp1Source.InstructionAddress,
    io.instruction_address,
    io.reg1_data
  )
  alu.io.op2 := Mux(
    io.aluop2_source_id === ALUOp2Source.Immediate,
    io.immediate_id,
    io.reg2_data
  )
  io.csr_write_data := MuxLookup(
    funct3,
    0.U
  )(
    IndexedSeq(
      InstructionsTypeCSR.csrrw  -> io.reg1_data,
      InstructionsTypeCSR.csrrc  -> (io.csr_read_data & (~io.reg1_data).asUInt),
      InstructionsTypeCSR.csrrs  -> (io.csr_read_data | io.reg1_data),
      InstructionsTypeCSR.csrrwi -> (0.U(27.W) ## uimm),
      InstructionsTypeCSR.csrrci -> (io.csr_read_data & (~(0.U(27.W) ## uimm)).asUInt),
      InstructionsTypeCSR.csrrsi -> (io.csr_read_data | 0.U(27.W) ## uimm),
    )
  )

  // memory access
  val mem_address_index = alu.io.result(log2Up(Parameters.WordSize) - 1, 0).asUInt
  val mem_read_data     = Wire(UInt(Parameters.DataWidth))
  io.memory_bundle.write_enable := false.B
  io.memory_bundle.write_data   := 0.U
  io.memory_bundle.address      := alu.io.result
  io.memory_bundle.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  mem_read_data                 := 0.U

  when(io.memory_read_enable_id) {
    val data  = io.memory_bundle.read_data
    val bytes = Wire(Vec(Parameters.WordSize, UInt(Parameters.ByteWidth)))
    for (i <- 0 until Parameters.WordSize) {
      bytes(i) := data((i + 1) * Parameters.ByteBits - 1, i * Parameters.ByteBits)
    }
    val byte = bytes(mem_address_index)
    val half = Mux(mem_address_index(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))

    mem_read_data := MuxLookup(funct3, 0.U)(
      Seq(
        InstructionsTypeL.lb  -> Cat(Fill(24, byte(7)), byte),
        InstructionsTypeL.lbu -> Cat(Fill(24, 0.U), byte),
        InstructionsTypeL.lh  -> Cat(Fill(16, half(15)), half),
        InstructionsTypeL.lhu -> Cat(Fill(16, 0.U), half),
        InstructionsTypeL.lw  -> data
      )
    )
  }.elsewhen(io.memory_write_enable_id) {
    io.memory_bundle.write_enable := true.B
    val data         = io.reg2_data
    val strobeInit   = VecInit(Seq.fill(Parameters.WordSize)(false.B))
    val writeStrobes = WireInit(strobeInit)
    val writeData    = WireDefault(0.U(Parameters.DataWidth))
    switch(funct3) {
      is(InstructionsTypeS.sb) {
        writeStrobes(mem_address_index) := true.B
        writeData                       := data(7, 0) << (mem_address_index << 3)
      }
      is(InstructionsTypeS.sh) {
        when(mem_address_index(1) === 0.U) {
          writeStrobes(0) := true.B
          writeStrobes(1) := true.B
          writeData       := data(15, 0)
        }.otherwise {
          writeStrobes(2) := true.B
          writeStrobes(3) := true.B
          writeData       := data(15, 0) << 16
        }
      }
      is(InstructionsTypeS.sw) {
        writeStrobes := VecInit(Seq.fill(Parameters.WordSize)(true.B))
        writeData    := data
      }
    }
    io.memory_bundle.write_data   := writeData
    io.memory_bundle.write_strobe := writeStrobes
  }

  // write back
  io.regs_write_data := MuxLookup(
    io.regs_write_source_id,
    alu.io.result
  )(
    IndexedSeq(
      RegWriteSource.Memory                 -> mem_read_data,
      RegWriteSource.CSR                    -> io.csr_read_data,
      RegWriteSource.NextInstructionAddress -> (io.instruction_address + 4.U)
    )
  )

  // jump and interrupt
  val instruction_jump_flag = (opcode === Instructions.jal) ||
    (opcode === Instructions.jalr) ||
    (opcode === InstructionTypes.B) && MuxLookup(
      funct3,
      false.B
    )(
      IndexedSeq(
        InstructionsTypeB.beq  -> (io.reg1_data === io.reg2_data),
        InstructionsTypeB.bne  -> (io.reg1_data =/= io.reg2_data),
        InstructionsTypeB.blt  -> (io.reg1_data.asSInt < io.reg2_data.asSInt),
        InstructionsTypeB.bge  -> (io.reg1_data.asSInt >= io.reg2_data.asSInt),
        InstructionsTypeB.bltu -> (io.reg1_data.asUInt < io.reg2_data.asUInt),
        InstructionsTypeB.bgeu -> (io.reg1_data.asUInt >= io.reg2_data.asUInt)
      )
    )
  io.clint_jump_flag    := instruction_jump_flag
  io.clint_jump_address := alu.io.result
  io.if_jump_flag       := io.interrupt_assert_clint || instruction_jump_flag
  io.if_jump_address    := Mux(io.interrupt_assert_clint, io.interrupt_handler_address_clint, alu.io.result)
}
