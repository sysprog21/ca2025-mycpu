# Define the text section where executable code resides
.section .text

# Make the _start label visible to the linker as the program entry point
.global _start

_start:
    # Architecture: Stack Initialization
    # Load the address of the top of the stack (defined in link.ld) into the Stack Pointer (sp)
    # This prevents stack overflow/underflow errors during C function calls
    la sp, _stack_top

    # Architecture: Control Flow Transfer
    # Jump to the main function in dma_benchmark.c
    call main

    # Architecture: Safety Barrier
    # Infinite loop to catch the processor if the main function ever returns
    # Prevents execution from running off into invalid memory
hang:
    j hang