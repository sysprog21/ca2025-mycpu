CROSS_COMPILE ?= riscv64-unknown-elf-

ASFLAGS = -march=rv32i -mabi=ilp32
CFLAGS = -O0 -Wall -march=rv32i -mabi=ilp32
LDFLAGS = --oformat=elf32-littleriscv

AS := $(CROSS_COMPILE)as
CC := $(CROSS_COMPILE)gcc
LD := $(CROSS_COMPILE)ld
OBJCOPY := $(CROSS_COMPILE)objcopy

%.o: %.S
	$(AS) -R $(ASFLAGS) -o $@ $<
%.elf: %.S
	$(AS) -R $(ASFLAGS) -o $(@:.elf=.o) $<
	$(CROSS_COMPILE)ld -o $@ -T link.lds $(LDFLAGS) $(@:.elf=.o)
%.elf: %.c init.o
	$(CC) $(CFLAGS) -c -o $(@:.elf=.o) $<
	$(CROSS_COMPILE)ld -o $@ -T link.lds $(LDFLAGS) $(@:.elf=.o) init.o

%.asmbin: %.elf
	$(OBJCOPY) -O binary -j .text -j .data $< $@

BINS = \
	fibonacci.asmbin \
	quicksort.asmbin \
	sb.asmbin \
	irqtrap.asmbin \
	uart.asmbin

# Clear the .DEFAULT_GOAL special variable, so that the following turns
# to the first target after .DEFAULT_GOAL is not set.
.DEFAULT_GOAL :=

all: $(BINS)

# Nyancat has minimal init (no trap handler) for correct memory layout
nyancat.asmbin: nyancat.c nyancat-data.h init_minimal.o
	$(CC) $(CFLAGS) -c -o nyancat.o nyancat.c
	$(CROSS_COMPILE)ld -o nyancat.elf -T link.lds $(LDFLAGS) nyancat.o init_minimal.o
	$(OBJCOPY) -O binary -j .text -j .data nyancat.elf $@

init_minimal.o: init_minimal.S
	$(AS) -R $(ASFLAGS) -o $@ $<

init_minimal.S:
	@printf '.section .text.init\n.globl _start\n_start:\n  li sp, 0x400000\n  call main\nloop:\n  j loop\n' > $@

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

update: $(BINS) nyancat.asmbin
	cp -f $(BINS) nyancat.asmbin ../src/main/resources
	@echo ""
	@echo "Updated $(words $(BINS)) test programs + nyancat.asmbin to ../src/main/resources"

clean:
	$(RM) *.o *.elf *.asmbin init_minimal.S nyancat-data.h

.PHONY: all update clean
