# MyCPU is freely redistributable under the MIT License. See the file
# LICENSE" for information on usage and redistribution of this file.
#

# Include common build utilities
include ../common/build.mk

# Default target: run tests
.DEFAULT_GOAL := test

SIM_TIME ?= 1000000
SIM_VCD ?= trace.vcd
WRITE_VCD ?= 1

test:
	cd .. && sbt "project mmioTrap" test

verilator:
	cd .. && PATH=$$HOME/.local/bin:$$PATH sbt "project mmioTrap" "runMain board.verilator.VerilogGenerator"
	cd verilog/verilator && verilator --trace --exe --cc sim.cpp Top.v ../../src/main/resources/vsrc/TrueDualPortRAM32.v && make -C obj_dir -f VTop.mk

verilator-sdl2:
	cd .. && PATH=$$HOME/.local/bin:$$PATH sbt "project mmioTrap" "runMain board.verilator.VerilogGenerator"
	cd verilog/verilator && verilator --trace --exe --cc sim.cpp Top.v ../../src/main/resources/vsrc/TrueDualPortRAM32.v \
		-Wno-WIDTHEXPAND -Wno-WIDTH \
		-CFLAGS "-DENABLE_SDL2 $$(sdl2-config --cflags)" -LDFLAGS "$$(sdl2-config --libs)" && \
		make -C obj_dir -f VTop.mk

sim: verilator
	@if [ "$(WRITE_VCD)" = "0" ]; then \
		cd verilog/verilator/obj_dir && ./VTop -time $(SIM_TIME) $(subst src/main/resources/,../../../src/main/resources/,$(SIM_ARGS)); \
	else \
		cd verilog/verilator/obj_dir && ./VTop -vcd ../../../$(SIM_VCD) -time $(SIM_TIME) $(subst src/main/resources/,../../../src/main/resources/,$(SIM_ARGS)); \
	fi

demo: verilator-sdl2
	@echo "🐱 Starting VGA demo with nyancat animation..."
	@echo "   Display: 640×480@72Hz with SDL2 visualization"
	@echo "   Program: nyancat.asmbin (12-frame nyancat animation)"
	@echo "   Note: Frame upload + animation takes significant time"
	@echo "   Duration: 500M cycles (~5 minutes, includes full animation)"
	@echo ""
	cd verilog/verilator/obj_dir && ./VTop -vga -instruction ../../../src/main/resources/nyancat.asmbin -time 500000000
	@echo ""
	@echo "✅ Demo complete! You should have seen animated nyancat."

indent:
	find . -name '*.scala' | xargs scalafmt
	clang-format -i verilog/verilator/*.cpp
	clang-format -i csrc/*.[ch]

compliance: check-riscof
	@echo "Running RISCOF compliance tests for 2-mmio-trap (RV32I + Zicsr)..."
	@cd ../tests && RISCOF_WORK=riscof_work_2mt ./run-compliance.sh 2-mmio-trap
	@echo ""
	@echo "Copying results to results/ directory..."
	@$(RM) -r results
	@mkdir -p results
	@cp -r ../tests/riscof_work_2mt/* results/
	@echo "Cleaning up auto-generated RISCOF test files..."
	@$(RM) -f src/test/scala/riscv/compliance/ComplianceTest.scala
	@$(RM) -f src/main/resources/test.asmbin
	@echo "✅ Compliance tests complete. Results in results/"
	@echo "📊 View report: results/report.html"

clean:
	cd .. && sbt "project mmioTrap" clean
	$(RM) -r test_run_dir
	$(RM) -r verilog/verilator/obj_dir
	$(RM) verilog/verilator/*.v
	$(RM) verilog/verilator/*.fir
	$(RM) verilog/verilator/*.anno.json
	$(RM) $(SIM_VCD)

distclean: clean
	$(RM) -r results

.PHONY: verilator verilator-sdl2 test indent sim demo compliance clean distclean
