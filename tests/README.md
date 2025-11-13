# RISCOF Compliance Testing for MyCPU

This directory contains the RISC-V compliance test infrastructure using RISCOF (RISC-V Compliance Framework).
The framework provides automated verification of all MyCPU processor implementations against official RISC-V architectural tests.

## Overview

RISCOF automates the verification process by comparing processor execution against the official RISC-V architectural test suite.
This validation ensures MyCPU implementations correctly execute the RV32I instruction set according to the RISC-V specification.
The framework uses rv32emu as the golden reference model for signature comparison.

## Directory Structure

```
riscof-tests/
├── config.ini                  # RISCOF configuration
├── run-compliance.sh           # Helper script to run tests
├── riscv-arch-test/            # Official RISC-V compliance tests (cloned)
├── rv32emu/                    # Reference model (cloned)
├── rv32emu_plugin/             # RISCOF plugin for rv32emu reference
│   ├── riscof_rv32emu.py
│   ├── rv32emu_isa.yaml
│   ├── rv32emu_platform.yaml
│   └── env/
│       ├── link.ld
│       └── model_test.h
└── mycpu_plugin/               # RISCOF plugin for MyCPU DUTs
    ├── riscof_mycpu.py
    ├── mycpu_isa.yaml
    ├── mycpu_platform.yaml
    ├── ComplianceTest.scala    # Shared Scala test harness
    └── env/
        ├── link.ld
        └── model_test.h
```

## Prerequisites

1. Install RISCOF:
   ```shell
   pip install riscof
   ```

2. Ensure RISC-V toolchain is in PATH:
   ```shell
   riscv32-unknown-elf-gcc --version
   ```

3. Ensure sbt and Chisel dependencies are available

## First-Time Setup

The test infrastructure depends on two external repositories that provide the test suite and reference implementation:
- `riscv-arch-test` provides the official RISC-V compliance test suite
- `rv32emu` serves as the reference implementation for signature comparison

Automated Setup (Recommended)

The setup process executes automatically when you run compliance tests for the first time:

```shell
cd riscof-tests
./run-compliance.sh 1-single-cycle
# Will automatically clone required repositories if missing
```

Or run setup explicitly:

```shell
cd riscof-tests
./setup.sh
```

The setup script will:
- Clone `riscv-arch-test` (official RISC-V test suite)
- Clone `rv32emu` (reference model)
- Verify test suite structure
- Report status with colored output

Manual Setup (Alternative)

```shell
cd riscof-tests
git clone --depth=1 https://github.com/riscv-non-isa/riscv-arch-test
git clone --depth=1 https://github.com/sysprog21/rv32emu
```

## Usage

### Quick Start: Running Compliance Tests

From individual project directories:

```shell
cd 1-single-cycle
make compliance    # Tests RV32I implementation

cd ../2-mmio-trap
make compliance    # Tests RV32I + Zicsr implementation

cd ../3-pipeline
make compliance    # Tests RV32I + Zicsr pipelined implementation
```

The `make compliance` target will:
1. Run RISCOF test framework
2. Execute real CPU simulation via ChiselTest
3. Generate signature files from actual CPU execution
4. Compare against rv32emu reference
5. Display pass/fail results and report location

### Direct RISCOF Execution

Test a specific MyCPU project directly:

```shell
cd riscof-tests
./run-compliance.sh 1-single-cycle
./run-compliance.sh 2-mmio-trap
./run-compliance.sh 3-pipeline
```

### Manual RISCOF Execution

For more control over test execution:

```shell
cd riscof-tests

# Setup (first time only)
riscof setup --dutname=mycpu

# Run tests for specific project
# (Edit config.ini to set PATH to desired project)
riscof run --config=config.ini \
           --suite=riscv-arch-test/riscv-test-suite/ \
           --env=riscv-arch-test/riscv-test-suite/env
```

### Results

After running compliance tests, results are generated in `riscof_work/`:

- HTML Report: `riscof_work/report.html` - Comprehensive test results with pass/fail status
- Signature Files: `riscof_work/rv32i_m/I/src/*/` - Memory dumps from CPU execution
- Comparison: Each test's DUT signature compared against rv32emu reference
- Logs: Detailed execution logs for debugging failures

Example output:
```
Running RISC-V compliance tests for 1-single-cycle (RV32I)...

Compliance test results:
PASSED: 35/41 tests
FAILED: 6/41 tests

Report available at: ../riscof-tests/riscof_work/report.html
```

## Architecture

### Reference Model: rv32emu

Uses [rv32emu](https://github.com/sysprog21/rv32emu) as the golden reference implementation. rv32emu is a fast, feature-complete RV32IMAFDCZicsr_Zifencei emulator.

rv32emu execution: Real emulator runs with `-q` (quiet) and `-a` (signature output) flags, 30-second timeout per test.

### DUT (Device Under Test): MyCPU

All three MyCPU projects use real CPU simulation via ChiselTest:

| Project | ISA Support | Description |
|---------|-------------|-------------|
| 1-single-cycle | RV32I | Single-cycle implementation |
| 2-mmio-trap | RV32I + Zicsr | MMIO and interrupt support |
| 3-pipeline | RV32I + Zicsr | Pipelined implementation |

MyCPU execution:
- ChiselTest simulates actual CPU hardware
- 100,000 cycle simulation per test
- Memory debug interface extracts signature region
- ELF symbol extraction (`begin_signature`, `end_signature`) determines memory range

### Test Flow

The verification process follows a six-stage pipeline:

1. Compilation: RISCOF compiles each test from the riscv-arch-test suite using RISC-V GCC toolchain
2. Reference Execution: rv32emu executes the compiled ELF binary and generates the golden reference signature
3. DUT Execution: MyCPU simulates the same ELF binary through ChiselTest for 100,000 cycles
4. Signature Extraction: The memory debug interface reads the signature region based on ELF symbols (e.g., 0x3000-0x3940)
5. Comparison: RISCOF performs byte-by-byte comparison of DUT signature against rv32emu reference
6. Report Generation: The framework produces an HTML report showing pass/fail status for each test with detailed logs

## ISA Coverage

### 1-single-cycle
- ISA: `RV32I` (base integer instructions only)
- Test Coverage: 41 RV32I compliance tests

### 2-mmio-trap and 3-pipeline
- ISA: `RV32I + Zicsr` (base integer + CSR instructions)
- Test Coverage: 41 RV32I + Zicsr compliance tests

Supported instruction categories:
- Integer computational instructions (ADD, SUB, AND, OR, XOR, etc.)
- Load/store instructions (LW, LH, LB, SW, SH, SB)
- Branch instructions (BEQ, BNE, BLT, BGE, BLTU, BGEU)
- Jump instructions (JAL, JALR)
- System instructions (ECALL, EBREAK)
- CSR instructions (CSRRW, CSRRS, CSRRC, and immediate variants) - 2-mmio-trap and 3-pipeline only

## Limitations

- Memory size: Tests limited to 32KB (MyCPU memory constraint)
- No M extension (multiplication/division)
- No A extension (atomics)
- No F/D extensions (floating-point)
- No C extension (compressed instructions)

## Troubleshooting

### riscof not found
```shell
pip install riscof
```

### RISC-V toolchain not found
Ensure `riscv32-unknown-elf-gcc` is in PATH or set RISCV environment variable.

### ChiselTest failures
Check that:
- sbt is properly configured
- Test harness (ComplianceTest.scala) is in project test directory
- TestTopModule interface matches requirements

### Signature mismatch
Enable VCD waveform generation to debug:
```shell
WRITE_VCD=1 ./run-compliance.sh 1-single-cycle
```

## Integration with Main Projects

Each MyCPU project includes a `make compliance` target for easy testing:

```shell
# Test 1-single-cycle (RV32I)
cd 1-single-cycle
make compliance

# Test 2-mmio-trap (RV32I + Zicsr)
cd 2-mmio-trap
make compliance

# Test 3-pipeline (RV32I + Zicsr)
cd 3-pipeline
make compliance
```

The Makefile targets automatically:
1. Navigate to riscof-tests directory
2. Run appropriate test suite via run-compliance.sh
3. Display pass/fail summary
4. Show HTML report location

## Performance

Typical execution times:
- Single test: ~10 seconds (includes JVM/sbt startup)
- Full 41-test suite: ~7 minutes
- Bottleneck: sbt invocation per test

Optimization considerations:
- Each test requires separate sbt invocation
- JVM startup overhead dominates short tests
- Parallel execution not currently implemented

## Cleaning

From the top-level directory:

```shell
# Clean build artifacts from all projects
make clean

# Deep clean: remove RISCOF results and all generated files
make distclean
```

The `make distclean` target removes:
- RISCOF work directory (`riscof_work/`)
- sbt build artifacts (`target/`, `project/target/`)
- Auto-generated compliance test files
- Temporary test resources (`test.asmbin`)
- Verilator generated files
- sbt output logs from test runs

Note: Source files and configuration are preserved.

## References

- [RISCOF Documentation](https://riscof.readthedocs.io/)
- [RISC-V Architectural Tests](https://github.com/riscv-non-isa/riscv-arch-test)
- [rv32emu](https://github.com/sysprog21/rv32emu)
- [RISC-V Specification](https://riscv.org/technical/specifications/)
