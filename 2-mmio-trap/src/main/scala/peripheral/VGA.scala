// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * VGA peripheral with MMIO framebuffer interface and dual-clock CDC
 *
 * Memory map (Base: 0x30000000):
 *   0x00: ID          - Peripheral identification (RO: 0x56474131 = 'VGA1')
 *   0x04: CTRL        - Display enable, blank, swap request, frame select, interrupt enable
 *   0x08: STATUS      - Vblank status, safe to swap, upload busy, current frame
 *   0x0C: INTR_STATUS - Vblank interrupt flag (W1C)
 *   0x10: UPLOAD_ADDR - Framebuffer upload address (nibble index + frame)
 *   0x14: STREAM_DATA - 8 pixels packed in 32-bit word (auto-increment)
 *   0x20-0x5C: PALETTE[0-15] - 6-bit VGA colors (RRGGBB)
 *
 * VGA timing: 640×480 @ 72Hz
 *   H_TOTAL=832, V_TOTAL=520, pixel clock=31.5 MHz
 *
 * Animation: 12 frames of 64×64 pixels (4-bit palette indices)
 *   Display: 6× scaled to 384×384 (centered in 640×480)
 *   Left margin: 128, Top margin: 48
 *
 * Clock domains:
 *   - CPU clock (sysclk): MMIO registers, palette, upload logic
 *   - Pixel clock (pixclk): VGA sync generator, framebuffer read, rendering
 */
class VGA extends Module {
  val io = IO(new Bundle {
    val bundle      = new RAMBundle      // MMIO interface (CPU clock domain)
    val pixClock    = Input(Clock())     // VGA pixel clock (31.5 MHz)
    val hsync       = Output(Bool())     // Horizontal sync
    val vsync       = Output(Bool())     // Vertical sync
    val rrggbb      = Output(UInt(6.W))  // 6-bit color output
    val activevideo = Output(Bool())     // Active display region
    val intr        = Output(Bool())     // Interrupt output (vblank)
    val x_pos       = Output(UInt(10.W)) // Current pixel X position
    val y_pos       = Output(UInt(10.W)) // Current pixel Y position
  })

  // ============ VGA Timing Parameters ============
  // 640×480 @ 72Hz, pixel clock = 31.5 MHz
  val H_ACTIVE = 640
  val H_FP     = 24                              // Front porch
  val H_SYNC   = 40                              // Sync pulse width
  val H_BP     = 128                             // Back porch
  val H_TOTAL  = H_ACTIVE + H_FP + H_SYNC + H_BP // 832

  val V_ACTIVE = 480
  val V_FP     = 9                               // Front porch
  val V_SYNC   = 3                               // Sync pulse width
  val V_BP     = 28                              // Back porch
  val V_TOTAL  = V_ACTIVE + V_FP + V_SYNC + V_BP // 520

  // Display scaling and positioning (6× scaling as per design spec)
  val FRAME_WIDTH    = 64
  val FRAME_HEIGHT   = 64
  val SCALE_FACTOR   = 6                               // 6× scaling: 64×64 → 384×384 (fits cleanly in 640×480)
  val DISPLAY_WIDTH  = FRAME_WIDTH * SCALE_FACTOR      // 64×6 = 384
  val DISPLAY_HEIGHT = FRAME_HEIGHT * SCALE_FACTOR     // 64×6 = 384
  val LEFT_MARGIN    = (H_ACTIVE - DISPLAY_WIDTH) / 2  // Horizontal center: (640-384)/2 = 128
  val TOP_MARGIN     = (V_ACTIVE - DISPLAY_HEIGHT) / 2 // Vertical center: (480-384)/2 = 48

  // Framebuffer parameters
  val NUM_FRAMES       = 12
  val PIXELS_PER_FRAME = 4096                          // 64×64
  val TOTAL_PIXELS     = NUM_FRAMES * PIXELS_PER_FRAME // 49152
  val WORDS_PER_FRAME  = PIXELS_PER_FRAME / 8          // 512 words (8 pixels per 32-bit word)
  val TOTAL_WORDS      = NUM_FRAMES * WORDS_PER_FRAME  // 6144 words
  val ADDR_WIDTH       = 13                            // log2(6144) rounded up

  // ============ Framebuffer RAM ============
  // Dual-port, dual-clock RAM: Write on CPU clock, Read on Pixel clock
  val framebuffer = Module(new TrueDualPortRAM32(TOTAL_WORDS, ADDR_WIDTH))

  // ============ CPU Clock Domain (sysclk) ============
  val sysClk = clock

  // MMIO Registers
  val ctrlReg       = RegInit(0.U(32.W)) // CTRL register
  val intrStatusReg = RegInit(0.U(32.W)) // INTR_STATUS register (W1C)
  val uploadAddrReg = RegInit(0.U(32.W)) // UPLOAD_ADDR register

  // Color palette (16 entries × 6-bit)
  val paletteReg = Reg(Vec(16, UInt(6.W)))

  // Control register bit fields
  val ctrl_en        = ctrlReg(0)
  val ctrl_blank     = ctrlReg(1)
  val ctrl_swap_req  = ctrlReg(2)
  val ctrl_frame_sel = ctrlReg(7, 4)
  val ctrl_vblank_ie = ctrlReg(8)

  // Cross-clock-domain wires (declared at module scope for CDC)
  val wire_in_vblank  = Wire(Bool())
  val wire_curr_frame = Wire(UInt(4.W))

  withClock(sysClk) {

    // Upload address register bit fields
    val upload_pix_addr = uploadAddrReg(15, 0)
    val upload_frame    = uploadAddrReg(19, 16)

    // MMIO address decode (mask to get offset within peripheral)
    val addr             = io.bundle.address & 0xff.U // VGA registers at 0x00-0x5F
    val addr_id          = addr === 0x00.U
    val addr_ctrl        = addr === 0x04.U
    val addr_status      = addr === 0x08.U
    val addr_intr_status = addr === 0x0c.U
    val addr_upload_addr = addr === 0x10.U
    val addr_stream_data = addr === 0x14.U
    val addr_palette     = (addr >= 0x20.U) && (addr < 0x60.U)
    val palette_idx      = (addr - 0x20.U) >> 2

    // CDC: Synchronize status signals from pixel domain to CPU domain
    val vblank_sync1      = RegNext(wire_in_vblank)
    val vblank_synced     = RegNext(vblank_sync1)
    val curr_frame_sync1  = RegNext(wire_curr_frame)
    val curr_frame_synced = RegNext(curr_frame_sync1)

    // Status signals for MMIO reads
    val status_in_vblank    = vblank_synced
    val status_safe_to_swap = vblank_synced // Safe to swap during vblank
    val status_upload_busy  = false.B       // Upload is never busy (write-through)
    val status_curr_frame   = curr_frame_synced

    // Vblank interrupt: Edge detection
    val vblank_prev        = RegNext(vblank_synced)
    val vblank_rising_edge = vblank_synced && !vblank_prev

    when(vblank_rising_edge && ctrl_vblank_ie) {
      intrStatusReg := 1.U // Set vblank interrupt flag
    }

    io.intr := (intrStatusReg =/= 0.U) && ctrl_vblank_ie

    // MMIO Read
    io.bundle.read_data := MuxLookup(addr, 0.U)(
      Seq(
        0x00.U -> 0x56474131.U, // 'VGA1' in ASCII
        0x04.U -> ctrlReg,
        0x08.U -> Cat(
          0.U(24.W),
          status_curr_frame,
          0.U(2.W),
          status_upload_busy,
          status_safe_to_swap,
          status_in_vblank
        ),
        0x0c.U -> intrStatusReg,
        0x10.U -> uploadAddrReg
      ) ++ (0 until 16).map(i => (0x20 + i * 4).U -> paletteReg(i))
    )

    // Framebuffer write port (CPU clock domain)
    framebuffer.io.clka := clock

    // Calculate framebuffer write signals
    val fb_write_en   = WireDefault(false.B)
    val fb_write_addr = WireDefault(0.U(ADDR_WIDTH.W))
    val fb_write_data = WireDefault(0.U(32.W))

    // MMIO Write
    when(io.bundle.write_enable) {
      when(addr_ctrl) {
        // Validate frame index is < 12 before updating control register
        // Frame index is bits [7:4], display enable is bit [0]
        val requested_frame = io.bundle.write_data(7, 4)
        val display_enable  = io.bundle.write_data(0)
        when(requested_frame < 12.U) {
          ctrlReg := io.bundle.write_data
        }.otherwise {
          // Invalid frame index: keep current frame, only update display enable
          ctrlReg := Cat(ctrlReg(31, 8), ctrlReg(7, 4), Cat(0.U(3.W), display_enable))
        }
      }.elsewhen(addr_intr_status) {
        // W1C: Write 1 to Clear
        intrStatusReg := intrStatusReg & ~io.bundle.write_data
      }.elsewhen(addr_upload_addr) {
        uploadAddrReg := io.bundle.write_data
      }.elsewhen(addr_stream_data) {
        // STREAM_DATA write: pack 8 pixels, write to framebuffer, auto-increment
        // Calculate word address in framebuffer
        val pixel_nibble_addr = upload_pix_addr
        val word_offset       = pixel_nibble_addr >> 3 // Divide by 8 (8 pixels per word)
        val frame_base        = upload_frame * WORDS_PER_FRAME.U
        val fb_addr           = frame_base + word_offset

        // Enable framebuffer write
        fb_write_en   := true.B
        fb_write_addr := fb_addr
        fb_write_data := io.bundle.write_data

        // Auto-increment by 8 pixels, wrap within frame
        val next_addr    = upload_pix_addr + 8.U
        val wrapped_addr = Mux(next_addr >= PIXELS_PER_FRAME.U, 0.U, next_addr)
        uploadAddrReg := Cat(upload_frame, wrapped_addr)
      }.elsewhen(addr_palette) {
        paletteReg(palette_idx) := io.bundle.write_data(5, 0)
      }
    }

    // Connect framebuffer write port
    framebuffer.io.wea   := fb_write_en
    framebuffer.io.addra := fb_write_addr
    framebuffer.io.dina  := fb_write_data
  }

  // ============ Pixel Clock Domain (pixclk) ============
  withClock(io.pixClock) {
    // VGA Sync Generator
    val h_count = RegInit(0.U(10.W))
    val v_count = RegInit(0.U(10.W))

    // Horizontal counter
    when(h_count === (H_TOTAL - 1).U) {
      h_count := 0.U
      // Vertical counter
      when(v_count === (V_TOTAL - 1).U) {
        v_count := 0.U
      }.otherwise {
        v_count := v_count + 1.U
      }
    }.otherwise {
      h_count := h_count + 1.U
    }

    // Sync signals (negative polarity for 640×480@72Hz)
    // Delayed by 2 cycles to align with pixel data pipeline
    val h_sync_pulse = (h_count >= (H_ACTIVE + H_FP).U) && (h_count < (H_ACTIVE + H_FP + H_SYNC).U)
    val v_sync_pulse = (v_count >= (V_ACTIVE + V_FP).U) && (v_count < (V_ACTIVE + V_FP + V_SYNC).U)
    val hsync_d1     = RegNext(!h_sync_pulse)
    val vsync_d1     = RegNext(!v_sync_pulse)
    io.hsync := RegNext(hsync_d1)
    io.vsync := RegNext(vsync_d1)

    // Active video region
    val h_active = h_count < H_ACTIVE.U
    val v_active = v_count < V_ACTIVE.U
    // Don't output activevideo yet - will be delayed to match pipeline

    // Pixel coordinates within active region (registered, matching reference vga-sync-gen.v)
    // These lag the counters by 1 cycle to match VGA timing pipeline
    val x_px = RegNext(h_count)
    val y_px = RegNext(v_count)

    // Check if within display area (384×384 centered)
    val in_display_x = (x_px >= LEFT_MARGIN.U) && (x_px < (LEFT_MARGIN + DISPLAY_WIDTH).U)
    val in_display_y = (y_px >= TOP_MARGIN.U) && (y_px < (TOP_MARGIN + DISPLAY_HEIGHT).U)
    val in_display   = in_display_x && in_display_y

    // Frame pixel coordinates (64×64) with 6× downscaling
    // Note: Use relative coordinates to avoid underflow
    val rel_x = Mux(x_px >= LEFT_MARGIN.U, x_px - LEFT_MARGIN.U, 0.U)
    val rel_y = Mux(y_px >= TOP_MARGIN.U, y_px - TOP_MARGIN.U, 0.U)

    // Division by 6: Manual implementation due to Verilator division bug
    // Use binary long division approximation: x/6 ≈ floor((x * 10923) / 65536)
    // For 10-bit input (0-1023), this gives correct results with minimal error
    // 10923/65536 = 0.166656 ≈ 1/6 (0.166667) - optimal constant for integer division
    // Extract bits [23:16] for 8-bit division result
    val frame_x_mult = rel_x * 10923.U
    val frame_x_div  = frame_x_mult(23, 16)
    val frame_y_mult = rel_y * 10923.U
    val frame_y_div  = frame_y_mult(23, 16)
    // Clamp to valid range [0, 63] to prevent out-of-bounds access
    // Use 8-bit division result directly, clamp will handle overflow
    val frame_x = Mux(frame_x_div >= FRAME_WIDTH.U, (FRAME_WIDTH - 1).U, frame_x_div(5, 0))
    val frame_y = Mux(frame_y_div >= FRAME_HEIGHT.U, (FRAME_HEIGHT - 1).U, frame_y_div(5, 0))

    // CDC: Synchronize control signals from CPU clock domain to pixel clock domain
    // 2-flop synchronizer for metastability prevention
    val curr_frame_sync1 = RegNext(ctrl_frame_sel)
    val curr_frame       = RegNext(curr_frame_sync1)

    val display_enabled_sync1 = RegNext(ctrl_en)
    val display_enabled       = RegNext(display_enabled_sync1)

    val blanking_sync1 = RegNext(ctrl_blank)
    val blanking       = RegNext(blanking_sync1)

    // CDC: Synchronize palette with proper 2-flop synchronizers
    // First synchronizer stage (pixel clock domain)
    val palette_sync1 = RegNext(paletteReg)
    // Second synchronizer stage for metastability prevention
    val palette_sync = RegNext(palette_sync1)

    // Framebuffer read logic
    // Pipeline stage: Delay coordinates and control signals to match pipeline depth
    // Total pipeline: x_px/y_px registration (1 cycle) + RAM read (1 cycle) = 2 cycles
    val frame_x_d1         = RegNext(frame_x)
    val frame_y_d1         = RegNext(frame_y)
    val in_display_d1      = RegNext(in_display)
    val in_display_d2      = RegNext(in_display_d1)      // Second delay to match 2-cycle pipeline
    val display_enabled_d1 = RegNext(display_enabled)
    val display_enabled_d2 = RegNext(display_enabled_d1) // Second delay to match 2-cycle pipeline
    val blanking_d1        = RegNext(blanking)
    val blanking_d2        = RegNext(blanking_d1)        // Second delay to match 2-cycle pipeline

    // Add second delay stage for h_active/v_active to match x_px/y_px registration delay
    val h_active_d1 = RegNext(h_active)
    val v_active_d1 = RegNext(v_active)
    val h_active_d2 = RegNext(h_active_d1)
    val v_active_d2 = RegNext(v_active_d1)

    // Calculate pixel address and word offset using CURRENT (non-delayed) coordinates
    // This sets up the address for the NEXT cycle
    val pixel_idx     = frame_y * FRAME_WIDTH.U + frame_x
    val word_offset   = pixel_idx >> 3  // Divide by 8 pixels per word
    val pixel_in_word = pixel_idx(2, 0) // Pixel position within word
    val frame_base    = curr_frame * WORDS_PER_FRAME.U
    val fb_read_addr  = frame_base + word_offset

    // Connect framebuffer read port (address set this cycle, data ready next cycle)
    framebuffer.io.clkb  := io.pixClock
    framebuffer.io.addrb := fb_read_addr

    // Delay pixel position to match RAM read latency
    // pixel_in_word was calculated this cycle, data arrives next cycle
    val pixel_in_word_d1 = RegNext(pixel_in_word)

    // Extract 4-bit pixel from 32-bit word (data available 1 cycle after address)
    val fb_word    = framebuffer.io.doutb
    val pixel_4bit = WireDefault(0.U(4.W))
    pixel_4bit := MuxLookup(pixel_in_word_d1, 0.U)(
      Seq(
        0.U -> fb_word(3, 0),
        1.U -> fb_word(7, 4),
        2.U -> fb_word(11, 8),
        3.U -> fb_word(15, 12),
        4.U -> fb_word(19, 16),
        5.U -> fb_word(23, 20),
        6.U -> fb_word(27, 24),
        7.U -> fb_word(31, 28)
      )
    )

    // Palette lookup
    val color_from_palette = palette_sync(pixel_4bit)

    val output_color = WireDefault(0.U(6.W))
    when(blanking) {
      output_color := 0.U // Force black
    }.elsewhen(display_enabled && in_display_d1) {
      output_color := color_from_palette
    }.otherwise {
      output_color := 0x01.U // Dark blue background
    }

    // Output with properly aligned pipeline: h_active_d2/v_active_d2 match pixel data timing
    io.rrggbb := Mux(h_active_d2 && v_active_d2, output_color, 0.U)

    // activevideo must also be delayed by 2 cycles to match pixel data pipeline
    io.activevideo := h_active_d2 && v_active_d2

    // Export pixel positions for simulator (use registered x_px/y_px for alignment)
    // Position outputs must be delayed to match activevideo (2 cycles total)
    val x_px_d1 = RegNext(x_px)
    val y_px_d1 = RegNext(y_px)
    io.x_pos := x_px_d1
    io.y_pos := y_px_d1

    // Vblank detection
    val in_vblank = v_count >= V_ACTIVE.U

    // Assign cross-clock-domain wires (pixel domain sources)
    wire_in_vblank  := in_vblank
    wire_curr_frame := curr_frame
  }
}
