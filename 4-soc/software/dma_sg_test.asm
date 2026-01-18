
  dma_sg_test.elf:     file format elf32-littleriscv


  Disassembly of section .text:

  00001000 <_start>:
      1000:	000ff117          	auipc	sp,0xff
      1004:	00010113          	mv	sp,sp
      1008:	1d4000ef          	jal	ra,11dc <main>

  0000100c <loop>:
      100c:	0000006f          	j	100c <loop>

  00001010 <uart_putc>:
      1010:	ff010113          	addi	sp,sp,-16 # ffff0 <main+0xfee14>
      1014:	10000737          	lui	a4,0x10000
      1018:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
      101c:	0027f793          	andi	a5,a5,2
      1020:	fe079ce3          	bnez	a5,1018 <uart_putc+0x8>
      1024:	00a72023          	sw	a0,0(a4)
      1028:	00012623          	sw	zero,12(sp)
      102c:	00c12703          	lw	a4,12(sp)
      1030:	1f300793          	li	a5,499
      1034:	00e7ce63          	blt	a5,a4,1050 <uart_putc+0x40>
      1038:	1f300713          	li	a4,499
      103c:	00c12783          	lw	a5,12(sp)
      1040:	00178793          	addi	a5,a5,1
      1044:	00f12623          	sw	a5,12(sp)
      1048:	00c12783          	lw	a5,12(sp)
      104c:	fef758e3          	bge	a4,a5,103c <uart_putc+0x2c>
      1050:	01010113          	addi	sp,sp,16
      1054:	00008067          	ret

  00001058 <uart_puts>:
      1058:	00054603          	lbu	a2,0(a0)
      105c:	04060a63          	beqz	a2,10b0 <uart_puts+0x58>
      1060:	ff010113          	addi	sp,sp,-16
      1064:	10000737          	lui	a4,0x10000
      1068:	1f300693          	li	a3,499
      106c:	00150513          	addi	a0,a0,1
      1070:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
      1074:	0027f793          	andi	a5,a5,2
      1078:	fe079ce3          	bnez	a5,1070 <uart_puts+0x18>
      107c:	00c72023          	sw	a2,0(a4)
      1080:	00012623          	sw	zero,12(sp)
      1084:	00c12783          	lw	a5,12(sp)
      1088:	00f6cc63          	blt	a3,a5,10a0 <uart_puts+0x48>
      108c:	00c12783          	lw	a5,12(sp)
      1090:	00178793          	addi	a5,a5,1
      1094:	00f12623          	sw	a5,12(sp)
      1098:	00c12783          	lw	a5,12(sp)
      109c:	fef6d8e3          	bge	a3,a5,108c <uart_puts+0x34>
      10a0:	00054603          	lbu	a2,0(a0)
      10a4:	fc0614e3          	bnez	a2,106c <uart_puts+0x14>
      10a8:	01010113          	addi	sp,sp,16
      10ac:	00008067          	ret
      10b0:	00008067          	ret

  000010b4 <uart_put_hex>:
      10b4:	000017b7          	lui	a5,0x1
      10b8:	31878793          	addi	a5,a5,792 # 1318 <main+0x13c>
      10bc:	0047a603          	lw	a2,4(a5)
      10c0:	0007a583          	lw	a1,0(a5)
      10c4:	0087a683          	lw	a3,8(a5)
      10c8:	00c7a703          	lw	a4,12(a5)
      10cc:	0107c783          	lbu	a5,16(a5)
      10d0:	fe010113          	addi	sp,sp,-32
      10d4:	00c12823          	sw	a2,16(sp)
      10d8:	00001637          	lui	a2,0x1
      10dc:	00b12623          	sw	a1,12(sp)
      10e0:	00d12a23          	sw	a3,20(sp)
      10e4:	00e12c23          	sw	a4,24(sp)
      10e8:	00f10e23          	sb	a5,28(sp)
      10ec:	03000593          	li	a1,48
      10f0:	31460613          	addi	a2,a2,788 # 1314 <main+0x138>
      10f4:	10000737          	lui	a4,0x10000
      10f8:	1f300693          	li	a3,499
      10fc:	00160613          	addi	a2,a2,1
      1100:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
      1104:	0027f793          	andi	a5,a5,2
      1108:	fe079ce3          	bnez	a5,1100 <uart_put_hex+0x4c>
      110c:	00b72023          	sw	a1,0(a4)
      1110:	00012023          	sw	zero,0(sp)
      1114:	00012783          	lw	a5,0(sp)
      1118:	00f6cc63          	blt	a3,a5,1130 <uart_put_hex+0x7c>
      111c:	00012783          	lw	a5,0(sp)
      1120:	00178793          	addi	a5,a5,1
      1124:	00f12023          	sw	a5,0(sp)
      1128:	00012783          	lw	a5,0(sp)
      112c:	fef6d8e3          	bge	a3,a5,111c <uart_put_hex+0x68>
      1130:	00064583          	lbu	a1,0(a2)
      1134:	fc0594e3          	bnez	a1,10fc <uart_put_hex+0x48>
      1138:	01c00613          	li	a2,28
      113c:	10000737          	lui	a4,0x10000
      1140:	1f300693          	li	a3,499
      1144:	ffc00813          	li	a6,-4
      1148:	00c557b3          	srl	a5,a0,a2
      114c:	02010593          	addi	a1,sp,32
      1150:	00f7f793          	andi	a5,a5,15
      1154:	00f587b3          	add	a5,a1,a5
      1158:	fec7c583          	lbu	a1,-20(a5)
      115c:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
      1160:	0027f793          	andi	a5,a5,2
      1164:	fe079ce3          	bnez	a5,115c <uart_put_hex+0xa8>
      1168:	00b72023          	sw	a1,0(a4)
      116c:	00012223          	sw	zero,4(sp)
      1170:	00412783          	lw	a5,4(sp)
      1174:	00f6cc63          	blt	a3,a5,118c <uart_put_hex+0xd8>
      1178:	00412783          	lw	a5,4(sp)
      117c:	00178793          	addi	a5,a5,1
      1180:	00f12223          	sw	a5,4(sp)
      1184:	00412783          	lw	a5,4(sp)
      1188:	fef6d8e3          	bge	a3,a5,1178 <uart_put_hex+0xc4>
      118c:	ffc60613          	addi	a2,a2,-4
      1190:	fb061ce3          	bne	a2,a6,1148 <uart_put_hex+0x94>
      1194:	10000737          	lui	a4,0x10000
      1198:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
      119c:	0027f793          	andi	a5,a5,2
      11a0:	fe079ce3          	bnez	a5,1198 <uart_put_hex+0xe4>
      11a4:	00a00793          	li	a5,10
      11a8:	00f72023          	sw	a5,0(a4)
      11ac:	00012423          	sw	zero,8(sp)
      11b0:	00812703          	lw	a4,8(sp)
      11b4:	1f300793          	li	a5,499
      11b8:	00e7ce63          	blt	a5,a4,11d4 <uart_put_hex+0x120>
      11bc:	1f300713          	li	a4,499
      11c0:	00812783          	lw	a5,8(sp)
      11c4:	00178793          	addi	a5,a5,1
      11c8:	00f12423          	sw	a5,8(sp)
      11cc:	00812783          	lw	a5,8(sp)
      11d0:	fef758e3          	bge	a4,a5,11c0 <uart_put_hex+0x10c>
      11d4:	02010113          	addi	sp,sp,32
      11d8:	00008067          	ret

  Disassembly of section .text.startup:

  000011dc <main>:
      11dc:	ed010113          	addi	sp,sp,-304
      11e0:	12112623          	sw	ra,300(sp)
      11e4:	12812423          	sw	s0,296(sp)
      11e8:	00000793          	li	a5,0
      11ec:	aaaa05b7          	lui	a1,0xaaaa0
      11f0:	02000613          	li	a2,32
      11f4:	00279713          	slli	a4,a5,0x2
      11f8:	12010513          	addi	a0,sp,288
      11fc:	00b786b3          	add	a3,a5,a1
      1200:	00e50733          	add	a4,a0,a4
      1204:	f0d72023          	sw	a3,-256(a4)
      1208:	f8072023          	sw	zero,-128(a4)
      120c:	00178793          	addi	a5,a5,1
      1210:	fec792e3          	bne	a5,a2,11f4 <main+0x18>
      1214:	02010793          	addi	a5,sp,32
      1218:	00f12223          	sw	a5,4(sp)
      121c:	0a010793          	addi	a5,sp,160
      1220:	00f12423          	sw	a5,8(sp)
      1224:	04000793          	li	a5,64
      1228:	00f12623          	sw	a5,12(sp)
      122c:	01010713          	addi	a4,sp,16
      1230:	00e12023          	sw	a4,0(sp)
      1234:	06010713          	addi	a4,sp,96
      1238:	00e12a23          	sw	a4,20(sp)
      123c:	0e010713          	addi	a4,sp,224
      1240:	00e12c23          	sw	a4,24(sp)
      1244:	00f12e23          	sw	a5,28(sp)
      1248:	00012823          	sw	zero,16(sp)
      124c:	0ff0000f          	fence
      1250:	00001537          	lui	a0,0x1
      1254:	32c50513          	addi	a0,a0,812 # 132c <main+0x150>
      1258:	e01ff0ef          	jal	ra,1058 <uart_puts>
      125c:	00010513          	mv	a0,sp
      1260:	e55ff0ef          	jal	ra,10b4 <uart_put_hex>
      1264:	00001537          	lui	a0,0x1
      1268:	34450513          	addi	a0,a0,836 # 1344 <main+0x168>
      126c:	dedff0ef          	jal	ra,1058 <uart_puts>
      1270:	100027b7          	lui	a5,0x10002
      1274:	00100713          	li	a4,1
      1278:	0027a423          	sw	sp,8(a5) # 10002008 <_stack_top+0xff02008>
      127c:	00e7a023          	sw	a4,0(a5)
      1280:	10002737          	lui	a4,0x10002
      1284:	00472783          	lw	a5,4(a4) # 10002004 <_stack_top+0xff02004>
      1288:	0027f793          	andi	a5,a5,2
      128c:	fe078ce3          	beqz	a5,1284 <main+0xa8>
      1290:	00001537          	lui	a0,0x1
      1294:	35850513          	addi	a0,a0,856 # 1358 <main+0x17c>
      1298:	dc1ff0ef          	jal	ra,1058 <uart_puts>
      129c:	00000413          	li	s0,0
      12a0:	00000693          	li	a3,0
      12a4:	02000613          	li	a2,32
      12a8:	00269713          	slli	a4,a3,0x2
      12ac:	12010793          	addi	a5,sp,288
      12b0:	00e78733          	add	a4,a5,a4
      12b4:	f8072783          	lw	a5,-128(a4)
      12b8:	f0072703          	lw	a4,-256(a4)
      12bc:	00168693          	addi	a3,a3,1
      12c0:	40e787b3          	sub	a5,a5,a4
      12c4:	00f037b3          	snez	a5,a5
      12c8:	00f40433          	add	s0,s0,a5
      12cc:	fcc69ee3          	bne	a3,a2,12a8 <main+0xcc>
      12d0:	02041663          	bnez	s0,12fc <main+0x120>
      12d4:	00001537          	lui	a0,0x1
      12d8:	36450513          	addi	a0,a0,868 # 1364 <main+0x188>
      12dc:	d7dff0ef          	jal	ra,1058 <uart_puts>
      12e0:	00001537          	lui	a0,0x1
      12e4:	38050513          	addi	a0,a0,896 # 1380 <main+0x1a4>
      12e8:	d71ff0ef          	jal	ra,1058 <uart_puts>
      12ec:	00001537          	lui	a0,0x1
      12f0:	39850513          	addi	a0,a0,920 # 1398 <main+0x1bc>
      12f4:	d65ff0ef          	jal	ra,1058 <uart_puts>
      12f8:	0000006f          	j	12f8 <main+0x11c>
      12fc:	00001537          	lui	a0,0x1
      1300:	3b450513          	addi	a0,a0,948 # 13b4 <main+0x1d8>
      1304:	d55ff0ef          	jal	ra,1058 <uart_puts>
      1308:	00040513          	mv	a0,s0
      130c:	da9ff0ef          	jal	ra,10b4 <uart_put_hex>
      1310:	fe9ff06f          	j	12f8 <main+0x11c>

  Disassembly of section .rodata.str1.4:

  00001314 <_stack_top-0xfecec>:
      1314:	7830                	flw	fa2,112(s0)
      1316:	0000                	unimp
      1318:	3130                	fld	fa2,96(a0)
      131a:	3332                	fld	ft6,296(sp)
      131c:	3534                	fld	fa3,104(a0)
      131e:	3736                	fld	fa4,360(sp)
      1320:	3938                	fld	fa4,112(a0)
      1322:	4241                	li	tp,16
      1324:	46454443          	fmadd.q	fs0,fa0,ft4,fs0,rmm
      1328:	0000                	unimp
      132a:	0000                	unimp
      132c:	4d44                	lw	s1,28(a0)
      132e:	2041                	jal	13ae <main+0x1d2>
      1330:	75746553          	0x75746553
      1334:	2070                	fld	fa2,192(s0)
      1336:	202e4b4f          	fnmadd.s	fs6,ft8,ft2,ft4,rmm
      133a:	6441                	lui	s0,0x10
      133c:	7264                	flw	fs1,100(a2)
      133e:	203a                	fld	ft0,392(sp)
      1340:	0000                	unimp
      1342:	0000                	unimp
      1344:	72617453          	0x72617453
      1348:	6974                	flw	fa3,84(a0)
      134a:	676e                	flw	fa4,216(sp)
      134c:	4420                	lw	s0,72(s0)
      134e:	414d                	li	sp,19
      1350:	2e2e                	fld	ft8,200(sp)
      1352:	0a2e                	slli	s4,s4,0xb
      1354:	0000                	unimp
      1356:	0000                	unimp
      1358:	4d44                	lw	s1,28(a0)
      135a:	2041                	jal	13da <main+0x1fe>
      135c:	6f44                	flw	fs1,28(a4)
      135e:	656e                	flw	fa0,216(sp)
      1360:	0a21                	addi	s4,s4,8
      1362:	0000                	unimp
      1364:	0a0a                	slli	s4,s4,0x2
      1366:	3d3d                	jal	11a4 <uart_put_hex+0xf0>
      1368:	3d3d                	jal	11a6 <uart_put_hex+0xf2>
      136a:	3d3d                	jal	11a8 <uart_put_hex+0xf4>
      136c:	3d3d                	jal	11aa <uart_put_hex+0xf6>
      136e:	3d3d                	jal	11ac <uart_put_hex+0xf8>
      1370:	3d3d                	jal	11ae <uart_put_hex+0xfa>
      1372:	3d3d                	jal	11b0 <uart_put_hex+0xfc>
      1374:	3d3d                	jal	11b2 <uart_put_hex+0xfe>
      1376:	3d3d                	jal	11b4 <uart_put_hex+0x100>
      1378:	3d3d                	jal	11b6 <uart_put_hex+0x102>
      137a:	3d3d                	jal	11b8 <uart_put_hex+0x104>
      137c:	0a3d                	addi	s4,s4,15
      137e:	0000                	unimp
      1380:	4355535b          	0x4355535b
      1384:	53534543          	fmadd.d	fa0,ft6,fs5,fa0,rmm
      1388:	205d                	jal	142e <main+0x252>
      138a:	6554                	flw	fa3,12(a0)
      138c:	50207473          	csrrci	s0,0x502,0
      1390:	7361                	lui	t1,0xffff8
      1392:	21646573          	csrrsi	a0,0x216,8
      1396:	000a                	c.slli	zero,0x2
      1398:	3d3d                	jal	11d6 <uart_put_hex+0x122>
      139a:	3d3d                	jal	11d8 <uart_put_hex+0x124>
      139c:	3d3d                	jal	11da <uart_put_hex+0x126>
      139e:	3d3d                	jal	11dc <main>
      13a0:	3d3d                	jal	11de <main+0x2>
      13a2:	3d3d                	jal	11e0 <main+0x4>
      13a4:	3d3d                	jal	11e2 <main+0x6>
      13a6:	3d3d                	jal	11e4 <main+0x8>
      13a8:	3d3d                	jal	11e6 <main+0xa>
      13aa:	3d3d                	jal	11e8 <main+0xc>
      13ac:	3d3d                	jal	11ea <main+0xe>
      13ae:	0a3d                	addi	s4,s4,15
      13b0:	0000                	unimp
      13b2:	0000                	unimp
      13b4:	5b0a                	lw	s6,160(sp)
      13b6:	4146                	lw	sp,80(sp)
      13b8:	4c49                	li	s8,18
      13ba:	4445                	li	s0,17
      13bc:	205d                	jal	1462 <main+0x286>
      13be:	7245                	lui	tp,0xffff1
      13c0:	6f72                	flw	ft10,28(sp)
      13c2:	2072                	fld	ft0,280(sp)
      13c4:	6e756f43          	fmadd.q	ft10,fa0,ft7,fa3,unknown
      13c8:	3a74                	fld	fa3,240(a2)
      13ca:	0020                	addi	s0,sp,8

  Disassembly of section .riscv.attributes:

  00000000 <.riscv.attributes>:
    0:	1b41                	addi	s6,s6,-16
    2:	0000                	unimp
    4:	7200                	flw	fs0,32(a2)
    6:	7369                	lui	t1,0xffffa
    8:	01007663          	bgeu	zero,a6,14 <_start-0xfec>
    c:	0011                	c.nop	4
    e:	0000                	unimp
    10:	1004                	addi	s1,sp,32
    12:	7205                	lui	tp,0xfffe1
    14:	3376                	fld	ft6,376(sp)
    16:	6932                	flw	fs2,12(sp)
    18:	7032                	flw	ft0,44(sp)
    1a:	0030                	addi	a2,sp,8

  Disassembly of section .comment:

  00000000 <.comment>:
    0:	3a434347          	fmsub.d	ft6,ft6,ft4,ft7,rmm
    4:	2820                	fld	fs0,80(s0)
    6:	2029                	jal	10 <_start-0xff0>
    8:	3031                	jal	fffff814 <_stack_top+0xffeff814>
    a:	322e                	fld	ft4,232(sp)
    c:	302e                	fld	ft0,232(sp)
    ...

  Disassembly of section .debug_line:

  00000000 <.debug_line>:
    0:	003e                	c.slli	zero,0xf
    2:	0000                	unimp
    4:	001e0003          	lb	zero,1(t3)
    8:	0000                	unimp
    a:	0101                	addi	sp,sp,0
    c:	000d0efb          	0xd0efb
    10:	0101                	addi	sp,sp,0
    12:	0101                	addi	sp,sp,0
    14:	0000                	unimp
    16:	0100                	addi	s0,sp,128
    18:	0000                	unimp
    1a:	0001                	nop
    1c:	72617473          	csrrci	s0,0x726,2
    20:	2e74                	fld	fa3,216(a2)
    22:	00000053          	fadd.s	ft0,ft0,ft0,rne
    26:	0000                	unimp
    28:	0500                	addi	s0,sp,640
    2a:	0002                	c.slli64	zero
    2c:	0010                	0x10
    2e:	1800                	addi	s0,sp,48
    30:	08090403          	lb	s0,128(s2)
    34:	0100                	addi	s0,sp,128
    36:	04090503          	lb	a0,64(s2)
    3a:	0100                	addi	s0,sp,128
    3c:	0409                	addi	s0,s0,2
    3e:	0000                	unimp
    40:	0101                	addi	sp,sp,0
    42:	00000847          	fmsub.s	fa6,ft0,ft0,ft0,rne
    46:	00240003          	lb	zero,2(s0) # 10002 <main+0xee26>
    4a:	0000                	unimp
    4c:	0101                	addi	sp,sp,0
    4e:	000d0efb          	0xd0efb
    52:	0101                	addi	sp,sp,0
    54:	0101                	addi	sp,sp,0
    56:	0000                	unimp
    58:	0100                	addi	s0,sp,128
    5a:	0000                	unimp
    5c:	0001                	nop
    5e:	6d64                	flw	fs1,92(a0)
    60:	5f61                	li	t5,-8
    62:	745f6773          	csrrsi	a4,0x745,30
    66:	7365                	lui	t1,0xffff9
    68:	2e74                	fld	fa3,216(a2)
    6a:	00000063          	beqz	zero,6a <_start-0xf96>
    6e:	0000                	unimp
    70:	0105                	addi	sp,sp,1
    72:	0500                	addi	s0,sp,640
    74:	1002                	c.slli	zero,0x20
    76:	0010                	0x10
    78:	0300                	addi	s0,sp,384
    7a:	05050123          	sb	a6,66(a0)
    7e:	00090103          	lb	sp,0(s2)
    82:	0100                	addi	s0,sp,128
    84:	0105                	addi	sp,sp,1
    86:	0306                	slli	t1,t1,0x1
    88:	097f                	0x97f
    8a:	0000                	unimp
    8c:	0501                	addi	a0,a0,0
    8e:	030d                	addi	t1,t1,3
    90:	0901                	addi	s2,s2,0
    92:	0004                	0x4
    94:	0501                	addi	a0,a0,0
    96:	0009                	c.nop	2
    98:	0402                	c.slli64	s0
    9a:	0601                	addi	a2,a2,0
    9c:	04090103          	lb	sp,64(s2)
    a0:	0100                	addi	s0,sp,128
    a2:	0b05                	addi	s6,s6,1
    a4:	0200                	addi	s0,sp,256
    a6:	0104                	addi	s1,sp,128
    a8:	00097f03          	0x97f03
    ac:	0100                	addi	s0,sp,128
    ae:	0d05                	addi	s10,s10,1
    b0:	0200                	addi	s0,sp,256
    b2:	0104                	addi	s1,sp,128
    b4:	0306                	slli	t1,t1,0x1
    b6:	0900                	addi	s0,sp,144
    b8:	0000                	unimp
    ba:	0501                	addi	a0,a0,0
    bc:	0402001b          	0x402001b
    c0:	0301                	addi	t1,t1,0
    c2:	0900                	addi	s0,sp,144
    c4:	0004                	0x4
    c6:	0501                	addi	a0,a0,0
    c8:	0402000b          	0x402000b
    cc:	0301                	addi	t1,t1,0
    ce:	0900                	addi	s0,sp,144
    d0:	0004                	0x4
    d2:	0501                	addi	a0,a0,0
    d4:	0605                	addi	a2,a2,1
    d6:	04090203          	lb	tp,64(s2)
    da:	0100                	addi	s0,sp,128
    dc:	0e05                	addi	t3,t3,1
    de:	0306                	slli	t1,t1,0x1
    e0:	0900                	addi	s0,sp,144
    e2:	0000                	unimp
    e4:	0501                	addi	a0,a0,0
    e6:	0605                	addi	a2,a2,1
    e8:	04090103          	lb	sp,64(s2)
    ec:	0100                	addi	s0,sp,128
    ee:	0a05                	addi	s4,s4,1
    f0:	00090003          	lb	zero,0(s2)
    f4:	0100                	addi	s0,sp,128
    f6:	1705                	addi	a4,a4,-31
    f8:	0306                	slli	t1,t1,0x1
    fa:	0900                	addi	s0,sp,144
    fc:	0000                	unimp
    fe:	0501                	addi	a0,a0,0
  100:	061e                	slli	a2,a2,0x7
  102:	04090003          	lb	zero,64(s2)
  106:	0100                	addi	s0,sp,128
  108:	2005                	jal	128 <_start-0xed8>
  10a:	0306                	slli	t1,t1,0x1
  10c:	0900                	addi	s0,sp,144
  10e:	0000                	unimp
  110:	0501                	addi	a0,a0,0
  112:	0305                	addi	t1,t1,1
  114:	0900                	addi	s0,sp,144
  116:	0004                	0x4
  118:	0501                	addi	a0,a0,0
  11a:	0009                	c.nop	2
  11c:	0402                	c.slli64	s0
  11e:	01030603          	lb	a2,16(t1) # ffff9010 <_stack_top+0xffef9010>
  122:	0c09                	addi	s8,s8,2
  124:	0100                	addi	s0,sp,128
  126:	2705                	jal	846 <_start-0x7ba>
  128:	0200                	addi	s0,sp,256
  12a:	0304                	addi	s1,sp,384
  12c:	00097f03          	0x97f03
  130:	0100                	addi	s0,sp,128
  132:	2805                	jal	162 <_start-0xe9e>
  134:	0200                	addi	s0,sp,256
  136:	0304                	addi	s1,sp,384
  138:	0306                	slli	t1,t1,0x1
  13a:	0900                	addi	s0,sp,144
  13c:	0000                	unimp
  13e:	0501                	addi	a0,a0,0
  140:	001e                	c.slli	zero,0x7
  142:	0402                	c.slli64	s0
  144:	00030603          	lb	a2,0(t1)
  148:	0c09                	addi	s8,s8,2
  14a:	0100                	addi	s0,sp,128
  14c:	2005                	jal	16c <_start-0xe94>
  14e:	0200                	addi	s0,sp,256
  150:	0304                	addi	s1,sp,384
  152:	0306                	slli	t1,t1,0x1
  154:	0900                	addi	s0,sp,144
  156:	0000                	unimp
  158:	0501                	addi	a0,a0,0
  15a:	0005                	c.nop	1
  15c:	0402                	c.slli64	s0
  15e:	09000303          	lb	t1,144(zero) # 90 <_start-0xf70>
  162:	0004                	0x4
  164:	0501                	addi	a0,a0,0
  166:	0301                	addi	t1,t1,0
  168:	0902                	c.slli64	s2
  16a:	0004                	0x4
  16c:	0601                	addi	a2,a2,0
  16e:	08090303          	lb	t1,128(s2)
  172:	0100                	addi	s0,sp,128
  174:	0505                	addi	a0,a0,1
  176:	00090103          	lb	sp,0(s2)
  17a:	0100                	addi	s0,sp,128
  17c:	0b05                	addi	s6,s6,1
  17e:	00090003          	lb	zero,0(s2)
  182:	0100                	addi	s0,sp,128
  184:	0c05                	addi	s8,s8,1
  186:	0306                	slli	t1,t1,0x1
  188:	0900                	addi	s0,sp,144
  18a:	0000                	unimp
  18c:	0501                	addi	a0,a0,0
  18e:	0900030b          	0x900030b
  192:	0004                	0x4
  194:	0501                	addi	a0,a0,0
  196:	0301                	addi	t1,t1,0
  198:	097f                	0x97f
  19a:	0004                	0x4
  19c:	0501                	addi	a0,a0,0
  19e:	030d                	addi	t1,t1,3
  1a0:	0978                	addi	a4,sp,156
  1a2:	0004                	0x4
  1a4:	0501                	addi	a0,a0,0
  1a6:	0305                	addi	t1,t1,1
  1a8:	00040903          	lb	s2,0(s0)
  1ac:	0501                	addi	a0,a0,0
  1ae:	0609                	addi	a2,a2,2
  1b0:	04090703          	lb	a4,64(s2)
  1b4:	0100                	addi	s0,sp,128
  1b6:	1505                	addi	a0,a0,-31
  1b8:	0306                	slli	t1,t1,0x1
  1ba:	0900                	addi	s0,sp,144
  1bc:	0000                	unimp
  1be:	0501                	addi	a0,a0,0
  1c0:	0609                	addi	a2,a2,2
  1c2:	04097703          	0x4097703
  1c6:	0100                	addi	s0,sp,128
  1c8:	0b05                	addi	s6,s6,1
  1ca:	00097f03          	0x97f03
  1ce:	0100                	addi	s0,sp,128
  1d0:	0d05                	addi	s10,s10,1
  1d2:	0306                	slli	t1,t1,0x1
  1d4:	0900                	addi	s0,sp,144
  1d6:	0000                	unimp
  1d8:	0501                	addi	a0,a0,0
  1da:	0900031b          	0x900031b
  1de:	0004                	0x4
  1e0:	0501                	addi	a0,a0,0
  1e2:	0900030b          	0x900030b
  1e6:	0004                	0x4
  1e8:	0501                	addi	a0,a0,0
  1ea:	0605                	addi	a2,a2,1
  1ec:	04090203          	lb	tp,64(s2)
  1f0:	0100                	addi	s0,sp,128
  1f2:	0e05                	addi	t3,t3,1
  1f4:	0306                	slli	t1,t1,0x1
  1f6:	0900                	addi	s0,sp,144
  1f8:	0000                	unimp
  1fa:	0501                	addi	a0,a0,0
  1fc:	0605                	addi	a2,a2,1
  1fe:	04090103          	lb	sp,64(s2)
  202:	0100                	addi	s0,sp,128
  204:	0a05                	addi	s4,s4,1
  206:	00090003          	lb	zero,0(s2)
  20a:	0100                	addi	s0,sp,128
  20c:	1705                	addi	a4,a4,-31
  20e:	0306                	slli	t1,t1,0x1
  210:	0900                	addi	s0,sp,144
  212:	0000                	unimp
  214:	0501                	addi	a0,a0,0
  216:	061e                	slli	a2,a2,0x7
  218:	04090003          	lb	zero,64(s2)
  21c:	0100                	addi	s0,sp,128
  21e:	2005                	jal	23e <_start-0xdc2>
  220:	0306                	slli	t1,t1,0x1
  222:	0900                	addi	s0,sp,144
  224:	0000                	unimp
  226:	0501                	addi	a0,a0,0
  228:	0305                	addi	t1,t1,1
  22a:	0900                	addi	s0,sp,144
  22c:	0004                	0x4
  22e:	0501                	addi	a0,a0,0
  230:	0609                	addi	a2,a2,2
  232:	04090103          	lb	sp,64(s2)
  236:	0100                	addi	s0,sp,128
  238:	2705                	jal	958 <_start-0x6a8>
  23a:	00097f03          	0x97f03
  23e:	0100                	addi	s0,sp,128
  240:	2805                	jal	270 <_start-0xd90>
  242:	0306                	slli	t1,t1,0x1
  244:	0900                	addi	s0,sp,144
  246:	0000                	unimp
  248:	0501                	addi	a0,a0,0
  24a:	061e                	slli	a2,a2,0x7
  24c:	0c090003          	lb	zero,192(s2)
  250:	0100                	addi	s0,sp,128
  252:	2005                	jal	272 <_start-0xd8e>
  254:	0306                	slli	t1,t1,0x1
  256:	0900                	addi	s0,sp,144
  258:	0000                	unimp
  25a:	0501                	addi	a0,a0,0
  25c:	0305                	addi	t1,t1,1
  25e:	0900                	addi	s0,sp,144
  260:	0004                	0x4
  262:	0501                	addi	a0,a0,0
  264:	0603060b          	0x603060b
  268:	0409                	addi	s0,s0,2
  26a:	0100                	addi	s0,sp,128
  26c:	0c05                	addi	s8,s8,1
  26e:	0306                	slli	t1,t1,0x1
  270:	0900                	addi	s0,sp,144
  272:	0000                	unimp
  274:	0501                	addi	a0,a0,0
  276:	0900030b          	0x900030b
  27a:	0004                	0x4
  27c:	0501                	addi	a0,a0,0
  27e:	0301                	addi	t1,t1,0
  280:	0902                	c.slli64	s2
  282:	0004                	0x4
  284:	0601                	addi	a2,a2,0
  286:	0c090303          	lb	t1,192(s2)
  28a:	0100                	addi	s0,sp,128
  28c:	0505                	addi	a0,a0,1
  28e:	00090103          	lb	sp,0(s2)
  292:	0100                	addi	s0,sp,128
  294:	1005                	c.nop	-31
  296:	0306                	slli	t1,t1,0x1
  298:	0900                	addi	s0,sp,144
  29a:	0000                	unimp
  29c:	0501                	addi	a0,a0,0
  29e:	0301                	addi	t1,t1,0
  2a0:	097f                	0x97f
  2a2:	001c                	0x1c
  2a4:	0501                	addi	a0,a0,0
  2a6:	0310                	addi	a2,sp,384
  2a8:	0901                	addi	s2,s2,0
  2aa:	0004                	0x4
  2ac:	0501                	addi	a0,a0,0
  2ae:	0605                	addi	a2,a2,1
  2b0:	18090103          	lb	sp,384(s2)
  2b4:	0100                	addi	s0,sp,128
  2b6:	0b05                	addi	s6,s6,1
  2b8:	00097903          	0x97903
  2bc:	0100                	addi	s0,sp,128
  2be:	0c05                	addi	s8,s8,1
  2c0:	0306                	slli	t1,t1,0x1
  2c2:	0900                	addi	s0,sp,144
  2c4:	0000                	unimp
  2c6:	0501                	addi	a0,a0,0
  2c8:	0310                	addi	a2,sp,384
  2ca:	0906                	slli	s2,s2,0x1
  2cc:	0004                	0x4
  2ce:	0501                	addi	a0,a0,0
  2d0:	030d                	addi	t1,t1,3
  2d2:	0971                	addi	s2,s2,28
  2d4:	0004                	0x4
  2d6:	0501                	addi	a0,a0,0
  2d8:	0305                	addi	t1,t1,1
  2da:	00040903          	lb	s2,0(s0)
  2de:	0501                	addi	a0,a0,0
  2e0:	0609                	addi	a2,a2,2
  2e2:	04090703          	lb	a4,64(s2)
  2e6:	0100                	addi	s0,sp,128
  2e8:	1505                	addi	a0,a0,-31
  2ea:	0306                	slli	t1,t1,0x1
  2ec:	0900                	addi	s0,sp,144
  2ee:	0000                	unimp
  2f0:	0501                	addi	a0,a0,0
  2f2:	0609                	addi	a2,a2,2
  2f4:	04097703          	0x4097703
  2f8:	0100                	addi	s0,sp,128
  2fa:	0b05                	addi	s6,s6,1
  2fc:	00097f03          	0x97f03
  300:	0100                	addi	s0,sp,128
  302:	0d05                	addi	s10,s10,1
  304:	0306                	slli	t1,t1,0x1
  306:	0900                	addi	s0,sp,144
  308:	0000                	unimp
  30a:	0501                	addi	a0,a0,0
  30c:	0900031b          	0x900031b
  310:	0004                	0x4
  312:	0501                	addi	a0,a0,0
  314:	0900030b          	0x900030b
  318:	0004                	0x4
  31a:	0501                	addi	a0,a0,0
  31c:	0605                	addi	a2,a2,1
  31e:	04090203          	lb	tp,64(s2)
  322:	0100                	addi	s0,sp,128
  324:	0e05                	addi	t3,t3,1
  326:	0306                	slli	t1,t1,0x1
  328:	0900                	addi	s0,sp,144
  32a:	0000                	unimp
  32c:	0501                	addi	a0,a0,0
  32e:	0605                	addi	a2,a2,1
  330:	04090103          	lb	sp,64(s2)
  334:	0100                	addi	s0,sp,128
  336:	0a05                	addi	s4,s4,1
  338:	00090003          	lb	zero,0(s2)
  33c:	0100                	addi	s0,sp,128
  33e:	1705                	addi	a4,a4,-31
  340:	0306                	slli	t1,t1,0x1
  342:	0900                	addi	s0,sp,144
  344:	0000                	unimp
  346:	0501                	addi	a0,a0,0
  348:	061e                	slli	a2,a2,0x7
  34a:	04090003          	lb	zero,64(s2)
  34e:	0100                	addi	s0,sp,128
  350:	2005                	jal	370 <_start-0xc90>
  352:	0306                	slli	t1,t1,0x1
  354:	0900                	addi	s0,sp,144
  356:	0000                	unimp
  358:	0501                	addi	a0,a0,0
  35a:	0305                	addi	t1,t1,1
  35c:	0900                	addi	s0,sp,144
  35e:	0004                	0x4
  360:	0501                	addi	a0,a0,0
  362:	0609                	addi	a2,a2,2
  364:	04090103          	lb	sp,64(s2)
  368:	0100                	addi	s0,sp,128
  36a:	2705                	jal	a8a <_start-0x576>
  36c:	00097f03          	0x97f03
  370:	0100                	addi	s0,sp,128
  372:	2805                	jal	3a2 <_start-0xc5e>
  374:	0306                	slli	t1,t1,0x1
  376:	0900                	addi	s0,sp,144
  378:	0000                	unimp
  37a:	0501                	addi	a0,a0,0
  37c:	061e                	slli	a2,a2,0x7
  37e:	0c090003          	lb	zero,192(s2)
  382:	0100                	addi	s0,sp,128
  384:	2005                	jal	3a4 <_start-0xc5c>
  386:	0306                	slli	t1,t1,0x1
  388:	0900                	addi	s0,sp,144
  38a:	0000                	unimp
  38c:	0501                	addi	a0,a0,0
  38e:	0305                	addi	t1,t1,1
  390:	0900                	addi	s0,sp,144
  392:	0004                	0x4
  394:	0501                	addi	a0,a0,0
  396:	0603060b          	0x603060b
  39a:	0409                	addi	s0,s0,2
  39c:	0100                	addi	s0,sp,128
  39e:	0c05                	addi	s8,s8,1
  3a0:	0306                	slli	t1,t1,0x1
  3a2:	0900                	addi	s0,sp,144
  3a4:	0000                	unimp
  3a6:	0501                	addi	a0,a0,0
  3a8:	0900030b          	0x900030b
  3ac:	0004                	0x4
  3ae:	0501                	addi	a0,a0,0
  3b0:	030d                	addi	t1,t1,3
  3b2:	00080977          	0x80977
  3b6:	0501                	addi	a0,a0,0
  3b8:	0305                	addi	t1,t1,1
  3ba:	00040903          	lb	s2,0(s0)
  3be:	0301                	addi	t1,t1,0
  3c0:	090e                	slli	s2,s2,0x3
  3c2:	0004                	0x4
  3c4:	0501                	addi	a0,a0,0
  3c6:	0009                	c.nop	2
  3c8:	0402                	c.slli64	s0
  3ca:	01030603          	lb	a2,16(t1)
  3ce:	0409                	addi	s0,s0,2
  3d0:	0100                	addi	s0,sp,128
  3d2:	1c05                	addi	s8,s8,-31
  3d4:	0200                	addi	s0,sp,256
  3d6:	0304                	addi	s1,sp,384
  3d8:	0306                	slli	t1,t1,0x1
  3da:	0900                	addi	s0,sp,144
  3dc:	0000                	unimp
  3de:	0501                	addi	a0,a0,0
  3e0:	0009                	c.nop	2
  3e2:	0402                	c.slli64	s0
  3e4:	09000303          	lb	t1,144(zero) # 90 <_start-0xf70>
  3e8:	0004                	0x4
  3ea:	0501                	addi	a0,a0,0
  3ec:	0028                	addi	a0,sp,8
  3ee:	0402                	c.slli64	s0
  3f0:	09000303          	lb	t1,144(zero) # 90 <_start-0xf70>
  3f4:	0004                	0x4
  3f6:	0501                	addi	a0,a0,0
  3f8:	0009                	c.nop	2
  3fa:	0402                	c.slli64	s0
  3fc:	09000303          	lb	t1,144(zero) # 90 <_start-0xf70>
  400:	0004                	0x4
  402:	0601                	addi	a2,a2,0
  404:	08096f03          	0x8096f03
  408:	0100                	addi	s0,sp,128
  40a:	0b05                	addi	s6,s6,1
  40c:	00097f03          	0x97f03
  410:	0100                	addi	s0,sp,128
  412:	0d05                	addi	s10,s10,1
  414:	0306                	slli	t1,t1,0x1
  416:	0900                	addi	s0,sp,144
  418:	0000                	unimp
  41a:	0501                	addi	a0,a0,0
  41c:	0900031b          	0x900031b
  420:	0004                	0x4
  422:	0501                	addi	a0,a0,0
  424:	0900030b          	0x900030b
  428:	0004                	0x4
  42a:	0501                	addi	a0,a0,0
  42c:	0605                	addi	a2,a2,1
  42e:	04090203          	lb	tp,64(s2)
  432:	0100                	addi	s0,sp,128
  434:	0e05                	addi	t3,t3,1
  436:	0306                	slli	t1,t1,0x1
  438:	0900                	addi	s0,sp,144
  43a:	0000                	unimp
  43c:	0501                	addi	a0,a0,0
  43e:	0605                	addi	a2,a2,1
  440:	04090103          	lb	sp,64(s2)
  444:	0100                	addi	s0,sp,128
  446:	0a05                	addi	s4,s4,1
  448:	00090003          	lb	zero,0(s2)
  44c:	0100                	addi	s0,sp,128
  44e:	1705                	addi	a4,a4,-31
  450:	0306                	slli	t1,t1,0x1
  452:	0900                	addi	s0,sp,144
  454:	0000                	unimp
  456:	0501                	addi	a0,a0,0
  458:	061e                	slli	a2,a2,0x7
  45a:	04090003          	lb	zero,64(s2)
  45e:	0100                	addi	s0,sp,128
  460:	2005                	jal	480 <_start-0xb80>
  462:	0306                	slli	t1,t1,0x1
  464:	0900                	addi	s0,sp,144
  466:	0000                	unimp
  468:	0501                	addi	a0,a0,0
  46a:	0305                	addi	t1,t1,1
  46c:	0900                	addi	s0,sp,144
  46e:	0004                	0x4
  470:	0501                	addi	a0,a0,0
  472:	0609                	addi	a2,a2,2
  474:	04090103          	lb	sp,64(s2)
  478:	0100                	addi	s0,sp,128
  47a:	2705                	jal	b9a <_start-0x466>
  47c:	00097f03          	0x97f03
  480:	0100                	addi	s0,sp,128
  482:	2805                	jal	4b2 <_start-0xb4e>
  484:	0306                	slli	t1,t1,0x1
  486:	0900                	addi	s0,sp,144
  488:	0000                	unimp
  48a:	0501                	addi	a0,a0,0
  48c:	061e                	slli	a2,a2,0x7
  48e:	0c090003          	lb	zero,192(s2)
  492:	0100                	addi	s0,sp,128
  494:	2005                	jal	4b4 <_start-0xb4c>
  496:	0306                	slli	t1,t1,0x1
  498:	0900                	addi	s0,sp,144
  49a:	0000                	unimp
  49c:	0501                	addi	a0,a0,0
  49e:	0305                	addi	t1,t1,1
  4a0:	0900                	addi	s0,sp,144
  4a2:	0004                	0x4
  4a4:	0501                	addi	a0,a0,0
  4a6:	061d                	addi	a2,a2,7
  4a8:	04090e03          	lb	t3,64(s2)
  4ac:	0100                	addi	s0,sp,128
  4ae:	1505                	addi	a0,a0,-31
  4b0:	00090003          	lb	zero,0(s2)
  4b4:	0100                	addi	s0,sp,128
  4b6:	0505                	addi	a0,a0,1
  4b8:	0306                	slli	t1,t1,0x1
  4ba:	0900                	addi	s0,sp,144
  4bc:	0000                	unimp
  4be:	0501                	addi	a0,a0,0
  4c0:	030d                	addi	t1,t1,3
  4c2:	0008096f          	jal	s2,804c2 <main+0x7f2e6>
  4c6:	0501                	addi	a0,a0,0
  4c8:	0609                	addi	a2,a2,2
  4ca:	04090103          	lb	sp,64(s2)
  4ce:	0100                	addi	s0,sp,128
  4d0:	0b05                	addi	s6,s6,1
  4d2:	00097f03          	0x97f03
  4d6:	0100                	addi	s0,sp,128
  4d8:	0d05                	addi	s10,s10,1
  4da:	0306                	slli	t1,t1,0x1
  4dc:	0900                	addi	s0,sp,144
  4de:	0000                	unimp
  4e0:	0501                	addi	a0,a0,0
  4e2:	0900031b          	0x900031b
  4e6:	0004                	0x4
  4e8:	0501                	addi	a0,a0,0
  4ea:	0900030b          	0x900030b
  4ee:	0004                	0x4
  4f0:	0501                	addi	a0,a0,0
  4f2:	0605                	addi	a2,a2,1
  4f4:	04090203          	lb	tp,64(s2)
  4f8:	0100                	addi	s0,sp,128
  4fa:	0e05                	addi	t3,t3,1
  4fc:	0306                	slli	t1,t1,0x1
  4fe:	0900                	addi	s0,sp,144
  500:	0000                	unimp
  502:	0501                	addi	a0,a0,0
  504:	0605                	addi	a2,a2,1
  506:	08090103          	lb	sp,128(s2)
  50a:	0100                	addi	s0,sp,128
  50c:	0a05                	addi	s4,s4,1
  50e:	00090003          	lb	zero,0(s2)
  512:	0100                	addi	s0,sp,128
  514:	1705                	addi	a4,a4,-31
  516:	0306                	slli	t1,t1,0x1
  518:	0900                	addi	s0,sp,144
  51a:	0000                	unimp
  51c:	0501                	addi	a0,a0,0
  51e:	061e                	slli	a2,a2,0x7
  520:	04090003          	lb	zero,64(s2)
  524:	0100                	addi	s0,sp,128
  526:	2005                	jal	546 <_start-0xaba>
  528:	0306                	slli	t1,t1,0x1
  52a:	0900                	addi	s0,sp,144
  52c:	0000                	unimp
  52e:	0501                	addi	a0,a0,0
  530:	0305                	addi	t1,t1,1
  532:	0900                	addi	s0,sp,144
  534:	0004                	0x4
  536:	0501                	addi	a0,a0,0
  538:	0609                	addi	a2,a2,2
  53a:	0c090103          	lb	sp,192(s2)
  53e:	0100                	addi	s0,sp,128
  540:	2705                	jal	c60 <_start-0x3a0>
  542:	00097f03          	0x97f03
  546:	0100                	addi	s0,sp,128
  548:	2805                	jal	578 <_start-0xa88>
  54a:	0306                	slli	t1,t1,0x1
  54c:	0900                	addi	s0,sp,144
  54e:	0000                	unimp
  550:	0501                	addi	a0,a0,0
  552:	061e                	slli	a2,a2,0x7
  554:	0c090003          	lb	zero,192(s2)
  558:	0100                	addi	s0,sp,128
  55a:	2005                	jal	57a <_start-0xa86>
  55c:	0306                	slli	t1,t1,0x1
  55e:	0900                	addi	s0,sp,144
  560:	0000                	unimp
  562:	0501                	addi	a0,a0,0
  564:	0305                	addi	t1,t1,1
  566:	0900                	addi	s0,sp,144
  568:	0004                	0x4
  56a:	0501                	addi	a0,a0,0
  56c:	0301                	addi	t1,t1,0
  56e:	0912                	slli	s2,s2,0x4
  570:	0004                	0x4
  572:	0901                	addi	s2,s2,0
  574:	0008                	0x8
  576:	0100                	addi	s0,sp,128
  578:	0501                	addi	a0,a0,0
  57a:	0001                	nop
  57c:	0205                	addi	tp,tp,1
  57e:	11dc                	addi	a5,sp,228
  580:	0000                	unimp
  582:	05013f03          	0x5013f03
  586:	0305                	addi	t1,t1,1
  588:	0902                	c.slli64	s2
  58a:	0000                	unimp
  58c:	0301                	addi	t1,t1,0
  58e:	0901                	addi	s2,s2,0
  590:	0000                	unimp
  592:	0301                	addi	t1,t1,0
  594:	0901                	addi	s2,s2,0
  596:	0000                	unimp
  598:	0301                	addi	t1,t1,0
  59a:	0901                	addi	s2,s2,0
  59c:	0000                	unimp
  59e:	0301                	addi	t1,t1,0
  5a0:	00000903          	lb	s2,0(zero) # 0 <_start-0x1000>
  5a4:	0501                	addi	a0,a0,0
  5a6:	030a                	slli	t1,t1,0x2
  5a8:	0900                	addi	s0,sp,144
  5aa:	0000                	unimp
  5ac:	0501                	addi	a0,a0,0
  5ae:	0315                	addi	t1,t1,5
  5b0:	0900                	addi	s0,sp,144
  5b2:	0000                	unimp
  5b4:	0501                	addi	a0,a0,0
  5b6:	0601                	addi	a2,a2,0
  5b8:	00097803          	0x97803
  5bc:	0100                	addi	s0,sp,128
  5be:	0e05                	addi	t3,t3,1
  5c0:	0c090803          	lb	a6,192(s2)
  5c4:	0100                	addi	s0,sp,128
  5c6:	2105                	jal	9e6 <_start-0x61a>
  5c8:	04090103          	lb	sp,64(s2)
  5cc:	0100                	addi	s0,sp,128
  5ce:	0505                	addi	a0,a0,1
  5d0:	04097f03          	0x4097f03
  5d4:	0100                	addi	s0,sp,128
  5d6:	0905                	addi	s2,s2,1
  5d8:	0200                	addi	s0,sp,256
  5da:	0304                	addi	s1,sp,384
  5dc:	0306                	slli	t1,t1,0x1
  5de:	0901                	addi	s2,s2,0
  5e0:	0004                	0x4
  5e2:	0501                	addi	a0,a0,0
  5e4:	0014                	0x14
  5e6:	0402                	c.slli64	s0
  5e8:	00030603          	lb	a2,0(t1)
  5ec:	0009                	c.nop	2
  5ee:	0100                	addi	s0,sp,128
  5f0:	2105                	jal	a10 <_start-0x5f0>
  5f2:	0200                	addi	s0,sp,256
  5f4:	0304                	addi	s1,sp,384
  5f6:	08090003          	lb	zero,128(s2)
  5fa:	0100                	addi	s0,sp,128
  5fc:	1405                	addi	s0,s0,-31
  5fe:	0200                	addi	s0,sp,256
  600:	0304                	addi	s1,sp,384
  602:	04090003          	lb	zero,64(s2)
  606:	0100                	addi	s0,sp,128
  608:	0905                	addi	s2,s2,1
  60a:	0200                	addi	s0,sp,256
  60c:	0304                	addi	s1,sp,384
  60e:	0306                	slli	t1,t1,0x1
  610:	0901                	addi	s2,s2,0
  612:	0008                	0x8
  614:	0501                	addi	a0,a0,0
  616:	0014                	0x14
  618:	0402                	c.slli64	s0
  61a:	00030603          	lb	a2,0(t1)
  61e:	0009                	c.nop	2
  620:	0100                	addi	s0,sp,128
  622:	1d05                	addi	s10,s10,-31
  624:	0200                	addi	s0,sp,256
  626:	0304                	addi	s1,sp,384
  628:	0306                	slli	t1,t1,0x1
  62a:	097e                	slli	s2,s2,0x1f
  62c:	0004                	0x4
  62e:	0501                	addi	a0,a0,0
  630:	001e                	c.slli	zero,0x7
  632:	0402                	c.slli64	s0
  634:	00030603          	lb	a2,0(t1)
  638:	0009                	c.nop	2
  63a:	0100                	addi	s0,sp,128
  63c:	1505                	addi	a0,a0,-31
  63e:	0200                	addi	s0,sp,256
  640:	0304                	addi	s1,sp,384
  642:	0306                	slli	t1,t1,0x1
  644:	0900                	addi	s0,sp,144
  646:	0004                	0x4
  648:	0501                	addi	a0,a0,0
  64a:	0005                	c.nop	1
  64c:	0402                	c.slli64	s0
  64e:	00030603          	lb	a2,0(t1)
  652:	0009                	c.nop	2
  654:	0100                	addi	s0,sp,128
  656:	0306                	slli	t1,t1,0x1
  658:	00040907          	0x40907
  65c:	0501                	addi	a0,a0,0
  65e:	0616                	slli	a2,a2,0x5
  660:	00090003          	lb	zero,0(s2)
  664:	0100                	addi	s0,sp,128
  666:	1405                	addi	s0,s0,-31
  668:	04090003          	lb	zero,64(s2)
  66c:	0100                	addi	s0,sp,128
  66e:	0505                	addi	a0,a0,1
  670:	0306                	slli	t1,t1,0x1
  672:	0901                	addi	s2,s2,0
  674:	0004                	0x4
  676:	0501                	addi	a0,a0,0
  678:	0616                	slli	a2,a2,0x5
  67a:	00090003          	lb	zero,0(s2)
  67e:	0100                	addi	s0,sp,128
  680:	1405                	addi	s0,s0,-31
  682:	04090003          	lb	zero,64(s2)
  686:	0100                	addi	s0,sp,128
  688:	0505                	addi	a0,a0,1
  68a:	0306                	slli	t1,t1,0x1
  68c:	0901                	addi	s2,s2,0
  68e:	0004                	0x4
  690:	0501                	addi	a0,a0,0
  692:	0612                	slli	a2,a2,0x4
  694:	00090003          	lb	zero,0(s2)
  698:	0100                	addi	s0,sp,128
  69a:	0505                	addi	a0,a0,1
  69c:	0306                	slli	t1,t1,0x1
  69e:	0901                	addi	s2,s2,0
  6a0:	0008                	0x8
  6a2:	0501                	addi	a0,a0,0
  6a4:	0616                	slli	a2,a2,0x5
  6a6:	00090003          	lb	zero,0(s2)
  6aa:	0100                	addi	s0,sp,128
  6ac:	1405                	addi	s0,s0,-31
  6ae:	04090003          	lb	zero,64(s2)
  6b2:	0100                	addi	s0,sp,128
  6b4:	0505                	addi	a0,a0,1
  6b6:	0306                	slli	t1,t1,0x1
  6b8:	00040903          	lb	s2,0(s0)
  6bc:	0501                	addi	a0,a0,0
  6be:	0616                	slli	a2,a2,0x5
  6c0:	00090003          	lb	zero,0(s2)
  6c4:	0100                	addi	s0,sp,128
  6c6:	1405                	addi	s0,s0,-31
  6c8:	04090003          	lb	zero,64(s2)
  6cc:	0100                	addi	s0,sp,128
  6ce:	0505                	addi	a0,a0,1
  6d0:	0306                	slli	t1,t1,0x1
  6d2:	0901                	addi	s2,s2,0
  6d4:	0004                	0x4
  6d6:	0501                	addi	a0,a0,0
  6d8:	0616                	slli	a2,a2,0x5
  6da:	00090003          	lb	zero,0(s2)
  6de:	0100                	addi	s0,sp,128
  6e0:	1405                	addi	s0,s0,-31
  6e2:	04090003          	lb	zero,64(s2)
  6e6:	0100                	addi	s0,sp,128
  6e8:	0505                	addi	a0,a0,1
  6ea:	0306                	slli	t1,t1,0x1
  6ec:	0901                	addi	s2,s2,0
  6ee:	0004                	0x4
  6f0:	0501                	addi	a0,a0,0
  6f2:	0612                	slli	a2,a2,0x4
  6f4:	00090003          	lb	zero,0(s2)
  6f8:	0100                	addi	s0,sp,128
  6fa:	0505                	addi	a0,a0,1
  6fc:	0306                	slli	t1,t1,0x1
  6fe:	0901                	addi	s2,s2,0
  700:	0004                	0x4
  702:	0501                	addi	a0,a0,0
  704:	0614                	addi	a3,sp,768
  706:	00090003          	lb	zero,0(s2)
  70a:	0100                	addi	s0,sp,128
  70c:	0505                	addi	a0,a0,1
  70e:	0306                	slli	t1,t1,0x1
  710:	0902                	c.slli64	s2
  712:	0004                	0x4
  714:	0301                	addi	t1,t1,0
  716:	00040903          	lb	s2,0(s0)
  71a:	0301                	addi	t1,t1,0
  71c:	0901                	addi	s2,s2,0
  71e:	000c                	0xc
  720:	0301                	addi	t1,t1,0
  722:	0902                	c.slli64	s2
  724:	0008                	0x8
  726:	0301                	addi	t1,t1,0
  728:	0901                	addi	s2,s2,0
  72a:	000c                	0xc
  72c:	0501                	addi	a0,a0,0
  72e:	00030613          	mv	a2,t1
  732:	0009                	c.nop	2
  734:	0100                	addi	s0,sp,128
  736:	0f05                	addi	t5,t5,1
  738:	04090103          	lb	sp,64(s2)
  73c:	0100                	addi	s0,sp,128
  73e:	1305                	addi	t1,t1,-31
  740:	04097f03          	0x4097f03
  744:	0100                	addi	s0,sp,128
  746:	0505                	addi	a0,a0,1
  748:	0306                	slli	t1,t1,0x1
  74a:	0901                	addi	s2,s2,0
  74c:	0004                	0x4
  74e:	0501                	addi	a0,a0,0
  750:	0003060f          	0x3060f
  754:	0009                	c.nop	2
  756:	0100                	addi	s0,sp,128
  758:	0e05                	addi	t3,t3,1
  75a:	04090403          	lb	s0,64(s2)
  75e:	0100                	addi	s0,sp,128
  760:	0505                	addi	a0,a0,1
  762:	0306                	slli	t1,t1,0x1
  764:	097f                	0x97f
  766:	0004                	0x4
  768:	0501                	addi	a0,a0,0
  76a:	0309                	addi	t1,t1,2
  76c:	0901                	addi	s2,s2,0
  76e:	0000                	unimp
  770:	0501                	addi	a0,a0,0
  772:	060e                	slli	a2,a2,0x3
  774:	00090003          	lb	zero,0(s2)
  778:	0100                	addi	s0,sp,128
  77a:	1b05                	addi	s6,s6,-31
  77c:	04090003          	lb	zero,64(s2)
  780:	0100                	addi	s0,sp,128
  782:	0c05                	addi	s8,s8,1
  784:	04090003          	lb	zero,64(s2)
  788:	0100                	addi	s0,sp,128
  78a:	0505                	addi	a0,a0,1
  78c:	0306                	slli	t1,t1,0x1
  78e:	0904                	addi	s1,sp,144
  790:	0004                	0x4
  792:	0301                	addi	t1,t1,0
  794:	000c0903          	lb	s2,0(s8)
  798:	0301                	addi	t1,t1,0
  79a:	0901                	addi	s2,s2,0
  79c:	0000                	unimp
  79e:	0501                	addi	a0,a0,0
  7a0:	030a                	slli	t1,t1,0x2
  7a2:	0900                	addi	s0,sp,144
  7a4:	0000                	unimp
  7a6:	0501                	addi	a0,a0,0
  7a8:	0315                	addi	t1,t1,5
  7aa:	0900                	addi	s0,sp,144
  7ac:	0000                	unimp
  7ae:	0501                	addi	a0,a0,0
  7b0:	0609                	addi	a2,a2,2
  7b2:	00097f03          	0x97f03
  7b6:	0100                	addi	s0,sp,128
  7b8:	0e05                	addi	t3,t3,1
  7ba:	04090103          	lb	sp,64(s2)
  7be:	0100                	addi	s0,sp,128
  7c0:	0505                	addi	a0,a0,1
  7c2:	04090003          	lb	zero,64(s2)
  7c6:	0100                	addi	s0,sp,128
  7c8:	0905                	addi	s2,s2,1
  7ca:	0306                	slli	t1,t1,0x1
  7cc:	0901                	addi	s2,s2,0
  7ce:	0004                	0x4
  7d0:	0501                	addi	a0,a0,0
  7d2:	0614                	addi	a3,sp,768
  7d4:	00090003          	lb	zero,0(s2)
  7d8:	0100                	addi	s0,sp,128
  7da:	2205                	jal	8fa <_start-0x706>
  7dc:	10090003          	lb	zero,256(s2)
  7e0:	0100                	addi	s0,sp,128
  7e2:	1e05                	addi	t3,t3,-31
  7e4:	04097f03          	0x4097f03
  7e8:	0100                	addi	s0,sp,128
  7ea:	1005                	c.nop	-31
  7ec:	04090203          	lb	tp,64(s2)
  7f0:	0100                	addi	s0,sp,128
  7f2:	1d05                	addi	s10,s10,-31
  7f4:	0306                	slli	t1,t1,0x1
  7f6:	097e                	slli	s2,s2,0x1f
  7f8:	000c                	0xc
  7fa:	0501                	addi	a0,a0,0
  7fc:	0315                	addi	t1,t1,5
  7fe:	0900                	addi	s0,sp,144
  800:	0000                	unimp
  802:	0501                	addi	a0,a0,0
  804:	0605                	addi	a2,a2,1
  806:	00090003          	lb	zero,0(s2)
  80a:	0100                	addi	s0,sp,128
  80c:	0306                	slli	t1,t1,0x1
  80e:	00040907          	0x40907
  812:	0501                	addi	a0,a0,0
  814:	0608                	addi	a0,sp,768
  816:	00090003          	lb	zero,0(s2)
  81a:	0100                	addi	s0,sp,128
  81c:	0905                	addi	s2,s2,1
  81e:	0306                	slli	t1,t1,0x1
  820:	0901                	addi	s2,s2,0
  822:	0004                	0x4
  824:	0301                	addi	t1,t1,0
  826:	0901                	addi	s2,s2,0
  828:	000c                	0xc
  82a:	0301                	addi	t1,t1,0
  82c:	0901                	addi	s2,s2,0
  82e:	000c                	0xc
  830:	0501                	addi	a0,a0,0
  832:	0005                	c.nop	1
  834:	0402                	c.slli64	s0
  836:	0301                	addi	t1,t1,0
  838:	0906                	slli	s2,s2,0x1
  83a:	000c                	0xc
  83c:	0501                	addi	a0,a0,0
  83e:	0009                	c.nop	2
  840:	0402                	c.slli64	s0
  842:	0301                	addi	t1,t1,0
  844:	0901                	addi	s2,s2,0
  846:	0000                	unimp
  848:	0501                	addi	a0,a0,0
  84a:	0402000b          	0x402000b
  84e:	0301                	addi	t1,t1,0
  850:	097f                	0x97f
  852:	0000                	unimp
  854:	0501                	addi	a0,a0,0
  856:	0005                	c.nop	1
  858:	0402                	c.slli64	s0
  85a:	0301                	addi	t1,t1,0
  85c:	0900                	addi	s0,sp,144
  85e:	0000                	unimp
  860:	0501                	addi	a0,a0,0
  862:	0009                	c.nop	2
  864:	0402                	c.slli64	s0
  866:	0301                	addi	t1,t1,0
  868:	0901                	addi	s2,s2,0
  86a:	0000                	unimp
  86c:	0501                	addi	a0,a0,0
  86e:	0402000b          	0x402000b
  872:	0301                	addi	t1,t1,0
  874:	097f                	0x97f
  876:	0000                	unimp
  878:	0501                	addi	a0,a0,0
  87a:	0309                	addi	t1,t1,2
  87c:	097c                	addi	a5,sp,156
  87e:	0004                	0x4
  880:	0301                	addi	t1,t1,0
  882:	0901                	addi	s2,s2,0
  884:	000c                	0xc
  886:	0901                	addi	s2,s2,0
  888:	000c                	0xc
  88a:	0100                	addi	s0,sp,128
  88c:	01              	Address 0x000000000000088c is out of bounds.


  Disassembly of section .debug_info:

  00000000 <.debug_info>:
    0:	0022                	c.slli	zero,0x8
    2:	0000                	unimp
    4:	0002                	c.slli64	zero
    6:	0000                	unimp
    8:	0000                	unimp
    a:	0104                	addi	s1,sp,128
    c:	0000                	unimp
    e:	0000                	unimp
    10:	1000                	addi	s0,sp,32
    12:	0000                	unimp
    14:	1010                	addi	a2,sp,32
    16:	0000                	unimp
    18:	0000                	unimp
    1a:	0000                	unimp
    1c:	0008                	0x8
    1e:	0000                	unimp
    20:	0031                	c.nop	12
    22:	0000                	unimp
    24:	8001                	c.srli64	s0
    26:	03de                	slli	t2,t2,0x17
    28:	0000                	unimp
    2a:	0004                	0x4
    2c:	0014                	0x14
    2e:	0000                	unimp
    30:	0104                	addi	s1,sp,128
    32:	000000d3          	fadd.s	ft1,ft0,ft0,rne
    36:	7e0c                	flw	fa1,56(a2)
    38:	0000                	unimp
    3a:	0800                	addi	s0,sp,16
    3c:	0000                	unimp
    3e:	d800                	sw	s0,48(s0)
    40:	0000                	unimp
    42:	0000                	unimp
    44:	0000                	unimp
    46:	4200                	lw	s0,0(a2)
    48:	0000                	unimp
    4a:	0200                	addi	s0,sp,256
    4c:	00b5                	addi	ra,ra,13
    4e:	0000                	unimp
    50:	0401                	addi	s0,s0,0
    52:	3616                	fld	fa2,352(sp)
    54:	0000                	unimp
    56:	0300                	addi	s0,sp,384
    58:	0025                	c.nop	9
    5a:	0000                	unimp
    5c:	0404                	addi	s1,sp,512
    5e:	00004b07          	flq	fs6,0(zero) # 0 <_start-0x1000>
    62:	0400                	addi	s0,sp,512
    64:	0801                	addi	a6,a6,0
    66:	0095                	addi	ra,ra,5
    68:	0000                	unimp
    6a:	1005                	c.nop	-31
    6c:	0110                	addi	a2,sp,128
    6e:	0919                	addi	s2,s2,6
    70:	00000083          	lb	ra,0(zero) # 0 <_start-0x1000>
    74:	8c06                	mv	s8,ra
    76:	0000                	unimp
    78:	0100                	addi	s0,sp,128
    7a:	0e1a                	slli	t3,t3,0x6
    7c:	0025                	c.nop	9
    7e:	0000                	unimp
    80:	0600                	addi	s0,sp,768
    82:	00ac                	addi	a1,sp,72
    84:	0000                	unimp
    86:	1b01                	addi	s6,s6,-32
    88:	250e                	fld	fa0,192(sp)
    8a:	0000                	unimp
    8c:	0400                	addi	s0,sp,512
    8e:	a306                	fsd	ft1,384(sp)
    90:	0000                	unimp
    92:	0100                	addi	s0,sp,128
    94:	0e1c                	addi	a5,sp,784
    96:	0025                	c.nop	9
    98:	0000                	unimp
    9a:	0608                	addi	a0,sp,768
    9c:	0000006f          	j	9c <_start-0xf64>
    a0:	1d01                	addi	s10,s10,-32
    a2:	250e                	fld	fa0,192(sp)
    a4:	0000                	unimp
    a6:	0c00                	addi	s0,sp,528
    a8:	0700                	addi	s0,sp,896
    aa:	00be                	slli	ra,ra,0xf
    ac:	0000                	unimp
    ae:	1e01                	addi	t3,t3,-32
    b0:	4420                	lw	s0,72(s0)
    b2:	0000                	unimp
    b4:	1000                	addi	s0,sp,32
    b6:	00008303          	lb	t1,0(ra)
    ba:	0800                	addi	s0,sp,16
    bc:	006a                	c.slli	zero,0x1a
    be:	0000                	unimp
    c0:	3f01                	jal	ffffffd0 <_stack_top+0xffefffd0>
    c2:	f905                	bnez	a0,fffffff2 <_stack_top+0xffeffff2>
    c4:	0001                	nop
    c6:	dc00                	sw	s0,56(s0)
    c8:	0011                	c.nop	4
    ca:	3800                	fld	fs0,48(s0)
    cc:	0001                	nop
    ce:	0100                	addi	s0,sp,128
    d0:	f99c                	fsw	fa5,48(a1)
    d2:	0001                	nop
    d4:	0900                	addi	s0,sp,144
    d6:	0058                	addi	a4,sp,4
    d8:	0000                	unimp
    da:	4201                	li	tp,0
    dc:	00021517          	auipc	a0,0x21
    e0:	1000                	addi	s0,sp,32
    e2:	7df09103          	lh	sp,2015(ra)
    e6:	7609                	lui	a2,0xfffe2
    e8:	0000                	unimp
    ea:	0100                	addi	s0,sp,128
    ec:	02151743          	fmadd.d	fa4,fa0,ft1,ft0,rtz
    f0:	0000                	unimp
    f2:	0310                	addi	a2,sp,384
    f4:	f091                	bnez	s1,fffffff8 <_stack_top+0xffeffff8>
    f6:	097e                	slli	s2,s2,0x1f
    f8:	0000003f 90194401 	0x901944010000003f
  100:	0000                	unimp
  102:	1000                	addi	s0,sp,32
  104:	7dd09103          	lh	sp,2013(ra)
  108:	4509                	li	a0,2
  10a:	0000                	unimp
  10c:	0100                	addi	s0,sp,128
  10e:	1945                	addi	s2,s2,-15
  110:	0090                	addi	a2,sp,64
  112:	0000                	unimp
  114:	0310                	addi	a2,sp,384
  116:	e091                	bnez	s1,11a <_start-0xee6>
  118:	0a7d                	addi	s4,s4,31
  11a:	7265                	lui	tp,0xffff9
  11c:	0072                	c.slli	zero,0x1c
  11e:	6d01                	0x6d01
  120:	f909                	bnez	a0,32 <_start-0xfce>
  122:	0001                	nop
  124:	0000                	unimp
  126:	0000                	unimp
  128:	0b00                	addi	s0,sp,400
  12a:	00a8                	addi	a0,sp,72
  12c:	0000                	unimp
  12e:	0000011b          	0x11b
  132:	690a                	flw	fs2,128(sp)
  134:	0100                	addi	s0,sp,128
  136:	0e48                	addi	a0,sp,788
  138:	01f9                	addi	gp,gp,30
  13a:	0000                	unimp
  13c:	001f 0000 0b00      	0xb000000001f
  142:	00c0                	addi	s0,sp,68
  144:	0000                	unimp
  146:	00000133          	add	sp,zero,zero
  14a:	690a                	flw	fs2,128(sp)
  14c:	0100                	addi	s0,sp,128
  14e:	0e6e                	slli	t3,t3,0x1b
  150:	01f9                	addi	gp,gp,30
  152:	0000                	unimp
  154:	003e                	c.slli	zero,0xf
  156:	0000                	unimp
  158:	0c00                	addi	s0,sp,528
  15a:	125c                	addi	a5,sp,292
  15c:	0000                	unimp
  15e:	0329                	addi	t1,t1,10
  160:	0000                	unimp
  162:	014a                	slli	sp,sp,0x12
  164:	0000                	unimp
  166:	010d                	addi	sp,sp,3
  168:	055a                	slli	a0,a0,0x16
  16a:	00132c03          	lw	s8,1(t1)
  16e:	0000                	unimp
  170:	640c                	flw	fa1,8(s0)
  172:	0012                	c.slli	zero,0x4
  174:	1a00                	addi	s0,sp,304
  176:	0002                	c.slli64	zero
  178:	5e00                	lw	s0,56(a2)
  17a:	0001                	nop
  17c:	0d00                	addi	s0,sp,656
  17e:	5a01                	li	s4,-32
  180:	7202                	flw	ft4,32(sp)
  182:	0000                	unimp
  184:	700c                	flw	fa1,32(s0)
  186:	0012                	c.slli	zero,0x4
  188:	2900                	fld	fs0,16(a0)
  18a:	75000003          	lb	zero,1872(zero) # 750 <_start-0x8b0>
  18e:	0001                	nop
  190:	0d00                	addi	s0,sp,656
  192:	5a01                	li	s4,-32
  194:	0305                	addi	t1,t1,1
  196:	1344                	addi	s1,sp,420
  198:	0000                	unimp
  19a:	0c00                	addi	s0,sp,528
  19c:	129c                	addi	a5,sp,352
  19e:	0000                	unimp
  1a0:	0329                	addi	t1,t1,10
  1a2:	0000                	unimp
  1a4:	018c                	addi	a1,sp,192
  1a6:	0000                	unimp
  1a8:	010d                	addi	sp,sp,3
  1aa:	055a                	slli	a0,a0,0x16
  1ac:	00135803          	lhu	a6,1(t1)
  1b0:	0000                	unimp
  1b2:	e00c                	fsw	fa1,0(s0)
  1b4:	0012                	c.slli	zero,0x4
  1b6:	2900                	fld	fs0,16(a0)
  1b8:	a3000003          	lb	zero,-1488(zero) # fffffa30 <_stack_top+0xffeffa30>
  1bc:	0001                	nop
  1be:	0d00                	addi	s0,sp,656
  1c0:	5a01                	li	s4,-32
  1c2:	0305                	addi	t1,t1,1
  1c4:	1364                	addi	s1,sp,428
  1c6:	0000                	unimp
  1c8:	0c00                	addi	s0,sp,528
  1ca:	12ec                	addi	a1,sp,364
  1cc:	0000                	unimp
  1ce:	0329                	addi	t1,t1,10
  1d0:	0000                	unimp
  1d2:	01ba                	slli	gp,gp,0xe
  1d4:	0000                	unimp
  1d6:	010d                	addi	sp,sp,3
  1d8:	055a                	slli	a0,a0,0x16
  1da:	00138003          	lb	zero,1(t2)
  1de:	0000                	unimp
  1e0:	f80c                	fsw	fa1,48(s0)
  1e2:	0012                	c.slli	zero,0x4
  1e4:	2900                	fld	fs0,16(a0)
  1e6:	d1000003          	lb	zero,-752(zero) # fffffd10 <_stack_top+0xffeffd10>
  1ea:	0001                	nop
  1ec:	0d00                	addi	s0,sp,656
  1ee:	5a01                	li	s4,-32
  1f0:	0305                	addi	t1,t1,1
  1f2:	1398                	addi	a4,sp,480
  1f4:	0000                	unimp
  1f6:	0c00                	addi	s0,sp,528
  1f8:	1308                	addi	a0,sp,416
  1fa:	0000                	unimp
  1fc:	0329                	addi	t1,t1,10
  1fe:	0000                	unimp
  200:	01e8                	addi	a0,sp,204
  202:	0000                	unimp
  204:	010d                	addi	sp,sp,3
  206:	055a                	slli	a0,a0,0x16
  208:	0013b403          	0x13b403
  20c:	0000                	unimp
  20e:	100e                	c.slli	zero,0x23
  210:	1a000013          	li	zero,416
  214:	0002                	c.slli64	zero
  216:	0d00                	addi	s0,sp,656
  218:	5a01                	li	s4,-32
  21a:	7802                	flw	fa6,32(sp)
  21c:	0000                	unimp
  21e:	0f00                	addi	s0,sp,912
  220:	0504                	addi	s1,sp,640
  222:	6e69                	lui	t3,0x1a
  224:	0074                	addi	a3,sp,12
  226:	0001f903          	0x1f903
  22a:	1000                	addi	s0,sp,32
  22c:	0031                	c.nop	12
  22e:	0000                	unimp
  230:	0215                	addi	tp,tp,5
  232:	0000                	unimp
  234:	3611                	jal	fffffd38 <_stack_top+0xffeffd38>
  236:	0000                	unimp
  238:	1f00                	addi	s0,sp,944
  23a:	0300                	addi	s0,sp,384
  23c:	0205                	addi	tp,tp,1
  23e:	0000                	unimp
  240:	0212                	slli	tp,tp,0x4
  242:	0001                	nop
  244:	0100                	addi	s0,sp,128
  246:	0632                	slli	a2,a2,0xc
  248:	10b4                	addi	a3,sp,104
  24a:	0000                	unimp
  24c:	0128                	addi	a0,sp,136
  24e:	0000                	unimp
  250:	9c01                	0x9c01
  252:	0308                	addi	a0,sp,384
  254:	0000                	unimp
  256:	6c617613          	andi	a2,sp,1734
  25a:	0100                	addi	s0,sp,128
  25c:	1c32                	slli	s8,s8,0x2c
  25e:	0025                	c.nop	9
  260:	0000                	unimp
  262:	5a01                	li	s4,-32
  264:	6814                	flw	fa3,16(s0)
  266:	7865                	lui	a6,0xffff9
  268:	0100                	addi	s0,sp,128
  26a:	1034                	addi	a3,sp,40
  26c:	0318                	addi	a4,sp,384
  26e:	0000                	unimp
  270:	9102                	jalr	sp
  272:	156c                	addi	a1,sp,684
  274:	113c                	addi	a5,sp,168
  276:	0000                	unimp
  278:	0058                	addi	a4,sp,4
  27a:	0000                	unimp
  27c:	0291                	addi	t0,t0,4
  27e:	0000                	unimp
  280:	6916                	flw	fs2,68(sp)
  282:	0100                	addi	s0,sp,128
  284:	0e36                	slli	t3,t3,0xd
  286:	01f9                	addi	gp,gp,30
  288:	0000                	unimp
  28a:	00034717          	auipc	a4,0x34
  28e:	3c00                	fld	fs0,56(s0)
  290:	0011                	c.nop	4
  292:	7800                	flw	fs0,48(s0)
  294:	0000                	unimp
  296:	0100                	addi	s0,sp,128
  298:	54180937          	lui	s2,0x54180
  29c:	80000003          	lb	zero,-2048(zero) # fffff800 <_stack_top+0xffeff800>
  2a0:	0000                	unimp
  2a2:	1900                	addi	s0,sp,176
  2a4:	035e                	slli	t1,t1,0x17
  2a6:	0000                	unimp
  2a8:	0090                	addi	a2,sp,64
  2aa:	0000                	unimp
  2ac:	5f1a                	lw	t5,164(sp)
  2ae:	02000003          	lb	zero,32(zero) # 20 <_start-0xfe0>
  2b2:	6491                	lui	s1,0x4
  2b4:	0000                	unimp
  2b6:	1b00                	addi	s0,sp,432
  2b8:	0329                	addi	t1,t1,10
  2ba:	0000                	unimp
  2bc:	10ec                	addi	a1,sp,108
  2be:	0000                	unimp
  2c0:	0030                	addi	a2,sp,8
  2c2:	0000                	unimp
  2c4:	3501                	jal	c4 <_start-0xf3c>
  2c6:	db05                	beqz	a4,1f6 <_start-0xe0a>
  2c8:	0002                	c.slli64	zero
  2ca:	1800                	addi	s0,sp,48
  2cc:	0336                	slli	t1,t1,0xd
  2ce:	0000                	unimp
  2d0:	00000093          	li	ra,0
  2d4:	00034717          	auipc	a4,0x34
  2d8:	f400                	fsw	fs0,40(s0)
  2da:	0010                	0x10
  2dc:	4800                	lw	s0,16(s0)
  2de:	0000                	unimp
  2e0:	0100                	addi	s0,sp,128
  2e2:	5418092f          	0x5418092f
  2e6:	b6000003          	lb	zero,-1184(zero) # fffffb60 <_stack_top+0xffeffb60>
  2ea:	0000                	unimp
  2ec:	1900                	addi	s0,sp,176
  2ee:	035e                	slli	t1,t1,0x17
  2f0:	0000                	unimp
  2f2:	0060                	addi	s0,sp,12
  2f4:	0000                	unimp
  2f6:	5f1a                	lw	t5,164(sp)
  2f8:	02000003          	lb	zero,32(zero) # 20 <_start-0xfe0>
  2fc:	6091                	lui	ra,0x4
  2fe:	0000                	unimp
  300:	1c00                	addi	s0,sp,560
  302:	00000347          	fmsub.s	ft6,ft0,ft0,ft0,rne
  306:	1194                	addi	a3,sp,224
  308:	0000                	unimp
  30a:	0040                	addi	s0,sp,4
  30c:	0000                	unimp
  30e:	3901                	jal	ffffff1e <_stack_top+0xffefff1e>
  310:	1d05                	addi	s10,s10,-31
  312:	0354                	addi	a3,sp,388
  314:	0000                	unimp
  316:	5e1e                	lw	t3,228(sp)
  318:	ac000003          	lb	zero,-1344(zero) # fffffac0 <_stack_top+0xffeffac0>
  31c:	0011                	c.nop	4
  31e:	2800                	fld	fs0,16(s0)
  320:	0000                	unimp
  322:	1a00                	addi	s0,sp,304
  324:	035f 0000 9102      	0x91020000035f
  32a:	0068                	addi	a0,sp,12
  32c:	0000                	unimp
  32e:	2410                	fld	fa2,8(s0)
  330:	18000003          	lb	zero,384(zero) # 180 <_start-0xe80>
  334:	11000003          	lb	zero,272(zero) # 110 <_start-0xef0>
  338:	0036                	c.slli	zero,0xd
  33a:	0000                	unimp
  33c:	0010                	0x10
  33e:	081f 0003 0400      	0x4000003081f
  344:	0801                	addi	a6,a6,0
  346:	009e                	slli	ra,ra,0x7
  348:	0000                	unimp
  34a:	1d1f 0003 2000      	0x200000031d1f
  350:	00c9                	addi	ra,ra,18
  352:	0000                	unimp
  354:	2c01                	jal	564 <_start-0xa9c>
  356:	0106                	slli	sp,sp,0x1
  358:	0341                	addi	t1,t1,16
  35a:	0000                	unimp
  35c:	7321                	lui	t1,0xfffe8
  35e:	0100                	addi	s0,sp,128
  360:	1c2c                	addi	a1,sp,568
  362:	0341                	addi	t1,t1,16
  364:	0000                	unimp
  366:	2200                	fld	fs0,0(a2)
  368:	2404                	fld	fs1,8(s0)
  36a:	20000003          	lb	zero,512(zero) # 200 <_start-0xe00>
  36e:	0060                	addi	s0,sp,12
  370:	0000                	unimp
  372:	2301                	jal	872 <_start-0x78e>
  374:	0106                	slli	sp,sp,0x1
  376:	0000036b          	0x36b
  37a:	6321                	lui	t1,0x8
  37c:	0100                	addi	s0,sp,128
  37e:	031d1523          	sh	a7,42(s10)
  382:	0000                	unimp
  384:	00691623          	sh	t1,12(s2) # 5418000c <_stack_top+0x5408000c>
  388:	2801                	jal	398 <_start-0xc68>
  38a:	00020017          	auipc	zero,0x20
  38e:	0000                	unimp
  390:	2400                	fld	fs0,8(s0)
  392:	00000347          	fmsub.s	ft6,ft0,ft0,ft0,rne
  396:	1010                	addi	a2,sp,32
  398:	0000                	unimp
  39a:	0048                	addi	a0,sp,4
  39c:	0000                	unimp
  39e:	9c01                	0x9c01
  3a0:	039c                	addi	a5,sp,448
  3a2:	0000                	unimp
  3a4:	5425                	li	s0,-23
  3a6:	01000003          	lb	zero,16(zero) # 10 <_start-0xff0>
  3aa:	1e5a                	slli	t3,t3,0x36
  3ac:	035e                	slli	t1,t1,0x17
  3ae:	0000                	unimp
  3b0:	1028                	addi	a0,sp,40
  3b2:	0000                	unimp
  3b4:	0028                	addi	a0,sp,8
  3b6:	0000                	unimp
  3b8:	5f1a                	lw	t5,164(sp)
  3ba:	02000003          	lb	zero,32(zero) # 20 <_start-0xfe0>
  3be:	7c91                	lui	s9,0xfffe4
  3c0:	0000                	unimp
  3c2:	2926                	fld	fs2,72(sp)
  3c4:	58000003          	lb	zero,1408(zero) # 580 <_start-0xa80>
  3c8:	0010                	0x10
  3ca:	5c00                	lw	s0,56(s0)
  3cc:	0000                	unimp
  3ce:	0100                	addi	s0,sp,128
  3d0:	189c                	addi	a5,sp,112
  3d2:	0336                	slli	t1,t1,0xd
  3d4:	0000                	unimp
  3d6:	00c9                	addi	ra,ra,18
  3d8:	0000                	unimp
  3da:	00034717          	auipc	a4,0x34
  3de:	6400                	flw	fs0,8(s0)
  3e0:	0010                	0x10
  3e2:	0000                	unimp
  3e4:	0000                	unimp
  3e6:	0100                	addi	s0,sp,128
  3e8:	5418092f          	0x5418092f
  3ec:	e7000003          	lb	zero,-400(zero) # fffffe70 <_stack_top+0xffeffe70>
  3f0:	0000                	unimp
  3f2:	1900                	addi	s0,sp,176
  3f4:	035e                	slli	t1,t1,0x17
  3f6:	0000                	unimp
  3f8:	0018                	0x18
  3fa:	0000                	unimp
  3fc:	5f1a                	lw	t5,164(sp)
  3fe:	02000003          	lb	zero,32(zero) # 20 <_start-0xfe0>
  402:	7c91                	lui	s9,0xfffe4
  404:	0000                	unimp
    ...

  Disassembly of section .debug_abbrev:

  00000000 <.debug_abbrev>:
    0:	1101                	addi	sp,sp,-32
    2:	1000                	addi	s0,sp,32
    4:	1106                	slli	sp,sp,0x21
    6:	1201                	addi	tp,tp,-32
    8:	0301                	addi	t1,t1,0
    a:	1b0e                	slli	s6,s6,0x23
    c:	250e                	fld	fa0,192(sp)
    e:	130e                	slli	t1,t1,0x23
    10:	0005                	c.nop	1
    12:	0000                	unimp
    14:	1101                	addi	sp,sp,-32
    16:	2501                	jal	616 <_start-0x9ea>
    18:	130e                	slli	t1,t1,0x23
    1a:	1b0e030b          	0x1b0e030b
    1e:	550e                	lw	a0,224(sp)
    20:	10011117          	auipc	sp,0x10011
    24:	02000017          	auipc	zero,0x2000
    28:	0016                	c.slli	zero,0x5
    2a:	0b3a0e03          	lb	t3,179(s4)
    2e:	0b390b3b          	0xb390b3b
    32:	1349                	addi	t1,t1,-14
    34:	0000                	unimp
    36:	49003503          	0x49003503
    3a:	04000013          	li	zero,64
    3e:	0024                	addi	s1,sp,8
    40:	0b3e0b0b          	0xb3e0b0b
    44:	00000e03          	lb	t3,0(zero) # 0 <_start-0x1000>
    48:	1305                	addi	t1,t1,-31
    4a:	0b01                	addi	s6,s6,0
    4c:	0b01880b          	0xb01880b
    50:	0b3a                	slli	s6,s6,0xe
    52:	0b390b3b          	0xb390b3b
    56:	1301                	addi	t1,t1,-32
    58:	0000                	unimp
    5a:	0d06                	slli	s10,s10,0x1
    5c:	0300                	addi	s0,sp,384
    5e:	3a0e                	fld	fs4,224(sp)
    60:	390b3b0b          	0x390b3b0b
    64:	3813490b          	0x3813490b
    68:	0700000b          	0x700000b
    6c:	0016                	c.slli	zero,0x5
    6e:	0b3a0e03          	lb	t3,179(s4)
    72:	0b390b3b          	0xb390b3b
    76:	1349                	addi	t1,t1,-14
    78:	0188                	addi	a0,sp,192
    7a:	0800000b          	0x800000b
    7e:	012e                	slli	sp,sp,0xb
    80:	0e03193f 0b3b0b3a 	0xb3b0b3a0e03193f
    88:	0b39                	addi	s6,s6,14
    8a:	1349                	addi	t1,t1,-14
    8c:	0111                	addi	sp,sp,4
    8e:	0612                	slli	a2,a2,0x4
    90:	1840                	addi	s0,sp,52
    92:	01194297          	auipc	t0,0x1194
    96:	09000013          	li	zero,144
    9a:	0034                	addi	a3,sp,8
    9c:	0b3a0e03          	lb	t3,179(s4)
    a0:	0b390b3b          	0xb390b3b
    a4:	1349                	addi	t1,t1,-14
    a6:	0188                	addi	a0,sp,192
    a8:	0018020b          	0x18020b
    ac:	0a00                	addi	s0,sp,272
    ae:	0034                	addi	a3,sp,8
    b0:	0b3a0803          	lb	a6,179(s4)
    b4:	0b390b3b          	0xb390b3b
    b8:	1349                	addi	t1,t1,-14
    ba:	1702                	slli	a4,a4,0x20
    bc:	0000                	unimp
    be:	55010b0b          	0x55010b0b
    c2:	00130117          	auipc	sp,0x130
    c6:	0c00                	addi	s0,sp,528
    c8:	8289                	srli	a3,a3,0x2
    ca:	0101                	addi	sp,sp,0
    cc:	0111                	addi	sp,sp,4
    ce:	1331                	addi	t1,t1,-20
    d0:	1301                	addi	t1,t1,-32
    d2:	0000                	unimp
    d4:	8a0d                	andi	a2,a2,3
    d6:	0182                	c.slli64	gp
    d8:	0200                	addi	s0,sp,256
    da:	9118                	0x9118
    dc:	1842                	slli	a6,a6,0x30
    de:	0000                	unimp
    e0:	890e                	mv	s2,gp
    e2:	0182                	c.slli64	gp
    e4:	1101                	addi	sp,sp,-32
    e6:	3101                	jal	fffffce6 <_stack_top+0xffeffce6>
    e8:	0f000013          	li	zero,240
    ec:	0024                	addi	s1,sp,8
    ee:	0b3e0b0b          	0xb3e0b0b
    f2:	00000803          	lb	a6,0(zero) # 0 <_start-0x1000>
    f6:	0110                	addi	a2,sp,128
    f8:	4901                	li	s2,0
    fa:	00130113          	addi	sp,t1,1 # 8001 <main+0x6e25>
    fe:	1100                	addi	s0,sp,160
  100:	0021                	c.nop	8
  102:	1349                	addi	t1,t1,-14
  104:	00000b2f          	0xb2f
  108:	2e12                	fld	ft8,256(sp)
  10a:	3f01                	jal	1a <_start-0xfe6>
  10c:	0319                	addi	t1,t1,6
  10e:	3a0e                	fld	fs4,224(sp)
  110:	390b3b0b          	0x390b3b0b
  114:	1119270b          	0x1119270b
  118:	1201                	addi	tp,tp,-32
  11a:	4006                	0x4006
  11c:	9718                	0x9718
  11e:	1942                	slli	s2,s2,0x30
  120:	1301                	addi	t1,t1,-32
  122:	0000                	unimp
  124:	03000513          	li	a0,48
  128:	3a08                	fld	fa0,48(a2)
  12a:	390b3b0b          	0x390b3b0b
  12e:	0213490b          	0x213490b
  132:	0018                	0x18
  134:	1400                	addi	s0,sp,544
  136:	0034                	addi	a3,sp,8
  138:	0b3a0803          	lb	a6,179(s4)
  13c:	0b390b3b          	0xb390b3b
  140:	1349                	addi	t1,t1,-14
  142:	1802                	slli	a6,a6,0x20
  144:	0000                	unimp
  146:	0b15                	addi	s6,s6,5
  148:	1101                	addi	sp,sp,-32
  14a:	1201                	addi	tp,tp,-32
  14c:	0106                	slli	sp,sp,0x1
  14e:	16000013          	li	zero,352
  152:	0034                	addi	a3,sp,8
  154:	0b3a0803          	lb	a6,179(s4)
  158:	0b390b3b          	0xb390b3b
  15c:	1349                	addi	t1,t1,-14
  15e:	0000                	unimp
  160:	31011d17          	auipc	s10,0x31011
  164:	55015213          	0x55015213
  168:	590b5817          	auipc	a6,0x590b5
  16c:	000b570b          	0xb570b
  170:	1800                	addi	s0,sp,48
  172:	0005                	c.nop	1
  174:	1331                	addi	t1,t1,-20
  176:	1702                	slli	a4,a4,0x20
  178:	0000                	unimp
  17a:	0b19                	addi	s6,s6,6
  17c:	3101                	jal	fffffd7c <_stack_top+0xffeffd7c>
  17e:	00175513          	srli	a0,a4,0x1
  182:	1a00                	addi	s0,sp,304
  184:	0034                	addi	a3,sp,8
  186:	1331                	addi	t1,t1,-20
  188:	1802                	slli	a6,a6,0x20
  18a:	0000                	unimp
  18c:	31011d1b          	0x31011d1b
  190:	55015213          	0x55015213
  194:	590b5817          	auipc	a6,0x590b5
  198:	010b570b          	0x10b570b
  19c:	1c000013          	li	zero,448
  1a0:	011d                	addi	sp,sp,7
  1a2:	1331                	addi	t1,t1,-20
  1a4:	0111                	addi	sp,sp,4
  1a6:	0612                	slli	a2,a2,0x4
  1a8:	0b58                	addi	a4,sp,404
  1aa:	0b59                	addi	s6,s6,22
  1ac:	00000b57          	0xb57
  1b0:	051d                	addi	a0,a0,7
  1b2:	3100                	fld	fs0,32(a0)
  1b4:	1e000013          	li	zero,480
  1b8:	1331010b          	0x1331010b
  1bc:	0111                	addi	sp,sp,4
  1be:	0612                	slli	a2,a2,0x4
  1c0:	0000                	unimp
  1c2:	261f 4900 0013      	0x134900261f
  1c8:	2000                	fld	fs0,0(s0)
  1ca:	012e                	slli	sp,sp,0xb
  1cc:	0e03193f 0b3b0b3a 	0xb3b0b3a0e03193f
  1d4:	0b39                	addi	s6,s6,14
  1d6:	0b201927          	0xb201927
  1da:	1301                	addi	t1,t1,-32
  1dc:	0000                	unimp
  1de:	0521                	addi	a0,a0,8
  1e0:	0300                	addi	s0,sp,384
  1e2:	3a08                	fld	fa0,48(a2)
  1e4:	390b3b0b          	0x390b3b0b
  1e8:	0013490b          	0x13490b
  1ec:	2200                	fld	fs0,0(a2)
  1ee:	0b0b000f          	0xb0b000f
  1f2:	1349                	addi	t1,t1,-14
  1f4:	0000                	unimp
  1f6:	00010b23          	sb	zero,22(sp) # 1300d8 <_stack_top+0x300d8>
  1fa:	2400                	fld	fs0,8(s0)
  1fc:	012e                	slli	sp,sp,0xb
  1fe:	1331                	addi	t1,t1,-20
  200:	0111                	addi	sp,sp,4
  202:	0612                	slli	a2,a2,0x4
  204:	1840                	addi	s0,sp,52
  206:	01194297          	auipc	t0,0x1194
  20a:	25000013          	li	zero,592
  20e:	0005                	c.nop	1
  210:	1331                	addi	t1,t1,-20
  212:	1802                	slli	a6,a6,0x20
  214:	0000                	unimp
  216:	2e26                	fld	ft8,72(sp)
  218:	3101                	jal	fffffe18 <_stack_top+0xffeffe18>
  21a:	12011113          	0x12011113
  21e:	4006                	0x4006
  220:	9718                	0x9718
  222:	1942                	slli	s2,s2,0x30
  224:	0000                	unimp
    ...

  Disassembly of section .debug_aranges:

  00000000 <.debug_aranges>:
    0:	001c                	0x1c
    2:	0000                	unimp
    4:	0002                	c.slli64	zero
    6:	0000                	unimp
    8:	0000                	unimp
    a:	0004                	0x4
    c:	0000                	unimp
    e:	0000                	unimp
    10:	1000                	addi	s0,sp,32
    12:	0000                	unimp
    14:	0010                	0x10
    ...
    1e:	0000                	unimp
    20:	0024                	addi	s1,sp,8
    22:	0000                	unimp
    24:	0002                	c.slli64	zero
    26:	0026                	c.slli	zero,0x9
    28:	0000                	unimp
    2a:	0004                	0x4
    2c:	0000                	unimp
    2e:	0000                	unimp
    30:	1010                	addi	a2,sp,32
    32:	0000                	unimp
    34:	01cc                	addi	a1,sp,196
    36:	0000                	unimp
    38:	11dc                	addi	a5,sp,228
    3a:	0000                	unimp
    3c:	0138                	addi	a4,sp,136
    ...

  Disassembly of section .debug_str:

  00000000 <.debug_str>:
    0:	72617473          	csrrci	s0,0x726,2
    4:	2e74                	fld	fa3,216(a2)
    6:	682f0053          	0x682f0053
    a:	2f656d6f          	jal	s10,56300 <main+0x55124>
    e:	6276                	flw	ft4,92(sp)
    10:	7375786f          	jal	a6,57f46 <main+0x56d6a>
    14:	7265                	lui	tp,0xffff9
    16:	5f61632f          	0x5f61632f
    1a:	7270                	flw	fa2,100(a2)
    1c:	63656a6f          	jal	s4,56652 <main+0x55476>
    20:	2f74                	fld	fa3,216(a4)
    22:	2d34                	fld	fa3,88(a0)
    24:	2f636f73          	csrrsi	t5,0x2f6,6
    28:	74666f73          	csrrsi	t5,0x746,12
    2c:	65726177          	0x65726177
    30:	4700                	lw	s0,8(a4)
    32:	554e                	lw	a0,240(sp)
    34:	4120                	lw	s0,64(a0)
    36:	2e322053          	0x2e322053
    3a:	312e3533          	0x312e3533
    3e:	6400                	flw	fs0,8(s0)
    40:	7365                	lui	t1,0xffff9
    42:	64003163          	0x64003163
    46:	7365                	lui	t1,0xffff9
    48:	75003263          	0x75003263
    4c:	736e                	flw	ft6,248(sp)
    4e:	6769                	lui	a4,0x1a
    50:	656e                	flw	fa0,216(sp)
    52:	2064                	fld	fs1,192(s0)
    54:	6e69                	lui	t3,0x1a
    56:	0074                	addi	a3,sp,12
    58:	5f637273          	csrrci	tp,0x5f6,6
    5c:	7562                	flw	fa0,56(sp)
    5e:	0066                	c.slli	zero,0x19
    60:	6175                	addi	sp,sp,368
    62:	7472                	flw	fs0,60(sp)
    64:	705f 7475 0063      	0x637475705f
    6a:	616d                	addi	sp,sp,240
    6c:	6e69                	lui	t3,0x1a
    6e:	6c00                	flw	fs0,24(s0)
    70:	6e65                	lui	t3,0x19
    72:	00687467          	0x687467
    76:	7364                	flw	fs1,100(a4)
    78:	5f74                	lw	a3,124(a4)
    7a:	7562                	flw	fa0,56(sp)
    7c:	0066                	c.slli	zero,0x19
    7e:	6d64                	flw	fs1,92(a0)
    80:	5f61                	li	t5,-8
    82:	745f6773          	csrrsi	a4,0x745,30
    86:	7365                	lui	t1,0xffff9
    88:	2e74                	fld	fa3,216(a2)
    8a:	656e0063          	beq	t3,s6,6ca <_start-0x936>
    8e:	7478                	flw	fa4,108(s0)
    90:	705f 7274 7500      	0x75007274705f
    96:	736e                	flw	ft6,248(sp)
    98:	6769                	lui	a4,0x1a
    9a:	656e                	flw	fa0,216(sp)
    9c:	2064                	fld	fs1,192(s0)
    9e:	72616863          	bltu	sp,t1,7ce <_start-0x832>
    a2:	6400                	flw	fs0,8(s0)
    a4:	615f7473          	csrrci	s0,0x615,30
    a8:	6464                	flw	fs1,76(s0)
    aa:	0072                	c.slli	zero,0x1c
    ac:	5f637273          	csrrci	tp,0x5f6,6
    b0:	6461                	lui	s0,0x18
    b2:	7264                	flw	fs1,100(a2)
    b4:	7500                	flw	fs0,40(a0)
    b6:	6e69                	lui	t3,0x1a
    b8:	3374                	fld	fa3,224(a4)
    ba:	5f32                	lw	t5,44(sp)
    bc:	0074                	addi	a3,sp,12
    be:	6d64                	flw	fs1,92(a0)
    c0:	5f61                	li	t5,-8
    c2:	6564                	flw	fs1,76(a0)
    c4:	745f6373          	csrrsi	t1,0x745,30
    c8:	7500                	flw	fs0,40(a0)
    ca:	7261                	lui	tp,0xffff8
    cc:	5f74                	lw	a3,124(a4)
    ce:	7570                	flw	fa2,108(a0)
    d0:	7374                	flw	fa3,100(a4)
    d2:	4700                	lw	s0,8(a4)
    d4:	554e                	lw	a0,240(sp)
    d6:	4320                	lw	s0,64(a4)
    d8:	3731                	jal	ffffffe4 <_stack_top+0xffefffe4>
    da:	3120                	fld	fs0,96(a0)
    dc:	2e30                	fld	fa2,88(a2)
    de:	2e32                	fld	ft8,264(sp)
    e0:	2030                	fld	fa2,64(s0)
    e2:	6d2d                	lui	s10,0xb
    e4:	7261                	lui	tp,0xffff8
    e6:	723d6863          	bltu	s10,gp,816 <_start-0x7ea>
    ea:	3376                	fld	ft6,376(sp)
    ec:	6932                	flw	fs2,12(sp)
    ee:	2d20                	fld	fs0,88(a0)
    f0:	616d                	addi	sp,sp,240
    f2:	6962                	flw	fs2,24(sp)
    f4:	693d                	lui	s2,0xf
    f6:	706c                	flw	fa1,100(s0)
    f8:	2d203233          	0x2d203233
    fc:	4f2d2067          	0x4f2d2067
  100:	0032                	c.slli	zero,0xc
  102:	6175                	addi	sp,sp,368
  104:	7472                	flw	fs0,60(sp)
  106:	705f 7475 685f      	0x685f7475705f
  10c:	7865                	lui	a6,0xffff9
    ...

  Disassembly of section .debug_loc:

  00000000 <.debug_loc>:
    0:	129c                	addi	a5,sp,352
    2:	0000                	unimp
    4:	12a8                	addi	a0,sp,360
    6:	0000                	unimp
    8:	0002                	c.slli64	zero
    a:	9f30                	0x9f30
    c:	12a8                	addi	a0,sp,360
    e:	0000                	unimp
    10:	1314                	addi	a3,sp,416
    12:	0000                	unimp
    14:	0001                	nop
    16:	0058                	addi	a4,sp,4
    18:	0000                	unimp
    1a:	0000                	unimp
    1c:	0000                	unimp
    1e:	dc00                	sw	s0,56(s0)
    20:	0011                	c.nop	4
    22:	f400                	fsw	fs0,40(s0)
    24:	0011                	c.nop	4
    26:	0200                	addi	s0,sp,256
    28:	3000                	fld	fs0,32(s0)
    2a:	f49f 0011 1800      	0x18000011f49f
    30:	0012                	c.slli	zero,0x4
    32:	0100                	addi	s0,sp,128
    34:	5f00                	lw	s0,56(a4)
    ...
    3e:	129c                	addi	a5,sp,352
    40:	0000                	unimp
    42:	12a8                	addi	a0,sp,360
    44:	0000                	unimp
    46:	0002                	c.slli64	zero
    48:	9f30                	0x9f30
    4a:	12a8                	addi	a0,sp,360
    4c:	0000                	unimp
    4e:	12c0                	addi	s0,sp,356
    50:	0000                	unimp
    52:	0001                	nop
    54:	c05d                	beqz	s0,fa <_start-0xf06>
    56:	0012                	c.slli	zero,0x4
    58:	cc00                	sw	s0,24(s0)
    5a:	0012                	c.slli	zero,0x4
    5c:	0300                	addi	s0,sp,384
    5e:	7d00                	flw	fs0,56(a0)
    60:	9f7f                	0x9f7f
    62:	12cc                	addi	a1,sp,356
    64:	0000                	unimp
    66:	12df 0000 0001      	0x1000012df
    6c:	fc5d                	bnez	s0,2a <_start-0xfd6>
    6e:	0012                	c.slli	zero,0x4
    70:	0700                	addi	s0,sp,896
    72:	01000013          	li	zero,16
    76:	5d00                	lw	s0,56(a0)
    ...
    80:	115c                	addi	a5,sp,164
    82:	0000                	unimp
    84:	118c                	addi	a1,sp,224
    86:	0000                	unimp
    88:	0001                	nop
    8a:	0000005b          	0x5b
    8e:	0000                	unimp
    90:	0000                	unimp
    92:	ec00                	fsw	fs0,24(s0)
    94:	0010                	0x10
    96:	fc00                	fsw	fs0,56(s0)
    98:	0010                	0x10
    9a:	0600                	addi	s0,sp,768
    9c:	0300                	addi	s0,sp,384
    9e:	1314                	addi	a3,sp,416
    a0:	0000                	unimp
    a2:	fc9f 0010 3c00      	0x3c000010fc9f
    a8:	0011                	c.nop	4
    aa:	0100                	addi	s0,sp,128
    ac:	5c00                	lw	s0,56(s0)
    ...
    b6:	1100                	addi	s0,sp,160
    b8:	0000                	unimp
    ba:	1130                	addi	a2,sp,168
    bc:	0000                	unimp
    be:	0001                	nop
    c0:	0000005b          	0x5b
    c4:	0000                	unimp
    c6:	0000                	unimp
    c8:	5800                	lw	s0,48(s0)
    ca:	0010                	0x10
    cc:	7000                	flw	fs0,32(s0)
    ce:	0010                	0x10
    d0:	0100                	addi	s0,sp,128
    d2:	5a00                	lw	s0,48(a2)
    d4:	1070                	addi	a2,sp,44
    d6:	0000                	unimp
    d8:	10b4                	addi	a3,sp,104
    da:	0000                	unimp
    dc:	0001                	nop
    de:	005a                	c.slli	zero,0x16
    e0:	0000                	unimp
    e2:	0000                	unimp
    e4:	0000                	unimp
    e6:	7000                	flw	fs0,32(s0)
    e8:	0010                	0x10
    ea:	a000                	fsd	fs0,0(s0)
    ec:	0010                	0x10
    ee:	0100                	addi	s0,sp,128
    f0:	5c00                	lw	s0,56(s0)
    ...

  Disassembly of section .debug_ranges:

  00000000 <.debug_ranges>:
    0:	1064                	addi	s1,sp,44
    2:	0000                	unimp
    4:	106c                	addi	a1,sp,44
    6:	0000                	unimp
    8:	1070                	addi	a2,sp,44
    a:	0000                	unimp
    c:	10a0                	addi	s0,sp,104
    ...
    16:	0000                	unimp
    18:	1068                	addi	a0,sp,44
    1a:	0000                	unimp
    1c:	106c                	addi	a1,sp,44
    1e:	0000                	unimp
    20:	1080                	addi	s0,sp,96
    22:	0000                	unimp
    24:	10a0                	addi	s0,sp,104
    ...
    2e:	0000                	unimp
    30:	10ec                	addi	a1,sp,108
    32:	0000                	unimp
    34:	10f0                	addi	a2,sp,108
    36:	0000                	unimp
    38:	10f4                	addi	a3,sp,108
    3a:	0000                	unimp
    3c:	113c                	addi	a5,sp,168
    ...
    46:	0000                	unimp
    48:	10f4                	addi	a3,sp,108
    4a:	0000                	unimp
    4c:	10fc                	addi	a5,sp,108
    4e:	0000                	unimp
    50:	1100                	addi	s0,sp,160
    52:	0000                	unimp
    54:	1130                	addi	a2,sp,168
    ...
    5e:	0000                	unimp
    60:	10f8                	addi	a4,sp,108
    62:	0000                	unimp
    64:	10fc                	addi	a5,sp,108
    66:	0000                	unimp
    68:	1110                	addi	a2,sp,160
    6a:	0000                	unimp
    6c:	1130                	addi	a2,sp,168
    ...
    76:	0000                	unimp
    78:	113c                	addi	a5,sp,168
    7a:	0000                	unimp
    7c:	1144                	addi	s1,sp,164
    7e:	0000                	unimp
    80:	115c                	addi	a5,sp,164
    82:	0000                	unimp
    84:	118c                	addi	a1,sp,224
    ...
    8e:	0000                	unimp
    90:	1140                	addi	s0,sp,164
    92:	0000                	unimp
    94:	1144                	addi	s1,sp,164
    96:	0000                	unimp
    98:	116c                	addi	a1,sp,172
    9a:	0000                	unimp
    9c:	118c                	addi	a1,sp,224
    ...
    a6:	0000                	unimp
    a8:	11dc                	addi	a5,sp,228
    aa:	0000                	unimp
    ac:	11dc                	addi	a5,sp,228
    ae:	0000                	unimp
    b0:	11e8                	addi	a0,sp,236
    b2:	0000                	unimp
    b4:	1214                	addi	a3,sp,288
    ...
    be:	0000                	unimp
    c0:	129c                	addi	a5,sp,352
    c2:	0000                	unimp
    c4:	129c                	addi	a5,sp,352
    c6:	0000                	unimp
    c8:	12a0                	addi	s0,sp,360
    ca:	0000                	unimp
    cc:	12d0                	addi	a2,sp,356
    ...
    d6:	0000                	unimp
    d8:	1010                	addi	a2,sp,32
    da:	0000                	unimp
    dc:	11dc                	addi	a5,sp,228
    de:	0000                	unimp
    e0:	11dc                	addi	a5,sp,228
    e2:	0000                	unimp
    e4:	1314                	addi	a3,sp,416
    ...

  Disassembly of section .debug_frame:

  00000000 <.debug_frame>:
    0:	000c                	0xc
    2:	0000                	unimp
    4:	ffff                	0xffff
    6:	ffff                	0xffff
    8:	7c010003          	lb	zero,1984(sp)
    c:	0d01                	addi	s10,s10,0
    e:	0002                	c.slli64	zero
    10:	0014                	0x14
    12:	0000                	unimp
    14:	0000                	unimp
    16:	0000                	unimp
    18:	1010                	addi	a2,sp,32
    1a:	0000                	unimp
    1c:	0048                	addi	a0,sp,4
    1e:	0000                	unimp
    20:	0e44                	addi	s1,sp,788
    22:	0210                	addi	a2,sp,256
    24:	0e40                	addi	s0,sp,788
    26:	0000                	unimp
    28:	0014                	0x14
    2a:	0000                	unimp
    2c:	0000                	unimp
    2e:	0000                	unimp
    30:	1058                	addi	a4,sp,36
    32:	0000                	unimp
    34:	005c                	addi	a5,sp,4
    36:	0000                	unimp
    38:	0e4c                	addi	a1,sp,788
    3a:	0210                	addi	a2,sp,256
    3c:	0e48                	addi	a0,sp,788
    3e:	0000                	unimp
    40:	0014                	0x14
    42:	0000                	unimp
    44:	0000                	unimp
    46:	0000                	unimp
    48:	10b4                	addi	a3,sp,104
    4a:	0000                	unimp
    4c:	0128                	addi	a0,sp,136
    4e:	0000                	unimp
    50:	0e60                	addi	s0,sp,796
    52:	0320                	addi	s0,sp,392
    54:	0104                	addi	s1,sp,128
    56:	000e                	c.slli	zero,0x3
    58:	0018                	0x18
    5a:	0000                	unimp
    5c:	0000                	unimp
    5e:	0000                	unimp
    60:	11dc                	addi	a5,sp,228
    62:	0000                	unimp
    64:	0138                	addi	a4,sp,136
    66:	0000                	unimp
    68:	0e44                	addi	s1,sp,788
    6a:	02b0                	addi	a2,sp,328
    6c:	8148                	0x8148
    6e:	8801                	andi	s0,s0,0
    70:	0002                	c.slli64	zero
    ...
