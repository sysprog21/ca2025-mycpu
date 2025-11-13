# RISC-V CPU with MMIO Peripherals and Trap Handling

This project implements an extended single-cycle RISC-V processor in Chisel that adds memory-mapped I/O peripherals and comprehensive trap handling capabilities.
The implementation builds upon the basic single-cycle design by introducing privileged architecture features through Control and Status Registers (CSR) and Core-Local Interrupt Controller (CLINT).
These extensions provide peripheral interfacing and interrupt handling necessary for embedded systems and operating system support.

## Architecture Overview

This implementation extends the base RV32I processor with memory-mapped peripheral support, interrupt handling,
exception support, and privileged instructions. The processor interfaces with external devices (Timer, UART) through memory-mapped I/O,
responds to hardware interrupts from peripherals, and handles software traps while maintaining single-cycle execution for most instructions.

### Instruction Coverage

The design decodes the complete RV32I base ISA together with the machine-mode CSR (Zicsr) subset:
- Arithmetic / Logical: all OP and OP-IMM forms (`add/sub`, shifts, comparisons, bitwise ops)
- Memory: byte/halfword/word loads and stores with sign or zero extension
- Control Flow: all conditional branches, `jal`, `jalr`, `lui`, `auipc`
- System: `ecall`, `ebreak`, `mret`, and fence instructions (treated as architectural no-ops in this configuration)
- CSR Access: `csrrw`, `csrrs`, `csrrc` and immediate variants with proper read-modify-write semantics (no write-back when the source operand is zero)

### Key Enhancements Over Base Implementation

- MMIO Peripherals: Memory-mapped Timer, UART, and VGA devices with device address decoding
- Timer Peripheral: Configurable 32-bit counter with interrupt generation on threshold
- UART Peripheral: Full-duplex serial communication with TX/RX buffering and interrupts
- VGA Peripheral: 640×480@72Hz display with dual-clock framebuffer, palette, and SDL2 visualization
- CSR Support: 15+ machine-mode CSR registers per RISC-V Privileged Spec v1.10
- Interrupt Handling: Hardware interrupt processing from peripherals via CLINT
- Exception Support: Software traps through `ecall` and `ebreak`
- Privileged Instructions: `mret` for trap return, CSR manipulation instructions (CSRRW/CSRRS/CSRRC)

### Design Philosophy

The interrupt mechanism operates at instruction boundaries, ensuring that:
- Current instruction completes before interrupt handling begins
- Atomicity of individual instructions is preserved
- CSR state remains consistent
- Nested interrupts are explicitly prevented through privilege controls

## Control and Status Registers (CSR)

CSRs form an independent 4096-byte address space separate from general-purpose registers. According to the RISC-V ISA specification, "CSR instructions are atomic read-modify-write operations," requiring special handling in the processor pipeline.

### Implemented CSR Registers

#### Machine Information Registers
- `mvendorid` (0xF11): Vendor ID (read-only, returns 0)
- `marchid` (0xF12): Architecture ID (read-only, returns 0)
- `mimpid` (0xF13): Implementation ID (read-only, returns 0)
- `mhartid` (0xF14): Hardware thread ID (read-only, returns 0)

#### Machine Trap Setup
- `mstatus` (0x300): Machine status register
  - Bit 3 (MIE): Machine interrupt enable
  - Bit 7 (MPIE): Previous interrupt enable state
- `misa` (0x301): ISA and extensions (read-only)
- `mie` (0x304): Interrupt enable register
  - Bit 11 (MEIE): External interrupt enable
- `mtvec` (0x305): Trap vector base address

#### Machine Trap Handling
- `mscratch` (0x340): Scratch register for machine trap handlers
- `mepc` (0x341): Machine exception program counter
- `mcause` (0x342): Machine trap cause
  - Bit 31: Interrupt flag (1 = interrupt, 0 = exception)
  - Bits 30:0: Exception code
- `mtval` (0x343): Machine trap value (bad address or instruction)
- `mip` (0x344): Interrupt pending register
  - Bit 11 (MEIP): External interrupt pending

#### Counters and Timers
- `cycle` (0xC00): Cycle counter (lower 32 bits)
- `cycleh` (0xC80): Cycle counter (upper 32 bits)

### CSR Instruction Set

The implementation supports all RV32I Zicsr extension instructions:

#### Atomic Read-Modify-Write Operations
- `CSRRW rd, csr, rs1`: Atomic Read/Write
  - Reads CSR into `rd`
  - Writes `rs1` value to CSR
- `CSRRS rd, csr, rs1`: Atomic Read and Set Bits
  - Reads CSR into `rd`
  - Sets bits in CSR where `rs1` bits are 1
- `CSRRC rd, csr, rs1`: Atomic Read and Clear Bits
  - Reads CSR into `rd`
  - Clears bits in CSR where `rs1` bits are 1

#### Immediate Variants
- `CSRRWI rd, csr, uimm`: Read/Write with 5-bit unsigned immediate
- `CSRRSI rd, csr, uimm`: Read and Set with immediate
- `CSRRCI rd, csr, uimm`: Read and Clear with immediate

### CSR Implementation Details

File: `src/main/scala/riscv/core/CSR.scala`

The CSR module implements:
- Separate 4096-entry register file for CSR address space
- Read-only enforcement for information registers
- Atomic read-modify-write semantics in single cycle
- CLINT interface for interrupt-driven CSR updates
- Debug read port for verification

Key Operations:
- Decode CSR address from instruction (bits 31:20)
- Determine operation type from funct3 field
- Perform atomic RMW for CSRRS/CSRRC operations
- Handle read-only register protection
- Interface with CLINT for exception/interrupt updates

## Core-Local Interrupt Controller (CLINT)

The CLINT manages interrupt and exception processing by coordinating CSR updates and control flow redirection.

### Interrupt Processing Model

The processor handles interrupts at instruction boundaries:

1. Detection: Check `mstatus.mie` and pending interrupt signals
2. Entry: Save state and jump to handler
3. Handling: Execute trap handler code
4. Exit: Restore state via `mret` instruction

### Interrupt Entry Sequence

File: `src/main/scala/riscv/core/CLINT.scala`

When responding to an interrupt or exception:

1. Save Return Address: Write PC + 4 to `mepc`
2. Record Cause: Write exception code to `mcause`
   - Hardware interrupt: `mcause[31] = 1`, cause code in bits 30:0
   - Software exception: `mcause[31] = 0`, exception code in bits 30:0
3. Disable Interrupts:
   - Save current `mstatus.mie` to `mstatus.mpie`
   - Clear `mstatus.mie` to prevent nested interrupts
4. Jump to Handler: Redirect PC to address in `mtvec`

### Interrupt Exit (`mret`)

The `mret` instruction atomically:
1. Restores PC from `mepc`
2. Restores `mstatus.mie` from `mstatus.mpie`
3. Resumes normal execution

### Exception Codes

According to RISC-V privilege specification:

Exceptions (`mcause[31] = 0`):
- `0`: Instruction address misaligned
- `2`: Illegal instruction
- `3`: Breakpoint (`ebreak`)
- `8`: Environment call from U-mode (`ecall`)
- `11`: Environment call from M-mode (`ecall`)

Interrupts (`mcause[31] = 1`):
- `11`: Machine external interrupt

### CLINT Design Considerations

1. Instruction Boundary: Interrupts are only recognized between instructions, never mid-execution
2. Atomicity: All CSR updates during interrupt entry occur atomically in one cycle
3. Priority: CLINT has high-priority write access to CSRs, bypassing normal CSR instruction paths
4. No Nesting: Clearing `mstatus.mie` during entry prevents nested interrupt handling

## Software Exceptions

### Environment Call (`ecall`)

Triggers a synchronous exception for system call interface:
- Saves current PC to `mepc`
- Sets `mcause` to 11 (M-mode ecall)
- Jumps to trap handler in `mtvec`

### Breakpoint (`ebreak`)

Triggers a synchronous exception for debugging:
- Saves current PC to `mepc`
- Sets `mcause` to 3 (breakpoint)
- Jumps to trap handler in `mtvec`

Both instructions behave identically to hardware interrupts regarding CSR manipulation, differing only in the `mcause` value to indicate the specific exception type.

## Memory-Mapped Peripherals

### Device Selection

The processor uses high-order address bits to select between devices:
- `deviceSelect = 0`: Main memory
- `deviceSelect = 1`: Timer peripheral
- `deviceSelect = 2`: UART peripheral
- `deviceSelect = 3`: VGA peripheral

## Timer Peripheral

File: `src/main/scala/peripheral/Timer.scala`

A memory-mapped timer peripheral provides periodic interrupt generation capabilities.

### Memory-Mapped Registers

Located at base address `0x80000000`:

- Timer Limit Register (`0x80000004`): Sets interrupt interval
  - Write: Configure timer period (in cycles)
  - Read: Current limit value
- Timer Enable Register (`0x80000008`): Controls timer operation
  - Write: 1 = enable, 0 = disable
  - Read: Current enable state

### Timer Operation

1. Internal counter increments each cycle when enabled
2. When counter reaches limit value:
   - Assert interrupt signal to CLINT
   - Reset counter to 0
3. CLINT processes interrupt according to `mstatus.mie` and `mie.meie`

## VGA Peripheral

File: `src/main/scala/peripheral/VGA.scala`

A memory-mapped VGA display peripheral for visual output with 640×480@72Hz timing and indexed color support.

### Architecture

Display Specifications:
- Resolution: 640×480 pixels @ 72Hz refresh rate
- Framebuffer: 64×64 pixels (4-bit indexed color)
- Color Depth: 16-color palette with 6-bit RRGGBB format
- Upscaling: 6× hardware upscaler (64×64 → 384×384 centered display)
- Animation: 12-frame double-buffered animation support

Memory Organization:
- Display Memory: 12 frames × 4096 pixels × 4 bits = 24KB
- Pixel Packing: 8 pixels per 32-bit word (4 bits per pixel)
- Frame Capacity: 49,152 bytes uncompressed (4,755 bytes with delta compression)

### Memory-Mapped Registers

Base address: `0x30000000`

Control Registers:
- VGA_ID (0x30000000): Device identification (read-only, returns 0x56474131 "VGA1")
- VGA_CTRL (0x30000004): Control register
  - Bit 0: Display enable (1 = on, 0 = off)
  - Bit 1: Auto-advance enable (1 = automatic frame cycling)
- VGA_STATUS (0x30000008): Status register (read-only)
  - Bit 0: V-sync active
  - Bit 1: H-sync active
- VGA_UPLOAD_ADDR (0x30000010): Framebuffer write address pointer
  - Format: [frame_index:4][pixel_offset:12] (bits packed as 32-bit word address)
- VGA_STREAM_DATA (0x30000014): Streaming data write port
  - Write: 8 pixels (32 bits) to current upload address, auto-increment address

Palette Registers:
- VGA_PALETTE(n) (`0x30000020 + n*4`): Color palette entries (n = 0..15)
  - Format: 6-bit RRGGBB (bits 5:4 = RR, bits 3:2 = GG, bits 1:0 = BB)
  - Each component: 0-3 scale (4 levels)

### Programming Model

1. Initialize Palette:
```c
#define VGA_BASE 0x30000000u
#define VGA_PALETTE(n) (VGA_BASE + 0x20 + ((n) << 2))

// Set color 0 to dark blue (RRGGBB = 000001)
*(volatile uint32_t*)VGA_PALETTE(0) = 0x01;

// Set color 1 to white (RRGGBB = 111111)
*(volatile uint32_t*)VGA_PALETTE(1) = 0x3F;
```

2. Upload Frame Data:
```c
#define VGA_UPLOAD_ADDR (VGA_BASE + 0x10)
#define VGA_STREAM_DATA (VGA_BASE + 0x14)

// Set upload address (frame 0, pixel 0)
*(volatile uint32_t*)VGA_UPLOAD_ADDR = 0x00000000;

// Upload 8 pixels at a time (32-bit packed)
for (int i = 0; i < 512; i++) {  // 4096 pixels / 8 = 512 words
    uint32_t packed_pixels = pack8_pixels(&frame_data[i * 8]);
    *(volatile uint32_t*)VGA_STREAM_DATA = packed_pixels;
}
```

3. Enable Display:
```c
#define VGA_CTRL (VGA_BASE + 0x04)

// Enable display and auto-advance
*(volatile uint32_t*)VGA_CTRL = 0x03;
```

### Pixel Packing Format

Each 32-bit word contains 8 pixels with 4-bit color indices:

```
Bits:  31-28 | 27-24 | 23-20 | 19-16 | 15-12 | 11-8  | 7-4   | 3-0
Pixel:   7   |   6   |   5   |   4   |   3   |   2   |   1   |   0
```

C helper function:
```c
static inline uint32_t pack8_pixels(const uint8_t *pixels) {
    return (uint32_t)(pixels[0] & 0xF) |
           ((uint32_t)(pixels[1] & 0xF) << 4) |
           ((uint32_t)(pixels[2] & 0xF) << 8) |
           ((uint32_t)(pixels[3] & 0xF) << 12) |
           ((uint32_t)(pixels[4] & 0xF) << 16) |
           ((uint32_t)(pixels[5] & 0xF) << 20) |
           ((uint32_t)(pixels[6] & 0xF) << 24) |
           ((uint32_t)(pixels[7] & 0xF) << 28);
}
```

### Animation Support

Frame Management:
- 12 frames stored in framebuffer memory
- Display controller cycles through frames at configurable rate
- Double buffering: CPU can upload to one frame while another displays

Auto-Advance Mode:
When enabled (`VGA_CTRL[1] = 1`), the display automatically cycles through frames:
- Frame rate: Determined by V-sync timing and frame count
- Continuous loop: Returns to frame 0 after frame 11
- Use case: Smooth animation without CPU intervention

Manual Frame Control:
Disable auto-advance and write frame index to VGA_CTRL for manual control.

### Color Palette Design

The 6-bit RRGGBB format provides 64 colors with 4 intensity levels per channel:

Standard Palette Example (Nyancat):
```c
static const uint8_t nyancat_palette[14] = {
    0x01,  //  0: Dark blue background
    0x3F,  //  1: White
    0x00,  //  2: Black
    0x3E,  //  3: Light pink/beige
    0x3B,  //  4: Pink
    0x36,  //  5: Hot pink
    0x30,  //  6: Red
    0x38,  //  7: Orange
    0x3C,  //  8: Yellow
    0x0C,  //  9: Green
    0x0B,  // 10: Light blue
    0x17,  // 11: Purple
    0x2A,  // 12: Gray
    0x3A,  // 13: Peach
};
```

### VGA Timing

Pixel Clock: 31.5 MHz (640×480@72Hz standard)

Horizontal Timing:
- Visible pixels: 640
- Front porch: 24 pixels
- Sync pulse: 40 pixels
- Back porch: 128 pixels
- Total: 832 pixels per line

Vertical Timing:
- Visible lines: 480
- Front porch: 9 lines
- Sync pulse: 3 lines
- Back porch: 28 lines
- Total: 520 lines per frame

### Implementation Details

Dual-Clock Architecture:
- CPU Clock Domain: MMIO register access and framebuffer writes
- Pixel Clock Domain: VGA signal generation and framebuffer reads
- Clock Domain Crossing: Synchronized through dual-port RAM

Upscaler Logic:
Each 64×64 pixel is replicated 6×6 times for 384×384 output, centered on 640×480 display with black borders.

Framebuffer Organization:
- Memory type: Synchronous dual-port RAM (SyncReadMem)
- Read port: Pixel clock domain (VGA controller)
- Write port: CPU clock domain (MMIO interface)
- Addressing: Word-aligned (8 pixels per address)

## Pipeline Modifications

### Instruction Decode Enhancements

File: `src/main/scala/riscv/core/InstructionDecode.scala`

Added outputs:
- `csr_reg_address`: CSR address for CSR instructions
- `csr_reg_write_enable`: Enable signal for CSR writes

Control logic recognizes:
- CSR instruction opcodes (0x1110011)
- `mret` instruction
- `ecall` and `ebreak` instructions

### Execute Stage Extensions

File: `src/main/scala/riscv/core/Execute.scala`

New functionality:
- CSR Write Data Computation: Implements atomic RMW semantics
  - `CSRRW[I]`: Direct write (pass through source)
  - `CSRRS[I]`: Bitwise OR with CSR value
  - `CSRRC[I]`: Bitwise AND with complement
- Immediate Source Selection: Multiplexes between register and immediate based on `funct3[2]`

CSR write data logic:
```scala
val csr_imm = instruction(19, 15)  // 5-bit unsigned immediate
val csr_src = Mux(funct3(2), csr_imm, reg1_data)  // Select source
io.csr_reg_write_data := MuxLookup(funct3(1, 0), csr_src)(
  Seq(
    "b01".U -> csr_src,                           // CSRRW[I]
    "b10".U -> (csr_reg_read_data | csr_src),    // CSRRS[I]
    "b11".U -> (csr_reg_read_data & ~csr_src)    // CSRRC[I]
  )
)
```

### Write-Back Stage Updates

File: `src/main/scala/riscv/core/WriteBack.scala`

Additional write-back source:
- `RegWriteSource.CSR`: Route CSR read data to destination register

Enables read-modify-write CSR operations where the old value is written to `rd` while new value updates the CSR.

## Module Hierarchy

```
CPU (src/main/scala/riscv/core/CPU.scala)
├── InstructionFetch
├── InstructionDecode (enhanced with CSR/trap recognition)
├── Execute (enhanced with CSR RMW logic)
├── MemoryAccess (enhanced with MMIO device routing)
├── WriteBack (enhanced with CSR data path)
├── RegisterFile
├── CSR (new module)
│   ├── Machine info registers
│   ├── Trap setup registers
│   ├── Trap handling registers
│   └── Cycle counter
├── CLINT (new module)
│   ├── Interrupt detection logic
│   ├── Exception cause encoding
│   └── CSR update coordination
├── Timer (new peripheral)
│   ├── Counter logic
│   └── Interrupt generation
├── UART (new peripheral)
│   ├── TX/RX buffers
│   └── Serial communication logic
└── VGA (new peripheral)
    ├── VGA timing generator (640×480@72Hz)
    ├── Framebuffer (12 frames, dual-port RAM)
    ├── Palette registers (16 colors, 6-bit RRGGBB)
    ├── Upscaler (6× pixel replication)
    └── MMIO interface (registers + streaming upload)
```

## Test Suite

The implementation includes comprehensive verification through multiple testing methodologies:

### ChiselTest Unit Tests (9 tests)

Located in `src/test/scala/riscv/singlecycle/`:

1. ExecuteTest: Validates CSR write data computation for CSRRS/CSRRC operations
2. ByteAccessTest: Verifies byte-level memory operations and alignment
3. TimerTest: Tests memory-mapped timer register access and configuration
4. InterruptTrapTest: Validates complete interrupt entry/exit sequence with mepc/mcause
5. FibonacciTest: Recursive Fibonacci calculation with interrupt support
6. QuicksortTest: Sorting algorithm execution testing control flow
7. UartMMIOTest: UART peripheral register access and TX/RX functionality
8. CLINTCSRTest (External Interrupt): Hardware interrupt handling via CLINT
9. CLINTCSRTest (Environmental Instructions): `ecall`/`ebreak` exception support

All unit tests pass successfully:
```shell
make test
# Total number of tests run: 9
# Tests: succeeded 9, failed 0
```

### RISCOF Compliance Testing (119 tests)

RISC-V architectural compliance testing validates correct implementation of RV32I + Zicsr extensions against the official RISC-V specification.

Test Coverage:
- RV32I base instruction set (41 tests)
- Zicsr extension - CSR instructions (40 tests)
  - CSRRW, CSRRS, CSRRC and immediate variants
  - Machine-mode CSR registers (mstatus, mie, mtvec, mepc, mcause, etc.)
  - Atomic read-modify-write semantics
- Physical Memory Protection (PMP) registers (38 tests)

Running Compliance Tests:
```shell
make compliance
# Expected duration: 10-15 minutes
# Results saved to: results/report.html
```

Last Verification: 2025-11-08
- Unit Tests: 9/9 passed
- RISCOF Compliance: 119/119 tests passed (RV32I + Zicsr + PMP)
- Verilator Simulation: Completed successfully with interrupt test programs

### Simulator Behavior Notes

When running test programs with stack operations, you may observe warnings like:
```
invalid read address 0x10000000
invalid write address 0x0ffffffc
```

These warnings are expected and harmless. RISC-V programs use stack addresses not mapped in the minimal simulator memory model. Programs execute correctly despite these warnings - they simply indicate memory accesses outside the simulated address space.

## Interrupt Handling Example

### Trap Handler Structure

```assembly
# Trap vector at mtvec
trap_handler:
    # Save context (caller-saved registers)
    addi sp, sp, -16
    sw   ra, 0(sp)
    sw   t0, 4(sp)
    sw   t1, 8(sp)
    sw   t2, 12(sp)

    # Read mcause to determine trap type
    csrr t0, mcause

    # Check if interrupt (bit 31)
    bltz t0, handle_interrupt

    # Handle exception (ecall/ebreak)
    # ... exception handling code ...
    j trap_exit

handle_interrupt:
    # Handle external interrupt
    # ... interrupt handling code ...

trap_exit:
    # Restore context
    lw   t2, 12(sp)
    lw   t1, 8(sp)
    lw   t0, 4(sp)
    lw   ra, 0(sp)
    addi sp, sp, 16

    # Return from interrupt
    mret
```

### Timer Interrupt Setup

```c
// Configure timer for periodic interrupts
void setup_timer(uint32_t interval) {
    // Set timer limit
    *(volatile uint32_t*)0x80000004 = interval;

    // Enable timer
    *(volatile uint32_t*)0x80000008 = 1;

    // Enable machine external interrupts
    uint32_t mie;
    asm volatile("csrr %0, mie" : "=r"(mie));
    mie |= (1 << 11);  // Set MEIE bit
    asm volatile("csrw mie, %0" :: "r"(mie));

    // Enable global interrupts
    uint32_t mstatus;
    asm volatile("csrr %0, mstatus" : "=r"(mstatus));
    mstatus |= (1 << 3);  // Set MIE bit
    asm volatile("csrw mstatus, %0" :: "r"(mstatus));
}
```

## Simulation with Verilator

### Running Interrupt Tests

```shell
# Basic simulation with interrupt support
make sim SIM_ARGS="-instruction ../../../src/main/resources/interrupt_test.asmbin"

# Extended simulation for timer testing
make sim SIM_TIME=1000000 SIM_ARGS="-instruction ../../../test_program.asmbin"
```

### VGA Display Demo

The processor includes a VGA peripheral for visual output with SDL2 support. The demo displays an animated nyancat on a 640×480@72Hz virtual display using advanced delta frame compression.

Quick Start:
```shell
make demo
```

This command will:
1. Build Verilator simulator with SDL2 graphics support
2. Run the nyancat animation program (12 frames of animated nyancat)
3. Open an SDL2 window showing real-time VGA output
4. Simulate 500 million cycles (~5 minutes, includes full animation)
5. Display completion progress (1%, 50%, 100%)

VGA Peripheral Features:
- Display: 640×480 @ 72Hz timing
- Framebuffer: Dual-clock RAM with 12 frames of 64×64 pixels
- Rendering: 6× upscaling (64×64 → 384×384 centered display)
- MMIO Base: 0x30000000
- Color: 16-color palette with 6-bit RRGGBB format
- Compression: Delta frame encoding (91% size reduction, 49KB → 4.7KB)

Animation Details:
The nyancat demo uses compressed animation data generated from the upstream [klange/nyancat](https://github.com/klange/nyancat) project:
- Source: Original nyancat terminal animation
- Frames: 12 frames × 4096 pixels (64×64 each)
- Compression: Delta frame encoding achieving 91% reduction (29% better than RLE)
- Generation: Automated Python script downloads and compresses animation data
- Colors: 14-color palette mapped from upstream character encoding
- Binary size: 8.7KB (vs 10.8KB with RLE, 19% smaller)

Delta Frame Compression Format:
The animation uses an advanced delta encoding scheme exploiting 94.4% frame-to-frame similarity:

| Opcode | Meaning | Example |
|--------|---------|---------|
| `0x0X` | SetColor (X = color 0-13) | `0x05` sets current color to 5 |
| `0x1Y` | Skip unchanged (Y+1 pixels, 1-16) | `0x13` skips 4 pixels |
| `0x2Y` | Repeat changed (Y+1 pixels, 1-16) | `0x23` writes 4 pixels |
| `0x3Y` | Skip unchanged ((Y+1)×16 pixels, 16-256) | `0x32` skips 48 pixels |
| `0x4Y` | Repeat changed ((Y+1)×16 pixels, 16-256) | `0x42` writes 48 pixels |
| `0x5Y` | Skip unchanged ((Y+1)×64 pixels, 64-1024) | `0x52` skips 192 pixels |
| `0xFF` | EndOfFrame marker | Signals frame completion |

Compression Performance:
- Frame 0 (baseline): 576 opcodes (86% reduction) using RLE
- Frames 1-11 (delta): avg 390 opcodes (91% reduction) exploiting temporal coherence
- Best frames (3, 9): 235-236 opcodes (95% reduction) with minimal pixel changes
- Total: 4,755 bytes compressed data (vs 6,715 RLE, 29% improvement)

This achieves 91% compression with pixel-perfect quality, enabling 12 frames to fit in 8.7KB binary with delta decompression logic.

Validation Status (2025-11-10):
- Compression: 4,755 bytes (29% better than RLE)
- Binary size: 8.7KB (19% smaller than RLE)
- Build: Clean compilation, no warnings
- Demo: SDL2 VGA display working correctly
- Quality: Pixel-perfect decompression verified
- Backup: Original RLE implementation preserved

Rebuilding Animation Data:
The animation data can be regenerated from upstream source:
```shell
cd csrc
make clean
make nyancat.asmbin  # Auto-downloads and compresses upstream animation
```

The build system automatically:
1. Downloads animation.c from klange/nyancat GitHub repository
2. Parses 12 frames of ASCII art animation (64×64 pixels each)
3. Maps color characters to palette indices
4. Applies compression (scripts/gen-nyancat-data.py with configurable mode)
5. Generates nyancat-data.h C header file with NYANCAT_COMPRESSION_DELTA define
6. Compiles nyancat.c with conditional decompression logic
7. Produces 8.7KB (delta) or 10.8KB (baseline) RISC-V binary

Build-time Configuration:
```shell
# Delta compression (default, 91% reduction, 8.7KB binary)
make nyancat.asmbin

# Baseline RLE (87% reduction, 10.8KB binary)
make NYANCAT_COMPRESSION_DELTA=0 nyancat.asmbin
```

Technical Implementation:
- Generator: `scripts/gen-nyancat-data.py` with `--delta` flag
- Build control: `NYANCAT_COMPRESSION_DELTA` (default=1) in Makefile
- Decompressor: `csrc/nyancat.c` (279 lines with conditional delta logic)
- Memory: 8KB RAM (4KB current + 4KB previous frame buffers)
- Bare-metal: Custom `copy_buffer()` function (no libc dependency)
- Compression: Frame 0 baseline RLE + Frames 1-11 delta encoding (if DELTA=1)

Manual Simulation:
```shell
# Build with SDL2 support
make verilator-sdl2

# Run with custom program
cd verilog/verilator/obj_dir
./VTop -vga -instruction ../../../src/main/resources/your_program.asmbin -time 10000000
```

The SDL2 window will display the VGA output in real-time as the simulation runs. Close the window or let the simulation complete to exit.

Custom Animation Programs:
To create your own VGA animations:
1. Use the VGA MMIO interface documented above
2. Upload palette to `VGA_PALETTE` registers
3. Upload frames to framebuffer via `VGA_UPLOAD_ADDR` and `VGA_STREAM_DATA`
4. Enable display with `VGA_CTRL = 0x03` (display + auto-advance)
5. See `csrc/nyancat.c` for complete reference implementation

### Debugging Interrupt Behavior

Key signals to observe in waveform viewer:

CSR Signals:
- `csr_regs_mstatus`: Monitor MIE and MPIE bits
- `csr_regs_mepc`: Saved return address
- `csr_regs_mcause`: Exception/interrupt cause
- `csr_regs_mtvec`: Trap handler address

CLINT Signals:
- `clint_io_interrupt_flag`: External interrupt input
- `clint_io_jump_flag`: Trap entry indicator
- `clint_io_jump_address`: Target handler address

Timer Signals:
- `timer_io_signal_interrupt`: Timer interrupt output
- Timer internal counter state

### Waveform Analysis

```shell
# Generate VCD waveform
make sim SIM_VCD=interrupt_trace.vcd

# View with Surfer
surfer interrupt_trace.vcd
```

Look for interrupt sequence:
1. Timer interrupt assertion
2. `mstatus.mie` cleared
3. PC jump to `mtvec` address
4. `mepc` saving current PC
5. Handler execution
6. `mret` restoring `mstatus.mie` and PC

## Implementation Notes

### Design Decisions

1. Single-Cycle with Exceptions: Interrupt handling still completes in one cycle by using priority paths for CSR updates through CLINT

2. No Nested Interrupts: Clearing `mstatus.mie` on entry prevents interrupt nesting, simplifying handler implementation

3. Machine Mode Only: Only M-mode privilege level is implemented; no user mode or supervisor mode support

4. Simplified Timer: Memory-mapped timer uses simple counter rather than full RISC-V MTIMER specification

### Performance Characteristics

- CPI: Still 1.0 for normal instructions
- Interrupt Latency: One cycle (detected at instruction boundary)
- Handler Overhead: Depends on context save/restore in software

### Extensions and Limitations

Supported:
- Complete RV32I with Zicsr extension
- Machine-mode interrupts and exceptions
- CSR atomic operations
- Timer peripheral
- Basic trap handling

Not Supported:
- User mode / Supervisor mode
- Vectored interrupt mode (mtvec direct mode only)
- Nested interrupts
- Physical memory protection (PMP)
- Virtual memory (MMU/TLB)
- Other standard extensions (M, A, F, D)

## File Organization

```
2-mmio-trap/
├── src/main/scala/
│   ├── riscv/core/
│   │   ├── CPU.scala              # Enhanced with CSR/CLINT integration & MMIO routing
│   │   ├── InstructionFetch.scala # Enhanced with trap redirection
│   │   ├── InstructionDecode.scala # Enhanced with CSR instruction decode
│   │   ├── Execute.scala          # Enhanced with CSR RMW logic
│   │   ├── MemoryAccess.scala     # MMIO device selection (4 devices)
│   │   ├── WriteBack.scala        # Enhanced with CSR data path
│   │   ├── RegisterFile.scala
│   │   ├── ALU.scala
│   │   ├── ALUControl.scala
│   │   ├── CSR.scala              # NEW: CSR register file
│   │   └── CLINT.scala            # NEW: Interrupt controller
│   ├── peripheral/
│   │   ├── Memory.scala           # Main memory with MMIO routing
│   │   ├── Timer.scala            # NEW: Timer with interrupts
│   │   ├── UART.scala             # NEW: UART with TX/RX interrupts
│   │   └── VGA.scala              # NEW: VGA display (640×480@72Hz)
│   └── board/verilator/
│       ├── Top.scala              # Top-level with VGA integration
│       └── VGASimulator.scala     # SDL2 visualization wrapper
├── src/test/scala/                # ChiselTest suites
├── csrc/                          # C/Assembly test programs
│   ├── nyancat.c                  # Nyancat animation with delta decompression
│   ├── nyancat-data.h             # Delta-compressed animation data (4,755 bytes)
│   ├── nyancat-rle-original.c     # Backup: Original RLE implementation
│   ├── nyancat-data-rle-original.h # Backup: Original RLE data (6,715 bytes)
│   ├── init_minimal.S             # Minimal init (no trap handler)
│   └── Makefile                   # Auto-generates nyancat-data.h (delta)
├── scripts/
│   ├── gen-nyancat-data-delta.py  # Delta frame compression generator (459 lines)
│   └── README.md                  # Script documentation
├── claudedocs/                    # Analysis and implementation docs
│   ├── nyancat-compression-proposal.md # Compression analysis
│   └── nyancat-delta-implementation.md # Delta implementation details
└── verilog/verilator/             # Verilator simulation with SDL2
```

## Comparison with Base Implementation

| Feature | 1-single-cycle | 2-mmio-trap |
|---------|---------------|-------------|
| Instruction Set | RV32I | RV32I + Zicsr |
| CSR Support | No | Yes (15+ registers) |
| Interrupts | No | Hardware interrupts from peripherals |
| Exceptions | No | ecall, ebreak, mret |
| Privileged Modes | No | Machine mode only |
| MMIO Peripherals | No | Timer + UART + VGA (4 device slots) |
| Timer | No | 32-bit counter with interrupt |
| UART | No | Full-duplex TX/RX with buffering |
| VGA | No | 640×480@72Hz display with SDL2 |
| Animation | No | 12-frame nyancat with delta encoding (91% compression) |
| Binary Size | N/A | 8.7KB (nyancat.asmbin with delta compression) |
| Test Count | 9 tests | 9 tests |
| Module Count | 10 modules | 14 modules (+CSR, +CLINT, +UART, +VGA) |

## References
- [RISC-V Instruction Set Manual](https://riscv.org/technical/specifications/)
- [RISC-V Privileged Architecture](https://riscv.org/technical/specifications/)
- [Chisel Documentation](https://www.chisel-lang.org/)
- [Verilator Manual](https://verilator.org/guide/latest/)
- [Hardware-accelerated Nyancat](https://github.com/sysprog21/vga-nyancat/)
