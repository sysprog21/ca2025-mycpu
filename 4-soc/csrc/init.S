# MyCPU is freely redistributable under the MIT License. See the file
# LICENSE" for information on usage and redistribution of this file.

# Stack configuration
# Stack starts at 0x00400000 (4MB) and grows down
# Minimum safe SP is 0x00200000 (2MB) to avoid .bss corruption at 0x00100000
# Stack overflow protection triggers if SP drops below this limit
.equ STACK_LIMIT, 0x00200000

.section .text.init
.globl _start
_start:
  # Initialize global pointer for linker relaxation
  .option push
  .option norelax
  la gp, __global_pointer$
  .option pop

  # Initialize stack pointer (4MB - standard across all implementations)
  li sp, 0x00400000

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
  # Use wfi (Wait For Interrupt) for power efficiency
loop:
  wfi
  j loop

# ==============================================================================
# CSR (Control and Status Register) Operations
# ==============================================================================
# Best Practices:
#   - csrs/csrsi: Set specific bits (read-modify-write, preserves other bits)
#   - csrc/csrci: Clear specific bits (read-modify-write, preserves other bits)
#   - csrw/csrrw: Full register write (use only when controlling entire register)
#   - Avoid magic numbers - use explicit bit shifts for clarity
#   - Preserve reserved bits per RISC-V Privileged Spec (WARL contract)
# ==============================================================================

.globl enable_interrupt
enable_interrupt:
  la t0, __trap_entry
  csrrw t1, mtvec, t0      # setup trap vector base
  li t0, (1 << 3)          # MIE bit (Machine Interrupt Enable)
  csrs mstatus, t0         # set MIE without clobbering other bits
  li t0, (1 << 7) | (1 << 11)  # MTIE (bit 7) | MEIE (bit 11)
  csrs mie, t0             # enable timer + external interrupts
  ret
.globl enable_paging
enable_paging:
  # SATP (Supervisor Address Translation and Protection)
  # Bits [31]: MODE = 1 (Sv32 paging enabled)
  # Bits [30:22]: ASID = 0 (Address Space ID)
  # Bits [21:0]: PPN = 0x00005 (Physical Page Number, ppn << 12 = 0x5000)
  li t0, 0x80000005
  csrw satp, t0
  ret
.globl get_mtval
get_mtval:
  csrr a0, mtval
  ret

.globl get_epc
get_epc:
  csrr a0, mepc
  ret
.weak trap_handler
trap_handler:
  ret
__trap_entry:
  csrw mscratch, sp
  addi sp, sp, -128

  # Stack overflow protection: check if SP < STACK_LIMIT
  li t0, STACK_LIMIT
  bltu sp, t0, __stack_overflow_trap

  sw ra, 4(sp)

  sw gp, 12(sp)
  sw tp, 16(sp)
  sw t0, 20(sp)
  sw t1, 24(sp)
  sw t2, 28(sp)
  sw s0, 32(sp)
  sw s1, 36(sp)
  sw a0, 40(sp)
  sw a1, 44(sp)
  sw a2, 48(sp)
  sw a3, 52(sp)
  sw a4, 56(sp)
  sw a5, 60(sp)
  sw a6, 64(sp)
  sw a7, 68(sp)
  sw s2, 72(sp)
  sw s3, 76(sp)
  sw s4, 80(sp)
  sw s5, 84(sp)
  sw s6, 88(sp)
  sw s7, 92(sp)
  sw s8, 96(sp)
  sw s9, 100(sp)
  sw s10, 104(sp)
  sw s11, 108(sp)
  sw t3, 112(sp)
  sw t4, 116(sp)
  sw t5, 120(sp)
  sw t6, 124(sp)

  csrr a0, mepc
  csrr a1, mcause
  call trap_handler

  lw ra, 4(sp)

  lw gp, 12(sp)
  lw tp, 16(sp)
  lw t0, 20(sp)
  lw t1, 24(sp)
  lw t2, 28(sp)
  lw s0, 32(sp)
  lw s1, 36(sp)
  lw a0, 40(sp)
  lw a1, 44(sp)
  lw a2, 48(sp)
  lw a3, 52(sp)
  lw a4, 56(sp)
  lw a5, 60(sp)
  lw a6, 64(sp)
  lw a7, 68(sp)
  lw s2, 72(sp)
  lw s3, 76(sp)
  lw s4, 80(sp)
  lw s5, 84(sp)
  lw s6, 88(sp)
  lw s7, 92(sp)
  lw s8, 96(sp)
  lw s9, 100(sp)
  lw s10, 104(sp)
  lw s11, 108(sp)
  lw t3, 112(sp)
  lw t4, 116(sp)
  lw t5, 120(sp)
  lw t6, 124(sp)
  csrr sp, mscratch
  mret

# Stack overflow trap handler
# Called when SP drops below STACK_LIMIT (0x00200000)
# This indicates stack corruption risk - halt immediately
__stack_overflow_trap:
  # Restore SP from mscratch to prevent further corruption
  csrr sp, mscratch

  # Write diagnostic signature to known address for debugging
  # 0xDEAD5ACF = "DEAD Stack OverFlow"
  li t0, 0xDEAD5ACF
  li t1, 0x00200000
  sw t0, 0(t1)

  # Trigger fatal exception by writing to invalid address
  # This ensures the system halts rather than continuing with corrupted stack
  li t0, 0xFFFFFFFF
  sw zero, 0(t0)

  # Infinite loop as final safety net
  # Use wfi (Wait For Interrupt) for power efficiency
__stack_overflow_halt:
  wfi
  j __stack_overflow_halt
