// amoadd-test.c (RV32, requires -march=rv32ima -mabi=ilp32)
#include <stdint.h>

volatile uint32_t amo_var = 1;
volatile uint32_t amo_old = 0;
volatile uint32_t amo_new = 0;

int main(void) {
    uint32_t old;

    // old = amo_var; amo_var += 5 (atomically)
    __asm__ volatile (
        "amoadd.w %0, %2, (%1)\n"
        : "=r"(old)
        : "r"(&amo_var), "r"(5)
        : "memory"
    );

    amo_old = old;
    amo_new = amo_var;
    return 0;
}
