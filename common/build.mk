# SPDX-License-Identifier: MIT
# MyCPU is freely redistributable under the MIT License. See the file
# "LICENSE" for information on usage and redistribution of this file.
#
# Common Build System Utilities
# Provides shared validation targets for all MyCPU projects:
#   - check-riscof: Validate RISCOF installation
#   - check-toolchain: Validate RISC-V toolchain
#   - check-verilator: Validate Verilator installation
#   - check-deps: Validate all dependencies

# RISCOF validation - checks if riscof is available before compliance tests
.PHONY: check-riscof
check-riscof:
	@echo "Validating RISCOF installation..."
	@if ! command -v riscof > /dev/null 2>&1; then \
		echo "Error: riscof not found in PATH"; \
		echo ""; \
		echo "RISCOF (RISC-V Architectural Test Framework) is required for compliance tests."; \
		echo ""; \
		echo "Installation options:"; \
		echo "  1. Install via pip:"; \
		echo "     pip install riscof"; \
		echo ""; \
		echo "  2. If using virtualenv:"; \
		echo "     python -m pip install riscof"; \
		echo "     # Or use: python -m riscof --version"; \
		echo ""; \
		echo "  3. If already installed, add to PATH:"; \
		echo "     export PATH=\"\$$HOME/.local/bin:\$$PATH\""; \
		echo ""; \
		echo "  4. Verify installation:"; \
		echo "     which riscof && riscof --version"; \
		echo ""; \
		exit 1; \
	else \
		if ! riscof --version > /dev/null 2>&1; then \
			echo "Warning: riscof found but not executable or broken"; \
			echo "   Location: $$(command -v riscof)"; \
			echo "   Try: pip install --upgrade --force-reinstall riscof"; \
			exit 1; \
		fi; \
		echo "RISCOF found: $$(command -v riscof)"; \
		echo "   Version: $$(riscof --version 2>&1 | head -n1)"; \
		echo ""; \
	fi

# Toolchain validation - checks if RISC-V toolchain is available
.PHONY: check-toolchain
check-toolchain:
	@echo "Validating RISC-V toolchain..."
	@export PATH="$$HOME/rv/toolchain/bin:$$HOME/riscv/toolchain/bin:/opt/riscv/bin:$$PATH"; \
	FOUND=0; \
	for prefix in riscv-none-elf- riscv32-unknown-elf- riscv64-unknown-elf- riscv32-unknown-linux-gnu- riscv-none-embed-; do \
		if command -v $${prefix}gcc > /dev/null 2>&1; then \
			echo "Toolchain found: $$(command -v $${prefix}gcc)"; \
			FOUND=1; \
			break; \
		fi; \
	done; \
	if [ $$FOUND -eq 0 ]; then \
		echo "Error: RISC-V toolchain not found"; \
		echo ""; \
		echo "Searched in:"; \
		echo "  - \$$HOME/rv/toolchain/bin"; \
		echo "  - \$$HOME/riscv/toolchain/bin"; \
		echo "  - /opt/riscv/bin"; \
		echo "  - \$$PATH"; \
		echo ""; \
		echo "Please install RISC-V GNU toolchain."; \
		echo "See: https://github.com/riscv-collab/riscv-gnu-toolchain"; \
		echo ""; \
		exit 1; \
	fi

# Verilator validation
.PHONY: check-verilator
check-verilator:
	@echo "Validating Verilator installation..."
	@if ! command -v verilator > /dev/null 2>&1; then \
		echo "Error: verilator not found in PATH"; \
		echo ""; \
		echo "Verilator 5.042+ is required for simulation."; \
		echo "Visit: https://verilator.org/guide/latest/install.html"; \
		echo ""; \
		exit 1; \
	else \
		if ! verilator --version > /dev/null 2>&1; then \
			echo "Warning: verilator found but not executable or broken"; \
			echo "   Location: $$(command -v verilator)"; \
			echo "   Try: Check installation and permissions"; \
			exit 1; \
		fi; \
		echo "Verilator found: $$(command -v verilator)"; \
		echo "   Version: $$(verilator --version | head -n1)"; \
		echo ""; \
	fi

# Combined validation for all dependencies
.PHONY: check-deps
check-deps: check-toolchain check-verilator check-riscof
	@echo "All dependencies validated successfully"
	@echo ""
