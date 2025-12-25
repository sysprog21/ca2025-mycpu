# 4-soc: System-on-Chip with AXI4-Lite Bus

RISC-V RV32I processor with AXI4-Lite bus interface, VGA, and UART peripherals.

## Features

- CPU: 5-stage pipelined RISC-V RV32I with forwarding and branch prediction
- Branch Prediction: BTB (32-entry) + RAS (4-entry) + IndirectBTB (8-entry) for reduced penalties
- Bus: AXI4-Lite protocol with master/slave state machines
- Peripherals:
  - VGA: 640x480@72Hz with 64x64 framebuffer (6x scaling) and 16-color palette
  - UART: Buffered TX/RX at 115200 baud with status register
  - Memory: 2MB main memory (program loaded at 0x1000)

## Architecture

```
CPU (AXI4-Lite Master) + BTB (32-entry) + RAS (4-entry) + IndirectBTB (8-entry)
  └─> BusSwitch (Address decoder, bits[31:29])
       ├─> 0x0000_0000: Main Memory (2MB)
       ├─> 0x2000_0000: VGA Controller
       │    ├─> 0x00: ID (RO) - 0x56474131 "VGA1"
       │    ├─> 0x04: STATUS (RO) - vblank, safe_to_swap, curr_frame
       │    ├─> 0x08: INTR_STATUS (W1C) - vblank interrupt
       │    ├─> 0x10: UPLOAD_ADDR (RW) - framebuffer address
       │    ├─> 0x14: STREAM_DATA (WO) - pixel data (auto-increment)
       │    ├─> 0x20: CTRL (RW) - enable, blank, swap, frame_sel
       │    └─> 0x24-0x60: PALETTE[0-15] (RW) - 6-bit RRGGBB
       └─> 0x4000_0000: UART Controller
            ├─> 0x00: STATUS (RO) - bit0=TX ready, bit1=RX valid
            ├─> 0x04: BAUD_RATE (RO) - 115200
            ├─> 0x08: INTERRUPT (WO) - write non-zero to set, zero to clear
            ├─> 0x0C: RX_DATA (RO) - received byte (read clears interrupt)
            └─> 0x10: TX_DATA (WO) - transmit byte
```

## Build & Test

```bash
# Run ChiselTest suite (from project root)
sbt "project soc" test

# Or use make targets (from 4-soc/)
make test

# Generate Verilog and build Verilator simulator
make verilator

# Run VGA test (nyancat demo with SDL2 display)
make check-vga

# Run UART loopback test (no window)
make check-uart

# Interactive MyCPU shell (type 'help' for commands)
make shell

# Run simulation with custom binary
make sim BINARY=csrc/nyancat.asmbin

# Run RISCOF compliance tests
make compliance

# Format code
make indent

# Clean build artifacts
make clean
```

## AXI4-Lite Transaction Flow

### Read Transaction
1. Master asserts `ARVALID` + address
2. Slave asserts `ARREADY` (handshake)
3. Slave provides `RVALID` + `RDATA`
4. Master asserts `RREADY` (handshake)

### Write Transaction
1. Master asserts `AWVALID` + address
2. Slave asserts `AWREADY` (handshake)
3. Master asserts `WVALID` + `WDATA` + `WSTRB`
4. Slave asserts `WREADY` (handshake)
5. Slave asserts `BVALID` + `BRESP`
6. Master asserts `BREADY` (handshake)

## Branch Prediction

### Branch Target Buffer (BTB)

32-entry direct-mapped cache with 2-bit saturating counter:

- Indexing: PC[6:2] selects entry, PC[31:7] as tag
- Counter states: SNT(0) → WNT(1) → WT(2) → ST(3)
- Prediction: Taken when counter >= 2 (WT or ST)
- Allocation: Only taken branches allocate; not-taken never pollutes BTB
- New entries: Initialize to WT(2) for quick loop adaptation
- Power: Operand-gated tag compare reduces switching on invalid entries

Pipeline integration:
- IF stage: Combinational lookup for same-cycle prediction
- ID stage: Registered update when branch resolves

Misprediction handling:
1. Predicted taken but not taken → Redirect to PC+4, decrement counter
2. Hit but wrong target → Redirect to correct target, update entry
3. Miss on taken branch → Allocate new entry with WT counter

### Return Address Stack (RAS)

4-entry stack for JALR return prediction (register-computed targets):

- Push: JAL with rd=ra/t0 pushes PC+4 (function call)
- Pop: JALR with rs1=ra/t0, rd=x0 pops predicted return address
- Overflow: Shift stack down, oldest entry lost
- Underflow: Stack pointer saturates at 0, output marked invalid
- Tail call: Simultaneous push+pop replaces top-of-stack
- Recovery: Restore interface for misprediction rollback

### Indirect Branch Target Buffer (IndirectBTB)

8-entry fully-associative table for non-return JALR prediction:

- Use cases: Function pointers, vtables, computed jumps, switch tables
- Entry format: valid, PC tag, rs1_hash (8-bit XOR-fold), target, age counter
- Lookup: PC-only matching in IF stage (rs1 not yet available)
- Update: Full (PC, rs1_hash) matching in ID stage when JALR resolves
- Replacement: LRU-approximated via age counters
- Complements RAS: Handles JALR patterns that are not function returns

Prediction priority (highest to lowest):
1. RAS - for return patterns (JALR rs1=ra/t0, rd=x0)
2. IndirectBTB - for other JALR (function pointers, vtables)
3. BTB - fallback for branches and direct jumps

## Design Notes

- AXI4-Lite replaces direct memory connections with standardized bus protocol
- Bus switch uses address bits [31:29] to route transactions (supports 8 slaves)
- VGA uses dual-clock CDC (system clock + 31.5 MHz pixel clock)
- VGA framebuffer: 12 frames x 64x64 pixels, 4-bit palette indices (8 pixels/word)
- VGA scaling: Fixed-point multiply-shift for 6x zoom (10923/65536 ≈ 1/6)
- All peripherals implement AXI4-Lite slave interface for uniformity
- MemoryAccess uses latched control signals to handle pipeline stall release timing
- CPU.scala latches full bus address during transactions for stable routing

## VGA Display Details

- Resolution: 640x480 @ 72Hz (H_TOTAL=832, V_TOTAL=520)
- Framebuffer: 64x64 pixels, centered and scaled 6x to 384x384 display area
- Colors: 16-entry palette, each entry 6-bit RRGGBB (2 bits per channel)
- Double buffering: 12 frames available, software selects via CTRL register
- Vblank interrupt: Edge-triggered, write-1-to-clear acknowledge

## MyCPU Shell

Interactive bare-metal shell for RISC-V processor inspection and debugging.

### Commands

| Command | Description |
|---------|-------------|
| `help`, `?` | Show available commands |
| `info` | Display CPU architecture (MISA) and memory map |
| `csr` | Show CSR register values (mstatus, misa, mvendorid, etc.) |
| `mem <addr>` | Read memory at address (e.g., `mem 0x20000000`) |
| `memw <addr> <val>` | Write value to memory (e.g., `memw 0x20000020 0x01`) |
| `perf` | Show performance counters (mcycle, minstret, CPI) |
| `clear` | Clear terminal screen |
| `reboot` | Software reset (jump to reset vector) |

### Line Editing

- Backspace: Delete character before cursor
- Ctrl-C: Exit shell (host terminal)

### Example Session

```
MyCPU Shell - Type 'help' for commands
MyCPU> info
MyCPU RISC-V Processor
----------------------
Architecture: RV32I
Vendor ID:    0x00000000
...

MyCPU> perf
Performance Counters:
  mcycle:   1234567
  minstret: 456789
  CPI:      2.70
```

## References

- VGA peripheral adapted from 2-mmio-trap with AXI4-Lite interface
- CPU core from 3-pipeline (5-stage with forwarding)
- Pixel clock division: 31.5 MHz (simulated at 1/4 system clock in Verilator)
