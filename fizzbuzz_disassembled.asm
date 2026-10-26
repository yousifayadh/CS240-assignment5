.text
    addi $t1, $zero, 1
    addi $t2, $zero, 101
    addi $t3, $zero, 0
L_0040000C:
    j L_004000A8
L_00400010:
    addi $v0, $zero, 11
    addi $a0, $zero, 70
    syscall
    addi $a0, $zero, 105
    syscall
    addi $a0, $zero, 122
    syscall
    addi $a0, $zero, 122
    syscall
    j L_004000E4
L_00400038:
    addi $v0, $zero, 11
    addi $a0, $zero, 66
    syscall
    addi $a0, $zero, 117
    syscall
    addi $a0, $zero, 122
    syscall
    addi $a0, $zero, 122
    syscall
    j L_004000E4
L_00400060:
    addi $v0, $zero, 11
    addi $a0, $zero, 70
    syscall
    addi $a0, $zero, 105
    syscall
    addi $a0, $zero, 122
    syscall
    addi $a0, $zero, 122
    syscall
    addi $a0, $zero, 66
    syscall
    addi $a0, $zero, 117
    syscall
    addi $a0, $zero, 122
    syscall
    addi $a0, $zero, 122
    syscall
    j L_004000E4
L_004000A8:
    addi $t4, $zero, 15
    div $t1, $t4
    mfhi $t3
    beq $t3, $zero, L_00400060
    addi $t4, $zero, 3
    div $t1, $t4
    mfhi $t3
    beq $t3, $zero, L_00400010
    addi $t4, $zero, 5
    div $t1, $t4
    mfhi $t3
    beq $t3, $zero, L_00400038
    addi $v0, $zero, 1
    addu $a0, $t1, $zero
    syscall
L_004000E4:
    addi $a0, $zero, 10
    addi $v0, $zero, 11
    syscall
    addi $t1, $t1, 1
    bne $t1, $t2, L_0040000C
