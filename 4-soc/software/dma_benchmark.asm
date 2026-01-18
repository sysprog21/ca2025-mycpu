
dma_benchmark.elf:     file format elf32-littleriscv


Disassembly of section .text:

00001000 <_start>:
    1000:	000ff117          	auipc	sp,0xff
    1004:	00010113          	mv	sp,sp
    1008:	280000ef          	jal	ra,1288 <main>

0000100c <hang>:
    100c:	0000006f          	j	100c <hang>

00001010 <simple_udiv>:
    1010:	00050813          	mv	a6,a0
    1014:	00000513          	li	a0,0
    1018:	04058463          	beqz	a1,1060 <simple_udiv+0x50>
    101c:	01f00713          	li	a4,31
    1020:	00000793          	li	a5,0
    1024:	00100313          	li	t1,1
    1028:	fff00893          	li	a7,-1
    102c:	00e856b3          	srl	a3,a6,a4
    1030:	0016f693          	andi	a3,a3,1
    1034:	00179793          	slli	a5,a5,0x1
    1038:	00f6e7b3          	or	a5,a3,a5
    103c:	00e316b3          	sll	a3,t1,a4
    1040:	fff70713          	addi	a4,a4,-1
    1044:	00b7e663          	bltu	a5,a1,1050 <simple_udiv+0x40>
    1048:	40b787b3          	sub	a5,a5,a1
    104c:	00d56533          	or	a0,a0,a3
    1050:	fd171ee3          	bne	a4,a7,102c <simple_udiv+0x1c>
    1054:	00060663          	beqz	a2,1060 <simple_udiv+0x50>
    1058:	00f62023          	sw	a5,0(a2)
    105c:	00008067          	ret
    1060:	00008067          	ret

00001064 <uart_putc>:
    1064:	ff010113          	addi	sp,sp,-16 # ffff0 <src_buf+0xfd890>
    1068:	10000737          	lui	a4,0x10000
    106c:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
    1070:	0027f793          	andi	a5,a5,2
    1074:	fe079ce3          	bnez	a5,106c <uart_putc+0x8>
    1078:	00a72023          	sw	a0,0(a4)
    107c:	00012623          	sw	zero,12(sp)
    1080:	00c12703          	lw	a4,12(sp)
    1084:	1f300793          	li	a5,499
    1088:	00e7ce63          	blt	a5,a4,10a4 <uart_putc+0x40>
    108c:	1f300713          	li	a4,499
    1090:	00c12783          	lw	a5,12(sp)
    1094:	00178793          	addi	a5,a5,1
    1098:	00f12623          	sw	a5,12(sp)
    109c:	00c12783          	lw	a5,12(sp)
    10a0:	fef758e3          	bge	a4,a5,1090 <uart_putc+0x2c>
    10a4:	01010113          	addi	sp,sp,16
    10a8:	00008067          	ret

000010ac <print_str>:
    10ac:	00054603          	lbu	a2,0(a0)
    10b0:	04060a63          	beqz	a2,1104 <print_str+0x58>
    10b4:	ff010113          	addi	sp,sp,-16
    10b8:	10000737          	lui	a4,0x10000
    10bc:	1f300693          	li	a3,499
    10c0:	00150513          	addi	a0,a0,1
    10c4:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
    10c8:	0027f793          	andi	a5,a5,2
    10cc:	fe079ce3          	bnez	a5,10c4 <print_str+0x18>
    10d0:	00c72023          	sw	a2,0(a4)
    10d4:	00012623          	sw	zero,12(sp)
    10d8:	00c12783          	lw	a5,12(sp)
    10dc:	00f6cc63          	blt	a3,a5,10f4 <print_str+0x48>
    10e0:	00c12783          	lw	a5,12(sp)
    10e4:	00178793          	addi	a5,a5,1
    10e8:	00f12623          	sw	a5,12(sp)
    10ec:	00c12783          	lw	a5,12(sp)
    10f0:	fef6d8e3          	bge	a3,a5,10e0 <print_str+0x34>
    10f4:	00054603          	lbu	a2,0(a0)
    10f8:	fc0614e3          	bnez	a2,10c0 <print_str+0x14>
    10fc:	01010113          	addi	sp,sp,16
    1100:	00008067          	ret
    1104:	00008067          	ret

00001108 <print_dec>:
    1108:	ff010113          	addi	sp,sp,-16
    110c:	0c050663          	beqz	a0,11d8 <print_dec+0xd0>
    1110:	00000e93          	li	t4,0
    1114:	00410f13          	addi	t5,sp,4
    1118:	fff00313          	li	t1,-1
    111c:	00900e13          	li	t3,9
    1120:	00100893          	li	a7,1
    1124:	01f55713          	srli	a4,a0,0x1f
    1128:	01f00793          	li	a5,31
    112c:	00000813          	li	a6,0
    1130:	fff78793          	addi	a5,a5,-1
    1134:	00f556b3          	srl	a3,a0,a5
    1138:	00171613          	slli	a2,a4,0x1
    113c:	0016f693          	andi	a3,a3,1
    1140:	00f895b3          	sll	a1,a7,a5
    1144:	02678663          	beq	a5,t1,1170 <print_dec+0x68>
    1148:	00c6e733          	or	a4,a3,a2
    114c:	feee72e3          	bgeu	t3,a4,1130 <print_dec+0x28>
    1150:	fff78793          	addi	a5,a5,-1
    1154:	ff670713          	addi	a4,a4,-10
    1158:	00f556b3          	srl	a3,a0,a5
    115c:	00b86833          	or	a6,a6,a1
    1160:	00171613          	slli	a2,a4,0x1
    1164:	0016f693          	andi	a3,a3,1
    1168:	00f895b3          	sll	a1,a7,a5
    116c:	fc679ee3          	bne	a5,t1,1148 <print_dec+0x40>
    1170:	001e8e93          	addi	t4,t4,1
    1174:	03070713          	addi	a4,a4,48
    1178:	0ff77713          	andi	a4,a4,255
    117c:	01df05b3          	add	a1,t5,t4
    1180:	fee58fa3          	sb	a4,-1(a1)
    1184:	00080663          	beqz	a6,1190 <print_dec+0x88>
    1188:	00080513          	mv	a0,a6
    118c:	f99ff06f          	j	1124 <print_dec+0x1c>
    1190:	100006b7          	lui	a3,0x10000
    1194:	1f300613          	li	a2,499
    1198:	0046a783          	lw	a5,4(a3) # 10000004 <_stack_top+0xff00004>
    119c:	0027f793          	andi	a5,a5,2
    11a0:	fe079ce3          	bnez	a5,1198 <print_dec+0x90>
    11a4:	00e6a023          	sw	a4,0(a3)
    11a8:	00012023          	sw	zero,0(sp)
    11ac:	00012783          	lw	a5,0(sp)
    11b0:	00f64c63          	blt	a2,a5,11c8 <print_dec+0xc0>
    11b4:	00012783          	lw	a5,0(sp)
    11b8:	00178793          	addi	a5,a5,1
    11bc:	00f12023          	sw	a5,0(sp)
    11c0:	00012783          	lw	a5,0(sp)
    11c4:	fef658e3          	bge	a2,a5,11b4 <print_dec+0xac>
    11c8:	fff58593          	addi	a1,a1,-1
    11cc:	04bf0663          	beq	t5,a1,1218 <print_dec+0x110>
    11d0:	fff5c703          	lbu	a4,-1(a1)
    11d4:	fc5ff06f          	j	1198 <print_dec+0x90>
    11d8:	10000737          	lui	a4,0x10000
    11dc:	00472783          	lw	a5,4(a4) # 10000004 <_stack_top+0xff00004>
    11e0:	0027f793          	andi	a5,a5,2
    11e4:	fe079ce3          	bnez	a5,11dc <print_dec+0xd4>
    11e8:	03000793          	li	a5,48
    11ec:	00f72023          	sw	a5,0(a4)
    11f0:	00012223          	sw	zero,4(sp)
    11f4:	00412683          	lw	a3,4(sp)
    11f8:	1f300793          	li	a5,499
    11fc:	1f300713          	li	a4,499
    1200:	00d7cc63          	blt	a5,a3,1218 <print_dec+0x110>
    1204:	00412783          	lw	a5,4(sp)
    1208:	00178793          	addi	a5,a5,1
    120c:	00f12223          	sw	a5,4(sp)
    1210:	00412783          	lw	a5,4(sp)
    1214:	fef758e3          	bge	a4,a5,1204 <print_dec+0xfc>
    1218:	01010113          	addi	sp,sp,16
    121c:	00008067          	ret

00001220 <read_cycles>:
    1220:	b0002573          	csrr	a0,mcycle
    1224:	00008067          	ret

00001228 <dma_transfer>:
    1228:	00b6a223          	sw	a1,4(a3)
    122c:	00a6a423          	sw	a0,8(a3)
    1230:	00c6a623          	sw	a2,12(a3)
    1234:	0006a023          	sw	zero,0(a3)
    1238:	0ff0000f          	fence
    123c:	100027b7          	lui	a5,0x10002
    1240:	00100713          	li	a4,1
    1244:	00d7a423          	sw	a3,8(a5) # 10002008 <_stack_top+0xff02008>
    1248:	00e7a023          	sw	a4,0(a5)
    124c:	10002737          	lui	a4,0x10002
    1250:	00472783          	lw	a5,4(a4) # 10002004 <_stack_top+0xff02004>
    1254:	0027f793          	andi	a5,a5,2
    1258:	fe078ce3          	beqz	a5,1250 <dma_transfer+0x28>
    125c:	00008067          	ret

00001260 <cpu_memcpy>:
    1260:	02060263          	beqz	a2,1284 <cpu_memcpy+0x24>
    1264:	00c50633          	add	a2,a0,a2
    1268:	0005c783          	lbu	a5,0(a1)
    126c:	00050713          	mv	a4,a0
    1270:	00150513          	addi	a0,a0,1
    1274:	0ff7f793          	andi	a5,a5,255
    1278:	00f70023          	sb	a5,0(a4)
    127c:	00158593          	addi	a1,a1,1
    1280:	fec514e3          	bne	a0,a2,1268 <cpu_memcpy+0x8>
    1284:	00008067          	ret

Disassembly of section .text.startup:

00001288 <main>:
    1288:	f6010113          	addi	sp,sp,-160
    128c:	02012623          	sw	zero,44(sp)
    1290:	02c12783          	lw	a5,44(sp)
    1294:	00002737          	lui	a4,0x2
    1298:	08112e23          	sw	ra,156(sp)
    129c:	08812c23          	sw	s0,152(sp)
    12a0:	08912a23          	sw	s1,148(sp)
    12a4:	09212823          	sw	s2,144(sp)
    12a8:	09312623          	sw	s3,140(sp)
    12ac:	09412423          	sw	s4,136(sp)
    12b0:	09512223          	sw	s5,132(sp)
    12b4:	09612023          	sw	s6,128(sp)
    12b8:	07712e23          	sw	s7,124(sp)
    12bc:	07812c23          	sw	s8,120(sp)
    12c0:	07912a23          	sw	s9,116(sp)
    12c4:	07a12823          	sw	s10,112(sp)
    12c8:	07b12623          	sw	s11,108(sp)
    12cc:	70f70713          	addi	a4,a4,1807 # 270f <dst_buf+0xfaf>
    12d0:	00f74c63          	blt	a4,a5,12e8 <main+0x60>
    12d4:	02c12783          	lw	a5,44(sp)
    12d8:	00178793          	addi	a5,a5,1
    12dc:	02f12623          	sw	a5,44(sp)
    12e0:	02c12783          	lw	a5,44(sp)
    12e4:	fef758e3          	bge	a4,a5,12d4 <main+0x4c>
    12e8:	00001537          	lui	a0,0x1
    12ec:	69c50513          	addi	a0,a0,1692 # 169c <main+0x414>
    12f0:	dbdff0ef          	jal	ra,10ac <print_str>
    12f4:	00001537          	lui	a0,0x1
    12f8:	6c050513          	addi	a0,a0,1728 # 16c0 <main+0x438>
    12fc:	db1ff0ef          	jal	ra,10ac <print_str>
    1300:	00001537          	lui	a0,0x1
    1304:	6e850513          	addi	a0,a0,1768 # 16e8 <main+0x460>
    1308:	da5ff0ef          	jal	ra,10ac <print_str>
    130c:	000027b7          	lui	a5,0x2
    1310:	76078713          	addi	a4,a5,1888 # 2760 <src_buf>
    1314:	00e12623          	sw	a4,12(sp)
    1318:	00000793          	li	a5,0
    131c:	000016b7          	lui	a3,0x1
    1320:	00f70023          	sb	a5,0(a4)
    1324:	00178793          	addi	a5,a5,1
    1328:	00170713          	addi	a4,a4,1
    132c:	fed79ae3          	bne	a5,a3,1320 <main+0x98>
    1330:	000017b7          	lui	a5,0x1
    1334:	72c78793          	addi	a5,a5,1836 # 172c <main+0x4a4>
    1338:	0007a803          	lw	a6,0(a5)
    133c:	0047a503          	lw	a0,4(a5)
    1340:	0087a583          	lw	a1,8(a5)
    1344:	00c7a603          	lw	a2,12(a5)
    1348:	0107a683          	lw	a3,16(a5)
    134c:	0147a703          	lw	a4,20(a5)
    1350:	0187a783          	lw	a5,24(a5)
    1354:	00001cb7          	lui	s9,0x1
    1358:	00001c37          	lui	s8,0x1
    135c:	04f12e23          	sw	a5,92(sp)
    1360:	684c8793          	addi	a5,s9,1668 # 1684 <main+0x3fc>
    1364:	00001bb7          	lui	s7,0x1
    1368:	00f12823          	sw	a5,16(sp)
    136c:	688c0793          	addi	a5,s8,1672 # 1688 <main+0x400>
    1370:	00f12a23          	sw	a5,20(sp)
    1374:	690b8793          	addi	a5,s7,1680 # 1690 <main+0x408>
    1378:	00f12c23          	sw	a5,24(sp)
    137c:	000027b7          	lui	a5,0x2
    1380:	00001a37          	lui	s4,0x1
    1384:	00001937          	lui	s2,0x1
    1388:	000014b7          	lui	s1,0x1
    138c:	76078793          	addi	a5,a5,1888 # 2760 <src_buf>
    1390:	05012223          	sw	a6,68(sp)
    1394:	04a12423          	sw	a0,72(sp)
    1398:	04b12623          	sw	a1,76(sp)
    139c:	04c12823          	sw	a2,80(sp)
    13a0:	04d12a23          	sw	a3,84(sp)
    13a4:	04e12c23          	sw	a4,88(sp)
    13a8:	760a0a13          	addi	s4,s4,1888 # 1760 <dst_buf>
    13ac:	75090913          	addi	s2,s2,1872 # 1750 <my_desc>
    13b0:	04410993          	addi	s3,sp,68
    13b4:	69848493          	addi	s1,s1,1688 # 1698 <main+0x410>
    13b8:	00f12e23          	sw	a5,28(sp)
    13bc:	10002437          	lui	s0,0x10002
    13c0:	00100b93          	li	s7,1
    13c4:	00900d13          	li	s10,9
    13c8:	1f300d93          	li	s11,499
    13cc:	b0002873          	csrr	a6,mcycle
    13d0:	0009a503          	lw	a0,0(s3)
    13d4:	02050c63          	beqz	a0,140c <main+0x184>
    13d8:	00c12783          	lw	a5,12(sp)
    13dc:	000a0313          	mv	t1,s4
    13e0:	00a78eb3          	add	t4,a5,a0
    13e4:	01c12783          	lw	a5,28(sp)
    13e8:	00f12623          	sw	a5,12(sp)
    13ec:	000027b7          	lui	a5,0x2
    13f0:	76078713          	addi	a4,a5,1888 # 2760 <src_buf>
    13f4:	00074583          	lbu	a1,0(a4)
    13f8:	00170713          	addi	a4,a4,1
    13fc:	00130313          	addi	t1,t1,1
    1400:	0ff5f593          	andi	a1,a1,255
    1404:	feb30fa3          	sb	a1,-1(t1)
    1408:	ffd716e3          	bne	a4,t4,13f4 <main+0x16c>
    140c:	b0002373          	csrr	t1,mcycle
    1410:	41030ab3          	sub	s5,t1,a6
    1414:	b0002e73          	csrr	t3,mcycle
    1418:	000027b7          	lui	a5,0x2
    141c:	76078793          	addi	a5,a5,1888 # 2760 <src_buf>
    1420:	00f92223          	sw	a5,4(s2)
    1424:	01492423          	sw	s4,8(s2)
    1428:	00a92623          	sw	a0,12(s2)
    142c:	00092023          	sw	zero,0(s2)
    1430:	0ff0000f          	fence
    1434:	01242423          	sw	s2,8(s0) # 10002008 <_stack_top+0xff02008>
    1438:	01742023          	sw	s7,0(s0)
    143c:	00442703          	lw	a4,4(s0)
    1440:	00277713          	andi	a4,a4,2
    1444:	fe070ce3          	beqz	a4,143c <main+0x1b4>
    1448:	b00027f3          	csrr	a5,mcycle
    144c:	00000c13          	li	s8,0
    1450:	41c78b33          	sub	s6,a5,t3
    1454:	00000c93          	li	s9,0
    1458:	1fc79063          	bne	a5,t3,1638 <main+0x3b0>
    145c:	cadff0ef          	jal	ra,1108 <print_dec>
    1460:	01012e03          	lw	t3,16(sp)
    1464:	00900e93          	li	t4,9
    1468:	10000537          	lui	a0,0x10000
    146c:	001e0e13          	addi	t3,t3,1
    1470:	00452703          	lw	a4,4(a0) # 10000004 <_stack_top+0xff00004>
    1474:	00277713          	andi	a4,a4,2
    1478:	fe071ce3          	bnez	a4,1470 <main+0x1e8>
    147c:	01d52023          	sw	t4,0(a0)
    1480:	02012823          	sw	zero,48(sp)
    1484:	03012703          	lw	a4,48(sp)
    1488:	00edcc63          	blt	s11,a4,14a0 <main+0x218>
    148c:	03012703          	lw	a4,48(sp)
    1490:	00170713          	addi	a4,a4,1
    1494:	02e12823          	sw	a4,48(sp)
    1498:	03012703          	lw	a4,48(sp)
    149c:	feedd8e3          	bge	s11,a4,148c <main+0x204>
    14a0:	000e4e83          	lbu	t4,0(t3)
    14a4:	fc0e94e3          	bnez	t4,146c <main+0x1e4>
    14a8:	000a8513          	mv	a0,s5
    14ac:	c5dff0ef          	jal	ra,1108 <print_dec>
    14b0:	01412303          	lw	t1,20(sp)
    14b4:	00900e13          	li	t3,9
    14b8:	10000537          	lui	a0,0x10000
    14bc:	00130313          	addi	t1,t1,1
    14c0:	00452703          	lw	a4,4(a0) # 10000004 <_stack_top+0xff00004>
    14c4:	00277713          	andi	a4,a4,2
    14c8:	fe071ce3          	bnez	a4,14c0 <main+0x238>
    14cc:	01c52023          	sw	t3,0(a0)
    14d0:	02012a23          	sw	zero,52(sp)
    14d4:	03412703          	lw	a4,52(sp)
    14d8:	00edcc63          	blt	s11,a4,14f0 <main+0x268>
    14dc:	03412703          	lw	a4,52(sp)
    14e0:	00170713          	addi	a4,a4,1
    14e4:	02e12a23          	sw	a4,52(sp)
    14e8:	03412703          	lw	a4,52(sp)
    14ec:	feedd8e3          	bge	s11,a4,14dc <main+0x254>
    14f0:	00034e03          	lbu	t3,0(t1)
    14f4:	fc0e14e3          	bnez	t3,14bc <main+0x234>
    14f8:	000b0513          	mv	a0,s6
    14fc:	c0dff0ef          	jal	ra,1108 <print_dec>
    1500:	01812303          	lw	t1,24(sp)
    1504:	00900713          	li	a4,9
    1508:	100008b7          	lui	a7,0x10000
    150c:	00130313          	addi	t1,t1,1
    1510:	0048a503          	lw	a0,4(a7) # 10000004 <_stack_top+0xff00004>
    1514:	00257513          	andi	a0,a0,2
    1518:	fe051ce3          	bnez	a0,1510 <main+0x288>
    151c:	00e8a023          	sw	a4,0(a7)
    1520:	02012c23          	sw	zero,56(sp)
    1524:	03812703          	lw	a4,56(sp)
    1528:	00edcc63          	blt	s11,a4,1540 <main+0x2b8>
    152c:	03812703          	lw	a4,56(sp)
    1530:	00170713          	addi	a4,a4,1
    1534:	02e12c23          	sw	a4,56(sp)
    1538:	03812703          	lw	a4,56(sp)
    153c:	feedd8e3          	bge	s11,a4,152c <main+0x2a4>
    1540:	00034703          	lbu	a4,0(t1)
    1544:	fc0714e3          	bnez	a4,150c <main+0x284>
    1548:	01f00713          	li	a4,31
    154c:	fff00e93          	li	t4,-1
    1550:	fff70713          	addi	a4,a4,-1
    1554:	00ecd8b3          	srl	a7,s9,a4
    1558:	001c1313          	slli	t1,s8,0x1
    155c:	0018f893          	andi	a7,a7,1
    1560:	00eb9e33          	sll	t3,s7,a4
    1564:	03d70663          	beq	a4,t4,1590 <main+0x308>
    1568:	0068ec33          	or	s8,a7,t1
    156c:	ff8d72e3          	bgeu	s10,s8,1550 <main+0x2c8>
    1570:	fff70713          	addi	a4,a4,-1
    1574:	ff6c0c13          	addi	s8,s8,-10
    1578:	00ecd8b3          	srl	a7,s9,a4
    157c:	01c56533          	or	a0,a0,t3
    1580:	001c1313          	slli	t1,s8,0x1
    1584:	0018f893          	andi	a7,a7,1
    1588:	00eb9e33          	sll	t3,s7,a4
    158c:	fdd71ee3          	bne	a4,t4,1568 <main+0x2e0>
    1590:	b79ff0ef          	jal	ra,1108 <print_dec>
    1594:	10000537          	lui	a0,0x10000
    1598:	00452703          	lw	a4,4(a0) # 10000004 <_stack_top+0xff00004>
    159c:	00277713          	andi	a4,a4,2
    15a0:	fe071ce3          	bnez	a4,1598 <main+0x310>
    15a4:	02e00713          	li	a4,46
    15a8:	00e52023          	sw	a4,0(a0)
    15ac:	02012e23          	sw	zero,60(sp)
    15b0:	03c12703          	lw	a4,60(sp)
    15b4:	00edcc63          	blt	s11,a4,15cc <main+0x344>
    15b8:	03c12703          	lw	a4,60(sp)
    15bc:	00170713          	addi	a4,a4,1
    15c0:	02e12e23          	sw	a4,60(sp)
    15c4:	03c12703          	lw	a4,60(sp)
    15c8:	feedd8e3          	bge	s11,a4,15b8 <main+0x330>
    15cc:	000c0513          	mv	a0,s8
    15d0:	b39ff0ef          	jal	ra,1108 <print_dec>
    15d4:	07800813          	li	a6,120
    15d8:	00048513          	mv	a0,s1
    15dc:	100005b7          	lui	a1,0x10000
    15e0:	00150513          	addi	a0,a0,1
    15e4:	0045a703          	lw	a4,4(a1) # 10000004 <_stack_top+0xff00004>
    15e8:	00277713          	andi	a4,a4,2
    15ec:	fe071ce3          	bnez	a4,15e4 <main+0x35c>
    15f0:	0105a023          	sw	a6,0(a1)
    15f4:	04012023          	sw	zero,64(sp)
    15f8:	04012703          	lw	a4,64(sp)
    15fc:	00edcc63          	blt	s11,a4,1614 <main+0x38c>
    1600:	04012703          	lw	a4,64(sp)
    1604:	00170713          	addi	a4,a4,1
    1608:	04e12023          	sw	a4,64(sp)
    160c:	04012703          	lw	a4,64(sp)
    1610:	feedd8e3          	bge	s11,a4,1600 <main+0x378>
    1614:	00054803          	lbu	a6,0(a0)
    1618:	fc0814e3          	bnez	a6,15e0 <main+0x358>
    161c:	00498993          	addi	s3,s3,4
    1620:	06010793          	addi	a5,sp,96
    1624:	daf994e3          	bne	s3,a5,13cc <main+0x144>
    1628:	00001537          	lui	a0,0x1
    162c:	71050513          	addi	a0,a0,1808 # 1710 <main+0x488>
    1630:	a7dff0ef          	jal	ra,10ac <print_str>
    1634:	0000006f          	j	1634 <main+0x3ac>
    1638:	002a9e93          	slli	t4,s5,0x2
    163c:	015e8eb3          	add	t4,t4,s5
    1640:	001e9e93          	slli	t4,t4,0x1
    1644:	01f00593          	li	a1,31
    1648:	00000713          	li	a4,0
    164c:	40fe0fb3          	sub	t6,t3,a5
    1650:	fff00f13          	li	t5,-1
    1654:	00bede33          	srl	t3,t4,a1
    1658:	001e7e13          	andi	t3,t3,1
    165c:	00171713          	slli	a4,a4,0x1
    1660:	00ee6733          	or	a4,t3,a4
    1664:	00bb9e33          	sll	t3,s7,a1
    1668:	fff58593          	addi	a1,a1,-1
    166c:	01676663          	bltu	a4,s6,1678 <main+0x3f0>
    1670:	01f70733          	add	a4,a4,t6
    1674:	01ccecb3          	or	s9,s9,t3
    1678:	fde59ee3          	bne	a1,t5,1654 <main+0x3cc>
    167c:	01fcdc13          	srli	s8,s9,0x1f
    1680:	dddff06f          	j	145c <main+0x1d4>

Disassembly of section .rodata.str1.4:

00001684 <.rodata.str1.4>:
    1684:	7c09                	lui	s8,0xfffe2
    1686:	0020                	addi	s0,sp,8
    1688:	2009                	jal	168a <main+0x402>
    168a:	2020                	fld	fs0,64(s0)
    168c:	207c                	fld	fa5,192(s0)
    168e:	0000                	unimp
    1690:	2009                	jal	1692 <main+0x40a>
    1692:	7c20                	flw	fs0,120(s0)
    1694:	0020                	addi	s0,sp,8
    1696:	0000                	unimp
    1698:	0a78                	addi	a4,sp,284
    169a:	0000                	unimp
    169c:	3d0a                	fld	fs10,160(sp)
    169e:	3d3d                	jal	14dc <main+0x254>
    16a0:	4420                	lw	s0,72(s0)
    16a2:	414d                	li	sp,19
    16a4:	5020                	lw	s0,96(s0)
    16a6:	7265                	lui	tp,0xffff9
    16a8:	6f66                	flw	ft10,88(sp)
    16aa:	6d72                	flw	fs10,28(sp)
    16ac:	6e61                	lui	t3,0x18
    16ae:	42206563          	bltu	zero,sp,1ad8 <dst_buf+0x378>
    16b2:	6e65                	lui	t3,0x19
    16b4:	616d6863          	bltu	s10,s6,1cc4 <dst_buf+0x564>
    16b8:	6b72                	flw	fs6,28(sp)
    16ba:	3d20                	fld	fs0,120(a0)
    16bc:	3d3d                	jal	14fa <main+0x272>
    16be:	000a                	c.slli	zero,0x2
    16c0:	657a6953          	0x657a6953
    16c4:	4228                	lw	a0,64(a2)
    16c6:	2029                	jal	16d0 <main+0x448>
    16c8:	207c                	fld	fa5,192(s0)
    16ca:	28555043          	fmadd.s	ft0,fa0,ft5,ft5,unknown
    16ce:	29637963          	bgeu	t1,s6,1960 <dst_buf+0x200>
    16d2:	7c20                	flw	fs0,120(s0)
    16d4:	4420                	lw	s0,72(s0)
    16d6:	414d                	li	sp,19
    16d8:	6328                	flw	fa0,64(a4)
    16da:	6379                	lui	t1,0x1e
    16dc:	2029                	jal	16e6 <main+0x45e>
    16de:	207c                	fld	fa5,192(s0)
    16e0:	6152                	flw	ft2,20(sp)
    16e2:	6974                	flw	fa3,84(a0)
    16e4:	00000a6f          	jal	s4,16e4 <main+0x45c>
    16e8:	2d2d                	jal	1d22 <dst_buf+0x5c2>
    16ea:	2d2d                	jal	1d24 <dst_buf+0x5c4>
    16ec:	2d2d                	jal	1d26 <dst_buf+0x5c6>
    16ee:	2d2d                	jal	1d28 <dst_buf+0x5c8>
    16f0:	2d2d                	jal	1d2a <dst_buf+0x5ca>
    16f2:	2d2d                	jal	1d2c <dst_buf+0x5cc>
    16f4:	2d2d                	jal	1d2e <dst_buf+0x5ce>
    16f6:	2d2d                	jal	1d30 <dst_buf+0x5d0>
    16f8:	2d2d                	jal	1d32 <dst_buf+0x5d2>
    16fa:	2d2d                	jal	1d34 <dst_buf+0x5d4>
    16fc:	2d2d                	jal	1d36 <dst_buf+0x5d6>
    16fe:	2d2d                	jal	1d38 <dst_buf+0x5d8>
    1700:	2d2d                	jal	1d3a <dst_buf+0x5da>
    1702:	2d2d                	jal	1d3c <dst_buf+0x5dc>
    1704:	2d2d                	jal	1d3e <dst_buf+0x5de>
    1706:	2d2d                	jal	1d40 <dst_buf+0x5e0>
    1708:	2d2d                	jal	1d42 <dst_buf+0x5e2>
    170a:	2d2d                	jal	1d44 <dst_buf+0x5e4>
    170c:	0a2d                	addi	s4,s4,11
    170e:	0000                	unimp
    1710:	3d3d                	jal	154e <main+0x2c6>
    1712:	203d                	jal	1740 <main+0x4b8>
    1714:	6542                	flw	fa0,16(sp)
    1716:	636e                	flw	ft6,216(sp)
    1718:	6d68                	flw	fa0,92(a0)
    171a:	7261                	lui	tp,0xffff8
    171c:	6f43206b          	0x6f43206b
    1720:	706d                	c.lui	zero,0xffffb
    1722:	656c                	flw	fa1,76(a0)
    1724:	6574                	flw	fa3,76(a0)
    1726:	3d20                	fld	fs0,120(a0)
    1728:	3d3d                	jal	1566 <main+0x2de>
    172a:	000a                	c.slli	zero,0x2

Disassembly of section .rodata:

0000172c <.rodata>:
    172c:	0040                	addi	s0,sp,4
    172e:	0000                	unimp
    1730:	0080                	addi	s0,sp,64
    1732:	0000                	unimp
    1734:	0100                	addi	s0,sp,128
    1736:	0000                	unimp
    1738:	0200                	addi	s0,sp,256
    173a:	0000                	unimp
    173c:	0400                	addi	s0,sp,512
    173e:	0000                	unimp
    1740:	0800                	addi	s0,sp,16
    1742:	0000                	unimp
    1744:	1000                	addi	s0,sp,32
	...

Disassembly of section .bss:

00001750 <my_desc>:
	...

00001760 <dst_buf>:
	...

00002760 <src_buf>:
	...

Disassembly of section .riscv.attributes:

00000000 <.riscv.attributes>:
   0:	1f41                	addi	t5,t5,-16
   2:	0000                	unimp
   4:	7200                	flw	fs0,32(a2)
   6:	7369                	lui	t1,0xffffa
   8:	01007663          	bgeu	zero,a6,14 <_start-0xfec>
   c:	0015                	c.nop	5
   e:	0000                	unimp
  10:	1004                	addi	s1,sp,32
  12:	7205                	lui	tp,0xfffe1
  14:	3376                	fld	ft6,376(sp)
  16:	6932                	flw	fs2,12(sp)
  18:	7032                	flw	ft0,44(sp)
  1a:	0030                	addi	a2,sp,8
  1c:	0108                	addi	a0,sp,128
  1e:	0b0a                	slli	s6,s6,0x2

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
       0:	0040                	addi	s0,sp,4
       2:	0000                	unimp
       4:	001e0003          	lb	zero,1(t3) # 19001 <src_buf+0x168a1>
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
      2e:	0300                	addi	s0,sp,384
      30:	010a                	slli	sp,sp,0x2
      32:	08090403          	lb	s0,128(s2)
      36:	0100                	addi	s0,sp,128
      38:	04090603          	lb	a2,64(s2)
      3c:	0100                	addi	s0,sp,128
      3e:	0409                	addi	s0,s0,2
      40:	0000                	unimp
      42:	0101                	addi	sp,sp,0
      44:	12cc                	addi	a1,sp,356
      46:	0000                	unimp
      48:	00260003          	lb	zero,2(a2)
      4c:	0000                	unimp
      4e:	0101                	addi	sp,sp,0
      50:	000d0efb          	0xd0efb
      54:	0101                	addi	sp,sp,0
      56:	0101                	addi	sp,sp,0
      58:	0000                	unimp
      5a:	0100                	addi	s0,sp,128
      5c:	0000                	unimp
      5e:	0001                	nop
      60:	6d64                	flw	fs1,92(a0)
      62:	5f61                	li	t5,-8
      64:	6562                	flw	fa0,24(sp)
      66:	636e                	flw	ft6,216(sp)
      68:	6d68                	flw	fa0,92(a0)
      6a:	7261                	lui	tp,0xffff8
      6c:	00632e6b          	0x632e6b
      70:	0000                	unimp
      72:	0000                	unimp
      74:	0105                	addi	sp,sp,1
      76:	0500                	addi	s0,sp,640
      78:	1002                	c.slli	zero,0x20
      7a:	0010                	0x10
      7c:	0300                	addi	s0,sp,384
      7e:	0121                	addi	sp,sp,8
      80:	0505                	addi	a0,a0,1
      82:	00090103          	lb	sp,0(s2)
      86:	0100                	addi	s0,sp,128
      88:	0105                	addi	sp,sp,1
      8a:	0306                	slli	t1,t1,0x1
      8c:	097f                	0x97f
      8e:	0000                	unimp
      90:	0501                	addi	a0,a0,0
      92:	0310                	addi	a2,sp,384
      94:	0902                	c.slli64	s2
      96:	0004                	0x4
      98:	0501                	addi	a0,a0,0
      9a:	0308                	addi	a0,sp,384
      9c:	097f                	0x97f
      9e:	0004                	0x4
      a0:	0501                	addi	a0,a0,0
      a2:	030e                	slli	t1,t1,0x3
      a4:	0906                	slli	s2,s2,0x1
      a6:	0004                	0x4
      a8:	0301                	addi	t1,t1,0
      aa:	097e                	slli	s2,s2,0x1f
      ac:	0004                	0x4
      ae:	0501                	addi	a0,a0,0
      b0:	031c                	addi	a5,sp,384
      b2:	0908                	addi	a0,sp,144
      b4:	0004                	0x4
      b6:	0501                	addi	a0,a0,0
      b8:	0305                	addi	t1,t1,1
      ba:	097a                	slli	s2,s2,0x1e
      bc:	0004                	0x4
      be:	0501                	addi	a0,a0,0
      c0:	0609                	addi	a2,a2,2
      c2:	04090103          	lb	sp,64(s2)
      c6:	0100                	addi	s0,sp,128
      c8:	0d05                	addi	s10,s10,1
      ca:	00090403          	lb	s0,0(s2)
      ce:	0100                	addi	s0,sp,128
      d0:	1a05                	addi	s4,s4,-31
      d2:	0306                	slli	t1,t1,0x1
      d4:	097d                	addi	s2,s2,31
      d6:	0000                	unimp
      d8:	0501                	addi	a0,a0,0
      da:	0320                	addi	s0,sp,392
      dc:	0900                	addi	s0,sp,144
      de:	0004                	0x4
      e0:	0501                	addi	a0,a0,0
      e2:	030d                	addi	t1,t1,3
      e4:	097f                	0x97f
      e6:	0004                	0x4
      e8:	0501                	addi	a0,a0,0
      ea:	0609                	addi	a2,a2,2
      ec:	04090103          	lb	sp,64(s2)
      f0:	0100                	addi	s0,sp,128
      f2:	0d05                	addi	s10,s10,1
      f4:	0306                	slli	t1,t1,0x1
      f6:	0900                	addi	s0,sp,144
      f8:	0000                	unimp
      fa:	0501                	addi	a0,a0,0
      fc:	0609                	addi	a2,a2,2
      fe:	04090203          	lb	tp,64(s2)
     102:	0100                	addi	s0,sp,128
     104:	1c05                	addi	s8,s8,-31
     106:	0306                	slli	t1,t1,0x1
     108:	0902                	c.slli64	s2
     10a:	0000                	unimp
     10c:	0501                	addi	a0,a0,0
     10e:	031f 097a 0004      	0x4097a031f
     114:	0501                	addi	a0,a0,0
     116:	030c                	addi	a1,sp,384
     118:	0904                	addi	s1,sp,144
     11a:	0004                	0x4
     11c:	0501                	addi	a0,a0,0
     11e:	0311                	addi	t1,t1,4
     120:	0901                	addi	s2,s2,0
     122:	0004                	0x4
     124:	0501                	addi	a0,a0,0
     126:	060d                	addi	a2,a2,3
     128:	04090103          	lb	sp,64(s2)
     12c:	0100                	addi	s0,sp,128
     12e:	1605                	addi	a2,a2,-31
     130:	0306                	slli	t1,t1,0x1
     132:	0900                	addi	s0,sp,144
     134:	0000                	unimp
     136:	0501                	addi	a0,a0,0
     138:	061e                	slli	a2,a2,0x7
     13a:	04097a03          	0x4097a03
     13e:	0100                	addi	s0,sp,128
     140:	1605                	addi	a2,a2,-31
     142:	00090003          	lb	zero,0(s2)
     146:	0100                	addi	s0,sp,128
     148:	0505                	addi	a0,a0,1
     14a:	0306                	slli	t1,t1,0x1
     14c:	0900                	addi	s0,sp,144
     14e:	0000                	unimp
     150:	0601                	addi	a2,a2,0
     152:	04090a03          	lb	s4,64(s2)
     156:	0100                	addi	s0,sp,128
     158:	0805                	addi	a6,a6,1
     15a:	0306                	slli	t1,t1,0x1
     15c:	0900                	addi	s0,sp,144
     15e:	0000                	unimp
     160:	0501                	addi	a0,a0,0
     162:	0609                	addi	a2,a2,2
     164:	04090103          	lb	sp,64(s2)
     168:	0100                	addi	s0,sp,128
     16a:	1405                	addi	s0,s0,-31
     16c:	0306                	slli	t1,t1,0x1
     16e:	0900                	addi	s0,sp,144
     170:	0000                	unimp
     172:	0501                	addi	a0,a0,0
     174:	0301                	addi	t1,t1,0
     176:	0902                	c.slli64	s2
     178:	0008                	0x8
     17a:	0601                	addi	a2,a2,0
     17c:	04090803          	lb	a6,64(s2)
     180:	0100                	addi	s0,sp,128
     182:	0505                	addi	a0,a0,1
     184:	00090203          	lb	tp,0(s2)
     188:	0100                	addi	s0,sp,128
     18a:	0105                	addi	sp,sp,1
     18c:	0306                	slli	t1,t1,0x1
     18e:	097e                	slli	s2,s2,0x1f
     190:	0000                	unimp
     192:	0501                	addi	a0,a0,0
     194:	030d                	addi	t1,t1,3
     196:	0902                	c.slli64	s2
     198:	0004                	0x4
     19a:	0501                	addi	a0,a0,0
     19c:	0009                	c.nop	2
     19e:	0402                	c.slli64	s0
     1a0:	0601                	addi	a2,a2,0
     1a2:	04090103          	lb	sp,64(s2)
     1a6:	0100                	addi	s0,sp,128
     1a8:	0b05                	addi	s6,s6,1
     1aa:	0200                	addi	s0,sp,256
     1ac:	0104                	addi	s1,sp,128
     1ae:	00097f03          	0x97f03
     1b2:	0100                	addi	s0,sp,128
     1b4:	0d05                	addi	s10,s10,1
     1b6:	0200                	addi	s0,sp,256
     1b8:	0104                	addi	s1,sp,128
     1ba:	0306                	slli	t1,t1,0x1
     1bc:	0900                	addi	s0,sp,144
     1be:	0000                	unimp
     1c0:	0501                	addi	a0,a0,0
     1c2:	0402001b          	0x402001b
     1c6:	0301                	addi	t1,t1,0
     1c8:	0900                	addi	s0,sp,144
     1ca:	0004                	0x4
     1cc:	0501                	addi	a0,a0,0
     1ce:	0402000b          	0x402000b
     1d2:	0301                	addi	t1,t1,0
     1d4:	0900                	addi	s0,sp,144
     1d6:	0004                	0x4
     1d8:	0501                	addi	a0,a0,0
     1da:	0605                	addi	a2,a2,1
     1dc:	04090403          	lb	s0,64(s2)
     1e0:	0100                	addi	s0,sp,128
     1e2:	0e05                	addi	t3,t3,1
     1e4:	0306                	slli	t1,t1,0x1
     1e6:	0900                	addi	s0,sp,144
     1e8:	0000                	unimp
     1ea:	0501                	addi	a0,a0,0
     1ec:	0605                	addi	a2,a2,1
     1ee:	04090303          	lb	t1,64(s2)
     1f2:	0100                	addi	s0,sp,128
     1f4:	0a05                	addi	s4,s4,1
     1f6:	00090003          	lb	zero,0(s2)
     1fa:	0100                	addi	s0,sp,128
     1fc:	1705                	addi	a4,a4,-31
     1fe:	0306                	slli	t1,t1,0x1
     200:	0900                	addi	s0,sp,144
     202:	0000                	unimp
     204:	0501                	addi	a0,a0,0
     206:	061e                	slli	a2,a2,0x7
     208:	04090003          	lb	zero,64(s2)
     20c:	0100                	addi	s0,sp,128
     20e:	2005                	jal	22e <_start-0xdd2>
     210:	0306                	slli	t1,t1,0x1
     212:	0900                	addi	s0,sp,144
     214:	0000                	unimp
     216:	0501                	addi	a0,a0,0
     218:	0305                	addi	t1,t1,1
     21a:	0900                	addi	s0,sp,144
     21c:	0004                	0x4
     21e:	0501                	addi	a0,a0,0
     220:	0009                	c.nop	2
     222:	0402                	c.slli64	s0
     224:	01030603          	lb	a2,16(t1) # ffffa010 <_stack_top+0xffefa010>
     228:	0c09                	addi	s8,s8,2
     22a:	0100                	addi	s0,sp,128
     22c:	2705                	jal	94c <_start-0x6b4>
     22e:	0200                	addi	s0,sp,256
     230:	0304                	addi	s1,sp,384
     232:	00097f03          	0x97f03
     236:	0100                	addi	s0,sp,128
     238:	2805                	jal	268 <_start-0xd98>
     23a:	0200                	addi	s0,sp,256
     23c:	0304                	addi	s1,sp,384
     23e:	0306                	slli	t1,t1,0x1
     240:	0900                	addi	s0,sp,144
     242:	0000                	unimp
     244:	0501                	addi	a0,a0,0
     246:	001e                	c.slli	zero,0x7
     248:	0402                	c.slli64	s0
     24a:	00030603          	lb	a2,0(t1)
     24e:	0c09                	addi	s8,s8,2
     250:	0100                	addi	s0,sp,128
     252:	2005                	jal	272 <_start-0xd8e>
     254:	0200                	addi	s0,sp,256
     256:	0304                	addi	s1,sp,384
     258:	0306                	slli	t1,t1,0x1
     25a:	0900                	addi	s0,sp,144
     25c:	0000                	unimp
     25e:	0501                	addi	a0,a0,0
     260:	0005                	c.nop	1
     262:	0402                	c.slli64	s0
     264:	09000303          	lb	t1,144(zero) # 90 <_start-0xf70>
     268:	0004                	0x4
     26a:	0501                	addi	a0,a0,0
     26c:	0301                	addi	t1,t1,0
     26e:	0902                	c.slli64	s2
     270:	0004                	0x4
     272:	0601                	addi	a2,a2,0
     274:	08090403          	lb	s0,128(s2)
     278:	0100                	addi	s0,sp,128
     27a:	0505                	addi	a0,a0,1
     27c:	00090103          	lb	sp,0(s2)
     280:	0100                	addi	s0,sp,128
     282:	0b05                	addi	s6,s6,1
     284:	00090003          	lb	zero,0(s2)
     288:	0100                	addi	s0,sp,128
     28a:	0c05                	addi	s8,s8,1
     28c:	0306                	slli	t1,t1,0x1
     28e:	0900                	addi	s0,sp,144
     290:	0000                	unimp
     292:	0501                	addi	a0,a0,0
     294:	0900030b          	0x900030b
     298:	0004                	0x4
     29a:	0501                	addi	a0,a0,0
     29c:	0301                	addi	t1,t1,0
     29e:	097f                	0x97f
     2a0:	0004                	0x4
     2a2:	0501                	addi	a0,a0,0
     2a4:	030d                	addi	t1,t1,3
     2a6:	00040973          	0x40973
     2aa:	0501                	addi	a0,a0,0
     2ac:	0305                	addi	t1,t1,1
     2ae:	00040907          	0x40907
     2b2:	0501                	addi	a0,a0,0
     2b4:	0609                	addi	a2,a2,2
     2b6:	04090803          	lb	a6,64(s2)
     2ba:	0100                	addi	s0,sp,128
     2bc:	1505                	addi	a0,a0,-31
     2be:	0306                	slli	t1,t1,0x1
     2c0:	0900                	addi	s0,sp,144
     2c2:	0000                	unimp
     2c4:	0501                	addi	a0,a0,0
     2c6:	0609                	addi	a2,a2,2
     2c8:	04097203          	0x4097203
     2cc:	0100                	addi	s0,sp,128
     2ce:	0b05                	addi	s6,s6,1
     2d0:	00097f03          	0x97f03
     2d4:	0100                	addi	s0,sp,128
     2d6:	0d05                	addi	s10,s10,1
     2d8:	0306                	slli	t1,t1,0x1
     2da:	0900                	addi	s0,sp,144
     2dc:	0000                	unimp
     2de:	0501                	addi	a0,a0,0
     2e0:	0900031b          	0x900031b
     2e4:	0004                	0x4
     2e6:	0501                	addi	a0,a0,0
     2e8:	0900030b          	0x900030b
     2ec:	0004                	0x4
     2ee:	0501                	addi	a0,a0,0
     2f0:	0605                	addi	a2,a2,1
     2f2:	04090403          	lb	s0,64(s2)
     2f6:	0100                	addi	s0,sp,128
     2f8:	0e05                	addi	t3,t3,1
     2fa:	0306                	slli	t1,t1,0x1
     2fc:	0900                	addi	s0,sp,144
     2fe:	0000                	unimp
     300:	0501                	addi	a0,a0,0
     302:	0605                	addi	a2,a2,1
     304:	04090303          	lb	t1,64(s2)
     308:	0100                	addi	s0,sp,128
     30a:	0a05                	addi	s4,s4,1
     30c:	00090003          	lb	zero,0(s2)
     310:	0100                	addi	s0,sp,128
     312:	1705                	addi	a4,a4,-31
     314:	0306                	slli	t1,t1,0x1
     316:	0900                	addi	s0,sp,144
     318:	0000                	unimp
     31a:	0501                	addi	a0,a0,0
     31c:	061e                	slli	a2,a2,0x7
     31e:	04090003          	lb	zero,64(s2)
     322:	0100                	addi	s0,sp,128
     324:	2005                	jal	344 <_start-0xcbc>
     326:	0306                	slli	t1,t1,0x1
     328:	0900                	addi	s0,sp,144
     32a:	0000                	unimp
     32c:	0501                	addi	a0,a0,0
     32e:	0305                	addi	t1,t1,1
     330:	0900                	addi	s0,sp,144
     332:	0004                	0x4
     334:	0501                	addi	a0,a0,0
     336:	0609                	addi	a2,a2,2
     338:	04090103          	lb	sp,64(s2)
     33c:	0100                	addi	s0,sp,128
     33e:	2705                	jal	a5e <_start-0x5a2>
     340:	00097f03          	0x97f03
     344:	0100                	addi	s0,sp,128
     346:	2805                	jal	376 <_start-0xc8a>
     348:	0306                	slli	t1,t1,0x1
     34a:	0900                	addi	s0,sp,144
     34c:	0000                	unimp
     34e:	0501                	addi	a0,a0,0
     350:	061e                	slli	a2,a2,0x7
     352:	0c090003          	lb	zero,192(s2)
     356:	0100                	addi	s0,sp,128
     358:	2005                	jal	378 <_start-0xc88>
     35a:	0306                	slli	t1,t1,0x1
     35c:	0900                	addi	s0,sp,144
     35e:	0000                	unimp
     360:	0501                	addi	a0,a0,0
     362:	0305                	addi	t1,t1,1
     364:	0900                	addi	s0,sp,144
     366:	0004                	0x4
     368:	0501                	addi	a0,a0,0
     36a:	0703060b          	0x703060b
     36e:	0409                	addi	s0,s0,2
     370:	0100                	addi	s0,sp,128
     372:	0c05                	addi	s8,s8,1
     374:	0306                	slli	t1,t1,0x1
     376:	0900                	addi	s0,sp,144
     378:	0000                	unimp
     37a:	0501                	addi	a0,a0,0
     37c:	0900030b          	0x900030b
     380:	0004                	0x4
     382:	0501                	addi	a0,a0,0
     384:	0301                	addi	t1,t1,0
     386:	00040903          	lb	s2,0(s0)
     38a:	0601                	addi	a2,a2,0
     38c:	0c090403          	lb	s0,192(s2)
     390:	0100                	addi	s0,sp,128
     392:	0505                	addi	a0,a0,1
     394:	00090103          	lb	sp,0(s2)
     398:	0100                	addi	s0,sp,128
     39a:	00090103          	lb	sp,0(s2)
     39e:	0100                	addi	s0,sp,128
     3a0:	00090203          	lb	tp,0(s2)
     3a4:	0100                	addi	s0,sp,128
     3a6:	0105                	addi	sp,sp,1
     3a8:	0306                	slli	t1,t1,0x1
     3aa:	097c                	addi	a5,sp,156
     3ac:	0000                	unimp
     3ae:	0501                	addi	a0,a0,0
     3b0:	0308                	addi	a0,sp,384
     3b2:	0904                	addi	s1,sp,144
     3b4:	0004                	0x4
     3b6:	0501                	addi	a0,a0,0
     3b8:	0305                	addi	t1,t1,1
     3ba:	0950                	addi	a2,sp,148
     3bc:	000c                	0xc
     3be:	0501                	addi	a0,a0,0
     3c0:	030c                	addi	a1,sp,384
     3c2:	0904                	addi	s1,sp,144
     3c4:	0004                	0x4
     3c6:	0501                	addi	a0,a0,0
     3c8:	031c                	addi	a5,sp,384
     3ca:	0902                	c.slli64	s2
     3cc:	0004                	0x4
     3ce:	0501                	addi	a0,a0,0
     3d0:	0616                	slli	a2,a2,0x5
     3d2:	04097a03          	0x4097a03
     3d6:	0100                	addi	s0,sp,128
     3d8:	0905                	addi	s2,s2,1
     3da:	00090103          	lb	sp,0(s2)
     3de:	0100                	addi	s0,sp,128
     3e0:	00090103          	lb	sp,0(s2)
     3e4:	0100                	addi	s0,sp,128
     3e6:	1a05                	addi	s4,s4,-31
     3e8:	0306                	slli	t1,t1,0x1
     3ea:	0900                	addi	s0,sp,144
     3ec:	0000                	unimp
     3ee:	0501                	addi	a0,a0,0
     3f0:	0609                	addi	a2,a2,2
     3f2:	04090203          	lb	tp,64(s2)
     3f6:	0100                	addi	s0,sp,128
     3f8:	0e05                	addi	t3,t3,1
     3fa:	0306                	slli	t1,t1,0x1
     3fc:	097c                	addi	a5,sp,156
     3fe:	0000                	unimp
     400:	0301                	addi	t1,t1,0
     402:	097d                	addi	s2,s2,31
     404:	0004                	0x4
     406:	0501                	addi	a0,a0,0
     408:	060d                	addi	a2,a2,3
     40a:	04090803          	lb	a6,64(s2)
     40e:	0100                	addi	s0,sp,128
     410:	1e05                	addi	t3,t3,-31
     412:	00097b03          	0x97b03
     416:	0100                	addi	s0,sp,128
     418:	1f05                	addi	t5,t5,-31
     41a:	0306                	slli	t1,t1,0x1
     41c:	0900                	addi	s0,sp,144
     41e:	0000                	unimp
     420:	0501                	addi	a0,a0,0
     422:	0609                	addi	a2,a2,2
     424:	04090103          	lb	sp,64(s2)
     428:	0100                	addi	s0,sp,128
     42a:	1a05                	addi	s4,s4,-31
     42c:	0306                	slli	t1,t1,0x1
     42e:	0901                	addi	s2,s2,0
     430:	0000                	unimp
     432:	0501                	addi	a0,a0,0
     434:	030d                	addi	t1,t1,3
     436:	097f                	0x97f
     438:	0004                	0x4
     43a:	0501                	addi	a0,a0,0
     43c:	0609                	addi	a2,a2,2
     43e:	04090103          	lb	sp,64(s2)
     442:	0100                	addi	s0,sp,128
     444:	1605                	addi	a2,a2,-31
     446:	00097e03          	0x97e03
     44a:	0100                	addi	s0,sp,128
     44c:	2005                	jal	46c <_start-0xb94>
     44e:	0306                	slli	t1,t1,0x1
     450:	0902                	c.slli64	s2
     452:	0000                	unimp
     454:	0501                	addi	a0,a0,0
     456:	031c                	addi	a5,sp,384
     458:	0904                	addi	s1,sp,144
     45a:	0004                	0x4
     45c:	0501                	addi	a0,a0,0
     45e:	0305                	addi	t1,t1,1
     460:	097a                	slli	s2,s2,0x1e
     462:	0004                	0x4
     464:	0501                	addi	a0,a0,0
     466:	030d                	addi	t1,t1,3
     468:	0902                	c.slli64	s2
     46a:	0004                	0x4
     46c:	0501                	addi	a0,a0,0
     46e:	0609                	addi	a2,a2,2
     470:	04090203          	lb	tp,64(s2)
     474:	0100                	addi	s0,sp,128
     476:	0c05                	addi	s8,s8,1
     478:	0306                	slli	t1,t1,0x1
     47a:	0900                	addi	s0,sp,144
     47c:	0000                	unimp
     47e:	0501                	addi	a0,a0,0
     480:	031f 097c 0004      	0x4097c031f
     486:	0501                	addi	a0,a0,0
     488:	0311                	addi	t1,t1,4
     48a:	0905                	addi	s2,s2,1
     48c:	0004                	0x4
     48e:	0501                	addi	a0,a0,0
     490:	060d                	addi	a2,a2,3
     492:	04090103          	lb	sp,64(s2)
     496:	0100                	addi	s0,sp,128
     498:	1a05                	addi	s4,s4,-31
     49a:	0306                	slli	t1,t1,0x1
     49c:	097c                	addi	a5,sp,156
     49e:	0000                	unimp
     4a0:	0501                	addi	a0,a0,0
     4a2:	0316                	slli	t1,t1,0x5
     4a4:	0904                	addi	s1,sp,144
     4a6:	0004                	0x4
     4a8:	0501                	addi	a0,a0,0
     4aa:	060d                	addi	a2,a2,3
     4ac:	04097f03          	0x4097f03
     4b0:	0100                	addi	s0,sp,128
     4b2:	1e05                	addi	t3,t3,-31
     4b4:	00097b03          	0x97b03
     4b8:	0100                	addi	s0,sp,128
     4ba:	0905                	addi	s2,s2,1
     4bc:	00090103          	lb	sp,0(s2)
     4c0:	0100                	addi	s0,sp,128
     4c2:	0d05                	addi	s10,s10,1
     4c4:	0306                	slli	t1,t1,0x1
     4c6:	0900                	addi	s0,sp,144
     4c8:	0000                	unimp
     4ca:	0501                	addi	a0,a0,0
     4cc:	0609                	addi	a2,a2,2
     4ce:	04090103          	lb	sp,64(s2)
     4d2:	0100                	addi	s0,sp,128
     4d4:	1605                	addi	a2,a2,-31
     4d6:	00097e03          	0x97e03
     4da:	0100                	addi	s0,sp,128
     4dc:	2005                	jal	4fc <_start-0xb04>
     4de:	0306                	slli	t1,t1,0x1
     4e0:	0902                	c.slli64	s2
     4e2:	0000                	unimp
     4e4:	0501                	addi	a0,a0,0
     4e6:	031c                	addi	a5,sp,384
     4e8:	0904                	addi	s1,sp,144
     4ea:	0004                	0x4
     4ec:	0501                	addi	a0,a0,0
     4ee:	0305                	addi	t1,t1,1
     4f0:	097a                	slli	s2,s2,0x1e
     4f2:	0004                	0x4
     4f4:	0601                	addi	a2,a2,0
     4f6:	04090a03          	lb	s4,64(s2)
     4fa:	0100                	addi	s0,sp,128
     4fc:	0905                	addi	s2,s2,1
     4fe:	00090103          	lb	sp,0(s2)
     502:	0100                	addi	s0,sp,128
     504:	00092d03          	lw	s10,0(s2)
     508:	0100                	addi	s0,sp,128
     50a:	1105                	addi	sp,sp,-31
     50c:	0306                	slli	t1,t1,0x1
     50e:	0900                	addi	s0,sp,144
     510:	0000                	unimp
     512:	0501                	addi	a0,a0,0
     514:	0900031b          	0x900031b
     518:	0004                	0x4
     51a:	0501                	addi	a0,a0,0
     51c:	0315                	addi	t1,t1,5
     51e:	0900                	addi	s0,sp,144
     520:	000c                	0xc
     522:	0501                	addi	a0,a0,0
     524:	7d03060b          	0x7d03060b
     528:	0409                	addi	s0,s0,2
     52a:	0100                	addi	s0,sp,128
     52c:	0d05                	addi	s10,s10,1
     52e:	0306                	slli	t1,t1,0x1
     530:	0962                	slli	s2,s2,0x18
     532:	000c                	0xc
     534:	0501                	addi	a0,a0,0
     536:	0305                	addi	t1,t1,1
     538:	00040907          	0x40907
     53c:	0501                	addi	a0,a0,0
     53e:	0609                	addi	a2,a2,2
     540:	04097a03          	0x4097a03
     544:	0100                	addi	s0,sp,128
     546:	0b05                	addi	s6,s6,1
     548:	00097f03          	0x97f03
     54c:	0100                	addi	s0,sp,128
     54e:	0d05                	addi	s10,s10,1
     550:	0306                	slli	t1,t1,0x1
     552:	0900                	addi	s0,sp,144
     554:	0000                	unimp
     556:	0501                	addi	a0,a0,0
     558:	0900031b          	0x900031b
     55c:	0004                	0x4
     55e:	0501                	addi	a0,a0,0
     560:	0900030b          	0x900030b
     564:	0004                	0x4
     566:	0501                	addi	a0,a0,0
     568:	0605                	addi	a2,a2,1
     56a:	04090403          	lb	s0,64(s2)
     56e:	0100                	addi	s0,sp,128
     570:	0e05                	addi	t3,t3,1
     572:	0306                	slli	t1,t1,0x1
     574:	0900                	addi	s0,sp,144
     576:	0000                	unimp
     578:	0501                	addi	a0,a0,0
     57a:	0605                	addi	a2,a2,1
     57c:	04090303          	lb	t1,64(s2)
     580:	0100                	addi	s0,sp,128
     582:	0a05                	addi	s4,s4,1
     584:	00090003          	lb	zero,0(s2)
     588:	0100                	addi	s0,sp,128
     58a:	1705                	addi	a4,a4,-31
     58c:	0306                	slli	t1,t1,0x1
     58e:	0900                	addi	s0,sp,144
     590:	0000                	unimp
     592:	0501                	addi	a0,a0,0
     594:	061e                	slli	a2,a2,0x7
     596:	04090003          	lb	zero,64(s2)
     59a:	0100                	addi	s0,sp,128
     59c:	2005                	jal	5bc <_start-0xa44>
     59e:	0306                	slli	t1,t1,0x1
     5a0:	0900                	addi	s0,sp,144
     5a2:	0000                	unimp
     5a4:	0501                	addi	a0,a0,0
     5a6:	0305                	addi	t1,t1,1
     5a8:	0900                	addi	s0,sp,144
     5aa:	0004                	0x4
     5ac:	0501                	addi	a0,a0,0
     5ae:	0609                	addi	a2,a2,2
     5b0:	04090103          	lb	sp,64(s2)
     5b4:	0100                	addi	s0,sp,128
     5b6:	2705                	jal	cd6 <_start-0x32a>
     5b8:	00097f03          	0x97f03
     5bc:	0100                	addi	s0,sp,128
     5be:	2805                	jal	5ee <_start-0xa12>
     5c0:	0306                	slli	t1,t1,0x1
     5c2:	0900                	addi	s0,sp,144
     5c4:	0000                	unimp
     5c6:	0501                	addi	a0,a0,0
     5c8:	061e                	slli	a2,a2,0x7
     5ca:	0c090003          	lb	zero,192(s2)
     5ce:	0100                	addi	s0,sp,128
     5d0:	2005                	jal	5f0 <_start-0xa10>
     5d2:	0306                	slli	t1,t1,0x1
     5d4:	0900                	addi	s0,sp,144
     5d6:	0000                	unimp
     5d8:	0501                	addi	a0,a0,0
     5da:	0305                	addi	t1,t1,1
     5dc:	0900                	addi	s0,sp,144
     5de:	0004                	0x4
     5e0:	0501                	addi	a0,a0,0
     5e2:	1d03060b          	0x1d03060b
     5e6:	0409                	addi	s0,s0,2
     5e8:	0100                	addi	s0,sp,128
     5ea:	0905                	addi	s2,s2,1
     5ec:	0306                	slli	t1,t1,0x1
     5ee:	0901                	addi	s2,s2,0
     5f0:	0008                	0x8
     5f2:	0601                	addi	a2,a2,0
     5f4:	04090003          	lb	zero,64(s2)
     5f8:	0100                	addi	s0,sp,128
     5fa:	0d05                	addi	s10,s10,1
     5fc:	0306                	slli	t1,t1,0x1
     5fe:	0004095b          	0x4095b
     602:	0501                	addi	a0,a0,0
     604:	0609                	addi	a2,a2,2
     606:	04090103          	lb	sp,64(s2)
     60a:	0100                	addi	s0,sp,128
     60c:	0b05                	addi	s6,s6,1
     60e:	00097f03          	0x97f03
     612:	0100                	addi	s0,sp,128
     614:	0d05                	addi	s10,s10,1
     616:	0306                	slli	t1,t1,0x1
     618:	0900                	addi	s0,sp,144
     61a:	0000                	unimp
     61c:	0501                	addi	a0,a0,0
     61e:	0900031b          	0x900031b
     622:	0004                	0x4
     624:	0501                	addi	a0,a0,0
     626:	0900030b          	0x900030b
     62a:	0004                	0x4
     62c:	0501                	addi	a0,a0,0
     62e:	0605                	addi	a2,a2,1
     630:	04090403          	lb	s0,64(s2)
     634:	0100                	addi	s0,sp,128
     636:	0e05                	addi	t3,t3,1
     638:	0306                	slli	t1,t1,0x1
     63a:	0900                	addi	s0,sp,144
     63c:	0000                	unimp
     63e:	0501                	addi	a0,a0,0
     640:	0605                	addi	a2,a2,1
     642:	08090303          	lb	t1,128(s2)
     646:	0100                	addi	s0,sp,128
     648:	0a05                	addi	s4,s4,1
     64a:	00090003          	lb	zero,0(s2)
     64e:	0100                	addi	s0,sp,128
     650:	1705                	addi	a4,a4,-31
     652:	0306                	slli	t1,t1,0x1
     654:	0900                	addi	s0,sp,144
     656:	0000                	unimp
     658:	0501                	addi	a0,a0,0
     65a:	061e                	slli	a2,a2,0x7
     65c:	04090003          	lb	zero,64(s2)
     660:	0100                	addi	s0,sp,128
     662:	2005                	jal	682 <_start-0x97e>
     664:	0306                	slli	t1,t1,0x1
     666:	0900                	addi	s0,sp,144
     668:	0000                	unimp
     66a:	0501                	addi	a0,a0,0
     66c:	0305                	addi	t1,t1,1
     66e:	0900                	addi	s0,sp,144
     670:	0004                	0x4
     672:	0501                	addi	a0,a0,0
     674:	0609                	addi	a2,a2,2
     676:	0c090103          	lb	sp,192(s2)
     67a:	0100                	addi	s0,sp,128
     67c:	2705                	jal	d9c <_start-0x264>
     67e:	00097f03          	0x97f03
     682:	0100                	addi	s0,sp,128
     684:	2805                	jal	6b4 <_start-0x94c>
     686:	0306                	slli	t1,t1,0x1
     688:	0900                	addi	s0,sp,144
     68a:	0000                	unimp
     68c:	0501                	addi	a0,a0,0
     68e:	061e                	slli	a2,a2,0x7
     690:	0c090003          	lb	zero,192(s2)
     694:	0100                	addi	s0,sp,128
     696:	2005                	jal	6b6 <_start-0x94a>
     698:	0306                	slli	t1,t1,0x1
     69a:	0900                	addi	s0,sp,144
     69c:	0000                	unimp
     69e:	0501                	addi	a0,a0,0
     6a0:	0305                	addi	t1,t1,1
     6a2:	0900                	addi	s0,sp,144
     6a4:	0004                	0x4
     6a6:	0501                	addi	a0,a0,0
     6a8:	0301                	addi	t1,t1,0
     6aa:	0920                	addi	s0,sp,152
     6ac:	0004                	0x4
     6ae:	0601                	addi	a2,a2,0
     6b0:	08091003          	lh	zero,128(s2)
     6b4:	0100                	addi	s0,sp,128
     6b6:	0505                	addi	a0,a0,1
     6b8:	00090103          	lb	sp,0(s2)
     6bc:	0100                	addi	s0,sp,128
     6be:	00090103          	lb	sp,0(s2)
     6c2:	0100                	addi	s0,sp,128
     6c4:	04090103          	lb	sp,64(s2)
     6c8:	0100                	addi	s0,sp,128
     6ca:	0105                	addi	sp,sp,1
     6cc:	0306                	slli	t1,t1,0x1
     6ce:	0901                	addi	s2,s2,0
     6d0:	0000                	unimp
     6d2:	0601                	addi	a2,a2,0
     6d4:	04090703          	lb	a4,64(s2)
     6d8:	0100                	addi	s0,sp,128
     6da:	0505                	addi	a0,a0,1
     6dc:	00090203          	lb	tp,0(s2)
     6e0:	0100                	addi	s0,sp,128
     6e2:	1805                	addi	a6,a6,-31
     6e4:	0306                	slli	t1,t1,0x1
     6e6:	0900                	addi	s0,sp,144
     6e8:	0000                	unimp
     6ea:	0501                	addi	a0,a0,0
     6ec:	0605                	addi	a2,a2,1
     6ee:	04090103          	lb	sp,64(s2)
     6f2:	0100                	addi	s0,sp,128
     6f4:	1805                	addi	a6,a6,-31
     6f6:	0306                	slli	t1,t1,0x1
     6f8:	0900                	addi	s0,sp,144
     6fa:	0000                	unimp
     6fc:	0501                	addi	a0,a0,0
     6fe:	0605                	addi	a2,a2,1
     700:	04090103          	lb	sp,64(s2)
     704:	0100                	addi	s0,sp,128
     706:	1605                	addi	a2,a2,-31
     708:	0306                	slli	t1,t1,0x1
     70a:	0900                	addi	s0,sp,144
     70c:	0000                	unimp
     70e:	0501                	addi	a0,a0,0
     710:	0605                	addi	a2,a2,1
     712:	04090103          	lb	sp,64(s2)
     716:	0100                	addi	s0,sp,128
     718:	1805                	addi	a6,a6,-31
     71a:	0306                	slli	t1,t1,0x1
     71c:	0900                	addi	s0,sp,144
     71e:	0000                	unimp
     720:	0501                	addi	a0,a0,0
     722:	0605                	addi	a2,a2,1
     724:	04090303          	lb	t1,64(s2)
     728:	0100                	addi	s0,sp,128
     72a:	04090303          	lb	t1,64(s2)
     72e:	0100                	addi	s0,sp,128
     730:	1305                	addi	t1,t1,-31
     732:	0306                	slli	t1,t1,0x1
     734:	0900                	addi	s0,sp,144
     736:	0000                	unimp
     738:	0501                	addi	a0,a0,0
     73a:	0901030f          	0x901030f
     73e:	0004                	0x4
     740:	0501                	addi	a0,a0,0
     742:	097f0313          	addi	t1,t5,151
     746:	0004                	0x4
     748:	0501                	addi	a0,a0,0
     74a:	0605                	addi	a2,a2,1
     74c:	04090103          	lb	sp,64(s2)
     750:	0100                	addi	s0,sp,128
     752:	0f05                	addi	t5,t5,1
     754:	0306                	slli	t1,t1,0x1
     756:	0900                	addi	s0,sp,144
     758:	0000                	unimp
     75a:	0501                	addi	a0,a0,0
     75c:	030e                	slli	t1,t1,0x3
     75e:	0904                	addi	s1,sp,144
     760:	0004                	0x4
     762:	0501                	addi	a0,a0,0
     764:	0605                	addi	a2,a2,1
     766:	04097f03          	0x4097f03
     76a:	0100                	addi	s0,sp,128
     76c:	0905                	addi	s2,s2,1
     76e:	00090103          	lb	sp,0(s2)
     772:	0100                	addi	s0,sp,128
     774:	0e05                	addi	t3,t3,1
     776:	0306                	slli	t1,t1,0x1
     778:	0900                	addi	s0,sp,144
     77a:	0000                	unimp
     77c:	0501                	addi	a0,a0,0
     77e:	0900031b          	0x900031b
     782:	0004                	0x4
     784:	0501                	addi	a0,a0,0
     786:	030c                	addi	a1,sp,384
     788:	0900                	addi	s0,sp,144
     78a:	0004                	0x4
     78c:	0501                	addi	a0,a0,0
     78e:	0301                	addi	t1,t1,0
     790:	00040903          	lb	s2,0(s0)
     794:	0601                	addi	a2,a2,0
     796:	04090803          	lb	a6,64(s2)
     79a:	0100                	addi	s0,sp,128
     79c:	0505                	addi	a0,a0,1
     79e:	00090103          	lb	sp,0(s2)
     7a2:	0100                	addi	s0,sp,128
     7a4:	00090103          	lb	sp,0(s2)
     7a8:	0100                	addi	s0,sp,128
     7aa:	00090103          	lb	sp,0(s2)
     7ae:	0100                	addi	s0,sp,128
     7b0:	0b05                	addi	s6,s6,1
     7b2:	00090003          	lb	zero,0(s2)
     7b6:	0100                	addi	s0,sp,128
     7b8:	0905                	addi	s2,s2,1
     7ba:	08090103          	lb	sp,128(s2)
     7be:	0100                	addi	s0,sp,128
     7c0:	1005                	c.nop	-31
     7c2:	0306                	slli	t1,t1,0x1
     7c4:	0900                	addi	s0,sp,144
     7c6:	0000                	unimp
     7c8:	0501                	addi	a0,a0,0
     7ca:	0900030b          	0x900030b
     7ce:	0008                	0x8
     7d0:	0501                	addi	a0,a0,0
     7d2:	0310                	addi	a2,sp,384
     7d4:	0900                	addi	s0,sp,144
     7d6:	0004                	0x4
     7d8:	0501                	addi	a0,a0,0
     7da:	030e                	slli	t1,t1,0x3
     7dc:	0900                	addi	s0,sp,144
     7de:	0004                	0x4
     7e0:	0501                	addi	a0,a0,0
     7e2:	0312                	slli	t1,t1,0x4
     7e4:	0900                	addi	s0,sp,144
     7e6:	0004                	0x4
     7e8:	0501                	addi	a0,a0,0
     7ea:	7f03060b          	0x7f03060b
     7ee:	0409                	addi	s0,s0,2
     7f0:	0100                	addi	s0,sp,128
     7f2:	0105                	addi	sp,sp,1
     7f4:	0306                	slli	t1,t1,0x1
     7f6:	00040903          	lb	s2,0(s0)
     7fa:	0901                	addi	s2,s2,0
     7fc:	0004                	0x4
     7fe:	0100                	addi	s0,sp,128
     800:	0501                	addi	a0,a0,0
     802:	0001                	nop
     804:	0205                	addi	tp,tp,1
     806:	1288                	addi	a0,sp,352
     808:	0000                	unimp
     80a:	0101b103          	0x101b103
     80e:	0505                	addi	a0,a0,1
     810:	00090203          	lb	tp,0(s2)
     814:	0100                	addi	s0,sp,128
     816:	0a05                	addi	s4,s4,1
     818:	00090003          	lb	zero,0(s2)
     81c:	0100                	addi	s0,sp,128
     81e:	0105                	addi	sp,sp,1
     820:	0306                	slli	t1,t1,0x1
     822:	097e                	slli	s2,s2,0x1f
     824:	0000                	unimp
     826:	0501                	addi	a0,a0,0
     828:	09020317          	auipc	t1,0x9020
     82c:	0004                	0x4
     82e:	0501                	addi	a0,a0,0
     830:	061e                	slli	a2,a2,0x7
     832:	04090003          	lb	zero,64(s2)
     836:	0100                	addi	s0,sp,128
     838:	2005                	jal	858 <_start-0x7a8>
     83a:	0306                	slli	t1,t1,0x1
     83c:	0900                	addi	s0,sp,144
     83e:	0000                	unimp
     840:	0501                	addi	a0,a0,0
     842:	0305                	addi	t1,t1,1
     844:	0900                	addi	s0,sp,144
     846:	0004                	0x4
     848:	0501                	addi	a0,a0,0
     84a:	0301                	addi	t1,t1,0
     84c:	097e                	slli	s2,s2,0x1f
     84e:	0004                	0x4
     850:	0501                	addi	a0,a0,0
     852:	0305                	addi	t1,t1,1
     854:	0902                	c.slli64	s2
     856:	0034                	addi	a3,sp,8
     858:	0501                	addi	a0,a0,0
     85a:	0009                	c.nop	2
     85c:	0402                	c.slli64	s0
     85e:	01030603          	lb	a2,16(t1) # 9020838 <_stack_top+0x8f20838>
     862:	0809                	addi	a6,a6,2
     864:	0100                	addi	s0,sp,128
     866:	2905                	jal	c96 <_start-0x36a>
     868:	0200                	addi	s0,sp,256
     86a:	0304                	addi	s1,sp,384
     86c:	00097f03          	0x97f03
     870:	0100                	addi	s0,sp,128
     872:	2a05                	jal	9a2 <_start-0x65e>
     874:	0200                	addi	s0,sp,256
     876:	0304                	addi	s1,sp,384
     878:	0306                	slli	t1,t1,0x1
     87a:	0900                	addi	s0,sp,144
     87c:	0000                	unimp
     87e:	0501                	addi	a0,a0,0
     880:	001e                	c.slli	zero,0x7
     882:	0402                	c.slli64	s0
     884:	00030603          	lb	a2,0(t1)
     888:	0c09                	addi	s8,s8,2
     88a:	0100                	addi	s0,sp,128
     88c:	2005                	jal	8ac <_start-0x754>
     88e:	0200                	addi	s0,sp,256
     890:	0304                	addi	s1,sp,384
     892:	0306                	slli	t1,t1,0x1
     894:	0900                	addi	s0,sp,144
     896:	0000                	unimp
     898:	0501                	addi	a0,a0,0
     89a:	0005                	c.nop	1
     89c:	0402                	c.slli64	s0
     89e:	09000303          	lb	t1,144(zero) # 90 <_start-0xf70>
     8a2:	0004                	0x4
     8a4:	0601                	addi	a2,a2,0
     8a6:	04090403          	lb	s0,64(s2)
     8aa:	0100                	addi	s0,sp,128
     8ac:	0c090103          	lb	sp,192(s2)
     8b0:	0100                	addi	s0,sp,128
     8b2:	0c090103          	lb	sp,192(s2)
     8b6:	0100                	addi	s0,sp,128
     8b8:	0c090303          	lb	t1,192(s2)
     8bc:	0100                	addi	s0,sp,128
     8be:	0a05                	addi	s4,s4,1
     8c0:	00090003          	lb	zero,0(s2)
     8c4:	0100                	addi	s0,sp,128
     8c6:	1505                	addi	a0,a0,-31
     8c8:	00090003          	lb	zero,0(s2)
     8cc:	0100                	addi	s0,sp,128
     8ce:	0505                	addi	a0,a0,1
     8d0:	0306                	slli	t1,t1,0x1
     8d2:	097d                	addi	s2,s2,31
     8d4:	0000                	unimp
     8d6:	0501                	addi	a0,a0,0
     8d8:	030e                	slli	t1,t1,0x3
     8da:	000c0903          	lb	s2,0(s8) # fffe2000 <_stack_top+0xffee2000>
     8de:	0501                	addi	a0,a0,0
     8e0:	0305                	addi	t1,t1,1
     8e2:	0900                	addi	s0,sp,144
     8e4:	0004                	0x4
     8e6:	0501                	addi	a0,a0,0
     8e8:	0009                	c.nop	2
     8ea:	0402                	c.slli64	s0
     8ec:	01030603          	lb	a2,16(t1)
     8f0:	0409                	addi	s0,s0,2
     8f2:	0100                	addi	s0,sp,128
     8f4:	1405                	addi	s0,s0,-31
     8f6:	0200                	addi	s0,sp,256
     8f8:	0304                	addi	s1,sp,384
     8fa:	0306                	slli	t1,t1,0x1
     8fc:	0900                	addi	s0,sp,144
     8fe:	0000                	unimp
     900:	0501                	addi	a0,a0,0
     902:	001f 0402 0603      	0x6030402001f
     908:	04097f03          	0x4097f03
     90c:	0100                	addi	s0,sp,128
     90e:	2005                	jal	92e <_start-0x6d2>
     910:	0200                	addi	s0,sp,256
     912:	0304                	addi	s1,sp,384
     914:	0306                	slli	t1,t1,0x1
     916:	0900                	addi	s0,sp,144
     918:	0000                	unimp
     91a:	0501                	addi	a0,a0,0
     91c:	0015                	c.nop	5
     91e:	0402                	c.slli64	s0
     920:	00030603          	lb	a2,0(t1)
     924:	0409                	addi	s0,s0,2
     926:	0100                	addi	s0,sp,128
     928:	0505                	addi	a0,a0,1
     92a:	0200                	addi	s0,sp,256
     92c:	0304                	addi	s1,sp,384
     92e:	0306                	slli	t1,t1,0x1
     930:	0900                	addi	s0,sp,144
     932:	0000                	unimp
     934:	0601                	addi	a2,a2,0
     936:	08090403          	lb	s0,128(s2)
     93a:	0100                	addi	s0,sp,128
     93c:	0905                	addi	s2,s2,1
     93e:	0306                	slli	t1,t1,0x1
     940:	0900                	addi	s0,sp,144
     942:	0000                	unimp
     944:	0501                	addi	a0,a0,0
     946:	0605                	addi	a2,a2,1
     948:	30090303          	lb	t1,768(s2)
     94c:	0100                	addi	s0,sp,128
     94e:	0a05                	addi	s4,s4,1
     950:	00090003          	lb	zero,0(s2)
     954:	0100                	addi	s0,sp,128
     956:	1505                	addi	a0,a0,-31
     958:	00090003          	lb	zero,0(s2)
     95c:	0100                	addi	s0,sp,128
     95e:	1705                	addi	a4,a4,-31
     960:	0306                	slli	t1,t1,0x1
     962:	001c095b          	0x1c095b
     966:	0501                	addi	a0,a0,0
     968:	031a                	slli	t1,t1,0x6
     96a:	0966                	slli	s2,s2,0x19
     96c:	0004                	0x4
     96e:	0501                	addi	a0,a0,0
     970:	0315                	addi	t1,t1,5
     972:	0908                	addi	a0,sp,144
     974:	0004                	0x4
     976:	0501                	addi	a0,a0,0
     978:	09120317          	auipc	t1,0x9120
     97c:	0008                	0x8
     97e:	0501                	addi	a0,a0,0
     980:	0309                	addi	t1,t1,2
     982:	0922                	slli	s2,s2,0x8
     984:	0004                	0x4
     986:	0501                	addi	a0,a0,0
     988:	031a                	slli	t1,t1,0x6
     98a:	0944                	addi	s1,sp,148
     98c:	0018                	0x18
     98e:	0501                	addi	a0,a0,0
     990:	0315                	addi	t1,t1,5
     992:	0908                	addi	a0,sp,144
     994:	0004                	0x4
     996:	0501                	addi	a0,a0,0
     998:	09120317          	auipc	t1,0x9120
     99c:	000c                	0xc
     99e:	0501                	addi	a0,a0,0
     9a0:	096e0313          	addi	t1,t3,150
     9a4:	0004                	0x4
     9a6:	0501                	addi	a0,a0,0
     9a8:	0901030f          	0x901030f
     9ac:	0004                	0x4
     9ae:	0501                	addi	a0,a0,0
     9b0:	030c                	addi	a1,sp,384
     9b2:	0940                	addi	s0,sp,148
     9b4:	0004                	0x4
     9b6:	0501                	addi	a0,a0,0
     9b8:	0305                	addi	t1,t1,1
     9ba:	0979                	addi	s2,s2,30
     9bc:	0004                	0x4
     9be:	0501                	addi	a0,a0,0
     9c0:	0609                	addi	a2,a2,2
     9c2:	0900fe03          	0x900fe03
     9c6:	0004                	0x4
     9c8:	0301                	addi	t1,t1,0
     9ca:	00000903          	lb	s2,0(zero) # 0 <_start-0x1000>
     9ce:	0501                	addi	a0,a0,0
     9d0:	0305                	addi	t1,t1,1
     9d2:	7fb0                	flw	fa2,120(a5)
     9d4:	0009                	c.nop	2
     9d6:	0100                	addi	s0,sp,128
     9d8:	00090103          	lb	sp,0(s2)
     9dc:	0100                	addi	s0,sp,128
     9de:	04090103          	lb	sp,64(s2)
     9e2:	0100                	addi	s0,sp,128
     9e4:	0905                	addi	s2,s2,1
     9e6:	0900cf03          	lbu	t5,144(ra)
     9ea:	0000                	unimp
     9ec:	0501                	addi	a0,a0,0
     9ee:	0957030b          	0x957030b
     9f2:	0004                	0x4
     9f4:	0501                	addi	a0,a0,0
     9f6:	7e030617          	auipc	a2,0x7e030
     9fa:	0809                	addi	a6,a6,2
     9fc:	0100                	addi	s0,sp,128
     9fe:	08090103          	lb	sp,128(s2)
     a02:	0100                	addi	s0,sp,128
     a04:	0905                	addi	s2,s2,1
     a06:	0306                	slli	t1,t1,0x1
     a08:	0902                	c.slli64	s2
     a0a:	0010                	0x10
     a0c:	0501                	addi	a0,a0,0
     a0e:	0610                	addi	a2,sp,768
     a10:	00090003          	lb	zero,0(s2)
     a14:	0100                	addi	s0,sp,128
     a16:	1205                	addi	tp,tp,-31
     a18:	04090003          	lb	zero,64(s2)
     a1c:	0100                	addi	s0,sp,128
     a1e:	0b05                	addi	s6,s6,1
     a20:	04090003          	lb	zero,64(s2)
     a24:	0100                	addi	s0,sp,128
     a26:	1005                	c.nop	-31
     a28:	04090003          	lb	zero,64(s2)
     a2c:	0100                	addi	s0,sp,128
     a2e:	0e05                	addi	t3,t3,1
     a30:	04090003          	lb	zero,64(s2)
     a34:	0100                	addi	s0,sp,128
     a36:	0b05                	addi	s6,s6,1
     a38:	0306                	slli	t1,t1,0x1
     a3a:	097f                	0x97f
     a3c:	0004                	0x4
     a3e:	0501                	addi	a0,a0,0
     a40:	0309                	addi	t1,t1,2
     a42:	092a                	slli	s2,s2,0xa
     a44:	0004                	0x4
     a46:	0501                	addi	a0,a0,0
     a48:	0305                	addi	t1,t1,1
     a4a:	7fae                	flw	ft11,232(sp)
     a4c:	0009                	c.nop	2
     a4e:	0100                	addi	s0,sp,128
     a50:	00090103          	lb	sp,0(s2)
     a54:	0100                	addi	s0,sp,128
     a56:	04090103          	lb	sp,64(s2)
     a5a:	0100                	addi	s0,sp,128
     a5c:	0905                	addi	s2,s2,1
     a5e:	0900d103          	lhu	sp,144(ra)
     a62:	0000                	unimp
     a64:	0501                	addi	a0,a0,0
     a66:	0612                	slli	a2,a2,0x4
     a68:	00090003          	lb	zero,0(s2)
     a6c:	0100                	addi	s0,sp,128
     a6e:	0905                	addi	s2,s2,1
     a70:	0306                	slli	t1,t1,0x1
     a72:	00040903          	lb	s2,0(s0)
     a76:	0501                	addi	a0,a0,0
     a78:	0305                	addi	t1,t1,1
     a7a:	7faa                	flw	ft11,168(sp)
     a7c:	0009                	c.nop	2
     a7e:	0100                	addi	s0,sp,128
     a80:	00090103          	lb	sp,0(s2)
     a84:	0100                	addi	s0,sp,128
     a86:	04090103          	lb	sp,64(s2)
     a8a:	0100                	addi	s0,sp,128
     a8c:	0905                	addi	s2,s2,1
     a8e:	0900d503          	lhu	a0,144(ra)
     a92:	0000                	unimp
     a94:	0501                	addi	a0,a0,0
     a96:	0305                	addi	t1,t1,1
     a98:	7fb5                	lui	t6,0xfffed
     a9a:	0009                	c.nop	2
     a9c:	0100                	addi	s0,sp,128
     a9e:	1805                	addi	a6,a6,-31
     aa0:	0306                	slli	t1,t1,0x1
     aa2:	0900                	addi	s0,sp,144
     aa4:	0000                	unimp
     aa6:	0501                	addi	a0,a0,0
     aa8:	0605                	addi	a2,a2,1
     aaa:	0c090103          	lb	sp,192(s2)
     aae:	0100                	addi	s0,sp,128
     ab0:	1805                	addi	a6,a6,-31
     ab2:	0306                	slli	t1,t1,0x1
     ab4:	0900                	addi	s0,sp,144
     ab6:	0000                	unimp
     ab8:	0501                	addi	a0,a0,0
     aba:	0605                	addi	a2,a2,1
     abc:	04090103          	lb	sp,64(s2)
     ac0:	0100                	addi	s0,sp,128
     ac2:	1605                	addi	a2,a2,-31
     ac4:	0306                	slli	t1,t1,0x1
     ac6:	0900                	addi	s0,sp,144
     ac8:	0000                	unimp
     aca:	0501                	addi	a0,a0,0
     acc:	0605                	addi	a2,a2,1
     ace:	04090103          	lb	sp,64(s2)
     ad2:	0100                	addi	s0,sp,128
     ad4:	1805                	addi	a6,a6,-31
     ad6:	0306                	slli	t1,t1,0x1
     ad8:	0900                	addi	s0,sp,144
     ada:	0000                	unimp
     adc:	0501                	addi	a0,a0,0
     ade:	0605                	addi	a2,a2,1
     ae0:	04090303          	lb	t1,64(s2)
     ae4:	0100                	addi	s0,sp,128
     ae6:	04090303          	lb	t1,64(s2)
     aea:	0100                	addi	s0,sp,128
     aec:	1305                	addi	t1,t1,-31
     aee:	0306                	slli	t1,t1,0x1
     af0:	0900                	addi	s0,sp,144
     af2:	0000                	unimp
     af4:	0501                	addi	a0,a0,0
     af6:	0605                	addi	a2,a2,1
     af8:	04090103          	lb	sp,64(s2)
     afc:	0100                	addi	s0,sp,128
     afe:	0f05                	addi	t5,t5,1
     b00:	0306                	slli	t1,t1,0x1
     b02:	0900                	addi	s0,sp,144
     b04:	0000                	unimp
     b06:	0501                	addi	a0,a0,0
     b08:	0605                	addi	a2,a2,1
     b0a:	04090303          	lb	t1,64(s2)
     b0e:	0100                	addi	s0,sp,128
     b10:	0905                	addi	s2,s2,1
     b12:	00090103          	lb	sp,0(s2)
     b16:	0100                	addi	s0,sp,128
     b18:	0e05                	addi	t3,t3,1
     b1a:	0306                	slli	t1,t1,0x1
     b1c:	0900                	addi	s0,sp,144
     b1e:	0000                	unimp
     b20:	0501                	addi	a0,a0,0
     b22:	0900031b          	0x900031b
     b26:	0004                	0x4
     b28:	0501                	addi	a0,a0,0
     b2a:	030c                	addi	a1,sp,384
     b2c:	0900                	addi	s0,sp,144
     b2e:	0004                	0x4
     b30:	0501                	addi	a0,a0,0
     b32:	0609                	addi	a2,a2,2
     b34:	04093e03          	0x4093e03
     b38:	0100                	addi	s0,sp,128
     b3a:	0505                	addi	a0,a0,1
     b3c:	097fa803          	lw	a6,151(t6) # fffed097 <_stack_top+0xffeed097>
     b40:	0000                	unimp
     b42:	0301                	addi	t1,t1,0
     b44:	0901                	addi	s2,s2,0
     b46:	0000                	unimp
     b48:	0301                	addi	t1,t1,0
     b4a:	0901                	addi	s2,s2,0
     b4c:	0004                	0x4
     b4e:	0501                	addi	a0,a0,0
     b50:	0309                	addi	t1,t1,2
     b52:	000900d7          	0x900d7
     b56:	0100                	addi	s0,sp,128
     b58:	1205                	addi	tp,tp,-31
     b5a:	0306                	slli	t1,t1,0x1
     b5c:	0900                	addi	s0,sp,144
     b5e:	0004                	0x4
     b60:	0501                	addi	a0,a0,0
     b62:	0609                	addi	a2,a2,2
     b64:	04090303          	lb	t1,64(s2)
     b68:	0100                	addi	s0,sp,128
     b6a:	00090103          	lb	sp,0(s2)
     b6e:	0100                	addi	s0,sp,128
     b70:	1205                	addi	tp,tp,-31
     b72:	0306                	slli	t1,t1,0x1
     b74:	097f                	0x97f
     b76:	0000                	unimp
     b78:	0501                	addi	a0,a0,0
     b7a:	030c                	addi	a1,sp,384
     b7c:	0901                	addi	s2,s2,0
     b7e:	0004                	0x4
     b80:	0501                	addi	a0,a0,0
     b82:	0009                	c.nop	2
     b84:	0402                	c.slli64	s0
     b86:	0602                	c.slli64	a2
     b88:	04090603          	lb	a2,64(s2)
     b8c:	0100                	addi	s0,sp,128
     b8e:	0200                	addi	s0,sp,256
     b90:	0204                	addi	s1,sp,256
     b92:	04090103          	lb	sp,64(s2)
     b96:	0100                	addi	s0,sp,128
     b98:	0b05                	addi	s6,s6,1
     b9a:	0200                	addi	s0,sp,256
     b9c:	0204                	addi	s1,sp,256
     b9e:	097ef203          	0x97ef203
     ba2:	0000                	unimp
     ba4:	0501                	addi	a0,a0,0
     ba6:	0009                	c.nop	2
     ba8:	0402                	c.slli64	s0
     baa:	0602                	c.slli64	a2
     bac:	09018d03          	lb	s10,144(gp)
     bb0:	0000                	unimp
     bb2:	0501                	addi	a0,a0,0
     bb4:	000c                	0xc
     bb6:	0402                	c.slli64	s0
     bb8:	0302                	c.slli64	t1
     bba:	04097ef3          	csrrci	t4,uscratch,18
     bbe:	0100                	addi	s0,sp,128
     bc0:	0d05                	addi	s10,s10,1
     bc2:	0200                	addi	s0,sp,256
     bc4:	0204                	addi	s1,sp,256
     bc6:	04097203          	0x4097203
     bca:	0100                	addi	s0,sp,128
     bcc:	0905                	addi	s2,s2,1
     bce:	0306                	slli	t1,t1,0x1
     bd0:	0004090f          	0x4090f
     bd4:	0501                	addi	a0,a0,0
     bd6:	0615                	addi	a2,a2,5
     bd8:	00090003          	lb	zero,0(s2)
     bdc:	0100                	addi	s0,sp,128
     bde:	0905                	addi	s2,s2,1
     be0:	0306                	slli	t1,t1,0x1
     be2:	0972                	slli	s2,s2,0x1c
     be4:	0004                	0x4
     be6:	0501                	addi	a0,a0,0
     be8:	097f030b          	0x97f030b
     bec:	0000                	unimp
     bee:	0501                	addi	a0,a0,0
     bf0:	060d                	addi	a2,a2,3
     bf2:	00090003          	lb	zero,0(s2)
     bf6:	0100                	addi	s0,sp,128
     bf8:	1b05                	addi	s6,s6,-31
     bfa:	04090003          	lb	zero,64(s2)
     bfe:	0100                	addi	s0,sp,128
     c00:	0b05                	addi	s6,s6,1
     c02:	04090003          	lb	zero,64(s2)
     c06:	0100                	addi	s0,sp,128
     c08:	0505                	addi	a0,a0,1
     c0a:	0306                	slli	t1,t1,0x1
     c0c:	0904                	addi	s1,sp,144
     c0e:	0004                	0x4
     c10:	0501                	addi	a0,a0,0
     c12:	060e                	slli	a2,a2,0x3
     c14:	00090003          	lb	zero,0(s2)
     c18:	0100                	addi	s0,sp,128
     c1a:	0505                	addi	a0,a0,1
     c1c:	0306                	slli	t1,t1,0x1
     c1e:	00040903          	lb	s2,0(s0)
     c22:	0501                	addi	a0,a0,0
     c24:	030a                	slli	t1,t1,0x2
     c26:	0900                	addi	s0,sp,144
     c28:	0000                	unimp
     c2a:	0501                	addi	a0,a0,0
     c2c:	00030617          	auipc	a2,0x30
     c30:	0009                	c.nop	2
     c32:	0100                	addi	s0,sp,128
     c34:	1e05                	addi	t3,t3,-31
     c36:	0306                	slli	t1,t1,0x1
     c38:	0900                	addi	s0,sp,144
     c3a:	0004                	0x4
     c3c:	0501                	addi	a0,a0,0
     c3e:	0620                	addi	s0,sp,776
     c40:	00090003          	lb	zero,0(s2)
     c44:	0100                	addi	s0,sp,128
     c46:	0505                	addi	a0,a0,1
     c48:	04090003          	lb	zero,64(s2)
     c4c:	0100                	addi	s0,sp,128
     c4e:	0905                	addi	s2,s2,1
     c50:	0306                	slli	t1,t1,0x1
     c52:	0901                	addi	s2,s2,0
     c54:	0004                	0x4
     c56:	0501                	addi	a0,a0,0
     c58:	097f0327          	0x97f0327
     c5c:	0000                	unimp
     c5e:	0501                	addi	a0,a0,0
     c60:	0628                	addi	a0,sp,776
     c62:	00090003          	lb	zero,0(s2)
     c66:	0100                	addi	s0,sp,128
     c68:	1e05                	addi	t3,t3,-31
     c6a:	0306                	slli	t1,t1,0x1
     c6c:	0900                	addi	s0,sp,144
     c6e:	000c                	0xc
     c70:	0501                	addi	a0,a0,0
     c72:	0620                	addi	s0,sp,776
     c74:	00090003          	lb	zero,0(s2)
     c78:	0100                	addi	s0,sp,128
     c7a:	0505                	addi	a0,a0,1
     c7c:	04090003          	lb	zero,64(s2)
     c80:	0100                	addi	s0,sp,128
     c82:	0b05                	addi	s6,s6,1
     c84:	0306                	slli	t1,t1,0x1
     c86:	00040907          	0x40907
     c8a:	0501                	addi	a0,a0,0
     c8c:	060c                	addi	a1,sp,768
     c8e:	00090003          	lb	zero,0(s2)
     c92:	0100                	addi	s0,sp,128
     c94:	0b05                	addi	s6,s6,1
     c96:	04090003          	lb	zero,64(s2)
     c9a:	0100                	addi	s0,sp,128
     c9c:	0905                	addi	s2,s2,1
     c9e:	0200                	addi	s0,sp,256
     ca0:	0204                	addi	s1,sp,256
     ca2:	0306                	slli	t1,t1,0x1
     ca4:	0409018f          	0x409018f
     ca8:	0100                	addi	s0,sp,128
     caa:	0200                	addi	s0,sp,256
     cac:	0204                	addi	s1,sp,256
     cae:	08090103          	lb	sp,128(s2)
     cb2:	0100                	addi	s0,sp,128
     cb4:	0b05                	addi	s6,s6,1
     cb6:	0200                	addi	s0,sp,256
     cb8:	0204                	addi	s1,sp,256
     cba:	097ef003          	0x97ef003
     cbe:	0000                	unimp
     cc0:	0501                	addi	a0,a0,0
     cc2:	0009                	c.nop	2
     cc4:	0402                	c.slli64	s0
     cc6:	0602                	c.slli64	a2
     cc8:	09018f03          	lb	t5,144(gp)
     ccc:	0000                	unimp
     cce:	0501                	addi	a0,a0,0
     cd0:	000c                	0xc
     cd2:	0402                	c.slli64	s0
     cd4:	0302                	c.slli64	t1
     cd6:	7ef1                	lui	t4,0xffffc
     cd8:	0409                	addi	s0,s0,2
     cda:	0100                	addi	s0,sp,128
     cdc:	0d05                	addi	s10,s10,1
     cde:	0200                	addi	s0,sp,256
     ce0:	0204                	addi	s1,sp,256
     ce2:	04097203          	0x4097203
     ce6:	0100                	addi	s0,sp,128
     ce8:	0905                	addi	s2,s2,1
     cea:	0306                	slli	t1,t1,0x1
     cec:	0004090f          	0x4090f
     cf0:	0501                	addi	a0,a0,0
     cf2:	0615                	addi	a2,a2,5
     cf4:	00090003          	lb	zero,0(s2)
     cf8:	0100                	addi	s0,sp,128
     cfa:	0905                	addi	s2,s2,1
     cfc:	0306                	slli	t1,t1,0x1
     cfe:	0972                	slli	s2,s2,0x1c
     d00:	0004                	0x4
     d02:	0501                	addi	a0,a0,0
     d04:	097f030b          	0x97f030b
     d08:	0000                	unimp
     d0a:	0501                	addi	a0,a0,0
     d0c:	060d                	addi	a2,a2,3
     d0e:	00090003          	lb	zero,0(s2)
     d12:	0100                	addi	s0,sp,128
     d14:	1b05                	addi	s6,s6,-31
     d16:	04090003          	lb	zero,64(s2)
     d1a:	0100                	addi	s0,sp,128
     d1c:	0b05                	addi	s6,s6,1
     d1e:	04090003          	lb	zero,64(s2)
     d22:	0100                	addi	s0,sp,128
     d24:	0505                	addi	a0,a0,1
     d26:	0306                	slli	t1,t1,0x1
     d28:	0904                	addi	s1,sp,144
     d2a:	0004                	0x4
     d2c:	0501                	addi	a0,a0,0
     d2e:	060e                	slli	a2,a2,0x3
     d30:	00090003          	lb	zero,0(s2)
     d34:	0100                	addi	s0,sp,128
     d36:	0505                	addi	a0,a0,1
     d38:	0306                	slli	t1,t1,0x1
     d3a:	00040903          	lb	s2,0(s0)
     d3e:	0501                	addi	a0,a0,0
     d40:	030a                	slli	t1,t1,0x2
     d42:	0900                	addi	s0,sp,144
     d44:	0000                	unimp
     d46:	0501                	addi	a0,a0,0
     d48:	00030617          	auipc	a2,0x30
     d4c:	0009                	c.nop	2
     d4e:	0100                	addi	s0,sp,128
     d50:	1e05                	addi	t3,t3,-31
     d52:	0306                	slli	t1,t1,0x1
     d54:	0900                	addi	s0,sp,144
     d56:	0004                	0x4
     d58:	0501                	addi	a0,a0,0
     d5a:	0620                	addi	s0,sp,776
     d5c:	00090003          	lb	zero,0(s2)
     d60:	0100                	addi	s0,sp,128
     d62:	0505                	addi	a0,a0,1
     d64:	04090003          	lb	zero,64(s2)
     d68:	0100                	addi	s0,sp,128
     d6a:	0905                	addi	s2,s2,1
     d6c:	0306                	slli	t1,t1,0x1
     d6e:	0901                	addi	s2,s2,0
     d70:	0004                	0x4
     d72:	0501                	addi	a0,a0,0
     d74:	097f0327          	0x97f0327
     d78:	0000                	unimp
     d7a:	0501                	addi	a0,a0,0
     d7c:	0628                	addi	a0,sp,776
     d7e:	00090003          	lb	zero,0(s2)
     d82:	0100                	addi	s0,sp,128
     d84:	1e05                	addi	t3,t3,-31
     d86:	0306                	slli	t1,t1,0x1
     d88:	0900                	addi	s0,sp,144
     d8a:	000c                	0xc
     d8c:	0501                	addi	a0,a0,0
     d8e:	0620                	addi	s0,sp,776
     d90:	00090003          	lb	zero,0(s2)
     d94:	0100                	addi	s0,sp,128
     d96:	0505                	addi	a0,a0,1
     d98:	04090003          	lb	zero,64(s2)
     d9c:	0100                	addi	s0,sp,128
     d9e:	0b05                	addi	s6,s6,1
     da0:	0306                	slli	t1,t1,0x1
     da2:	00040907          	0x40907
     da6:	0501                	addi	a0,a0,0
     da8:	060c                	addi	a1,sp,768
     daa:	00090003          	lb	zero,0(s2)
     dae:	0100                	addi	s0,sp,128
     db0:	0b05                	addi	s6,s6,1
     db2:	04090003          	lb	zero,64(s2)
     db6:	0100                	addi	s0,sp,128
     db8:	0905                	addi	s2,s2,1
     dba:	0200                	addi	s0,sp,256
     dbc:	0204                	addi	s1,sp,256
     dbe:	0306                	slli	t1,t1,0x1
     dc0:	0191                	addi	gp,gp,4
     dc2:	0409                	addi	s0,s0,2
     dc4:	0100                	addi	s0,sp,128
     dc6:	0200                	addi	s0,sp,256
     dc8:	0204                	addi	s1,sp,256
     dca:	08090103          	lb	sp,128(s2)
     dce:	0100                	addi	s0,sp,128
     dd0:	0b05                	addi	s6,s6,1
     dd2:	0200                	addi	s0,sp,256
     dd4:	0204                	addi	s1,sp,256
     dd6:	097eee03          	0x97eee03
     dda:	0000                	unimp
     ddc:	0501                	addi	a0,a0,0
     dde:	0009                	c.nop	2
     de0:	0402                	c.slli64	s0
     de2:	0602                	c.slli64	a2
     de4:	09019103          	lh	sp,144(gp)
     de8:	0000                	unimp
     dea:	0501                	addi	a0,a0,0
     dec:	000c                	0xc
     dee:	0402                	c.slli64	s0
     df0:	0302                	c.slli64	t1
     df2:	04097eef          	jal	t4,97e32 <src_buf+0x956d2>
     df6:	0100                	addi	s0,sp,128
     df8:	0d05                	addi	s10,s10,1
     dfa:	0200                	addi	s0,sp,256
     dfc:	0204                	addi	s1,sp,256
     dfe:	04097203          	0x4097203
     e02:	0100                	addi	s0,sp,128
     e04:	0905                	addi	s2,s2,1
     e06:	0306                	slli	t1,t1,0x1
     e08:	0004090f          	0x4090f
     e0c:	0501                	addi	a0,a0,0
     e0e:	0615                	addi	a2,a2,5
     e10:	00090003          	lb	zero,0(s2)
     e14:	0100                	addi	s0,sp,128
     e16:	0905                	addi	s2,s2,1
     e18:	0306                	slli	t1,t1,0x1
     e1a:	0972                	slli	s2,s2,0x1c
     e1c:	0004                	0x4
     e1e:	0501                	addi	a0,a0,0
     e20:	097f030b          	0x97f030b
     e24:	0000                	unimp
     e26:	0501                	addi	a0,a0,0
     e28:	060d                	addi	a2,a2,3
     e2a:	00090003          	lb	zero,0(s2)
     e2e:	0100                	addi	s0,sp,128
     e30:	1b05                	addi	s6,s6,-31
     e32:	04090003          	lb	zero,64(s2)
     e36:	0100                	addi	s0,sp,128
     e38:	0b05                	addi	s6,s6,1
     e3a:	04090003          	lb	zero,64(s2)
     e3e:	0100                	addi	s0,sp,128
     e40:	0505                	addi	a0,a0,1
     e42:	0306                	slli	t1,t1,0x1
     e44:	0904                	addi	s1,sp,144
     e46:	0004                	0x4
     e48:	0501                	addi	a0,a0,0
     e4a:	060e                	slli	a2,a2,0x3
     e4c:	00090003          	lb	zero,0(s2)
     e50:	0100                	addi	s0,sp,128
     e52:	0505                	addi	a0,a0,1
     e54:	0306                	slli	t1,t1,0x1
     e56:	00040903          	lb	s2,0(s0)
     e5a:	0501                	addi	a0,a0,0
     e5c:	030a                	slli	t1,t1,0x2
     e5e:	0900                	addi	s0,sp,144
     e60:	0000                	unimp
     e62:	0501                	addi	a0,a0,0
     e64:	00030617          	auipc	a2,0x30
     e68:	0009                	c.nop	2
     e6a:	0100                	addi	s0,sp,128
     e6c:	1e05                	addi	t3,t3,-31
     e6e:	0306                	slli	t1,t1,0x1
     e70:	0900                	addi	s0,sp,144
     e72:	0004                	0x4
     e74:	0501                	addi	a0,a0,0
     e76:	0620                	addi	s0,sp,776
     e78:	00090003          	lb	zero,0(s2)
     e7c:	0100                	addi	s0,sp,128
     e7e:	0505                	addi	a0,a0,1
     e80:	04090003          	lb	zero,64(s2)
     e84:	0100                	addi	s0,sp,128
     e86:	0905                	addi	s2,s2,1
     e88:	0306                	slli	t1,t1,0x1
     e8a:	0901                	addi	s2,s2,0
     e8c:	0004                	0x4
     e8e:	0501                	addi	a0,a0,0
     e90:	097f0327          	0x97f0327
     e94:	0000                	unimp
     e96:	0501                	addi	a0,a0,0
     e98:	0628                	addi	a0,sp,776
     e9a:	00090003          	lb	zero,0(s2)
     e9e:	0100                	addi	s0,sp,128
     ea0:	1e05                	addi	t3,t3,-31
     ea2:	0306                	slli	t1,t1,0x1
     ea4:	0900                	addi	s0,sp,144
     ea6:	000c                	0xc
     ea8:	0501                	addi	a0,a0,0
     eaa:	0620                	addi	s0,sp,776
     eac:	00090003          	lb	zero,0(s2)
     eb0:	0100                	addi	s0,sp,128
     eb2:	0505                	addi	a0,a0,1
     eb4:	04090003          	lb	zero,64(s2)
     eb8:	0100                	addi	s0,sp,128
     eba:	0b05                	addi	s6,s6,1
     ebc:	0306                	slli	t1,t1,0x1
     ebe:	00040907          	0x40907
     ec2:	0501                	addi	a0,a0,0
     ec4:	060c                	addi	a1,sp,768
     ec6:	00090003          	lb	zero,0(s2)
     eca:	0100                	addi	s0,sp,128
     ecc:	0b05                	addi	s6,s6,1
     ece:	04090003          	lb	zero,64(s2)
     ed2:	0100                	addi	s0,sp,128
     ed4:	0e05                	addi	t3,t3,1
     ed6:	04095b03          	lhu	s6,64(s2)
     eda:	0100                	addi	s0,sp,128
     edc:	0505                	addi	a0,a0,1
     ede:	04090003          	lb	zero,64(s2)
     ee2:	0100                	addi	s0,sp,128
     ee4:	0d05                	addi	s10,s10,1
     ee6:	0306                	slli	t1,t1,0x1
     ee8:	0905                	addi	s2,s2,1
     eea:	0004                	0x4
     eec:	0501                	addi	a0,a0,0
     eee:	031e                	slli	t1,t1,0x7
     ef0:	0000097b          	0x97b
     ef4:	0501                	addi	a0,a0,0
     ef6:	061f 0003 0009      	0x90003061f
     efc:	0100                	addi	s0,sp,128
     efe:	0905                	addi	s2,s2,1
     f00:	0306                	slli	t1,t1,0x1
     f02:	0901                	addi	s2,s2,0
     f04:	0004                	0x4
     f06:	0501                	addi	a0,a0,0
     f08:	061a                	slli	a2,a2,0x6
     f0a:	00090103          	lb	sp,0(s2)
     f0e:	0100                	addi	s0,sp,128
     f10:	0d05                	addi	s10,s10,1
     f12:	04097f03          	0x4097f03
     f16:	0100                	addi	s0,sp,128
     f18:	0905                	addi	s2,s2,1
     f1a:	0306                	slli	t1,t1,0x1
     f1c:	0901                	addi	s2,s2,0
     f1e:	0004                	0x4
     f20:	0501                	addi	a0,a0,0
     f22:	0316                	slli	t1,t1,0x5
     f24:	097e                	slli	s2,s2,0x1f
     f26:	0000                	unimp
     f28:	0501                	addi	a0,a0,0
     f2a:	0620                	addi	s0,sp,776
     f2c:	00090203          	lb	tp,0(s2)
     f30:	0100                	addi	s0,sp,128
     f32:	1c05                	addi	s8,s8,-31
     f34:	04090403          	lb	s0,64(s2)
     f38:	0100                	addi	s0,sp,128
     f3a:	0505                	addi	a0,a0,1
     f3c:	04097a03          	0x4097a03
     f40:	0100                	addi	s0,sp,128
     f42:	0d05                	addi	s10,s10,1
     f44:	04090203          	lb	tp,64(s2)
     f48:	0100                	addi	s0,sp,128
     f4a:	0905                	addi	s2,s2,1
     f4c:	0306                	slli	t1,t1,0x1
     f4e:	0902                	c.slli64	s2
     f50:	0004                	0x4
     f52:	0501                	addi	a0,a0,0
     f54:	060c                	addi	a1,sp,768
     f56:	00090003          	lb	zero,0(s2)
     f5a:	0100                	addi	s0,sp,128
     f5c:	1f05                	addi	t5,t5,-31
     f5e:	04097c03          	0x4097c03
     f62:	0100                	addi	s0,sp,128
     f64:	1105                	addi	sp,sp,-31
     f66:	04090503          	lb	a0,64(s2)
     f6a:	0100                	addi	s0,sp,128
     f6c:	0d05                	addi	s10,s10,1
     f6e:	0306                	slli	t1,t1,0x1
     f70:	0901                	addi	s2,s2,0
     f72:	0004                	0x4
     f74:	0501                	addi	a0,a0,0
     f76:	061a                	slli	a2,a2,0x6
     f78:	00097c03          	0x97c03
     f7c:	0100                	addi	s0,sp,128
     f7e:	1605                	addi	a2,a2,-31
     f80:	04090403          	lb	s0,64(s2)
     f84:	0100                	addi	s0,sp,128
     f86:	0d05                	addi	s10,s10,1
     f88:	0306                	slli	t1,t1,0x1
     f8a:	097f                	0x97f
     f8c:	0004                	0x4
     f8e:	0501                	addi	a0,a0,0
     f90:	031e                	slli	t1,t1,0x7
     f92:	0000097b          	0x97b
     f96:	0501                	addi	a0,a0,0
     f98:	0309                	addi	t1,t1,2
     f9a:	0901                	addi	s2,s2,0
     f9c:	0000                	unimp
     f9e:	0501                	addi	a0,a0,0
     fa0:	060d                	addi	a2,a2,3
     fa2:	00090003          	lb	zero,0(s2)
     fa6:	0100                	addi	s0,sp,128
     fa8:	0905                	addi	s2,s2,1
     faa:	0306                	slli	t1,t1,0x1
     fac:	0901                	addi	s2,s2,0
     fae:	0004                	0x4
     fb0:	0501                	addi	a0,a0,0
     fb2:	0316                	slli	t1,t1,0x5
     fb4:	097e                	slli	s2,s2,0x1f
     fb6:	0000                	unimp
     fb8:	0501                	addi	a0,a0,0
     fba:	0620                	addi	s0,sp,776
     fbc:	00090203          	lb	tp,0(s2)
     fc0:	0100                	addi	s0,sp,128
     fc2:	1c05                	addi	s8,s8,-31
     fc4:	04090403          	lb	s0,64(s2)
     fc8:	0100                	addi	s0,sp,128
     fca:	0505                	addi	a0,a0,1
     fcc:	04097a03          	0x4097a03
     fd0:	0100                	addi	s0,sp,128
     fd2:	0306                	slli	t1,t1,0x1
     fd4:	090a                	slli	s2,s2,0x2
     fd6:	0004                	0x4
     fd8:	0501                	addi	a0,a0,0
     fda:	0309                	addi	t1,t1,2
     fdc:	0901                	addi	s2,s2,0
     fde:	0000                	unimp
     fe0:	0301                	addi	t1,t1,0
     fe2:	01b1                	addi	gp,gp,12
     fe4:	0009                	c.nop	2
     fe6:	0100                	addi	s0,sp,128
     fe8:	04090103          	lb	sp,64(s2)
     fec:	0100                	addi	s0,sp,128
     fee:	0b05                	addi	s6,s6,1
     ff0:	097ee803          	0x97ee803
     ff4:	0000                	unimp
     ff6:	0501                	addi	a0,a0,0
     ff8:	0309                	addi	t1,t1,2
     ffa:	0901                	addi	s2,s2,0
     ffc:	0000                	unimp
     ffe:	0501                	addi	a0,a0,0
    1000:	060d                	addi	a2,a2,3
    1002:	00097103          	0x97103
    1006:	0100                	addi	s0,sp,128
    1008:	0905                	addi	s2,s2,1
    100a:	0306                	slli	t1,t1,0x1
    100c:	0901                	addi	s2,s2,0
    100e:	0004                	0x4
    1010:	0501                	addi	a0,a0,0
    1012:	097f030b          	0x97f030b
    1016:	0000                	unimp
    1018:	0501                	addi	a0,a0,0
    101a:	060d                	addi	a2,a2,3
    101c:	00090003          	lb	zero,0(s2)
    1020:	0100                	addi	s0,sp,128
    1022:	1b05                	addi	s6,s6,-31
    1024:	04090003          	lb	zero,64(s2)
    1028:	0100                	addi	s0,sp,128
    102a:	0b05                	addi	s6,s6,1
    102c:	04090003          	lb	zero,64(s2)
    1030:	0100                	addi	s0,sp,128
    1032:	0505                	addi	a0,a0,1
    1034:	0306                	slli	t1,t1,0x1
    1036:	0904                	addi	s1,sp,144
    1038:	0004                	0x4
    103a:	0501                	addi	a0,a0,0
    103c:	060e                	slli	a2,a2,0x3
    103e:	00090003          	lb	zero,0(s2)
    1042:	0100                	addi	s0,sp,128
    1044:	0505                	addi	a0,a0,1
    1046:	0306                	slli	t1,t1,0x1
    1048:	00080903          	lb	s2,0(a6)
    104c:	0501                	addi	a0,a0,0
    104e:	030a                	slli	t1,t1,0x2
    1050:	0900                	addi	s0,sp,144
    1052:	0000                	unimp
    1054:	0501                	addi	a0,a0,0
    1056:	00030617          	auipc	a2,0x30
    105a:	0009                	c.nop	2
    105c:	0100                	addi	s0,sp,128
    105e:	1e05                	addi	t3,t3,-31
    1060:	0306                	slli	t1,t1,0x1
    1062:	0900                	addi	s0,sp,144
    1064:	0004                	0x4
    1066:	0501                	addi	a0,a0,0
    1068:	0620                	addi	s0,sp,776
    106a:	00090003          	lb	zero,0(s2)
    106e:	0100                	addi	s0,sp,128
    1070:	0505                	addi	a0,a0,1
    1072:	04090003          	lb	zero,64(s2)
    1076:	0100                	addi	s0,sp,128
    1078:	0905                	addi	s2,s2,1
    107a:	0306                	slli	t1,t1,0x1
    107c:	0901                	addi	s2,s2,0
    107e:	0004                	0x4
    1080:	0501                	addi	a0,a0,0
    1082:	097f0327          	0x97f0327
    1086:	0000                	unimp
    1088:	0501                	addi	a0,a0,0
    108a:	0628                	addi	a0,sp,776
    108c:	00090003          	lb	zero,0(s2)
    1090:	0100                	addi	s0,sp,128
    1092:	1e05                	addi	t3,t3,-31
    1094:	0306                	slli	t1,t1,0x1
    1096:	0900                	addi	s0,sp,144
    1098:	000c                	0xc
    109a:	0501                	addi	a0,a0,0
    109c:	0620                	addi	s0,sp,776
    109e:	00090003          	lb	zero,0(s2)
    10a2:	0100                	addi	s0,sp,128
    10a4:	0505                	addi	a0,a0,1
    10a6:	04090003          	lb	zero,64(s2)
    10aa:	0100                	addi	s0,sp,128
    10ac:	0b05                	addi	s6,s6,1
    10ae:	0306                	slli	t1,t1,0x1
    10b0:	00040907          	0x40907
    10b4:	0501                	addi	a0,a0,0
    10b6:	0309                	addi	t1,t1,2
    10b8:	0199                	addi	gp,gp,6
    10ba:	0009                	c.nop	2
    10bc:	0100                	addi	s0,sp,128
    10be:	08090103          	lb	sp,128(s2)
    10c2:	0100                	addi	s0,sp,128
    10c4:	0b05                	addi	s6,s6,1
    10c6:	097ee603          	0x97ee603
    10ca:	0000                	unimp
    10cc:	0501                	addi	a0,a0,0
    10ce:	060c                	addi	a1,sp,768
    10d0:	00090003          	lb	zero,0(s2)
    10d4:	0100                	addi	s0,sp,128
    10d6:	0905                	addi	s2,s2,1
    10d8:	09019903          	lh	s2,144(gp)
    10dc:	0004                	0x4
    10de:	0501                	addi	a0,a0,0
    10e0:	030d                	addi	t1,t1,3
    10e2:	7ed9                	lui	t4,0xffff6
    10e4:	0409                	addi	s0,s0,2
    10e6:	0100                	addi	s0,sp,128
    10e8:	0905                	addi	s2,s2,1
    10ea:	0306                	slli	t1,t1,0x1
    10ec:	0004090f          	0x4090f
    10f0:	0501                	addi	a0,a0,0
    10f2:	0615                	addi	a2,a2,5
    10f4:	00090003          	lb	zero,0(s2)
    10f8:	0100                	addi	s0,sp,128
    10fa:	0905                	addi	s2,s2,1
    10fc:	0306                	slli	t1,t1,0x1
    10fe:	0972                	slli	s2,s2,0x1c
    1100:	0004                	0x4
    1102:	0501                	addi	a0,a0,0
    1104:	097f030b          	0x97f030b
    1108:	0000                	unimp
    110a:	0501                	addi	a0,a0,0
    110c:	060d                	addi	a2,a2,3
    110e:	00090003          	lb	zero,0(s2)
    1112:	0100                	addi	s0,sp,128
    1114:	1b05                	addi	s6,s6,-31
    1116:	04090003          	lb	zero,64(s2)
    111a:	0100                	addi	s0,sp,128
    111c:	0b05                	addi	s6,s6,1
    111e:	04090003          	lb	zero,64(s2)
    1122:	0100                	addi	s0,sp,128
    1124:	0505                	addi	a0,a0,1
    1126:	0306                	slli	t1,t1,0x1
    1128:	0904                	addi	s1,sp,144
    112a:	0004                	0x4
    112c:	0501                	addi	a0,a0,0
    112e:	060e                	slli	a2,a2,0x3
    1130:	00090003          	lb	zero,0(s2)
    1134:	0100                	addi	s0,sp,128
    1136:	0505                	addi	a0,a0,1
    1138:	0306                	slli	t1,t1,0x1
    113a:	00040903          	lb	s2,0(s0)
    113e:	0501                	addi	a0,a0,0
    1140:	030a                	slli	t1,t1,0x2
    1142:	0900                	addi	s0,sp,144
    1144:	0000                	unimp
    1146:	0501                	addi	a0,a0,0
    1148:	00030617          	auipc	a2,0x30
    114c:	0009                	c.nop	2
    114e:	0100                	addi	s0,sp,128
    1150:	1e05                	addi	t3,t3,-31
    1152:	0306                	slli	t1,t1,0x1
    1154:	0900                	addi	s0,sp,144
    1156:	0004                	0x4
    1158:	0501                	addi	a0,a0,0
    115a:	0620                	addi	s0,sp,776
    115c:	00090003          	lb	zero,0(s2)
    1160:	0100                	addi	s0,sp,128
    1162:	0505                	addi	a0,a0,1
    1164:	04090003          	lb	zero,64(s2)
    1168:	0100                	addi	s0,sp,128
    116a:	0905                	addi	s2,s2,1
    116c:	0306                	slli	t1,t1,0x1
    116e:	0901                	addi	s2,s2,0
    1170:	0004                	0x4
    1172:	0501                	addi	a0,a0,0
    1174:	097f0327          	0x97f0327
    1178:	0000                	unimp
    117a:	0501                	addi	a0,a0,0
    117c:	0628                	addi	a0,sp,776
    117e:	00090003          	lb	zero,0(s2)
    1182:	0100                	addi	s0,sp,128
    1184:	1e05                	addi	t3,t3,-31
    1186:	0306                	slli	t1,t1,0x1
    1188:	0900                	addi	s0,sp,144
    118a:	000c                	0xc
    118c:	0501                	addi	a0,a0,0
    118e:	0620                	addi	s0,sp,776
    1190:	00090003          	lb	zero,0(s2)
    1194:	0100                	addi	s0,sp,128
    1196:	0505                	addi	a0,a0,1
    1198:	04090003          	lb	zero,64(s2)
    119c:	0100                	addi	s0,sp,128
    119e:	0b05                	addi	s6,s6,1
    11a0:	0306                	slli	t1,t1,0x1
    11a2:	00040907          	0x40907
    11a6:	0501                	addi	a0,a0,0
    11a8:	060c                	addi	a1,sp,768
    11aa:	00090003          	lb	zero,0(s2)
    11ae:	0100                	addi	s0,sp,128
    11b0:	0b05                	addi	s6,s6,1
    11b2:	04090003          	lb	zero,64(s2)
    11b6:	0100                	addi	s0,sp,128
    11b8:	1c05                	addi	s8,s8,-31
    11ba:	0200                	addi	s0,sp,256
    11bc:	0204                	addi	s1,sp,256
    11be:	0306                	slli	t1,t1,0x1
    11c0:	00f6                	slli	ra,ra,0x1d
    11c2:	0409                	addi	s0,s0,2
    11c4:	0100                	addi	s0,sp,128
    11c6:	1505                	addi	a0,a0,-31
    11c8:	0200                	addi	s0,sp,256
    11ca:	0204                	addi	s1,sp,256
    11cc:	00090003          	lb	zero,0(s2)
    11d0:	0100                	addi	s0,sp,128
    11d2:	0505                	addi	a0,a0,1
    11d4:	0200                	addi	s0,sp,256
    11d6:	0204                	addi	s1,sp,256
    11d8:	0306                	slli	t1,t1,0x1
    11da:	0900                	addi	s0,sp,144
    11dc:	0000                	unimp
    11de:	0601                	addi	a2,a2,0
    11e0:	0c092703          	lw	a4,192(s2)
    11e4:	0100                	addi	s0,sp,128
    11e6:	0200                	addi	s0,sp,256
    11e8:	0104                	addi	s1,sp,128
    11ea:	0c090303          	lb	t1,192(s2)
    11ee:	0100                	addi	s0,sp,128
    11f0:	0905                	addi	s2,s2,1
    11f2:	0200                	addi	s0,sp,256
    11f4:	0104                	addi	s1,sp,128
    11f6:	00090103          	lb	sp,0(s2)
    11fa:	0100                	addi	s0,sp,128
    11fc:	0b05                	addi	s6,s6,1
    11fe:	0200                	addi	s0,sp,256
    1200:	0104                	addi	s1,sp,128
    1202:	00097f03          	0x97f03
    1206:	0100                	addi	s0,sp,128
    1208:	0505                	addi	a0,a0,1
    120a:	0200                	addi	s0,sp,256
    120c:	0104                	addi	s1,sp,128
    120e:	00090003          	lb	zero,0(s2)
    1212:	0100                	addi	s0,sp,128
    1214:	0905                	addi	s2,s2,1
    1216:	0200                	addi	s0,sp,256
    1218:	0104                	addi	s1,sp,128
    121a:	00090103          	lb	sp,0(s2)
    121e:	0100                	addi	s0,sp,128
    1220:	0b05                	addi	s6,s6,1
    1222:	0200                	addi	s0,sp,256
    1224:	0104                	addi	s1,sp,128
    1226:	00097f03          	0x97f03
    122a:	0100                	addi	s0,sp,128
    122c:	0d05                	addi	s10,s10,1
    122e:	04096803          	0x4096803
    1232:	0100                	addi	s0,sp,128
    1234:	00090103          	lb	sp,0(s2)
    1238:	0100                	addi	s0,sp,128
    123a:	1505                	addi	a0,a0,-31
    123c:	0306                	slli	t1,t1,0x1
    123e:	0900                	addi	s0,sp,144
    1240:	0000                	unimp
    1242:	0501                	addi	a0,a0,0
    1244:	0605                	addi	a2,a2,1
    1246:	097ecc03          	lbu	s8,151(t4) # ffff6097 <_stack_top+0xffef6097>
    124a:	000c                	0xc
    124c:	0501                	addi	a0,a0,0
    124e:	0316                	slli	t1,t1,0x5
    1250:	0906                	slli	s2,s2,0x1
    1252:	0000                	unimp
    1254:	0501                	addi	a0,a0,0
    1256:	060e                	slli	a2,a2,0x3
    1258:	00090003          	lb	zero,0(s2)
    125c:	0100                	addi	s0,sp,128
    125e:	04097e03          	0x4097e03
    1262:	0100                	addi	s0,sp,128
    1264:	1105                	addi	sp,sp,-31
    1266:	04090703          	lb	a4,64(s2)
    126a:	0100                	addi	s0,sp,128
    126c:	0505                	addi	a0,a0,1
    126e:	04097b03          	0x4097b03
    1272:	0100                	addi	s0,sp,128
    1274:	0905                	addi	s2,s2,1
    1276:	0306                	slli	t1,t1,0x1
    1278:	0901                	addi	s2,s2,0
    127a:	0004                	0x4
    127c:	0501                	addi	a0,a0,0
    127e:	030d                	addi	t1,t1,3
    1280:	0904                	addi	s1,sp,144
    1282:	0000                	unimp
    1284:	0501                	addi	a0,a0,0
    1286:	061a                	slli	a2,a2,0x6
    1288:	00097d03          	0x97d03
    128c:	0100                	addi	s0,sp,128
    128e:	2005                	jal	12ae <main+0x26>
    1290:	04090003          	lb	zero,64(s2)
    1294:	0100                	addi	s0,sp,128
    1296:	0d05                	addi	s10,s10,1
    1298:	04097f03          	0x4097f03
    129c:	0100                	addi	s0,sp,128
    129e:	0905                	addi	s2,s2,1
    12a0:	0306                	slli	t1,t1,0x1
    12a2:	0901                	addi	s2,s2,0
    12a4:	0004                	0x4
    12a6:	0501                	addi	a0,a0,0
    12a8:	060d                	addi	a2,a2,3
    12aa:	00090003          	lb	zero,0(s2)
    12ae:	0100                	addi	s0,sp,128
    12b0:	0905                	addi	s2,s2,1
    12b2:	0306                	slli	t1,t1,0x1
    12b4:	0902                	c.slli64	s2
    12b6:	0004                	0x4
    12b8:	0501                	addi	a0,a0,0
    12ba:	061c                	addi	a5,sp,768
    12bc:	00090203          	lb	tp,0(s2)
    12c0:	0100                	addi	s0,sp,128
    12c2:	1f05                	addi	t5,t5,-31
    12c4:	04097a03          	0x4097a03
    12c8:	0100                	addi	s0,sp,128
    12ca:	0c05                	addi	s8,s8,1
    12cc:	04090403          	lb	s0,64(s2)
    12d0:	0100                	addi	s0,sp,128
    12d2:	1105                	addi	sp,sp,-31
    12d4:	04090103          	lb	sp,64(s2)
    12d8:	0100                	addi	s0,sp,128
    12da:	0d05                	addi	s10,s10,1
    12dc:	0306                	slli	t1,t1,0x1
    12de:	0901                	addi	s2,s2,0
    12e0:	0004                	0x4
    12e2:	0501                	addi	a0,a0,0
    12e4:	0616                	slli	a2,a2,0x5
    12e6:	00090003          	lb	zero,0(s2)
    12ea:	0100                	addi	s0,sp,128
    12ec:	1e05                	addi	t3,t3,-31
    12ee:	0306                	slli	t1,t1,0x1
    12f0:	097a                	slli	s2,s2,0x1e
    12f2:	0004                	0x4
    12f4:	0501                	addi	a0,a0,0
    12f6:	0316                	slli	t1,t1,0x5
    12f8:	0900                	addi	s0,sp,144
    12fa:	0000                	unimp
    12fc:	0501                	addi	a0,a0,0
    12fe:	0605                	addi	a2,a2,1
    1300:	00090003          	lb	zero,0(s2)
    1304:	0100                	addi	s0,sp,128
    1306:	1a05                	addi	s4,s4,-31
    1308:	04090203          	lb	tp,64(s2)
    130c:	0100                	addi	s0,sp,128
    130e:	0809                	addi	a6,a6,2
    1310:	0000                	unimp
    1312:	0101                	addi	sp,sp,0

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
  26:	0aac                	addi	a1,sp,344
  28:	0000                	unimp
  2a:	0004                	0x4
  2c:	0014                	0x14
  2e:	0000                	unimp
  30:	0104                	addi	s1,sp,128
  32:	000000c3          	fmadd.s	ft1,ft0,ft0,ft0,rne
  36:	f20c                	fsw	fa1,32(a2)
  38:	0000                	unimp
  3a:	0800                	addi	s0,sp,16
  3c:	0000                	unimp
  3e:	7800                	flw	fs0,48(s0)
  40:	0002                	c.slli64	zero
  42:	0000                	unimp
  44:	0000                	unimp
  46:	4400                	lw	s0,8(s0)
  48:	0000                	unimp
  4a:	0200                	addi	s0,sp,256
  4c:	0124                	addi	s1,sp,136
  4e:	0000                	unimp
  50:	0601                	addi	a2,a2,0
  52:	00003617          	auipc	a2,0x3
  56:	0300                	addi	s0,sp,384
  58:	0025                	c.nop	9
  5a:	0000                	unimp
  5c:	0104                	addi	s1,sp,128
  5e:	7908                	flw	fa0,48(a0)
  60:	0000                	unimp
  62:	0200                	addi	s0,sp,256
  64:	019f 0000 0701      	0x7010000019f
  6a:	4916                	lw	s2,68(sp)
  6c:	0000                	unimp
  6e:	0400                	addi	s0,sp,512
  70:	0704                	addi	s1,sp,896
  72:	00000117          	auipc	sp,0x0
  76:	6702                	flw	fa4,0(sp)
  78:	0000                	unimp
  7a:	0100                	addi	s0,sp,128
  7c:	1c08                	addi	a0,sp,560
  7e:	005c                	addi	a5,sp,4
  80:	0000                	unimp
  82:	0804                	addi	s1,sp,16
  84:	00010d07          	0x10d07
  88:	0500                	addi	s0,sp,640
  8a:	1010                	addi	a2,sp,32
  8c:	6e01                	0x6e01
  8e:	a209                	j	190 <_start-0xe70>
  90:	0000                	unimp
  92:	0600                	addi	s0,sp,768
  94:	01b2                	slli	gp,gp,0xc
  96:	0000                	unimp
  98:	6f01                	0x6f01
  9a:	3d0e                	fld	fs10,224(sp)
  9c:	0000                	unimp
  9e:	0000                	unimp
  a0:	7006                	flw	ft0,96(sp)
  a2:	0000                	unimp
  a4:	0100                	addi	s0,sp,128
  a6:	0e70                	addi	a2,sp,796
  a8:	003d                	c.nop	15
  aa:	0000                	unimp
  ac:	0604                	addi	s1,sp,768
  ae:	00a9                	addi	ra,ra,10
  b0:	0000                	unimp
  b2:	7101                	addi	sp,sp,-512
  b4:	3d0e                	fld	fs10,224(sp)
  b6:	0000                	unimp
  b8:	0800                	addi	s0,sp,16
  ba:	e006                	fsw	ft1,0(sp)
  bc:	0001                	nop
  be:	0100                	addi	s0,sp,128
  c0:	0e72                	slli	t3,t3,0x1c
  c2:	003d                	c.nop	15
  c4:	0000                	unimp
  c6:	000c                	0xc
  c8:	0001c507          	flq	fa0,0(gp)
  cc:	0100                	addi	s0,sp,128
  ce:	00632073          	csrs	0x6,t1
  d2:	0000                	unimp
  d4:	0310                	addi	a2,sp,384
  d6:	00a2                	slli	ra,ra,0x8
  d8:	0000                	unimp
  da:	2508                	fld	fa0,8(a0)
  dc:	0000                	unimp
  de:	c500                	sw	s0,8(a0)
  e0:	0000                	unimp
  e2:	0900                	addi	s0,sp,144
  e4:	0049                	c.nop	18
  e6:	0000                	unimp
  e8:	0fff                	0xfff
  ea:	0a00                	addi	s0,sp,272
  ec:	0045                	c.nop	17
  ee:	0000                	unimp
  f0:	a901                	j	500 <_start-0xb00>
  f2:	b409                	j	fffffaf4 <_stack_top+0xffeffaf4>
  f4:	0000                	unimp
  f6:	1000                	addi	s0,sp,32
  f8:	0305                	addi	t1,t1,1
  fa:	2760                	fld	fs0,200(a4)
  fc:	0000                	unimp
  fe:	8f0a                	mv	t5,sp
 100:	0000                	unimp
 102:	0100                	addi	s0,sp,128
 104:	09aa                	slli	s3,s3,0xa
 106:	00b4                	addi	a3,sp,72
 108:	0000                	unimp
 10a:	0510                	addi	a2,sp,640
 10c:	00176003          	0x176003
 110:	0a00                	addi	s0,sp,272
 112:	018d                	addi	gp,gp,3
 114:	0000                	unimp
 116:	ab01                	j	626 <_start-0x9da>
 118:	af15                	j	84c <_start-0x7b4>
 11a:	0000                	unimp
 11c:	1000                	addi	s0,sp,32
 11e:	0305                	addi	t1,t1,1
 120:	1750                	addi	a2,sp,932
 122:	0000                	unimp
 124:	0001020b          	0x1020b
 128:	0100                	addi	s0,sp,128
 12a:	05b1                	addi	a1,a1,12
 12c:	0000062f          	0x62f
 130:	1288                	addi	a0,sp,352
 132:	0000                	unimp
 134:	03fc                	addi	a5,sp,460
 136:	0000                	unimp
 138:	9c01                	0x9c01
 13a:	0000062f          	0x62f
 13e:	070c                	addi	a1,sp,896
 140:	0001                	nop
 142:	0100                	addi	s0,sp,128
 144:	09c1                	addi	s3,s3,16
 146:	0000063b          	0x63b
 14a:	7fa49103          	lh	sp,2042(s1)
 14e:	780d                	lui	a6,0xfffe3
 150:	0000                	unimp
 152:	4000                	lw	s0,0(s0)
 154:	0001                	nop
 156:	0e00                	addi	s0,sp,784
 158:	b401006b          	0xb401006b
 15c:	00063617          	auipc	a2,0x63
 160:	0300                	addi	s0,sp,384
 162:	8c91                	sub	s1,s1,a2
 164:	007f                	0x7f
 166:	980d                	andi	s0,s0,-29
 168:	0000                	unimp
 16a:	5800                	lw	s0,48(s0)
 16c:	0001                	nop
 16e:	0f00                	addi	s0,sp,912
 170:	0069                	c.nop	26
 172:	bd01                	j	ffffff82 <_stack_top+0xffefff82>
 174:	2f0e                	fld	ft10,192(sp)
 176:	0006                	c.slli	zero,0x1
 178:	0000                	unimp
 17a:	0000                	unimp
 17c:	0000                	unimp
 17e:	b00d                	j	fffff9a0 <_stack_top+0xffeff9a0>
 180:	0000                	unimp
 182:	d600                	sw	s0,40(a2)
 184:	0005                	c.nop	1
 186:	0f00                	addi	s0,sp,912
 188:	0069                	c.nop	26
 18a:	c401                	beqz	s0,192 <_start-0xe6e>
 18c:	2f0e                	fld	ft10,192(sp)
 18e:	0006                	c.slli	zero,0x1
 190:	1f00                	addi	s0,sp,944
 192:	0000                	unimp
 194:	1000                	addi	s0,sp,32
 196:	00e0                	addi	s0,sp,76
 198:	0000                	unimp
 19a:	db11                	beqz	a4,ae <_start-0xf52>
 19c:	0001                	nop
 19e:	0100                	addi	s0,sp,128
 1a0:	0dc5                	addi	s11,s11,17
 1a2:	0000062f          	0x62f
 1a6:	00000033          	add	zero,zero,zero
 1aa:	6612                	flw	fa2,4(sp)
 1ac:	0001                	nop
 1ae:	0100                	addi	s0,sp,128
 1b0:	12c8                	addi	a0,sp,356
 1b2:	0050                	addi	a2,sp,4
 1b4:	0000                	unimp
 1b6:	4812                	lw	a6,4(sp)
 1b8:	0001                	nop
 1ba:	0100                	addi	s0,sp,128
 1bc:	12ca                	slli	t0,t0,0x32
 1be:	0050                	addi	a2,sp,4
 1c0:	0000                	unimp
 1c2:	4d0c                	lw	a1,24(a0)
 1c4:	0000                	unimp
 1c6:	0100                	addi	s0,sp,128
 1c8:	003d12cb          	fnmsub.s	ft5,fs10,ft3,ft0,rtz
 1cc:	0000                	unimp
 1ce:	6501                	0x6501
 1d0:	3e12                	fld	ft8,288(sp)
 1d2:	0001                	nop
 1d4:	0100                	addi	s0,sp,128
 1d6:	12ce                	slli	t0,t0,0x33
 1d8:	0050                	addi	a2,sp,4
 1da:	0000                	unimp
 1dc:	8712                	mv	a4,tp
 1de:	0000                	unimp
 1e0:	0100                	addi	s0,sp,128
 1e2:	12d0                	addi	a2,sp,356
 1e4:	0050                	addi	a2,sp,4
 1e6:	0000                	unimp
 1e8:	970c                	0x970c
 1ea:	0000                	unimp
 1ec:	0100                	addi	s0,sp,128
 1ee:	12d1                	addi	t0,t0,-12
 1f0:	003d                	c.nop	15
 1f2:	0000                	unimp
 1f4:	6601                	0x6601
 1f6:	3811                	jal	fffffa0a <_stack_top+0xffeffa0a>
 1f8:	0001                	nop
 1fa:	0100                	addi	s0,sp,128
 1fc:	12d4                	addi	a3,sp,356
 1fe:	003d                	c.nop	15
 200:	0000                	unimp
 202:	005d                	c.nop	23
 204:	0000                	unimp
 206:	3f11                	jal	11a <_start-0xee6>
 208:	0000                	unimp
 20a:	0100                	addi	s0,sp,128
 20c:	12e2                	slli	t0,t0,0x38
 20e:	003d                	c.nop	15
 210:	0000                	unimp
 212:	0088                	addi	a0,sp,64
 214:	0000                	unimp
 216:	bc11                	j	fffffc2a <_stack_top+0xffeffc2a>
 218:	0000                	unimp
 21a:	0100                	addi	s0,sp,128
 21c:	19e2                	slli	s3,s3,0x38
 21e:	003d                	c.nop	15
 220:	0000                	unimp
 222:	0000009b          	0x9b
 226:	00163813          	seqz	a6,a2
 22a:	4400                	lw	s0,8(s0)
 22c:	0000                	unimp
 22e:	9f00                	0x9f00
 230:	0002                	c.slli64	zero
 232:	1200                	addi	s0,sp,288
 234:	015c                	addi	a5,sp,132
 236:	0000                	unimp
 238:	d601                	beqz	a2,140 <_start-0xec0>
 23a:	3d16                	fld	fs10,352(sp)
 23c:	0000                	unimp
 23e:	1400                	addi	s0,sp,544
 240:	00000797          	auipc	a5,0x0
 244:	1644                	addi	s1,sp,804
 246:	0000                	unimp
 248:	0038                	addi	a4,sp,8
 24a:	0000                	unimp
 24c:	d701                	beqz	a4,154 <_start-0xeac>
 24e:	1515                	addi	a0,a0,-27
 250:	07c0                	addi	s0,sp,964
 252:	0000                	unimp
 254:	f206                	fsw	ft1,36(sp)
 256:	00000233          	add	tp,zero,zero
 25a:	1500                	addi	s0,sp,672
 25c:	07b4                	addi	a3,sp,968
 25e:	0000                	unimp
 260:	6601                	0x6601
 262:	a815                	j	296 <_start-0xd6a>
 264:	01000007          	0x1000007
 268:	166d                	addi	a2,a2,-5
 26a:	07cc                	addi	a1,sp,964
 26c:	0000                	unimp
 26e:	d816                	sw	t0,48(sp)
 270:	14000007          	0x14000007
 274:	00000797          	auipc	a5,0x0
 278:	1644                	addi	s1,sp,804
 27a:	0000                	unimp
 27c:	0038                	addi	a4,sp,8
 27e:	0000                	unimp
 280:	2101                	jal	680 <_start-0x980>
 282:	150a                	slli	a0,a0,0x22
 284:	07c0                	addi	s0,sp,964
 286:	0000                	unimp
 288:	f206                	fsw	ft1,36(sp)
 28a:	00000233          	add	tp,zero,zero
 28e:	1500                	addi	s0,sp,672
 290:	07b4                	addi	a3,sp,968
 292:	0000                	unimp
 294:	6601                	0x6601
 296:	a815                	j	2ca <_start-0xd36>
 298:	01000007          	0x1000007
 29c:	176d                	addi	a4,a4,-5
 29e:	07cc                	addi	a1,sp,964
 2a0:	0000                	unimp
 2a2:	00ae                	slli	ra,ra,0xb
 2a4:	0000                	unimp
 2a6:	0007d817          	auipc	a6,0x7d
 2aa:	cd00                	sw	s0,24(a0)
 2ac:	0000                	unimp
 2ae:	1800                	addi	s0,sp,48
 2b0:	07e4                	addi	s1,sp,972
 2b2:	0000                	unimp
 2b4:	0260                	addi	s0,sp,268
 2b6:	0000                	unimp
 2b8:	0007e517          	auipc	a0,0x7e
 2bc:	ec00                	fsw	fs0,24(s0)
 2be:	0000                	unimp
 2c0:	0000                	unimp
 2c2:	0000                	unimp
 2c4:	1900                	addi	s0,sp,176
 2c6:	0000064b          	fnmsub.s	fa2,ft0,ft0,ft0,rne
 2ca:	137c                	addi	a5,sp,428
 2cc:	0000                	unimp
 2ce:	0110                	addi	a2,sp,128
 2d0:	0000                	unimp
 2d2:	c901                	beqz	a0,2e2 <_start-0xd1e>
 2d4:	e709                	bnez	a4,2de <_start-0xd22>
 2d6:	0002                	c.slli64	zero
 2d8:	1a00                	addi	s0,sp,304
 2da:	0670                	addi	a2,sp,780
 2dc:	0000                	unimp
 2de:	00000123          	sb	zero,2(zero) # 2 <_start-0xffe>
 2e2:	641a                	flw	fs0,132(sp)
 2e4:	0006                	c.slli	zero,0x1
 2e6:	7d00                	flw	fs0,56(a0)
 2e8:	0001                	nop
 2ea:	1a00                	addi	s0,sp,304
 2ec:	0658                	addi	a4,sp,772
 2ee:	0000                	unimp
 2f0:	0195                	addi	gp,gp,5
 2f2:	0000                	unimp
 2f4:	1010                	addi	a2,sp,32
 2f6:	0001                	nop
 2f8:	1700                	addi	s0,sp,928
 2fa:	067c                	addi	a5,sp,780
 2fc:	0000                	unimp
 2fe:	01ad                	addi	gp,gp,11
 300:	0000                	unimp
 302:	00068617          	auipc	a2,0x68
 306:	e800                	fsw	fs0,16(s0)
 308:	0001                	nop
 30a:	0000                	unimp
 30c:	1900                	addi	s0,sp,176
 30e:	0699                	addi	a3,a3,6
 310:	0000                	unimp
 312:	1380                	addi	s0,sp,480
 314:	0000                	unimp
 316:	0138                	addi	a4,sp,136
 318:	0000                	unimp
 31a:	cf01                	beqz	a4,332 <_start-0xcce>
 31c:	2009                	jal	31e <_start-0xce2>
 31e:	1a000003          	lb	zero,416(zero) # 1a0 <_start-0xe60>
 322:	06ca                	slli	a3,a3,0x12
 324:	0000                	unimp
 326:	0000020b          	0x20b
 32a:	be1a                	fsd	ft6,312(sp)
 32c:	0006                	c.slli	zero,0x1
 32e:	2300                	fld	fs0,0(a4)
 330:	0002                	c.slli64	zero
 332:	1a00                	addi	s0,sp,304
 334:	06b2                	slli	a3,a3,0xc
 336:	0000                	unimp
 338:	0236                	slli	tp,tp,0xd
 33a:	0000                	unimp
 33c:	a61a                	fsd	ft6,264(sp)
 33e:	0006                	c.slli	zero,0x1
 340:	4e00                	lw	s0,24(a2)
 342:	0002                	c.slli64	zero
 344:	0000                	unimp
 346:	5519                	li	a0,-26
 348:	c4000007          	0xc4000007
 34c:	60000013          	li	zero,1536
 350:	0001                	nop
 352:	0100                	addi	s0,sp,128
 354:	09dc                	addi	a5,sp,212
 356:	0000036b          	0x36b
 35a:	621a                	flw	ft4,132(sp)
 35c:	66000007          	0x66000007
 360:	0002                	c.slli64	zero
 362:	1b00                	addi	s0,sp,432
 364:	00000773          	0x773
 368:	13c8                	addi	a0,sp,484
 36a:	0000                	unimp
 36c:	0180                	addi	s0,sp,192
 36e:	0000                	unimp
 370:	4f01                	li	t5,0
 372:	1a09                	addi	s4,s4,-30
 374:	0780                	addi	s0,sp,960
 376:	0000                	unimp
 378:	0289                	addi	t0,t0,2
 37a:	0000                	unimp
 37c:	8a18                	0x8a18
 37e:	a0000007          	0xa0000007
 382:	0001                	nop
 384:	1c00                	addi	s0,sp,560
 386:	0000078b          	0x78b
 38a:	7f909103          	lh	sp,2041(ra)
 38e:	0000                	unimp
 390:	1d00                	addi	s0,sp,688
 392:	06dd                	addi	a3,a3,23
 394:	0000                	unimp
 396:	13cc                	addi	a1,sp,484
 398:	0000                	unimp
 39a:	0004                	0x4
 39c:	0000                	unimp
 39e:	c801                	beqz	s0,3ae <_start-0xc52>
 3a0:	851e                	mv	a0,t2
 3a2:	16000003          	lb	zero,352(zero) # 160 <_start-0xea0>
 3a6:	06ee                	slli	a3,a3,0x1b
 3a8:	0000                	unimp
 3aa:	1d00                	addi	s0,sp,688
 3ac:	06dd                	addi	a3,a3,23
 3ae:	0000                	unimp
 3b0:	140c                	addi	a1,sp,544
 3b2:	0000                	unimp
 3b4:	0004                	0x4
 3b6:	0000                	unimp
 3b8:	ca01                	beqz	a2,3c8 <_start-0xc38>
 3ba:	9f1c                	0x9f1c
 3bc:	16000003          	lb	zero,352(zero) # 160 <_start-0xea0>
 3c0:	06ee                	slli	a3,a3,0x1b
 3c2:	0000                	unimp
 3c4:	1d00                	addi	s0,sp,688
 3c6:	06dd                	addi	a3,a3,23
 3c8:	0000                	unimp
 3ca:	1414                	addi	a3,sp,544
 3cc:	0000                	unimp
 3ce:	0004                	0x4
 3d0:	0000                	unimp
 3d2:	ce01                	beqz	a2,3ea <_start-0xc16>
 3d4:	b91e                	fsd	ft7,176(sp)
 3d6:	16000003          	lb	zero,352(zero) # 160 <_start-0xea0>
 3da:	06ee                	slli	a3,a3,0x1b
 3dc:	0000                	unimp
 3de:	1d00                	addi	s0,sp,688
 3e0:	06dd                	addi	a3,a3,23
 3e2:	0000                	unimp
 3e4:	1448                	addi	a0,sp,548
 3e6:	0000                	unimp
 3e8:	0004                	0x4
 3ea:	0000                	unimp
 3ec:	d001                	beqz	s0,2ec <_start-0xd14>
 3ee:	d31c                	sw	a5,32(a4)
 3f0:	16000003          	lb	zero,352(zero) # 160 <_start-0xea0>
 3f4:	06ee                	slli	a3,a3,0x1b
 3f6:	0000                	unimp
 3f8:	1900                	addi	s0,sp,176
 3fa:	0755                	addi	a4,a4,21
 3fc:	0000                	unimp
 3fe:	14b0                	addi	a2,sp,616
 400:	0000                	unimp
 402:	01b8                	addi	a4,sp,200
 404:	0000                	unimp
 406:	de01                	beqz	a2,31e <_start-0xce2>
 408:	2209                	jal	50a <_start-0xaf6>
 40a:	0004                	0x4
 40c:	1a00                	addi	s0,sp,304
 40e:	0762                	slli	a4,a4,0x18
 410:	0000                	unimp
 412:	029c                	addi	a5,sp,320
 414:	0000                	unimp
 416:	0007731b          	0x7731b
 41a:	b800                	fsd	fs0,48(s0)
 41c:	0014                	0x14
 41e:	d000                	sw	s0,32(s0)
 420:	0001                	nop
 422:	0100                	addi	s0,sp,128
 424:	801a094f          	fnmadd.s	fs2,fs4,ft1,fa6,rne
 428:	bf000007          	0xbf000007
 42c:	0002                	c.slli64	zero
 42e:	1e00                	addi	s0,sp,816
 430:	078a                	slli	a5,a5,0x2
 432:	0000                	unimp
 434:	14d0                	addi	a2,sp,612
 436:	0000                	unimp
 438:	0020                	addi	s0,sp,8
 43a:	0000                	unimp
 43c:	8b1c                	0x8b1c
 43e:	03000007          	0x3000007
 442:	9491                	srai	s1,s1,0x24
 444:	007f                	0x7f
 446:	0000                	unimp
 448:	5519                	li	a0,-26
 44a:	00000007          	0x7
 44e:	0015                	c.nop	5
 450:	e800                	fsw	fs0,16(s0)
 452:	0001                	nop
 454:	0100                	addi	s0,sp,128
 456:	09e0                	addi	s0,sp,220
 458:	0471                	addi	s0,s0,28
 45a:	0000                	unimp
 45c:	621a                	flw	ft4,132(sp)
 45e:	d2000007          	0xd2000007
 462:	0002                	c.slli64	zero
 464:	1b00                	addi	s0,sp,432
 466:	00000773          	0x773
 46a:	1508                	addi	a0,sp,672
 46c:	0000                	unimp
 46e:	0200                	addi	s0,sp,256
 470:	0000                	unimp
 472:	4f01                	li	t5,0
 474:	1a09                	addi	s4,s4,-30
 476:	0780                	addi	s0,sp,960
 478:	0000                	unimp
 47a:	02f5                	addi	t0,t0,29
 47c:	0000                	unimp
 47e:	8a1e                	mv	s4,t2
 480:	20000007          	0x20000007
 484:	0015                	c.nop	5
 486:	2000                	fld	fs0,0(s0)
 488:	0000                	unimp
 48a:	1c00                	addi	s0,sp,560
 48c:	0000078b          	0x78b
 490:	7f989103          	lh	sp,2041(a7)
 494:	0000                	unimp
 496:	1900                	addi	s0,sp,176
 498:	00000797          	auipc	a5,0x0
 49c:	1548                	addi	a0,sp,676
 49e:	0000                	unimp
 4a0:	0218                	addi	a4,sp,256
 4a2:	0000                	unimp
 4a4:	e301                	bnez	a4,4a4 <_start-0xb5c>
 4a6:	f011                	bnez	s0,3aa <_start-0xc56>
 4a8:	0004                	0x4
 4aa:	1f00                	addi	s0,sp,944
 4ac:	07c0                	addi	s0,sp,964
 4ae:	0000                	unimp
 4b0:	b41f 0007 1f00      	0x1f000007b41f
 4b6:	07a8                	addi	a0,sp,968
 4b8:	0000                	unimp
 4ba:	1810                	addi	a2,sp,48
 4bc:	0002                	c.slli64	zero
 4be:	1600                	addi	s0,sp,800
 4c0:	07cc                	addi	a1,sp,964
 4c2:	0000                	unimp
 4c4:	d816                	sw	t0,48(sp)
 4c6:	1b000007          	0x1b000007
 4ca:	00000797          	auipc	a5,0x0
 4ce:	1548                	addi	a0,sp,676
 4d0:	0000                	unimp
 4d2:	0218                	addi	a4,sp,256
 4d4:	0000                	unimp
 4d6:	2101                	jal	8d6 <_start-0x72a>
 4d8:	1f0a                	slli	t5,t5,0x22
 4da:	07c0                	addi	s0,sp,964
 4dc:	0000                	unimp
 4de:	b41f 0007 1f00      	0x1f000007b41f
 4e4:	07a8                	addi	a0,sp,968
 4e6:	0000                	unimp
 4e8:	1810                	addi	a2,sp,48
 4ea:	0002                	c.slli64	zero
 4ec:	1700                	addi	s0,sp,928
 4ee:	07cc                	addi	a1,sp,964
 4f0:	0000                	unimp
 4f2:	0308                	addi	a0,sp,384
 4f4:	0000                	unimp
 4f6:	0007d817          	auipc	a6,0x7d
 4fa:	1b00                	addi	s0,sp,432
 4fc:	18000003          	lb	zero,384(zero) # 180 <_start-0xe80>
 500:	07e4                	addi	s1,sp,972
 502:	0000                	unimp
 504:	0218                	addi	a4,sp,256
 506:	0000                	unimp
 508:	0007e517          	auipc	a0,0x7e
 50c:	2e00                	fld	fs0,24(a2)
 50e:	00000003          	lb	zero,0(zero) # 0 <_start-0x1000>
 512:	0000                	unimp
 514:	0000                	unimp
 516:	551d                	li	a0,-25
 518:	94000007          	0x94000007
 51c:	0015                	c.nop	5
 51e:	3800                	fld	fs0,48(s0)
 520:	0000                	unimp
 522:	0100                	addi	s0,sp,128
 524:	09e6                	slli	s3,s3,0x19
 526:	0000053b          	0x53b
 52a:	621f 0007 1400      	0x14000007621f
 530:	00000773          	0x773
 534:	1594                	addi	a3,sp,736
 536:	0000                	unimp
 538:	0038                	addi	a4,sp,8
 53a:	0000                	unimp
 53c:	4f01                	li	t5,0
 53e:	1a09                	addi	s4,s4,-30
 540:	0780                	addi	s0,sp,960
 542:	0000                	unimp
 544:	0359                	addi	t1,t1,22
 546:	0000                	unimp
 548:	8a1e                	mv	s4,t2
 54a:	ac000007          	0xac000007
 54e:	0015                	c.nop	5
 550:	2000                	fld	fs0,0(s0)
 552:	0000                	unimp
 554:	1c00                	addi	s0,sp,560
 556:	0000078b          	0x78b
 55a:	7f9c9103          	lh	sp,2041(s9)
 55e:	0000                	unimp
 560:	1900                	addi	s0,sp,176
 562:	0755                	addi	a4,a4,21
 564:	0000                	unimp
 566:	15d4                	addi	a3,sp,740
 568:	0000                	unimp
 56a:	0230                	addi	a2,sp,264
 56c:	0000                	unimp
 56e:	e801                	bnez	s0,57e <_start-0xa82>
 570:	8a09                	andi	a2,a2,2
 572:	0005                	c.nop	1
 574:	1a00                	addi	s0,sp,304
 576:	0762                	slli	a4,a4,0x18
 578:	0000                	unimp
 57a:	036e                	slli	t1,t1,0x1b
 57c:	0000                	unimp
 57e:	0007731b          	0x7731b
 582:	dc00                	sw	s0,56(s0)
 584:	0015                	c.nop	5
 586:	4800                	lw	s0,16(s0)
 588:	0002                	c.slli64	zero
 58a:	0100                	addi	s0,sp,128
 58c:	801a094f          	fnmadd.s	fs2,fs4,ft1,fa6,rne
 590:	91000007          	0x91000007
 594:	1e000003          	lb	zero,480(zero) # 1e0 <_start-0xe20>
 598:	078a                	slli	a5,a5,0x2
 59a:	0000                	unimp
 59c:	15f4                	addi	a3,sp,748
 59e:	0000                	unimp
 5a0:	0020                	addi	s0,sp,8
 5a2:	0000                	unimp
 5a4:	8b1c                	0x8b1c
 5a6:	03000007          	0x3000007
 5aa:	a091                	j	5ee <_start-0xa12>
 5ac:	007f                	0x7f
 5ae:	0000                	unimp
 5b0:	6020                	flw	fs0,64(s0)
 5b2:	0014                	0x14
 5b4:	fb00                	fsw	fs0,48(a4)
 5b6:	0006                	c.slli	zero,0x1
 5b8:	2100                	fld	fs0,0(a0)
 5ba:	14b0                	addi	a2,sp,616
 5bc:	0000                	unimp
 5be:	000006fb          	0x6fb
 5c2:	000005a7          	0x5a7
 5c6:	0122                	slli	sp,sp,0x8
 5c8:	025a                	slli	tp,tp,0x16
 5ca:	0085                	addi	ra,ra,1
 5cc:	2100                	fld	fs0,0(a0)
 5ce:	1500                	addi	s0,sp,672
 5d0:	0000                	unimp
 5d2:	000006fb          	0x6fb
 5d6:	000005bb          	0x5bb
 5da:	0122                	slli	sp,sp,0x8
 5dc:	025a                	slli	tp,tp,0x16
 5de:	0086                	slli	ra,ra,0x1
 5e0:	2000                	fld	fs0,0(s0)
 5e2:	1594                	addi	a3,sp,736
 5e4:	0000                	unimp
 5e6:	000006fb          	0x6fb
 5ea:	0015d423          	0x15d423
 5ee:	fb00                	fsw	fs0,48(a4)
 5f0:	0006                	c.slli	zero,0x1
 5f2:	2200                	fld	fs0,0(a2)
 5f4:	5a01                	li	s4,-32
 5f6:	8802                	jr	a6
 5f8:	0000                	unimp
 5fa:	0000                	unimp
 5fc:	f421                	bnez	s0,544 <_start-0xabc>
 5fe:	0012                	c.slli	zero,0x4
 600:	5500                	lw	s0,40(a0)
 602:	ed000007          	0xed000007
 606:	0005                	c.nop	1
 608:	2200                	fld	fs0,0(a2)
 60a:	5a01                	li	s4,-32
 60c:	0305                	addi	t1,t1,1
 60e:	169c                	addi	a5,sp,864
 610:	0000                	unimp
 612:	2100                	fld	fs0,0(a0)
 614:	1300                	addi	s0,sp,416
 616:	0000                	unimp
 618:	0755                	addi	a4,a4,21
 61a:	0000                	unimp
 61c:	0604                	addi	s1,sp,768
 61e:	0000                	unimp
 620:	0122                	slli	sp,sp,0x8
 622:	055a                	slli	a0,a0,0x16
 624:	0016c003          	lbu	zero,1(a3) # 1001 <_start+0x1>
 628:	0000                	unimp
 62a:	0c21                	addi	s8,s8,8
 62c:	55000013          	li	zero,1360
 630:	1b000007          	0x1b000007
 634:	0006                	c.slli	zero,0x1
 636:	2200                	fld	fs0,0(a2)
 638:	5a01                	li	s4,-32
 63a:	0305                	addi	t1,t1,1
 63c:	16e8                	addi	a0,sp,876
 63e:	0000                	unimp
 640:	2300                	fld	fs0,0(a4)
 642:	1634                	addi	a3,sp,808
 644:	0000                	unimp
 646:	0755                	addi	a4,a4,21
 648:	0000                	unimp
 64a:	0122                	slli	sp,sp,0x8
 64c:	055a                	slli	a0,a0,0x16
 64e:	00171003          	lh	zero,1(a4)
 652:	0000                	unimp
 654:	2400                	fld	fs0,8(s0)
 656:	0504                	addi	s1,sp,640
 658:	6e69                	lui	t3,0x1a
 65a:	0074                	addi	a3,sp,12
 65c:	00062f03          	lw	t5,0(a2) # 68302 <src_buf+0x65ba2>
 660:	0800                	addi	s0,sp,16
 662:	0000062f          	0x62f
 666:	0000064b          	fnmsub.s	fa2,ft0,ft0,ft0,rne
 66a:	4925                	li	s2,9
 66c:	0000                	unimp
 66e:	0600                	addi	s0,sp,768
 670:	2600                	fld	fs0,8(a2)
 672:	01d0                	addi	a2,sp,196
 674:	0000                	unimp
 676:	9c01                	0x9c01
 678:	0106                	slli	sp,sp,0x1
 67a:	0691                	addi	a3,a3,4
 67c:	0000                	unimp
 67e:	74736427          	0x74736427
 682:	0100                	addi	s0,sp,128
 684:	179c                	addi	a5,sp,992
 686:	0691                	addi	a3,a3,4
 688:	0000                	unimp
 68a:	63727327          	0x63727327
 68e:	0100                	addi	s0,sp,128
 690:	229c                	fld	fa5,0(a3)
 692:	0691                	addi	a3,a3,4
 694:	0000                	unimp
 696:	6e656c27          	0x6e656c27
 69a:	0100                	addi	s0,sp,128
 69c:	309c                	fld	fa5,32(s1)
 69e:	003d                	c.nop	15
 6a0:	0000                	unimp
 6a2:	6428                	flw	fa0,72(s0)
 6a4:	0100                	addi	s0,sp,128
 6a6:	179e                	slli	a5,a5,0x27
 6a8:	00000693          	li	a3,0
 6ac:	7328                	flw	fa0,96(a4)
 6ae:	0100                	addi	s0,sp,128
 6b0:	179f 0693 0000      	0x693179f
 6b6:	2900                	fld	fs0,16(a0)
 6b8:	2a04                	fld	fs1,16(a2)
 6ba:	3104                	fld	fs1,32(a0)
 6bc:	0000                	unimp
 6be:	2600                	fld	fs0,8(a2)
 6c0:	0179                	addi	sp,sp,30
 6c2:	0000                	unimp
 6c4:	7e01                	lui	t3,0xfffe0
 6c6:	0106                	slli	sp,sp,0x1
 6c8:	000006d7          	0x6d7
 6cc:	74736427          	0x74736427
 6d0:	0100                	addi	s0,sp,128
 6d2:	197e                	slli	s2,s2,0x3f
 6d4:	0691                	addi	a3,a3,4
 6d6:	0000                	unimp
 6d8:	63727327          	0x63727327
 6dc:	0100                	addi	s0,sp,128
 6de:	197f                	0x197f
 6e0:	0691                	addi	a3,a3,4
 6e2:	0000                	unimp
 6e4:	6e656c27          	0x6e656c27
 6e8:	0100                	addi	s0,sp,128
 6ea:	1c80                	addi	s0,sp,624
 6ec:	003d                	c.nop	15
 6ee:	0000                	unimp
 6f0:	0000562b          	0x562b
 6f4:	0100                	addi	s0,sp,128
 6f6:	2881                	jal	746 <_start-0x8ba>
 6f8:	000006d7          	0x6d7
 6fc:	2a00                	fld	fs0,16(a2)
 6fe:	af04                	fsd	fs1,24(a4)
 700:	0000                	unimp
 702:	2c00                	fld	fs0,24(s0)
 704:	012c                	addi	a1,sp,136
 706:	0000                	unimp
 708:	7601                	lui	a2,0xfffe0
 70a:	500a                	0x500a
 70c:	0000                	unimp
 70e:	0100                	addi	s0,sp,128
 710:	000006fb          	0x6fb
 714:	3112                	fld	ft2,288(sp)
 716:	0001                	nop
 718:	0100                	addi	s0,sp,128
 71a:	0e78                	addi	a4,sp,796
 71c:	0050                	addi	a2,sp,4
 71e:	0000                	unimp
 720:	2600                	fld	fs0,8(a2)
 722:	00b2                	slli	ra,ra,0xc
 724:	0000                	unimp
 726:	5401                	li	s0,-32
 728:	0106                	slli	sp,sp,0x1
 72a:	0739                	addi	a4,a4,14
 72c:	0000                	unimp
 72e:	6c617627          	0x6c617627
 732:	0100                	addi	s0,sp,128
 734:	1954                	addi	a3,sp,180
 736:	003d                	c.nop	15
 738:	0000                	unimp
 73a:	8612                	mv	a2,tp
 73c:	0001                	nop
 73e:	0100                	addi	s0,sp,128
 740:	0a56                	slli	s4,s4,0x15
 742:	0739                	addi	a4,a4,14
 744:	0000                	unimp
 746:	6928                	flw	fa0,80(a0)
 748:	0100                	addi	s0,sp,128
 74a:	062f0957          	0x62f0957
 74e:	0000                	unimp
 750:	282d                	jal	78a <_start-0x876>
 752:	6572                	flw	fa0,28(sp)
 754:	006d                	c.nop	27
 756:	5f01                	li	t5,-32
 758:	3d12                	fld	fs10,288(sp)
 75a:	0000                	unimp
 75c:	0000                	unimp
 75e:	0800                	addi	s0,sp,16
 760:	0749                	addi	a4,a4,18
 762:	0000                	unimp
 764:	0749                	addi	a4,a4,18
 766:	0000                	unimp
 768:	4925                	li	s2,9
 76a:	0000                	unimp
 76c:	0b00                	addi	s0,sp,400
 76e:	0400                	addi	s0,sp,512
 770:	0801                	addi	a6,a6,0
 772:	0082                	c.slli64	ra
 774:	0000                	unimp
 776:	492e                	lw	s2,200(sp)
 778:	26000007          	0x26000007
 77c:	000001bb          	0x1bb
 780:	4c01                	li	s8,0
 782:	0106                	slli	sp,sp,0x1
 784:	076d                	addi	a4,a4,27
 786:	0000                	unimp
 788:	01007327          	0x1007327
 78c:	1c4c                	addi	a1,sp,564
 78e:	076d                	addi	a4,a4,27
 790:	0000                	unimp
 792:	2a00                	fld	fs0,16(a2)
 794:	5004                	lw	s1,32(s0)
 796:	26000007          	0x26000007
 79a:	0195                	addi	gp,gp,5
 79c:	0000                	unimp
 79e:	3d01                	jal	5ae <_start-0xa52>
 7a0:	0106                	slli	sp,sp,0x1
 7a2:	00000797          	auipc	a5,0x0
 7a6:	01006327          	0x1006327
 7aa:	153d                	addi	a0,a0,-17
 7ac:	0749                	addi	a4,a4,18
 7ae:	0000                	unimp
 7b0:	282d                	jal	7ea <_start-0x816>
 7b2:	0069                	c.nop	26
 7b4:	4701                	li	a4,0
 7b6:	00063617          	auipc	a2,0x63
 7ba:	0000                	unimp
 7bc:	2f00                	fld	fs0,24(a4)
 7be:	0150                	addi	a2,sp,132
 7c0:	0000                	unimp
 7c2:	2101                	jal	bc2 <_start-0x43e>
 7c4:	3d0a                	fld	fs10,160(sp)
 7c6:	0000                	unimp
 7c8:	0100                	addi	s0,sp,128
 7ca:	07f1                	addi	a5,a5,28
 7cc:	0000                	unimp
 7ce:	0000a02b          	0xa02b
 7d2:	0100                	addi	s0,sp,128
 7d4:	1f21                	addi	t5,t5,-24
 7d6:	003d                	c.nop	15
 7d8:	0000                	unimp
 7da:	00005f2b          	0x5f2b
 7de:	0100                	addi	s0,sp,128
 7e0:	3221                	jal	e8 <_start-0xf18>
 7e2:	003d                	c.nop	15
 7e4:	0000                	unimp
 7e6:	0001a82b          	0x1a82b
 7ea:	0100                	addi	s0,sp,128
 7ec:	4521                	li	a0,8
 7ee:	07f1                	addi	a5,a5,28
 7f0:	0000                	unimp
 7f2:	7012                	flw	ft0,36(sp)
 7f4:	0001                	nop
 7f6:	0100                	addi	s0,sp,128
 7f8:	0e26                	slli	t3,t3,0x9
 7fa:	003d                	c.nop	15
 7fc:	0000                	unimp
 7fe:	7228                	flw	fa0,96(a2)
 800:	6d65                	lui	s10,0x19
 802:	0100                	addi	s0,sp,128
 804:	003d0e27          	0x3d0e27
 808:	0000                	unimp
 80a:	282d                	jal	844 <_start-0x7bc>
 80c:	0069                	c.nop	26
 80e:	2901                	jal	c1e <_start-0x3e2>
 810:	2f0e                	fld	ft10,192(sp)
 812:	0006                	c.slli	zero,0x1
 814:	0000                	unimp
 816:	2a00                	fld	fs0,16(a2)
 818:	3d04                	fld	fs1,56(a0)
 81a:	0000                	unimp
 81c:	3000                	fld	fs0,32(s0)
 81e:	00000797          	auipc	a5,0x0
 822:	1010                	addi	a2,sp,32
 824:	0000                	unimp
 826:	0054                	addi	a3,sp,4
 828:	0000                	unimp
 82a:	9c01                	0x9c01
 82c:	0871                	addi	a6,a6,28
 82e:	0000                	unimp
 830:	a81a                	fsd	ft6,16(sp)
 832:	a4000007          	0xa4000007
 836:	15000003          	lb	zero,336(zero) # 150 <_start-0xeb0>
 83a:	07b4                	addi	a3,sp,968
 83c:	0000                	unimp
 83e:	5b01                	li	s6,-32
 840:	c015                	beqz	s0,864 <_start-0x79c>
 842:	01000007          	0x1000007
 846:	165c                	addi	a5,sp,804
 848:	07cc                	addi	a1,sp,964
 84a:	0000                	unimp
 84c:	d816                	sw	t0,48(sp)
 84e:	14000007          	0x14000007
 852:	00000797          	auipc	a5,0x0
 856:	101c                	addi	a5,sp,32
 858:	0000                	unimp
 85a:	0044                	addi	s1,sp,4
 85c:	0000                	unimp
 85e:	2101                	jal	c5e <_start-0x3a2>
 860:	1f0a                	slli	t5,t5,0x22
 862:	07c0                	addi	s0,sp,964
 864:	0000                	unimp
 866:	b41f 0007 1f00      	0x1f000007b41f
 86c:	07a8                	addi	a0,sp,968
 86e:	0000                	unimp
 870:	0007cc17          	auipc	s8,0x7c
 874:	c200                	sw	s0,0(a2)
 876:	17000003          	lb	zero,368(zero) # 170 <_start-0xe90>
 87a:	07d8                	addi	a4,sp,964
 87c:	0000                	unimp
 87e:	03d5                	addi	t2,t2,21
 880:	0000                	unimp
 882:	e418                	fsw	fa4,8(s0)
 884:	00000007          	0x7
 888:	0000                	unimp
 88a:	1700                	addi	s0,sp,928
 88c:	07e5                	addi	a5,a5,25
 88e:	0000                	unimp
 890:	03e8                	addi	a0,sp,460
 892:	0000                	unimp
 894:	0000                	unimp
 896:	3000                	fld	fs0,32(s0)
 898:	00000773          	0x773
 89c:	1064                	addi	s1,sp,44
 89e:	0000                	unimp
 8a0:	0048                	addi	a0,sp,4
 8a2:	0000                	unimp
 8a4:	9c01                	0x9c01
 8a6:	08a2                	slli	a7,a7,0x8
 8a8:	0000                	unimp
 8aa:	8015                	srli	s0,s0,0x5
 8ac:	01000007          	0x1000007
 8b0:	1e5a                	slli	t3,t3,0x36
 8b2:	078a                	slli	a5,a5,0x2
 8b4:	0000                	unimp
 8b6:	107c                	addi	a5,sp,44
 8b8:	0000                	unimp
 8ba:	0028                	addi	a0,sp,8
 8bc:	0000                	unimp
 8be:	8b1c                	0x8b1c
 8c0:	02000007          	0x2000007
 8c4:	7c91                	lui	s9,0xfffe4
 8c6:	0000                	unimp
 8c8:	5530                	lw	a2,104(a0)
 8ca:	ac000007          	0xac000007
 8ce:	0010                	0x10
 8d0:	5c00                	lw	s0,56(s0)
 8d2:	0000                	unimp
 8d4:	0100                	addi	s0,sp,128
 8d6:	eb9c                	fsw	fa5,16(a5)
 8d8:	0008                	0x8
 8da:	1a00                	addi	s0,sp,304
 8dc:	0762                	slli	a4,a4,0x18
 8de:	0000                	unimp
 8e0:	00000413          	li	s0,0
 8e4:	0007731b          	0x7731b
 8e8:	b800                	fsd	fs0,48(s0)
 8ea:	0010                	0x10
 8ec:	1800                	addi	s0,sp,48
 8ee:	0000                	unimp
 8f0:	0100                	addi	s0,sp,128
 8f2:	801a094f          	fnmadd.s	fs2,fs4,ft1,fa6,rne
 8f6:	31000007          	0x31000007
 8fa:	0004                	0x4
 8fc:	1800                	addi	s0,sp,48
 8fe:	078a                	slli	a5,a5,0x2
 900:	0000                	unimp
 902:	0030                	addi	a2,sp,8
 904:	0000                	unimp
 906:	8b1c                	0x8b1c
 908:	02000007          	0x2000007
 90c:	7c91                	lui	s9,0xfffe4
 90e:	0000                	unimp
 910:	3000                	fld	fs0,32(s0)
 912:	000006fb          	0x6fb
 916:	1108                	addi	a0,sp,160
 918:	0000                	unimp
 91a:	0118                	addi	a4,sp,128
 91c:	0000                	unimp
 91e:	9c01                	0x9c01
 920:	0a24                	addi	s1,sp,280
 922:	0000                	unimp
 924:	081a                	slli	a6,a6,0x6
 926:	44000007          	0x44000007
 92a:	0004                	0x4
 92c:	1600                	addi	s0,sp,800
 92e:	0714                	addi	a3,sp,896
 930:	0000                	unimp
 932:	2031                	jal	93e <_start-0x6c2>
 934:	00000007          	0x7
 938:	fb1d                	bnez	a4,86e <_start-0x792>
 93a:	0006                	c.slli	zero,0x1
 93c:	1800                	addi	s0,sp,48
 93e:	0011                	c.nop	4
 940:	c000                	sw	s0,0(s0)
 942:	0000                	unimp
 944:	0100                	addi	s0,sp,128
 946:	0654                	addi	a3,sp,772
 948:	000009f7          	0x9f7
 94c:	081a                	slli	a6,a6,0x6
 94e:	7e000007          	0x7e000007
 952:	0004                	0x4
 954:	1c00                	addi	s0,sp,560
 956:	0714                	addi	a3,sp,896
 958:	0000                	unimp
 95a:	9102                	jalr	sp
 95c:	1774                	addi	a3,sp,940
 95e:	0720                	addi	s0,sp,904
 960:	0000                	unimp
 962:	049c                	addi	a5,sp,576
 964:	0000                	unimp
 966:	2a32                	fld	fs4,264(sp)
 968:	18000007          	0x18000007
 96c:	0011                	c.nop	4
 96e:	6c00                	flw	fs0,24(s0)
 970:	0000                	unimp
 972:	ca00                	sw	s0,16(a2)
 974:	0009                	c.nop	2
 976:	1700                	addi	s0,sp,928
 978:	0000072b          	0x72b
 97c:	04c1                	addi	s1,s1,16
 97e:	0000                	unimp
 980:	9714                	0x9714
 982:	18000007          	0x18000007
 986:	0011                	c.nop	4
 988:	5800                	lw	s0,48(s0)
 98a:	0000                	unimp
 98c:	0100                	addi	s0,sp,128
 98e:	0f60                	addi	s0,sp,924
 990:	c01f 0007 1f00      	0x1f000007c01f
 996:	07b4                	addi	a3,sp,968
 998:	0000                	unimp
 99a:	a81f 0007 1600      	0x16000007a81f
 9a0:	07cc                	addi	a1,sp,964
 9a2:	0000                	unimp
 9a4:	d816                	sw	t0,48(sp)
 9a6:	14000007          	0x14000007
 9aa:	00000797          	auipc	a5,0x0
 9ae:	1118                	addi	a4,sp,160
 9b0:	0000                	unimp
 9b2:	0058                	addi	a4,sp,4
 9b4:	0000                	unimp
 9b6:	2101                	jal	db6 <_start-0x24a>
 9b8:	1f0a                	slli	t5,t5,0x22
 9ba:	07c0                	addi	s0,sp,964
 9bc:	0000                	unimp
 9be:	b41f 0007 1f00      	0x1f000007b41f
 9c4:	07a8                	addi	a0,sp,968
 9c6:	0000                	unimp
 9c8:	0007cc17          	auipc	s8,0x7c
 9cc:	e100                	fsw	fs0,0(a0)
 9ce:	0004                	0x4
 9d0:	1700                	addi	s0,sp,928
 9d2:	07d8                	addi	a4,sp,964
 9d4:	0000                	unimp
 9d6:	0500                	addi	s0,sp,640
 9d8:	0000                	unimp
 9da:	e418                	fsw	fa4,8(s0)
 9dc:	48000007          	0x48000007
 9e0:	0000                	unimp
 9e2:	1700                	addi	s0,sp,928
 9e4:	07e5                	addi	a5,a5,25
 9e6:	0000                	unimp
 9e8:	051f 0000 0000      	0x51f
 9ee:	0000                	unimp
 9f0:	7314                	flw	fa3,32(a4)
 9f2:	90000007          	0x90000007
 9f6:	0011                	c.nop	4
 9f8:	3800                	fld	fs0,48(s0)
 9fa:	0000                	unimp
 9fc:	0100                	addi	s0,sp,128
 9fe:	0965                	addi	s2,s2,25
 a00:	801a                	c.mv	zero,t1
 a02:	56000007          	0x56000007
 a06:	0005                	c.nop	1
 a08:	1800                	addi	s0,sp,48
 a0a:	078a                	slli	a5,a5,0x2
 a0c:	0000                	unimp
 a0e:	0060                	addi	s0,sp,12
 a10:	0000                	unimp
 a12:	8b1c                	0x8b1c
 a14:	02000007          	0x2000007
 a18:	7091                	lui	ra,0xfffe4
 a1a:	0000                	unimp
 a1c:	1400                	addi	s0,sp,544
 a1e:	00000773          	0x773
 a22:	11d8                	addi	a4,sp,228
 a24:	0000                	unimp
 a26:	0040                	addi	s0,sp,4
 a28:	0000                	unimp
 a2a:	5a01                	li	s4,-32
 a2c:	1f09                	addi	t5,t5,-30
 a2e:	0780                	addi	s0,sp,960
 a30:	0000                	unimp
 a32:	8a1e                	mv	s4,t2
 a34:	f0000007          	0xf0000007
 a38:	0011                	c.nop	4
 a3a:	2800                	fld	fs0,16(s0)
 a3c:	0000                	unimp
 a3e:	1c00                	addi	s0,sp,560
 a40:	0000078b          	0x78b
 a44:	9102                	jalr	sp
 a46:	0074                	addi	a3,sp,12
 a48:	0000                	unimp
 a4a:	dd30                	sw	a2,120(a0)
 a4c:	0006                	c.slli	zero,0x1
 a4e:	2000                	fld	fs0,0(s0)
 a50:	0012                	c.slli	zero,0x4
 a52:	0800                	addi	s0,sp,16
 a54:	0000                	unimp
 a56:	0100                	addi	s0,sp,128
 a58:	449c                	lw	a5,8(s1)
 a5a:	000a                	c.slli	zero,0x2
 a5c:	1c00                	addi	s0,sp,560
 a5e:	06ee                	slli	a3,a3,0x1b
 a60:	0000                	unimp
 a62:	5a06                	lw	s4,96(sp)
 a64:	935b0493          	addi	s1,s6,-1739
 a68:	0004                	0x4
 a6a:	9930                	0x9930
 a6c:	0006                	c.slli	zero,0x1
 a6e:	2800                	fld	fs0,16(s0)
 a70:	0012                	c.slli	zero,0x4
 a72:	3800                	fld	fs0,48(s0)
 a74:	0000                	unimp
 a76:	0100                	addi	s0,sp,128
 a78:	749c                	flw	fa5,40(s1)
 a7a:	000a                	c.slli	zero,0x2
 a7c:	1500                	addi	s0,sp,672
 a7e:	06a6                	slli	a3,a3,0x9
 a80:	0000                	unimp
 a82:	5a01                	li	s4,-32
 a84:	b215                	j	3a8 <_start-0xc58>
 a86:	0006                	c.slli	zero,0x1
 a88:	0100                	addi	s0,sp,128
 a8a:	06be155b          	0x6be155b
 a8e:	0000                	unimp
 a90:	5c01                	li	s8,-32
 a92:	ca15                	beqz	a2,ac6 <_start-0x53a>
 a94:	0006                	c.slli	zero,0x1
 a96:	0100                	addi	s0,sp,128
 a98:	005d                	c.nop	23
 a9a:	00064b33          	xor	s6,a2,zero
 a9e:	6000                	flw	fs0,0(s0)
 aa0:	0012                	c.slli	zero,0x4
 aa2:	2800                	fld	fs0,16(s0)
 aa4:	0000                	unimp
 aa6:	0100                	addi	s0,sp,128
 aa8:	1a9c                	addi	a5,sp,368
 aaa:	0658                	addi	a4,sp,772
 aac:	0000                	unimp
 aae:	056a                	slli	a0,a0,0x1a
 ab0:	0000                	unimp
 ab2:	641a                	flw	fs0,132(sp)
 ab4:	0006                	c.slli	zero,0x1
 ab6:	8b00                	0x8b00
 ab8:	0005                	c.nop	1
 aba:	1a00                	addi	s0,sp,304
 abc:	0670                	addi	a2,sp,780
 abe:	0000                	unimp
 ac0:	05ac                	addi	a1,sp,712
 ac2:	0000                	unimp
 ac4:	00067c17          	auipc	s8,0x67
 ac8:	dc00                	sw	s0,56(s0)
 aca:	0005                	c.nop	1
 acc:	1c00                	addi	s0,sp,560
 ace:	0686                	slli	a3,a3,0x1
 ad0:	0000                	unimp
 ad2:	5b01                	li	s6,-32
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
  7e:	0101                	addi	sp,sp,0
  80:	1349                	addi	t1,t1,-14
  82:	1301                	addi	t1,t1,-32
  84:	0000                	unimp
  86:	2109                	jal	488 <_start-0xb78>
  88:	4900                	lw	s0,16(a0)
  8a:	00052f13          	slti	t5,a0,0
  8e:	0a00                	addi	s0,sp,272
  90:	0034                	addi	a3,sp,8
  92:	0b3a0e03          	lb	t3,179(s4)
  96:	0b390b3b          	0xb390b3b
  9a:	1349                	addi	t1,t1,-14
  9c:	0188193f 0018020b 	0x18020b0188193f
  a4:	0b00                	addi	s0,sp,400
  a6:	012e                	slli	sp,sp,0xb
  a8:	0e03193f 0b3b0b3a 	0xb3b0b3a0e03193f
  b0:	0b39                	addi	s6,s6,14
  b2:	1349                	addi	t1,t1,-14
  b4:	0111                	addi	sp,sp,4
  b6:	0612                	slli	a2,a2,0x4
  b8:	1840                	addi	s0,sp,52
  ba:	01194297          	auipc	t0,0x1194
  be:	0c000013          	li	zero,192
  c2:	0034                	addi	a3,sp,8
  c4:	0b3a0e03          	lb	t3,179(s4)
  c8:	0b390b3b          	0xb390b3b
  cc:	1349                	addi	t1,t1,-14
  ce:	1802                	slli	a6,a6,0x20
  d0:	0000                	unimp
  d2:	0b0d                	addi	s6,s6,3
  d4:	5501                	li	a0,-32
  d6:	00130117          	auipc	sp,0x130
  da:	0e00                	addi	s0,sp,784
  dc:	0034                	addi	a3,sp,8
  de:	0b3a0803          	lb	a6,179(s4)
  e2:	0b390b3b          	0xb390b3b
  e6:	1349                	addi	t1,t1,-14
  e8:	1802                	slli	a6,a6,0x20
  ea:	0000                	unimp
  ec:	0300340f          	0x300340f
  f0:	3a08                	fld	fa0,48(a2)
  f2:	390b3b0b          	0x390b3b0b
  f6:	0213490b          	0x213490b
  fa:	10000017          	auipc	zero,0x10000
  fe:	1755010b          	0x1755010b
 102:	0000                	unimp
 104:	3411                	jal	fffffb08 <_stack_top+0xffeffb08>
 106:	0300                	addi	s0,sp,384
 108:	3a0e                	fld	fs4,224(sp)
 10a:	390b3b0b          	0x390b3b0b
 10e:	0213490b          	0x213490b
 112:	12000017          	auipc	zero,0x12000
 116:	0034                	addi	a3,sp,8
 118:	0b3a0e03          	lb	t3,179(s4)
 11c:	0b390b3b          	0xb390b3b
 120:	1349                	addi	t1,t1,-14
 122:	0000                	unimp
 124:	11010b13          	addi	s6,sp,272 # 1301e6 <_stack_top+0x301e6>
 128:	1201                	addi	tp,tp,-32
 12a:	0106                	slli	sp,sp,0x1
 12c:	14000013          	li	zero,320
 130:	011d                	addi	sp,sp,7
 132:	1331                	addi	t1,t1,-20
 134:	0111                	addi	sp,sp,4
 136:	0612                	slli	a2,a2,0x4
 138:	0b58                	addi	a4,sp,404
 13a:	0b59                	addi	s6,s6,22
 13c:	00000b57          	0xb57
 140:	0515                	addi	a0,a0,5
 142:	3100                	fld	fs0,32(a0)
 144:	00180213          	addi	tp,a6,1 # 7d4f7 <src_buf+0x7ad97>
 148:	1600                	addi	s0,sp,800
 14a:	0034                	addi	a3,sp,8
 14c:	1331                	addi	t1,t1,-20
 14e:	0000                	unimp
 150:	31003417          	auipc	s0,0x31003
 154:	00170213          	addi	tp,a4,1
 158:	1800                	addi	s0,sp,48
 15a:	1331010b          	0x1331010b
 15e:	1755                	addi	a4,a4,-11
 160:	0000                	unimp
 162:	1d19                	addi	s10,s10,-26
 164:	3101                	jal	fffffd64 <_stack_top+0xffeffd64>
 166:	55015213          	0x55015213
 16a:	590b5817          	auipc	a6,0x590b5
 16e:	010b570b          	0x10b570b
 172:	1a000013          	li	zero,416
 176:	0005                	c.nop	1
 178:	1331                	addi	t1,t1,-20
 17a:	1702                	slli	a4,a4,0x20
 17c:	0000                	unimp
 17e:	31011d1b          	0x31011d1b
 182:	55015213          	0x55015213
 186:	590b5817          	auipc	a6,0x590b5
 18a:	000b570b          	0xb570b
 18e:	1c00                	addi	s0,sp,560
 190:	0034                	addi	a3,sp,8
 192:	1331                	addi	t1,t1,-20
 194:	1802                	slli	a6,a6,0x20
 196:	0000                	unimp
 198:	1d1d                	addi	s10,s10,-25
 19a:	3101                	jal	fffffd9a <_stack_top+0xffeffd9a>
 19c:	12011113          	0x12011113
 1a0:	5806                	lw	a6,96(sp)
 1a2:	570b590b          	0x570b590b
 1a6:	0013010b          	0x13010b
 1aa:	1e00                	addi	s0,sp,816
 1ac:	1331010b          	0x1331010b
 1b0:	0111                	addi	sp,sp,4
 1b2:	0612                	slli	a2,a2,0x4
 1b4:	0000                	unimp
 1b6:	051f 3100 0013      	0x133100051f
 1bc:	2000                	fld	fs0,0(s0)
 1be:	8289                	srli	a3,a3,0x2
 1c0:	0001                	nop
 1c2:	0111                	addi	sp,sp,4
 1c4:	1331                	addi	t1,t1,-20
 1c6:	0000                	unimp
 1c8:	8921                	andi	a0,a0,8
 1ca:	0182                	c.slli64	gp
 1cc:	1101                	addi	sp,sp,-32
 1ce:	3101                	jal	fffffdce <_stack_top+0xffeffdce>
 1d0:	00130113          	addi	sp,t1,1 # 9120999 <_stack_top+0x9020999>
 1d4:	2200                	fld	fs0,0(a2)
 1d6:	828a                	mv	t0,sp
 1d8:	0001                	nop
 1da:	1802                	slli	a6,a6,0x20
 1dc:	4291                	li	t0,4
 1de:	0018                	0x18
 1e0:	2300                	fld	fs0,0(a4)
 1e2:	8289                	srli	a3,a3,0x2
 1e4:	0101                	addi	sp,sp,0
 1e6:	0111                	addi	sp,sp,4
 1e8:	1331                	addi	t1,t1,-20
 1ea:	0000                	unimp
 1ec:	2424                	fld	fs1,72(s0)
 1ee:	0b00                	addi	s0,sp,400
 1f0:	030b3e0b          	0x30b3e0b
 1f4:	0008                	0x8
 1f6:	2500                	fld	fs0,8(a0)
 1f8:	0021                	c.nop	8
 1fa:	1349                	addi	t1,t1,-14
 1fc:	00000b2f          	0xb2f
 200:	2e26                	fld	ft8,72(sp)
 202:	3f01                	jal	112 <_start-0xeee>
 204:	0319                	addi	t1,t1,6
 206:	3a0e                	fld	fs4,224(sp)
 208:	390b3b0b          	0x390b3b0b
 20c:	2019270b          	0x2019270b
 210:	0013010b          	0x13010b
 214:	2700                	fld	fs0,8(a4)
 216:	0005                	c.nop	1
 218:	0b3a0803          	lb	a6,179(s4)
 21c:	0b390b3b          	0xb390b3b
 220:	1349                	addi	t1,t1,-14
 222:	0000                	unimp
 224:	3428                	fld	fa0,104(s0)
 226:	0300                	addi	s0,sp,384
 228:	3a08                	fld	fa0,48(a2)
 22a:	390b3b0b          	0x390b3b0b
 22e:	0013490b          	0x13490b
 232:	2900                	fld	fs0,16(a0)
 234:	0b0b000f          	0xb0b000f
 238:	0000                	unimp
 23a:	0f2a                	slli	t5,t5,0xa
 23c:	0b00                	addi	s0,sp,400
 23e:	0013490b          	0x13490b
 242:	2b00                	fld	fs0,16(a4)
 244:	0005                	c.nop	1
 246:	0b3a0e03          	lb	t3,179(s4)
 24a:	0b390b3b          	0xb390b3b
 24e:	1349                	addi	t1,t1,-14
 250:	0000                	unimp
 252:	2e2c                	fld	fa1,88(a2)
 254:	3f01                	jal	164 <_start-0xe9c>
 256:	0319                	addi	t1,t1,6
 258:	3a0e                	fld	fs4,224(sp)
 25a:	390b3b0b          	0x390b3b0b
 25e:	2013490b          	0x2013490b
 262:	0013010b          	0x13010b
 266:	2d00                	fld	fs0,24(a0)
 268:	0000010b          	0x10b
 26c:	262e                	fld	fa2,200(sp)
 26e:	4900                	lw	s0,16(a0)
 270:	2f000013          	li	zero,752
 274:	012e                	slli	sp,sp,0xb
 276:	0e03193f 0b3b0b3a 	0xb3b0b3a0e03193f
 27e:	0b39                	addi	s6,s6,14
 280:	13491927          	0x13491927
 284:	0b20                	addi	s0,sp,408
 286:	1301                	addi	t1,t1,-32
 288:	0000                	unimp
 28a:	2e30                	fld	fa2,88(a2)
 28c:	3101                	jal	fffffe8c <_stack_top+0xffeffe8c>
 28e:	12011113          	0x12011113
 292:	4006                	0x4006
 294:	9718                	0x9718
 296:	1942                	slli	s2,s2,0x30
 298:	1301                	addi	t1,t1,-32
 29a:	0000                	unimp
 29c:	3431                	jal	fffffca8 <_stack_top+0xffeffca8>
 29e:	3100                	fld	fs0,32(a0)
 2a0:	000b1c13          	slli	s8,s6,0x0
 2a4:	3200                	fld	fs0,32(a2)
 2a6:	1331010b          	0x1331010b
 2aa:	0111                	addi	sp,sp,4
 2ac:	0612                	slli	a2,a2,0x4
 2ae:	1301                	addi	t1,t1,-32
 2b0:	0000                	unimp
 2b2:	31012e33          	0x31012e33
 2b6:	12011113          	0x12011113
 2ba:	4006                	0x4006
 2bc:	9718                	0x9718
 2be:	1942                	slli	s2,s2,0x30
 2c0:	0000                	unimp
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
  34:	0278                	addi	a4,sp,268
  36:	0000                	unimp
  38:	1288                	addi	a0,sp,352
  3a:	0000                	unimp
  3c:	03fc                	addi	a5,sp,460
	...

Disassembly of section .debug_str:

00000000 <.debug_str>:
   0:	72617473          	csrrci	s0,0x726,2
   4:	2e74                	fld	fa3,216(a2)
   6:	682f0053          	0x682f0053
   a:	2f656d6f          	jal	s10,56300 <src_buf+0x53ba0>
   e:	6276                	flw	ft4,92(sp)
  10:	7375786f          	jal	a6,57f46 <src_buf+0x557e6>
  14:	7265                	lui	tp,0xffff9
  16:	5f61632f          	0x5f61632f
  1a:	7270                	flw	fa2,100(a2)
  1c:	63656a6f          	jal	s4,56652 <src_buf+0x53ef2>
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
  3e:	7200                	flw	fs0,32(a2)
  40:	695f 746e 7300      	0x7300746e695f
  46:	6372                	flw	ft6,28(sp)
  48:	625f 6675 6300      	0x63006675625f
  4e:	7570                	flw	fa2,108(a0)
  50:	745f 6d69 0065      	0x656d69745f
  56:	6564                	flw	fs1,76(a0)
  58:	6d5f6373          	csrrsi	t1,0x6d5,30
  5c:	6d65                	lui	s10,0x19
  5e:	6400                	flw	fs0,8(s0)
  60:	7669                	lui	a2,0xffffa
  62:	7369                	lui	t1,0xffffa
  64:	7500726f          	jal	tp,77b4 <src_buf+0x5054>
  68:	6e69                	lui	t3,0x1a
  6a:	3674                	fld	fa3,232(a2)
  6c:	5f34                	lw	a3,120(a4)
  6e:	0074                	addi	a3,sp,12
  70:	5f637273          	csrrci	tp,0x5f6,6
  74:	6461                	lui	s0,0x18
  76:	7264                	flw	fs1,100(a2)
  78:	7500                	flw	fs0,40(a0)
  7a:	736e                	flw	ft6,248(sp)
  7c:	6769                	lui	a4,0x1a
  7e:	656e                	flw	fa0,216(sp)
  80:	2064                	fld	fs1,192(s0)
  82:	72616863          	bltu	sp,t1,7b2 <_start-0x84e>
  86:	6500                	flw	fs0,8(a0)
  88:	646e                	flw	fs0,216(sp)
  8a:	645f 616d 6400      	0x6400616d645f
  90:	625f7473          	csrrci	s0,0x625,30
  94:	6675                	lui	a2,0x1d
  96:	6400                	flw	fs0,8(s0)
  98:	616d                	addi	sp,sp,240
  9a:	745f 6d69 0065      	0x656d69745f
  a0:	6964                	flw	fs1,84(a0)
  a2:	6976                	flw	fs2,92(sp)
  a4:	6564                	flw	fs1,76(a0)
  a6:	646e                	flw	fs0,216(sp)
  a8:	6400                	flw	fs0,8(s0)
  aa:	615f7473          	csrrci	s0,0x615,30
  ae:	6464                	flw	fs1,76(s0)
  b0:	0072                	c.slli	zero,0x1c
  b2:	7270                	flw	fa2,100(a2)
  b4:	6e69                	lui	t3,0x1a
  b6:	5f74                	lw	a3,124(a4)
  b8:	6564                	flw	fs1,76(a0)
  ba:	5f720063          	beq	tp,s7,69a <_start-0x966>
  be:	7266                	flw	ft4,120(sp)
  c0:	6361                	lui	t1,0x18
  c2:	4700                	lw	s0,8(a4)
  c4:	554e                	lw	a0,240(sp)
  c6:	4320                	lw	s0,64(a4)
  c8:	3731                	jal	ffffffd4 <_stack_top+0xffefffd4>
  ca:	3120                	fld	fs0,96(a0)
  cc:	2e30                	fld	fa2,88(a2)
  ce:	2e32                	fld	ft8,264(sp)
  d0:	2030                	fld	fa2,64(s0)
  d2:	6d2d                	lui	s10,0xb
  d4:	7261                	lui	tp,0xffff8
  d6:	723d6863          	bltu	s10,gp,806 <_start-0x7fa>
  da:	3376                	fld	ft6,376(sp)
  dc:	6932                	flw	fs2,12(sp)
  de:	2d20                	fld	fs0,88(a0)
  e0:	616d                	addi	sp,sp,240
  e2:	6962                	flw	fs2,24(sp)
  e4:	693d                	lui	s2,0xf
  e6:	706c                	flw	fa1,100(s0)
  e8:	2d203233          	0x2d203233
  ec:	4f2d2067          	0x4f2d2067
  f0:	0032                	c.slli	zero,0xc
  f2:	6d64                	flw	fs1,92(a0)
  f4:	5f61                	li	t5,-8
  f6:	6562                	flw	fa0,24(sp)
  f8:	636e                	flw	ft6,216(sp)
  fa:	6d68                	flw	fa0,92(a0)
  fc:	7261                	lui	tp,0xffff8
  fe:	00632e6b          	0x632e6b
 102:	616d                	addi	sp,sp,240
 104:	6e69                	lui	t3,0x1a
 106:	7300                	flw	fs0,32(a4)
 108:	7a69                	lui	s4,0xffffa
 10a:	7365                	lui	t1,0xffff9
 10c:	6c00                	flw	fs0,24(s0)
 10e:	20676e6f          	jal	t3,76314 <src_buf+0x73bb4>
 112:	6f6c                	flw	fa1,92(a4)
 114:	676e                	flw	fa4,216(sp)
 116:	7520                	flw	fs0,104(a0)
 118:	736e                	flw	ft6,248(sp)
 11a:	6769                	lui	a4,0x1a
 11c:	656e                	flw	fa0,216(sp)
 11e:	2064                	fld	fs1,192(s0)
 120:	6e69                	lui	t3,0x1a
 122:	0074                	addi	a3,sp,12
 124:	6975                	lui	s2,0x1d
 126:	746e                	flw	fs0,248(sp)
 128:	5f38                	lw	a4,120(a4)
 12a:	0074                	addi	a3,sp,12
 12c:	6572                	flw	fa0,28(sp)
 12e:	6461                	lui	s0,0x18
 130:	635f 6379 656c      	0x656c6379635f
 136:	61720073          	0x61720073
 13a:	6974                	flw	fa3,84(a0)
 13c:	7473006f          	j	31082 <src_buf+0x2e922>
 140:	7261                	lui	tp,0xffff8
 142:	5f74                	lw	a3,124(a4)
 144:	6d64                	flw	fs1,92(a0)
 146:	0061                	c.nop	24
 148:	6e65                	lui	t3,0x19
 14a:	5f64                	lw	s1,124(a4)
 14c:	00757063          	bgeu	a0,t2,14c <_start-0xeb4>
 150:	706d6973          	csrrsi	s2,0x706,26
 154:	656c                	flw	fa1,76(a0)
 156:	755f 6964 0076      	0x766964755f
 15c:	7564                	flw	fs1,108(a0)
 15e:	6d6d                	lui	s10,0x1b
 160:	5f79                	li	t5,-2
 162:	6572                	flw	fa0,28(sp)
 164:	006d                	c.nop	27
 166:	72617473          	csrrci	s0,0x726,2
 16a:	5f74                	lw	a3,124(a4)
 16c:	00757063          	bgeu	a0,t2,16c <_start-0xe94>
 170:	7571                	lui	a0,0xffffc
 172:	6569746f          	jal	s0,977c8 <src_buf+0x95068>
 176:	746e                	flw	fs0,248(sp)
 178:	6400                	flw	fs0,8(s0)
 17a:	616d                	addi	sp,sp,240
 17c:	745f 6172 736e      	0x736e6172745f
 182:	6566                	flw	fa0,88(sp)
 184:	0072                	c.slli	zero,0x1c
 186:	7562                	flw	fa0,56(sp)
 188:	6666                	flw	fa2,88(sp)
 18a:	7265                	lui	tp,0xffff9
 18c:	6d00                	flw	fs0,24(a0)
 18e:	5f79                	li	t5,-2
 190:	6564                	flw	fs1,76(a0)
 192:	75006373          	csrrsi	t1,0x750,0
 196:	7261                	lui	tp,0xffff8
 198:	5f74                	lw	a3,124(a4)
 19a:	7570                	flw	fa2,108(a0)
 19c:	6374                	flw	fa3,68(a4)
 19e:	7500                	flw	fs0,40(a0)
 1a0:	6e69                	lui	t3,0x1a
 1a2:	3374                	fld	fa3,224(a4)
 1a4:	5f32                	lw	t5,44(sp)
 1a6:	0074                	addi	a3,sp,12
 1a8:	6572                	flw	fa0,28(sp)
 1aa:	616d                	addi	sp,sp,240
 1ac:	6e69                	lui	t3,0x1a
 1ae:	6564                	flw	fs1,76(a0)
 1b0:	0072                	c.slli	zero,0x1c
 1b2:	656e                	flw	fa0,216(sp)
 1b4:	7478                	flw	fa4,108(s0)
 1b6:	705f 7274 7000      	0x70007274705f
 1bc:	6972                	flw	fs2,28(sp)
 1be:	746e                	flw	fs0,248(sp)
 1c0:	735f 7274 6400      	0x64007274735f
 1c6:	616d                	addi	sp,sp,240
 1c8:	645f 7365 5f63      	0x5f637365645f
 1ce:	0074                	addi	a3,sp,12
 1d0:	5f757063          	bgeu	a0,s7,7b0 <_start-0x850>
 1d4:	656d                	lui	a0,0x1b
 1d6:	636d                	lui	t1,0x1b
 1d8:	7970                	flw	fa2,116(a0)
 1da:	7300                	flw	fs0,32(a4)
 1dc:	7a69                	lui	s4,0xffffa
 1de:	0065                	c.nop	25
 1e0:	656c                	flw	fa1,76(a0)
 1e2:	676e                	flw	fa4,216(sp)
 1e4:	6874                	flw	fa3,84(s0)
	...

Disassembly of section .debug_loc:

00000000 <.debug_loc>:
   0:	130c                	addi	a1,sp,416
   2:	0000                	unimp
   4:	1320                	addi	s0,sp,424
   6:	0000                	unimp
   8:	0002                	c.slli64	zero
   a:	9f30                	0x9f30
   c:	1320                	addi	s0,sp,424
   e:	0000                	unimp
  10:	1334                	addi	a3,sp,424
  12:	0000                	unimp
  14:	0001                	nop
  16:	005f 0000 0000      	0x5f
  1c:	0000                	unimp
  1e:	6000                	flw	fs0,0(s0)
  20:	cc000013          	li	zero,-832
  24:	02000013          	li	zero,32
  28:	3000                	fld	fs0,32(s0)
  2a:	009f 0000 0000      	0x9f
  30:	0000                	unimp
  32:	cc00                	sw	s0,24(s0)
  34:	f4000013          	li	zero,-192
  38:	02000013          	li	zero,32
  3c:	8300                	0x8300
  3e:	f400                	fsw	fs0,40(s0)
  40:	5f000013          	li	zero,1520
  44:	0014                	0x14
  46:	0100                	addi	s0,sp,128
  48:	5a00                	lw	s0,48(a2)
  4a:	1638                	addi	a4,sp,808
  4c:	0000                	unimp
  4e:	1684                	addi	s1,sp,864
  50:	0000                	unimp
  52:	0001                	nop
  54:	005a                	c.slli	zero,0x16
  56:	0000                	unimp
  58:	0000                	unimp
  5a:	0000                	unimp
  5c:	5400                	lw	s0,40(s0)
  5e:	0014                	0x14
  60:	5c00                	lw	s0,56(s0)
  62:	0014                	0x14
  64:	0200                	addi	s0,sp,256
  66:	3000                	fld	fs0,32(s0)
  68:	5c9f 0014 3800      	0x380000145c9f
  6e:	0016                	c.slli	zero,0x5
  70:	0100                	addi	s0,sp,128
  72:	6900                	flw	fs0,16(a0)
  74:	1638                	addi	a4,sp,808
  76:	0000                	unimp
  78:	1684                	addi	s1,sp,864
  7a:	0000                	unimp
  7c:	0002                	c.slli64	zero
  7e:	9f30                	0x9f30
	...
  88:	1590                	addi	a2,sp,736
  8a:	0000                	unimp
  8c:	00001593          	slli	a1,zero,0x0
  90:	0001                	nop
  92:	005a                	c.slli	zero,0x16
  94:	0000                	unimp
  96:	0000                	unimp
  98:	0000                	unimp
  9a:	9000                	0x9000
  9c:	0015                	c.nop	5
  9e:	1c00                	addi	s0,sp,560
  a0:	0016                	c.slli	zero,0x5
  a2:	0100                	addi	s0,sp,128
  a4:	6800                	flw	fs0,16(s0)
	...
  ae:	1644                	addi	s1,sp,804
  b0:	0000                	unimp
  b2:	1654                	addi	a3,sp,804
  b4:	0000                	unimp
  b6:	0002                	c.slli64	zero
  b8:	9f30                	0x9f30
  ba:	1654                	addi	a3,sp,804
  bc:	0000                	unimp
  be:	1684                	addi	s1,sp,864
  c0:	0000                	unimp
  c2:	0001                	nop
  c4:	0069                	c.nop	26
  c6:	0000                	unimp
  c8:	0000                	unimp
  ca:	0000                	unimp
  cc:	4400                	lw	s0,8(s0)
  ce:	0016                	c.slli	zero,0x5
  d0:	5400                	lw	s0,40(s0)
  d2:	0016                	c.slli	zero,0x5
  d4:	0200                	addi	s0,sp,256
  d6:	3000                	fld	fs0,32(s0)
  d8:	549f 0016 8400      	0x84000016549f
  de:	0016                	c.slli	zero,0x5
  e0:	0100                	addi	s0,sp,128
  e2:	5e00                	lw	s0,56(a2)
	...
  ec:	1644                	addi	s1,sp,804
  ee:	0000                	unimp
  f0:	1654                	addi	a3,sp,804
  f2:	0000                	unimp
  f4:	0002                	c.slli64	zero
  f6:	16549f4f          	fnmadd.q	ft10,fs1,ft5,ft2,rtz
  fa:	0000                	unimp
  fc:	166c                	addi	a1,sp,812
  fe:	0000                	unimp
 100:	0001                	nop
 102:	00166c5b          	0x166c5b
 106:	7800                	flw	fs0,48(s0)
 108:	0016                	c.slli	zero,0x5
 10a:	0300                	addi	s0,sp,384
 10c:	7b00                	flw	fs0,48(a4)
 10e:	9f01                	0x9f01
 110:	1678                	addi	a4,sp,812
 112:	0000                	unimp
 114:	1684                	addi	s1,sp,864
 116:	0000                	unimp
 118:	0001                	nop
 11a:	0000005b          	0x5b
 11e:	0000                	unimp
 120:	0000                	unimp
 122:	d400                	sw	s0,40(s0)
 124:	f4000013          	li	zero,-192
 128:	06000013          	li	zero,96
 12c:	8300                	0x8300
 12e:	0600                	addi	s0,sp,768
 130:	1c31                	addi	s8,s8,-20
 132:	f49f 0013 fc00      	0xfc000013f49f
 138:	0c000013          	li	zero,192
 13c:	7a00                	flw	fs0,48(a2)
 13e:	7e00                	flw	fs0,56(a2)
 140:	1c00                	addi	s0,sp,560
 142:	00275f03          	lhu	t5,2(a4) # 1a002 <src_buf+0x178a2>
 146:	2200                	fld	fs0,0(a2)
 148:	fc9f 0013 0800      	0x8000013fc9f
 14e:	0014                	0x14
 150:	0c00                	addi	s0,sp,528
 152:	7a00                	flw	fs0,48(a2)
 154:	7e00                	flw	fs0,56(a2)
 156:	1c00                	addi	s0,sp,560
 158:	00276003          	0x276003
 15c:	2200                	fld	fs0,0(a2)
 15e:	089f 0014 0c00      	0xc000014089f
 164:	0014                	0x14
 166:	0c00                	addi	s0,sp,528
 168:	7a00                	flw	fs0,48(a2)
 16a:	7e00                	flw	fs0,56(a2)
 16c:	1c00                	addi	s0,sp,560
 16e:	00275f03          	lhu	t5,2(a4)
 172:	2200                	fld	fs0,0(a2)
 174:	009f 0000 0000      	0x9f
 17a:	0000                	unimp
 17c:	d400                	sw	s0,40(s0)
 17e:	0c000013          	li	zero,192
 182:	0014                	0x14
 184:	0600                	addi	s0,sp,768
 186:	0300                	addi	s0,sp,384
 188:	2760                	fld	fs0,200(a4)
 18a:	0000                	unimp
 18c:	009f 0000 0000      	0x9f
 192:	0000                	unimp
 194:	d400                	sw	s0,40(s0)
 196:	0c000013          	li	zero,192
 19a:	0014                	0x14
 19c:	0600                	addi	s0,sp,768
 19e:	0300                	addi	s0,sp,384
 1a0:	1760                	addi	s0,sp,940
 1a2:	0000                	unimp
 1a4:	009f 0000 0000      	0x9f
 1aa:	0000                	unimp
 1ac:	d400                	sw	s0,40(s0)
 1ae:	f4000013          	li	zero,-192
 1b2:	06000013          	li	zero,96
 1b6:	0300                	addi	s0,sp,384
 1b8:	1760                	addi	s0,sp,940
 1ba:	0000                	unimp
 1bc:	f49f 0013 0000      	0x13f49f
 1c2:	0014                	0x14
 1c4:	0100                	addi	s0,sp,128
 1c6:	5600                	lw	s0,40(a2)
 1c8:	1400                	addi	s0,sp,544
 1ca:	0000                	unimp
 1cc:	1408                	addi	a0,sp,544
 1ce:	0000                	unimp
 1d0:	7f760003          	lb	zero,2039(a2) # 1d7f7 <src_buf+0x1b097>
 1d4:	089f 0014 0c00      	0xc000014089f
 1da:	0014                	0x14
 1dc:	0100                	addi	s0,sp,128
 1de:	5600                	lw	s0,40(a2)
	...
 1e8:	13d4                	addi	a3,sp,484
 1ea:	0000                	unimp
 1ec:	13f4                	addi	a3,sp,492
 1ee:	0000                	unimp
 1f0:	0006                	c.slli	zero,0x1
 1f2:	00276003          	0x276003
 1f6:	9f00                	0x9f00
 1f8:	13f4                	addi	a3,sp,492
 1fa:	0000                	unimp
 1fc:	140c                	addi	a1,sp,544
 1fe:	0000                	unimp
 200:	0001                	nop
 202:	005e                	c.slli	zero,0x17
 204:	0000                	unimp
 206:	0000                	unimp
 208:	0000                	unimp
 20a:	1800                	addi	s0,sp,48
 20c:	0014                	0x14
 20e:	4800                	lw	s0,16(s0)
 210:	0014                	0x14
 212:	0600                	addi	s0,sp,768
 214:	0300                	addi	s0,sp,384
 216:	1750                	addi	a2,sp,932
 218:	0000                	unimp
 21a:	009f 0000 0000      	0x9f
 220:	0000                	unimp
 222:	1800                	addi	s0,sp,48
 224:	0014                	0x14
 226:	4800                	lw	s0,16(s0)
 228:	0014                	0x14
 22a:	0100                	addi	s0,sp,128
 22c:	5a00                	lw	s0,48(a2)
	...
 236:	1418                	addi	a4,sp,544
 238:	0000                	unimp
 23a:	1448                	addi	a0,sp,548
 23c:	0000                	unimp
 23e:	0006                	c.slli	zero,0x1
 240:	00276003          	0x276003
 244:	9f00                	0x9f00
	...
 24e:	1418                	addi	a4,sp,544
 250:	0000                	unimp
 252:	1448                	addi	a0,sp,548
 254:	0000                	unimp
 256:	0006                	c.slli	zero,0x1
 258:	00176003          	0x176003
 25c:	9f00                	0x9f00
	...
 266:	1460                	addi	s0,sp,556
 268:	0000                	unimp
 26a:	146c                	addi	a1,sp,556
 26c:	0000                	unimp
 26e:	0006                	c.slli	zero,0x1
 270:	00168403          	lb	s0,1(a3)
 274:	9f00                	0x9f00
 276:	146c                	addi	a1,sp,556
 278:	0000                	unimp
 27a:	14a8                	addi	a0,sp,616
 27c:	0000                	unimp
 27e:	0001                	nop
 280:	006c                	addi	a1,sp,12
 282:	0000                	unimp
 284:	0000                	unimp
 286:	0000                	unimp
 288:	7000                	flw	fs0,32(s0)
 28a:	0014                	0x14
 28c:	a000                	fsd	fs0,0(s0)
 28e:	0014                	0x14
 290:	0100                	addi	s0,sp,128
 292:	6d00                	flw	fs0,24(a0)
	...
 29c:	14b0                	addi	a2,sp,616
 29e:	0000                	unimp
 2a0:	14bc                	addi	a5,sp,616
 2a2:	0000                	unimp
 2a4:	0006                	c.slli	zero,0x1
 2a6:	00168803          	lb	a6,1(a3)
 2aa:	9f00                	0x9f00
 2ac:	14bc                	addi	a5,sp,616
 2ae:	0000                	unimp
 2b0:	14f8                	addi	a4,sp,620
 2b2:	0000                	unimp
 2b4:	0001                	nop
 2b6:	0056                	c.slli	zero,0x15
 2b8:	0000                	unimp
 2ba:	0000                	unimp
 2bc:	0000                	unimp
 2be:	c000                	sw	s0,0(s0)
 2c0:	0014                	0x14
 2c2:	f000                	fsw	fs0,32(s0)
 2c4:	0014                	0x14
 2c6:	0100                	addi	s0,sp,128
 2c8:	6c00                	flw	fs0,24(s0)
	...
 2d2:	1500                	addi	s0,sp,672
 2d4:	0000                	unimp
 2d6:	150c                	addi	a1,sp,672
 2d8:	0000                	unimp
 2da:	0006                	c.slli	zero,0x1
 2dc:	00169003          	lh	zero,1(a3)
 2e0:	9f00                	0x9f00
 2e2:	150c                	addi	a1,sp,672
 2e4:	0000                	unimp
 2e6:	1550                	addi	a2,sp,676
 2e8:	0000                	unimp
 2ea:	0001                	nop
 2ec:	0056                	c.slli	zero,0x15
 2ee:	0000                	unimp
 2f0:	0000                	unimp
 2f2:	0000                	unimp
 2f4:	1000                	addi	s0,sp,32
 2f6:	0015                	c.nop	5
 2f8:	2800                	fld	fs0,16(s0)
 2fa:	0015                	c.nop	5
 2fc:	0100                	addi	s0,sp,128
 2fe:	5e00                	lw	s0,56(a2)
	...
 308:	1550                	addi	a2,sp,676
 30a:	0000                	unimp
 30c:	1590                	addi	a2,sp,736
 30e:	0000                	unimp
 310:	0001                	nop
 312:	005a                	c.slli	zero,0x16
 314:	0000                	unimp
 316:	0000                	unimp
 318:	0000                	unimp
 31a:	5000                	lw	s0,32(s0)
 31c:	0015                	c.nop	5
 31e:	9000                	0x9000
 320:	0015                	c.nop	5
 322:	0100                	addi	s0,sp,128
 324:	6800                	flw	fs0,16(s0)
	...
 32e:	1554                	addi	a3,sp,676
 330:	0000                	unimp
 332:	1574                	addi	a3,sp,684
 334:	0000                	unimp
 336:	0001                	nop
 338:	745e                	flw	fs0,244(sp)
 33a:	0015                	c.nop	5
 33c:	8000                	0x8000
 33e:	0015                	c.nop	5
 340:	0300                	addi	s0,sp,384
 342:	7e00                	flw	fs0,56(a2)
 344:	9f01                	0x9f01
 346:	1580                	addi	s0,sp,736
 348:	0000                	unimp
 34a:	1590                	addi	a2,sp,736
 34c:	0000                	unimp
 34e:	0001                	nop
 350:	005e                	c.slli	zero,0x17
 352:	0000                	unimp
 354:	0000                	unimp
 356:	0000                	unimp
 358:	9400                	0x9400
 35a:	0015                	c.nop	5
 35c:	cc00                	sw	s0,24(s0)
 35e:	0015                	c.nop	5
 360:	0300                	addi	s0,sp,384
 362:	0800                	addi	s0,sp,16
 364:	9f2e                	add	t5,t5,a1
	...
 36e:	15d4                	addi	a3,sp,740
 370:	0000                	unimp
 372:	15e0                	addi	s0,sp,748
 374:	0000                	unimp
 376:	0006                	c.slli	zero,0x1
 378:	00169803          	lh	a6,1(a3)
 37c:	9f00                	0x9f00
 37e:	15e0                	addi	s0,sp,748
 380:	0000                	unimp
 382:	161c                	addi	a5,sp,800
 384:	0000                	unimp
 386:	0001                	nop
 388:	005a                	c.slli	zero,0x16
 38a:	0000                	unimp
 38c:	0000                	unimp
 38e:	0000                	unimp
 390:	e400                	fsw	fs0,8(s0)
 392:	0015                	c.nop	5
 394:	1400                	addi	s0,sp,544
 396:	0016                	c.slli	zero,0x5
 398:	0100                	addi	s0,sp,128
 39a:	6000                	flw	fs0,0(s0)
	...
 3a4:	1010                	addi	a2,sp,32
 3a6:	0000                	unimp
 3a8:	1018                	addi	a4,sp,32
 3aa:	0000                	unimp
 3ac:	0001                	nop
 3ae:	185a                	slli	a6,a6,0x36
 3b0:	0010                	0x10
 3b2:	6400                	flw	fs0,8(s0)
 3b4:	0010                	0x10
 3b6:	0100                	addi	s0,sp,128
 3b8:	6000                	flw	fs0,0(s0)
	...
 3c2:	102c                	addi	a1,sp,40
 3c4:	0000                	unimp
 3c6:	1060                	addi	s0,sp,44
 3c8:	0000                	unimp
 3ca:	0001                	nop
 3cc:	005a                	c.slli	zero,0x16
 3ce:	0000                	unimp
 3d0:	0000                	unimp
 3d2:	0000                	unimp
 3d4:	2c00                	fld	fs0,24(s0)
 3d6:	0010                	0x10
 3d8:	6000                	flw	fs0,0(s0)
 3da:	0010                	0x10
 3dc:	0100                	addi	s0,sp,128
 3de:	5f00                	lw	s0,56(a4)
	...
 3e8:	102c                	addi	a1,sp,40
 3ea:	0000                	unimp
 3ec:	1044                	addi	s1,sp,36
 3ee:	0000                	unimp
 3f0:	0001                	nop
 3f2:	445e                	lw	s0,212(sp)
 3f4:	0010                	0x10
 3f6:	5000                	lw	s0,32(s0)
 3f8:	0010                	0x10
 3fa:	0300                	addi	s0,sp,384
 3fc:	7e00                	flw	fs0,56(a2)
 3fe:	9f01                	0x9f01
 400:	1050                	addi	a2,sp,36
 402:	0000                	unimp
 404:	1060                	addi	s0,sp,44
 406:	0000                	unimp
 408:	0001                	nop
 40a:	005e                	c.slli	zero,0x17
 40c:	0000                	unimp
 40e:	0000                	unimp
 410:	0000                	unimp
 412:	ac00                	fsd	fs0,24(s0)
 414:	0010                	0x10
 416:	c400                	sw	s0,8(s0)
 418:	0010                	0x10
 41a:	0100                	addi	s0,sp,128
 41c:	5a00                	lw	s0,48(a2)
 41e:	10c4                	addi	s1,sp,100
 420:	0000                	unimp
 422:	1108                	addi	a0,sp,160
 424:	0000                	unimp
 426:	0001                	nop
 428:	005a                	c.slli	zero,0x16
 42a:	0000                	unimp
 42c:	0000                	unimp
 42e:	0000                	unimp
 430:	c400                	sw	s0,8(s0)
 432:	0010                	0x10
 434:	f400                	fsw	fs0,40(s0)
 436:	0010                	0x10
 438:	0100                	addi	s0,sp,128
 43a:	5c00                	lw	s0,56(s0)
	...
 444:	1108                	addi	a0,sp,160
 446:	0000                	unimp
 448:	1124                	addi	s1,sp,168
 44a:	0000                	unimp
 44c:	0001                	nop
 44e:	245a                	fld	fs0,400(sp)
 450:	0011                	c.nop	4
 452:	d800                	sw	s0,48(s0)
 454:	0011                	c.nop	4
 456:	0400                	addi	s0,sp,512
 458:	f300                	fsw	fs0,32(a4)
 45a:	5a01                	li	s4,-32
 45c:	d89f 0011 1800      	0x18000011d89f
 462:	0012                	c.slli	zero,0x4
 464:	0100                	addi	s0,sp,128
 466:	5a00                	lw	s0,48(a2)
 468:	1218                	addi	a4,sp,288
 46a:	0000                	unimp
 46c:	1220                	addi	s0,sp,296
 46e:	0000                	unimp
 470:	0004                	0x4
 472:	9f5a01f3          	0x9f5a01f3
	...
 47e:	1124                	addi	s1,sp,168
 480:	0000                	unimp
 482:	1170                	addi	a2,sp,172
 484:	0000                	unimp
 486:	0001                	nop
 488:	705a                	flw	ft0,180(sp)
 48a:	0011                	c.nop	4
 48c:	d800                	sw	s0,48(s0)
 48e:	0011                	c.nop	4
 490:	0100                	addi	s0,sp,128
 492:	6000                	flw	fs0,0(s0)
	...
 49c:	1124                	addi	s1,sp,168
 49e:	0000                	unimp
 4a0:	1198                	addi	a4,sp,224
 4a2:	0000                	unimp
 4a4:	0001                	nop
 4a6:	d46d                	beqz	s0,490 <_start-0xb70>
 4a8:	0011                	c.nop	4
 4aa:	d800                	sw	s0,48(s0)
 4ac:	0011                	c.nop	4
 4ae:	0800                	addi	s0,sp,16
 4b0:	7b00                	flw	fs0,48(a4)
 4b2:	9100                	0x9100
 4b4:	1c00                	addi	s0,sp,560
 4b6:	009f0b23          	sb	s1,22(t5)
 4ba:	0000                	unimp
 4bc:	0000                	unimp
 4be:	0000                	unimp
 4c0:	7000                	flw	fs0,32(s0)
 4c2:	0011                	c.nop	4
 4c4:	7800                	flw	fs0,48(s0)
 4c6:	0011                	c.nop	4
 4c8:	0100                	addi	s0,sp,128
 4ca:	5e00                	lw	s0,56(a2)
 4cc:	1178                	addi	a4,sp,172
 4ce:	0000                	unimp
 4d0:	117c                	addi	a5,sp,172
 4d2:	0000                	unimp
 4d4:	507e0003          	lb	zero,1287(t3) # 1a507 <src_buf+0x17da7>
 4d8:	009f 0000 0000      	0x9f
 4de:	0000                	unimp
 4e0:	2400                	fld	fs0,8(s0)
 4e2:	0011                	c.nop	4
 4e4:	3000                	fld	fs0,32(s0)
 4e6:	0011                	c.nop	4
 4e8:	0200                	addi	s0,sp,256
 4ea:	3000                	fld	fs0,32(s0)
 4ec:	309f 0011 7000      	0x70000011309f
 4f2:	0011                	c.nop	4
 4f4:	0100                	addi	s0,sp,128
 4f6:	6000                	flw	fs0,0(s0)
	...
 500:	1124                	addi	s1,sp,168
 502:	0000                	unimp
 504:	1128                	addi	a0,sp,168
 506:	0000                	unimp
 508:	0002                	c.slli64	zero
 50a:	9f30                	0x9f30
 50c:	1128                	addi	a0,sp,168
 50e:	0000                	unimp
 510:	1170                	addi	a2,sp,172
 512:	0000                	unimp
 514:	0001                	nop
 516:	005e                	c.slli	zero,0x17
 518:	0000                	unimp
 51a:	0000                	unimp
 51c:	0000                	unimp
 51e:	2400                	fld	fs0,8(s0)
 520:	0011                	c.nop	4
 522:	3000                	fld	fs0,32(s0)
 524:	0011                	c.nop	4
 526:	0200                	addi	s0,sp,256
 528:	4f00                	lw	s0,24(a4)
 52a:	349f 0011 5400      	0x54000011349f
 530:	0011                	c.nop	4
 532:	0100                	addi	s0,sp,128
 534:	5f00                	lw	s0,56(a4)
 536:	1154                	addi	a3,sp,164
 538:	0000                	unimp
 53a:	1160                	addi	s0,sp,172
 53c:	0000                	unimp
 53e:	017f0003          	lb	zero,23(t5)
 542:	609f 0011 7000      	0x70000011609f
 548:	0011                	c.nop	4
 54a:	0100                	addi	s0,sp,128
 54c:	5f00                	lw	s0,56(a4)
	...
 556:	11d4                	addi	a3,sp,228
 558:	0000                	unimp
 55a:	11d8                	addi	a4,sp,228
 55c:	0000                	unimp
 55e:	0002                	c.slli64	zero
 560:	00007f7b          	0x7f7b
 564:	0000                	unimp
 566:	0000                	unimp
 568:	0000                	unimp
 56a:	1260                	addi	s0,sp,300
 56c:	0000                	unimp
 56e:	1268                	addi	a0,sp,300
 570:	0000                	unimp
 572:	0001                	nop
 574:	685a                	flw	fa6,148(sp)
 576:	0012                	c.slli	zero,0x4
 578:	8800                	0x8800
 57a:	0012                	c.slli	zero,0x4
 57c:	0400                	addi	s0,sp,512
 57e:	f300                	fsw	fs0,32(a4)
 580:	5a01                	li	s4,-32
 582:	009f 0000 0000      	0x9f
 588:	0000                	unimp
 58a:	6000                	flw	fs0,0(s0)
 58c:	0012                	c.slli	zero,0x4
 58e:	6800                	flw	fs0,16(s0)
 590:	0012                	c.slli	zero,0x4
 592:	0100                	addi	s0,sp,128
 594:	5b00                	lw	s0,48(a4)
 596:	1268                	addi	a0,sp,300
 598:	0000                	unimp
 59a:	1288                	addi	a0,sp,352
 59c:	0000                	unimp
 59e:	0004                	0x4
 5a0:	9f5b01f3          	0x9f5b01f3
	...
 5ac:	1260                	addi	s0,sp,300
 5ae:	0000                	unimp
 5b0:	1260                	addi	s0,sp,300
 5b2:	0000                	unimp
 5b4:	0001                	nop
 5b6:	605c                	flw	fa5,4(s0)
 5b8:	0012                	c.slli	zero,0x4
 5ba:	6800                	flw	fs0,16(s0)
 5bc:	0012                	c.slli	zero,0x4
 5be:	0300                	addi	s0,sp,384
 5c0:	7c00                	flw	fs0,56(s0)
 5c2:	9f7f                	0x9f7f
 5c4:	1268                	addi	a0,sp,300
 5c6:	0000                	unimp
 5c8:	1284                	addi	s1,sp,352
 5ca:	0000                	unimp
 5cc:	0006                	c.slli	zero,0x1
 5ce:	315c01f3          	0x315c01f3
 5d2:	9f1c                	0x9f1c
	...
 5dc:	1260                	addi	s0,sp,300
 5de:	0000                	unimp
 5e0:	1274                	addi	a3,sp,300
 5e2:	0000                	unimp
 5e4:	0001                	nop
 5e6:	745a                	flw	fs0,180(sp)
 5e8:	0012                	c.slli	zero,0x4
 5ea:	8000                	0x8000
 5ec:	0012                	c.slli	zero,0x4
 5ee:	0100                	addi	s0,sp,128
 5f0:	5e00                	lw	s0,56(a2)
 5f2:	1280                	addi	s0,sp,352
 5f4:	0000                	unimp
 5f6:	1288                	addi	a0,sp,352
 5f8:	0000                	unimp
 5fa:	0001                	nop
 5fc:	005a                	c.slli	zero,0x16
 5fe:	0000                	unimp
 600:	0000                	unimp
 602:	0000                	unimp
	...

Disassembly of section .debug_ranges:

00000000 <.debug_ranges>:
   0:	101c                	addi	a5,sp,32
   2:	0000                	unimp
   4:	1020                	addi	s0,sp,40
   6:	0000                	unimp
   8:	1024                	addi	s1,sp,40
   a:	0000                	unimp
   c:	1054                	addi	a3,sp,36
	...
  16:	0000                	unimp
  18:	10b8                	addi	a4,sp,104
  1a:	0000                	unimp
  1c:	10c0                	addi	s0,sp,100
  1e:	0000                	unimp
  20:	10c4                	addi	s1,sp,100
  22:	0000                	unimp
  24:	10f4                	addi	a3,sp,108
	...
  2e:	0000                	unimp
  30:	10bc                	addi	a5,sp,104
  32:	0000                	unimp
  34:	10c0                	addi	s0,sp,100
  36:	0000                	unimp
  38:	10d4                	addi	a3,sp,100
  3a:	0000                	unimp
  3c:	10f4                	addi	a3,sp,108
	...
  46:	0000                	unimp
  48:	1118                	addi	a4,sp,160
  4a:	0000                	unimp
  4c:	112c                	addi	a1,sp,168
  4e:	0000                	unimp
  50:	1130                	addi	a2,sp,168
  52:	0000                	unimp
  54:	1170                	addi	a2,sp,172
	...
  5e:	0000                	unimp
  60:	1194                	addi	a3,sp,224
  62:	0000                	unimp
  64:	1198                	addi	a4,sp,224
  66:	0000                	unimp
  68:	11a8                	addi	a0,sp,232
  6a:	0000                	unimp
  6c:	11c8                	addi	a0,sp,228
	...
  76:	0000                	unimp
  78:	1288                	addi	a0,sp,352
  7a:	0000                	unimp
  7c:	1288                	addi	a0,sp,352
  7e:	0000                	unimp
  80:	128c                	addi	a1,sp,352
  82:	0000                	unimp
  84:	1298                	addi	a4,sp,352
  86:	0000                	unimp
  88:	12cc                	addi	a1,sp,356
  8a:	0000                	unimp
  8c:	12e8                	addi	a0,sp,364
	...
  96:	0000                	unimp
  98:	130c                	addi	a1,sp,416
  9a:	0000                	unimp
  9c:	130c                	addi	a1,sp,416
  9e:	0000                	unimp
  a0:	1318                	addi	a4,sp,416
  a2:	0000                	unimp
  a4:	1330                	addi	a2,sp,424
	...
  ae:	0000                	unimp
  b0:	1360                	addi	s0,sp,428
  b2:	0000                	unimp
  b4:	1390                	addi	a2,sp,480
  b6:	0000                	unimp
  b8:	13a8                	addi	a0,sp,488
  ba:	0000                	unimp
  bc:	1594                	addi	a3,sp,736
  be:	0000                	unimp
  c0:	1594                	addi	a3,sp,736
  c2:	0000                	unimp
  c4:	15cc                	addi	a1,sp,740
  c6:	0000                	unimp
  c8:	15cc                	addi	a1,sp,740
  ca:	0000                	unimp
  cc:	1628                	addi	a0,sp,808
  ce:	0000                	unimp
  d0:	1638                	addi	a4,sp,808
  d2:	0000                	unimp
  d4:	1684                	addi	s1,sp,864
	...
  de:	0000                	unimp
  e0:	137c                	addi	a5,sp,428
  e2:	0000                	unimp
  e4:	1390                	addi	a2,sp,480
  e6:	0000                	unimp
  e8:	13a8                	addi	a0,sp,488
  ea:	0000                	unimp
  ec:	1594                	addi	a3,sp,736
  ee:	0000                	unimp
  f0:	1594                	addi	a3,sp,736
  f2:	0000                	unimp
  f4:	15cc                	addi	a1,sp,740
  f6:	0000                	unimp
  f8:	15cc                	addi	a1,sp,740
  fa:	0000                	unimp
  fc:	161c                	addi	a5,sp,800
  fe:	0000                	unimp
 100:	1638                	addi	a4,sp,808
 102:	0000                	unimp
 104:	1684                	addi	s1,sp,864
	...
 10e:	0000                	unimp
 110:	137c                	addi	a5,sp,428
 112:	0000                	unimp
 114:	1380                	addi	s0,sp,480
 116:	0000                	unimp
 118:	138c                	addi	a1,sp,480
 11a:	0000                	unimp
 11c:	1390                	addi	a2,sp,480
 11e:	0000                	unimp
 120:	13b8                	addi	a4,sp,488
 122:	0000                	unimp
 124:	13bc                	addi	a5,sp,488
 126:	0000                	unimp
 128:	13d4                	addi	a3,sp,484
 12a:	0000                	unimp
 12c:	140c                	addi	a1,sp,544
	...
 136:	0000                	unimp
 138:	1380                	addi	s0,sp,480
 13a:	0000                	unimp
 13c:	138c                	addi	a1,sp,480
 13e:	0000                	unimp
 140:	13a8                	addi	a0,sp,488
 142:	0000                	unimp
 144:	13b8                	addi	a4,sp,488
 146:	0000                	unimp
 148:	13bc                	addi	a5,sp,488
 14a:	0000                	unimp
 14c:	13c4                	addi	s1,sp,484
 14e:	0000                	unimp
 150:	1418                	addi	a4,sp,544
 152:	0000                	unimp
 154:	1448                	addi	a0,sp,548
	...
 15e:	0000                	unimp
 160:	13c4                	addi	s1,sp,484
 162:	0000                	unimp
 164:	13cc                	addi	a1,sp,484
 166:	0000                	unimp
 168:	1460                	addi	s0,sp,556
 16a:	0000                	unimp
 16c:	1460                	addi	s0,sp,556
 16e:	0000                	unimp
 170:	1464                	addi	s1,sp,556
 172:	0000                	unimp
 174:	14a8                	addi	a0,sp,616
	...
 17e:	0000                	unimp
 180:	13c8                	addi	a0,sp,484
 182:	0000                	unimp
 184:	13cc                	addi	a1,sp,484
 186:	0000                	unimp
 188:	1468                	addi	a0,sp,556
 18a:	0000                	unimp
 18c:	146c                	addi	a1,sp,556
 18e:	0000                	unimp
 190:	1470                	addi	a2,sp,556
 192:	0000                	unimp
 194:	14a0                	addi	s0,sp,616
	...
 19e:	0000                	unimp
 1a0:	13c8                	addi	a0,sp,484
 1a2:	0000                	unimp
 1a4:	13cc                	addi	a1,sp,484
 1a6:	0000                	unimp
 1a8:	1480                	addi	s0,sp,608
 1aa:	0000                	unimp
 1ac:	14a0                	addi	s0,sp,616
	...
 1b6:	0000                	unimp
 1b8:	14b0                	addi	a2,sp,616
 1ba:	0000                	unimp
 1bc:	14b0                	addi	a2,sp,616
 1be:	0000                	unimp
 1c0:	14b4                	addi	a3,sp,616
 1c2:	0000                	unimp
 1c4:	14f8                	addi	a4,sp,620
	...
 1ce:	0000                	unimp
 1d0:	14b8                	addi	a4,sp,616
 1d2:	0000                	unimp
 1d4:	14bc                	addi	a5,sp,616
 1d6:	0000                	unimp
 1d8:	14c0                	addi	s0,sp,612
 1da:	0000                	unimp
 1dc:	14f0                	addi	a2,sp,620
	...
 1e6:	0000                	unimp
 1e8:	1500                	addi	s0,sp,672
 1ea:	0000                	unimp
 1ec:	1500                	addi	s0,sp,672
 1ee:	0000                	unimp
 1f0:	1504                	addi	s1,sp,672
 1f2:	0000                	unimp
 1f4:	1548                	addi	a0,sp,676
	...
 1fe:	0000                	unimp
 200:	1508                	addi	a0,sp,672
 202:	0000                	unimp
 204:	150c                	addi	a1,sp,672
 206:	0000                	unimp
 208:	1510                	addi	a2,sp,672
 20a:	0000                	unimp
 20c:	1540                	addi	s0,sp,676
	...
 216:	0000                	unimp
 218:	1548                	addi	a0,sp,676
 21a:	0000                	unimp
 21c:	1590                	addi	a2,sp,736
 21e:	0000                	unimp
 220:	167c                	addi	a5,sp,812
 222:	0000                	unimp
 224:	1684                	addi	s1,sp,864
	...
 22e:	0000                	unimp
 230:	15d4                	addi	a3,sp,740
 232:	0000                	unimp
 234:	15d8                	addi	a4,sp,740
 236:	0000                	unimp
 238:	15dc                	addi	a5,sp,740
 23a:	0000                	unimp
 23c:	161c                	addi	a5,sp,800
	...
 246:	0000                	unimp
 248:	15dc                	addi	a5,sp,740
 24a:	0000                	unimp
 24c:	15e0                	addi	s0,sp,748
 24e:	0000                	unimp
 250:	15e4                	addi	s1,sp,748
 252:	0000                	unimp
 254:	1614                	addi	a3,sp,800
	...
 25e:	0000                	unimp
 260:	1644                	addi	s1,sp,804
 262:	0000                	unimp
 264:	1648                	addi	a0,sp,804
 266:	0000                	unimp
 268:	164c                	addi	a1,sp,804
 26a:	0000                	unimp
 26c:	167c                	addi	a5,sp,812
	...
 276:	0000                	unimp
 278:	1010                	addi	a2,sp,32
 27a:	0000                	unimp
 27c:	1288                	addi	a0,sp,352
 27e:	0000                	unimp
 280:	1288                	addi	a0,sp,352
 282:	0000                	unimp
 284:	1684                	addi	s1,sp,864
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
  10:	000c                	0xc
  12:	0000                	unimp
  14:	0000                	unimp
  16:	0000                	unimp
  18:	1010                	addi	a2,sp,32
  1a:	0000                	unimp
  1c:	0054                	addi	a3,sp,4
  1e:	0000                	unimp
  20:	0014                	0x14
  22:	0000                	unimp
  24:	0000                	unimp
  26:	0000                	unimp
  28:	1064                	addi	s1,sp,44
  2a:	0000                	unimp
  2c:	0048                	addi	a0,sp,4
  2e:	0000                	unimp
  30:	0e44                	addi	s1,sp,788
  32:	0210                	addi	a2,sp,256
  34:	0e40                	addi	s0,sp,788
  36:	0000                	unimp
  38:	0014                	0x14
  3a:	0000                	unimp
  3c:	0000                	unimp
  3e:	0000                	unimp
  40:	10ac                	addi	a1,sp,104
  42:	0000                	unimp
  44:	005c                	addi	a5,sp,4
  46:	0000                	unimp
  48:	0e4c                	addi	a1,sp,788
  4a:	0210                	addi	a2,sp,256
  4c:	0e48                	addi	a0,sp,788
  4e:	0000                	unimp
  50:	0014                	0x14
  52:	0000                	unimp
  54:	0000                	unimp
  56:	0000                	unimp
  58:	1108                	addi	a0,sp,160
  5a:	0000                	unimp
  5c:	0118                	addi	a4,sp,128
  5e:	0000                	unimp
  60:	0e44                	addi	s1,sp,788
  62:	0310                	addi	a2,sp,384
  64:	0110                	addi	a2,sp,128
  66:	000e                	c.slli	zero,0x3
  68:	000c                	0xc
  6a:	0000                	unimp
  6c:	0000                	unimp
  6e:	0000                	unimp
  70:	1220                	addi	s0,sp,296
  72:	0000                	unimp
  74:	0008                	0x8
  76:	0000                	unimp
  78:	000c                	0xc
  7a:	0000                	unimp
  7c:	0000                	unimp
  7e:	0000                	unimp
  80:	1228                	addi	a0,sp,296
  82:	0000                	unimp
  84:	0038                	addi	a4,sp,8
  86:	0000                	unimp
  88:	000c                	0xc
  8a:	0000                	unimp
  8c:	0000                	unimp
  8e:	0000                	unimp
  90:	1260                	addi	s0,sp,300
  92:	0000                	unimp
  94:	0028                	addi	a0,sp,8
  96:	0000                	unimp
  98:	002c                	addi	a1,sp,8
  9a:	0000                	unimp
  9c:	0000                	unimp
  9e:	0000                	unimp
  a0:	1288                	addi	a0,sp,352
  a2:	0000                	unimp
  a4:	03fc                	addi	a5,sp,460
  a6:	0000                	unimp
  a8:	0e44                	addi	s1,sp,788
  aa:	01a0                	addi	s0,sp,200
  ac:	4002                	0x4002
  ae:	0181                	addi	gp,gp,0
  b0:	0288                	addi	a0,sp,320
  b2:	0389                	addi	t2,t2,2
  b4:	0492                	slli	s1,s1,0x4
  b6:	06940593          	addi	a1,s0,105 # 18069 <src_buf+0x15909>
  ba:	0795                	addi	a5,a5,5
  bc:	0896                	slli	a7,a7,0x5
  be:	0a980997          	auipc	s3,0xa980
  c2:	0b99                	addi	s7,s7,6
  c4:	0c9a                	slli	s9,s9,0x6
  c6:	          	0xd9b
