"""
Generate trophy item textures for Project Titan Core.

Run from anywhere — outputs PNGs directly into the mod texture directory.
Usage: python gen_trophy_texture.py

To add a new tier: define its color palette in TIERS and its pixel grid in PIXEL_GRIDS.
"""

import struct
import zlib
import os

# --- Color palettes per tier ---
# Each palette: T=transparent, D=dark outline, G=main color, L=light highlight, S=shine, B=base

TIERS = {
    1: {
        "name": "trophy_tier_1",
        "T": (0,   0,   0,   0),
        "D": (80,  50,  8,   255),  # dark brown outline
        "G": (205, 155, 29,  255),  # gold
        "L": (255, 210, 0,   255),  # light gold
        "S": (255, 248, 180, 255),  # shine
        "B": (120, 80,  15,  255),  # darker base
    },
    2: {
        "name": "trophy_tier_2",
        "T": (0,   0,   0,   0),
        "D": (70,  72,  80,  255),  # dark outline
        "G": (185, 190, 200, 255),  # iron silver
        "L": (225, 228, 238, 255),  # light silver
        "S": (248, 250, 255, 255),  # shine
        "B": (130, 133, 143, 255),  # darker silver base
    },
    3: {
        "name": "trophy_tier_3",
        "T": (0,   0,   0,   0),
        "D": (0,   90,  110, 255),  # dark teal outline
        "G": (80,  215, 235, 255),  # diamond cyan
        "L": (150, 245, 255, 255),  # light aqua
        "S": (225, 255, 255, 255),  # shine
        "B": (30,  145, 175, 255),  # darker teal base
    },
}

# --- Pixel grids (16x16, one char per pixel using palette keys) ---
# T=transparent, D=dark outline, G=main color, L=light, S=shine, B=base

PIXEL_GRIDS = {
    1: [
        "TTTTTTTTTTTTTTTT",  # 0
        "TTTDDDDDDDDDDTTT",  # 1  cup top edge
        "TTDGGGGGGGGGGDTT",  # 2  cup interior
        "TTDSLGGGGGGGGGDTT" [:16],  # 3  shine top-left — truncated to 16
        "TTDGGGGGGGGGGDTT",  # 4
        "TTTDGGGGGGGGDTTT",  # 5  cup sides narrow
        "TTTTDGGGGGGDTTTT",  # 6
        "TTTTDDGGGGDDTTTT",  # 7  cup foot
        "TTTTTTDGGDTTTTTT",  # 8  stem
        "TTTTTTDGGDTTTTTT",  # 9  stem
        "TTTTTTDGGDTTTTTT",  # 10 stem
        "TTTTDDDGGDDDTTTT",  # 11 base flare
        "TTTDBBBBBBBBDTTT",  # 12 base
        "TTDDBBBBBBBBDDTT",  # 13 base wider
        "TTDDDDDDDDDDDDTT",  # 14 base bottom
        "TTTTTTTTTTTTTTTT",  # 15
    ],
}

# Fix: row 3 in the grid above has 17 chars due to the comment, define clean grids here
PIXEL_GRIDS[2] = PIXEL_GRIDS[1] = [
    "TTTTTTTTTTTTTTTT",
    "TTTDDDDDDDDDDTTT",
    "TTDGGGGGGGGGGDTT",
    "TTDSLGGGGGGGGDTT",
    "TTDGGGGGGGGGGDTT",
    "TTTDGGGGGGGGDTTT",
    "TTTTDGGGGGGDTTTT",
    "TTTTDDGGGGDDTTTT",
    "TTTTTTDGGDTTTTTT",
    "TTTTTTDGGDTTTTTT",
    "TTTTTTDGGDTTTTTT",
    "TTTTDDDGGDDDTTTT",
    "TTTDBBBBBBBBDTTT",
    "TTDDBBBBBBBBDDTT",
    "TTDDDDDDDDDDDDTT",
    "TTTTTTTTTTTTTTTT",
]
PIXEL_GRIDS[3] = PIXEL_GRIDS[1]


def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
    payload = chunk_type + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def build_png(pixels: list[list[tuple]]) -> bytes:
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = make_chunk(b"IHDR", struct.pack(">II", 16, 16) + bytes([8, 6, 0, 0, 0]))

    raw = b""
    for row in pixels:
        raw += b"\x00"  # filter type: None
        for px in row:
            raw += bytes(px)

    idat = make_chunk(b"IDAT", zlib.compress(raw, 9))
    iend = make_chunk(b"IEND", b"")
    return sig + ihdr + idat + iend


def generate_tier(tier_id: int):
    palette = TIERS[tier_id]
    grid = PIXEL_GRIDS[tier_id]

    assert len(grid) == 16, f"Tier {tier_id} grid must have 16 rows"
    for i, row in enumerate(grid):
        assert len(row) == 16, f"Tier {tier_id} row {i} must have 16 chars, got {len(row)}: {row!r}"

    pixels = []
    for row_str in grid:
        row = []
        for ch in row_str:
            assert ch in palette, f"Unknown palette key '{ch}' in tier {tier_id} grid"
            row.append(palette[ch])
        pixels.append(row)

    return build_png(pixels)


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    texture_dir = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "item"
    )
    os.makedirs(texture_dir, exist_ok=True)

    for tier_id, tier_info in TIERS.items():
        png_data = generate_tier(tier_id)
        out_path = os.path.join(texture_dir, f"{tier_info['name']}.png")
        with open(out_path, "wb") as f:
            f.write(png_data)
        print(f"  Written: {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()