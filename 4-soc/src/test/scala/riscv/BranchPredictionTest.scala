// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.BranchTargetBuffer
import riscv.core.ReturnAddressStack

class BranchTargetBufferTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Branch Target Buffer")

  it should "not predict taken on empty BTB (cold miss)" in {
    test(new BranchTargetBuffer(16)).withAnnotations(TestAnnotations.annos) { dut =>
      // Query an address - BTB is empty, should not predict taken
      dut.io.pc.poke(0x1000.U)
      dut.io.update_valid.poke(false.B)

      dut.io.predicted_taken.expect(false.B)
      // predicted_pc should be pc+4 when not predicting taken
      dut.io.predicted_pc.expect(0x1004.U)
    }
  }

  it should "predict taken after a branch is taken and recorded" in {
    test(new BranchTargetBuffer(16)).withAnnotations(TestAnnotations.annos) { dut =>
      val branchPC = 0x1000L
      val targetPC = 0x2000L

      // Update BTB with taken branch
      dut.io.update_valid.poke(true.B)
      dut.io.update_pc.poke(branchPC.U)
      dut.io.update_target.poke(targetPC.U)
      dut.io.update_taken.poke(true.B)
      dut.clock.step()
      dut.io.update_valid.poke(false.B)

      // Query same PC - should hit and predict taken (counter initialized to 2 = WT)
      dut.io.pc.poke(branchPC.U)
      dut.clock.step()

      dut.io.predicted_taken.expect(true.B)
      dut.io.predicted_pc.expect(targetPC.U)
    }
  }

  it should "not predict taken when counter falls below threshold" in {
    test(new BranchTargetBuffer(16)).withAnnotations(TestAnnotations.annos) { dut =>
      val branchPC = 0x1000L
      val targetPC = 0x2000L

      // First, record a taken branch (counter = 2)
      dut.io.update_valid.poke(true.B)
      dut.io.update_pc.poke(branchPC.U)
      dut.io.update_target.poke(targetPC.U)
      dut.io.update_taken.poke(true.B)
      dut.clock.step()

      // Now record it as not-taken 3 times (counter: 2 -> 1 -> 0 -> 0)
      dut.io.update_taken.poke(false.B)
      dut.clock.step()
      dut.clock.step()
      dut.clock.step()
      dut.io.update_valid.poke(false.B)

      // Query - should not predict taken (counter < 2)
      dut.io.pc.poke(branchPC.U)
      dut.clock.step()

      dut.io.predicted_taken.expect(false.B)
    }
  }

  it should "saturate counter at maximum (3) on repeated taken" in {
    test(new BranchTargetBuffer(16)).withAnnotations(TestAnnotations.annos) { dut =>
      val branchPC = 0x1000L
      val targetPC = 0x2000L

      // Record taken branch 5 times (counter should saturate at 3)
      for (_ <- 0 until 5) {
        dut.io.update_valid.poke(true.B)
        dut.io.update_pc.poke(branchPC.U)
        dut.io.update_target.poke(targetPC.U)
        dut.io.update_taken.poke(true.B)
        dut.clock.step()
      }
      dut.io.update_valid.poke(false.B)

      // Query - should still predict taken
      dut.io.pc.poke(branchPC.U)
      dut.clock.step()
      dut.io.predicted_taken.expect(true.B)

      // Now one not-taken should not flip prediction (counter 3 -> 2, still >= 2)
      dut.io.update_valid.poke(true.B)
      dut.io.update_taken.poke(false.B)
      dut.clock.step()
      dut.io.update_valid.poke(false.B)

      dut.io.pc.poke(branchPC.U)
      dut.clock.step()
      dut.io.predicted_taken.expect(true.B) // Still taken at counter=2
    }
  }

  it should "invalidate entry when counter would reach 0 (free slot)" in {
    test(new BranchTargetBuffer(16)).withAnnotations(TestAnnotations.annos) { dut =>
      val branchPC = 0x1000L
      val targetPC = 0x2000L

      // First, record a taken branch to create an entry (counter = 2)
      dut.io.update_valid.poke(true.B)
      dut.io.update_pc.poke(branchPC.U)
      dut.io.update_target.poke(targetPC.U)
      dut.io.update_taken.poke(true.B)
      dut.clock.step()

      // Record not-taken twice (counter: 2 -> 1 -> invalid)
      // Entry is invalidated when counter would reach 0 to free the slot
      dut.io.update_taken.poke(false.B)
      dut.clock.step() // counter: 2 -> 1
      dut.clock.step() // counter: 1 -> invalid
      dut.io.update_valid.poke(false.B)

      // Query - should not predict taken (entry invalidated)
      dut.io.pc.poke(branchPC.U)
      dut.clock.step()
      dut.io.predicted_taken.expect(false.B)

      // Now a taken update creates a NEW entry with counter=2 (Weakly Taken)
      // This is a "fresh start" for the branch
      dut.io.update_valid.poke(true.B)
      dut.io.update_taken.poke(true.B)
      dut.clock.step()
      dut.io.update_valid.poke(false.B)

      dut.io.pc.poke(branchPC.U)
      dut.clock.step()
      dut.io.predicted_taken.expect(true.B) // New entry with counter=2 predicts taken
    }
  }

  it should "handle aliasing (different PCs mapping to same index)" in {
    test(new BranchTargetBuffer(16)).withAnnotations(TestAnnotations.annos) { dut =>
      // With 16 entries, index = PC[5:2], so PC 0x1000 and 0x1040 have same index
      val pc1     = 0x1000L // index = 0
      val pc2     = 0x1040L // index = 0 (same index, different tag)
      val target1 = 0x2000L
      val target2 = 0x3000L

      // Record first branch
      dut.io.update_valid.poke(true.B)
      dut.io.update_pc.poke(pc1.U)
      dut.io.update_target.poke(target1.U)
      dut.io.update_taken.poke(true.B)
      dut.clock.step()

      // Record second branch (evicts first due to same index)
      dut.io.update_pc.poke(pc2.U)
      dut.io.update_target.poke(target2.U)
      dut.clock.step()
      dut.io.update_valid.poke(false.B)

      // Query first PC - should miss (evicted)
      dut.io.pc.poke(pc1.U)
      dut.clock.step()
      dut.io.predicted_taken.expect(false.B)

      // Query second PC - should hit
      dut.io.pc.poke(pc2.U)
      dut.clock.step()
      dut.io.predicted_taken.expect(true.B)
      dut.io.predicted_pc.expect(target2.U)
    }
  }
}

class ReturnAddressStackTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Return Address Stack")

  it should "return invalid when empty" in {
    test(new ReturnAddressStack(4)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.push.poke(false.B)
      dut.io.pop.poke(false.B)
      dut.io.restore.poke(false.B)

      dut.io.valid.expect(false.B)
    }
  }

  it should "push and pop correctly" in {
    test(new ReturnAddressStack(4)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.restore.poke(false.B)
      dut.io.pop.poke(false.B)

      // Push address 0x1000
      dut.io.push.poke(true.B)
      dut.io.push_addr.poke(0x1000.U)
      dut.clock.step()
      dut.io.push.poke(false.B)

      // Stack should be valid with 0x1000 on top
      dut.io.valid.expect(true.B)
      dut.io.predicted_addr.expect(0x1000.U)

      // Push another address 0x2000
      dut.io.push.poke(true.B)
      dut.io.push_addr.poke(0x2000.U)
      dut.clock.step()
      dut.io.push.poke(false.B)

      // Top should now be 0x2000
      dut.io.valid.expect(true.B)
      dut.io.predicted_addr.expect(0x2000.U)

      // Pop - should get 0x2000 then 0x1000
      dut.io.pop.poke(true.B)
      dut.clock.step()

      // After pop, top is 0x1000
      dut.io.valid.expect(true.B)
      dut.io.predicted_addr.expect(0x1000.U)

      dut.clock.step()
      dut.io.pop.poke(false.B)

      // Stack empty after second pop
      dut.io.valid.expect(false.B)
    }
  }

  it should "handle overflow gracefully (shift down)" in {
    test(new ReturnAddressStack(4)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.restore.poke(false.B)
      dut.io.pop.poke(false.B)

      // Push 5 addresses into 4-entry stack
      for (i <- 1 to 5) {
        dut.io.push.poke(true.B)
        dut.io.push_addr.poke((i * 0x1000).U)
        dut.clock.step()
      }
      dut.io.push.poke(false.B)

      // Top should be 0x5000 (most recent)
      dut.io.valid.expect(true.B)
      dut.io.predicted_addr.expect(0x5000.U)

      // Pop all 4 - should get 5000, 4000, 3000, 2000 (oldest 1000 was lost)
      val expected = Seq(0x5000L, 0x4000L, 0x3000L, 0x2000L)
      for (exp <- expected) {
        dut.io.predicted_addr.expect(exp.U)
        dut.io.pop.poke(true.B)
        dut.clock.step()
      }
      dut.io.pop.poke(false.B)

      // Now empty
      dut.io.valid.expect(false.B)
    }
  }

  it should "handle underflow gracefully (saturate at 0)" in {
    test(new ReturnAddressStack(4)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.restore.poke(false.B)
      dut.io.push.poke(false.B)

      // Push one address
      dut.io.push.poke(true.B)
      dut.io.push_addr.poke(0x1000.U)
      dut.clock.step()
      dut.io.push.poke(false.B)

      // Pop twice (second should be underflow)
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.clock.step() // Second pop on empty stack
      dut.io.pop.poke(false.B)

      // Should still be empty, not crash
      dut.io.valid.expect(false.B)
    }
  }

  it should "handle simultaneous push and pop (replace TOS)" in {
    test(new ReturnAddressStack(4)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.restore.poke(false.B)

      // Push initial address
      dut.io.push.poke(true.B)
      dut.io.push_addr.poke(0x1000.U)
      dut.io.pop.poke(false.B)
      dut.clock.step()

      // Simultaneous push and pop (tail call pattern)
      dut.io.push.poke(true.B)
      dut.io.push_addr.poke(0x2000.U)
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.push.poke(false.B)
      dut.io.pop.poke(false.B)

      // TOS should be replaced with 0x2000
      dut.io.valid.expect(true.B)
      dut.io.predicted_addr.expect(0x2000.U)
    }
  }

  it should "restore state after misprediction" in {
    test(new ReturnAddressStack(4)).withAnnotations(TestAnnotations.annos) { dut =>
      dut.io.push.poke(false.B)
      dut.io.pop.poke(false.B)

      // Push two addresses
      dut.io.push.poke(true.B)
      dut.io.push_addr.poke(0x1000.U)
      dut.clock.step()
      dut.io.push_addr.poke(0x2000.U)
      dut.clock.step()
      dut.io.push.poke(false.B)

      // Speculative pop
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      // TOS is now 0x1000
      dut.io.predicted_addr.expect(0x1000.U)

      // Restore 0x2000 (misprediction recovery)
      dut.io.restore.poke(true.B)
      dut.io.restore_valid.poke(true.B)
      dut.io.restore_addr.poke(0x2000.U)
      dut.clock.step()
      dut.io.restore.poke(false.B)

      // TOS should be back to 0x2000
      dut.io.predicted_addr.expect(0x2000.U)
    }
  }
}
