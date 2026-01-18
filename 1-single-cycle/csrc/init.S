# MyCPU is freely redistributable under the MIT License. See the file
# "LICENSE" for information on usage and redistribution of this file.

.section .text.init
.global _start
_start:
    # Initialize global pointer for linker relaxation
    .option push
    .option norelax
    la gp, __global_pointer$
    .option pop

    # Initialize stack pointer (4KB - fits within 8KB memory model for simulation)
    li sp, 4096

    # Clear .sbss section (small uninitialized data)
    la t0, __sbss_start
    la t1, __sbss_end
sbss_clear_loop:
    bgeu t0, t1, sbss_clear_done
    sw zero, 0(t0)
    addi t0, t0, 4
    j sbss_clear_loop
sbss_clear_done:

    # Clear .bss section (large uninitialized data)
    la t0, __bss_start
    la t1, __bss_end
bss_clear_loop:
    bgeu t0, t1, bss_clear_done
    sw zero, 0(t0)
    addi t0, t0, 4
    j bss_clear_loop
bss_clear_done:

    # Call main function
    call main

    # Halt loop if main returns
loop:
    j loop

