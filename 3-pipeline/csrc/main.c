typedef unsigned int size_t;
typedef unsigned long long uint64_t;
typedef int int32_t;

void *memcpy(void *dest, const void *src, size_t n) {
    char *d = (char *)dest;
    const char *s = (const char *)src;
    while (n--) {
        *d++ = *s++;
    }
    return dest;
}

#include "perfcounter.h"

#define SYS_WRITE 64
#define STDOUT 1

size_t strlen(const char *s)
{
    const char *p = s;
    while (*p)
        p++;
    return (size_t)(p - s);
}

int puts(const char *s)
{
    register int a0_ret asm("a0");
    while (*s) {
        register int a0 asm("a0") = STDOUT;
        register const char *a1 asm("a1") = s;
        register size_t a2 asm("a2") = 1;
        register int a7 asm("a7") = SYS_WRITE;

        asm volatile("ecall"
                     : "+r"(a0)
                     : "r"(a1), "r"(a2), "r"(a7)
                     : "memory");
        s++;
    }

    const char *newline = "\n";
    register int a0_nl asm("a0") = STDOUT;
    register const char *a1_nl asm("a1") = newline;
    register size_t a2_nl asm("a2") = 1;
    register int a7_nl asm("a7") = SYS_WRITE;

    asm volatile("ecall"
                 : "=r"(a0_ret)
                 : "r"(a0_nl), "r"(a1_nl), "r"(a2_nl), "r"(a7_nl)
                 : "memory");

    return a0_ret;
}

void print_char(char c) {
    register int a0 asm("a0") = STDOUT;
    register const char *a1 asm("a1") = &c;
    register size_t a2 asm("a2") = 1;
    register int a7 asm("a7") = SYS_WRITE;

    asm volatile("ecall"
                 : "+r"(a0)
                 : "r"(a1), "r"(a2), "r"(a7)
                 : "memory");
}

void print_uint64(uint64_t n) {
    if (n == 0) {
        print_char('0');
        return;
    }

    char buf[20];
    int i = 0;
    while (n > 0) {
        buf[i++] = (n % 10) + '0';
        n /= 10;
    }

    for (int j = i - 1; j >= 0; j--) {
        print_char(buf[j]);
    }
}

void print_int(int n) {
    if (n < 0) {
        print_char('-');
        n = -n;
    }
    print_uint64((uint64_t)n);
}

extern int hw1_function(void);
extern int sine_approx_main(void);

int main(void) {

    puts("--- Assignment 2 Results ---");

    uint64_t start_cycles, end_cycles;
    uint64_t total_cycles_hw1, total_cycles_sine;
    int hw1_result, sine_result;

    start_cycles = get_cycles();
    hw1_result = hw1_function();
    end_cycles = get_cycles();
    total_cycles_hw1 = end_cycles - start_cycles;

    start_cycles = get_cycles();
    sine_result = sine_approx_main();
    end_cycles = get_cycles();
    total_cycles_sine = end_cycles - start_cycles;

    puts("[Homework 1 Function (Assembly)]");
    puts("  Result (from a0): ");
    print_int(hw1_result);
    puts("");
    puts("  Total Cycles: ");
    print_uint64(total_cycles_hw1);
    puts("");

    puts("\n[Sine Approx (Quiz 3) Function (C)]");
    puts("  Result (return value): ");
    print_int(sine_result);
    puts("");
    puts("  Total Cycles: ");
    print_uint64(total_cycles_sine);
    puts("");

    return 0;
}