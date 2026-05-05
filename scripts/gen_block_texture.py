"""
Generate block textures for Project Titan Core.
Outputs 32x32 PNGs for inactive and active states.
Run: python scripts/gen_block_texture.py

Side texture: horizontal gold rings on warm stone.
Top/bottom texture: concentric gold squares matching the ring heights.
Model: cube_column (side + end textures).
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

# --- Palette ---
STONE   = (148, 132, 108, 255)
EDGE    = ( 88,  78,  62, 255)
RIM     = ( 50,  43,  33, 255)
GOLD    = (218, 170,  38, 255)
GOLDHI  = (255, 222,  72, 255)
GOLDSH  = (145, 110,  16, 255)
CREAM   = (255, 248, 210, 255)
WHITE   = (255, 255, 248, 255)
CREAM_A = (255, 240, 155, 255)
WHITE_A = (255, 255, 205, 255)
WARM_ST = (162, 145, 118, 255)


def glow_intensity(x: int) -> float:
    dist = abs(x - 15.5)
    if dist <= 3.0: return 1.0
    if dist <= 7.0: return (7.0 - dist) / 4.0
    return 0.0


def side_pixel(x: int, y: int, active: bool = False) -> tuple:
    if x == 0 or x == 31 or y == 0 or y == 31: return RIM
    if x == 1 or x == 30 or y == 1 or y == 30: return EDGE

    if y == 6:          return GOLDHI
    if y in (7, 8):     return GOLD
    if y == 9:          return GOLDSH

    if y == 14:         return GOLDHI
    if y in (15, 16):
        gi = glow_intensity(x)
        if gi == 0.0:   return GOLD
        c_cream = CREAM_A if active else CREAM
        c_white = WHITE_A if active else WHITE
        if gi <= 0.5:   return blend(GOLD, c_cream, gi * 2.0)
        else:           return blend(c_cream, c_white, (gi - 0.5) * 2.0)
    if y == 17:         return GOLDSH

    if y == 22:         return GOLDHI
    if y in (23, 24):   return GOLD
    if y == 25:         return GOLDSH

    if active and y in (12, 13, 18, 19):
        return blend(STONE, WARM_ST, 0.22)

    return STONE


def top_pixel(x: int, y: int, active: bool = False) -> tuple:
    """
    Concentric gold squares. dist = distance from nearest edge.
    Matches the ring heights of the side texture:
      Ring 1/3 (rows 6-9 / 22-25 from edges)  ->  dist 6-9
      Ring 2   (rows 14-17, centre)            ->  dist 13-15
    """
    dist = min(x, y, SIZE - 1 - x, SIZE - 1 - y)

    if dist == 0: return RIM
    if dist == 1: return EDGE

    if dist == 6:           return GOLDHI
    if dist in (7, 8):      return GOLD
    if dist == 9:           return GOLDSH

    if dist == 13:          return GOLDHI
    if dist == 14:          return blend(GOLD, CREAM_A if active else CREAM, 0.6)
    if dist == 15:          return WHITE_A if active else WHITE

    if active and dist in (10, 11, 12):
        return blend(STONE, WARM_ST, 0.18)

    return STONE


def generate_side(active: bool = False) -> list:
    return [[side_pixel(x, y, active) for x in range(SIZE)] for y in range(SIZE)]


def generate_top(active: bool = False) -> list:
    return [[top_pixel(x, y, active) for x in range(SIZE)] for y in range(SIZE)]


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    texture_dir = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "block"
    )
    os.makedirs(texture_dir, exist_ok=True)

    outputs = [
        (generate_side(False),  "titan_core"),
        (generate_side(True),   "titan_core_active"),
        (generate_top(False),   "titan_core_top"),
        (generate_top(True),    "titan_core_top_active"),
    ]
    for pixels, name in outputs:
        png_data = build_png(pixels)
        out_path = os.path.join(texture_dir, f"{name}.png")
        with open(out_path, "wb") as f:
            f.write(png_data)
        print(f"  Written: {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()
