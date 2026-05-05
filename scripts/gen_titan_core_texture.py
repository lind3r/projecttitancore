"""
Generate block textures for the Titan Core machine.

Produces two 16x16 PNGs:
  titan_core.png        — idle (circles unlit)
  titan_core_active.png — crafting (circles glowing)

Run from anywhere: python scripts/gen_titan_core_texture.py
"""

import struct
import zlib
import os
import math


def lerp(a: tuple, b: tuple, t: float) -> tuple:
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
    payload = chunk_type + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def build_png(pixels: list[list[tuple]]) -> bytes:
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = make_chunk(b"IHDR", struct.pack(">II", 16, 16) + bytes([8, 6, 0, 0, 0]))
    raw = b""
    for row in pixels:
        raw += b"\x00"
        for px in row:
            raw += bytes(px)
    idat = make_chunk(b"IDAT", zlib.compress(raw, 9))
    iend = make_chunk(b"IEND", b"")
    return sig + ihdr + idat + iend


# --- Color palettes ---
# Each palette defines colors for each zone of the circular design.
# Zones from outermost to innermost:
#   bg           — block background (fills corners outside the ring)
#   outer_outer  — outermost edge of the outer ring
#   outer_inner  — innermost edge of the outer ring
#   gap          — dark channel separating the two rings
#   inner_outer  — outermost edge of the inner ring
#   inner_inner  — innermost edge of the inner ring
#   core_edge    — outer edge of the central core fill
#   core_center  — exact center of the core

IDLE = {
    "bg":          (18,  14,  30,  255),
    "outer_outer": (38,  22,  72,  255),  # deep purple
    "outer_inner": (75,  48,  122, 255),  # medium purple-blue
    "gap":         (20,  16,  34,  255),
    "inner_outer": (36,  85,  110, 255),  # dark teal
    "inner_inner": (62,  132, 160, 255),  # medium teal
    "core_edge":   (50,  112, 138, 255),
    "core_center": (72,  148, 175, 255),  # slightly lighter teal
}

ACTIVE = {
    "bg":          (18,  14,  30,  255),
    "outer_outer": (90,  38,  178, 255),  # deep purple
    "outer_inner": (178, 92,  255, 255),  # vivid violet
    "gap":         (30,  22,  52,  255),  # slight purple glow in gap
    "inner_outer": (62,  192, 228, 255),  # bright cyan
    "inner_inner": (182, 245, 255, 255),  # near-white cyan
    "core_edge":   (152, 232, 250, 255),
    "core_center": (232, 255, 255, 255),  # almost white with cyan tint
}

# Ring boundary distances (pixels from center of 16x16 texture)
OUTER_RING_OUTER = 7.3   # outer edge of outer ring → outer bg boundary
OUTER_RING_INNER = 5.7   # inner edge of outer ring → gap begins
GAP_INNER        = 4.2   # inner edge of gap → inner ring begins
INNER_RING_INNER = 2.7   # inner edge of inner ring → core begins


def pixel_color(col: int, row: int, p: dict) -> tuple:
    dist = math.sqrt((col - 7.5) ** 2 + (row - 7.5) ** 2)

    if dist > OUTER_RING_OUTER:
        return p["bg"]

    if dist > OUTER_RING_INNER:
        # Outer ring — lerp from inner_color (bright) to outer_color (dark) as dist increases
        t = (dist - OUTER_RING_INNER) / (OUTER_RING_OUTER - OUTER_RING_INNER)
        return lerp(p["outer_inner"], p["outer_outer"], t)

    if dist > GAP_INNER:
        return p["gap"]

    if dist > INNER_RING_INNER:
        # Inner ring — same direction: inner edge bright, outer edge darker
        t = (dist - INNER_RING_INNER) / (GAP_INNER - INNER_RING_INNER)
        return lerp(p["inner_inner"], p["inner_outer"], t)

    # Core fill — center is brightest
    t = dist / INNER_RING_INNER
    return lerp(p["core_center"], p["core_edge"], t)


def generate_texture(palette: dict) -> bytes:
    pixels = [
        [pixel_color(col, row, palette) for col in range(16)]
        for row in range(16)
    ]
    return build_png(pixels)


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    out_dir = os.path.join(
        script_dir, "..", "mod", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "block"
    )
    os.makedirs(out_dir, exist_ok=True)

    for filename, palette in [("titan_core.png", IDLE), ("titan_core_active.png", ACTIVE)]:
        data = generate_texture(palette)
        path = os.path.join(out_dir, filename)
        with open(path, "wb") as f:
            f.write(data)
        print(f"  Written: {os.path.relpath(path, script_dir)}")


if __name__ == "__main__":
    main()
