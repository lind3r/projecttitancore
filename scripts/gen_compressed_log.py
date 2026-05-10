"""
Generate the Compressed Log block texture.

Wood-themed, NOT holy palette — Compressed Log is a tier-recipe ingredient
for the Titan Core (T5 corner slots), so it sits outside the cathedral
aesthetic. Style follows the Extra Utilities compressed-cobble convention:
the same texture as the natural variant, but with a darker, thicker border
to read as "stuffed nine-into-one".

Run: python scripts/gen_compressed_log.py
Output: src/main/resources/assets/projecttitancore/textures/block/compressed_log.png
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

# --- Wood palette (oak-bark family) ---
BARK_HI   = (162, 122,  76, 255)   # highlight streak
BARK      = (122,  90,  53, 255)   # mid bark
BARK_SH   = ( 88,  64,  38, 255)   # shaded streak
GROOVE    = ( 54,  38,  22, 255)   # deep crevice / inner border
RIM       = ( 26,  18,  10, 255)   # outermost border, very dark


# Per-column bark shade — deterministic so the texture tiles vertically and
# adjacent blocks share a consistent grain. Columns alternate between three
# shades plus the occasional deep groove for character.
def column_shade(x: int) -> tuple:
    # Reserve x=2..29 for the inner 28-px-wide grain (2px border on each side).
    # Hash spreads shades unevenly across columns so the result doesn't read
    # as banding. Tuned by eye, not magic.
    seed = (x * 7 + 3) % 13
    if seed in (0, 5):
        return GROOVE                   # 2 columns become deep grooves
    if seed in (1, 6, 9):
        return BARK_HI                  # 3 columns are highlights
    if seed in (3, 8, 11):
        return BARK_SH                  # 3 columns are shaded
    return BARK                         # rest is mid-tone


def render_compressed_log(x: int, y: int) -> tuple:
    # Outer 1px ring — pitch-dark frame, thickest visual component of the
    # "compressed" tell.
    if x == 0 or x == SIZE - 1 or y == 0 or y == SIZE - 1:
        return RIM
    # Inner 1px ring — slightly lighter dark, gives the border a bevelled,
    # 2-px-thick read rather than a flat outline.
    if x == 1 or x == SIZE - 2 or y == 1 or y == SIZE - 2:
        return GROOVE

    base = column_shade(x)

    # Sparse horizontal flecks — knot/grain noise so columns don't read as
    # perfectly straight stripes. Two passes: subtle dark and subtle light.
    if (x * 31 + y * 17) % 41 == 0:
        return blend(base, GROOVE, 0.45)
    if (x * 19 + y * 23) % 53 == 0:
        return blend(base, BARK_HI, 0.40)

    return base


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    texture_dir = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "block"
    )
    os.makedirs(texture_dir, exist_ok=True)

    pixels = [[render_compressed_log(x, y) for x in range(SIZE)] for y in range(SIZE)]
    out_path = os.path.join(texture_dir, "compressed_log.png")
    with open(out_path, "wb") as f:
        f.write(build_png(pixels))
    print(f"  Written: {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()
