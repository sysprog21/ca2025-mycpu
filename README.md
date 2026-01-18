# RISC-V CPU Labs in Chisel

> [!NOTE]
> Code fragments marked `CA25: Exercise` are intentionally incomplete lab exercises.
> [0-minimal](0-minimal/) and [4-soc](4-soc/) are complete implementations; other projects require improvements marked with `CA25: Exercise` comments.

This repository presents progressive RISC-V processor implementations in Chisel: single-cycle → interrupt-capable → pipelined → SoC.
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

### `4-soc/`
This project implements a complete System-on-Chip with AXI4-Lite bus interface.
The design includes VGA output (640x480@72Hz), UART (115200 baud), and advanced branch prediction.
Branch prediction combines BTB (32-entry), RAS (4-entry), and IndirectBTB (8-entry) for reduced control hazard penalties.

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

### [System-on-Chip](4-soc/)
This project integrates the pipelined core with AXI4-Lite bus protocol for standardized peripheral access.

| Component | Description | Features |
|-----------|-------------|----------|
| AXI4-Lite Bus | Master/slave state machines | Address decoder via bits[31:29], supports 8 slaves |
| VGA Controller | 640x480@72Hz display | 64x64 framebuffer with 6x scaling, 16-color palette, double buffering |
| UART Controller | Serial communication | 115200 baud, buffered TX/RX, interrupt support |
| Branch Prediction | Multi-level prediction | BTB (32-entry) + RAS (4-entry) + IndirectBTB (8-entry) |

The VGA peripheral uses dual-clock CDC for system and 31.5 MHz pixel clocks.
The branch prediction hierarchy prioritizes RAS for returns, IndirectBTB for function pointers, and BTB as fallback.
The MyCPU shell provides interactive debugging with memory inspection, CSR access, and performance counters.

## Build and Test Workflow

### Dependencies

**Core Requirements:**
- **SBT** 1.9.7+ - Scala build tool
- **Chisel** 3.6.1 with legacy FIRRTL compiler 1.6.0
- **Verilator** 5.042+ - HDL simulator for Verilog
- **RISC-V Toolchain** (optional) - For assembling C/asm test programs

**For Compliance Testing:**
- **RISCOF** 1.25.3+ - RISC-V Architectural Test Framework
  ```bash
  pip install riscof
  # Verify installation
  riscof --version
  ```

**Dependency Validation:**
```bash
make check-deps        # Validate all dependencies
make check-riscof      # Check RISCOF only
make check-toolchain   # Check RISC-V toolchain only
make check-verilator   # Check Verilator only
```

> **Note**: Run `make check-deps` before starting work to verify all required tools are installed.

### Build System Architecture

The legacy FIRRTL compiler generates Verilog through the `ChiselStage.emitVerilog()` API.
This approach compiles slower than CIRCT but eliminates the external firtool dependency.
The toolchain selection enables builds on systems lacking pre-built CIRCT binaries such as macOS ARM64 and custom Linux distributions.

### Repository-wide targets (from top-level directory)

```shell
make help          # Display all available build targets and usage
make check-deps    # Validate all dependencies (toolchain, verilator, riscof)
make test-all      # Run ChiselTest suite for all four projects
make clean         # Clean build artifacts from all projects
make distclean     # Deep clean: remove RISCOF results and all generated files
```

### Per-project targets (run from project directories)

Per-project targets (execute from `1-single-cycle/`, `2-mmio-trap/`, `3-pipeline/`, or `4-soc/`):
```shell
make test       # Run ChiselTest suite
make verilator  # Generate Verilog (via legacy FIRRTL compiler) and build Verilator simulator
make sim        # Run Verilator simulation; generates waveforms in trace.vcd
make indent     # Format Scala and C++ sources (scalafmt + clang-format)
make clean      # Remove build artifacts
make compliance # Run RISCOF compliance tests (validates RISCOF first)
```

Additional targets for `4-soc/`:
```shell
make check-vga   # Run VGA test with nyancat demo (requires SDL2)
make check-uart  # Run UART loopback and echo tests
make shell       # Start interactive MyCPU shell
make sim BINARY=csrc/nyancat.asmbin  # Run simulation with custom binary
```

**Note on compliance testing:** The `make compliance` target automatically validates RISCOF installation before running tests. This provides immediate feedback if RISCOF is missing, rather than discovering it 10-15 minutes into the test run.

The `make verilator` target performs two steps:
1. Generate Verilog from Chisel using `sbt "runMain board.verilator.VerilogGenerator"`
2. Compile the Verilator C++ simulator with VCD tracing support

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
4-soc        → Integrate with bus and peripherals
  ├─ AXI4-Lite       → Standardized bus protocol
  ├─ VGA/UART        → Peripheral integration
  └─ Branch Prediction → Advanced control flow optimization
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

## License
This project is available under a permissive MIT-style license.
Use of this source code is governed by a MIT license that can be found in the [LICENSE](LICENSE) file.
