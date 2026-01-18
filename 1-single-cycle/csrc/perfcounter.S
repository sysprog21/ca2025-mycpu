# Performance counter functions for RISC-V
# Read 64-bit cycle counter

.text

.globl get_cycles
.align 2
get_cycles:
    csrr a1, cycleh
    csrr a0, cycle
    csrr a2, cycleh
    bne a1, a2, get_cycles
    ret

.size get_cycles,.-get_cycles

# Read 64-bit instruction retired counter
.globl get_instret
.align 2
get_instret:
    csrr a1, instreth
    csrr a0, instret
    csrr a2, instreth
    bne a1, a2, get_instret
    ret

.size get_instret,.-get_instret