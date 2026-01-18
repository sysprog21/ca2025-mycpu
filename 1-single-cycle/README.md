# Single-Cycle RISC-V CPU

Single-cycle RISC-V processor where each instruction completes in one clock period.
Each instruction executes atomically without conflicts from other instructions.
Clock period accommodates the slowest instruction path, simplifying design at performance cost.

## Architecture Overview

Single-cycle CPU executes one complete instruction per clock cycle.
All five pipeline stages (IF, ID, EX, MEM, WB) operate combinationally within one cycle.
Clock frequency limited by critical path through longest instruction (typically load).
Straightforward to implement and reason about, but sacrifices throughput compared to pipelined designs.

### Key Characteristics

- Execution Model: One instruction per clock cycle
- Instruction Set: RV32I base integer instruction set (47 instructions)
- Implementation: Chisel HDL (Hardware Description Language)
- Verification: ChiselTest framework with 9 unit tests
- Compliance: 41 RISCOF architectural tests
- Simulation: Verilator-based cycle-accurate simulation with VCD waveform generation

## Supported Instruction Set

Complete RV32I base integer instruction set implementation.

### Arithmetic and Logic Operations

R-type register operations (10 instructions):
`add`, `sub`, `slt`, `sltu`, `and`, `or`, `xor`, `sll`, `srl`, `sra`

I-type immediate operations (9 instructions):
`addi`, `slti`, `sltiu`, `andi`, `ori`, `xori`, `slli`, `srli`, `srai`

### Memory Access Operations

Load instructions with sign/zero extension (5 instructions):
`lb`, `lh`, `lw`, `lbu`, `lhu`

Store instructions with byte-level strobes (3 instructions):
`sb`, `sh`, `sw`

### Control Flow Operations

Branch instructions with signed/unsigned comparisons (6 instructions):
`beq`, `bne`, `blt`, `bge`, `bltu`, `bgeu`

Jump instructions for function calls and computed jumps (2 instructions):
`jal`, `jalr`

Upper immediate instructions for large constant loading (2 instructions):
`lui`, `auipc`

System and fence instructions (4 instructions):
`ecall`, `ebreak`, `fence`, `fence.i`
Decoded and treated as architectural no-ops for machine-mode handoff.

## Execution Pipeline Stages

Five sequential stages divide instruction execution into discrete phases.
All stages execute combinationally within a single clock cycle.

### 1. Instruction Fetch (IF)
> File: `src/main/scala/riscv/core/InstructionFetch.scala`

Retrieves instruction from memory using current PC value.
Updates PC for next instruction: sequential (PC+4) or branch target.
Handles instruction memory latency with valid signal gating.

Key Operations:
- Read 32-bit instruction from memory at address PC
- Calculate next PC value (PC + 4 or jump address)
- Gate PC update with instruction_valid signal
- Propagate PC value to subsequent stages

### 2. Instruction Decode (ID)
> File: `src/main/scala/riscv/core/InstructionDecode.scala`

Interprets instruction encoding and generates control signals.
Extracts register addresses and immediate values per RISC-V encoding.
Determines datapath routing for ALU operands and write-back source.

Key Operations:
- Decode 7-bit opcode (instruction[6:0])
- Extract funct3 (instruction[14:12]) and funct7 (instruction[31:25])
- Generate register read addresses (rs1, rs2, rd)
- Extract and sign-extend immediate values per instruction type
- Generate control signals (ALU op, memory access, write-back source)

Immediate Encoding Formats:
- I-type: Sign-extended 12-bit immediate for arithmetic and loads
- S-type: Sign-extended 12-bit immediate (split encoding) for stores
- B-type: Sign-extended 13-bit immediate (LSB=0) for branches
- U-type: 20-bit immediate (shifted left 12) for upper immediate loads
- J-type: Sign-extended 21-bit immediate (LSB=0) for JAL

### 3. Execute (EX)
> File: `src/main/scala/riscv/core/Execute.scala`

Performs ALU calculations and evaluates branch conditions.
Computes target addresses for jumps and branches.
Determines whether control flow changes occur.

Key Operations:
- Execute ALU operations (arithmetic, logic, shifts, comparisons)
- Calculate branch target addresses (PC + immediate)
- Calculate JAL target (PC + immediate)
- Calculate JALR target ((rs1 + immediate) & ~1, LSB cleared per spec)
- Evaluate branch conditions (equality, signed/unsigned less-than)
- Forward ALU result to memory stage or write-back

ALU Operations (11 functions):
- Addition and subtraction with 32-bit wrap-around
- Bitwise logic (AND, OR, XOR)
- Shifts: logical left, logical right, arithmetic right
- Comparisons: signed less-than, unsigned less-than

### 4. Memory Access (MEM)
> File: `src/main/scala/riscv/core/MemoryAccess.scala`

Executes load/store operations with byte-level addressing.
Handles sub-word operations (byte, halfword) with proper alignment.
Extends loaded data with sign or zero extension per instruction type.

Key Operations:
- Calculate effective memory address from ALU result
- Perform memory reads: LB, LH, LW, LBU, LHU
- Select byte/halfword from word-aligned memory
- Apply sign extension (LB, LH) or zero extension (LBU, LHU)
- Execute memory writes: SB, SH, SW
- Generate byte-enable strobes for sub-word stores
- Align write data to appropriate byte lanes
- Pass through ALU results for non-memory instructions

Memory Interface:
- 32-bit word-aligned addressing with byte selection
- Byte-level write strobes (4 bits) for sub-word stores
- Address bits [1:0] determine byte position within word
- Support for unaligned access via byte/halfword extraction

### 5. Write-Back (WB)
> File: `src/main/scala/riscv/core/WriteBack.scala`

Selects final result to write to register file.
Multiplexes between ALU result, memory data, and PC+4.
Enforces x0 (zero register) immutability per RISC-V specification.

Key Operations:
- Select data source via 3-way multiplexer
- Write selected data to destination register
- Enforce x0 hardwired to zero (writes ignored)

Write-Back Source Selection:
- ALU result: Arithmetic, logic, shift, comparison operations
- Memory data: Load instructions (LB, LH, LW, LBU, LHU)
- PC + 4: Jump and link instructions (JAL, JALR) for return address

## CA25: Lab Exercises

Nine lab exercises marked with `CA25: Exercise` comments throughout the codebase.
Students complete these exercises to build a fully functional RV32I processor.
Exercises progressively build understanding of each pipeline stage.
Each exercise validated by corresponding unit tests or end-to-end programs.

### Exercise Overview

Exercise 1: Immediate Extension (`InstructionDecode.scala`)
- Task: Implement S-type, B-type, and J-type immediate extraction
- Difficulty: Intermediate
- Key Concepts: RISC-V instruction encoding, bit manipulation, sign extension
- Validation: InstructionDecoderTest
- Bitfield Formats:
  - S-type: `{inst[31:25], inst[11:7]}` concatenated, sign-extended to 32 bits
  - B-type: `{inst[31], inst[7], inst[30:25], inst[11:8], 0}` reordered, sign-extended, LSB=0
  - J-type: `{inst[31], inst[19:12], inst[20], inst[30:21], 0}` reordered, sign-extended, LSB=0

Exercise 2: Control Signal Generation (`InstructionDecode.scala`)
- Task: Generate write-back source and ALU operand routing signals
- Difficulty: Beginner to Intermediate
- Key Concepts: Control signal multiplexing, datapath control
- Validation: InstructionDecoderTest
- Control Signals: Write-back source (ALU/Memory/PC+4), ALU op1 (PC/rs1), ALU op2 (imm/rs2)

Exercise 3: ALU Control Decode (`ALUControl.scala`)
- Task: Map opcode, funct3, funct7 fields to ALU operation codes
- Difficulty: Intermediate
- Key Concepts: Instruction decoding, ALU function selection, funct7[5] disambiguation
- Validation: ExecuteTest
- Critical Pattern: `funct7(5) == 1.U` selects SUB/SRA; `funct7(5) == 0.U` selects ADD/SRL

Exercise 4: Branch Comparison Logic (`Execute.scala`)
- Task: Implement six RV32I branch conditions
- Difficulty: Beginner to Intermediate
- Key Concepts: Signed comparison, unsigned comparison, equality checking
- Validation: ExecuteTest
- Branch Types: BEQ, BNE (equality), BLT, BGE (signed), BLTU, BGEU (unsigned)

Exercise 5: Jump Target Address Calculation (`Execute.scala`)
- Task: Compute target addresses for branches, JAL, and JALR
- Difficulty: Beginner
- Key Concepts: PC-relative addressing, JALR LSB clearing
- Validation: ExecuteTest, end-to-end programs
- JALR Requirement: `target = (rs1 + imm) & ~1` per RISC-V specification

Exercise 6: Load Data Extension (`MemoryAccess.scala`)
- Task: Implement byte/halfword sign and zero extension
- Difficulty: Beginner
- Key Concepts: Sign extension, zero extension, byte/halfword selection
- Validation: ByteAccessTest (in CPUTest)
- Extension Logic: LB/LH sign-extend from bit 7/15; LBU/LHU zero-extend

Exercise 7: Store Data Alignment (`MemoryAccess.scala`)
- Task: Generate byte strobes and align write data for stores
- Difficulty: Intermediate
- Key Concepts: Byte-level memory access, write strobes, data alignment
- Validation: ByteAccessTest (in CPUTest)
- Strobe Mapping:
  - SB: One byte strobe active, determined by address[1:0], data shifted 8*index bits
  - SH: Two byte strobes active, determined by address[1], data shifted 0 or 16 bits
  - SW: All four byte strobes active, no shift required

Exercise 8: Write-Back Multiplexer (`WriteBack.scala`)
- Task: Select final write-back data source among three options
- Difficulty: Beginner
- Key Concepts: Multiplexer design, data source selection
- Validation: CPUTest end-to-end programs (fibonacci, quicksort)
- Sources: ALU result (default), memory read data (loads), PC+4 (JAL/JALR)

Exercise 9: PC Update Logic (`InstructionFetch.scala`)
- Task: Implement PC update for sequential and control-flow instructions
- Difficulty: Beginner
- Key Concepts: Program counter management, control flow
- Validation: InstructionFetchTest
- Logic: PC = jump_address when jump_flag asserted, else PC+4, gated by instruction_valid

### Exercise Workflow

Recommended implementation sequence follows datapath stages for systematic learning.

Phase 1: Instruction Decode (Exercises 1–2)
1. Implement S/B/J immediate extraction with proper bit reordering and sign extension
2. Implement control signal generation based on opcode decoding
3. Validate with InstructionDecoderTest
4. Command: `sbt "project singleCycle" "testOnly *InstructionDecoderTest"`

Phase 2: ALU Control (Exercise 3)
1. Implement ALU control logic with funct3/funct7 decoding
2. Handle funct7[5] disambiguation for SUB/SRA vs ADD/SRL
3. Validate with ExecuteTest
4. Command: `sbt "project singleCycle" "testOnly *ExecuteTest"`

Phase 3: Execution Stage (Exercises 4–5)
1. Implement branch comparison logic for all six branch types
2. Implement jump target address calculation with JALR LSB clearing
3. Revalidate with ExecuteTest
4. Command: `sbt "project singleCycle" "testOnly *ExecuteTest"`

Phase 4: Memory Access (Exercises 6–7)
1. Implement load data extension with sign/zero extension logic
2. Implement store data alignment with byte strobe generation
3. Validate with ByteAccessTest and CPUTest
4. Command: `sbt "project singleCycle" test`

Phase 5: Write-Back and Fetch (Exercises 8–9)
1. Implement write-back multiplexer with three source selection
2. Implement PC update logic with jump and sequential paths
3. Full validation with all 9 unit tests
4. Architectural validation with RISCOF compliance suite
5. Commands:
   - `sbt "project singleCycle" test`
   - `make compliance` (10-15 minutes, 41 RV32I tests)

### Debugging Tips

#### VCD Waveform Analysis for Exercise Debugging

Generate VCD waveforms with `WRITE_VCD=1 make sim`.
Monitor key signals organized by pipeline stage and exercise area.
Use GTKWave or Surfer for waveform visualization.

Instruction Fetch (IF) - Exercise 9:
- `pc`: Current program counter value
- `jump_flag_id`: Control flow change indicator from Execute stage
- `jump_address_id`: Target address for jumps and branches
- `instruction_valid`: Valid instruction indicator, gates PC updates

Instruction Decode (ID) - Exercises 1–2:
- `opcode`: 7-bit instruction opcode field (bits [6:0])
- `funct3`, `funct7`: Function field selectors for operation disambiguation
- `immKind`: Immediate type indicator (I/S/B/U/J/None enumeration)
- `immediate`: Extracted and sign-extended immediate value (32 bits)
- `wb_reg_write_source`: Write-back source control (ALU/Memory/PC+4)
- `ex_aluop1_source`: ALU operand 1 source selection (PC vs rs1)
- `ex_aluop2_source`: ALU operand 2 source selection (immediate vs rs2)

Execute (EX) - Exercises 3–5:
- `alu.func`: Selected ALU operation from ALUControl module
- `alu.op1`, `alu.op2`: ALU input operands (32 bits each)
- `alu.result`: ALU computation result (32 bits)
- `branchCondition`: Branch decision (taken/not taken boolean)
- `if_jump_flag`: Jump/branch flag propagated to IF stage
- `if_jump_address`: Computed target address for control flow changes

Memory Access (MEM) - Exercises 6–7:
- `mem_address_index`: Byte position within word (address bits [1:0])
- `write_strobe`: Byte lane enables for store operations (4 bits)
- `wb_memory_read_data`: Loaded data after sign/zero extension (32 bits)

Write-Back (WB) - Exercise 8:
- `regs_write_source`: Final multiplexer control signal
- `regs_write_data`: Value written to register file (32 bits)

#### Common Student Pitfalls

Immediate Extension (Exercise 1):
- Incorrect bit slice ordering for B-type and J-type immediates
- Forgetting LSB=0 requirement for branch and jump immediates
- Using wrong bit as sign bit for sign extension
- Concatenating bits in wrong order per RISC-V encoding
- Detection: InstructionDecoderTest failures, incorrect branch/jump targets in VCD

ALU Control (Exercise 3):
- Confusing funct3 with funct7 in shift operation decoding
- Not checking funct7[5] bit for SUB/SRA disambiguation
- Incorrect default ALU operation (using zero instead of add)
- Missing OpImm vs Op opcode distinction
- Detection: ExecuteTest failures, arithmetic/shift operation errors

Branch Comparison (Exercise 4):
- Mixing signed and unsigned comparison logic (casting errors)
- Incorrect BNE/BEQ equality condition implementation
- Wrong comparison operator for BLT/BGE vs BLTU/BGEU
- Detection: ExecuteTest branch condition failures, wrong control flow in programs

JALR Target (Exercise 5):
- Forgetting LSB clearing operation (& ~1)
- Using PC as base instead of rs1 register value
- Incorrect immediate addition before LSB clearing
- Detection: Function return errors, indirect jump failures in fibonacci/quicksort

Load Extension (Exercise 6):
- Incorrect sign bit selection (byte(7) for LB, half(15) for LH)
- Mixing sign extension and zero extension logic
- Wrong byte/halfword extraction based on address bits
- Not handling all five load variants (LB, LH, LW, LBU, LHU)
- Detection: ByteAccessTest failures, negative number handling errors

Store Alignment (Exercise 7):
- Incorrect byte strobe generation for SB and SH
- Wrong shift amounts for byte/halfword data positioning
- Not considering address[1:0] for byte lane selection
- Detection: ByteAccessTest failures, memory corruption in end-to-end tests

PC Update (Exercise 9):
- Not gating PC update with instruction_valid signal
- Incorrect multiplexer logic for jump vs sequential execution
- Forgetting to output NOP when instruction invalid
- Detection: InstructionFetchTest failures, incorrect program execution flow

## Data Path and Control

### Data Path Components

Data paths transmit operands and results between functional units.
Register file provides two read ports and one write port for simultaneous access.
Memory interface separates instruction fetch from data access.

Components:
- Register File: 32 general-purpose registers (x0-x31), x0 hardwired to zero
- ALU: Arithmetic Logic Unit performing 11 operations
- Memory Interface: Separate instruction and data memory ports
- Multiplexers: Select between PC/register operands, immediate/register operands, write-back sources
- Program Counter: Maintains current instruction address

### Control Signals

Control signals route data through execution pipeline.
Generated by decode stage based on instruction opcode and function fields.
Direct multiplexer selection and enable signals.

Control Signal Definitions:
- `memory_read_enable`: Enable memory read operations (load instructions)
- `memory_write_enable`: Enable memory write operations (store instructions)
- `reg_write_enable`: Enable register file writes (most instructions except branches, stores)
- `alu_funct`: Specify ALU operation (11 function codes)
- `aluop1_source`: Select first ALU operand (register rs1 or PC)
- `aluop2_source`: Select second ALU operand (register rs2 or immediate)
- `wb_reg_write_source`: Select write-back data source (ALU/Memory/PC+4)

## Module Hierarchy

```
CPU (src/main/scala/riscv/core/CPU.scala)
├── InstructionFetch
│   └── ProgramCounter (PC register and update logic)
├── InstructionDecode
│   └── Control signal generation (opcode/funct3/funct7 decode)
├── Execute
│   ├── ALU (common/src/main/scala/riscv/core/ALU.scala)
│   └── ALUControl (maps instruction fields to ALU operations)
├── MemoryAccess
│   └── Memory interface logic (byte/halfword/word access)
├── WriteBack
│   └── Data multiplexing (3-way mux for write-back source)
└── RegisterFile (common/src/main/scala/riscv/core/RegisterFile.scala)
```

## Test Suite

Comprehensive verification through unit tests and architectural compliance tests.
Unit tests validate individual stages and integration behavior.
RISCOF compliance tests verify adherence to RISC-V specification.

### ChiselTest Unit Tests (9 tests)

Located in `src/test/scala/riscv/singlecycle/`.
Test individual modules and end-to-end program execution.
All tests must pass before RISCOF compliance validation.

Test Coverage:
1. InstructionFetch: PC update logic, sequential and jump execution
2. InstructionDecode: Control signal generation for all instruction types
3. Execute: ALU operations, branch conditions, target calculation
4. ByteAccess: Byte-level load/store operations with proper extension
5. RegisterFile: Register read/write operations, x0 immutability
6. Fibonacci: Recursive function calls, stack operations, JALR
7. Quicksort: Array manipulation, complex control flow

Run all unit tests:
```shell
make test
# Expected output:
# Total number of tests run: 9
# Tests: succeeded 9, failed 0
```

### RISCOF Compliance Testing (41 tests)

RISC-V architectural compliance framework validates RV32I implementation.
Tests compare CPU behavior against reference model (rv32emu).
Verifies instruction semantics, corner cases, and edge conditions.

Test Coverage by Category:
- Arithmetic: ADD, SUB, ADDI (register overflow, immediate bounds)
- Logical: AND, OR, XOR, ANDI, ORI, XORI (bitwise operations)
- Shift: SLL, SRL, SRA, SLLI, SRLI, SRAI (shift amounts 0-31, logical vs arithmetic)
- Comparison: SLT, SLTU, SLTI, SLTIU (signed vs unsigned, boundary values)
- Load: LB, LH, LW, LBU, LHU (sign/zero extension, alignment)
- Store: SB, SH, SW (byte strobes, alignment)
- Branch: BEQ, BNE, BLT, BGE, BLTU, BGEU (taken/not taken, forward/backward)
- Jump: JAL, JALR (return address, LSB clearing)
- Upper Immediate: LUI, AUIPC (large constants, PC-relative addressing)

Run compliance tests:
```shell
make compliance
# Expected duration: 10-15 minutes
# Results: results/report.html (HTML test report)
# Signature files: results/mycpu/*.signature.output
```

Last Verification: 2025-11-08
- Unit Tests: 9/9 passed
- RISCOF Compliance: 41/41 RV32I tests passed
- Verilator Simulation: fibonacci.asmbin, quicksort.asmbin executed successfully

### Simulator Behavior Notes

Verilator simulation may generate memory access warnings during execution.
Warnings indicate accesses outside simulated memory range.
Programs execute correctly despite warnings.

Example warnings (expected and harmless):
```
invalid read address 0x10000000
invalid write address 0x0ffffffc
```

These occur when programs use stack addresses beyond simulated memory model.
Minimal simulator implements limited address space for educational purposes.
Production implementations would handle full 32-bit address space.

## Simulation with Verilator

### Running Simulations

Verilator converts Chisel HDL to cycle-accurate C++ simulator.
Generates VCD waveforms for debugging and verification.
Supports custom test programs compiled with RISC-V toolchain.

Basic simulation commands:
```shell
# Generate Verilog and build Verilator simulator
make verilator

# Run simulation with default program (fibonacci.asmbin)
make sim

# Run with specific test program
make sim SIM_ARGS="-instruction src/main/resources/quicksort.asmbin"

# Custom simulation duration and waveform file
make sim SIM_TIME=100000 SIM_VCD=custom.vcd
```

### Simulation Parameters

Configuration options for Verilator simulation:
- `SIM_TIME`: Maximum simulation cycles, default 1,000,000
- `SIM_VCD`: Waveform output filename, default trace.vcd
- `SIM_ARGS`: Additional arguments passed to simulator executable
- `WRITE_VCD`: Set to 1 to enable VCD waveform generation

### Test Programs

Located in `src/main/resources/` directory.
Compiled from assembly or C source in `csrc/` directory.
Binary format: flat 32-bit instruction stream.

Available programs:
- `fibonacci.asmbin`: Recursive Fibonacci calculation, tests function calls and stack
- `quicksort.asmbin`: Array sorting with complex control flow
- `sb.asmbin`: Byte store/load operations test

### Waveform Analysis

View VCD waveforms with open-source or commercial tools.
Inspect signal values, timing relationships, and control flow.

Waveform viewers:
```shell
# GTKWave (cross-platform, open-source)
gtkwave trace.vcd

# Surfer (modern Rust-based viewer)
surfer trace.vcd
```

Key signals for debugging:
- `io_instruction_address`: Current PC value (instruction fetch address)
- `io_instruction`: Fetched 32-bit instruction
- `io_memory_bundle_address`: Memory access address (loads/stores)
- `io_memory_bundle_write_data`: Data written to memory
- `io_memory_bundle_read_data`: Data read from memory
- `inst_fetch_*`: Instruction fetch stage internal signals
- `id_*`: Instruction decode stage internal signals
- `ex_*`: Execute stage internal signals
- `mem_*`: Memory access stage internal signals
- `wb_*`: Write-back stage internal signals

## Implementation Notes

### Design Decisions

Single-Cycle Architecture Limitations:
Clock period determined by longest instruction path (load instruction).
All instructions take same time regardless of complexity.
Simpler to implement and debug compared to pipelined designs.
Limited performance: cannot overlap instruction execution.

Memory Architecture:
Separate instruction and data memory interfaces simplify timing.
Instruction memory read-only during execution.
Data memory supports byte-level write strobes for sub-word stores.
Would require modification for unified memory or cache hierarchies.

No Hazard Handling:
Single-cycle execution eliminates pipeline hazards.
No need for forwarding, stalling, or branch prediction.
Each instruction completes before next begins.
Simplifies control logic at cost of throughput.

### Performance Characteristics

Performance metrics for single-cycle implementation:
- CPI (Cycles Per Instruction): Exactly 1.0 for all instructions
- IPC (Instructions Per Cycle): Exactly 1.0, no instruction overlap
- Clock Frequency: Limited by critical path (IF → ID → EX → MEM → WB)
- Critical Path: Typically load instruction with memory read and extension
- Throughput: One instruction per cycle, no pipelining benefits

### Extensions and Limitations

Supported features:
- Complete RV32I base instruction set (47 instructions)
- Verilator simulation with VCD waveform generation
- Comprehensive unit test coverage (9 tests)
- RISCOF architectural compliance (41 tests)
- Byte-level memory access with proper alignment

Not supported (see `2-mmio-trap/` for extensions):
- Interrupts and exceptions (CLINT, trap handling)
- CSR (Control and Status Registers)
- Privileged instructions (machine mode, supervisor mode)
- M extension (multiply/divide)
- A extension (atomic operations)
- F/D extensions (floating-point)
- C extension (compressed instructions)

## File Organization

```
1-single-cycle/
├── src/main/scala/
│   ├── riscv/
│   │   ├── Parameters.scala         # CPU configuration parameters
│   │   ├── CPUBundle.scala          # Top-level I/O bundle definition
│   │   └── core/
│   │       ├── CPU.scala            # Top-level CPU integration
│   │       ├── InstructionFetch.scala
│   │       ├── InstructionDecode.scala
│   │       ├── Execute.scala
│   │       ├── MemoryAccess.scala
│   │       ├── WriteBack.scala
│   │       └── ALUControl.scala
│   ├── peripheral/
│   │   ├── Memory.scala             # Data memory with byte strobes
│   │   ├── InstructionROM.scala     # Instruction memory loader
│   │   └── ROMLoader.scala          # Binary file to memory converter
│   └── board/verilator/
│       └── Top.scala                # Verilator simulation top-level
├── src/test/scala/riscv/singlecycle/
│   ├── InstructionFetchTest.scala   # IF stage tests
│   ├── InstructionDecoderTest.scala # ID stage tests
│   ├── ExecuteTest.scala            # EX stage tests
│   ├── RegisterFileTest.scala       # Register file tests
│   └── CPUTest.scala                # End-to-end tests
├── src/main/resources/
│   ├── fibonacci.asmbin             # Fibonacci test program
│   ├── quicksort.asmbin             # Quicksort test program
│   └── sb.asmbin                    # Byte access test program
├── csrc/
│   ├── fibonacci.S                  # Fibonacci assembly source
│   ├── quicksort.c                  # Quicksort C source
│   ├── link.lds                     # Linker script
│   └── Makefile                     # Test program build system
├── verilog/verilator/
│   ├── Top.v                        # Generated Verilog (via make verilator)
│   ├── sim_main.cpp                 # Verilator C++ testbench
│   └── Makefile                     # Verilator build system
├── tests/                           # RISCOF compliance test infrastructure
├── Makefile                         # Top-level build automation
└── README.md                        # This file
```

## References

Technical documentation and learning resources:

- [RISC-V Instruction Set Manual](https://riscv.org/technical/specifications/) - Official ISA specification
- [RISC-V Unprivileged Specification](https://github.com/riscv/riscv-isa-manual/releases) - RV32I instruction encoding
- [Chisel Documentation](https://www.chisel-lang.org/) - Chisel HDL tutorials and API reference
- [ChiselTest Guide](https://github.com/ucb-bar/chiseltest) - Testing framework documentation
- [Verilator Manual](https://verilator.org/guide/latest/) - Simulation and optimization guide
- [RISCOF Documentation](https://riscof.readthedocs.io/) - Compliance testing framework
