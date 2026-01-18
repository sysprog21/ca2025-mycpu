// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

/**
 * Memory-Mapped I/O Register Definitions
 *
 * This header defines MMIO peripheral base addresses and register offsets
 * for the MyCPU RISC-V processor implementation.
 */

/* Video RAM base address and access pointer */
#define VRAM_BASE 0x20000000
#define VRAM ((volatile unsigned char *) VRAM_BASE)

/* Timer peripheral registers (base: 0x80000000) */
#define TIMER_BASE 0x80000000
/* +0x04: Timer limit register */
#define TIMER_LIMIT ((volatile unsigned int *) (TIMER_BASE + 4))
/* +0x08: Timer enable register */
#define TIMER_ENABLED ((volatile unsigned int *) (TIMER_BASE + 8))

/**
 * UART peripheral registers (base: 0x40000000)
 *
 * Hardware Architecture:
 *   The UART uses ready/valid handshaking internally (Chisel DecoupledIO)
 *   with BufferedTx (single-byte buffer) for transmission and Rx with
 *   interrupt signaling for reception. Status registers are NOT exposed
 *   to software via MMIO, requiring careful driver design.
 *
 * Register Map:
 *   +0x00: Reserved
 *   +0x04: UART_BAUDRATE - Baud rate configuration (read/write)
 *   +0x08: UART_ENABLE   - Enable control and interrupt clear (write)
 *   +0x0C: UART_RECV     - Receive data register (read, clears RX interrupt)
 *   +0x10: UART_SEND     - Transmit data register (write)
 *
 * TX Hardware Behavior and Limitations:
 *   - Architecture: CPU → Buffer (1 byte) → Tx (shift register) → txd pin
 *   - Buffer state: empty (ready) or full (busy)
 *   - CRITICAL LIMITATION: No TX ready status exposed to software
 *   - Write behavior: Writes to UART_SEND when buffer is full are DROPPED
 *   - Hardware does not stall writes or queue beyond single buffer
 *
 *   Software must use one of these strategies:
 *     1. Conservative pacing: Add inter-character delays (see UART_TX_DELAY)
 *     2. Slow baud rates: Ensure buffer drains faster than writes arrive
 *     3. Short messages: Verify total message fits within buffer drain time
 *     4. Hardware modification: Expose tx.io.channel.ready as status bit
 *
 *   For test environments (fast simulation, 8-byte messages), no delay needed.
 *   For production (slow baud, long messages), use UART_TX_DELAY or modify HW.
 *
 * RX Hardware Behavior and Limitations:
 *   - Architecture: rxd pin → Rx (shift register) → rxData register → interrupt
 *   - Interrupt flag set when data received, cleared when UART_RECV read
 *   - CRITICAL LIMITATION: No RX data valid flag exposed to software
 *   - Read behavior: Returns last received byte, or 0 if no data received yet
 *
 *   This creates an ambiguity problem:
 *     - Reading UART_RECV with no data: returns 0
 *     - Reading UART_RECV after receiving 0x00: returns 0
 *     - After first read, hardware clears valid flag, subsequent reads return 0
 *     - Software cannot distinguish "no data" from "valid 0x00 byte"
 *
 *   Polling mode (uart_getc):
 *     - Treats non-zero as valid data, times out on zero
 *     - CANNOT reliably receive 0x00 bytes
 *     - Suitable ONLY for ASCII text transmission (0x01-0xFF)
 *
 *   Interrupt mode (uart_rx_interrupt_handler + uart_getc_nonblocking):
 *     - ISR reads hardware immediately when interrupt fires
 *     - Software buffer stores byte (including 0x00) with separate valid flag
 *     - CAN reliably receive all byte values 0x00-0xFF
 *     - Recommended for binary data transmission
 *
 *   Hardware modification for reliable polling:
 *     Option A: Return valid bit in bit 31, data in bits [7:0]
 *     Option B: Make read block until data valid (requires bus stall support)
 *
 * Read Side Effects:
 *   - UART_RECV: Clears RX interrupt flag on read
 *   - UART_BAUDRATE: No side effects
 *   - Reading when no data available: Returns 0, no interrupt cleared
 *
 * Error Handling Strategy:
 *   - Initialize with uart_init() before any operations
 *   - Check return codes from all UART functions
 *   - Use uart_is_initialized() to verify initialization state
 *   - For ASCII text: Use polling mode (uart_getc)
 *   - For binary data: Use interrupt mode (uart_rx_interrupt_handler)
 *   - For production TX: Configure UART_TX_DELAY or use slow baud rates
 *
 * Recommended Hardware Improvements:
 *   1. Expose tx.io.channel.ready as UART_STATUS bit 0 (TX ready)
 *   2. Expose rx.io.channel.valid as UART_STATUS bit 1 (RX valid)
 *   3. Add UART_STATUS register at offset +0x00
 *   4. Make UART_RECV return 0x100 | data when valid, 0x000 when not
 *   5. Make MMIO bus stall writes when TX buffer full
 */
#define UART_BASE 0x40000000
/* +0x04: Baud rate (R/W) */
#define UART_BAUDRATE ((volatile unsigned int *) (UART_BASE + 4))
/* +0x08: Enable/IRQ clear (W) */
#define UART_ENABLE ((volatile unsigned int *) (UART_BASE + 8))
/* +0x0C: RX data (R) */
#define UART_RECV ((volatile unsigned int *) (UART_BASE + 12))
/* +0x10: TX data (W) */
#define UART_SEND ((volatile unsigned int *) (UART_BASE + 16))

/**
 * UART Usage Examples
 *
 * Example 1: Simple TX (test environment, short ASCII messages)
 *
 *   *UART_BAUDRATE = 115200;
 *   *UART_ENABLE = 1;
 *   *UART_SEND = 'H';
 *   *UART_SEND = 'i';
 *   *UART_SEND = '\n';
 *
 * Example 2: RX polling mode (ASCII only, cannot receive 0x00)
 *
 *   *UART_BAUDRATE = 115200;
 *   *UART_ENABLE = 1;
 *   unsigned int data;
 *   for (int i = 0; i < 1000; i++) {
 *     data = *UART_RECV;
 *     if (data != 0) {
 *       char c = (char)(data & 0xFF);
 *       // Process received character
 *       break;
 *     }
 *   }
 *
 * Example 3: Echo server (loopback test)
 *
 *   *UART_BAUDRATE = 115200;
 *   *UART_ENABLE = 1;
 *   *UART_SEND = 'X';  // Send test character
 *   for (volatile int i = 0; i < 20; i++) ;  // Small delay
 *   unsigned int received = *UART_RECV;  // Read back (loopback)
 *   if ((received & 0xFF) == 'X') {
 *     // Loopback successful
 *   }
 *
 * For comprehensive RX implementations (polling + interrupt-driven),
 * see uart_rx.c reference implementation in csrc/ directory.
 */
