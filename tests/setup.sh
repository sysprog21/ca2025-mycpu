#!/usr/bin/env bash

# RISCOF Test Infrastructure Setup Script
# Clones required external repositories for RISCV compliance testing

set -euo pipefail

# Repository URLs
ARCH_TEST_REPO="https://github.com/riscv-non-isa/riscv-arch-test"
RV32EMU_REPO="https://github.com/sysprog21/rv32emu"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "==================================="
echo "RISCOF Test Infrastructure Setup"
echo "==================================="
echo ""

# Check for git
if ! command -v git &> /dev/null; then
    echo -e "${RED}Error: git not found${NC}"
    echo "Please install git to clone test repositories"
    exit 1
fi

# Clone riscv-arch-test if not present
if [ -d "riscv-arch-test" ]; then
    echo -e "${GREEN}✓${NC} riscv-arch-test already present"
else
    echo -e "${YELLOW}→${NC} Cloning riscv-arch-test (official RISC-V compliance tests)..."
    if git clone --depth=1 "$ARCH_TEST_REPO"; then
        echo -e "${GREEN}✓${NC} riscv-arch-test cloned successfully"
    else
        echo -e "${RED}✗${NC} Failed to clone riscv-arch-test"
        exit 1
    fi
fi

# Clone rv32emu if not present
if [ -d "rv32emu" ]; then
    echo -e "${GREEN}✓${NC} rv32emu already present"
else
    echo -e "${YELLOW}→${NC} Cloning rv32emu (reference model)..."
    if git clone --depth=1 "$RV32EMU_REPO"; then
        echo -e "${GREEN}✓${NC} rv32emu cloned successfully"
    else
        echo -e "${RED}✗${NC} Failed to clone rv32emu"
        exit 1
    fi
fi

# Verify test suite structure
echo ""
echo "Verifying test suite structure..."

if [ -d "riscv-arch-test/riscv-test-suite" ]; then
    TEST_COUNT=$(find riscv-arch-test/riscv-test-suite -name "*.S" | wc -l)
    echo -e "${GREEN}✓${NC} Test suite verified (found $TEST_COUNT test files)"
else
    echo -e "${RED}✗${NC} Test suite structure invalid"
    exit 1
fi

if [ -d "riscv-arch-test/riscv-test-suite/env" ]; then
    echo -e "${GREEN}✓${NC} Test environment verified"
else
    echo -e "${RED}✗${NC} Test environment missing"
    exit 1
fi

echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Run compliance tests: ./run-compliance.sh [PROJECT]"
echo "  2. Projects: 1-single-cycle, 2-mmio-trap, or 3-pipeline"
echo ""
echo "Example:"
echo "  ./run-compliance.sh 1-single-cycle"
echo ""
