# MyCPU is freely redistributable under the MIT License. See the file
# "LICENSE" for information on usage and redistribution of this file.

# Include common build utilities
include ../common/build.mk

# Default target: run tests
.DEFAULT_GOAL := test

SIM_TIME ?= 1000000
SIM_VCD ?= trace.vcd

test:
	cd .. && sbt "project pipeline" test

verilator:
	cd .. && PATH=$$HOME/.local/bin:$$PATH sbt "project pipeline" "runMain board.verilator.VerilogGenerator"
	cd verilog/verilator && verilator --trace --exe --cc sim.cpp Top.v && make -C obj_dir -f VTop.mk

sim: verilator
	cd verilog/verilator/obj_dir && ./VTop -vcd ../../../$(SIM_VCD) -time $(SIM_TIME) $(subst src/main/resources/,../../../src/main/resources/,$(SIM_ARGS))

indent:
	find . -name '*.scala' | xargs scalafmt
	clang-format -i verilog/verilator/*.cpp
	clang-format -i csrc/*.[ch]

compliance: check-riscof
	@echo "Running RISCOF compliance tests for 3-pipeline (RV32I + Zicsr)..."
	@cd ../tests && RISCOF_WORK=riscof_work_3pl ./run-compliance.sh 3-pipeline
	@echo ""
	@echo "Copying results to results/ directory..."
	@$(RM) -r results
	@mkdir -p results
	@cp -r ../tests/riscof_work_3pl/* results/
	@echo "Cleaning up auto-generated RISCOF test files..."
	@$(RM) -f src/test/scala/riscv/compliance/ComplianceTest.scala
	@$(RM) -f src/main/resources/test.asmbin
	@echo "✅ Compliance tests complete. Results in results/"
	@echo "📊 View report: results/report.html"

clean:
	cd .. && sbt "project pipeline" clean
	$(RM) -r test_run_dir
	$(RM) -r verilog/verilator/obj_dir
	$(RM) verilog/verilator/*.v
	$(RM) verilog/verilator/*.fir
	$(RM) verilog/verilator/*.anno.json
	$(RM) $(SIM_VCD)

distclean: clean
	$(RM) -r results

.PHONY: verilator test indent sim compliance clean distclean
