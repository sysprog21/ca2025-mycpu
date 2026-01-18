// ---------------------------------------------------------
// DMA Performance Benchmark
// ---------------------------------------------------------

// Define fixed-width integer types
typedef unsigned char uint8_t;
typedef unsigned int uint32_t;
typedef unsigned long long uint64_t;

// ---------------------------------------------------------
// Hardware Register Definitions
// ---------------------------------------------------------

// UART Registers (Slave 2)
#define UART_BASE 0x10000000
#define UART_TX ((volatile uint32_t *) (UART_BASE + 0x00))
#define UART_STATUS ((volatile uint32_t *) (UART_BASE + 0x04))
#define UART_BUSY 0x02

// DMA Registers (Slave 3)
#define DMA_BASE 0x10002000
#define DMA_CTRL ((volatile uint32_t *) (DMA_BASE + 0x00))
#define DMA_STATUS ((volatile uint32_t *) (DMA_BASE + 0x04))
#define DMA_HEAD_PTR ((volatile uint32_t *) (DMA_BASE + 0x08))
#define DMA_START 0x01
#define DMA_DONE 0x02

// ---------------------------------------------------------
// Software Arithmetic Helpers
// ---------------------------------------------------------

// Software implementation of unsigned division to avoid linker errors
uint32_t simple_udiv(uint32_t dividend, uint32_t divisor, uint32_t *remainder)
{
    if (divisor == 0)
        return 0;

    uint32_t quotient = 0;
    uint32_t rem = 0;

    for (int i = 31; i >= 0; i--) {
        rem <<= 1;
        rem |= (dividend >> i) & 1;

        if (rem >= divisor) {
            rem -= divisor;
            quotient |= (1 << i);
        }
    }

    if (remainder)
        *remainder = rem;
    return quotient;
}

// ---------------------------------------------------------
// UART Driver
// ---------------------------------------------------------

// Send a single character with busy-wait check to prevent deadlock
void uart_putc(char c)
{
    // Wait until UART TX FIFO is empty
    while ((*UART_STATUS) & UART_BUSY)
        ;

    // Write character to TX register
    *UART_TX = c;

    // Stabilization delay for simulator
    for (volatile int i = 0; i < 500; i++)
        ;
}

// Print a string
void print_str(const char *s)
{
    while (*s) {
        uart_putc(*s++);
    }
}

// Print a decimal integer using software division
void print_dec(uint32_t val)
{
    char buffer[12];
    int i = 0;

    if (val == 0) {
        uart_putc('0');
        return;
    }

    while (val > 0) {
        uint32_t rem;
        val = simple_udiv(val, 10, &rem);
        buffer[i++] = rem + '0';
    }

    while (i > 0) {
        uart_putc(buffer[--i]);
    }
}

// ---------------------------------------------------------
// DMA Controller Logic
// ---------------------------------------------------------

// Descriptor structure (Must be 16-byte aligned)
typedef struct {
    uint32_t next_ptr;
    uint32_t src_addr;
    uint32_t dst_addr;
    uint32_t length;
} __attribute__((aligned(16))) dma_desc_t;

// Read machine cycle counter
uint64_t read_cycles()
{
    uint64_t cycles;
    asm volatile("csrr %0, mcycle" : "=r"(cycles));
    return cycles;
}

// Execute DMA transfer
void dma_transfer(void *dst,
                  void *src,
                  uint32_t len,
                  volatile dma_desc_t *desc_mem)
{
    // Setup descriptor
    desc_mem->src_addr = (uint32_t) src;
    desc_mem->dst_addr = (uint32_t) dst;
    desc_mem->length = len;
    desc_mem->next_ptr = 0;

    // Memory fence
    asm volatile("fence");

    // Start transfer
    *DMA_HEAD_PTR = (uint32_t) desc_mem;
    *DMA_CTRL = DMA_START;

    // Poll for completion
    while (1) {
        if ((*DMA_STATUS) & DMA_DONE)
            break;
    }
}

// ---------------------------------------------------------
// CPU Fallback Logic
// ---------------------------------------------------------

// Standard memcpy using volatile pointers to prevent optimization
void cpu_memcpy(void *dst, void *src, uint32_t len)
{
    volatile uint8_t *d = (volatile uint8_t *) dst;
    volatile uint8_t *s = (volatile uint8_t *) src;
    while (len--) {
        *d++ = *s++;
    }
}

// ---------------------------------------------------------
// Memory Buffers
// ---------------------------------------------------------

uint8_t src_buf[4096] __attribute__((aligned(16)));
uint8_t dst_buf[4096] __attribute__((aligned(16)));
volatile dma_desc_t my_desc __attribute__((aligned(16)));

// ---------------------------------------------------------
// Main Execution
// ---------------------------------------------------------

int main()
{
    // Initial system stabilization delay
    for (volatile int k = 0; k < 10000; k++)
        ;

    // Print Header
    print_str("\n=== DMA Performance Benchmark ===\n");
    print_str("Size(B) | CPU(cyc) | DMA(cyc) | Ratio\n");
    print_str("-------------------------------------\n");

    // Initialize source buffer
    for (int i = 0; i < 4096; i++)
        src_buf[i] = (uint8_t) (i & 0xFF);

    // Define test sizes
    int sizes[] = {64, 128, 256, 512, 1024, 2048, 4096};

    // Benchmark Loop
    for (int i = 0; i < 7; i++) {
        int size = sizes[i];

        // 1. Measure CPU performance
        uint64_t start_cpu = read_cycles();
        cpu_memcpy(dst_buf, src_buf, size);
        uint64_t end_cpu = read_cycles();
        uint32_t cpu_time = (uint32_t) (end_cpu - start_cpu);

        // 2. Measure DMA performance
        uint64_t start_dma = read_cycles();
        dma_transfer(dst_buf, src_buf, size, &my_desc);
        uint64_t end_dma = read_cycles();
        uint32_t dma_time = (uint32_t) (end_dma - start_dma);

        // 3. Calculate Ratio
        uint32_t ratio = 0;
        if (dma_time > 0) {
            uint32_t dummy_rem;
            ratio = simple_udiv(cpu_time * 10, dma_time, &dummy_rem);
        }

        // 4. Output Results
        print_dec(size);
        print_str("\t| ");
        print_dec(cpu_time);
        print_str("\t   | ");
        print_dec(dma_time);
        print_str("\t  | ");

        uint32_t r_int, r_frac;
        r_int = simple_udiv(ratio, 10, &r_frac);

        print_dec(r_int);
        print_str(".");
        print_dec(r_frac);
        print_str("x\n");
    }

    print_str("=== Benchmark Complete ===\n");

    // Halt execution
    while (1)
        ;
    return 0;
}