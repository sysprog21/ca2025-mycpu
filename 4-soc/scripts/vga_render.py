#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""
VGA trace renderer - extracts VGA frames from Verilator VCD trace and renders to terminal
Supports both text-mode (ANSI colors) and image export (PNG)
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Tuple, Optional


class VCDParser:
    """Lightweight VCD parser for VGA signals"""

    def __init__(self, vcd_path: str):
        self.vcd_path = vcd_path
        self.signals = {}
        self.signal_ids = {}

    def parse_header(self):
        """Parse VCD header to extract signal identifiers"""
        with open(self.vcd_path, "r") as f:
            for line in f:
                if line.startswith("$var") or line.strip().startswith("$var"):
                    # $var wire 1 ~# io_vga_hsync $end
                    # Handle both with/without leading spaces
                    line = line.strip()
                    parts = line.split()
                    if len(parts) >= 5:
                        sig_id = parts[3]
                        sig_name = parts[4]
                        # Only keep top-level io_vga signals, not internal vga_ signals
                        if sig_name.startswith("io_vga_"):
                            self.signal_ids[sig_name] = sig_id
                            self.signals[sig_id] = {"name": sig_name, "value": "0"}
                if "$enddefinitions" in line:
                    break

    def extract_frames(self, max_pixels: int = 100000):
        """Extract VGA pixel data from VCD trace"""
        if not self.signal_ids:
            self.parse_header()

        # VGA signal IDs
        hsync_id = self.signal_ids.get("io_vga_hsync")
        vsync_id = self.signal_ids.get("io_vga_vsync")
        active_id = self.signal_ids.get("io_vga_activevideo")
        color_id = self.signal_ids.get("io_vga_rrggbb")
        x_id = self.signal_ids.get("io_vga_x_pos")
        y_id = self.signal_ids.get("io_vga_y_pos")

        if not all([hsync_id, vsync_id, active_id, color_id]):
            print(f"Error: Missing VGA signals in VCD file", file=sys.stderr)
            print(f"Found signals: {list(self.signal_ids.keys())}", file=sys.stderr)
            return []

        print(
            f"VGA signal IDs: hsync={hsync_id}, vsync={vsync_id}, active={active_id}, color={color_id}"
        )

        frames = []
        current_frame = {}
        pixel_count = 0
        in_frame = False
        prev_vsync = "1"

        with open(self.vcd_path, "r") as f:
            # Skip header
            for line in f:
                if line.startswith("$enddefinitions"):
                    break

            # Process value changes
            current_state = {
                "hsync": "1",
                "vsync": "1",
                "active": "0",
                "color": "000000",
                "x": 0,
                "y": 0,
            }

            for line in f:
                line = line.strip()
                if not line:
                    continue

                # Skip timestamp markers
                if line.startswith("#"):
                    continue

                # Parse signal changes: "0~#" or "b101010 "$"
                if len(line) > 1 and line[0] in "01":
                    sig_id = line[1:].strip()
                    value = line[0]

                    if sig_id == vsync_id:
                        # Detect frame boundary (vsync falling edge)
                        if prev_vsync == "0" and value == "1" and current_frame:
                            frames.append(current_frame)
                            current_frame = {}
                            in_frame = False
                            if len(frames) % 10 == 0:
                                print(
                                    f"Extracted {len(frames)} frames ({pixel_count} pixels total)"
                                )
                        prev_vsync = value
                        current_state["vsync"] = value

                    elif sig_id == active_id:
                        current_state["active"] = value

                elif line.startswith("b"):
                    # Binary value: "b101010 $"
                    parts = line.split()
                    if len(parts) >= 2:
                        value_str = parts[0][1:]  # Remove 'b' prefix
                        sig_id = parts[1]

                        if sig_id == color_id and current_state["active"] == "1":
                            # Convert binary to 6-bit color
                            try:
                                color_val = int(value_str, 2) if value_str else 0
                                x = current_state["x"]
                                y = current_state["y"]

                                # Store pixel (deduplicate by position)
                                current_frame[(x, y)] = color_val
                                pixel_count += 1

                                if pixel_count >= max_pixels:
                                    if current_frame:
                                        frames.append(current_frame)
                                    print(f"Reached pixel limit: {max_pixels}")
                                    return frames

                            except ValueError:
                                pass

                        elif sig_id == x_id:
                            try:
                                current_state["x"] = (
                                    int(value_str, 2) if value_str else 0
                                )
                            except ValueError:
                                pass

                        elif sig_id == y_id:
                            try:
                                current_state["y"] = (
                                    int(value_str, 2) if value_str else 0
                                )
                            except ValueError:
                                pass

        # Add final frame if any
        if current_frame:
            frames.append(current_frame)

        print(f"Extracted {len(frames)} total frames ({pixel_count} total pixels)")
        return frames


def rrggbb_to_rgb(rrggbb: int) -> Tuple[int, int, int]:
    """Convert 6-bit RRGGBB to 24-bit RGB"""
    rr = (rrggbb >> 4) & 0x3
    gg = (rrggbb >> 2) & 0x3
    bb = rrggbb & 0x3

    # Scale 2-bit to 8-bit (0-3 -> 0-255)
    r = (rr * 255) // 3
    g = (gg * 255) // 3
    b = (bb * 255) // 3

    return (r, g, b)


def render_frame_terminal(
    frame: dict, width: int = 640, height: int = 480, scale: int = 2
):
    """Render frame to terminal using ANSI escape codes"""
    # Use Unicode block characters for better resolution
    BLOCK = "█"

    # Find actual frame bounds
    if not frame:
        print("Empty frame")
        return

    xs = [x for x, y in frame.keys()]
    ys = [y for x, y in frame.keys()]

    if not xs or not ys:
        print("No pixel data")
        return

    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)

    print(
        f"\nFrame bounds: X[{min_x}:{max_x}] Y[{min_y}:{max_y}] ({len(frame)} pixels)"
    )

    # Render with scaling
    for y in range(min_y, max_y + 1, scale):
        line = ""
        for x in range(min_x, max_x + 1, scale):
            color_val = frame.get((x, y), 0)
            r, g, b = rrggbb_to_rgb(color_val)

            # ANSI 24-bit true color
            line += f"\033[38;2;{r};{g};{b}m{BLOCK}\033[0m"

        print(line)


def render_frame_png(
    frame: dict, output_path: str, width: int = 640, height: int = 480
):
    """Render frame to PNG image"""
    try:
        from PIL import Image
    except ImportError:
        print(
            "Error: PIL (Pillow) not installed. Install with: pip install Pillow",
            file=sys.stderr,
        )
        return False

    img = Image.new("RGB", (width, height), color="black")
    pixels = img.load()

    for (x, y), color_val in frame.items():
        if 0 <= x < width and 0 <= y < height:
            r, g, b = rrggbb_to_rgb(color_val)
            pixels[x, y] = (r, g, b)

    img.save(output_path)
    print(f"Saved frame to: {output_path}")
    return True


def main():
    parser = argparse.ArgumentParser(description="Render VGA frames from VCD trace")
    parser.add_argument("vcd_file", help="Input VCD trace file")
    parser.add_argument(
        "--frames", type=int, default=1, help="Number of frames to render"
    )
    parser.add_argument(
        "--output", "-o", help="Output PNG file (if not specified, render to terminal)"
    )
    parser.add_argument(
        "--scale", type=int, default=2, help="Scale factor for terminal rendering"
    )
    parser.add_argument(
        "--max-pixels", type=int, default=500000, help="Maximum pixels to extract"
    )

    args = parser.parse_args()

    if not Path(args.vcd_file).exists():
        print(f"Error: VCD file not found: {args.vcd_file}", file=sys.stderr)
        return 1

    print(f"Parsing VCD file: {args.vcd_file}")

    parser = VCDParser(args.vcd_file)
    frames = parser.extract_frames(max_pixels=args.max_pixels)

    if not frames:
        print("Error: No frames extracted from VCD", file=sys.stderr)
        return 1

    print(f"\nRendering {min(args.frames, len(frames))} frame(s)...")

    for i, frame in enumerate(frames[: args.frames]):
        print(f"\n{'='*60}")
        print(f"Frame {i}")
        print(f"{'='*60}")

        if args.output:
            output_path = (
                args.output.replace(".png", f"_frame{i}.png")
                if len(frames) > 1
                else args.output
            )
            render_frame_png(frame, output_path)
        else:
            render_frame_terminal(frame, scale=args.scale)

    return 0


if __name__ == "__main__":
    sys.exit(main())
