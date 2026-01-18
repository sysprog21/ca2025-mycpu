.section .text
.global _start

_start:
    # 1. Initialize Stack Pointer (SP)
    # Load the address defined as _stack_top in the linker script into the stack pointer register.
    la sp, _stack_top

    # 2. Jump to C Main Function
    # Call the main() function defined in main.c
    call main

    # 3. Infinite Loop (Trap)
    # If main() returns (which it shouldn't), we trap the CPU here.
loop:
    j loop