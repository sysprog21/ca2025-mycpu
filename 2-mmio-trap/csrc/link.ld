/* RISC-V Linker Script for MyCPU Bare-Metal Programs
 *
 * This linker script defines the memory layout and section placement for
 * programs running on the MyCPU RISC-V processor. It is designed for
 * bare-metal execution without an operating system.
 *
 * Memory Layout:
 *   0x00000000 - 0x00000FFF : Reserved (trap vectors, boot ROM, etc.)
 *   0x00001000 - 0x003FFFFF : RAM (4MB - 4KB for program code and data)
 *
 * ABI Compliance:
 *   - Follows RISC-V ELF psABI specification
 *   - Entry point: _start (must be in .text.start section)
 *   - Stack grows downward from high memory
 */

/* Entry point for program execution */
ENTRY(_start)

/* Memory region definitions
 * RAM: Read-Write-Execute region for all program sections
 *      Start: 0x00001000 (4KB offset avoids reserved low memory)
 *      Size:  4MB - 4KB = 0x003FF000 bytes
 */
MEMORY
{
    RAM (rwx) : ORIGIN = 0x00001000, LENGTH = 4M - 0x1000
}

SECTIONS
{
    /* .text section: Executable code
     *
     * Layout guarantees:
     *   1. .text.start section placed first (contains _start entry point)
     *   2. All other .text* sections follow
     *
     * This ordering ensures the processor begins execution at _start when
     * jumping to the program's entry address (0x00001000).
     */
    .text : {
        *(.text.start)  /* _start must be first for correct entry point */
        *(.text*)       /* All other code sections */
    } > RAM

    /* .rodata section: Read-only data
     *
     * Contains:
     *   - String literals
     *   - Constant arrays and structures
     *   - Jump tables for switch statements
     *
     * Note: Even though this is read-only data, it's placed in RAM (rwx)
     * because MyCPU does not implement memory protection or separate ROM.
     */
    .rodata ALIGN(4) : {
        *(.rodata*)
    } > RAM

    /* .data section: Initialized global and static variables
     *
     * Contains variables with non-zero initial values. The program loader
     * or startup code is responsible for copying initial values from the
     * binary image to RAM before main() executes.
     */
    .data ALIGN(4) : {
        *(.sdata*)      /* Small data section (GP-relative access) */
        *(.data*)
    } > RAM

    /* .bss section: Zero-initialized global and static variables
     *
     * Contains:
     *   - Variables explicitly initialized to zero
     *   - Uninitialized global/static variables (C standard requires zero-init)
     *   - COMMON blocks (tentative definitions in C)
     *
     * The startup code (_start) must zero this section before calling main().
     * This section occupies no space in the binary image, only in RAM.
     */
    .bss ALIGN(4) : {
        __bss_start = .;
        *(.sbss*)       /* Small BSS section */
        *(.bss*)
        *(COMMON)
        __bss_end = .;
    } > RAM

    /* End of allocated memory - useful for heap/memory management */
    _end = .;
    PROVIDE(end = .);

    /* Stack top symbol - matches start.S stack initialization (0x00400000) */
    PROVIDE(_stack_top = ORIGIN(RAM) + LENGTH(RAM));

    /* Discard unneeded sections to reduce binary size */
    /DISCARD/ : {
        *(.comment)
        *(.eh_frame)
        *(.note*)
    }
}
