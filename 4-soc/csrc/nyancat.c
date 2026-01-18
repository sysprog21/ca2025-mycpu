// SPDX-License-Identifier: MIT
// Nyancat animation program for VGA peripheral (4-soc version)
// Uses delta-RLE compression for superior compression ratio
// When USE_PREPACKED_FRAMES=1, uses host-prepacked frame words.

#include <stdint.h>

#include "mmio.h"

// Enable prepacked frames to guarantee correct pixels in SDL demo
#define USE_PREPACKED_FRAMES 1

#if USE_PREPACKED_FRAMES
#include "nyancat-frames.h"
#endif

#if !USE_PREPACKED_FRAMES
// Custom memory copy for bare-metal environment (no libc)
// Only needed for delta decompression when not using prepacked frames
static inline void copy_buffer(uint8_t *dest, const uint8_t *src, int n)
{
    for (int i = 0; i < n; i++)
        dest[i] = src[i];
}
#endif

// Animation constants (use VGA constants from mmio.h where applicable)
#define FRAME_SIZE VGA_FRAME_SIZE
#define FRAME_COUNT VGA_NUM_FRAMES
#define PIXELS_PER_WORD VGA_PIXELS_PER_WORD
#define WORDS_PER_FRAME VGA_WORDS_PER_FRAME
#define PALETTE_SIZE 14  // Nyancat color count
#define PALETTE_MAX 16   // VGA palette entries

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

// Initialize VGA palette with nyancat colors
void vga_init_palette(void)
{
    for (int i = 0; i < PALETTE_SIZE; i++) {
        vga_write32(VGA_ADDR_PALETTE(i), nyancat_palette[i] & 0x3F);
    }
    // Fill remaining palette entries with black
    for (int i = PALETTE_SIZE; i < PALETTE_MAX; i++) {
        vga_write32(VGA_ADDR_PALETTE(i), 0x00);
    }
}

#if !USE_PREPACKED_FRAMES
// Frame buffers for delta decompression (8KB total)
// Only needed when runtime decompression is used
static uint8_t frame_buffer[FRAME_SIZE];       // Current frame buffer
static uint8_t prev_frame_buffer[FRAME_SIZE];  // Previous frame for delta
#endif

// Compressed nyancat frames from nyancat-frames.h; offsets index into this
// array
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
    vga_write32(VGA_ADDR_UPLOAD_ADDR,
                ((uint32_t) (frame_index & 0xF) << 16) | 0);

#if USE_PREPACKED_FRAMES
    // Directly upload prepacked words for this frame (saves 8KB .bss)
    for (int i = 0; i < WORDS_PER_FRAME; i++) {
        vga_write32(VGA_ADDR_STREAM_DATA, nyancat_frames[frame_index][i]);
    }
#else
    // Runtime delta-RLE decompression (requires frame_buffer and
    // prev_frame_buffer)

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
            if (opcode == END_OF_FRAME)
                break;  // End of frame

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
            if (opcode == END_OF_FRAME)
                break;  // End of frame

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
        uint32_t packed = vga_pack8_pixels(&frame_buffer[i]);
        vga_write32(VGA_ADDR_STREAM_DATA, packed);
    }
#endif
}

#else

// Fallback: baseline opcode-RLE decompression
void vga_upload_frame_rle(int frame_index)
{
    // Set upload address to start of frame
    vga_write32(VGA_ADDR_UPLOAD_ADDR,
                ((uint32_t) (frame_index & 0xF) << 16) | 0);

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
        if (opcode == END_OF_FRAME)
            break;  // End of frame

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
        uint32_t packed = vga_pack8_pixels(&frame_buffer[i]);
        vga_write32(VGA_ADDR_STREAM_DATA, packed);
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
    uint32_t id = vga_read32(VGA_ADDR_ID);
    if (id != VGA_EXPECTED_ID)
        return 1;

    // Initialize palette and enable display
    vga_init_palette();
    vga_write32(VGA_ADDR_CTRL, 0x01);

#if NYANCAT_COMPRESSION_DELTA
    // Upload all frames (delta decompression) while keeping frame 0 displayed
    for (int frame = 0; frame < FRAME_COUNT; frame++)
        vga_upload_frame_delta(frame);

    // Animate: cycle through frames infinitely
    for (uint32_t frame = 0;;) {
        vga_write32(VGA_ADDR_CTRL, (frame << 4) | 0x01);
        delay(50000);
        frame = (frame + 1 < FRAME_COUNT) ? frame + 1 : 0;
    }
#else
    // Upload all frames (baseline RLE) while keeping frame 0 displayed
    for (int frame = 0; frame < FRAME_COUNT; frame++)
        vga_upload_frame_rle(frame);

    // Animate: cycle through frames infinitely
    for (uint32_t frame = 0;;) {
        vga_write32(VGA_ADDR_CTRL, (frame << 4) | 0x01);
        delay(50000);
        frame = (frame + 1 < FRAME_COUNT) ? frame + 1 : 0;
    }
#endif
}
