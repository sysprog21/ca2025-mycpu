#include <stdint.h>

// ==========================================
// 1. Hardware Address Definitions
// ==========================================
#define DMA_BASE_ADDR 0x60000000
#define UART_BASE_ADDR 0x40000000

// DMA Register Offsets
#define DMA_REG_CONTROL 0x00
#define DMA_REG_STATUS 0x04
#define DMA_REG_HEAD 0x08

// Bit Masks
#define DMA_CTRL_START (1 << 0)
#define DMA_STATUS_DONE (1 << 1)
#define DMA_STATUS_BUSY (1 << 0)

// Helper Macros for Register Access
#define DMA_WRITE(offset, val) \
    (*((volatile uint32_t *) (DMA_BASE_ADDR + offset)) = (val))
#define DMA_READ(offset) (*((volatile uint32_t *) (DMA_BASE_ADDR + offset)))
#define UART_TX_REG (*((volatile uint32_t *) (UART_BASE_ADDR)))

// ==========================================
// 2. Data Structures
// ==========================================
// Descriptor structure must match hardware definition
typedef struct {
    uint32_t next_ptr;  // 0x00
    uint32_t src_addr;  // 0x04
    uint32_t dst_addr;  // 0x08
    uint32_t length;    // 0x0C
} dma_descriptor_t;

// ==========================================
// 3. Helper Functions
// ==========================================

// Simple UART print function (polling mode)
void print_str(const char *str)
{
    while (*str) {
        UART_TX_REG = *str++;
        // Add a small delay if needed, but usually hardware FIFO handles it
        for (volatile int i = 0; i < 100; i++)
            ;
    }
}

void print_hex(uint32_t val)
{
    char hex_digits[] = "0123456789ABCDEF";
    char buf[9];
    buf[8] = '\0';
    for (int i = 7; i >= 0; i--) {
        buf[i] = hex_digits[val & 0xF];
        val >>= 4;
    }
    print_str("0x");
    print_str(buf);
}

// ==========================================
// 4. Main Test Program
// ==========================================
// Test Data Buffers (Located in RAM)
#define DATA_LEN 16
uint32_t src_data[DATA_LEN];
uint32_t dst_data[DATA_LEN];

// The Descriptor needs to be in memory too
dma_descriptor_t my_desc;

int main()
{
    print_str("\r\n[DMA Driver] Starting DMA Test...\r\n");

    // --- Step 1: Initialize Data ---
    print_str("[DMA Driver] Initializing Memory...\r\n");
    for (int i = 0; i < DATA_LEN; i++) {
        src_data[i] = 0xAAAA0000 + i;  // Pattern: AAAA0000, AAAA0001...
        dst_data[i] = 0x00000000;      // Clear destination
    }

    // --- Step 2: Configure Descriptor ---
    // Physical address of buffers (in bare-metal, &var is the physical addr)
    my_desc.src_addr = (uint32_t) src_data;
    my_desc.dst_addr = (uint32_t) dst_data;
    my_desc.length = DATA_LEN * 4;  // Length in bytes (16 words * 4 bytes)
    my_desc.next_ptr = 0;           // NULL pointer (last descriptor)

    print_str("[DMA Driver] Descriptor configured.\r\n");
    print_str("   SRC: ");
    print_hex(my_desc.src_addr);
    print_str("\r\n");
    print_str("   DST: ");
    print_hex(my_desc.dst_addr);
    print_str("\r\n");
    print_str("   LEN: ");
    print_hex(my_desc.length);
    print_str("\r\n");

    // --- Step 3: Launch DMA ---
    print_str("[DMA Driver] Writing HEAD_PTR...\r\n");
    DMA_WRITE(DMA_REG_HEAD, (uint32_t) &my_desc);

    print_str("[DMA Driver] Setting START bit...\r\n");
    DMA_WRITE(DMA_REG_CONTROL, DMA_CTRL_START);

    // --- Step 4: Poll Status ---
    print_str("[DMA Driver] Waiting for completion...\r\n");
    while (1) {
        uint32_t status = DMA_READ(DMA_REG_STATUS);
        if (status & DMA_STATUS_DONE) {
            break;
        }
    }
