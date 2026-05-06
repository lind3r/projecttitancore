"""
Generate Titan Shard item textures for Project Titan Core.

Run from anywhere — outputs PNGs directly into the mod texture directory.
Usage: python gen_shard_texture.py

All 10 tiers share a single shard silhouette (SHARD_GRID); the visual progression
is purely palette-driven. To add or retune a tier, edit its TIERS entry and rerun.
"""

import struct
import zlib
import os

# --- Palettes per tier ---
# Palette keys: T=transparent, D=dark outline, G=main body, L=highlight, S=shine pixel
# Arc: dim mineral (1) → kindled (2-3) → vital (4) → cool/aware (5-6) → radiant (7)
#      → ethereal ivory (8) → divine gold (9) → blazing capstone (10).
# Tiers 8-10 pull directly from the project's holy palette (see CLAUDE.md "Visual Theme").

TIERS = {
    1: {
        "name": "mote_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (60, 60, 60, 255),
        "G": (140, 140, 140, 255),
        "L": (200, 200, 200, 255),
        "S": (240, 240, 240, 255),
    },
    2: {
        "name": "ember_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (80, 30, 15, 255),
        "G": (170, 70, 30, 255),
        "L": (220, 120, 50, 255),
        "S": (255, 180, 80, 255),
    },
    3: {
        "name": "spark_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (120, 60, 10, 255),
        "G": (220, 140, 30, 255),
        "L": (255, 200, 80, 255),
        "S": (255, 250, 180, 255),
    },
    4: {
        "name": "pulse_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (100, 20, 30, 255),
        "G": (190, 40, 60, 255),
        "L": (230, 90, 110, 255),
        "S": (255, 170, 180, 255),
    },
    5: {
        "name": "echo_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (20, 50, 90, 255),
        "G": (50, 120, 180, 255),
        "L": (120, 180, 230, 255),
        "S": (200, 230, 255, 255),
    },
    6: {
        "name": "will_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (50, 20, 80, 255),
        "G": (110, 60, 160, 255),
        "L": (170, 130, 220, 255),
        "S": (225, 205, 250, 255),
    },
    7: {
        "name": "voice_of_the_titan",
        "T": (0, 0, 0, 0),
        "D": (20, 80, 90, 255),
        "G": (50, 160, 170, 255),
        "L": (120, 220, 225, 255),
        "S": (220, 255, 255, 255),
    },
    8: {
        "name": "soul_of_the_titan",
        "T": (0, 0, 0, 0),
        # Holy palette: ivory ethereal
        "D": (0x46, 0x36, 0x12, 255),
        "G": (0xEC, 0xE4, 0xD0, 255),
        "L": (0xFF, 0xF8, 0xD7, 255),
        "S": (0xFF, 0xFF, 0xF5, 255),
    },
    9: {
        "name": "ascendant_shard",
        "T": (0, 0, 0, 0),
        # Holy gold
        "D": (0x94, 0x6E, 0x12, 255),
        "G": (0xDC, 0xAC, 0x2A, 255),
        "L": (0xFF, 0xE0, 0x54, 255),
        "S": (0xFF, 0xFC, 0xAF, 255),
    },
    10: {
        "name": "heart_of_the_titan",
        "T": (0, 0, 0, 0),
        # Capstone — full blazing holy gold against the deepest frame brown
        "D": (0x46, 0x36, 0x12, 255),
        "G": (0xFF, 0xE0, 0x54, 255),
        "L": (0xFF, 0xFC, 0xAF, 255),
        "S": (0xFF, 0xFF, 0xEB, 255),
    },
}

# --- Shared shard silhouette (16x16) ---
# Symmetric diamond crystal, tip-up and tip-down, top-left highlight (L)
# with a single shine pixel (S) at the upper facet.

SHARD_GRID = [
    "TTTTTTTTTTTTTTTT",
    "TTTTTTTTTTTTTTTT",
    "TTTTTTTDDTTTTTTT",
    "TTTTTTDLGDTTTTTT",
    "TTTTTDLLGGDTTTTT",
    "TTTTDLLGGGGDTTTT",
    "TTTDLLSGGGGGDTTT",
    "TTDLLGGGGGGGGDTT",
    "TTDLGGGGGGGGGDTT",
    "TTTDLGGGGGGGDTTT",
    "TTTTDLGGGGGDTTTT",
    "TTTTTDLGGGDTTTTT",
    "TTTTTTDLGDTTTTTT",
    "TTTTTTTDDTTTTTTT",
    "TTTTTTTTTTTTTTTT",
    "TTTTTTTTTTTTTTTT",
]


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
    grid = SHARD_GRID

    assert len(grid) == 16, f"SHARD_GRID must have 16 rows"
    for i, row in enumerate(grid):
        assert len(row) == 16, f"SHARD_GRID row {i} must have 16 chars, got {len(row)}: {row!r}"

    pixels = []
    for row_str in grid:
        row = []
        for ch in row_str:
            assert ch in palette, f"Unknown palette key '{ch}' in tier {tier_id}"
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
