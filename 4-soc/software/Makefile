# ===========================================================================
# Toolchain Configuration
# ===========================================================================
# Prefix for the RISC-V GCC toolchain. 
# Adjust this if your compiler is named differently (e.g., riscv-none-embed-).
CROSS_COMPILE = riscv64-unknown-elf-

CC      = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy
OBJDUMP = $(CROSS_COMPILE)objdump

# ===========================================================================
# Compiler Flags
# ===========================================================================
# -march=rv32i:  Target the 32-bit integer-only architecture (Matches MyCPU).
# -mabi=ilp32:   Use the 32-bit integer ABI.
# -O2:           Optimization level 2.
# -g:            Include debug information.
# -nostartfiles: Do not link standard system startup files (we use start.S).
CFLAGS  = -march=rv32i -mabi=ilp32 -O2 -g -Wall
LDFLAGS = -T link.ld -nostartfiles -nostdlib

# ===========================================================================
# Targets
# ===========================================================================
TARGET = dma_sg_test
SRCS   = start.S dma_sg_test.c

# Default rule: Build ELF, Binary, Hex, and Disassembly
all: $(TARGET).elf $(TARGET).bin $(TARGET).hex $(TARGET).asm

# Link object files to create the ELF executable
$(TARGET).elf: $(SRCS) link.ld
	$(CC) $(CFLAGS) $(LDFLAGS) $(SRCS) -o $@

# Create a raw binary file (for general loading)
$(TARGET).bin: $(TARGET).elf
	$(OBJCOPY) -O binary $< $@

# Create a Verilog Hex file (Crucial for Verilog $readmemh)
$(TARGET).hex: $(TARGET).elf
	$(OBJCOPY) -O verilog $< $@

# Create a disassembly file (For debugging inspection)
$(TARGET).asm: $(TARGET).elf
	$(OBJDUMP) -D $< > $@

# Clean build artifacts
clean:
	rm -f *.elf *.bin *.hex *.asm

.PHONY: all clean