# MyCPU is freely redistributable under the MIT License. See the file
# LICENSE" for information on usage and redistribution of this file.

# Include common build utilities
include ../common/build.mk

# Default target: run tests
.DEFAULT_GOAL := test

SIM_TIME ?= 1000000
SIM_VCD ?= trace.vcd
WRITE_VCD ?= 1

test:
	cd .. && sbt "project singleCycle" test

verilator:
	cd .. && PATH=$$HOME/.local/bin:$$PATH sbt "project singleCycle" "runMain board.verilator.VerilogGenerator"
	cd verilog/verilator && verilator --trace --exe --cc sim.cpp Top.v && make -C obj_dir -f VTop.mk

sim: verilator
	@if [ "$(WRITE_VCD)" = "0" ]; then \
		cd verilog/verilator/obj_dir && ./VTop -time $(SIM_TIME) $(subst src/main/resources/,../../../src/main/resources/,$(SIM_ARGS)); \
	else \
		cd verilog/verilator/obj_dir && ./VTop -vcd ../../../$(SIM_VCD) -time $(SIM_TIME) $(subst src/main/resources/,../../../src/main/resources/,$(SIM_ARGS)); \
	fi

indent:
	find . -name '*.scala' | xargs scalafmt
	clang-format -i verilog/verilator/*.cpp
	clang-format -i csrc/*.[ch]

compliance: check-riscof
	@echo "Running RISCOF compliance tests for 1-single-cycle (RV32I)..."
	@cd ../tests && RISCOF_WORK=riscof_work_1sc ./run-compliance.sh 1-single-cycle
	@echo ""
	@echo "Copying results to results/ directory..."
	@$(RM) -r results
	@mkdir -p results
	@cp -r ../tests/riscof_work_1sc/* results/
	@echo "Cleaning up auto-generated RISCOF test files..."
	@$(RM) -f src/test/scala/riscv/compliance/ComplianceTest.scala
	@$(RM) -f src/main/resources/test.asmbin
	@echo "✅ Compliance tests complete. Results in results/"
	@echo "📊 View report: results/report.html"

clean:
	cd .. && sbt "project singleCycle" clean
	$(RM) -r test_run_dir
	$(RM) -r verilog/verilator/obj_dir
	$(RM) verilog/verilator/*.v
	$(RM) verilog/verilator/*.fir
	$(RM) verilog/verilator/*.anno.json
	$(RM) $(SIM_VCD)

distclean: clean
	$(RM) -r results

.PHONY: verilator test indent sim compliance clean distclean
