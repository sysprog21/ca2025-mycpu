// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.singlecycle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.ALUOp1Source
import riscv.core.ALUOp2Source
import riscv.core.InstructionDecode
import riscv.core.InstructionTypes
import riscv.core.RegWriteSource
import riscv.TestAnnotations

class InstructionDecoderTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("InstructionDecoder")
  it should "decode RV32I instructions and generate correct control signals" in {
    test(new InstructionDecode).withAnnotations(TestAnnotations.annos) { c =>
      // InstructionTypes.L , I-type load command
      c.io.instruction.poke(0x0040a183L.U) // lw x3, 4(x1)
      c.io.ex_aluop1_source.expect(ALUOp1Source.Register)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.ex_immediate.expect(4.U)
      c.io.regs_reg1_read_address.expect(1.U)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(3.U)
      c.io.wb_reg_write_source.expect(RegWriteSource.Memory)
      c.io.memory_read_enable.expect(true.B)
      c.io.memory_write_enable.expect(false.B)
      c.clock.step()

      // InstructionTypes.S
      c.io.instruction.poke(0x00a02223L.U) // sw x10, 4(x0)
      c.io.ex_aluop1_source.expect(ALUOp1Source.Register)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.ex_immediate.expect(4.U)
      c.io.regs_reg1_read_address.expect(0.U)
      c.io.regs_reg2_read_address.expect(10.U)
      c.io.memory_write_enable.expect(true.B)
      c.io.reg_write_enable.expect(false.B)
      c.clock.step()

      // InstructionTypes.I, I-type instructions
      c.io.instruction.poke(0x0184f193L.U) // andi x3, x9, 24
      c.io.ex_aluop1_source.expect(ALUOp1Source.Register)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.ex_immediate.expect(24.U)
      c.io.regs_reg1_read_address.expect(9.U)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(3.U)
      c.io.wb_reg_write_source.expect(RegWriteSource.ALUResult)
      c.clock.step()

      // InstructionTypes.B, B-type instructions
      c.io.instruction.poke(0x00415863L.U) // bge x2, x4, 16
      c.io.ex_aluop1_source.expect(ALUOp1Source.InstructionAddress)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.ex_immediate.expect(16.U)
      c.io.regs_reg1_read_address.expect(2.U)
      c.io.regs_reg2_read_address.expect(4.U)
      c.clock.step()

      // InstructionTypes.RM, R-type instructions
      c.io.instruction.poke(0x002081b3L.U) // add
      c.io.ex_aluop1_source.expect(ALUOp1Source.Register)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Register)
      c.io.regs_reg1_read_address.expect(1.U)
      c.io.regs_reg2_read_address.expect(2.U)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(3.U)
      c.clock.step()

      // lui, U-type
      c.io.instruction.poke(0x000022b7L.U) // lui x5, 2
      c.io.regs_reg1_read_address.expect(0.U)
      c.io.ex_aluop1_source.expect(ALUOp1Source.Register) // little special, see how ID and EX treat lui
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(5.U)
      c.io.ex_immediate.expect((2 << 12).U)
      c.io.wb_reg_write_source.expect(RegWriteSource.ALUResult)
      c.clock.step()

      // jal, J-type
      c.io.instruction.poke(0x008002efL.U) // jal x5, 8
      c.io.ex_aluop1_source.expect(ALUOp1Source.InstructionAddress)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.ex_immediate.expect(8.U)
      c.io.wb_reg_write_source.expect(RegWriteSource.NextInstructionAddress)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(5.U)
      c.clock.step()

      // jalr, I-type
      c.io.instruction.poke(0x008082e7L.U) // jalr x5, x1, 8
      c.io.ex_aluop1_source.expect(ALUOp1Source.Register)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.regs_reg1_read_address.expect(1.U)
      c.io.ex_immediate.expect(8.U)
      c.io.wb_reg_write_source.expect(RegWriteSource.NextInstructionAddress)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(5.U)
      c.clock.step()

      // auipc, U-type
      c.io.instruction.poke(0x00007117L.U) // auipc x2, 7
      c.io.ex_aluop1_source.expect(ALUOp1Source.InstructionAddress)
      c.io.ex_aluop2_source.expect(ALUOp2Source.Immediate)
      c.io.ex_immediate.expect((7 << 12).U)
      c.io.reg_write_enable.expect(true.B)
      c.io.reg_write_address.expect(2.U)
      c.io.wb_reg_write_source.expect(RegWriteSource.ALUResult)
      c.clock.step()

    }
  }
}
