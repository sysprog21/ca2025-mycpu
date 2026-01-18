// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

#include "mmio.h"

/* Test validation constants
 * These addresses and values are used by the test suite (CPUTest.scala)
 * to verify trap handler execution and state transitions.
 */
#define TEST_MARKER_ADDR ((volatile unsigned int *) 0x4)
#define TEST_INIT_VALUE 0xDEADBEEF
#define TEST_TRAP_VALUE 0x2022

/* RISC-V mcause register bit fields (Privileged Spec v1.10) */
#define MCAUSE_INTERRUPT_BIT 0x80000000U
#define MCAUSE_CODE_MASK 0x1FU

/* RISC-V interrupt codes (mcause[31]=1, code in bits 4:0) */
#define IRQ_MACHINE_TIMER 7U     /* Machine timer interrupt (MTI) */
#define IRQ_MACHINE_EXTERNAL 11U /* Machine external interrupt (MEI) */

/* RISC-V exception codes (mcause[31]=0, code in bits 4:0) */
#define EXC_ILLEGAL_INST 2U /* Illegal instruction */
#define EXC_BREAKPOINT 3U   /* Breakpoint (ebreak) */
#define EXC_ECALL_MMODE 11U /* Environment call from M-mode */

/* RISC-V instruction sizes */
#define INST_SIZE_BYTES 4U /* RV32I instruction size (32-bit) */

extern void enable_interrupt();

/* Trap handler with proper interrupt acknowledgment
 *
 * Parameters (passed from __trap_entry in init.S):
 *   epc   - Machine exception program counter (mepc) - return address
 *   cause - Machine trap cause (mcause):
 *           bit 31: 1=interrupt, 0=exception
 *           bits 30:0: interrupt/exception code
 *
 * Interrupt codes (mcause[31]=1):
 *   7  - Machine timer interrupt (MTI)
 *   11 - Machine external interrupt (MEI)
 *
 * Exception codes (mcause[31]=0):
 *   2  - Illegal instruction
 *   3  - Breakpoint
 *   11 - Environment call from M-mode (ecall)
 *
 * CRITICAL: Interrupts must be acknowledged to prevent trap storms.
 * Level-triggered interrupts (like Timer) will re-trigger immediately
 * after mret unless the interrupt source is cleared.
 */
void trap_handler(void *epc, unsigned int cause)
{
    /* Write trap marker for test validation */
    *TEST_MARKER_ADDR = TEST_TRAP_VALUE;

    /* Check if trap is an interrupt (bit 31 set) or exception */
    if (cause & MCAUSE_INTERRUPT_BIT) {
        /* Interrupt path - extract interrupt code from bits 4:0 */
        unsigned int irq = cause & MCAUSE_CODE_MASK;

        switch (irq) {
        case IRQ_MACHINE_TIMER:
            /* Machine timer interrupt (MTI)
             * Clear by writing new compare value to TIMER_LIMIT.
             * Timer interrupt is level-triggered: asserted while (count >=
             * limit). Writing 0xFFFFFFFF effectively disables timer interrupts
             * until counter wraps around (~42 seconds at 100MHz).
             */
            *TIMER_LIMIT = 0xFFFFFFFF;
            break;

        case IRQ_MACHINE_EXTERNAL:
            /* Machine external interrupt (MEI)
             * For UART RX: Read UART_RECV register to clear interrupt
             * For PLIC: Implement claim/complete protocol if present
             */
            /* Extensibility hook - add UART/PLIC handling here */
            break;

        default:
            /* Unhandled interrupt - should not occur in current hardware
             * In production: log error or halt system
             */
            break;
        }
    } else {
        /* Exception path - extract exception code from bits 4:0 */
        unsigned int exception = cause & MCAUSE_CODE_MASK;

        switch (exception) {
        case EXC_ECALL_MMODE:
            /* Environment call (ecall) - advance mepc past ecall instruction
             * ecall is 32-bit instruction (4 bytes), increment return address
             * to prevent infinite trap loop on mret.
             */
            *(unsigned int *) epc += INST_SIZE_BYTES;
            break;

        case EXC_ILLEGAL_INST:
        case EXC_BREAKPOINT:
            /* Illegal instruction or breakpoint
             * In production: typically halt or log error
             * In test environments: may use for controlled exception testing
             * Extensibility hook - add debugging/logging here
             */
            break;

        default:
            /* Unhandled exception - should not occur with valid code
             * Examples: load/store misaligned, load/store access fault
             */
            break;
        }
    }
}

int main()
{
    /* Initialize test marker for validation */
    *TEST_MARKER_ADDR = TEST_INIT_VALUE;

    /* Enable machine-mode interrupts (timer, external) */
    enable_interrupt();

    /* Idle loop with power-efficient Wait For Interrupt (WFI)
     * WFI halts CPU until interrupt arrives, saving power.
     * Simulator: NOP equivalent (no power savings)
     * FPGA: Clock-gating capable (30-70% power reduction)
     */
    for (;;)
        __asm__ volatile("wfi");
}
