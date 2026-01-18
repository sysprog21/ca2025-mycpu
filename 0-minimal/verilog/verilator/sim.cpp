// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

#include <verilated.h>
#include <verilated_vcd_c.h>

#include <algorithm>
#include <array>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include "VTop.h"

constexpr int TRACE_DEPTH = 99;
constexpr int RESET_CYCLES = 2;

// Represents the main memory of the simulated CPU.
class Memory
{
    std::vector<uint32_t> memory;

public:
    Memory(size_t size) : memory(size, 0) {}

    // Reads a 32-bit word from the specified byte address.
    uint32_t read(size_t address)
    {
        address /= 4;
        if (address >= memory.size()) {
            // Out-of-bounds reads are silently ignored, returning 0. This is
            // because the address bus may contain arbitrary values when not
            // actively reading.
            return 0;
        }
        return memory[address];
    }

    // Writes a 32-bit word to the specified byte address, respecting the byte
    // strobes.
    void write(size_t address,
               uint32_t value,
               const std::array<bool, 4> &write_strobe)
    {
        address /= 4;
        if (address >= memory.size()) {
            std::cerr << "Error: Invalid write address 0x" << std::hex
                      << address * 4 << std::dec << std::endl;
            return;
        }

        uint32_t write_mask = 0;
        for (size_t i = 0; i < 4; ++i) {
            if (write_strobe[i]) {
                write_mask |= (0xFF << (i * 8));
            }
        }

        memory[address] =
            (memory[address] & ~write_mask) | (value & write_mask);
    }

    // Loads a binary file into memory at a specified address.
    void load_binary(const std::string &filename, size_t load_address = 0x1000)
    {
        std::ifstream file(filename, std::ios::binary);
        if (!file) {
            throw std::runtime_error("Could not open file: " + filename);
        }

        file.seekg(0, std::ios::end);
        size_t size = file.tellg();
        file.seekg(0, std::ios::beg);

        if (load_address + size > memory.size() * 4) {
            throw std::runtime_error("File " + filename +
                                     " is too large for memory.");
        }

        // Read the file word by word (4 bytes at a time).
        for (size_t i = 0; i < size / 4; ++i) {
            file.read(reinterpret_cast<char *>(&memory[i + load_address / 4]),
                      sizeof(uint32_t));
        }
    }
};

// Manages VCD (Value Change Dump) tracing for Verilator simulations.
class VCDTracer
{
    std::unique_ptr<VerilatedVcdC> tfp;

public:
    VCDTracer() : tfp(nullptr) {}

    // Enables VCD tracing and opens the specified trace file.
    void enable(const std::string &filename, VTop &top)
    {
        Verilated::traceEverOn(true);
        tfp.reset(new VerilatedVcdC());
        top.trace(tfp.get(), TRACE_DEPTH);
        tfp->open(filename.c_str());
        if (!tfp->isOpen()) {
            throw std::runtime_error("Failed to open VCD dump file: " +
                                     filename);
        }
    }

    // Dumps the current signal values to the VCD file at the given simulation
    // time.
    void dump(vluint64_t time)
    {
        if (tfp) {
            tfp->dump(time);
        }
    }

    // Closes the VCD file upon destruction.
    ~VCDTracer()
    {
        if (tfp) {
            tfp->close();
        }
    }
};

// Parses a string as a number, supporting "0x" prefix for hexadecimal values.
uint32_t parse_number(const std::string &str)
{
    if (str.size() > 2 &&
        (str.substr(0, 2) == "0x" || str.substr(0, 2) == "0X")) {
        return std::stoul(str.substr(2), nullptr, 16);
    }
    return std::stoul(str);
}

// Main simulator class that orchestrates the Verilator simulation.
class Simulator
{
    std::unique_ptr<VTop> top;
    std::unique_ptr<VCDTracer> vcd_tracer;
    std::unique_ptr<Memory> memory;

    vluint64_t main_time = 0;
    vluint64_t max_sim_time = 10000;
    uint32_t halt_address = 0;
    size_t memory_words = 1024 * 1024;  // 4MB
    std::string instruction_filename;
    bool dump_signature = false;
    unsigned long signature_begin, signature_end;
    std::string signature_filename;

public:
    Simulator(const std::vector<std::string> &args)
        : top(new VTop()), vcd_tracer(new VCDTracer())
    {
        parse_args(args);
        memory.reset(new Memory(memory_words));
        if (!instruction_filename.empty()) {
            memory->load_binary(instruction_filename);
        }
    }

    // Parses command-line arguments to configure the simulation.
    void parse_args(const std::vector<std::string> &args)
    {
        for (auto it = args.begin(); it != args.end(); ++it) {
            if (*it == "-halt" && std::next(it) != args.end()) {
                halt_address = parse_number(*++it);
            } else if (*it == "-memory" && std::next(it) != args.end()) {
                memory_words = std::stoull(*++it);
            } else if (*it == "-time" && std::next(it) != args.end()) {
                max_sim_time = std::stoull(*++it);
            } else if (*it == "-vcd" && std::next(it) != args.end()) {
                vcd_tracer->enable(*++it, *top);
            } else if (*it == "-signature" &&
                       std::distance(it, args.end()) > 3) {
                dump_signature = true;
                signature_begin = parse_number(*++it);
                signature_end = parse_number(*++it);
                signature_filename = *++it;
            } else if (*it == "-instruction" && std::next(it) != args.end()) {
                instruction_filename = *++it;
            }
        }
    }

    // Runs the Verilator simulation loop.
    void run()
    {
        // Initialize simulation state.
        top->reset = 1;
        top->clock = 0;
        top->io_instruction_valid = 1;
        top->eval();
        vcd_tracer->dump(main_time);

        uint32_t data_memory_read_word = 0;
        uint32_t inst_memory_read_word = 0;
        std::array<bool, 4> memory_write_strobe = {{false}};

        // Main simulation loop.
        while (main_time < max_sim_time && !Verilated::gotFinish()) {
            main_time++;
            top->clock = !top->clock;

            if (main_time > RESET_CYCLES) {
                top->reset = 0;
            }

            top->io_memory_bundle_read_data = data_memory_read_word;
            top->io_instruction = inst_memory_read_word;
            top->eval();

            data_memory_read_word = memory->read(top->io_memory_bundle_address);
            inst_memory_read_word = memory->read(top->io_instruction_address);

            if (top->io_memory_bundle_write_enable) {
                memory_write_strobe[0] = top->io_memory_bundle_write_strobe_0;
                memory_write_strobe[1] = top->io_memory_bundle_write_strobe_1;
                memory_write_strobe[2] = top->io_memory_bundle_write_strobe_2;
                memory_write_strobe[3] = top->io_memory_bundle_write_strobe_3;
                memory->write(top->io_memory_bundle_address,
                              top->io_memory_bundle_write_data,
                              memory_write_strobe);
            }

            vcd_tracer->dump(main_time);

            if (halt_address && memory->read(halt_address) == 0xBABECAFE) {
                std::cout << "Halt condition met at address 0x" << std::hex
                          << halt_address << std::dec << std::endl;
                break;
            }

            if (main_time > 0 && max_sim_time > 10 &&
                main_time % (max_sim_time / 10) == 0) {
                std::cerr << "Simulation progress: "
                          << (main_time * 100 / max_sim_time) << "%"
                          << std::endl;
            }
        }

        if (dump_signature) {
            generate_signature();
        }
    }

    // Generates a signature file from a specified memory range.
    void generate_signature()
    {
        std::ofstream signature_file(signature_filename);
        if (!signature_file) {
            std::cerr << "Error: Could not open signature file "
                      << signature_filename << std::endl;
            return;
        }

        char data[9] = {0};
        for (size_t addr = signature_begin; addr < signature_end; addr += 4) {
            snprintf(data, 9, "%08x", memory->read(addr));
            signature_file << data << std::endl;
        }
    }

    ~Simulator()
    {
        if (top) {
            top->final();
        }
    }
};

int main(int argc, char **argv)
{
    Verilated::commandArgs(argc, argv);
    std::vector<std::string> args(argv, argv + argc);

    try {
        Simulator simulator(args);
        simulator.run();
    } catch (const std::exception &e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}
