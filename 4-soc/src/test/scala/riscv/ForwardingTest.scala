// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.Forwarding
import riscv.core.ForwardingType

class ForwardingTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Forwarding Unit")

  // Helper to set up common test conditions
  def setupForwarding(
      dut: Forwarding,
      rs1_id: Int,
      rs2_id: Int,
      rs1_ex: Int,
      rs2_ex: Int,
      rd_mem: Int,
      rd_wb: Int,
      we_mem: Boolean,
      we_wb: Boolean
  ): Unit = {
    dut.io.rs1_id.poke(rs1_id.U)
    dut.io.rs2_id.poke(rs2_id.U)
    dut.io.rs1_ex.poke(rs1_ex.U)
    dut.io.rs2_ex.poke(rs2_ex.U)
    dut.io.rd_mem.poke(rd_mem.U)
    dut.io.rd_wb.poke(rd_wb.U)
    dut.io.reg_write_enable_mem.poke(we_mem.B)
    dut.io.reg_write_enable_wb.poke(we_wb.B)
  }

  // ==================== EX Stage Forwarding Tests ====================

  it should "detect no forwarding when no hazard exists (EX stage)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=1, rs2_ex=2, but rd_mem=3, rd_wb=4 (no match)
      setupForwarding(dut, 0, 0, 1, 2, 3, 4, true, true)

      dut.io.reg1_forward_ex.expect(ForwardingType.NoForward)
      dut.io.reg2_forward_ex.expect(ForwardingType.NoForward)
    }
  }

  it should "forward from MEM stage to EX rs1 (1-cycle RAW hazard)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=5, rd_mem=5 -> forward from MEM
      setupForwarding(dut, 0, 0, 5, 2, 5, 0, true, false)

      dut.io.reg1_forward_ex.expect(ForwardingType.ForwardFromMEM)
      dut.io.reg2_forward_ex.expect(ForwardingType.NoForward)
    }
  }

  it should "forward from MEM stage to EX rs2 (1-cycle RAW hazard)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs2_ex=5, rd_mem=5 -> forward from MEM
      setupForwarding(dut, 0, 0, 1, 5, 5, 0, true, false)

      dut.io.reg1_forward_ex.expect(ForwardingType.NoForward)
      dut.io.reg2_forward_ex.expect(ForwardingType.ForwardFromMEM)
    }
  }

  it should "forward from WB stage to EX rs1 (2-cycle RAW hazard)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=5, rd_wb=5 -> forward from WB
      setupForwarding(dut, 0, 0, 5, 2, 0, 5, false, true)

      dut.io.reg1_forward_ex.expect(ForwardingType.ForwardFromWB)
      dut.io.reg2_forward_ex.expect(ForwardingType.NoForward)
    }
  }

  it should "forward from WB stage to EX rs2 (2-cycle RAW hazard)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs2_ex=5, rd_wb=5 -> forward from WB
      setupForwarding(dut, 0, 0, 1, 5, 0, 5, false, true)

      dut.io.reg1_forward_ex.expect(ForwardingType.NoForward)
      dut.io.reg2_forward_ex.expect(ForwardingType.ForwardFromWB)
    }
  }

  it should "prioritize MEM over WB for EX forwarding (both match)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=5 matches both rd_mem=5 and rd_wb=5 -> MEM wins
      setupForwarding(dut, 0, 0, 5, 2, 5, 5, true, true)

      dut.io.reg1_forward_ex.expect(ForwardingType.ForwardFromMEM)
    }
  }

  it should "not forward x0 (hardwired zero)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=0, rd_mem=0 -> no forward (x0 always reads as 0)
      setupForwarding(dut, 0, 0, 0, 1, 0, 0, true, true)

      dut.io.reg1_forward_ex.expect(ForwardingType.NoForward)
    }
  }

  it should "not forward when write enable is false" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=5, rd_mem=5 but we_mem=false -> no forward
      setupForwarding(dut, 0, 0, 5, 2, 5, 0, false, false)

      dut.io.reg1_forward_ex.expect(ForwardingType.NoForward)
    }
  }

  it should "fallback to WB when MEM disabled but WB matches" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_ex=5, rd_mem=5 (disabled), rd_wb=5 (enabled) -> forward from WB
      setupForwarding(dut, 0, 0, 5, 2, 5, 5, false, true)

      dut.io.reg1_forward_ex.expect(ForwardingType.ForwardFromWB)
    }
  }

  // ==================== ID Stage Forwarding Tests ====================

  it should "detect no forwarding when no hazard exists (ID stage)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_id=1, rs2_id=2, but rd_mem=3, rd_wb=4 (no match)
      setupForwarding(dut, 1, 2, 0, 0, 3, 4, true, true)

      dut.io.reg1_forward_id.expect(ForwardingType.NoForward)
      dut.io.reg2_forward_id.expect(ForwardingType.NoForward)
    }
  }

  it should "forward from MEM stage to ID rs1 (early branch resolution)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_id=5, rd_mem=5 -> forward from MEM to ID
      setupForwarding(dut, 5, 2, 0, 0, 5, 0, true, false)

      dut.io.reg1_forward_id.expect(ForwardingType.ForwardFromMEM)
      dut.io.reg2_forward_id.expect(ForwardingType.NoForward)
    }
  }

  it should "forward from MEM stage to ID rs2 (early branch resolution)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs2_id=5, rd_mem=5 -> forward from MEM to ID
      setupForwarding(dut, 1, 5, 0, 0, 5, 0, true, false)

      dut.io.reg1_forward_id.expect(ForwardingType.NoForward)
      dut.io.reg2_forward_id.expect(ForwardingType.ForwardFromMEM)
    }
  }

  it should "forward from WB stage to ID rs1" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_id=5, rd_wb=5 -> forward from WB to ID
      setupForwarding(dut, 5, 2, 0, 0, 0, 5, false, true)

      dut.io.reg1_forward_id.expect(ForwardingType.ForwardFromWB)
    }
  }

  it should "forward both rs1 and rs2 simultaneously (ID and EX)" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // Multiple hazards: rs1_id=5, rs2_id=6, rs1_ex=5, rs2_ex=6, rd_mem=5, rd_wb=6
      setupForwarding(dut, 5, 6, 5, 6, 5, 6, true, true)

      // ID stage: rs1_id=5 matches rd_mem=5, rs2_id=6 matches rd_wb=6
      dut.io.reg1_forward_id.expect(ForwardingType.ForwardFromMEM)
      dut.io.reg2_forward_id.expect(ForwardingType.ForwardFromWB)

      // EX stage: same pattern
      dut.io.reg1_forward_ex.expect(ForwardingType.ForwardFromMEM)
      dut.io.reg2_forward_ex.expect(ForwardingType.ForwardFromWB)
    }
  }

  it should "prioritize MEM over WB for ID forwarding" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_id=5 matches both rd_mem=5 and rd_wb=5 -> MEM wins
      setupForwarding(dut, 5, 2, 0, 0, 5, 5, true, true)

      dut.io.reg1_forward_id.expect(ForwardingType.ForwardFromMEM)
    }
  }

  it should "not forward x0 to ID stage" in {
    test(new Forwarding).withAnnotations(TestAnnotations.annos) { dut =>
      // rs1_id=0 with rd_mem=0 -> no forward
      setupForwarding(dut, 0, 1, 0, 0, 0, 0, true, true)

      dut.io.reg1_forward_id.expect(ForwardingType.NoForward)
    }
  }
}
