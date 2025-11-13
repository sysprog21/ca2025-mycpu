# Pipelined RISC-V CPU Lab

This project contains four pipelined RV32I processor implementations that demonstrate progressive performance optimization techniques.
Each design builds upon the single-cycle baseline while sharing common infrastructure including instruction memory, register file, and peripheral models from previous labs.
The implementations show how increasingly sophisticated pipeline techniques eliminate performance bottlenecks while preserving architectural correctness.

## Implementations at a Glance

| Implementation | Stages | Highlight |
| --- | --- | --- |
| `ImplementationType.ThreeStage` | IF → ID → EX/MEM/WB (folded) | Minimal pipeline that introduces control-flow redirection and CLINT interaction with a single execute stage. |
| `ImplementationType.FiveStageStall` | IF → ID → EX → MEM → WB | Classic five-stage design that resolves data hazards with interlocks (bubbles) and performs branch resolution in EX. |
| `ImplementationType.FiveStageForward` | IF → ID → EX → MEM → WB | Adds bypass paths from MEM/WB back to EX to reduce stalls caused by RAW hazards. |
| `ImplementationType.FiveStageFinal` | IF → ID → EX → MEM → WB | Combines forwarding, refined flush logic, and the optimized CLINT/CSR handshake that matches the interrupt-capable single-cycle core. |

Select the implementation by passing the desired constant to `new CPU(implementation = …)` in `board/verilator/Top.scala` or within the unit tests.

## Hazards in This Lab

Pipelining introduces overlapping instruction execution, which naturally creates hazards. The lab highlights three families and shows how each implementation responds:

### Structural Hazards

The pipelines assume a Harvard-style memory system: instruction fetch and data memory have independent ports, so structural hazards are intentionally avoided. The register file supports two reads and one write per cycle. If you experiment with alternative memories (for example, a unified single-port SRAM), you must introduce arbitration or buffers to avoid fetch/data conflicts.

### Data Hazards

1. Read-After-Write (RAW):
   - *Three-stage* and *five-stage stall* cores insert bubbles when an instruction consumes a value still in flight. This behavior is encoded in `Control.scala` and the hazard unit.
   - *Forwarding* and *final* cores extend `Forwarding.scala` to feed results from MEM or WB back into EX. The tests under `PipelineProgramTest` inspect the register file after running `hazard.asmbin` to ensure RAW hazards are resolved without corrupting architectural state.

2. Write-After-Write (WAW) / Write-After-Read (WAR):
   Single-issue in-order execution eliminates these hazards because writeback occurs in program order. The unified tests still monitor the register file (`hazard.asmbin`) to confirm that the chosen hazard strategy does not disturb older writes.

### Control Hazards

Branches and jumps must redirect the instruction fetch stage. The designs use the following techniques:

- `Control.scala` asserts flush signals whenever a taken branch or exception is detected.
- The CLINT module coordinates interrupt entry (`mtvec`, `mepc`, `mcause`) and ensures that the pipeline drains correctly before executing `mret`.
- `PipelineProgramTest` runs `irqtrap.asmbin`, toggles `interrupt_flag`, and checks that the machine returns to the instruction stream with a valid CSR snapshot. The test accepts either timer or external interrupt causes (`0x80000007` or `0x8000000b`) because both codes are architecturally legal depending on which device raised the interrupt.

### Interaction with the Hazard Unit

Each pipeline variant ties the hazard unit into the register file and the forwarding network. Pay special attention to:

- `Control.scala` (stall and flush decisions).
- `Forwarding.scala` (mux selects for EX operands).
- `ID2EX.scala` and `EX2MEM.scala` (registering control signals so that the hazard unit can observe the pipeline state).

The tests exercise these paths automatically, but it is useful to inspect waveform dumps (`make sim SIM_ARGS="..."`) to see how hazards propagate through the pipeline.

## Software Payloads

The available `.asmbin` programs fall into three groups:

| Program | Purpose |
| --- | --- |
| `fibonacci`, `quicksort`, `sb`, `hazard` | General RV32I coverage (ALU, branches, memory). |
| `irqtrap` | CSR and CLINT integration (interrupt entry/exit). |
| `uart` | MMIO interaction: programs the UART, writes a short string, and leaves a completion flag (`0xCAFEF00D`) in memory for automated checks. |

The Scala tests read back program outputs via the debug interfaces exposed through `TestTopModule`. The UART-specific harness in `PipelineUartTest` also tracks transmit byte count to ensure the MMIO behavior matches the expectations from Lab 2.

## Test Suite

The implementation includes comprehensive verification through multiple testing methodologies:

### ChiselTest Unit Tests (25 tests)

Located in `src/test/scala/riscv/`:

The test suite validates all four pipeline implementations (ThreeStage, FiveStageStall, FiveStageForward, FiveStageFinal) across multiple dimensions:

1. PipelineProgramTest: Validates correct execution of test programs
   - fibonacci.asmbin: Recursive Fibonacci calculation
   - quicksort.asmbin: Array sorting algorithm
   - hazard.asmbin: Data hazard handling (RAW, WAW)
   - irqtrap.asmbin: Interrupt entry/exit sequences

2. PipelineUartTest: MMIO peripheral verification
   - UART register access and configuration
   - TX/RX buffer operations
   - Memory-mapped I/O correctness

3. PipelineRegisterTest: Pipeline register functionality
   - IF2ID, ID2EX, EX2MEM, MEM2WB register correctness
   - Control signal propagation

All unit tests pass successfully:
```shell
make test
# Total number of tests run: 25
# Tests: succeeded 25, failed 0
```

### RISCOF Compliance Testing (119 tests)

RISC-V architectural compliance testing validates correct implementation of RV32I + Zicsr extensions with pipelined execution.

Test Coverage:
- RV32I base instruction set (41 tests)
- Zicsr extension - CSR instructions (40 tests)
  - CSRRW, CSRRS, CSRRC and immediate variants
  - Machine-mode CSR registers (mstatus, mie, mtvec, mepc, mcause, etc.)
  - Atomic read-modify-write semantics in pipelined context
- Physical Memory Protection (PMP) registers (38 tests)

Running Compliance Tests:
```shell
make compliance
# Expected duration: 10-15 minutes
# Results saved to: results/report.html
```

Last Verification: 2025-11-08
- Unit Tests: 25/25 passed (all 4 pipeline variants)
- RISCOF Compliance: 119/119 tests passed (RV32I + Zicsr + PMP)
- Verilator Simulation: Completed successfully with all test programs

### Simulator Behavior Notes

When running test programs, you may observe warnings like:
```
invalid read address 0x10000000
invalid write address 0x0ffffffc
```

These warnings are expected and harmless. RISC-V programs use stack addresses not mapped in the minimal simulator memory model. Programs execute correctly despite these warnings - they simply indicate memory accesses outside the simulated address space.

## Building and Testing

Available Makefile targets:
```shell
# Run ChiselTest unit tests
make test

# Generate Verilog and build Verilator simulator
make verilator

# Run Verilator simulation with a test program
make sim SIM_ARGS="-instruction src/main/resources/hazard.asmbin"

# Run RISCOF compliance tests
make compliance

# Format code (Scala + C++)
make indent

# Clean build artifacts
make clean

# Remove all generated files including compliance results
make distclean
```

## Extending the Lab

- Try inserting deliberate structural conflicts (e.g., single-port data/instruction memory) and extend `Control.scala` to schedule access.
- Add branch prediction or delayed-branch support. Update `PipelineProgramTest` with new directed cases to verify that your prediction mechanism handles back-to-back branches and trap entries.
- Explore dual-issue or longer pipelines. You can reuse the current verification scaffolding by adding new `PipelineConfig` entries and supplying the correct expected register values for the hazard tests.
