// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

case class PipelineConfig(name: String, implementation: Int, hazardX1: BigInt)

object PipelineConfigs {
  val ThreeStage: PipelineConfig =
    PipelineConfig("Three-stage Pipelined CPU", ImplementationType.ThreeStage, hazardX1 = 26)
  val FiveStageStall: PipelineConfig =
    PipelineConfig("Five-stage Pipelined CPU with Stalling", ImplementationType.FiveStageStall, hazardX1 = 46)
  val FiveStageForward: PipelineConfig =
    PipelineConfig("Five-stage Pipelined CPU with Forwarding", ImplementationType.FiveStageForward, hazardX1 = 27)
  val FiveStageFinal: PipelineConfig =
    PipelineConfig(
      "Five-stage Pipelined CPU with Reduced Branch Delay",
      ImplementationType.FiveStageFinal,
      hazardX1 = 26
    )

  val All: Seq[PipelineConfig] = Seq(ThreeStage, FiveStageStall, FiveStageForward, FiveStageFinal)
}
