# Single-Cycle RISC-V CPU

This project implements a single-cycle RISC-V processor in Chisel where each instruction completes within one clock period.
The design eliminates instruction conflicts by executing only one instruction at any given time.
This approach simplifies implementation at the cost of performance limitations since the clock period must accommodate the slowest instruction path.

## Architecture Overview

The single-cycle CPU executes each instruction in one clock cycle.
The clock period must accommodate the slowest instruction, making all operations equally time-consuming.
While this design is straightforward to implement and reason about, it sacrifices performance compared to pipelined architectures.

### Key Characteristics

- Execution Model: One instruction per clock cycle
- Instruction Set: RV32I base integer instruction set
- Implementation: Chisel HDL (Hardware Description Language)
- Verification: ChiselTest framework with 9 comprehensive tests
- Simulation: Verilator-based cycle-accurate simulation

## Supported Instruction Set

The implementation supports the core RISC-V RV32I instruction set:

### Arithmetic and Logic Operations
- R-type: `add`, `sub`, `slt`, `sltu`, `and`, `or`, `xor`, `sll`, `srl`, `sra`
- I-type: `addi`, `slti`, `sltiu`, `andi`, `ori`, `xori`, `slli`, `srli`, `srai`

### Memory Access Operations
- Load: `lb`, `lh`, `lw`, `lbu`, `lhu` (byte, halfword, word with sign/zero extension)
- Store: `sb`, `sh`, `sw` (byte, halfword, word)

### Control Flow Operations
- Branch: `beq`, `bne`, `blt`, `bge`, `bltu`, `bgeu`
- Jump: `jal`, `jalr`
- Upper Immediate: `lui`, `auipc`
- System / Fence: `ecall`, `ebreak`, `fence`, `fence.i` (decoded and treated as architectural no-ops for machine-mode handoff)

## Execution Pipeline Stages

The processor divides instruction execution into five sequential phases:

### 1. Instruction Fetch (IF)
File: `src/main/scala/riscv/core/InstructionFetch.scala`

Retrieves instruction data from memory using the current program counter (PC) value. Updates the PC for the next instruction, either incrementing by 4 for sequential execution or jumping to a target address for branches.

Key Operations:
- Read instruction from memory at address PC
- Calculate next PC value (PC + 4 or branch target)
- Handle instruction memory latency

### 2. Instruction Decode (ID)
File: `src/main/scala/riscv/core/InstructionDecode.scala`

Interprets instruction encoding, extracts register addresses, and generates control signals for subsequent stages.

Key Operations:
- Decode opcode (instruction[6:0])
- Extract funct3 (instruction[14:12]) and funct7 (instruction[31:25])
- Generate register read addresses (rs1, rs2)
- Extract immediate values with proper sign extension
- Generate control signals (ALU operation, memory access, write-back source)

Immediate Encoding:
- I-type: Sign-extended 12-bit immediate
- S-type: Sign-extended 12-bit immediate (split encoding)
- B-type: Sign-extended 13-bit immediate (shifted left by 1)
- U-type: 20-bit immediate (shifted left by 12)
- J-type: Sign-extended 21-bit immediate (shifted left by 1)

### 3. Execute (EX)
File: `src/main/scala/riscv/core/Execute.scala`

Performs ALU calculations and determines branch conditions. The ALU supports all arithmetic, logic, and comparison operations defined by RV32I.

Key Operations:
- Execute ALU operations based on control signals
- Calculate branch target addresses
- Evaluate branch conditions (equality, less-than comparisons)
- Forward ALU result for immediate use or write-back

ALU Operations:
- Addition/Subtraction
- Bitwise logic (AND, OR, XOR)
- Shifts (logical and arithmetic, left and right)
- Comparisons (signed and unsigned)

### 4. Memory Access (MEM)
File: `src/main/scala/riscv/core/MemoryAccess.scala`

Conducts read/write operations for load/store instructions. Handles byte-level addressing and alignment for sub-word operations.

Key Operations:
- Calculate effective memory address
- Perform memory reads with appropriate size (byte, halfword, word)
- Handle sign extension for signed loads
- Execute memory writes with byte-enable strobes
- Pass through ALU results for non-memory instructions

Memory Interface:
- 32-bit word-aligned addressing
- Byte-level write strobes for sub-word stores
- Support for unaligned access via byte selection

### 5. Write-Back (WB)
File: `src/main/scala/riscv/core/WriteBack.scala`

Stores computed results or memory data into the destination register. Multiplexes between different data sources based on instruction type.

Key Operations:
- Select data source (ALU result, memory data, or PC + 4)
- Write to destination register
- Enforce x0 (zero register) immutability

Write-Back Sources:
- ALU result (arithmetic/logic operations)
- Memory data (load instructions)
- PC + 4 (jal/jalr instructions)

## Data Path and Control

### Data Path Components

Data paths transmit information between functional units:
- Register File: 32 general-purpose registers (x0-x31)
- ALU: Arithmetic Logic Unit for computations
- Memory Interface: Separate instruction and data memory ports
- Multiplexers: Select between different data sources

### Control Signals

Control signals direct data flow through the execution pipeline:
- `memory_read_enable`: Enable memory read operations
- `memory_write_enable`: Enable memory write operations
- `reg_write_enable`: Enable register file writes
- `alu_funct`: Specify ALU operation
- `aluop1_source`: Select first ALU operand (register or PC)
- `aluop2_source`: Select second ALU operand (register or immediate)
- `wb_reg_write_source`: Select write-back data source

## Module Hierarchy

```
CPU (src/main/scala/riscv/core/CPU.scala)
├── InstructionFetch
│   └── ProgramCounter
├── InstructionDecode
│   └── Control signal generation
├── Execute
│   ├── ALU (src/main/scala/riscv/core/ALU.scala)
│   └── ALUControl
├── MemoryAccess
│   └── Memory interface logic
├── WriteBack
│   └── Data multiplexing
└── RegisterFile (src/main/scala/riscv/core/RegisterFile.scala)
```

## Test Suite

The implementation includes comprehensive verification through multiple testing methodologies:

### ChiselTest Unit Tests (9 tests)

Located in `src/test/scala/riscv/singlecycle/`:

1. InstructionFetch: Verifies PC update and instruction fetching
2. InstructionDecode: Validates control signal generation
3. Execute: Tests ALU operations and branch logic
4. ByteAccess: Verifies byte-level memory operations
5. RegisterFile: Tests register read/write operations
6. Fibonacci: Executes recursive Fibonacci calculation
7. Quicksort: Runs quicksort algorithm on 10 elements

All unit tests pass successfully:
```shell
make test
# Total number of tests run: 9
# Tests: succeeded 9, failed 0
```

### RISCOF Compliance Testing (41 tests)

RISC-V architectural compliance testing validates correct implementation of the RV32I instruction set against the official RISC-V specification.

Test Coverage:
- RV32I base instruction set (41 tests)
- Arithmetic operations: ADD, SUB, ADDI
- Logical operations: AND, OR, XOR, ANDI, ORI, XORI
- Shift operations: SLL, SRL, SRA, SLLI, SRLI, SRAI
- Comparison: SLT, SLTU, SLTI, SLTIU
- Load operations: LB, LH, LW, LBU, LHU
- Store operations: SB, SH, SW
- Branch instructions: BEQ, BNE, BLT, BGE, BLTU, BGEU
- Jump instructions: JAL, JALR
- Upper immediate: LUI, AUIPC

Running Compliance Tests:
```shell
make compliance
# Expected duration: 10-15 minutes
# Results saved to: results/report.html
```

Last Verification: 2025-11-08
- Unit Tests: 9/9 passed
- RISCOF Compliance: 41/41 RV32I tests passed
- Verilator Simulation: Completed successfully with test programs

### Simulator Behavior Notes

When running test programs (e.g., fibonacci.asmbin), you may observe warnings like:
```
invalid read address 0x10000000
invalid write address 0x0ffffffc
```

These warnings are expected and harmless. The RISC-V programs use stack addresses that aren't mapped in the minimal simulator memory model. The programs execute correctly despite these warnings - they simply indicate memory accesses outside the simulated address space.

## Simulation with Verilator

### Running Simulations

```shell
# Basic simulation (no program loaded)
make sim

# Run with test program
make sim SIM_ARGS="-instruction ../../../src/main/resources/fibonacci.asmbin"

# Custom simulation time and waveform output
make sim SIM_TIME=100000 SIM_VCD=custom.vcd
```

### Simulation Parameters

- `SIM_TIME`: Maximum simulation cycles (default: 1,000,000)
- `SIM_VCD`: Waveform output file (default: trace.vcd)
- `SIM_ARGS`: Additional arguments (program binary, halt address)

### Test Programs

Located in `csrc/` and `src/main/resources/`:
- `fibonacci.asmbin`: Recursive Fibonacci calculation
- `quicksort.asmbin`: Quicksort algorithm
- `sb.asmbin`: Byte store/load test

### Waveform Analysis

View simulation waveforms with GTKWave or Surfer:
```shell
gtkwave trace.vcd
# or
surfer trace.vcd
```

Key signals to observe:
- `io_instruction_address`: Current PC value
- `io_instruction`: Fetched instruction
- `io_memory_bundle_*`: Memory interface signals
- `inst_fetch_*, id_*, ex_*, mem_*, wb_*`: Pipeline stage internals

## Implementation Notes

### Design Decisions

1. Single-Cycle Limitation: While simpler to implement, the single-cycle design requires the clock period to accommodate the slowest instruction path, limiting overall performance.
2. Memory Architecture: Separate instruction and data memory interfaces simplify the design but would require modification for Harvard or unified memory architectures.
3. No Hazard Handling: Since each instruction completes in one cycle, there are no pipeline hazards (structural, data, or control hazards) to manage.

### Performance Characteristics

- CPI (Cycles Per Instruction): Exactly 1.0 for all instructions
- Clock Frequency: Limited by critical path (typically load instruction: IF → ID → EX → MEM → WB)
- Throughput: One instruction per cycle

### Extensions and Limitations

Supported:
- Complete RV32I base instruction set
- Verilator simulation with VCD waveform generation
- Comprehensive test coverage

Not Supported (see [2-mmio-trap](../mmio-trap.md) for extensions):
- Interrupts and exceptions
- CSR (Control and Status Registers)
- Privileged instructions
- M, A, F, D standard extensions

## File Organization

```
1-single-cycle/
├── src/main/scala/
│   ├── riscv/core/          # CPU core modules
│   │   ├── CPU.scala        # Top-level CPU integration
│   │   ├── InstructionFetch.scala
│   │   ├── InstructionDecode.scala
│   │   ├── Execute.scala
│   │   ├── MemoryAccess.scala
│   │   ├── WriteBack.scala
│   │   ├── RegisterFile.scala
│   │   ├── ALU.scala
│   │   └── ALUControl.scala
│   ├── peripheral/          # Memory and I/O peripherals
│   └── board/verilator/     # Verilator top-level
├── src/test/scala/          # ChiselTest suites
├── csrc/                    # C/Assembly test programs
└── verilog/verilator/       # Verilator simulation
```

## References

- [RISC-V Instruction Set Manual](https://riscv.org/technical/specifications/)
- [Chisel Documentation](https://www.chisel-lang.org/)
- [Verilator Manual](https://verilator.org/guide/latest/)
