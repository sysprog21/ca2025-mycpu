#include <verilated.h>
#include <verilated_vcd_c.h>

#include <algorithm>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include "VTop.h"  // From Verilating "top.v"

#ifdef ENABLE_SDL2
#include <SDL.h>
#endif

class Memory
{
    std::vector<uint32_t> memory;

public:
    Memory(size_t size) : memory(size, 0) {}
    uint32_t read(size_t address)
    {
        address = address / 4;
        if (address >= memory.size()) {
            // Silently return 0 for out-of-bounds reads (expected for stack
            // operations)
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
            // Silently ignore out-of-bounds writes (expected for stack
            // operations)
            return;
        }
        memory[address] =
            (memory[address] & ~write_mask) | (value & write_mask);
    }

    void load_binary(std::string const &filename, size_t load_address = 0x1000)
    {
        std::ifstream file(filename, std::ios::binary);
        if (!file)
            throw std::runtime_error("Could not open file " + filename);
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

constexpr uint32_t DEVICE_SELECT_BITS = 3;
constexpr uint32_t DEVICE_SHIFT = 32 - DEVICE_SELECT_BITS;
constexpr uint32_t DEVICE_MASK = (1u << DEVICE_SHIFT) - 1u;
constexpr uint32_t UART_BASE = 0x40000000u;
constexpr uint32_t TIMER_BASE = 0x80000000u;
constexpr uint32_t VGA_BASE = 0x30000000u;

class TimerMMIO
{
    uint32_t limit = 0;
    bool enabled = false;

public:
    void write(uint32_t offset, uint32_t value)
    {
        if (offset == 0x4) {
            limit = value;
        } else if (offset == 0x8) {
            enabled = value != 0;
        }
    }

    uint32_t read(uint32_t offset) const
    {
        if (offset == 0x4)
            return limit;
        if (offset == 0x8)
            return enabled ? 1u : 0u;
        return 0;
    }
};

class UartMMIO
{
    uint32_t baudrate = 115200;
    bool enabled = false;
    uint8_t last_rx = 0;
    std::string tx_log;

public:
    void write(uint32_t offset, uint32_t value)
    {
        switch (offset) {
        case 0x4:
            baudrate = value;
            break;
        case 0x8:
            enabled = value != 0;
            break;
        case 0x10: {
            uint8_t ch = static_cast<uint8_t>(value & 0xFF);
            if (enabled) {
                tx_log.push_back(static_cast<char>(ch));
                std::cout << static_cast<char>(ch) << std::flush;
            }
            break;
        }
        default:
            break;
        }
    }

    uint32_t read(uint32_t offset) const
    {
        if (offset == 0x4)
            return baudrate;
        if (offset == 0xC)
            return last_rx;
        return 0;
    }
};

#ifdef ENABLE_SDL2
class VGADisplay
{
    static constexpr int H_RES = 640;
    static constexpr int V_RES = 480;

    SDL_Window *window = nullptr;
    SDL_Renderer *renderer = nullptr;
    SDL_Texture *texture = nullptr;
    std::vector<uint8_t> framebuffer;
    bool prev_vsync = true;
    bool should_quit = false;

    // Color conversion: 2-bit VGA channel → 8-bit RGB
    // Maps 2-bit color values to 8-bit with even spacing:
    //   0b00 → 0   (0%)
    //   0b01 → 85  (33%)
    //   0b10 → 170 (67%)
    //   0b11 → 255 (100%)
    static constexpr uint8_t vga2bit_to_8bit(uint8_t val) { return val * 85; }

public:
    VGADisplay() : framebuffer(H_RES * V_RES * 4, 0)  // BGRA format
    {
        if (SDL_Init(SDL_INIT_VIDEO) < 0) {
            throw std::runtime_error(std::string("SDL_Init failed: ") +
                                     SDL_GetError());
        }

        window = SDL_CreateWindow(
            "VGA Display - MyCPU", SDL_WINDOWPOS_UNDEFINED,
            SDL_WINDOWPOS_UNDEFINED, H_RES, V_RES, SDL_WINDOW_SHOWN);
        if (!window) {
            SDL_Quit();
            throw std::runtime_error(std::string("SDL_CreateWindow failed: ") +
                                     SDL_GetError());
        }
        std::cout << "[SDL2] Window opened: 640x480 'VGA Display - MyCPU'"
                  << std::endl;
        std::cout << "[SDL2] Press ESC or close window to stop simulation early"
                  << std::endl;

        renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);
        if (!renderer) {
            SDL_DestroyWindow(window);
            SDL_Quit();
            throw std::runtime_error(
                std::string("SDL_CreateRenderer failed: ") + SDL_GetError());
        }

        SDL_SetRenderDrawColor(renderer, 0, 0, 0, SDL_ALPHA_OPAQUE);
        SDL_RenderClear(renderer);

        texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888,
                                    SDL_TEXTUREACCESS_STREAMING, H_RES, V_RES);
        if (!texture) {
            SDL_DestroyRenderer(renderer);
            SDL_DestroyWindow(window);
            SDL_Quit();
            throw std::runtime_error(std::string("SDL_CreateTexture failed: ") +
                                     SDL_GetError());
        }
    }

    ~VGADisplay()
    {
        if (texture)
            SDL_DestroyTexture(texture);
        if (renderer)
            SDL_DestroyRenderer(renderer);
        if (window)
            SDL_DestroyWindow(window);
        SDL_Quit();
    }

    // Update pixel using hardware-provided positions (Bug #6 fix)
    // Use x_pos/y_pos directly from VGA hardware instead of tracking with
    // hsync/vsync
    void update_pixel(uint8_t rrggbb,
                      uint8_t activevideo,
                      uint16_t x_pos,
                      uint16_t y_pos)
    {
        // Use hardware-provided pixel positions (already aligned with VGA
        // timing)
        if (activevideo && x_pos < H_RES && y_pos < V_RES) {
            int idx = (y_pos * H_RES + x_pos) * 4;
            // Convert 2-bit per channel (RRGGBB) to 8-bit RGB
            framebuffer[idx] = vga2bit_to_8bit(rrggbb & 0b11);             // B
            framebuffer[idx + 1] = vga2bit_to_8bit((rrggbb >> 2) & 0b11);  // G
            framebuffer[idx + 2] = vga2bit_to_8bit((rrggbb >> 4) & 0b11);  // R
            framebuffer[idx + 3] = 255;                                    // A
        }
    }

    // Detect vsync falling edge to trigger frame rendering
    void check_vsync(bool vsync)
    {
        // Vsync falling edge indicates frame complete
        if (!vsync && prev_vsync)
            render();
        prev_vsync = vsync;
    }

    // Render framebuffer to SDL window
    void render()
    {
        // Handle SDL events
        SDL_Event e;
        while (SDL_PollEvent(&e)) {
            if (e.type == SDL_QUIT)
                should_quit = true;
            // Support ESC key to quit as well
            if (e.type == SDL_KEYDOWN && e.key.keysym.sym == SDLK_ESCAPE)
                should_quit = true;
        }

        // Upload framebuffer to texture and display
        SDL_UpdateTexture(texture, nullptr, framebuffer.data(), H_RES * 4);
        SDL_RenderCopy(renderer, texture, nullptr, nullptr);
        SDL_RenderPresent(renderer);
    }

    bool quit_requested() const { return should_quit; }
};
#endif

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
        if (tfp)
            tfp->dump(time);
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
        if (prefix == "0x" || prefix == "0X")
            return std::stoul(str.substr(2), nullptr, 16);
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
    TimerMMIO timer;
    UartMMIO uart;
#ifdef ENABLE_SDL2
    std::unique_ptr<VGADisplay> vga_display;
    bool enable_vga = false;
    uint64_t last_render_time = 0;
#endif

public:
    void parse_args(std::vector<std::string> const &args)
    {
        auto it = std::find(args.begin(), args.end(), "-halt");
        if (it != args.end())
            halt_address = parse_number(*(it + 1));

        it = std::find(args.begin(), args.end(), "-memory");
        if (it != args.end())
            memory_words = std::stoull(*(it + 1));

        it = std::find(args.begin(), args.end(), "-time");
        if (it != args.end())
            max_sim_time = std::stoull(*(it + 1));

        it = std::find(args.begin(), args.end(), "-vcd");
        if (it != args.end())
            vcd_tracer->enable(*(it + 1), *top);

        it = std::find(args.begin(), args.end(), "-signature");
        if (it != args.end()) {
            dump_signature = true;
            signature_begin = parse_number(*(it + 1));
            signature_end = parse_number(*(it + 2));
            signature_filename = *(it + 3);
        }

        it = std::find(args.begin(), args.end(), "-instruction");
        if (it != args.end())
            instruction_filename = *(it + 1);

#ifdef ENABLE_SDL2
        it = std::find(args.begin(), args.end(), "-vga");
        if (it != args.end())
            enable_vga = true;
#endif
    }

    Simulator(std::vector<std::string> const &args)
        : top(std::make_unique<VTop>()),
          vcd_tracer(std::make_unique<VCDTracer>())
    {
        parse_args(args);
        memory = std::make_unique<Memory>(memory_words);
        if (!instruction_filename.empty())
            memory->load_binary(instruction_filename);
#ifdef ENABLE_SDL2
        if (enable_vga)
            vga_display = std::make_unique<VGADisplay>();
#endif
    }

    void run()
    {
        top->reset = 1;
        top->clock = 0;
        top->io_instruction_valid = 1;
#ifdef ENABLE_SDL2
        // VGA pixel clock (drive with system clock for simplicity)
        top->io_vga_pixclk = 0;
#endif
        top->eval();
        vcd_tracer->dump(main_time);
        uint32_t data_memory_read_word = 0;
        uint32_t inst_memory_read_word = 0;
        uint32_t timer_interrupt = 0;
        uint32_t counter = 0;
        uint32_t clocktime = 1;
        bool memory_write_strobe[4] = {false};
        while (main_time < max_sim_time && !Verilated::gotFinish()) {
            ++main_time;
            ++counter;
            if (counter > clocktime) {
                top->clock = !top->clock;
                counter = 0;
            }
            if (main_time > 2)
                top->reset = 0;
            // top->io_mem_slave_read_data = memory_read_word;
            top->io_memory_bundle_read_data = data_memory_read_word;
            top->io_instruction = inst_memory_read_word;
#ifdef ENABLE_SDL2
            // Toggle VGA pixel clock (synchronized with system clock)
            top->io_vga_pixclk = top->clock;
#endif
            top->eval();
            top->io_interrupt_flag = 0;

            uint32_t device_select = top->io_deviceSelect;
            uint32_t low_address = top->io_memory_bundle_address & DEVICE_MASK;
            uint32_t effective_address =
                (device_select << DEVICE_SHIFT) | low_address;
            bool is_uart = (effective_address & 0xF0000000u) == UART_BASE;
            bool is_timer = (effective_address & 0xF0000000u) == TIMER_BASE;
            bool is_vga = (effective_address & 0xF0000000u) == VGA_BASE;

            if (top->io_memory_bundle_write_enable) {
                memory_write_strobe[0] = top->io_memory_bundle_write_strobe_0;
                memory_write_strobe[1] = top->io_memory_bundle_write_strobe_1;
                memory_write_strobe[2] = top->io_memory_bundle_write_strobe_2;
                memory_write_strobe[3] = top->io_memory_bundle_write_strobe_3;
                if (device_select == 0) {
                    memory->write(effective_address,
                                  top->io_memory_bundle_write_data,
                                  memory_write_strobe);
                } else if (is_uart) {
                    uart.write(effective_address - UART_BASE,
                               top->io_memory_bundle_write_data);
                } else if (is_timer) {
                    timer.write(effective_address - TIMER_BASE,
                                top->io_memory_bundle_write_data);
                } else if (is_vga) {
                    // VGA is hardware-only, writes are ignored in simulator
                    // (handled by VGA Chisel module directly)
                }
            }

            if (device_select == 0) {
                data_memory_read_word = memory->read(effective_address);
            } else if (is_uart) {
                data_memory_read_word =
                    uart.read(effective_address - UART_BASE);
            } else if (is_timer) {
                data_memory_read_word =
                    timer.read(effective_address - TIMER_BASE);
            } else if (is_vga) {
                // VGA is hardware-only, reads return 0
                data_memory_read_word = 0;
            } else {
                data_memory_read_word = 0;
            }
            inst_memory_read_word =
                memory->readInst(top->io_instruction_address);
            vcd_tracer->dump(main_time);

#ifdef ENABLE_SDL2
            // Update VGA display using hardware-provided positions (Bug #6 fix)
            if (vga_display) {
                vga_display->update_pixel(top->io_vga_rrggbb,
                                          top->io_vga_activevideo,
                                          top->io_vga_x_pos, top->io_vga_y_pos);
                vga_display->check_vsync(top->io_vga_vsync);

                // Check if user requested to quit
                if (vga_display->quit_requested()) {
                    std::cout << "\n[SDL2] User closed window or pressed ESC - "
                                 "stopping simulation"
                              << std::endl;
                    break;
                }
            }
#endif

            if (halt_address) {
                if (memory->read(halt_address) == 0xBABECAFE)
                    break;
            }

            // print simulation progress in percentage every 1%
            if (main_time % (max_sim_time / 100) == 0) {
                std::cout << "Simulation progress: "
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

#ifdef ENABLE_SDL2
        // Final render to display last frame
        if (vga_display)
            vga_display->render();
#endif
    }

    ~Simulator()
    {
        if (top)
            top->final();
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
