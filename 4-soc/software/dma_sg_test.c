// ---------------------------------------------------------
// Type Definitions
// ---------------------------------------------------------
typedef unsigned int uint32_t;
typedef unsigned char uint8_t;

// ---------------------------------------------------------
// Registers
// ---------------------------------------------------------
#define UART_BASE 0x10000000
#define UART_TX ((volatile uint32_t *) (UART_BASE + 0x00))
#define UART_STATUS ((volatile uint32_t *) (UART_BASE + 0x04))
#define UART_BUSY 0x02

#define DMA_BASE 0x10002000
#define DMA_CTRL ((volatile uint32_t *) (DMA_BASE + 0x00))
#define DMA_STATUS ((volatile uint32_t *) (DMA_BASE + 0x04))
#define DMA_HEAD_PTR ((volatile uint32_t *) (DMA_BASE + 0x08))
#define DMA_START 0x01
#define DMA_DONE 0x02

// ---------------------------------------------------------
// Data Structures
// ---------------------------------------------------------
typedef struct {
    uint32_t next_ptr;
    uint32_t src_addr;
    uint32_t dst_addr;
    uint32_t length;
} __attribute__((aligned(16))) dma_desc_t;

// ---------------------------------------------------------
// Helper Functions
// ---------------------------------------------------------
void uart_putc(char c)
{
    while ((*UART_STATUS) & UART_BUSY)
        ;
    *UART_TX = c;
    for (volatile int i = 0; i < 500; i++)
        ;
}

void uart_puts(const char *s)
{
    while (*s)
        uart_putc(*s++);
}

void uart_put_hex(uint32_t val)
{
    const char hex[] = "0123456789ABCDEF";
    uart_puts("0x");
    for (int i = 7; i >= 0; i--) {
        uart_putc(hex[(val >> (i * 4)) & 0xF]);
    }
    uart_putc('\n');
}

// ---------------------------------------------------------
// Main Entry Point
// ---------------------------------------------------------
int main()
{
    // Local buffers to ensure correct memory initialization
    volatile uint32_t src_buf[32] __attribute__((aligned(16)));
    volatile uint32_t dst_buf[32] __attribute__((aligned(16)));
    volatile dma_desc_t desc1 __attribute__((aligned(16)));
    volatile dma_desc_t desc2 __attribute__((aligned(16)));

    // 1. Initialize Data
    for (int i = 0; i < 32; i++) {
        src_buf[i] = 0xAAAA0000 + i;
        dst_buf[i] = 0;
    }

    // 2. Setup Descriptors
    // Descriptor 1
    desc1.src_addr = (uint32_t) src_buf;
    desc1.dst_addr = (uint32_t) dst_buf;
    desc1.length = 16 * 4;
    desc1.next_ptr = (uint32_t) &desc2;

    // Descriptor 2
    desc2.src_addr = (uint32_t) &src_buf[16];
    desc2.dst_addr = (uint32_t) &dst_buf[16];
    desc2.length = 16 * 4;
    desc2.next_ptr = 0;

    asm volatile("fence");

    // 3. Start DMA
    uart_puts("DMA Setup OK. Addr: ");
    uart_put_hex((uint32_t) &desc1);

    uart_puts("Starting DMA...\n");
    *DMA_HEAD_PTR = (uint32_t) &desc1;
    *DMA_CTRL = DMA_START;

    // 4. Wait for Completion
    while (1) {
        if ((*DMA_STATUS) & DMA_DONE)
            break;
    }

    uart_puts("DMA Done!\n");

    // 5. Verify Data
    int err = 0;
    for (int i = 0; i < 32; i++) {
        if (dst_buf[i] != src_buf[i]) {
            err++;
        }
    }

    // 6. Report Result
    if (err == 0) {
        uart_puts("\n\n=======================\n");
        uart_puts("[SUCCESS] Test Passed!\n");
        uart_puts("=======================\n");
    } else {
        uart_puts("\n[FAILED] Error Count: ");
        uart_put_hex(err);
    }

    while (1)
        ;
    return 0;
}