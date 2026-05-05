"""
Generate block textures for Project Titan Core - holy theme.
Outputs 32x32 PNGs for inactive and active states.
Run: python scripts/gen_block_texture.py

Side: ivory marble base, gold Greek cross, glowing halo at the intersection,
gold trim bands top/bottom, dark gold rim.
Top/bottom: ivory marble, centred Greek cross with halo, diagonal sunburst rays.
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
CX = 15.5
CY = 15.5

# --- Holy palette ---
IVORY     = (236, 228, 208, 255)
IVORY_SH  = (208, 198, 174, 255)
RIM       = ( 70,  54,  18, 255)
EDGE      = (138, 102,  32, 255)
GOLD      = (220, 172,  42, 255)
GOLDHI    = (255, 224,  84, 255)
GOLDSH    = (148, 110,  18, 255)
HALO      = (255, 248, 215, 255)
HALO_HI   = (255, 255, 245, 255)
HALO_A    = (255, 252, 175, 255)
HALO_HIA  = (255, 255, 235, 255)
RAY       = (245, 224, 162, 255)
RAY_A     = (255, 240, 180, 255)


def dist_center(x: int, y: int) -> float:
    dx = x - CX
    dy = y - CY
    return (dx * dx + dy * dy) ** 0.5


def in_vertical_arm(x: int, y: int) -> bool:
    return 14 <= x <= 17 and 4 <= y <= 27


def in_horizontal_arm(x: int, y: int) -> bool:
    return 14 <= y <= 17 and 4 <= x <= 27


def cross_body(active: bool) -> tuple:
    return blend(GOLD, GOLDHI, 0.35) if active else GOLD


def cross_color(x: int, y: int, active: bool) -> tuple:
    in_v = in_vertical_arm(x, y)
    in_h = in_horizontal_arm(x, y)
    body = cross_body(active)

    if in_v and in_h:
        return GOLDHI

    if in_v:
        if y == 4:     return GOLDHI
        if y == 27:    return GOLDSH
        if x == 14:    return GOLDHI
        if x == 17:    return GOLDSH
        return body

    # in_h
    if x == 4:         return GOLDHI
    if x == 27:        return GOLDSH
    if y == 14:        return GOLDHI
    if y == 17:        return GOLDSH
    return body


def halo_disc(x: int, y: int, active: bool):
    """Returns halo color if pixel is inside the central halo disc, else None."""
    d = dist_center(x, y)
    halo_inner = 2.5 if active else 2.0
    halo_mid   = 5.5 if active else 4.5
    if d < halo_inner:
        return HALO_HIA if active else HALO_HI
    if d < halo_mid:
        return HALO_A if active else HALO
    return None


def marble(x: int, y: int) -> tuple:
    # Sparse pseudo-random veining for subtle texture
    if (x * 3 + y * 5) % 17 == 0 and (x + y * 2) % 13 == 0: return IVORY_SH
    if (x * 7 + y * 3) % 23 == 1 and (x + y) % 5 == 0:     return IVORY_SH
    return IVORY


def side_pixel(x: int, y: int, active: bool = False) -> tuple:
    if x == 0 or x == SIZE - 1 or y == 0 or y == SIZE - 1: return RIM
    if x == 1 or x == SIZE - 2 or y == 1 or y == SIZE - 2: return EDGE

    # Trim bands top and bottom
    if y == 2 or y == SIZE - 3:    return GOLD
    if y == 3:                     return GOLDSH
    if y == SIZE - 4:              return GOLDHI

    # Halo disc — overrides cross arms and ivory inside the disc radius
    halo = halo_disc(x, y, active)
    if halo is not None:
        return halo

    if in_vertical_arm(x, y) or in_horizontal_arm(x, y):
        return cross_color(x, y, active)

    # Outer bloom on ivory
    d = dist_center(x, y)
    halo_mid = 5.5 if active else 4.5
    bloom_r = 13.0 if active else 10.0
    if d < bloom_r:
        target = HALO_A if active else HALO
        t = (bloom_r - d) / max(0.001, bloom_r - halo_mid) * 0.55
        return blend(marble(x, y), target, t)

    return marble(x, y)


def top_pixel(x: int, y: int, active: bool = False) -> tuple:
    if x == 0 or x == SIZE - 1 or y == 0 or y == SIZE - 1: return RIM
    if x == 1 or x == SIZE - 2 or y == 1 or y == SIZE - 2: return EDGE

    halo = halo_disc(x, y, active)
    if halo is not None:
        return halo

    if in_vertical_arm(x, y) or in_horizontal_arm(x, y):
        return cross_color(x, y, active)

    dx = x - CX
    dy = y - CY
    d = (dx * dx + dy * dy) ** 0.5
    halo_mid = 5.5 if active else 4.5

    # Diagonal sunburst rays between halo edge and frame
    if halo_mid < d < 14.0 and abs(abs(dx) - abs(dy)) < 1.2:
        target = RAY_A if active else RAY
        t = 0.65 if active else 0.5
        return blend(marble(x, y), target, t)

    # Outer halo bloom
    bloom_r = 13.0 if active else 10.0
    if d < bloom_r:
        target = HALO_A if active else HALO
        t = (bloom_r - d) / max(0.001, bloom_r - halo_mid) * 0.55
        return blend(marble(x, y), target, t)

    return marble(x, y)


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
