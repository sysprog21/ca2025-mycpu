// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

#ifndef MMIO_H
#define MMIO_H

#include <stdint.h>

/**
 * Memory-Mapped I/O Register Definitions
 *
 * This header defines MMIO peripheral base addresses and register offsets
 * for the MyCPU RISC-V processor implementation.
 */

/**
 * VGA peripheral registers (base: 0x20000000)
 *
 * Register Map:
 *   +0x00: VGA_ID          - Peripheral ID (RO: 0x56474131 = 'VGA1')
 *   +0x04: VGA_STATUS      - Vblank status, safe to swap, upload busy
 *   +0x08: VGA_INTR_STATUS - Vblank interrupt flag (W1C)
 *   +0x10: VGA_UPLOAD_ADDR - Framebuffer upload address (nibble index + frame)
 *   +0x14: VGA_STREAM_DATA - 8 pixels packed in 32-bit word (auto-increment)
 *   +0x20: VGA_CTRL        - Display enable, blank, swap request, frame select
 *   +0x24-0x60: VGA_PALETTE[0-15] - 6-bit VGA colors (RRGGBB)
 *
 * Two access patterns are supported:
 *   1. Pointer-based (direct dereference): *VGA_CTRL = 0x01;
 *   2. Address-based (function access): vga_write32(VGA_ADDR_CTRL, 0x01);
 */
#define VGA_BASE 0x20000000u

/* Address-only macros for function-based access (vga_write32/vga_read32) */
#define VGA_ADDR_ID (VGA_BASE + 0x00)
#define VGA_ADDR_STATUS (VGA_BASE + 0x04)
#define VGA_ADDR_INTR_STATUS (VGA_BASE + 0x08)
#define VGA_ADDR_UPLOAD_ADDR (VGA_BASE + 0x10)
#define VGA_ADDR_STREAM_DATA (VGA_BASE + 0x14)
#define VGA_ADDR_CTRL (VGA_BASE + 0x20)
#define VGA_ADDR_PALETTE(n) (VGA_BASE + 0x24 + ((n) << 2))

/* Pointer macros for direct dereference access */
#define VGA_ID ((volatile uint32_t *) (VGA_BASE + 0x00))          /* RO */
#define VGA_STATUS ((volatile uint32_t *) (VGA_BASE + 0x04))      /* RO */
#define VGA_INTR_STATUS ((volatile uint32_t *) (VGA_BASE + 0x08)) /* W1C */
#define VGA_UPLOAD_ADDR ((volatile uint32_t *) (VGA_BASE + 0x10)) /* R/W */
#define VGA_STREAM_DATA ((volatile uint32_t *) (VGA_BASE + 0x14)) /* WO */
#define VGA_CTRL ((volatile uint32_t *) (VGA_BASE + 0x20))        /* R/W */
#define VGA_PALETTE_BASE (VGA_BASE + 0x24)
#define VGA_PALETTE(n) ((volatile uint32_t *) (VGA_PALETTE_BASE + ((n) << 2)))

/* VGA framebuffer constants */
#define VGA_FRAME_WIDTH 64
#define VGA_FRAME_HEIGHT 64
#define VGA_FRAME_SIZE (VGA_FRAME_WIDTH * VGA_FRAME_HEIGHT)
#define VGA_PIXELS_PER_WORD 8
#define VGA_WORDS_PER_FRAME (VGA_FRAME_SIZE / VGA_PIXELS_PER_WORD)
#define VGA_NUM_FRAMES 12
#define VGA_EXPECTED_ID 0x56474131u /* 'VGA1' */

/* VGA MMIO access helper functions */
static inline void vga_write32(uint32_t addr, uint32_t val)
{
    *(volatile uint32_t *) addr = val;
    __asm__ volatile("" ::
                         : "memory"); /* Compiler barrier: prevent reordering */
}

static inline uint32_t vga_read32(uint32_t addr)
{
    uint32_t val = *(volatile uint32_t *) addr;
    __asm__ volatile("" ::
                         : "memory"); /* Compiler barrier: prevent reordering */
    return val;
}

/* Pack 8 4-bit pixels into a 32-bit word for VGA framebuffer upload */
static inline uint32_t vga_pack8_pixels(const uint8_t *pixels)
{
    return (uint32_t) (pixels[0] & 0xF) | ((uint32_t) (pixels[1] & 0xF) << 4) |
           ((uint32_t) (pixels[2] & 0xF) << 8) |
           ((uint32_t) (pixels[3] & 0xF) << 12) |
           ((uint32_t) (pixels[4] & 0xF) << 16) |
           ((uint32_t) (pixels[5] & 0xF) << 20) |
           ((uint32_t) (pixels[6] & 0xF) << 24) |
           ((uint32_t) (pixels[7] & 0xF) << 28);
}


/* Timer peripheral registers (base: 0x80000000) */
#define TIMER_BASE 0x80000000u
#define TIMER_LIMIT ((volatile uint32_t *) (TIMER_BASE + 0x04))   /* R/W */
#define TIMER_ENABLED ((volatile uint32_t *) (TIMER_BASE + 0x08)) /* R/W */

/**
 * UART peripheral registers (base: 0x40000000)
 *
 * Hardware Architecture:
 *   The UART uses ready/valid handshaking internally (Chisel DecoupledIO)
 *   with BufferedTx (single-byte buffer) for transmission and Rx with
 *   interrupt signaling for reception.
 *
 * Register Map:
 *   +0x00: UART_STATUS    - Status register (read-only)
 *                           bit 0: TX ready (buffer can accept data)
 *                           bit 1: RX valid (received data available)
 *   +0x04: UART_BAUDRATE  - Configured baud rate (read-only, compile-time)
 *   +0x08: UART_INTERRUPT - Interrupt enable (write: non-zero enables)
 *   +0x0C: UART_RECV      - Receive data register (read clears interrupt)
 *   +0x10: UART_SEND      - Transmit data register (write-only)
 *
 * TX Operation:
 *   - Architecture: CPU → Buffer (1 byte) → Tx (shift register) → txd pin
 *   - Poll UART_STATUS bit 0 before writing to ensure buffer ready
 *   - Writes when buffer full are silently dropped
 *
 * RX Operation:
 *   - Architecture: rxd pin → Rx (shift register) → rxData register
 *   - Poll UART_STATUS bit 1 to check for valid data
 *   - Reading UART_RECV clears the RX interrupt flag
 *   - Can reliably receive all byte values 0x00-0xFF using STATUS polling
 *
 * Limitations:
 *   - Baud rate is fixed at compile time (read-only via UART_BAUDRATE)
 *   - Single-byte TX buffer: poll STATUS before each write
 *   - Single-byte RX buffer: read promptly to avoid overrun
 *
 * Usage Pattern:
 *   // TX: Wait for ready, then send
 *   while (!(*UART_STATUS & 0x01)) ;  // Wait for TX ready
 *   *UART_SEND = byte;
 *
 *   // RX: Wait for valid, then read
 *   while (!(*UART_STATUS & 0x02)) ;  // Wait for RX valid
 *   byte = *UART_RECV;
 */
#define UART_BASE 0x40000000u
#define UART_STATUS ((volatile uint32_t *) (UART_BASE + 0x00))    /* RO */
#define UART_BAUDRATE ((volatile uint32_t *) (UART_BASE + 0x04))  /* RO */
#define UART_INTERRUPT ((volatile uint32_t *) (UART_BASE + 0x08)) /* WO */
#define UART_RECV ((volatile uint32_t *) (UART_BASE + 0x0C))      /* RO */
#define UART_SEND ((volatile uint32_t *) (UART_BASE + 0x10))      /* WO */

/* Legacy alias for backward compatibility */
#define UART_ENABLE UART_INTERRUPT

/**
 * UART Usage Examples
 *
 * Example 1: Reliable TX with STATUS polling
 *
 *   *UART_BAUDRATE = 115200;
 *   *UART_ENABLE = 1;
 *   const char *msg = "Hello\n";
 *   while (*msg) {
 *     while (!(*UART_STATUS & 0x01)) ;  // Wait for TX ready
 *     *UART_SEND = *msg++;
 *   }
 *
 * Example 2: Reliable RX with STATUS polling (handles all bytes 0x00-0xFF)
 *
 *   *UART_BAUDRATE = 115200;
 *   *UART_ENABLE = 1;
 *   while (!(*UART_STATUS & 0x02)) ;  // Wait for RX valid
 *   unsigned char byte = (unsigned char) (*UART_RECV & 0xFF);
 *
 * Example 3: Echo server (loopback test)
 *
 *   *UART_BAUDRATE = 115200;
 *   *UART_ENABLE = 1;
 *   while (!(*UART_STATUS & 0x01)) ;  // Wait for TX ready
 *   *UART_SEND = 'X';
 *   while (!(*UART_STATUS & 0x02)) ;  // Wait for RX valid
 *   unsigned int received = *UART_RECV;
 *   if ((received & 0xFF) == 'X') {
 *     // Loopback successful
 *   }
 */

/* Test harness registers (simulation only) */
/* Cast via uintptr_t to suppress -Warray-bounds warning at -O2 */
#define TEST_DONE_FLAG ((volatile uint32_t *) (uintptr_t) 0x100)
#define TEST_RESULT ((volatile uint32_t *) (uintptr_t) 0x104)

#endif /* MMIO_H */
