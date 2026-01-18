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

## Lab Exercises (16-21)

This lab introduces 6 exercises that build upon the foundational concepts from previous labs (exercises 1-15). These exercises focus on pipeline-specific challenges: hazard detection, data forwarding, and control-flow management across multiple pipeline stages.

### Exercise Overview

Exercise 16: ALU Operation Implementation (`ALU.scala`)
- Complete all missing RV32I ALU operations (bitwise and shift operations)
- Implement SLL, SRL, SRA, SLT, SLTU, XOR, OR, AND
- Use 5-bit shift amounts (instruction[4:0])
- SRA requires arithmetic shift on signed integers
- Difficulty: Beginner
- Validation: PipelineProgramTest with fibonacci.asmbin and quicksort.asmbin
- Key Concepts: Bitwise logic, arithmetic vs logical shifts, unsigned/signed comparisons

Exercise 17: Data Forwarding to EX Stage (`fivestage_final/Forwarding.scala`)
- Implement EX stage forwarding path selection for rs1 and rs2
- Detect RAW (Read-After-Write) hazards from EX/MEM and MEM/WB pipeline registers
- Priority rule: MEM stage has priority over WB stage when both match
- Never forward from x0 (hardwired zero register)
- Difficulty: Intermediate
- Validation: PipelineProgramTest with hazard.asmbin (check hazardX1 values)
- Key Concepts: Data hazards, bypass paths, forwarding priority, register dependencies

Exercise 18: Data Forwarding to ID Stage (`fivestage_final/Forwarding.scala`)
- Implement ID stage forwarding path selection for early branch resolution
- Enable branch comparison in ID stage using forwarded data from EX/MEM or MEM/WB
- Priority rule: MEM stage has priority over WB stage
- Never forward from x0
- Difficulty: Intermediate to Advanced
- Validation: PipelineProgramTest with branches in hazard.asmbin
- Key Concepts: Early branch resolution, ID-stage timing, branch penalty reduction

Exercise 19: Pipeline Hazard Detection (`fivestage_final/Control.scala`)
- Implement comprehensive hazard detection logic
- Detect load-use hazards (EX and MEM stage loads)
- Detect jump register dependencies (JALR with pending load)
- Generate appropriate stall and flush signals
- Filter out x0 register dependencies
- Difficulty: Advanced
- Validation: PipelineProgramTest with hazard.asmbin and irqtrap.asmbin
- Key Concepts: Load-use hazards, jump dependencies, stall vs flush, pipeline control

Exercise 20: Pipeline Register Flush Logic (`fivestage_final/IF2ID.scala`)
- Wire pipeline registers with stall and flush support
- Instantiate PipelineRegister for instruction, instruction_address, interrupt_flag
- Set correct default values for flush: NOP (0x00000013), EntryAddress, 0.U
- Difficulty: Beginner to Intermediate
- Validation: PipelineRegisterTest
- Key Concepts: Pipeline register behavior, flush semantics, stall propagation

Exercise 21: Hazard Detection Summary and Analysis (`fivestage_final/Control.scala`)
- Conceptual understanding: explain stall vs flush distinction
- Explain why load-use hazards require stalls (data not ready)
- Explain jump dependencies with JALR
- Explain why branch penalty is 1 cycle with ID-stage resolution
- Summarize all stall conditions in the FiveStageFinal variant
- Difficulty: Beginner (conceptual)
- Validation: Understanding demonstrated through correct Exercise 19 implementation
- Key Concepts: Pipeline control theory, performance optimization, hazard taxonomy

### Exercise Implementation Workflow

The exercises are designed to be completed in a specific order that builds understanding progressively:

Phase 1: Foundation (Exercises 16, 20)
- Complete ALU operations (Exercise 16) to ensure basic functionality
- Implement pipeline register wiring (Exercise 20) to understand pipeline boundaries
- Run PipelineRegisterTest to validate pipeline register behavior
- These provide the foundation for hazard handling

Phase 2: Data Forwarding (Exercises 17, 18)
- Implement EX stage forwarding (Exercise 17) for ALU-to-ALU dependencies
- Implement ID stage forwarding (Exercise 18) for early branch resolution
- Study completed implementations in FiveStageStall and FiveStageForward variants
- Run PipelineProgramTest with hazard.asmbin to verify forwarding correctness
- Check that hazardX1 values match expected results for each variant

Phase 3: Hazard Control (Exercise 19)
- Implement comprehensive hazard detection logic
- Handle load-use hazards, jump dependencies, and control hazards
- Coordinate with forwarding paths from Exercises 17-18
- Run PipelineProgramTest with all test programs
- Verify correct stall insertion and flush behavior

Phase 4: Conceptual Understanding (Exercise 21)
- Reflect on hazard detection implementation
- Document understanding of stall vs flush decisions
- Explain performance implications of different hazard strategies
- Compare behavior across ThreeStage, FiveStageStall, FiveStageForward, FiveStageFinal

### Pipeline Concepts

Stall vs Flush

Stall: Freeze pipeline stages to prevent incorrect execution
- Used when data is not yet ready (load-use hazards)
- Inserts a "bubble" (NOP) into the pipeline
- Affected stages: IF and ID typically stall together
- Example: LW x1, 0(x2); ADD x3, x1, x4 requires a 1-cycle stall

Flush: Clear pipeline stages due to control flow changes
- Used when instructions should not execute (branch taken, exceptions)
- Sets pipeline registers to default values (NOP, EntryAddress)
- In FiveStageFinal: Only IF stage flushed on branch (ID already resolved)
- Example: BEQ taken flushes the incorrectly fetched instruction

Load-Use Hazards

Definition: An instruction needs data from a load instruction that hasn't completed
- Load instructions produce data in MEM stage (3 cycles after IF)
- Dependent instructions need data in EX stage (2 cycles after IF)
- Gap of 1 cycle cannot be resolved by forwarding alone

Example sequence:
```
LW  x1, 0(x2)     # Load produces result in MEM stage
ADD x3, x1, x4    # ADD needs x1 in EX stage (too early!)
```

Resolution:
- Stall the ADD instruction for 1 cycle
- Forward the loaded value from MEM to EX on the next cycle
- Total penalty: 1 cycle stall

ID-Stage Branch Resolution

Traditional approach (EX-stage branch resolution):
- Branch condition evaluated in EX stage
- Branch penalty: 2 cycles (flush IF and ID)
- Example: BEQ, BNE, BLT, BGE, BLTU, BGEU

Optimized approach (ID-stage branch resolution, FiveStageFinal):
- Branch condition evaluated in ID stage using forwarded operands
- Requires ID-stage forwarding (Exercise 18)
- Branch penalty: 1 cycle (flush only IF)
- Benefit: 50% reduction in branch penalty

Forwarding Paths:
- EX/MEM → ID: Forward ALU result to branch comparator
- MEM/WB → ID: Forward memory load result to branch comparator
- Priority: MEM stage has priority over WB stage

### Cross-References to Previous Labs

Exercises 16-21 build directly on concepts from earlier labs:

From 1-single-cycle and 2-mmio-trap:
- Exercise 4 (Branch Comparison): Now used in ID stage with forwarded data (Exercise 18)
- Exercise 3 (ALU Control): ALU must be complete for forwarding to work correctly (Exercise 16)
- Exercise 15 (PC Update): Pipeline control extends PC logic with stall/flush (Exercise 19)
- CSR/CLINT behavior: Interrupt handling must coordinate with pipeline flush logic

New pipeline-specific concepts:
- Pipeline registers with stall/flush capability (Exercise 20)
- Data hazard detection and forwarding (Exercises 17, 18, 19)
- Control hazard handling with reduced penalties (Exercise 19)
- Multi-stage coordination for trap handling

### Testing Guidance for Exercises

Using Tests to Verify Exercises:

Exercise 16 (ALU):
- Run PipelineProgramTest with fibonacci.asmbin
- Check that x1 contains correct Fibonacci result
- Run quicksort.asmbin to verify comparison operations (SLT, SLTU)

Exercise 17 (EX Stage Forwarding):
- Run PipelineProgramTest with hazard.asmbin
- Check hazardX1 values against PipelineConfigs.scala expectations
- ThreeStage: 26, FiveStageStall: 46, FiveStageForward: 27, FiveStageFinal: 26
- Lower values indicate better forwarding (fewer stalls)

Exercise 18 (ID Stage Forwarding):
- Run PipelineProgramTest with hazard.asmbin containing branches
- Verify branch decisions use correct forwarded operand values
- Check that branches resolve correctly with pending EX/MEM results

Exercise 19 (Hazard Detection):
- Run all PipelineProgramTest programs
- Verify no data corruption in register file
- Run irqtrap.asmbin to check interrupt coordination
- Check that mcause contains expected interrupt codes (0x80000007 or 0x8000000b)

Exercise 20 (Pipeline Registers):
- Run PipelineRegisterTest to validate stall/flush behavior
- Verify that flush sets instruction to NOP (0x00000013)
- Verify that flush sets address to EntryAddress
- Check stall freezes register values correctly

Debugging Strategies:

Waveform Analysis:
- Generate VCD with: make sim SIM_ARGS="-instruction src/main/resources/hazard.asmbin"
- Examine forwarding signals: reg1_forward_ex, reg2_forward_ex, reg1_forward_id, reg2_forward_id
- Check stall signals: if_stall, id_flush, pc_stall
- Check flush signals: if_flush, id_flush

Systematic Debugging:
- Start with simpler variants: Study FiveStageStall and FiveStageForward implementations
- Isolate specific hazards: Create minimal test sequences
  - EX load-use: LW x1, 0(x2); ADD x3, x1, x4
  - ID branch with forwarding: ADD x1, x2, x3; BEQ x1, x4, target
- Compare pipeline register values across cycles
- Verify forwarding priorities (MEM > WB)

Common Student Mistakes:

Forwarding Errors (Exercises 17, 18):
- Wrong priority: Choosing WB over MEM when both match
- Forgetting x0 filter: Attempting to forward from hardwired zero register
- Wrong field comparison: Comparing rd with wrong operand (rs1/rs2)
- Using wrong signals: Comparing against immediate or shifted values

Hazard Detection Errors (Exercise 19):
- Confusing stall and flush: Using flush when stall is needed, or vice versa
- Missing dependencies: Only checking rs1, forgetting rs2
- Incorrect flush scope: Flushing ID stage on branch in FiveStageFinal (should only flush IF)
- Missing jump dependencies: Not detecting JALR with pending load to jump register

Pipeline Register Errors (Exercise 20):
- Wrong default values: Not using InstructionsNop.nop or ProgramCounter.EntryAddress
- Incorrect wiring: Connecting wrong signals to stall or flush inputs
- Missing registers: Forgetting interrupt_flag pipeline register

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
