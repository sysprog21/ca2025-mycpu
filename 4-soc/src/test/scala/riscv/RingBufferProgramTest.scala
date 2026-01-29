// // SPDX-License-Identifier: MIT
// // MyCPU is freely redistributable under the MIT License. See the file
// // "LICENSE" for information on usage and redistribution of this file.

// package riscv

// import chisel3._
// import chiseltest._
// import org.scalatest.flatspec.AnyFlatSpec

// class RingBufferProgramTest extends AnyFlatSpec with ChiselScalatestTester {
//   behavior.of("Ring Buffer Program")

//   private val RingBaseAddress   = 0x00100000L
//   private val ProdHeadOffset    = 0x0L
//   private val ProdTailOffset    = 0x40L
//   private val ConsHeadOffset    = 0x80L
//   private val ConsTailOffset    = 0xC0L
//   private val StartCountOffset  = 0x200L
//   private val StartGoOffset     = 0x204L
//   private val ResultOffset      = 0x208L
//   private val ExpectedTransfers = 10

//   private def readMemWord(dut: TestTopModule, address: Long): BigInt = {
//     dut.io.mem_debug_read_address.poke(address.U)
//     dut.clock.step()
//     dut.io.mem_debug_read_data.peek().litValue
//   }

//   it should "run ring-buffer and validate LR/SC + AMO results" in {
//     test(new TestTopModule("ring-buffer.asmbin")).withAnnotations(TestAnnotations.annos) { dut =>
//       dut.clock.setTimeout(0)

//       // Allow the ring-buffer program and synchronization to run.
//       dut.clock.step(200000)

//       val startCount = readMemWord(dut, RingBaseAddress + StartCountOffset)
//       val startGo    = readMemWord(dut, RingBaseAddress + StartGoOffset)
//       assert(startCount == 4, s"start_count should be 4, got $startCount")
//       assert(startGo == 1, s"start_go should be 1, got $startGo")

//       val prodHead = readMemWord(dut, RingBaseAddress + ProdHeadOffset)
//       val prodTail = readMemWord(dut, RingBaseAddress + ProdTailOffset)
//       val consHead = readMemWord(dut, RingBaseAddress + ConsHeadOffset)
//       val consTail = readMemWord(dut, RingBaseAddress + ConsTailOffset)
//       val result   = readMemWord(dut, RingBaseAddress + ResultOffset)

//       assert(prodHead == ExpectedTransfers, s"prod.head should be $ExpectedTransfers, got $prodHead")
//       assert(prodTail == ExpectedTransfers, s"prod.tail should be $ExpectedTransfers, got $prodTail")
//       assert(consHead == ExpectedTransfers, s"cons.head should be $ExpectedTransfers, got $consHead")
//       assert(consTail == ExpectedTransfers, s"cons.tail should be $ExpectedTransfers, got $consTail")
//       assert(result == 1, s"ring-buffer result flag should be 1, got $result")
//     }
//   }
// }
