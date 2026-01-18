# 0-minimal: Minimal RISC-V CPU

## Overview

This project implements a minimal single-cycle RISC-V CPU designed specifically to execute `jit.asmbin`,
a self-modifying code demonstration showcasing [JIT compilation](https://en.wikipedia.org/wiki/Just-in-time_compilation) concepts.
The implementation includes only the 5 instructions required by this program, making it an excellent educational example of focused processor design.
This approach demonstrates how processors can be optimized for specific workload requirements rather than implementing complete instruction sets.

## Supported Instructions

The CPU supports exactly these RISC-V instructions:
1. AUIPC (Add Upper Immediate to PC) - PC-relative addressing for data section access
2. ADDI (Add Immediate) - arithmetic operations and register initialization
3. LW (Load Word) - word-aligned memory reads without byte/halfword support
4. SW (Store Word) - word-aligned memory writes with full 32-bit strobes
5. JALR (Jump and Link Register) - function calls, returns, and dynamic jump targets

ECALL is not required for this minimal CPU.
The test infrastructure reads register values directly through a debug interface,
eliminating the need for system call support or host communication mechanisms.

## Architecture

This is a single-cycle processor where each instruction completes in one clock cycle.
The design organizes the datapath into five combinational stages that all execute within the same cycle:

```
┌──────────┐    ┌─────────┐    ┌─────────┐    ┌────────┐    ┌───────────┐
│    IF    │───▶│   ID    │───▶│   EX    │───▶│  MEM   │───▶│    WB     │
│  Fetch   │    │ Decode  │    │ Execute │    │ Access │    │ WriteBack │
└──────────┘    └─────────┘    └─────────┘    └────────┘    └───────────┘
     │               │               │             │              │
     └───── PC ──────┴──── RegFile ──┴──── ALU ────┴──── Memory ──┘
        (updates at cycle end)     (reads at cycle start, writes at end)
```

Unlike a pipelined processor where each stage spans one cycle and multiple instructions overlap, this single-cycle design completes all five stages combinationally before the next clock edge.
The clock period must accommodate the longest instruction path (typically load: IF → ID → EX → MEM → WB).

Key architectural features:
- Addition-only ALU eliminates all subtraction, logic, and shift circuits
- JALR path clears LSB (`target & ~1`) for proper alignment per RISC-V specification
- WriteBack multiplexer selects between ALU result, memory data, or PC+4 based on instruction type
- Harvard-style memory access, with separate instruction and data memories simulated. For JIT support, writes are synchronized to both memories.
- Register file reads occur combinationally; writes happen at clock edge

## Design Highlights

### Ultra-Minimal ALU
The ALU supports only addition operations (op1 + op2), eliminating all subtraction, logic, and shift circuits.
This single operation serves ADDI arithmetic, AUIPC PC-relative addressing, LW/SW address calculation, and JALR target computation.
The simplified design reduces hardware complexity while supporting all required instruction semantics.

### Simplified Memory Access
Memory operations support word-aligned access only, eliminating byte/halfword extraction and alignment logic.
Load Word (LW) reads 32-bit values directly: `data ← mem[addr]`.
Store Word (SW) writes with all byte strobes enabled: `mem[addr] ← data`.

The memory subsystem uses SyncReadMem (synchronous block RAM) with one-cycle read latency.
The test infrastructure divides the CPU clock by 4 to accommodate this latency, allowing signals to stabilize across multiple fast memory cycles before each CPU cycle.
This approach simplifies the educational design while maintaining functional correctness for the JIT test program.
Real hardware implementations would typically use separate instruction and data memories (Harvard architecture) to avoid multi-ported memory requirements.

### Minimal Decode Logic
The instruction decoder handles only 5 opcodes (Load, OpImm, Store, Auipc, Jalr), significantly reducing control complexity.
Immediate extraction supports three encoding formats: I-type (12-bit sign-extended), S-type (12-bit sign-extended split), and U-type (20-bit upper immediate).
The design eliminates funct3/funct7 field decoding since all five operations are uniquely determined by opcode alone.
B-type (branch) and J-type (jump) encodings are not required for this minimal instruction subset.

### Harvard Architecture Memory
- Separate instruction and data memories for FPGA block RAM synthesis
- imem: Instruction memory (1 read port for instruction fetch)
- dmem: Data memory (1 read + 1 write port for load/store)
- JIT Support: Synchronized writes to both memories enable self-modifying code
- FPGA-Ready: Maps cleanly to dual-port block RAM (no multi-port conflicts)
- Trade-off: 2× memory capacity (imem + dmem) vs. synthesizable design

## Test Program: jit.asmbin

### What jit.asmbin Does

The `jit.asmbin` binary demonstrates RISC-V self-modifying code and JIT (Just-In-Time) compilation concepts through a four-stage process:

1. Instructions as Data: The program stores compiled instructions as hex data in memory, simulating JIT compiler output
2. Runtime Code Generation: The CPU copies these instructions to an executable code buffer at runtime
3. Dynamic Execution: Control flow jumps to the copied code and executes the dynamically generated instructions
4. Result Verification: Execution completes with register a0 = 42, validated through the debug interface

Source code (`csrc/jit.S`):
```assembly
# JIT instructions stored as data
jit_instructions:
    .word 0x02a00513    # addi a0, zero, 42
    .word 0x00008067    # jalr zero, ra, 0 (ret)

    # Runtime: copy instructions to executable buffer, then execute
    la t0, jit_instructions
    la t1, jit_code_buffer
    lw t2, 0(t0)
    sw t2, 0(t1)
    lw t2, 4(t0)
    sw t2, 4(t1)
    la t0, jit_code_buffer
    jalr ra, t0, 0      # Execute JIT code
    # Returns here with a0 = 42
    # Test verification reads a0 directly
```

This minimal example demonstrates the complete JIT cycle: encode → copy → execute.
The pattern forms the foundation of modern JIT compilers including V8 (JavaScript), PyPy (Python), and LuaJIT (Lua).
The 60-byte binary efficiently illustrates these concepts without requiring complex instruction set support.

Real-world JIT implementations require additional considerations this minimal design intentionally omits:
- Instruction cache coherency: CPUs typically require explicit cache invalidation (`fence.i`) after self-modifying code writes
- `W^X` (Write XOR Execute) security policies: Modern systems prevent simultaneous write and execute permissions on memory pages
- Branch prediction and BTB (Branch Target Buffer): Production CPUs optimize indirect jumps through JALR with prediction hardware

This unified memory design executes modified code immediately without cache coherency concerns, simplifying the educational demonstration.

## Building and Testing

Build and validate the minimal CPU implementation:
```shell
cd 0-minimal
# Run ChiselTest (verifies a0 = 42)
make test
```

Expected test output:
```
[info] JITTest:
[info] Minimal CPU - JIT Test
[info] - should correctly execute jit.asmbin and set a0 to 42
[info] All tests passed.
```

Run Verilator simulation with VCD waveform generation:
```shell
# Run Verilator simulation with trace analysis
make sim
```

The included Python script analyzes Verilator simulation traces to verify correct CPU behavior across multiple dimensions.
JIT Code Execution: Confirms PC reaches and executes from JIT code buffer (0x102c).
Execution Duration: Tracks cycle count spent executing dynamically generated code.
Memory Layout: Validates expected address layout from jit.S assembly.

Example analysis output:
```
======================================================================
 VCD Trace Analysis Report - 0-minimal RISC-V CPU
======================================================================

Overall Status: [PASS]

Key Findings:
  - JIT Code Execution: OK (499978 cycles at buffer)
  - Register a0 == 42: NO
  - Memory Writes Detected: NO (0 writes)

Detailed Statistics:
  - PC Samples: 500000
  - Max PC Address: 0x00001030
  - Register Writes: 0
  - Writes to a0 (x10): 0

Expected Memory Layout:
  - Entry Point:       0x00001000
  - JIT Code Buffer:   0x0000102c
  - JIT Instructions:  0x00001034

Interpretation:
  - [OK] CPU successfully executed the JIT self-modifying code.
  - Note: ChiselTest validates a0=42 via a separate debug interface.
```

View waveforms with GTKWave or Surfer for detailed signal analysis:
```shell
gtkwave trace.vcd
# or
surfer trace.vcd
```

Building the Verilator simulation requires a C++ compiler that supports C++11.

### Rebuilding jit.asmbin from Source (Optional)

A prebuilt `jit.asmbin` (60 bytes) is provided in `src/main/resources/` for immediate testing.
If you need to rebuild the JIT test program from source:
```shell
# Build jit.asmbin from source (requires RISC-V toolchain)
cd csrc
make              # Builds jit.asmbin in csrc/
make update       # Copies jit.asmbin to ../src/main/resources/
cd ..
```

Toolchain requirement: RISC-V GNU toolchain at `~/riscv/toolchain/bin/` or set `CROSS_COMPILE` environment variable.
The analysis script uses only Python standard library without external dependencies.

## File Structure

```
0-minimal/
├── src/main/scala/
│   ├── riscv/
│   │   ├── Parameters.scala          # CPU parameters
│   │   ├── CPUBundle.scala           # Top-level I/O bundle
│   │   ├── core/
│   │   │   ├── InstructionFetch.scala    # PC management
│   │   │   ├── InstructionDecode.scala   # 5-instruction decoder
│   │   │   ├── Execute.scala             # Addition-only ALU + JALR
│   │   │   ├── MemoryAccess.scala        # Word-aligned LW/SW
│   │   │   ├── WriteBack.scala           # Result mux
│   │   │   ├── RegisterFile.scala        # 32 registers
│   │   │   └── CPU.scala                 # Top CPU module
│   │   └── peripheral/
│   │       ├── Memory.scala              # Harvard architecture (imem + dmem)
│   │       ├── InstructionROM.scala      # Instruction loader
│   │       └── ROMLoader.scala           # ROM → Memory loader
│   ├── board/
│   │   └── Top.scala                 # Integration top module
│   └── resources/
│       └── jit.asmbin                # Prebuilt JIT test binary (60 bytes)
├── src/test/scala/riscv/
│   ├── TestTopModule.scala           # Test harness
│   └── JITTest.scala                 # JIT execution test
├── csrc/
│   ├── jit.S                         # JIT test program source
│   ├── link.lds                      # Linker script
│   └── Makefile                      # Build jit.asmbin from source
├── scripts/
│   └── analyze_trace.py              # Enhanced VCD trace analysis tool
├── Makefile                          # Build automation
└── README.md                         # This file
```
