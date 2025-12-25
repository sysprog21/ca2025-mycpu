// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package peripheral

import bus.AXI4LiteChannels
import bus.AXI4LiteSlave
import chisel3._
import chisel3.util._
import riscv.Parameters

/**
 * VGA peripheral with AXI4-Lite interface and dual-clock CDC
 *
 * Memory map (Base: 0x20000000):
 *   0x00: ID          - Peripheral identification (RO: 0x56474131 = 'VGA1')
 *   0x04: STATUS      - [31:16] timing_error_count, [7:4] frame, [2] busy, [1] safe, [0] vblank
 *   0x08: INTR_STATUS - Vblank interrupt flag (W1C)
 *   0x10: UPLOAD_ADDR - Framebuffer upload address (nibble index + frame)
 *   0x14: STREAM_DATA - 8 pixels packed in 32-bit word (auto-increment)
 *   0x20: CTRL        - Display enable, blank, swap request, frame select, interrupt enable
 *   0x24-0x60: PALETTE[0-15] - 16 entries, 6-bit VGA colors (RRGGBB)
 *
 * VGA timing: 640×480 @ 72Hz
 *   H_TOTAL=832, V_TOTAL=520, pixel clock=31.5 MHz
 *
 * Lost-sync detection:
 *   The timing_error_count field in STATUS tracks timing anomalies in the pixel
 *   clock domain. Errors indicate counter overflow (h_count >= H_TOTAL or
 *   v_count >= V_TOTAL) which should never occur in normal operation. Non-zero
 *   values suggest clock domain issues, configuration errors, or hardware faults.
 */
class VGA extends Module {
  val io = IO(new Bundle {
    val channels    = Flipped(new AXI4LiteChannels(8, Parameters.DataBits))
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
  val H_ACTIVE = 640
  val H_FP     = 24
  val H_SYNC   = 40
  val H_BP     = 128
  val H_TOTAL  = H_ACTIVE + H_FP + H_SYNC + H_BP

  val V_ACTIVE = 480
  val V_FP     = 9
  val V_SYNC   = 3
  val V_BP     = 28
  val V_TOTAL  = V_ACTIVE + V_FP + V_SYNC + V_BP

  // Display scaling and positioning
  val FRAME_WIDTH    = 64
  val FRAME_HEIGHT   = 64
  val SCALE_FACTOR   = 6
  val DISPLAY_WIDTH  = FRAME_WIDTH * SCALE_FACTOR
  val DISPLAY_HEIGHT = FRAME_HEIGHT * SCALE_FACTOR
  val LEFT_MARGIN    = (H_ACTIVE - DISPLAY_WIDTH) / 2
  val TOP_MARGIN     = (V_ACTIVE - DISPLAY_HEIGHT) / 2

  // Fixed-point multiplier for divide-by-SCALE_FACTOR (6):
  // x / 6 ≈ x * (65536/6) >> 16 = x * 10923 >> 16
  // Using ceil(65536/6) = 10923 for proper rounding
  val DIV_BY_SCALE_MULT = 10923

  // Framebuffer parameters
  val NUM_FRAMES       = 12
  val PIXELS_PER_FRAME = 4096
  val TOTAL_PIXELS     = NUM_FRAMES * PIXELS_PER_FRAME
  val WORDS_PER_FRAME  = PIXELS_PER_FRAME / 8
  val TOTAL_WORDS      = NUM_FRAMES * WORDS_PER_FRAME
  val ADDR_WIDTH       = 13

  // ============ MMIO Register Offsets ============
  // These match the memory map documented in the class header
  object Reg {
    val ID           = 0x00 // Peripheral ID ('VGA1' = 0x56474131)
    val STATUS       = 0x04 // Status register
    val INTR_STATUS  = 0x08 // Interrupt status (W1C)
    val UPLOAD_ADDR  = 0x10 // Framebuffer upload address
    val STREAM_DATA  = 0x14 // Pixel data streaming port
    val CTRL         = 0x20 // Control register
    val PALETTE_BASE = 0x24 // Palette entries start here
    val PALETTE_END  = 0x64 // Palette entries end here (16 entries: 0x24-0x60)
  }

  // Peripheral identification constant
  val VGA_ID = 0x56474131.U // ASCII 'VGA1'

  // ============ Framebuffer RAM ============
  val framebuffer = Module(new TrueDualPortRAM32(TOTAL_WORDS, ADDR_WIDTH))

  // ============ AXI4-Lite Slave Interface ============
  val slave = Module(new AXI4LiteSlave(8, Parameters.DataBits))
  slave.io.channels <> io.channels

  // ============ CPU Clock Domain (sysclk) ============
  val sysClk = clock

  // MMIO Registers
  val ctrlReg       = RegInit(0.U(32.W))
  val intrStatusReg = RegInit(0.U(32.W))
  val uploadAddrReg = RegInit(0.U(32.W))
  val paletteReg    = RegInit(VecInit(Seq.fill(16)(0.U(6.W))))

  // Control register bit fields
  val ctrl_en        = ctrlReg(0)
  val ctrl_blank     = ctrlReg(1)
  val ctrl_swap_req  = ctrlReg(2)
  val ctrl_frame_sel = ctrlReg(7, 4)
  val ctrl_vblank_ie = ctrlReg(8)

  // Cross-clock-domain wires
  val wire_in_vblank       = Wire(Bool())
  val wire_curr_frame      = Wire(UInt(4.W))
  val wire_timing_err_flag = Wire(Bool()) // Pulse/toggle from pixel domain on timing error

  withClock(sysClk) {
    // Upload address fields (clamp frame index to valid range)
    val upload_pix_addr  = uploadAddrReg(15, 0)
    val upload_frame_raw = uploadAddrReg(19, 16)
    val upload_frame     = Mux(upload_frame_raw >= NUM_FRAMES.U, (NUM_FRAMES - 1).U, upload_frame_raw)

    // CDC: Synchronize status signals from pixel domain
    val vblank_sync1      = RegNext(wire_in_vblank)
    val vblank_synced     = RegNext(vblank_sync1)
    val curr_frame_sync1  = RegNext(wire_curr_frame)
    val curr_frame_synced = RegNext(curr_frame_sync1)

    // CDC for timing error: Toggle-based crossing (avoids torn multi-bit reads)
    // Pixel domain toggles wire_timing_err_flag on each error event.
    // Sys domain detects toggle edges and increments local counter.
    val timing_err_sync1   = RegNext(wire_timing_err_flag)
    val timing_err_sync2   = RegNext(timing_err_sync1)
    val timing_err_sync3   = RegNext(timing_err_sync2)
    val timing_err_edge    = timing_err_sync2 =/= timing_err_sync3 // Toggle edge detected
    val timing_error_count = RegInit(0.U(16.W))
    when(timing_err_edge && timing_error_count < "hFFFF".U) {
      timing_error_count := timing_error_count + 1.U
    }

    // Status signals
    val status_in_vblank    = vblank_synced
    val status_safe_to_swap = vblank_synced
    val status_upload_busy  = false.B
    val status_curr_frame   = curr_frame_synced

    // Vblank interrupt: Edge detection
    val vblank_prev        = RegNext(vblank_synced)
    val vblank_rising_edge = vblank_synced && !vblank_prev

    when(vblank_rising_edge && ctrl_vblank_ie) {
      intrStatusReg := 1.U
    }

    io.intr := (intrStatusReg =/= 0.U) && ctrl_vblank_ie

    // MMIO address decode (mask to get offset within peripheral)
    val addr             = slave.io.bundle.address & 0xff.U // VGA registers at 0x00-0xFF
    val addr_id          = addr === Reg.ID.U
    val addr_status      = addr === Reg.STATUS.U
    val addr_intr_status = addr === Reg.INTR_STATUS.U
    val addr_upload_addr = addr === Reg.UPLOAD_ADDR.U
    val addr_stream_data = addr === Reg.STREAM_DATA.U
    val addr_ctrl        = addr === Reg.CTRL.U
    val addr_palette     = (addr >= Reg.PALETTE_BASE.U) && (addr < Reg.PALETTE_END.U)
    val palette_idx      = (addr - Reg.PALETTE_BASE.U) >> 2

    // AXI4-Lite Read handling
    // CRITICAL: read_valid must ONLY be asserted when peripheral has valid data ready
    // in response to a read request. The AXI4LiteSlave state machine waits for
    // read_valid before capturing read_data and setting RVALID.
    //
    // Protocol timing:
    // 1. Slave sets io.bundle.read = true (enters ReadData state)
    // 2. Peripheral computes read_data based on address
    // 3. Peripheral sets read_valid = true (data ready)
    // 4. Slave captures read_data, sets RVALID, clears read
    //
    // Setting read_valid unconditionally breaks this sequence - the slave captures
    // data before the peripheral has prepared it!

    val read_data_prepared = WireDefault(0.U(32.W))

    when(addr_id) {
      read_data_prepared := VGA_ID
    }.elsewhen(addr_ctrl) {
      read_data_prepared := ctrlReg
    }.elsewhen(addr_status) {
      // STATUS register layout:
      //   [31:16] timing_error_count - Lost-sync error counter (sys domain, CDC-safe)
      //   [15:8]  reserved
      //   [7:4]   curr_frame         - Current display frame index
      //   [3]     reserved
      //   [2]     upload_busy        - Framebuffer upload in progress
      //   [1]     safe_to_swap       - Safe to swap frames (same as vblank)
      //   [0]     in_vblank          - Currently in vertical blanking period
      read_data_prepared := Cat(
        timing_error_count,
        0.U(8.W),
        status_curr_frame,
        0.U(1.W),
        status_upload_busy,
        status_safe_to_swap,
        status_in_vblank
      )
    }.elsewhen(addr_intr_status) {
      read_data_prepared := intrStatusReg
    }.elsewhen(addr_upload_addr) {
      read_data_prepared := uploadAddrReg
    }.elsewhen(addr_palette) {
      read_data_prepared := paletteReg(palette_idx)
    }

    // Only assert read_valid when there's an active read request
    // This gives the combinational logic time to prepare the correct data
    slave.io.bundle.read_valid := slave.io.bundle.read
    slave.io.bundle.read_data  := read_data_prepared

    // Framebuffer write port
    framebuffer.io.clka := clock

    val fb_write_en   = WireDefault(false.B)
    val fb_write_addr = WireDefault(0.U(ADDR_WIDTH.W))
    val fb_write_data = WireDefault(0.U(32.W))

    // AXI4-Lite Write handling
    when(slave.io.bundle.write) {
      when(addr_ctrl) {
        val requested_frame = slave.io.bundle.write_data(7, 4)
        val display_enable  = slave.io.bundle.write_data(0)
        when(requested_frame < 12.U) {
          ctrlReg := slave.io.bundle.write_data
        }.otherwise {
          ctrlReg := Cat(ctrlReg(31, 8), ctrlReg(7, 4), Cat(0.U(3.W), display_enable))
        }
      }.elsewhen(addr_intr_status) {
        intrStatusReg := intrStatusReg & ~slave.io.bundle.write_data
      }.elsewhen(addr_upload_addr) {
        uploadAddrReg := slave.io.bundle.write_data
      }.elsewhen(addr_stream_data) {
        val pixel_nibble_addr = upload_pix_addr
        val word_offset       = pixel_nibble_addr >> 3
        val frame_base        = upload_frame * WORDS_PER_FRAME.U
        val fb_addr           = frame_base + word_offset

        fb_write_en   := true.B
        fb_write_addr := fb_addr
        fb_write_data := slave.io.bundle.write_data

        val next_addr    = upload_pix_addr + 8.U
        val wrapped_addr = Mux(next_addr >= PIXELS_PER_FRAME.U, 0.U, next_addr)
        uploadAddrReg := Cat(upload_frame, wrapped_addr)
      }.elsewhen(addr_palette) {
        paletteReg(palette_idx) := slave.io.bundle.write_data(5, 0)
      }
    }

    framebuffer.io.wea   := fb_write_en
    framebuffer.io.addra := fb_write_addr
    framebuffer.io.dina  := fb_write_data
  }

  // ============ Pixel Clock Domain (pixclk) ============
  withClock(io.pixClock) {
    val h_count = RegInit(0.U(10.W))
    val v_count = RegInit(0.U(10.W))

    when(h_count === (H_TOTAL - 1).U) {
      h_count := 0.U
      when(v_count === (V_TOTAL - 1).U) {
        v_count := 0.U
      }.otherwise {
        v_count := v_count + 1.U
      }
    }.otherwise {
      h_count := h_count + 1.U
    }

    // First-frame guard: suppress undefined framebuffer data on power-up
    // Use counter (starts at 0) instead of RegInit(true.B) to avoid CDC reset issues
    val frame_count = RegInit(0.U(2.W))
    val first_frame = frame_count === 0.U
    when(h_count === (H_TOTAL - 1).U && v_count === (V_TOTAL - 1).U && frame_count < 2.U) {
      frame_count := frame_count + 1.U
    }

    val h_sync_pulse = (h_count >= (H_ACTIVE + H_FP).U) && (h_count < (H_ACTIVE + H_FP + H_SYNC).U)
    val v_sync_pulse = (v_count >= (V_ACTIVE + V_FP).U) && (v_count < (V_ACTIVE + V_FP + V_SYNC).U)
    val hsync_d1     = RegNext(!h_sync_pulse)
    val vsync_d1     = RegNext(!v_sync_pulse)
    io.hsync := RegNext(hsync_d1)
    io.vsync := RegNext(vsync_d1)

    val h_active = h_count < H_ACTIVE.U
    val v_active = v_count < V_ACTIVE.U

    val x_px = RegNext(h_count)
    val y_px = RegNext(v_count)

    val in_display_x = (x_px >= LEFT_MARGIN.U) && (x_px < (LEFT_MARGIN + DISPLAY_WIDTH).U)
    val in_display_y = (y_px >= TOP_MARGIN.U) && (y_px < (TOP_MARGIN + DISPLAY_HEIGHT).U)
    val in_display   = in_display_x && in_display_y

    val rel_x = Mux(x_px >= LEFT_MARGIN.U, x_px - LEFT_MARGIN.U, 0.U)
    val rel_y = Mux(y_px >= TOP_MARGIN.U, y_px - TOP_MARGIN.U, 0.U)

    // Fixed-point division by SCALE_FACTOR using multiply-shift
    val frame_x_mult = rel_x * DIV_BY_SCALE_MULT.U
    val frame_x_div  = frame_x_mult(23, 16)
    val frame_y_mult = rel_y * DIV_BY_SCALE_MULT.U
    val frame_y_div  = frame_y_mult(23, 16)
    val frame_x      = Mux(frame_x_div >= FRAME_WIDTH.U, (FRAME_WIDTH - 1).U, frame_x_div(5, 0))
    val frame_y      = Mux(frame_y_div >= FRAME_HEIGHT.U, (FRAME_HEIGHT - 1).U, frame_y_div(5, 0))

    // CDC: Synchronize control signals
    val curr_frame_sync1 = RegNext(ctrl_frame_sel)
    val curr_frame       = RegNext(curr_frame_sync1)

    val display_enabled_sync1 = RegNext(ctrl_en)
    val display_enabled       = RegNext(display_enabled_sync1)

    val blanking_sync1 = RegNext(ctrl_blank)
    val blanking       = RegNext(blanking_sync1)

    val palette_sync1 = RegNext(paletteReg)
    val palette_sync  = RegNext(palette_sync1)

    // Pipeline delays
    val frame_x_d1         = RegNext(frame_x)
    val frame_y_d1         = RegNext(frame_y)
    val in_display_d1      = RegNext(in_display)
    val in_display_d2      = RegNext(in_display_d1)
    val display_enabled_d1 = RegNext(display_enabled)
    val display_enabled_d2 = RegNext(display_enabled_d1)
    val blanking_d1        = RegNext(blanking)
    val blanking_d2        = RegNext(blanking_d1)

    val h_active_d1 = RegNext(h_active)
    val v_active_d1 = RegNext(v_active)
    val h_active_d2 = RegNext(h_active_d1)
    val v_active_d2 = RegNext(v_active_d1)

    val pixel_idx     = frame_y * FRAME_WIDTH.U + frame_x
    val word_offset   = pixel_idx >> 3
    val pixel_in_word = pixel_idx(2, 0)
    val frame_base    = curr_frame * WORDS_PER_FRAME.U
    val fb_read_addr  = frame_base + word_offset

    framebuffer.io.clkb  := io.pixClock
    framebuffer.io.addrb := fb_read_addr

    val pixel_in_word_d1 = RegNext(pixel_in_word)

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

    val color_from_palette = palette_sync(pixel_4bit)

    val output_color = WireDefault(0.U(6.W))
    when(blanking) {
      output_color := 0.U
    }.elsewhen(display_enabled && in_display_d1) {
      output_color := color_from_palette
    }.otherwise {
      output_color := 0x01.U
    }

    // Gate output during first frame to suppress undefined framebuffer data
    io.rrggbb      := Mux(h_active_d2 && v_active_d2 && !first_frame, output_color, 0.U)
    io.activevideo := h_active_d2 && v_active_d2

    val x_px_d1 = RegNext(x_px)
    val y_px_d1 = RegNext(y_px)
    io.x_pos := x_px_d1
    io.y_pos := y_px_d1

    val in_vblank = v_count >= V_ACTIVE.U

    // Lost-sync detection: Track timing anomalies via toggle-based CDC
    // Counter overflow detection - h_count and v_count should never exceed their bounds
    // in normal operation. If they do, it indicates clock domain issues, configuration
    // errors, or hardware faults.
    //
    // Toggle approach: Each error event toggles a flag, which is then CDC-synchronized
    // to sys domain where the actual counting happens. This avoids torn multi-bit reads.
    val timing_err_toggle = RegInit(false.B)
    val h_overflow        = h_count >= H_TOTAL.U
    val v_overflow        = v_count >= V_TOTAL.U
    val timing_anomaly    = h_overflow || v_overflow

    when(timing_anomaly) {
      timing_err_toggle := !timing_err_toggle
    }

    wire_in_vblank       := in_vblank
    wire_curr_frame      := curr_frame
    wire_timing_err_flag := timing_err_toggle
  }
}
