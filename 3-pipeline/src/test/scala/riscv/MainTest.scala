package riscv

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MainTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Pipeline CPU"
  it should "execute custom main.asmbin program" in {
    test(new TestTopModule("main.asmbin", ImplementationType.FiveStageForward)).withAnnotations(TestAnnotations.annos) { c =>
      c.clock.setTimeout(0)
      for (i <- 1 to 1000) {
        c.clock.step()
      }
    }
  }
}
