// SPDX-License-Identifier: MIT
// Nyancat animation program for VGA peripheral
// Uses delta-RLE compression for superior compression ratio

#include <stdint.h>

// Custom memory copy for bare-metal environment (no libc)
static inline void copy_buffer(uint8_t *dest, const uint8_t *src, int n)
{
    for (int i = 0; i < n; i++)
        dest[i] = src[i];
}

// VGA MMIO register addresses (base: 0x30000000)
#define VGA_BASE 0x30000000u
#define VGA_ID (VGA_BASE + 0x00)
#define VGA_CTRL (VGA_BASE + 0x04)
#define VGA_STATUS (VGA_BASE + 0x08)
#define VGA_UPLOAD_ADDR (VGA_BASE + 0x10)
#define VGA_STREAM_DATA (VGA_BASE + 0x14)
#define VGA_PALETTE(n) (VGA_BASE + 0x20 + ((n) << 2))

// Animation constants
#define FRAME_SIZE 4096    // 64×64 pixels
#define FRAME_COUNT 12     // Total animation frames
#define PIXELS_PER_WORD 8  // 8 4-bit pixels per 32-bit word
#define PALETTE_SIZE 14    // Nyancat color count
#define PALETTE_MAX 16     // VGA palette entries

// Opcode format constants
#define OPCODE_MASK 0xF0   // Extract opcode type
#define PARAM_MASK 0x0F    // Extract opcode parameter
#define END_OF_FRAME 0xFF  // Frame terminator

// Opcode types
#define OP_SET_COLOR 0x00        // Set current color
#define OP_SKIP_1 0x10           // Skip (1-16) - delta only
#define OP_REPEAT_1 0x20         // Repeat (1-16)
#define OP_SKIP_16 0x30          // Skip (16-256) - delta only
#define OP_REPEAT_16 0x30        // Repeat (16-256) - baseline only
#define OP_REPEAT_16_DELTA 0x40  // Repeat (16-256) - delta only
#define OP_SKIP_64 0x50          // Skip (64-1024) - delta only

// Nyancat color palette (6-bit RRGGBB values)
static const uint8_t nyancat_palette[14] = {
    0x01,  //  0: Dark blue background
    0x3F,  //  1: White
    0x00,  //  2: Black
    0x3E,  //  3: Light pink/beige
    0x3B,  //  4: Pink
    0x36,  //  5: Hot pink
    0x30,  //  6: Red
    0x38,  //  7: Orange
    0x3C,  //  8: Yellow
    0x0C,  //  9: Green
    0x0B,  // 10: Light blue
    0x17,  // 11: Purple
    0x2A,  // 12: Gray
    0x3A,  // 13: Peach
};

// Include compressed frame data (delta-RLE)
#include "nyancat-data.h"

// MMIO access functions
static inline void vga_write32(uint32_t addr, uint32_t val)
{
    *(volatile uint32_t *) addr = val;
}

static inline uint32_t vga_read32(uint32_t addr)
{
    return *(volatile uint32_t *) addr;
}

// Pack 8 4-bit pixels into a 32-bit word
static inline uint32_t pack8_pixels(const uint8_t *pixels)
{
    return (uint32_t) (pixels[0] & 0xF) | ((uint32_t) (pixels[1] & 0xF) << 4) |
           ((uint32_t) (pixels[2] & 0xF) << 8) |
           ((uint32_t) (pixels[3] & 0xF) << 12) |
           ((uint32_t) (pixels[4] & 0xF) << 16) |
           ((uint32_t) (pixels[5] & 0xF) << 20) |
           ((uint32_t) (pixels[6] & 0xF) << 24) |
           ((uint32_t) (pixels[7] & 0xF) << 28);
}

// Initialize VGA palette with nyancat colors
void vga_init_palette(void)
{
    for (int i = 0; i < PALETTE_SIZE; i++) {
        vga_write32(VGA_PALETTE(i), nyancat_palette[i] & 0x3F);
    }
    // Fill remaining palette entries with black
    for (int i = PALETTE_SIZE; i < PALETTE_MAX; i++) {
        vga_write32(VGA_PALETTE(i), 0x00);
    }
}

// Frame buffers for delta decompression
static uint8_t frame_buffer[FRAME_SIZE];       // Current frame buffer
static uint8_t prev_frame_buffer[FRAME_SIZE];  // Previous frame for delta

// Upload compressed data size (defined in header)
extern const uint8_t nyancat_compressed_data[];

#if NYANCAT_COMPRESSION_DELTA

// Delta frame decompression
// Frame 0 (baseline): 0x0X=SetColor, 0x2Y=Repeat(1-16), 0x3Y=Repeat*16(16-256)
// Frame 1-11 (delta): 0x0X=SetColor, 0x1Y=Skip(1-16), 0x2Y=Repeat(1-16),
//                      0x3Y=Skip*16(16-256), 0x4Y=Repeat*16(16-256),
//                      0x5Y=Skip*64(64-1024)
void vga_upload_frame_delta(int frame_index)
{
    // Set upload address to start of frame
    vga_write32(VGA_UPLOAD_ADDR, ((uint32_t) (frame_index & 0xF) << 16) | 0);

    // Get compressed data for this frame with bounds
    uint16_t offset = nyancat_frame_offsets[frame_index];
    uint16_t next_offset = (frame_index < FRAME_COUNT - 1)
                               ? nyancat_frame_offsets[frame_index + 1]
                               : sizeof(nyancat_compressed_data);
    const uint8_t *compressed_data = &nyancat_compressed_data[offset];
    const uint8_t *data_end = &nyancat_compressed_data[next_offset];

    if (frame_index == 0) {
        // Frame 0: baseline RLE decompression
        int output_index = 0;
        uint8_t current_color = 0;
        const uint8_t *p = compressed_data;

        while (output_index < FRAME_SIZE && p < data_end) {
            uint8_t opcode = *p++;

            if (opcode == END_OF_FRAME) {
                break;  // End of frame
            }
            if ((opcode & OPCODE_MASK) == OP_SET_COLOR) {
                current_color = opcode & PARAM_MASK;  // SetColor
            } else if ((opcode & OPCODE_MASK) == OP_REPEAT_1) {
                int count = (opcode & PARAM_MASK) + 1;  // Repeat (1-16)
                for (int i = 0; i < count && output_index < FRAME_SIZE; i++)
                    frame_buffer[output_index++] = current_color;
            } else if ((opcode & OPCODE_MASK) == OP_REPEAT_16) {
                int count =
                    ((opcode & PARAM_MASK) + 1) * 16;  // Repeat (16-256)
                for (int i = 0; i < count && output_index < FRAME_SIZE; i++)
                    frame_buffer[output_index++] = current_color;
            }
        }

        // Fill remaining with background if incomplete
        while (output_index < FRAME_SIZE)
            frame_buffer[output_index++] = 0;

    } else {
        // Frame 1-11: delta decompression
        // Start from previous frame
        copy_buffer(frame_buffer, prev_frame_buffer, FRAME_SIZE);

        int pos = 0;
        uint8_t current_color = 0;
        const uint8_t *p = compressed_data;

        while (pos < FRAME_SIZE && p < data_end) {
            uint8_t opcode = *p++;

            if (opcode == END_OF_FRAME) {
                break;  // End of frame
            }
            if ((opcode & OPCODE_MASK) == OP_SET_COLOR) {
                current_color = opcode & PARAM_MASK;  // SetColor
            } else if ((opcode & OPCODE_MASK) == OP_SKIP_1) {
                int skip = (opcode & PARAM_MASK) + 1;  // Skip unchanged (1-16)
                pos += skip;
            } else if ((opcode & OPCODE_MASK) == OP_REPEAT_1) {
                int count = (opcode & PARAM_MASK) + 1;  // Repeat changed (1-16)
                for (int i = 0; i < count && pos < FRAME_SIZE; i++)
                    frame_buffer[pos++] = current_color;
            } else if ((opcode & OPCODE_MASK) == OP_SKIP_16) {
                int skip = ((opcode & PARAM_MASK) + 1) *
                           16;  // Skip unchanged (16-256)
                pos += skip;
            } else if ((opcode & OPCODE_MASK) == OP_REPEAT_16_DELTA) {
                int count = ((opcode & PARAM_MASK) + 1) *
                            16;  // Repeat changed (16-256)
                for (int i = 0; i < count && pos < FRAME_SIZE; i++)
                    frame_buffer[pos++] = current_color;
            } else if ((opcode & OPCODE_MASK) == OP_SKIP_64) {
                int skip = ((opcode & PARAM_MASK) + 1) *
                           64;  // Skip unchanged (64-1024)
                pos += skip;
            }
        }
    }

    // Save current frame for next delta
    copy_buffer(prev_frame_buffer, frame_buffer, FRAME_SIZE);

    // Upload decompressed frame to VGA (512 words = 4096 pixels / 8)
    for (int i = 0; i < FRAME_SIZE; i += PIXELS_PER_WORD) {
        uint32_t packed = pack8_pixels(&frame_buffer[i]);
        vga_write32(VGA_STREAM_DATA, packed);
    }
}

#else

// Fallback: baseline opcode-RLE decompression
void vga_upload_frame_rle(int frame_index)
{
    // Set upload address to start of frame
    vga_write32(VGA_UPLOAD_ADDR, ((uint32_t) (frame_index & 0xF) << 16) | 0);

    // Get compressed data for this frame with bounds
    uint16_t offset = nyancat_frame_offsets[frame_index];
    uint16_t next_offset = (frame_index < FRAME_COUNT - 1)
                               ? nyancat_frame_offsets[frame_index + 1]
                               : sizeof(nyancat_compressed_data);
    const uint8_t *rle_data = &nyancat_compressed_data[offset];
    const uint8_t *data_end = &nyancat_compressed_data[next_offset];

    // Decompress frame into buffer
    int output_index = 0;
    uint8_t current_color = 0;
    const uint8_t *p = rle_data;

    while (output_index < FRAME_SIZE && p < data_end) {
        uint8_t opcode = *p++;

        if (opcode == END_OF_FRAME) {
            break;  // End of frame
        }
        if ((opcode & OPCODE_MASK) == OP_SET_COLOR) {
            current_color = opcode & PARAM_MASK;  // SetColor
        } else if ((opcode & OPCODE_MASK) == OP_REPEAT_1) {
            int count = (opcode & PARAM_MASK) + 1;  // Repeat (1-16)
            for (int i = 0; i < count && output_index < FRAME_SIZE; i++)
                frame_buffer[output_index++] = current_color;
        } else if ((opcode & OPCODE_MASK) == OP_REPEAT_16) {
            int count = ((opcode & PARAM_MASK) + 1) * 16;  // Repeat (16-256)
            for (int i = 0; i < count && output_index < FRAME_SIZE; i++)
                frame_buffer[output_index++] = current_color;
        }
    }

    // Fill remaining with background if incomplete
    while (output_index < FRAME_SIZE)
        frame_buffer[output_index++] = 0;

    // Upload decompressed frame to VGA
    for (int i = 0; i < FRAME_SIZE; i += PIXELS_PER_WORD) {
        uint32_t packed = pack8_pixels(&frame_buffer[i]);
        vga_write32(VGA_STREAM_DATA, packed);
    }
}

#endif

// Simple delay function (~20Hz frame rate)
// Use inline assembly to prevent compiler optimization
static inline void delay(uint32_t cycles)
{
    for (uint32_t i = 0; i < cycles; i++)
        __asm__ volatile("nop");
}

int main(void)
{
    // Verify VGA peripheral presence
    uint32_t id = vga_read32(VGA_ID);
    if (id != 0x56474131)
        return 1;

    // Initialize palette and enable display
    vga_init_palette();
    vga_write32(VGA_CTRL, 0x01);

#if NYANCAT_COMPRESSION_DELTA
    // Upload all frames (delta decompression)
    for (int frame = 0; frame < FRAME_COUNT; frame++) {
        vga_upload_frame_delta(frame);
        vga_write32(VGA_CTRL,
                    (frame << 4) | 0x01);  // Display frame as we upload
    }

    // Animate: cycle through frames infinitely
    for (uint32_t frame = 0;;) {
        vga_write32(VGA_CTRL, (frame << 4) | 0x01);
        delay(50000);
        frame = (frame + 1 < FRAME_COUNT) ? frame + 1 : 0;
    }
#else
    // Upload all frames (baseline RLE)
    for (int frame = 0; frame < FRAME_COUNT; frame++) {
        vga_upload_frame_rle(frame);
        vga_write32(VGA_CTRL,
                    (frame << 4) | 0x01);  // Display frame as we upload
    }

    // Animate: cycle through frames infinitely
    for (uint32_t frame = 0;;) {
        vga_write32(VGA_CTRL, (frame << 4) | 0x01);
        delay(50000);
        frame = (frame + 1 < FRAME_COUNT) ? frame + 1 : 0;
    }
#endif
}
