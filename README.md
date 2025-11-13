# RISC-V CPU Labs in Chisel

> [!NOTE]
> Code fragments marked `CA25: Exercise` are intentionally incomplete lab exercises.
> Only [0-minimal](0-minimal/) is complete; other implementations require improvements marked with `CA25: Exercise` comments.

This repository presents progressive RISC-V processor implementations in Chisel: single-cycle → interrupt-capable → pipelined.
Each lab increases architectural complexity while preserving common verification infrastructure.
All designs target RV32I ISA and execute real C programs compiled by GNU toolchain.
Verification combines ChiselTest unit tests with RISCOF compliance suite for architectural correctness.

The implementation uses SBT 1.10.7, Chisel 3.6.1, legacy FIRRTL 1.6.0, and Verilator 5.042+.
Verilog generation via `ChiselStage.emitVerilog()` eliminates CIRCT/firtool dependency.
This approach supports Linux, macOS Intel/ARM64, and other Unix-like systems.

## Repository Layout

### `0-minimal/`
This project implements an ultra-minimal 5-instruction CPU supporting only AUIPC, ADDI, LW, SW, and JALR.
The design demonstrates JIT self-modifying code execution patterns.
This serves as an educational example of focused processor design optimized for specific workloads.

### `1-single-cycle/`
This project provides a complete RV32I implementation with single-cycle execution.
The design uses Harvard architecture with separate instruction and data memory paths.
The implementation includes Verilator peripheral models and nine ChiselTest validation cases.

### `2-mmio-trap/`
This project extends the single-cycle core by adding Zicsr extension and machine-mode privilege level.
The CLINT peripheral generates timer and software interrupts for trap handling.
The design ensures atomic state updates through CSR-based trap mechanisms.

### `3-pipeline/`
This project offers four pipeline variants: one 3-stage baseline and three 5-stage implementations.
The variants demonstrate progressive optimization from stalling through forwarding to early branch resolution.
These techniques improve CPI from ~2.5 to ~1.2 through systematic hazard mitigation.

### `tests/`
This directory contains the RISCOF compliance framework for architectural validation.
The framework includes test plugins and reference model configuration for ISA conformance checking.

## Lab Highlights

### [Minimal CPU](0-minimal/)
This lab focuses on a minimal instruction set containing only AUIPC, ADDI, LW, SW, and JALR.
The processor features an addition-only ALU with word-aligned memory access restrictions.
The test program `jit.asmbin` (60 bytes) demonstrates the encode → copy → execute cycle of JIT compilation.
Verification occurs through debug register reads since the design omits ECALL support.
This educational approach demonstrates building processors tailored to specific workload requirements.

### [Single-Cycle Core](1-single-cycle/)
This lab implements the full RV32I instruction set achieving CPI=1 through single-cycle execution.
The Harvard architecture physically separates instruction and data memory paths for concurrent access.
Verilator peripheral models provide memory interfaces including ROM loader and instruction/data memory components.
Nine ChiselTest cases systematically validate ALU operations, control flow logic, and complete program execution.
The test programs include `fibonacci.asmbin` (recursive calculation) and `quicksort.asmbin` (array sorting algorithm).

### [Interrupt-Capable Core](2-mmio-trap/)
This lab implements the Zicsr extension including `CSRRW`, `CSRRS`, `CSRRC` and their immediate variants.
Machine-mode CSRs control trap behavior: `mstatus` (interrupt enable), `mtvec` (trap vector), `mepc` (return PC), `mcause` (trap reason).
The CLINT peripheral generates timer interrupts (`mtime`/`mtimecmp`) and software interrupts (`msip`).
The critical design decision gives CLINT CSR writes priority over CPU writes to ensure atomic trap entry.
This trap handling mechanism preserves single-cycle execution timing for non-trapping instructions.

### [Pipelined Cores](3-pipeline/)
This lab provides four implementations that demonstrate progressive hazard mitigation strategies.

| Variant | Stages | Hazard Strategy | CPI | Key Features |
|---------|--------|-----------------|-----|--------------|
| ThreeStage | IF-EX-WB | Stall on all hazards | ~2.5 | Learning baseline, simplified control |
| FiveStageStall | IF-ID-EX-MEM-WB | Stall-based detection | ~1.8 | Classic pipeline, no forwarding |
| FiveStageForward | IF-ID-EX-MEM-WB | EX/MEM → EX forwarding | ~1.3 | Eliminates most data hazards |
| FiveStageFinal (DEFAULT) | IF-ID-EX-MEM-WB | ID forwarding + early branch | ~1.2 | Reduced control hazard penalty |

All variants implement data hazard detection, control hazard handling, and pipeline flushing on branch misprediction.
The CSR and CLINT integration maintains correct trap handling despite pipelined execution complexity.
The comprehensive test suite validates pipeline register correctness, hazard scenarios, and complete program execution.

## Build and Test Workflow

### Requirements

The build system requires SBT 1.9.7+ for Scala project management and dependency resolution.
Chisel hardware generation needs version 3.6.1 with the legacy FIRRTL 1.6.0 compiler for Verilog output.
Simulation depends on Verilator 5.042+ which provides cycle-accurate C++ simulation with VCD waveform support.
Test program compilation optionally uses RISC-V GNU toolchain (default path: `$HOME/riscv/toolchain/bin/` or set `CROSS_COMPILE`).

The legacy FIRRTL compiler generates Verilog through the `ChiselStage.emitVerilog()` API.
This approach compiles slower than CIRCT but eliminates the external firtool dependency.
The toolchain selection enables builds on systems lacking pre-built CIRCT binaries such as macOS ARM64 and custom Linux distributions.

### Targets

Repository-wide targets (execute from project root):
```shell
make clean      # Remove build artifacts from all projects
make distclean  # Deep clean: RISCOF work directories, sbt cache, Verilator outputs, compliance test files
```

Per-project targets (execute from `0-minimal/`, `1-single-cycle/`, `2-mmio-trap/`, or `3-pipeline/`):
```shell
make test       # Run ChiselTest suite (unit and integration tests)
make verilator  # Generate Verilog via ChiselStage, compile Verilator C++ simulator with VCD tracing
make sim        # Execute Verilator simulation (default: VCD enabled, output to trace.vcd)
make indent     # Format Scala (scalafmt) and C++ (clang-format) sources
make compliance # Run RISCOF architectural compliance tests (RV32I validation)
```

Simulation customization:
```shell
make sim SIM_ARGS="-instruction src/main/resources/fibonacci.asmbin" SIM_TIME=100000  # Custom program and cycle limit
WRITE_VCD=0 make sim  # Disable VCD waveform generation for faster execution (useful for compliance tests)
```

## Learning Path

The recommended study sequence builds processor complexity progressively:
```
0-minimal    → Learn focused ISA design for specific workloads
1-single-cycle → Understand complete RV32I implementation
2-mmio-trap  → Add privileged architecture and interrupt handling
3-pipeline   → Optimize performance through pipelining
  ├─ ThreeStage      → Simplified pipeline fundamentals
  ├─ FiveStageStall  → Classic pipeline with hazards
  ├─ FiveStageForward → Data forwarding optimization
  └─ FiveStageFinal  → Control hazard reduction
```

The following critical source files contain comprehensive Scaladoc documentation:
- `src/main/scala/riscv/core/CPU.scala` implements top-level processor architecture and module integration
- `src/main/scala/riscv/core/Execute.scala` handles ALU operations, branch resolution, and CSR access logic
- `src/main/scala/riscv/core/InstructionDecode.scala` performs instruction field extraction and control signal generation
- `src/main/scala/riscv/core/MemoryAccess.scala` manages load/store alignment and MMIO device routing
- `src/main/scala/riscv/core/CSR.scala` (2-mmio-trap, 3-pipeline) implements machine-mode CSRs with interrupt priority
- `src/test/scala/riscv/compliance/ComplianceTestBase.scala` provides RISCOF integration framework

The architectural designs involve these fundamental trade-offs:
- Single-cycle achieves CPI=1 but combinational complexity limits clock frequency, while pipelines achieve CPI<2 with higher clock frequency at the cost of hazard logic overhead
- Stalling uses simple hardware but achieves lower IPC, while forwarding requires complex multiplexing but eliminates most stalls, and early branching performs ID-stage comparison to reduce flush penalties
- CSR atomic updates give CLINT priority to ensure trap state consistency and prevent race conditions between hardware interrupts and software CSR access

## Notes for Students

These labs build progressively where each project extends concepts from previous ones.
Students should complete the sequence in order to understand the architectural evolution from simple to complex designs.
Code sections marked `CA25: Exercise` require student implementation as part of the coursework assignments.
The existing module boundaries must be respected to maintain compatibility with the provided test infrastructure.
All components include detailed Scaladoc documentation which students should read before making modifications.
The `make test` command should be executed after each change to verify correctness through the ChiselTest validation suite.
Students can use `make sim` with VCD output for debugging where GTKWave or Surfer provide signal trace visualization.
The RISCOF compliance tests validate ISA conformance and passing all tests ensures the implementation meets architectural correctness requirements.
