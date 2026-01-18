# SPDX-License-Identifier: MIT
# Top-level Makefile for MyCPU projects

# Include common build utilities
include common/build.mk

# Project directories
PROJECTS := 1-single-cycle 2-mmio-trap 3-pipeline 4-soc
ALL_MODULES := common $(PROJECTS)
TEST_DIR := tests

.PHONY: clean distclean

# Clean build artifacts from all projects
clean:
	@echo "Cleaning build artifacts from all projects..."
	@for project in $(PROJECTS); do \
		if [ -f $$project/Makefile ]; then \
			echo "Cleaning $$project..."; \
			$(MAKE) -C $$project clean 2>/dev/null || true; \
		fi; \
	done
	@echo "Clean complete."

# Deep clean: remove test results and all generated files
distclean: clean
	@echo "Performing deep clean..."
	@echo "Removing test work directories and artifacts..."
	@rm -rf $(TEST_DIR)/riscof_work
	@rm -rf $(TEST_DIR)/riscof_work_1sc
	@rm -rf $(TEST_DIR)/riscof_work_2mt
	@rm -rf $(TEST_DIR)/riscof_work_3pl
	@rm -rf $(TEST_DIR)/rv32emu
	@rm -rf $(TEST_DIR)/riscv-arch-test
	@echo "Removing RISCOF generated ISA/platform YAML files..."
	@rm -f $(TEST_DIR)/riscof_work/*.yaml
	@echo "Removing sbt build artifacts from all modules (common + projects)..."
	@for module in $(ALL_MODULES); do \
		echo "  Cleaning $$module build artifacts..."; \
		rm -rf $$module/target; \
		rm -rf $$module/project/target; \
		rm -rf $$module/project/project; \
		rm -rf $$module/.bloop; \
		rm -rf $$module/.metals; \
		rm -rf $$module/.vscode; \
		rm -f $$module/*.log; \
	done
	@echo "Removing auto-generated compliance test files..."
	@for project in $(PROJECTS); do \
		rm -f $$project/src/test/scala/riscv/compliance/ComplianceTest.scala; \
	done
	@echo "Removing temporary test resources..."
	@for project in $(PROJECTS); do \
		rm -f $$project/src/main/resources/test.asmbin; \
	done
	@echo "Removing Verilator generated files..."
	@for project in $(PROJECTS); do \
		rm -rf $$project/generated; \
		rm -f $$project/*.vcd; \
		rm -f $$project/*.fst; \
	done
	@echo "Removing sbt output logs from test directories..."
	@find $(TEST_DIR)/riscof_work -name "sbt_output.log" -delete 2>/dev/null || true
	@echo "Removing ChiselTest artifacts..."
	@rm -rf test_run_dir
	@echo ""
	@echo "Deep clean complete. All generated files removed."
	@echo "Source files and configuration preserved."
