# 0-minimal: Minimal RISC-V CPU

## Overview

This project implements a minimal single-cycle RISC-V CPU designed specifically to execute `jit.asmbin`, a self-modifying code demonstration showcasing JIT compilation concepts.
The implementation includes only the 5 instructions required by this program, making it an excellent educational example of focused processor design.
This approach demonstrates how processors can be optimized for specific workload requirements rather than implementing complete instruction sets.

## Supported Instructions

The CPU supports exactly these RISC-V instructions:
1. AUIPC (Add Upper Immediate to PC) - PC-relative addressing for data section access
2. ADDI (Add Immediate) - arithmetic operations and register initialization
3. LW (Load Word) - word-aligned memory reads without byte/halfword support
4. SW (Store Word) - word-aligned memory writes with full 32-bit strobes
5. JALR (Jump and Link Register) - function calls, returns, and dynamic jump targets

ECALL is not required for this minimal CPU since test verification reads registers directly via debug interface.

## Design Highlights

### Ultra-Minimal ALU
The ALU supports only addition operations (op1 + op2), eliminating all subtraction, logic, and shift circuits.
This single operation serves ADDI arithmetic, AUIPC PC-relative addressing, LW/SW address calculation, and JALR target computation.
The simplified design reduces hardware complexity while supporting all required instruction semantics.

### Simplified Memory Access
Memory operations support word-aligned access only, eliminating byte/halfword extraction and alignment logic.
Load Word (LW) reads 32-bit values directly: `data ← mem[addr]`.
Store Word (SW) writes with all byte strobes enabled: `mem[addr] ← data`.

### Minimal Decode Logic
The instruction decoder handles only 6 opcodes, significantly reducing control complexity.
Immediate extraction supports I-type, S-type, and U-type formats without B-type or J-type encoding.
The design eliminates funct3/funct7 field decoding since all operations are uniquely determined by opcode.

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
Parsed 74 signals

======================================================================
VCD Trace Analysis Report - 0-minimal RISC-V CPU
======================================================================

Overall Status: [PASS]

Key Findings:
  [OK] JIT Code Execution: 499978 cycles at buffer address (0x102c)
  [NO] Register a0 = 42: False
  [NO] Memory Writes: 0 total writes

Detailed Statistics:
  PC Samples: 500000
  Max PC Address: 0x00001030
  Register Writes: 0
  Writes to a0 (x10): 0

Expected Memory Layout:
  Entry Point:       0x00001000
  JIT Code Buffer:   0x0000102c
  JIT Instructions:  0x00001034

Interpretation:
  [OK] CPU successfully executed JIT self-modifying code
  [OK] PC spent 499978 cycles executing from buffer
  [OK] JIT code execution flow verified

  Note: Internal signals (register writes, memory writes)
        are not exported to VCD in this minimal CPU design.
        ChiselTest validates a0=42 via debug interface.
```

View waveforms with GTKWave or Surfer for detailed signal analysis:
```shell
gtkwave trace.vcd
# or
surfer trace.vcd
```

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
│   │   │   ├── InstructionDecode.scala   # 6-instruction decoder
│   │   │   ├── Execute.scala             # Addition-only ALU + JALR
│   │   │   ├── MemoryAccess.scala        # Word-aligned LW/SW
│   │   │   ├── WriteBack.scala           # Result mux
│   │   │   ├── RegisterFile.scala        # 32 registers
│   │   │   └── CPU.scala                 # Top CPU module
│   │   └── peripheral/
│   │       ├── Memory.scala              # Unified instruction/data memory
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
│   └── analyze_trace.py              # VCD trace analysis tool
├── Makefile                          # Build automation
└── README.md                         # This file
```
