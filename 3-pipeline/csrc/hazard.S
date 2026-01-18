# SPDX-License-Identifier: MIT
# MyCPU is freely redistributable under the MIT License. See the file
# "LICENSE" for information on usage and redistribution of this file.

.globl _start
_start:
  csrr a0, cycle

  addi t0, zero, 1  
  sub t1, zero, t0  
  and t2, t0, t1    
  sw t2, 4(zero)    
  j skip1
  or t2, t0, t1
  xor t2, t0, t1

skip1:
  addi t1, t2, 1    
  add t2, t1, t2    
  and t2, t1, t2    
  lw t2, 2(t2)      
  or t3, t1, t2     
  blt t2, t3, skip2 
  or t3, t0, t0     
  xor t3, t0, t1    

skip2:
  addi t4, zero, 3  
  bne t3, t4, skip1 
  sw t3, 8(zero)
  auipc t4, 0
  jalr t4, 8(t4)
  jalr t4, 4(t4)
  csrr a1, cycle
  sub ra, a1, a0

loop:
  j loop