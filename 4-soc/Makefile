# SPDX-License-Identifier: MIT
# MyCPU is freely redistributable under the MIT License. See the file
# "LICENSE" for information on usage and redistribution of this file.

# Include common build utilities
include ../common/build.mk

test:
	cd .. && sbt "project soc" test

verilator:
	@if java -version >/dev/null 2>&1; then \
		cd .. && PATH=$$HOME/.local/bin:$$PATH sbt "project soc" "runMain board.verilator.VerilogGenerator"; \
	else \
		echo "⚠️  Java runtime not found; using existing generated Verilog in verilog/verilator"; \
		if [ ! -f verilog/verilator/Top.v ]; then \
			echo "❌ Top.v missing; install Java (set JAVA_HOME) to regenerate Verilog"; \
			exit 1; \
		fi; \
	fi
	cd verilog/verilator && verilator --exe --cc sim.cpp Top.v \
		-CFLAGS "$$(sdl2-config --cflags)" \
		-LDFLAGS "$$(sdl2-config --libs)" && \
		make -C obj_dir -f VTop.mk

sim: verilator
	@if [ -z "$(BINARY)" ]; then \
		echo "Usage: make sim BINARY=<path/to/file.asmbin>"; \
		echo "Example: make sim BINARY=csrc/nyancat.asmbin"; \
		exit 1; \
	fi
	@if [ ! -f "$(BINARY)" ]; then \
		echo "Error: Binary file not found: $(BINARY)"; \
		exit 1; \
	fi
	@echo "🎮 Running simulation with $(BINARY)..."
	cd verilog/verilator/obj_dir && ./VTop -i ../../../$(BINARY)

check-vga: verilator
	@echo "🔄 Building nyancat binary..."
	@$(MAKE) -C csrc nyancat.asmbin >/dev/null
	@echo "🖥️  Running VGA test (nyancat)..."
	@echo "   - Press ESC or close window to exit"
	@echo "   - VGA output: 640×480 @ 72Hz"
	@echo ""
	cd verilog/verilator/obj_dir && ./VTop -i ../../../csrc/nyancat.asmbin
	@echo ""
	@echo "✅ VGA test complete!"

shell: verilator
	@echo "🔄 Building MyCPU shell binary..."
	@$(MAKE) -C csrc shell.asmbin >/dev/null
	@echo "🐚 Starting MyCPU interactive shell..."
	@echo "   - Type 'help' for available commands"
	@echo "   - Line editing: arrows, Home/End, Ctrl-A/E/U/K/W"
	@echo "   - Press Ctrl-C to exit"
	@echo ""
	cd verilog/verilator/obj_dir && ./VTop -i ../../../csrc/shell.asmbin --terminal --headless

# UART tests: loopback self-test + interactive echo test
check-uart: verilator
	@echo "🔄 Building UART binaries..."
	@$(MAKE) -C csrc uart.asmbin shell.asmbin >/dev/null
	@echo ""
	@echo "📡 [1/2] Running UART loopback test..."
	cd verilog/verilator/obj_dir && ./VTop -i ../../../csrc/uart.asmbin
	@echo ""
	@echo "📡 [2/2] Running UART echo test (may take ~30 seconds)..."
	@cd verilog/verilator/obj_dir && \
		printf "Test\r" | timeout 30 ./VTop -i ../../../csrc/shell.asmbin --terminal --headless 2>&1 | \
		tee /tmp/uart_test_output.txt | tail -5
	@echo ""
	@if grep -q "Test" /tmp/uart_test_output.txt; then \
		echo "✅ UART tests PASSED"; \
	else \
		echo "❌ UART echo test FAILED - 'Test' was not found in output"; \
		exit 1; \
	fi

indent:
	find . -name '*.scala' | xargs scalafmt
	clang-format -i verilog/verilator/*.cpp verilog/verilator/*.h
	clang-format -i csrc/*.[ch]

compliance: check-riscof
	@echo "Running RISCOF compliance tests for 4-soc (RV32I + Zicsr)..."
	@cd ../tests && RISCOF_WORK=riscof_work_4soc ./run-compliance.sh 4-soc
	@echo ""
	@echo "Copying results to results/ directory..."
	@$(RM) -r results
	@mkdir -p results
	@cp -r ../tests/riscof_work_4soc/* results/
	@echo "Cleaning up auto-generated RISCOF test files..."
	@$(RM) -f src/test/scala/riscv/compliance/ComplianceTest.scala
	@$(RM) -f src/main/resources/test.asmbin
	@echo "✅ Compliance tests complete. Results in results/"
	@echo "📊 View report: results/report.html"

clean:
	cd .. && sbt "project soc" clean
	$(MAKE) -C csrc clean
	$(RM) -r test_run_dir
	$(RM) -r verilog/verilator/obj_dir
	$(RM) verilog/verilator/*.v
	$(RM) verilog/verilator/*.fir
	$(RM) verilog/verilator/*.anno.json

distclean: clean
	$(RM) -r results

.PHONY: verilator test indent sim check-vga check-uart shell compliance clean distclean
