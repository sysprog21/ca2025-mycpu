# JIT test - RISC-V self-modifying code demonstration

# This simulates real JIT behavior:
# 1. Instructions stored as hex data (like the C uint32_t array)
# 2. Copy them to executable memory at runtime
# 3. Jump to the copied code and execute

.data
# The "compiled" JIT instructions, stored as hex data
# This corresponds to: uint32_t instructions[] = {...};
jit_instructions:
    .word 0x02a00513    # addi a0, zero, 42
    .word 0x00008067    # jalr zero, ra, 0 (ret)

.text
.globl _start

_start:
    # Simulate JIT: copy instructions from data to executable code buffer
    # This is what a real JIT does after compiling
    la t0, jit_instructions     # Source: hex data
    la t1, jit_code_buffer      # Destination: executable buffer

    # Copy first instruction
    lw t2, 0(t0)
    sw t2, 0(t1)

    # Copy second instruction
    lw t2, 4(t0)
    sw t2, 4(t1)

    # Now execute the JIT code (calling the "function")
    la t0, jit_code_buffer
    jalr ra, t0, 0

    # Returns here with a0 = 42
    # Test verification reads a0 register directly via debug interface

# Executable code buffer where JIT instructions are copied to
# Pre-filled with nops (0x00000013), will be overwritten at runtime
jit_code_buffer:
    .word 0x00000013    # nop (addi x0, x0, 0)
    .word 0x00000013    # nop (addi x0, x0, 0)

# Expected result:
# - Register a0 = 42 after JIT code execution
# - Test verification reads a0 directly via debug interface (no ecall)
# - This demonstrates the full JIT cycle:
#   1. Store instructions as data (hex encoding)
#   2. Copy to executable memory
#   3. Execute the copied code
#   4. Verify result in target register
