#include <verilated.h>
#include <verilated_vcd_c.h>

#include <algorithm>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include "VTop.h"  // From Verilating "top.v"


class Memory
{
    std::vector<uint32_t> memory;

public:
    Memory(size_t size) : memory(size, 0) {}
    uint32_t read(size_t address)
    {
        address = address / 4;
        if (address >= memory.size()) {
            // Silently return 0 for out-of-range reads (address bus may have
            // arbitrary values when not actually reading)
            return 0;
        }

        return memory[address];
    }

    uint32_t readInst(size_t address)
    {
        address = address / 4;
        if (address >= memory.size()) {
            printf("invalid read Inst address 0x%08zx\n", address * 4);
            return 0;
        }

        return memory[address];
    }

    void write(size_t address, uint32_t value, bool write_strobe[4])
    {
        address = address / 4;
        uint32_t write_mask = 0;
        if (write_strobe[0])
            write_mask |= 0x000000FF;
        if (write_strobe[1])
            write_mask |= 0x0000FF00;
        if (write_strobe[2])
            write_mask |= 0x00FF0000;
        if (write_strobe[3])
            write_mask |= 0xFF000000;
        if (address >= memory.size()) {
            printf("invalid write address 0x%08zx\n", address * 4);
            return;
        }
        memory[address] =
            (memory[address] & ~write_mask) | (value & write_mask);
    }

    void load_binary(std::string const &filename, size_t load_address = 0x1000)
    {
        std::ifstream file(filename, std::ios::binary);
        if (!file) {
            throw std::runtime_error("Could not open file " + filename);
        }
        file.seekg(0, std::ios::end);
        size_t size = file.tellg();
        if (load_address + size > memory.size() * 4) {
            throw std::runtime_error(
                "File " + filename + " is too large (File is " +
                std::to_string(size) + " bytes. Memory is " +
                std::to_string(memory.size() * 4 - load_address) + " bytes.)");
        }
        file.seekg(0, std::ios::beg);
        for (int i = 0; i < size / 4; ++i) {
            file.read(reinterpret_cast<char *>(&memory[i + load_address / 4]),
                      sizeof(uint32_t));
        }
    }
};

class VCDTracer
{
    VerilatedVcdC *tfp = nullptr;

public:
    void enable(std::string const &filename, VTop &top)
    {
        Verilated::traceEverOn(true);
        tfp = new VerilatedVcdC;
        top.trace(tfp, 99);
        tfp->open(filename.c_str());
        tfp->set_time_resolution("1ps");
        tfp->set_time_unit("1ns");
        if (!tfp->isOpen()) {
            throw std::runtime_error("Failed to open VCD dump file " +
                                     filename);
        }
    }

    void dump(vluint64_t time)
    {
        if (tfp) {
            tfp->dump(time);
        }
    }

    ~VCDTracer()
    {
        if (tfp) {
            tfp->close();
            delete tfp;
        }
    }
};

uint32_t parse_number(std::string const &str)
{
    if (str.size() > 2) {
        auto &&prefix = str.substr(0, 2);
        if (prefix == "0x" || prefix == "0X") {
            return std::stoul(str.substr(2), nullptr, 16);
        }
    }
    return std::stoul(str);
}

class Simulator
{
    vluint64_t main_time = 0;
    vluint64_t max_sim_time = 10000;
    uint32_t halt_address = 0;
    size_t memory_words = 1024 * 1024;  // 4MB
    bool dump_vcd = false;
    std::unique_ptr<VTop> top;
    std::unique_ptr<VCDTracer> vcd_tracer;
    std::unique_ptr<Memory> memory;
    bool dump_signature = false;
    unsigned long signature_begin, signature_end;
    std::string signature_filename;
    std::string instruction_filename;

public:
    void parse_args(std::vector<std::string> const &args)
    {
        auto it = std::find(args.begin(), args.end(), "-halt");
        if (it != args.end()) {
            halt_address = parse_number(*(it + 1));
        }

        it = std::find(args.begin(), args.end(), "-memory");
        if (it != args.end()) {
            memory_words = std::stoull(*(it + 1));
        }

        it = std::find(args.begin(), args.end(), "-time");
        if (it != args.end()) {
            max_sim_time = std::stoull(*(it + 1));
        }

        it = std::find(args.begin(), args.end(), "-vcd");
        if (it != args.end()) {
            vcd_tracer->enable(*(it + 1), *top);
        }

        it = std::find(args.begin(), args.end(), "-signature");
        if (it != args.end()) {
            dump_signature = true;
            signature_begin = parse_number(*(it + 1));
            signature_end = parse_number(*(it + 2));
            signature_filename = *(it + 3);
        }

        it = std::find(args.begin(), args.end(), "-instruction");
        if (it != args.end()) {
            instruction_filename = *(it + 1);
        }
    }

    Simulator(std::vector<std::string> const &args)
        : top(std::make_unique<VTop>()),
          vcd_tracer(std::make_unique<VCDTracer>())
    {
        parse_args(args);
        memory = std::make_unique<Memory>(memory_words);
        if (!instruction_filename.empty()) {
            memory->load_binary(instruction_filename);
        }
    }

    void run()
    {
        top->reset = 1;
        top->clock = 0;
        top->io_instruction_valid = 1;
        top->eval();
        vcd_tracer->dump(main_time);
        uint32_t data_memory_read_word = 0;
        uint32_t inst_memory_read_word = 0;
        uint32_t counter = 0;
        uint32_t clocktime = 1;
        bool memory_write_strobe[4] = {false};
        int uart_write_time_counter = 0,
            uart_write_time_limit =
                4;  // every limit, an UART write completes; this is tricky part
        while (main_time < max_sim_time && !Verilated::gotFinish()) {
            ++main_time;
            ++counter;
            if (counter > clocktime) {
                top->clock = !top->clock;
                counter = 0;
            }
            if (main_time > 2) {
                top->reset = 0;
            }
            // top->io_mem_slave_read_data = memory_read_word;
            top->io_memory_bundle_read_data = data_memory_read_word;
            top->io_instruction = inst_memory_read_word;
            top->clock = !top->clock;
            top->eval();

            if (top->io_deviceSelect == 2 &&
                top->io_memory_bundle_write_enable) {
                if (uart_write_time_counter == 0)
                    std::cout << (char) top->io_memory_bundle_write_data
                              << std::flush;  // Output to UART
                uart_write_time_counter =
                    (uart_write_time_counter + 1) % uart_write_time_limit;
            } else {
                uart_write_time_counter = 0;
            }

            data_memory_read_word = memory->read(top->io_memory_bundle_address);
            inst_memory_read_word =
                memory->readInst(top->io_instruction_address);

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
            if (halt_address) {
                if (memory->read(halt_address) == 0xBABECAFE) {
                    break;
                }
            }

            // print simulation progress in percentage every 10%
            if (main_time % (max_sim_time / 10) == 0 && main_time > 0) {
                std::cerr << "Simulation progress: "
                          << (main_time * 100 / max_sim_time) << "%"
                          << std::endl;
            }
        }

        if (dump_signature) {
            char data[9] = {0};
            std::ofstream signature_file(signature_filename);
            for (size_t addr = signature_begin; addr < signature_end;
                 addr += 4) {
                snprintf(data, 9, "%08x", memory->read(addr));
                signature_file << data << std::endl;
            }
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
    Simulator simulator(args);
    simulator.run();
    return 0;
}
