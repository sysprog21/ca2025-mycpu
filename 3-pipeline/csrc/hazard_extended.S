# SPDX-License-Identifier: MIT
# MyCPU is freely redistributable under the MIT License. See the file
# "LICENSE" for information on usage and redistribution of this file.

# Extended hazard test covering all hazard types
# Tests: RAW, WAW, load-use, store-load, CSR, branch, JAL hazards

.globl _start
_start:
  csrr a0, cycle          # Start cycle count

  # ===== Section 1: WAW (Write-After-Write) Hazard =====
  # Test multiple writes to same register - later write must win
  addi t0, zero, 1        # t0 = 1 (first write)
  addi t0, zero, 2        # t0 = 2 (second write, WAW hazard)
  sw t0, 0x10(zero)       # mem[0x10] = 2 (should store final value)

  # ===== Section 2: Store-Load Forwarding (Memory Hazard) =====
  # Test store followed by load from same address
  li sp, 0x1000           # Initialize stack pointer
  addi t1, zero, 0xAB     # t1 = 0xAB
  sw t1, 0(sp)            # Store t1 to mem[sp]
  lw t2, 0(sp)            # Load from mem[sp] - needs store buffer forwarding
  sw t2, 0x14(zero)       # mem[0x14] = 0xAB (validate forwarding)

  # ===== Section 3: Multiple Consecutive Loads =====
  # Test back-to-back loads with dependencies
  sw zero, 0(sp)          # Initialize mem[sp] = 0
  sw zero, 4(sp)          # Initialize mem[sp+4] = 0
  sw zero, 8(sp)          # Initialize mem[sp+8] = 0
  lw t3, 0(sp)            # Load t3 (load-use hazard on next)
  lw t4, 4(sp)            # Load t4 (load-use hazard on next)
  lw t5, 8(sp)            # Load t5 (load-use hazard on next)
  add t6, t3, t4          # t6 = t3 + t4 (use both loads)
  add a1, t6, t5          # a1 = t6 + t5 (chain dependency)
  sw a1, 0x18(zero)       # mem[0x18] = 0 (sum of zeros)

  # ===== Section 4: Branch Condition RAW Hazard =====
  # Test branch with immediate RAW hazard in condition
  addi a2, zero, 5        # a2 = 5
  beq a2, zero, skip1     # Branch not taken (a2 != 0, immediate RAW)
  addi a3, zero, 10       # a3 = 10 (executed)
  j skip1_end
skip1:
  addi a3, zero, 99       # a3 = 99 (not executed)
skip1_end:
  sw a3, 0x1C(zero)       # mem[0x1C] = 10

  # ===== Section 5: JAL Return Address Hazard =====
  # Test link register immediate use
  jal ra, func            # ra = PC+4, jump to func
  addi a4, ra, 0          # a4 = ra (immediate RAW on link register)
  sw a4, 0x20(zero)       # mem[0x20] = return address
  j section6

func:
  addi a5, zero, 100      # a5 = 100 (function body)
  jalr zero, 0(ra)        # Return to caller

  # ===== Section 6: CSR RAW Hazard =====
section6:
  csrr a6, cycle          # Read cycle CSR
  csrr a7, cycle          # Read cycle CSR again (RAW on CSR)
  sub a7, a7, a6          # a7 = cycle_diff (should be small, ≥1)
  li a6, 0x1888           # a6 = 0x1888 signature
  add a7, a7, a6          # a7 = diff + 0x1888 (mark with signature)
  sw a7, 0x24(zero)       # mem[0x24] = 0x1888 + diff

  # ===== Section 7: Long Dependency Chain =====
  # Test 4-cycle RAW dependency chain
  addi s2, zero, 1        # s2 = 1 (cycle 1)
  addi s3, s2, 1          # s3 = s2 + 1 = 2 (RAW: s2)
  addi s4, s3, 1          # s4 = s3 + 1 = 3 (RAW: s3)
  addi s5, s4, 1          # s5 = s4 + 1 = 4 (RAW: s4)
  add s6, s2, s5          # s6 = s2 + s5 = 1 + 4 = 5 (RAW: s2, s5)
  sw s6, 0x28(zero)       # mem[0x28] = 5

  # ===== Section 8: Forwarding from WB Stage =====
  # Test 2-cycle old data forwarding (MEM/WB → EX)
  addi s7, zero, 7        # s7 = 7
  nop                     # Bubble (s7 in MEM)
  nop                     # Bubble (s7 in WB)
  add s8, s7, zero        # s8 = s7 (forward from WB stage)
  sw s8, 0x2C(zero)       # mem[0x2C] = 7

  # ===== Section 9: Load-to-Store Forwarding =====
  # Test load result used immediately by store
  li s9, 0x1004           # s9 = address
  sw zero, 0(s9)          # Initialize mem[s9] = 0
  lw s10, 0(s9)           # s10 = mem[s9] = 0 (load)
  sw s10, 0x30(zero)      # mem[0x30] = 0 (store uses load result)

  # ===== Section 10: Branch with Multiple RAW =====
  # Test branch condition with both operands having RAW
  addi s11, zero, 3       # s11 = 3
  addi t0, zero, 3        # t0 = 3
  bne s11, t0, skip2      # Branch not taken (RAW on both s11, t0)
  addi t1, zero, 20       # t1 = 20 (executed)
  j skip2_end
skip2:
  addi t1, zero, 30       # t1 = 30 (not executed)
skip2_end:
  sw t1, 0x34(zero)       # mem[0x34] = 20

  # ===== End: Calculate cycle count =====
  csrr a1, cycle          # End cycle count
  sub s0, a1, a0          # s0 = total cycles (result register)

  # ===== Validation Summary =====
  # mem[0x10] = 2 (WAW test)
  # mem[0x14] = 0xAB (store-load test)
  # mem[0x18] = 0 (multi-load test)
  # mem[0x1C] = 10 (branch RAW test)
  # mem[0x20] = return_address (JAL hazard test)
  # mem[0x24] = 0x1888 (CSR hazard test)
  # mem[0x28] = 5 (long chain test)
  # mem[0x2C] = 7 (WB forwarding test)
  # mem[0x30] = 0 (load-store test)
  # mem[0x34] = 20 (branch multi-RAW test)
  # s0 (x8) = cycle count

loop:
  j loop
