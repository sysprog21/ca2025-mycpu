// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ALUTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ALU"

  it should "perform zero operation correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.zero)
      dut.io.op1.poke(12345.U)
      dut.io.op2.poke(67890.U)
      dut.clock.step()
      dut.io.result.expect(0.U)
    }
  }

  it should "perform addition correctly" in {
    test(new ALU) { dut =>
      // Basic addition
      dut.io.func.poke(ALUFunctions.add)
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(20.U)
      dut.clock.step()
      dut.io.result.expect(30.U)

      // Addition with overflow (32-bit wrap around)
      dut.io.op1.poke("hFFFFFFFF".U)
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // Large numbers
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("h87654321".U)
      dut.clock.step()
      dut.io.result.expect("h99999999".U)
    }
  }

  it should "perform subtraction correctly" in {
    test(new ALU) { dut =>
      // Basic subtraction
      dut.io.func.poke(ALUFunctions.sub)
      dut.io.op1.poke(50.U)
      dut.io.op2.poke(30.U)
      dut.clock.step()
      dut.io.result.expect(20.U)

      // Subtraction with underflow (32-bit wrap around)
      dut.io.op1.poke(0.U)
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U)

      // Negative result representation
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(20.U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFF6".U) // -10 in two's complement
    }
  }

  it should "perform logical left shift correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.sll)

      // Basic shift
      dut.io.op1.poke(1.U)
      dut.io.op2.poke(4.U)
      dut.clock.step()
      dut.io.result.expect(16.U)

      // Shift by 0
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // Maximum shift (31 bits)
      dut.io.op1.poke(1.U)
      dut.io.op2.poke(31.U)
      dut.clock.step()
      dut.io.result.expect("h80000000".U)

      // Shift amount > 31 (only lower 5 bits used)
      dut.io.op1.poke(1.U)
      dut.io.op2.poke(36.U) // 36 = 32 + 4, so shifts by 4
      dut.clock.step()
      dut.io.result.expect(16.U)

      // Bits shifted out
      dut.io.op1.poke("h80000001".U)
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect(2.U) // High bit shifted out
    }
  }

  it should "perform signed less than comparison correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.slt)

      // Positive < Positive (true)
      dut.io.op1.poke(5.U)
      dut.io.op2.poke(10.U)
      dut.clock.step()
      dut.io.result.expect(1.U)

      // Positive < Positive (false)
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(5.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // Equal values
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(10.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // Negative < Positive (true)
      dut.io.op1.poke("hFFFFFFFF".U) // -1
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect(1.U)

      // Positive < Negative (false)
      dut.io.op1.poke(1.U)
      dut.io.op2.poke("hFFFFFFFF".U) // -1
      dut.clock.step()
      dut.io.result.expect(0.U)

      // Negative < Negative
      dut.io.op1.poke("hFFFFFFFE".U) // -2
      dut.io.op2.poke("hFFFFFFFF".U) // -1
      dut.clock.step()
      dut.io.result.expect(1.U)
    }
  }

  it should "perform XOR operation correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.xor)

      // Basic XOR
      dut.io.op1.poke("h0F0F0F0F".U)
      dut.io.op2.poke("hF0F0F0F0".U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U)

      // XOR with self (always 0)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("h12345678".U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // XOR with 0 (identity)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // XOR with all 1s (inversion)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("hFFFFFFFF".U)
      dut.clock.step()
      dut.io.result.expect("hEDCBA987".U)
    }
  }

  it should "perform OR operation correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.or)

      // Basic OR
      dut.io.op1.poke("h0F0F0F0F".U)
      dut.io.op2.poke("hF0F0F0F0".U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U)

      // OR with 0 (identity)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // OR with all 1s
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("hFFFFFFFF".U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U)

      // OR with self (idempotent)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("h12345678".U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)
    }
  }

  it should "perform AND operation correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.and)

      // Basic AND
      dut.io.op1.poke("h0F0F0F0F".U)
      dut.io.op2.poke("hF0F0F0F0".U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // AND with 0
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // AND with all 1s (identity)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("hFFFFFFFF".U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // AND with self (idempotent)
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("h12345678".U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // Masking operation
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke("h00FF00FF".U)
      dut.clock.step()
      dut.io.result.expect("h00340078".U)
    }
  }

  it should "perform logical right shift correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.srl)

      // Basic shift
      dut.io.op1.poke(16.U)
      dut.io.op2.poke(4.U)
      dut.clock.step()
      dut.io.result.expect(1.U)

      // Shift by 0
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // Shift negative number (logical, fills with 0)
      dut.io.op1.poke("h80000000".U)
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect("h40000000".U)

      // Maximum shift
      dut.io.op1.poke("h80000000".U)
      dut.io.op2.poke(31.U)
      dut.clock.step()
      dut.io.result.expect(1.U)

      // Shift amount > 31 (only lower 5 bits used)
      dut.io.op1.poke(16.U)
      dut.io.op2.poke(36.U) // 36 = 32 + 4, so shifts by 4
      dut.clock.step()
      dut.io.result.expect(1.U)
    }
  }

  it should "perform arithmetic right shift correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.sra)

      // Positive number shift (behaves like logical)
      dut.io.op1.poke(16.U)
      dut.io.op2.poke(4.U)
      dut.clock.step()
      dut.io.result.expect(1.U)

      // Shift by 0
      dut.io.op1.poke("h12345678".U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect("h12345678".U)

      // Negative number shift (fills with 1, sign extension)
      dut.io.op1.poke("h80000000".U) // -2147483648
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect("hC0000000".U) // Sign bit preserved

      // Maximum shift of negative number
      dut.io.op1.poke("h80000000".U)
      dut.io.op2.poke(31.U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U) // All 1s (sign-extended)

      // -1 shifted (always -1)
      dut.io.op1.poke("hFFFFFFFF".U)
      dut.io.op2.poke(5.U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U)

      // Negative number shift by 4
      dut.io.op1.poke("hFEDCBA98".U)
      dut.io.op2.poke(4.U)
      dut.clock.step()
      dut.io.result.expect("hFFEDCBA9".U)
    }
  }

  it should "perform unsigned less than comparison correctly" in {
    test(new ALU) { dut =>
      dut.io.func.poke(ALUFunctions.sltu)

      // Basic unsigned comparison
      dut.io.op1.poke(5.U)
      dut.io.op2.poke(10.U)
      dut.clock.step()
      dut.io.result.expect(1.U)

      // Reverse
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(5.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // Equal values
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(10.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // Large unsigned vs small (both treated as unsigned)
      dut.io.op1.poke("hFFFFFFFF".U) // 4294967295 (unsigned)
      dut.io.op2.poke(1.U)
      dut.clock.step()
      dut.io.result.expect(0.U) // False: 4294967295 is NOT < 1

      // Reverse: small < large
      dut.io.op1.poke(1.U)
      dut.io.op2.poke("hFFFFFFFF".U)
      dut.clock.step()
      dut.io.result.expect(1.U) // True: 1 < 4294967295

      // Two large values
      dut.io.op1.poke("h7FFFFFFF".U) // 2147483647
      dut.io.op2.poke("h80000000".U) // 2147483648 (unsigned)
      dut.clock.step()
      dut.io.result.expect(1.U) // True: 2147483647 < 2147483648
    }
  }

  it should "handle edge cases correctly" in {
    test(new ALU) { dut =>
      // All zeros
      dut.io.func.poke(ALUFunctions.add)
      dut.io.op1.poke(0.U)
      dut.io.op2.poke(0.U)
      dut.clock.step()
      dut.io.result.expect(0.U)

      // All ones
      dut.io.func.poke(ALUFunctions.and)
      dut.io.op1.poke("hFFFFFFFF".U)
      dut.io.op2.poke("hFFFFFFFF".U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFF".U)

      // Max positive + max positive (overflow)
      dut.io.func.poke(ALUFunctions.add)
      dut.io.op1.poke("h7FFFFFFF".U)
      dut.io.op2.poke("h7FFFFFFF".U)
      dut.clock.step()
      dut.io.result.expect("hFFFFFFFE".U) // Wraps to negative in two's complement
    }
  }

  it should "be combinational (no clock dependency)" in {
    test(new ALU) { dut =>
      // Set inputs
      dut.io.func.poke(ALUFunctions.add)
      dut.io.op1.poke(10.U)
      dut.io.op2.poke(20.U)

      // Result should be available immediately (combinational)
      dut.io.result.expect(30.U)

      // Change inputs without clock step
      dut.io.op1.poke(5.U)
      dut.io.op2.poke(15.U)

      // Result should update immediately
      dut.io.result.expect(20.U)
    }
  }
}
