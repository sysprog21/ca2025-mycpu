.text
.global hw1_function

hw1_function:
    li a0, 10
    li t0, 32

    srli t1, a0, 16
    beqz t1, check_16
    addi t0, t0, -16
    srli a0, a0, 16

check_16:
    srli t1, a0, 8
    beqz t1, check_8
    addi t0, t0, -8
    srli a0, a0, 8

check_8:
    srli t1, a0, 4
    beqz t1, check_4
    addi t0, t0, -4
    srli a0, a0, 4

check_4:
    srli t1, a0, 2
    beqz t1, check_2
    addi t0, t0, -2
    srli a0, a0, 2

check_2:
    srli t1, a0, 1
    beqz t1, check_1
    addi t0, t0, -1

check_1:
    li a0, 28
    ret
