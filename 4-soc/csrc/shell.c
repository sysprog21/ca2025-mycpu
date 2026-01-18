// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

#include "mmio.h"

/* Bare-metal runtime support - compiler may optimize loops to these */
void *memcpy(void *dest, const void *src, unsigned int n)
{
    char *d = dest;
    const char *s = src;
    while (n--)
        *d++ = *s++;
    return dest;
}

void *memmove(void *dest, const void *src, unsigned int n)
{
    char *d = dest;
    const char *s = src;

    if (d < s) {
        while (n--)
            *d++ = *s++;
    } else {
        d += n;
        s += n;
        while (n--)
            *--d = *--s;
    }
    return dest;
}

void *memset(void *dest, int c, unsigned int n)
{
    unsigned char *d = dest;
    while (n--)
        *d++ = (unsigned char) c;
    return dest;
}

unsigned int strlen(const char *s)
{
    unsigned int len = 0;
    while (*s++)
        len++;
    return len;
}

/* MyCPU Shell - Interactive shell with line editing
 *
 * Features:
 *   - RISC-V processor information commands (info, csr, perf)
 *   - Memory inspection utilities (mem, memw)
 *   - linenoise-style line editing with cursor movement
 *   - Command history with up/down arrow navigation
 *
 * Line Editing Keys:
 *   Backspace     - Delete character before cursor
 *   Delete        - Delete character at cursor
 *   Left/Right    - Move cursor
 *   Home/Ctrl-A   - Move to line start
 *   End/Ctrl-E    - Move to line end
 *   Up/Down       - Navigate command history
 *   Ctrl-U        - Clear entire line
 *   Ctrl-K        - Delete from cursor to end
 *   Ctrl-W        - Delete previous word
 *
 * Commands:
 *   help   - Show available commands with descriptions
 *   info   - Display CPU information and memory map
 *   csr    - Show CSR register values
 *   mem    - Read memory: mem <address>
 *   memw   - Write memory: memw <address> <value>
 *   perf   - Show performance counters (mcycle, minstret, CPI)
 *   clear  - Clear screen
 *   reboot - Software reset
 */

/* Line buffer configuration */
#define LINE_BUF_SIZE 80
#define MAX_ARGS 8

/* Check if TX is ready (buffer can accept data) */
static inline int uart_tx_ready(void)
{
    return (*UART_STATUS & 0x01) != 0;
}

/* Send character with TX ready polling (required to avoid dropped chars) */
static inline void uart_putc(unsigned char c)
{
    while (!uart_tx_ready())
        ; /* Wait for TX buffer ready */
    *UART_SEND = (unsigned int) c;
}

static void uart_puts(const char *s)
{
    while (*s)
        uart_putc((unsigned char) *s++);
}


static inline int uart_rx_ready(void)
{
    return (*UART_STATUS & 0x02) != 0;
}

static unsigned char uart_getc(void)
{
    while (!uart_rx_ready())
        ;
    return (unsigned char) (*UART_RECV & 0xFF);
}

/* String comparison (returns 1 if equal) */
static int str_eq(const char *a, const char *b)
{
    while (*a && *b) {
        if (*a++ != *b++)
            return 0;
    }
    return *a == *b;
}

/* Print unsigned integer in decimal (no hardware division) */
static void print_uint(unsigned int val)
{
    /* Divisors for up to 10 digits (max 4,294,967,295) */
    static const unsigned int divisors[] = {
        1000000000, 100000000, 10000000, 1000000, 100000,
        10000,      1000,      100,      10,      1};
    int started = 0;

    if (val == 0) {
        uart_putc('0');
        return;
    }

    for (int i = 0; i < 10; i++) {
        unsigned int d = divisors[i];
        int digit = 0;

        /* Repeated subtraction instead of division */
        while (val >= d) {
            val -= d;
            digit++;
        }

        if (digit > 0 || started) {
            uart_putc('0' + digit);
            started = 1;
        }
    }
}

/* Print unsigned integer in hexadecimal */
static void print_hex(unsigned int val, int digits)
{
    static const char hex[] = "0123456789abcdef";
    for (int i = digits - 1; i >= 0; i--)
        uart_putc(hex[(val >> (i * 4)) & 0xF]);
}

static void term_clear_screen(void)
{
    uart_puts("\033[2J\033[H");
}

/* CSR read helpers using inline assembly */
static inline unsigned int read_mvendorid(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, mvendorid" : "=r"(val));
    return val;
}

static inline unsigned int read_marchid(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, marchid" : "=r"(val));
    return val;
}

static inline unsigned int read_mimpid(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, mimpid" : "=r"(val));
    return val;
}

static inline unsigned int read_mhartid(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, mhartid" : "=r"(val));
    return val;
}

static inline unsigned int read_misa(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, misa" : "=r"(val));
    return val;
}

static inline unsigned int read_mstatus(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, mstatus" : "=r"(val));
    return val;
}

static inline unsigned int read_mcycle(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, mcycle" : "=r"(val));
    return val;
}

static inline unsigned int read_minstret(void)
{
    unsigned int val;
    __asm__ volatile("csrr %0, minstret" : "=r"(val));
    return val;
}

/* Parse hexadecimal string to integer */
static unsigned int parse_hex(const char *s)
{
    unsigned int val = 0;

    /* Skip optional 0x prefix */
    if (s[0] == '0' && (s[1] == 'x' || s[1] == 'X'))
        s += 2;

    while (*s) {
        char c = *s++;
        unsigned int digit;
        if (c >= '0' && c <= '9')
            digit = c - '0';
        else if (c >= 'a' && c <= 'f')
            digit = c - 'a' + 10;
        else if (c >= 'A' && c <= 'F')
            digit = c - 'A' + 10;
        else
            break;
        val = (val << 4) | digit;
    }
    return val;
}

/* Global line buffer - word-aligned for proper memory access */
static unsigned int line_buf_words[LINE_BUF_SIZE / 4 + 1];
#define line_buf ((char *) line_buf_words)

/* Write a byte to line_buf using word-aligned read-modify-write */
static void line_buf_write_byte(int index, unsigned char val)
{
    int word_idx = index >> 2;       /* index / 4 */
    int byte_pos = (index & 3) << 3; /* (index % 4) * 8 */
    unsigned int mask = ~(0xFF << byte_pos);
    unsigned int word = line_buf_words[word_idx];
    word = (word & mask) | ((unsigned int) val << byte_pos);
    line_buf_words[word_idx] = word;
}

/* Read line with backspace support - uses word-aligned memory access */
static int read_line(void)
{
    unsigned char c;
    int len = 0;

    while (1) {
        c = uart_getc();

        if (c == '\r' || c == '\n') {
            line_buf_write_byte(len, '\0');
            uart_puts("\r\n");
            return len;
        } else if (c == 0x7F || c == 0x08) {
            /* Backspace */
            if (len > 0) {
                len--;
                uart_puts("\b \b");
            }
        } else if (c >= 0x20 && c < 0x7F && len < LINE_BUF_SIZE - 1) {
            /* Printable character - use word-aligned write */
            line_buf_write_byte(len, c);
            len++;
            uart_putc(c);
        }
        /* Ignore escape sequences and control chars */
    }
}

/* Parse command line into arguments - uses word-aligned writes for null
 * terminators */
static int parse_args(char *line, char *argv[])
{
    int argc = 0;
    int idx = 0;

    while (line[idx] && argc < MAX_ARGS) {
        /* Skip leading whitespace */
        while (line[idx] == ' ')
            idx++;

        if (line[idx] == '\0')
            break;

        /* Start of argument */
        argv[argc++] = &line[idx];

        /* Find end of argument */
        while (line[idx] && line[idx] != ' ')
            idx++;

        /* Null-terminate argument using word-aligned write */
        if (line[idx]) {
            line_buf_write_byte(idx, '\0');
            idx++;
        }
    }

    return argc;
}

/* Command: help */
static void cmd_help(void)
{
    uart_puts("MyCPU Shell Commands:\r\n");
    uart_puts("\r\n");
    uart_puts("  help, ?      Show this help message\r\n");
    uart_puts("  info         Display CPU architecture and memory map\r\n");
    uart_puts("  csr          Show CSR register values\r\n");
    uart_puts("  mem <addr>   Read memory (e.g., mem 0x20000000)\r\n");
    uart_puts("  memw <a> <v> Write memory (e.g., memw 0x20000020 0x01)\r\n");
    uart_puts("  perf         Show performance counters\r\n");
    uart_puts("  clear        Clear screen\r\n");
    uart_puts("  reboot       Software reset\r\n");
    uart_puts("\r\n");
    uart_puts("Backspace supported for line editing.\r\n");
}

/* Command: info - Display RISC-V processor information */
static void cmd_info(void)
{
    unsigned int misa = read_misa();

    uart_puts("MyCPU RISC-V Processor\r\n");
    uart_puts("----------------------\r\n");

    /* Architecture */
    uart_puts("Architecture: RV32");
    if (misa & (1 << ('I' - 'A')))
        uart_putc('I');
    if (misa & (1 << ('M' - 'A')))
        uart_putc('M');
    if (misa & (1 << ('A' - 'A')))
        uart_putc('A');
    if (misa & (1 << ('F' - 'A')))
        uart_putc('F');
    if (misa & (1 << ('D' - 'A')))
        uart_putc('D');
    if (misa & (1 << ('C' - 'A')))
        uart_putc('C');
    uart_puts("\r\n");

    /* Vendor info */
    uart_puts("Vendor ID:    0x");
    print_hex(read_mvendorid(), 8);
    uart_puts("\r\n");
    uart_puts("Architecture: 0x");
    print_hex(read_marchid(), 8);
    uart_puts("\r\n");
    uart_puts("Implement ID: 0x");
    print_hex(read_mimpid(), 8);
    uart_puts("\r\n");
    uart_puts("Hart ID:      ");
    print_uint(read_mhartid());
    uart_puts("\r\n");

    /* Memory map */
    uart_puts("\r\nMemory Map:\r\n");
    uart_puts("  0x00000000  Main Memory (2MB)\r\n");
    uart_puts("  0x20000000  VGA Controller\r\n");
    uart_puts("  0x40000000  UART\r\n");
}

/* Command: csr - Display CSR values */
static void cmd_csr(void)
{
    uart_puts("CSR Registers:\r\n");
    uart_puts("  mstatus:   0x");
    print_hex(read_mstatus(), 8);
    uart_puts("\r\n");
    uart_puts("  misa:      0x");
    print_hex(read_misa(), 8);
    uart_puts("\r\n");
    uart_puts("  mvendorid: 0x");
    print_hex(read_mvendorid(), 8);
    uart_puts("\r\n");
    uart_puts("  marchid:   0x");
    print_hex(read_marchid(), 8);
    uart_puts("\r\n");
    uart_puts("  mimpid:    0x");
    print_hex(read_mimpid(), 8);
    uart_puts("\r\n");
    uart_puts("  mhartid:   ");
    print_uint(read_mhartid());
    uart_puts("\r\n");
}

/* Command: mem - Read memory */
static void cmd_mem(int argc, char *argv[])
{
    if (argc < 2) {
        uart_puts("Usage: mem <address>\r\n");
        uart_puts("Example: mem 0x20000000\r\n");
        return;
    }

    unsigned int addr = parse_hex(argv[1]);

    /* Align to 4-byte boundary */
    addr &= ~0x3;

    uart_puts("0x");
    print_hex(addr, 8);
    uart_puts(": 0x");
    print_hex(*(volatile unsigned int *) addr, 8);
    uart_puts("\r\n");
}

/* Command: memw - Write memory */
static void cmd_memw(int argc, char *argv[])
{
    if (argc < 3) {
        uart_puts("Usage: memw <address> <value>\r\n");
        uart_puts("Example: memw 0x20000020 0x01\r\n");
        return;
    }

    unsigned int addr = parse_hex(argv[1]);
    unsigned int val = parse_hex(argv[2]);

    /* Align to 4-byte boundary */
    addr &= ~0x3;

    *(volatile unsigned int *) addr = val;

    uart_puts("Wrote 0x");
    print_hex(val, 8);
    uart_puts(" to 0x");
    print_hex(addr, 8);
    uart_puts("\r\n");
}

/* Unsigned division using repeated subtraction (no M extension) */
static unsigned int udiv(unsigned int num, unsigned int den)
{
    if (den == 0)
        return 0;

    unsigned int quot = 0;
    while (num >= den) {
        num -= den;
        quot++;
    }
    return quot;
}

/* Unsigned modulo using repeated subtraction (no M extension) */
static unsigned int umod(unsigned int num, unsigned int den)
{
    if (den == 0)
        return 0;

    while (num >= den)
        num -= den;
    return num;
}

/* Command: perf - Show performance counters */
static void cmd_perf(void)
{
    unsigned int cycles = read_mcycle();
    unsigned int instret = read_minstret();

    uart_puts("Performance Counters:\r\n");
    uart_puts("  mcycle:   ");
    print_uint(cycles);
    uart_puts("\r\n");
    uart_puts("  minstret: ");
    print_uint(instret);
    uart_puts("\r\n");

    if (instret > 0) {
        unsigned int cpi_int = udiv(cycles, instret);
        unsigned int cpi_frac = umod(udiv(cycles * 100, instret), 100);

        uart_puts("  CPI:      ");
        print_uint(cpi_int);
        uart_puts(".");
        if (cpi_frac < 10)
            uart_putc('0');
        print_uint(cpi_frac);
        uart_puts("\r\n");
    }
}

/* Command: reboot - Software reset */
static void cmd_reboot(void)
{
    uart_puts("Rebooting...\r\n");

    /* Jump to reset vector */
    __asm__ volatile("jr zero");
}

/* Process command */
static void process_command(char *line)
{
    char *argv[MAX_ARGS];
    int argc = parse_args(line, argv);

    if (argc == 0)
        return;

    if (str_eq(argv[0], "help") || str_eq(argv[0], "?")) {
        cmd_help();
    } else if (str_eq(argv[0], "info")) {
        cmd_info();
    } else if (str_eq(argv[0], "csr")) {
        cmd_csr();
    } else if (str_eq(argv[0], "mem")) {
        cmd_mem(argc, argv);
    } else if (str_eq(argv[0], "memw")) {
        cmd_memw(argc, argv);
    } else if (str_eq(argv[0], "perf")) {
        cmd_perf();
    } else if (str_eq(argv[0], "clear") || str_eq(argv[0], "cls")) {
        term_clear_screen();
    } else if (str_eq(argv[0], "reboot") || str_eq(argv[0], "reset")) {
        cmd_reboot();
    } else {
        uart_puts("Unknown command: ");
        uart_puts(argv[0]);
        uart_puts("\r\nType 'help' for available commands.\r\n");
    }
}

int main(void)
{
    /* Clear line buffer at startup (ensures clean state) */
    for (int i = 0; i < LINE_BUF_SIZE / 4 + 1; i++)
        line_buf_words[i] = 0;

    /* Initialize UART */
    *UART_ENABLE = 1;

    uart_puts("\r\nMyCPU Shell - Type 'help' for commands\r\n");

    while (1) {
        uart_puts("MyCPU> ");
        int len = read_line();
        if (len > 0)
            process_command(line_buf);
    }

    return 0;
}
