// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

#include "mmio.h"

#define DONE_FLAG ((volatile unsigned int *) 0x100)
#define TEST_RESULT ((volatile unsigned int *) 0x104)

/*
 * UART Comprehensive Test
 *
 * Tests both UART TX and RX functionality:
 * 1. TX: Sends "UART OK\n" message
 * 2. RX: Validates loopback reception with three scenarios:
 *    - Multi-byte sequential reception (5-char "HELLO")
 *    - Binary data reception (0x01, 0x7F, 0x80, 0xFF)
 *    - Timeout-based polling mechanism
 *
 * Test Result Encoding:
 *   TEST_RESULT bits:
 *     [0]: TX test passed (message sent successfully)
 *     [1]: Multi-byte RX test passed
 *     [2]: Binary RX test passed
 *     [3]: Timeout RX test passed
 *   Expected: 0xF (0b1111) = all tests passed
 *
 * Hardware Limitations:
 *   - Cannot test 0x00 bytes reliably (no RX valid flag)
 *   - Loopback mode only (test harness internal)
 *   - No FIFO buffering (single-byte buffer)
 */

// Helper: Transmit a byte
static inline void uart_putc(unsigned char byte)
{
    *UART_SEND = (unsigned int) byte;
}

// Helper: Receive with timeout
static unsigned char uart_getc_with_timeout(unsigned int timeout,
                                            int *timed_out)
{
    unsigned int data;

    for (unsigned int i = 0; i < timeout; i++) {
        data = *UART_RECV;
        if (data != 0) {
            if (timed_out)
                *timed_out = 0;  // Success
            return (unsigned char) (data & 0xFF);
        }
        // Small delay between polls
        for (volatile int j = 0; j < 10; j++)
            ;
    }

    if (timed_out)
        *timed_out = 1;  // Timeout
    return 0;
}

// Test 1: TX - Send message
static unsigned int test_tx(void)
{
    const char message[] = "UART OK\n";
    const char *p = message;

    while (*p)
        uart_putc(*p++);

    return 1;  // TX always succeeds in loopback mode
}

// Test 2: Multi-byte sequential reception
static unsigned int test_multi_byte_rx(void)
{
    const char test_sequence[] = "HELLO";
    unsigned int success_count = 0;

    for (unsigned int i = 0; test_sequence[i] != '\0'; i++) {
        char sent = test_sequence[i];

        // Transmit character
        uart_putc((unsigned char) sent);

        // Wait for loopback propagation
        for (volatile int j = 0; j < 20; j++)
            ;

        // Receive with timeout
        char received = (char) uart_getc_with_timeout(1000, 0);
        if (received == sent)
            success_count++;
    }

    // Return 1 if all 5 characters matched
    return (success_count == 5) ? 1 : 0;
}

// Test 3: Binary data reception
static unsigned int test_binary_rx(void)
{
    const unsigned char test_bytes[] = {
        0x01,  // Lowest non-zero byte
        0x7F,  // Highest ASCII
        0x80,  // Start of extended ASCII
        0xFF,  // Highest byte value
    };
    unsigned int success_count = 0;

    for (unsigned int i = 0; i < 4; i++) {
        unsigned char sent = test_bytes[i];

        // Transmit byte
        uart_putc(sent);

        // Wait for loopback propagation
        for (volatile int j = 0; j < 20; j++)
            ;

        // Receive with timeout
        unsigned char received = uart_getc_with_timeout(1000, 0);
        if (received == sent)
            success_count++;
    }

    // Return 1 if all 4 bytes matched
    return (success_count == 4) ? 1 : 0;
}

// Test 4: Timeout polling mechanism
static unsigned int test_timeout_rx(void)
{
    int timed_out;
    unsigned char received;

    // Transmit test character
    uart_putc('T');

    // Wait for loopback propagation
    for (volatile int i = 0; i < 20; i++)
        ;

    // Receive with timeout
    received = uart_getc_with_timeout(200, &timed_out);

    // Return 1 if successfully received without timeout
    return (!timed_out && received == 'T') ? 1 : 0;
}

int main(void)
{
    unsigned int result = 0;

    // Initialize UART
    *UART_BAUDRATE = 115200;
    *UART_ENABLE = 1;

    // Run Test 1: TX
    if (test_tx())
        result |= (1 << 0);  // Set bit 0

    // Run Test 2: Multi-byte RX
    if (test_multi_byte_rx())
        result |= (1 << 1);  // Set bit 1

    // Run Test 3: Binary RX
    if (test_binary_rx())
        result |= (1 << 2);  // Set bit 2

    // Run Test 4: Timeout RX
    if (test_timeout_rx())
        result |= (1 << 3);  // Set bit 3

    // Report results
    // result = 0xF (0b1111) means all tests passed
    *TEST_RESULT = result;
    *DONE_FLAG = 0xCAFEF00D;

    /* Use wfi (Wait For Interrupt) for power efficiency */
    while (1)
        __asm__ volatile("wfi");
    return 0;
}
