# Startup code for bare metal RISC-V
.section .text._start
.globl _start
.type _start, @function

_start:
    # Set up stack pointer
    la sp, __stack_top

    # Clear BSS
    la t0, __bss_start
    la t1, __bss_end
1:
    bge t0, t1, 2f
    sw zero, 0(t0)
    addi t0, t0, 4
    j 1b

2:
    # Call main
    call main

    # Exit syscall (if main returns)
    li a7, 93    # exit syscall number
    li a0, 0     # exit code
    ecall

    # Infinite loop (should never reach here)
3:
    j 3b

.size _start, .-_start

# Provide BSS markers if linker script doesn't define them
.weak __bss_start
.weak __bss_end
.weak __stack_top