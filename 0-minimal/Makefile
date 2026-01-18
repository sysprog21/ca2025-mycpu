# MyCPU is freely redistributable under the MIT License. See the file
# LICENSE" for information on usage and redistribution of this file.

# Variables
SBT = cd .. && sbt "project minimal"
PYTHON ?= python3

SRC_DIR := src/main/resources
VERILATOR_DIR := verilog/verilator
OBJ_DIR := $(VERILATOR_DIR)/obj_dir

SIM_TIME ?= 1000000
SIM_VCD ?= trace.vcd
JIT_BINARY := $(SRC_DIR)/jit.asmbin

# Primary Targets
.PHONY: all test verilator sim

all: test

test:
	$(SBT) test

verilator:
	@mkdir -p $(VERILATOR_DIR)
	@test -f $(VERILATOR_DIR)/sim.cpp || { echo "ERROR: $(VERILATOR_DIR)/sim.cpp missing"; exit 1; }
	# The following command assumes verilator is in ~/.local/bin
	cd .. && PATH=$$HOME/.local/bin:$$PATH sbt "project minimal" "runMain board.verilator.VerilogGenerator"
	cd $(VERILATOR_DIR) && verilator --trace --exe --cc sim.cpp Top.v && make -C obj_dir -f VTop.mk CXXFLAGS+="-std=c++17 -Wall"

sim: verilator
	@echo "Running Verilator simulation for $(JIT_BINARY)..."
	cd $(OBJ_DIR) && ./VTop -vcd ../../../$(SIM_VCD) -time $(SIM_TIME) -instruction ../../../$(JIT_BINARY)
	@$(PYTHON) scripts/analyze_trace.py $(SIM_VCD)


# Utility Targets
.PHONY: indent clean distclean

indent:
	find . -name '*.scala' | xargs scalafmt
	clang-format -i $(VERILATOR_DIR)/*.cpp csrc/*.[ch] 2>/dev/null || true

clean:
	$(SBT) clean
	rm -rf test_run_dir $(OBJ_DIR)
	rm -f $(VERILATOR_DIR)/*.v \
	      $(VERILATOR_DIR)/*.fir \
	      $(VERILATOR_DIR)/*.anno.json \
	      verilog/*.txt \
	      $(SRC_DIR)/*.o \
	      $(SIM_VCD)

distclean: clean
