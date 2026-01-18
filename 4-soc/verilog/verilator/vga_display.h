// SPDX-License-Identifier: MIT
// VGA Display - SDL2-based renderer for VGA peripheral output

#pragma once

#include <SDL.h>
#include <cstdint>
#include <memory>

class VGADisplay
{
    SDL_Window *window;
    SDL_Renderer *renderer;
    SDL_Texture *texture;

    static constexpr int VGA_WIDTH = 640;
    static constexpr int VGA_HEIGHT = 480;
    static constexpr int WINDOW_SCALE = 1;

    uint32_t framebuffer[VGA_HEIGHT][VGA_WIDTH];
    bool enabled;

public:
    VGADisplay()
        : window(nullptr), renderer(nullptr), texture(nullptr), enabled(false)
    {
        // Initialize framebuffer to black
        for (int y = 0; y < VGA_HEIGHT; y++) {
            for (int x = 0; x < VGA_WIDTH; x++) {
                framebuffer[y][x] = 0xFF000000;  // ARGB: opaque black
            }
        }
    }

    ~VGADisplay() { cleanup(); }

    bool init()
    {
        if (enabled)
            return true;

        auto try_init = []() { return SDL_Init(SDL_INIT_VIDEO) == 0; };

        if (!try_init()) {
            fprintf(stderr, "SDL_Init Error: %s\n", SDL_GetError());
            // Headless CI / no display: fall back to dummy driver so demo can
            // proceed
            SDL_SetHint(SDL_HINT_FRAMEBUFFER_ACCELERATION, "0");
            SDL_SetHint(SDL_HINT_RENDER_DRIVER, "software");
            SDL_setenv("SDL_VIDEODRIVER", "dummy", 1);
            if (!try_init()) {
                fprintf(stderr, "SDL_Init fallback (dummy) failed: %s\n",
                        SDL_GetError());
                return false;
            }
            fprintf(stderr,
                    "Using SDL dummy video driver (no visible window)\n");
        }

        window = SDL_CreateWindow(
            "MyCPU VGA Display - Nyancat", SDL_WINDOWPOS_CENTERED,
            SDL_WINDOWPOS_CENTERED, VGA_WIDTH * WINDOW_SCALE,
            VGA_HEIGHT * WINDOW_SCALE, SDL_WINDOW_SHOWN);

        if (!window) {
            fprintf(stderr, "SDL_CreateWindow Error: %s\n", SDL_GetError());
            SDL_Quit();
            return false;
        }

        renderer = SDL_CreateRenderer(
            window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
        if (!renderer) {
            fprintf(stderr, "SDL_CreateRenderer Error: %s\n", SDL_GetError());
            SDL_DestroyWindow(window);
            SDL_Quit();
            return false;
        }

        texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888,
                                    SDL_TEXTUREACCESS_STREAMING, VGA_WIDTH,
                                    VGA_HEIGHT);

        if (!texture) {
            fprintf(stderr, "SDL_CreateTexture Error: %s\n", SDL_GetError());
            SDL_DestroyRenderer(renderer);
            SDL_DestroyWindow(window);
            SDL_Quit();
            return false;
        }

        enabled = true;
        printf("SDL2 VGA Display initialized (%dx%d)\n", VGA_WIDTH, VGA_HEIGHT);
        return true;
    }

    void cleanup()
    {
        if (texture) {
            SDL_DestroyTexture(texture);
            texture = nullptr;
        }
        if (renderer) {
            SDL_DestroyRenderer(renderer);
            renderer = nullptr;
        }
        if (window) {
            SDL_DestroyWindow(window);
            window = nullptr;
        }
        if (enabled) {
            SDL_Quit();
            enabled = false;
        }
    }

    // Convert 6-bit RRGGBB to 32-bit ARGB
    static uint32_t rrggbb_to_argb(uint8_t rrggbb)
    {
        uint8_t rr = (rrggbb >> 4) & 0x3;
        uint8_t gg = (rrggbb >> 2) & 0x3;
        uint8_t bb = rrggbb & 0x3;

        // Scale 2-bit to 8-bit (0-3 -> 0-255)
        uint8_t r = (rr * 255) / 3;
        uint8_t g = (gg * 255) / 3;
        uint8_t b = (bb * 255) / 3;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // Update pixel at current position
    void update_pixel(uint16_t x, uint16_t y, uint8_t rrggbb, bool active)
    {
        if (!enabled || !active)
            return;

        if (x < VGA_WIDTH && y < VGA_HEIGHT)
            framebuffer[y][x] = rrggbb_to_argb(rrggbb);
    }

    // Render framebuffer to screen
    void render()
    {
        if (!enabled)
            return;

        // Update texture with framebuffer
        SDL_UpdateTexture(texture, nullptr, framebuffer,
                          VGA_WIDTH * sizeof(uint32_t));

        // Clear and render
        SDL_RenderClear(renderer);
        SDL_RenderCopy(renderer, texture, nullptr, nullptr);
        SDL_RenderPresent(renderer);
    }

    // Process SDL events (returns false if user closes window)
    bool poll_events()
    {
        if (!enabled)
            return true;

        SDL_Event event;
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT)
                return false;
            if (event.type == SDL_KEYDOWN) {
                if (event.key.keysym.sym == SDLK_ESCAPE)
                    return false;
            }
        }
        return true;
    }

    bool is_enabled() const { return enabled; }

    // Save current framebuffer as BMP file
    bool save_frame(const char *filename)
    {
        if (!enabled)
            return false;

        SDL_Surface *surface = SDL_CreateRGBSurfaceFrom(
            framebuffer, VGA_WIDTH, VGA_HEIGHT, 32, VGA_WIDTH * 4,
            0x00FF0000,  // R mask
            0x0000FF00,  // G mask
            0x000000FF,  // B mask
            0xFF000000   // A mask
        );

        if (!surface) {
            fprintf(stderr, "Failed to create surface: %s\n", SDL_GetError());
            return false;
        }

        int result = SDL_SaveBMP(surface, filename);
        SDL_FreeSurface(surface);

        if (result != 0) {
            fprintf(stderr, "Failed to save BMP %s: %s\n", filename,
                    SDL_GetError());
            return false;
        }

        return true;
    }
};
