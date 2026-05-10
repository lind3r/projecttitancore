"""
Generate Titan Relic textures for Project Titan Core.

Run from anywhere — outputs PNGs directly into the mod texture directory.
Usage: python gen_relic_texture.py

Currently generates the Crown of the Titan — the modpack-completion reward dropped
by the Titan Trial gateway. Follows the gen_shard_texture.py pattern so it can grow
to host more relics later (just add a RELICS entry).
"""

import struct
import zlib
import os

# Holy palette (see CLAUDE.md "Visual Theme")
PALETTE = {
    "T": (0, 0, 0, 0),                 # transparent
    "D": (0x46, 0x36, 0x12, 255),      # frame outer dark gold rim
    "G": (0xDC, 0xAC, 0x2A, 255),      # gold body
    "L": (0xFF, 0xE0, 0x54, 255),      # gold highlight
    "S": (0xFF, 0xFC, 0xAF, 255),      # halo bright (active)
}

# Crown of the Titan — three tall jewel-tipped spires above a thin gold band.
# Layout is 3-4-3: outer spires are 3 cols wide (DLD), the centre spire is
# 4 cols (DLLD) so the whole crown can be perfectly mirrored around the
# image's vertical centerline (between cols 7 and 8). The centre spire's
# extra width also gives it a slight "main spire" hierarchy.
# Inset band jewels at cols 3 / 7-8 / 12 align vertically with each spire.
CROWN_GRID = [
    "TTTTTTTTTTTTTTTT",  # 0
    "TTTTTTTTTTTTTTTT",  # 1
    "TTTTTTTTTTTTTTTT",  # 2
    "TTTSTTTSSTTTSTTT",  # 3 — jewel tips (cols 3, 7-8, 12)
    "TTDLDTDLLDTDLDTT",  # 4 — frame + neck highlight
    "TTDGDTDGGDTDGDTT",  # 5 — spire stem
    "TTDGDTDGGDTDGDTT",  # 6 — spire stem
    "TTDGDTDGGDTDGDTT",  # 7 — spire stem
    "TDDDDDDDDDDDDDDT",  # 8 — band top dark rim
    "TDLLLLLLLLLLLLDT",  # 9 — band top highlight
    "TDGSGGGSSGGGSGDT",  # 10 — band body w/ inset jewels (aligned to spires)
    "TDLLLLLLLLLLLLDT",  # 11 — band bottom highlight
    "TDDDDDDDDDDDDDDT",  # 12 — band bottom dark rim
    "TTTTTTTTTTTTTTTT",  # 13
    "TTTTTTTTTTTTTTTT",  # 14
    "TTTTTTTTTTTTTTTT",  # 15
]

RELICS = {
    "crown_of_the_titan": CROWN_GRID,
}


def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
    payload = chunk_type + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def build_png(pixels):
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


def render(grid):
    assert len(grid) == 16, f"grid must have 16 rows, got {len(grid)}"
    pixels = []
    for i, row_str in enumerate(grid):
        assert len(row_str) == 16, f"row {i} has {len(row_str)} chars: {row_str!r}"
        row = []
        for ch in row_str:
            assert ch in PALETTE, f"unknown palette key '{ch}' in row {i}"
            row.append(PALETTE[ch])
        pixels.append(row)
    return build_png(pixels)


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    texture_dir = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "item",
    )
    os.makedirs(texture_dir, exist_ok=True)

    for slug, grid in RELICS.items():
        out_path = os.path.join(texture_dir, f"{slug}.png")
        with open(out_path, "wb") as f:
            f.write(render(grid))
        print(f"  Written: {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()
