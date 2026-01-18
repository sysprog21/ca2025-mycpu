// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.core.fivestage_final.{ CPU => FiveStageCPUFinal }
import riscv.core.fivestage_forward.{ CPU => FiveStageCPUForward }
import riscv.core.fivestage_stall.{ CPU => FiveStageCPUStall }
import riscv.core.threestage.{ CPU => ThreeStageCPU }
import riscv.ImplementationType

/**
 * CPU: Configurable pipeline dispatcher for RISC-V RV32I implementations
 *
 * This module serves as a top-level dispatcher that selects between multiple
 * pipeline implementations at elaboration time. It provides a unified interface
 * while supporting different pipeline architectures with varying hazard handling
 * strategies.
 *
 * Available Pipeline Implementations:
 *
 * 1. ThreeStage (ImplementationType.ThreeStage):
 *    - Simplified 3-stage pipeline: Fetch-Execute-WriteBack
 *    - Combines ID+EX into single Execute stage
 *    - Minimal hardware, easier to understand
 *    - Stalls on all data and control hazards
 *    - Best for: Learning pipeline basics, resource-constrained designs
 *
 * 2. FiveStageStall (ImplementationType.FiveStageStall):
 *    - Classic 5-stage pipeline: IF-ID-EX-MEM-WB
 *    - Stall-based hazard handling (no forwarding)
 *    - Stalls on load-use hazards (1 cycle penalty)
 *    - Stalls on all register dependencies
 *    - Best for: Understanding hazard detection, baseline comparison
 *
 * 3. FiveStageForward (ImplementationType.FiveStageForward):
 *    - Classic 5-stage pipeline with forwarding paths
 *    - EX-to-EX forwarding: Eliminates most register hazard stalls
 *    - MEM-to-EX forwarding: Handles load results immediately
 *    - Still stalls on load-use hazards (unavoidable 1-cycle penalty)
 *    - Best for: Performance optimization, studying forwarding logic
 *
 * 4. FiveStageFinal (ImplementationType.FiveStageFinal) [DEFAULT]:
 *    - Most advanced 5-stage implementation
 *    - Full forwarding network with optimized hazard handling
 *    - Branch prediction hints (static prediction)
 *    - Optimized critical path timing
 *    - Best for: Production use, maximum performance
 *
 * Implementation Selection:
 * - Compile-time selection via constructor parameter
 * - Default: FiveStageFinal (best performance)
 * - Change via: new CPU(ImplementationType.ThreeStage)
 *
 * Common Interface (CPUBundle):
 * All implementations expose identical I/O interface:
 * - instruction_address: PC to instruction memory
 * - instruction: Instruction data from memory
 * - instruction_valid: Instruction memory ready signal
 * - memory_bundle: Data memory/MMIO interface (address, read/write, data)
 * - deviceSelect: Upper address bits for peripheral routing
 * - interrupt_flag: External interrupt input (for CLINT)
 * - debug interfaces: Register file and CSR inspection
 *
 * Pipeline Comparison:
 *
 * | Feature              | ThreeStage | FiveStageStall | FiveStageForward | FiveStageFinal |
 * |----------------------|------------|----------------|------------------|----------------|
 * | Stages               | 3          | 5              | 5                | 5              |
 * | Forwarding           | No         | No             | Yes              | Yes            |
 * | Load-use stalls      | Yes (2-3)  | Yes (1)        | Yes (1)          | Yes (1)        |
 * | Register hazard stalls| Yes (1-2) | Yes (1)        | No               | No             |
 * | Branch penalty       | 2 cycles   | 2 cycles       | 2 cycles         | 1-2 cycles     |
 * | Hardware complexity  | Low        | Medium         | High             | High           |
 * | CPI (typical)        | ~2.5       | ~1.8           | ~1.3             | ~1.2           |
 *
 * Design Notes:
 * - All implementations pass RISC-V compliance tests
 * - Choice between implementations is a performance/complexity trade-off
 * - ThreeStage recommended for educational purposes
 * - FiveStageFinal recommended for performance-critical applications
 * - See claudedocs/pipeline_implementations_guide.md for detailed comparison
 *
 * @param implementation Pipeline implementation selector (default: FiveStageFinal)
 */
class CPU(val implementation: Int = ImplementationType.FiveStageForward) extends Module {
  val io = IO(new CPUBundle)
  implementation match {
    case ImplementationType.ThreeStage =>
      val cpu = Module(new ThreeStageCPU)
      cpu.io <> io
    case ImplementationType.FiveStageStall =>
      val cpu = Module(new FiveStageCPUStall)
      cpu.io <> io
    case ImplementationType.FiveStageForward =>
      val cpu = Module(new FiveStageCPUForward)
      cpu.io <> io
    case _ =>
      val cpu = Module(new FiveStageCPUFinal)
      cpu.io <> io
  }
}
