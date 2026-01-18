CROSS_COMPILE ?= $(HOME)/rv/toolchain/bin/riscv-none-elf-

ASFLAGS = -march=rv32i_zicsr -mabi=ilp32
# Optimization required: -O2 produces large stack frames that overflow in deep call chains
# RV32I lacks hardware multiply/divide, so -O2 significantly reduces code size
CFLAGS = -O2 -Wall -fno-builtin -march=rv32i_zicsr -mabi=ilp32
LDFLAGS = --oformat=elf32-littleriscv

AS := $(CROSS_COMPILE)as
CC := $(CROSS_COMPILE)gcc
LD := $(CROSS_COMPILE)ld
OBJCOPY := $(CROSS_COMPILE)objcopy

# Program targets (add new programs here)
PROGRAMS := nyancat uart shell
BINARIES := $(PROGRAMS:%=%.asmbin)

%.asmbin: %.elf
	$(OBJCOPY) -O binary -j .text -j .data $< $@

.DEFAULT_GOAL := all

all: $(BINARIES)

# All programs use proper init.S with ABI compliance and .bss clearing
nyancat.asmbin: nyancat.c nyancat-data.h init.o link.lds
	$(CC) $(CFLAGS) -DNYANCAT_COMPRESSION_DELTA=$(NYANCAT_COMPRESSION_DELTA) -c -o nyancat.o nyancat.c
	$(CROSS_COMPILE)ld -o nyancat.elf -T link.lds $(LDFLAGS) nyancat.o init.o
	$(OBJCOPY) -O binary -j .text -j .data nyancat.elf $@

uart.asmbin: uart.c mmio.h init.o link.lds
	$(CC) $(CFLAGS) -c -o uart.o uart.c
	$(CROSS_COMPILE)ld -o uart.elf -T link.lds $(LDFLAGS) uart.o init.o
	$(OBJCOPY) -O binary -j .text -j .data uart.elf $@

shell.asmbin: shell.c mmio.h init.o link.lds
	$(CC) $(CFLAGS) -c -o shell.o shell.c
	$(CROSS_COMPILE)ld -o shell.elf -T link.lds $(LDFLAGS) shell.o init.o
	$(OBJCOPY) -O binary -j .text -j .data shell.elf $@

init.o: init.S
	$(AS) -R $(ASFLAGS) -o $@ $<

# Generate nyancat animation data from upstream source
# Configure compression mode via NYANCAT_COMPRESSION_DELTA (default: 1 for delta-RLE)
# - NYANCAT_COMPRESSION_DELTA=1: delta-RLE compression (91% reduction, 4.7KB)
# - NYANCAT_COMPRESSION_DELTA=0: baseline RLE (87% reduction, 6.7KB)
NYANCAT_COMPRESSION_DELTA ?= 1

nyancat-data.h: ../../scripts/gen-nyancat-data.py
ifeq ($(NYANCAT_COMPRESSION_DELTA),1)
	python3 $< --delta --output $@
else
	python3 $< --output $@
endif

# Nyancat build depends on generated data
nyancat.o: nyancat.c nyancat-data.h

update: $(BINARIES)
	cp -f $(BINARIES) ../src/main/resources
	@echo ""
	@echo "Updated $(BINARIES) to ../src/main/resources"

clean:
	$(RM) *.o *.elf *.asmbin init_minimal.S nyancat-data.h

# Convenience targets (prevent implicit rule interference)
$(PROGRAMS): %: %.asmbin

.PHONY: all update clean $(PROGRAMS)
