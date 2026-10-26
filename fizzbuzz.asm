addi $t1, $zero, 1
addi $t2, $zero, 101
addi $t3, $zero, 0

loop:
j start

fizz1:
addi $v0, $zero, 11
addi $a0, $zero, 'F'
syscall
addi $a0, $zero, 'i'
syscall
addi $a0, $zero, 'z'
syscall
addi $a0, $zero, 'z'
syscall
j endfizz

buzz1:
addi $v0, $zero, 11
addi $a0, $zero, 'B'
syscall
addi $a0, $zero, 'u'
syscall
addi $a0, $zero, 'z'
syscall
addi $a0, $zero, 'z'
syscall
j endbuzz

fizzbuzz1:
addi $v0, $zero, 11
addi $a0, $zero, 'F'
syscall
addi $a0, $zero, 'i'
syscall
addi $a0, $zero, 'z'
syscall
addi $a0, $zero, 'z'
syscall
addi $a0, $zero, 'B'
syscall
addi $a0, $zero, 'u'
syscall
addi $a0, $zero, 'z'
syscall
addi $a0, $zero, 'z'
syscall
j endfizzbuzz


start: 
addi $t4, $zero, 15
div $t1, $t4
mfhi $t3
beq $t3, $zero, fizzbuzz1
addi $t4, $zero, 3
div $t1, $t4
mfhi $t3
beq $t3, $zero, fizz1
addi $t4, $zero, 5
div $t1, $t4
mfhi $t3
beq $t3, $zero, buzz1


addi $v0, $zero, 1
addu $a0, $t1, $zero
syscall


endfizz:

endbuzz:

endfizzbuzz:

addi $a0, $zero, '\n'
addi $v0, $zero, 11
syscall

addi $t1, $t1, 1
bne $t1, $t2, loop