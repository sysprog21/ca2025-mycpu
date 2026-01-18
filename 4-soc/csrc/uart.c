// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

#include "mmio.h"

/* Magic value to signal test completion to simulator */
#define TEST_DONE_MAGIC 0xCAFEF00Du

/*
 * UART Comprehensive Test
 *
 * Tests both UART TX and RX functionality using STATUS register polling:
 * 1. TX: Sends "UART OK\n" message with TX-ready polling
 * 2. RX: Validates loopback reception with three scenarios:
 *    - Multi-byte sequential reception (5-char "HELLO")
 *    - Binary data reception (0x00, 0x01, 0x7F, 0x80, 0xFF)
 *    - Timeout-based polling mechanism
 *
 * Test Result Encoding:
 *   TEST_RESULT bits:
 *     [0]: TX test passed (message sent successfully)
 *     [1]: Multi-byte RX test passed
 *     [2]: Binary RX test passed (includes 0x00 byte)
 *     [3]: Timeout RX test passed
 *   Expected: 0xF (0b1111) = all tests passed
 *
 * Implementation Notes:
 *   - Uses UART_STATUS bit 0 (TX ready) for reliable transmission
 *   - Uses UART_STATUS bit 1 (RX valid) for reliable reception
 *   - Can now reliably receive 0x00 bytes (STATUS-based polling)
 *   - Loopback mode only (test harness internal)
 */

// Helper: Wait for TX ready, then transmit a byte
static inline void uart_putc(unsigned char byte)
{
    while (!(*UART_STATUS & 0x01))
        ;  // Wait for TX buffer ready
    *UART_SEND = (unsigned int) byte;
}

// Receive with timeout using STATUS register
static unsigned char uart_getc_with_timeout(unsigned int timeout,
                                            int *timed_out)
{
    for (unsigned int i = 0; i < timeout; i++) {
        if (*UART_STATUS & 0x02) {  // Check RX valid bit
            if (timed_out)
                *timed_out = 0;  // Success
            return (unsigned char) (*UART_RECV & 0xFF);
        }
        // Small delay between polls
        for (volatile int j = 0; j < 10; j++)
            ;
    }

    if (timed_out)
        *timed_out = 1;  // Timeout
    return 0;
}

// Drain any pending RX data (for loopback cleanup)
static void uart_drain_rx(void)
{
    // Wait for any in-flight TX to complete and loop back
    for (volatile int i = 0; i < 10000; i++)
        ;

    // Drain all pending RX data
    while (*UART_STATUS & 0x02) {
        (void) *UART_RECV;  // Read and discard
        for (volatile int j = 0; j < 100; j++)
            ;  // Brief delay between reads
    }
}

// Test 1: TX - Send message
static unsigned int test_tx(void)
{
    const char message[] = "UART OK\n";
    const char *p = message;

    while (*p)
        uart_putc(*p++);

    // In loopback mode, drain the echoed bytes to avoid polluting RX tests
    uart_drain_rx();

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

        // Receive with timeout (check timed_out to avoid false matches)
        int timed_out = -1;
        char received = (char) uart_getc_with_timeout(1000, &timed_out);

        if (!timed_out && received == sent) {
            success_count++;
        }
    }

    // Return 1 if all 5 characters matched
    return (success_count == 5) ? 1 : 0;
}

// Test 3: Binary data reception (including 0x00)
static unsigned int test_binary_rx(void)
{
    const unsigned char test_bytes[] = {
        0x00,  // Null byte (now testable with STATUS register)
        0x01,  // Lowest non-zero byte
        0x7F,  // Highest ASCII
        0x80,  // Start of extended ASCII
        0xFF   // Highest byte value
    };
    unsigned int success_count = 0;

    for (unsigned int i = 0; i < 5; i++) {
        unsigned char sent = test_bytes[i];

        // Transmit byte
        uart_putc(sent);

        // Wait for loopback propagation
        for (volatile int j = 0; j < 20; j++)
            ;

        // Receive with timeout (check timed_out to avoid false matches)
        int timed_out = -1;
        unsigned char received = uart_getc_with_timeout(1000, &timed_out);

        if (!timed_out && received == sent) {
            success_count++;
        }
    }

    // Return 1 if all 5 bytes matched
    return (success_count == 5) ? 1 : 0;
}

// Test 4: Timeout polling mechanism
static unsigned int test_timeout_rx(void)
{
    int timed_out =
        -1;  // Initialize to detect if uart_getc_with_timeout wrote to it
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
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Warray-bounds"
    *TEST_RESULT = result;
    *TEST_DONE_FLAG = TEST_DONE_MAGIC;
#pragma GCC diagnostic pop

    /* Use wfi (Wait For Interrupt) for power efficiency */
    while (1)
        __asm__ volatile("wfi");
    return 0;
}
