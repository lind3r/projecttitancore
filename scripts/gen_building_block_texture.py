"""
Generate building block textures for Project Titan Core - holy theme.
Each variant is registered in VARIANTS with a (x, y) -> RGBA render function.
Output: 32x32 tileable PNGs in src/main/resources/.../textures/block/.

Run: python scripts/gen_building_block_texture.py

To add a chiseled variant later:
    1. Write a `render_<name>(x, y)` function returning an (r, g, b, a) tuple.
    2. Add it to VARIANTS as `"<name>": render_<name>`.
    3. Re-run this script and register the block in code + resources.
"""

import struct
import zlib
import os


def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
    payload = chunk_type + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def build_png(pixels: list, size: int = 32) -> bytes:
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = make_chunk(b"IHDR", struct.pack(">II", size, size) + bytes([8, 6, 0, 0, 0]))
    raw = b""
    for row in pixels:
        raw += b"\x00"
        for px in row:
            raw += bytes(px)
    idat = make_chunk(b"IDAT", zlib.compress(raw, 9))
    iend = make_chunk(b"IEND", b"")
    return sig + ihdr + idat + iend


def blend(c1: tuple, c2: tuple, t: float) -> tuple:
    t = max(0.0, min(1.0, t))
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))


SIZE = 32

# --- Holy palette (shared with gen_block_texture.py) ---
IVORY     = (236, 228, 208, 255)
IVORY_SH  = (208, 198, 174, 255)
EDGE      = (138, 102,  32, 255)
GOLD      = (220, 172,  42, 255)
GOLDHI    = (255, 224,  84, 255)
GOLDSH    = (148, 110,  18, 255)
HALO      = (255, 248, 215, 255)


def marble(x: int, y: int) -> tuple:
    """Sparse pseudo-random veining; deterministic per coordinate so it tiles."""
    if (x * 3 + y * 5) % 17 == 0 and (x + y * 2) % 13 == 0: return IVORY_SH
    if (x * 7 + y * 3) % 23 == 1 and (x + y) % 5 == 0:     return IVORY_SH
    return IVORY


# ---------------------------------------------------------------------------
# Variant: holy_bricks
# Tileable brick pattern. 4 rows of 8px-tall bricks, 16px wide, offset half a
# brick on odd rows. Mortar = aged dark gold, brick body = ivory marble with
# a 1px highlight on top and 1px shadow on bottom for depth.
# ---------------------------------------------------------------------------
def render_holy_bricks(x: int, y: int) -> tuple:
    BRICK_H = 8
    BRICK_W = 16

    # Horizontal mortar at every 8px row boundary
    if y % BRICK_H == 0:
        return GOLDSH

    row = y // BRICK_H

    # Vertical mortar — even rows have seams at x=0,16; odd rows offset to x=8,24
    if row % 2 == 0:
        is_mortar_v = (x % BRICK_W == 0)
    else:
        is_mortar_v = (x % BRICK_W == 8)

    if is_mortar_v:
        return GOLDSH

    rel_y = y % BRICK_H  # 1..7

    # Top edge of brick — soft highlight to suggest a chamfered upper face
    if rel_y == 1:
        return blend(marble(x, y), HALO, 0.45)

    # Bottom edge — shadow
    if rel_y == BRICK_H - 1:
        return IVORY_SH

    return marble(x, y)


# ---------------------------------------------------------------------------
# Variant registry — add new entries here for chiseled / etched variants.
# ---------------------------------------------------------------------------
VARIANTS = {
    "holy_bricks": render_holy_bricks,
}


def generate(render_fn) -> list:
    return [[render_fn(x, y) for x in range(SIZE)] for y in range(SIZE)]


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    texture_dir = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "block"
    )
    os.makedirs(texture_dir, exist_ok=True)

    for name, render_fn in VARIANTS.items():
        pixels = generate(render_fn)
        png_data = build_png(pixels)
        out_path = os.path.join(texture_dir, f"{name}.png")
        with open(out_path, "wb") as f:
            f.write(png_data)
        print(f"  Written: {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()
