"""
Generate a pure-white beam texture for the Titan Core.

Why ship our own instead of using vanilla `textures/entity/beacon_beam.png`:
something in the Project Titan modpack (a mod that ships its own
`assets/minecraft/textures/entity/beacon_beam.png`) overrides the vanilla
beam with a teal-tinted variant, so any `BeaconRenderer.renderBeaconBeam`
call ends up multiplying our chosen tint against teal — pure-white tint
reads as teal, saturated tints (e.g. magenta) survive because the
multiplied channels happen to suppress the teal cast. By bundling our own
pure-white beam under the projecttitancore namespace we guarantee
`tint × texture = tint` no matter which mods are loaded.

Output: src/main/resources/assets/projecttitancore/textures/entity/titan_beam.png

Run: python scripts/gen_beam_texture.py

Texture is 16x16 (matches vanilla beacon_beam dimensions), all pixels white
with a subtle column-wise alpha variation so the beam keeps a faint vertical
stripe feel when scrolled — but all RGB values stay pure (255,255,255) so
nothing biases the tint we send from `TitanCoreRenderer`.
"""

import os
import random
import struct
import zlib


def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
    payload = chunk_type + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def build_png(pixels, w: int, h: int) -> bytes:
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = make_chunk(b"IHDR", struct.pack(">II", w, h) + bytes([8, 6, 0, 0, 0]))
    raw = b""
    for row in pixels:
        raw += b"\x00"
        for px in row:
            raw += bytes(px)
    idat = make_chunk(b"IDAT", zlib.compress(raw, 9))
    iend = make_chunk(b"IEND", b"")
    return sig + ihdr + idat + iend


W, H = 16, 16
random.seed(7)

column_alphas = []
for x in range(W):
    r = random.random()
    if r < 0.60:
        column_alphas.append(255)
    elif r < 0.90:
        column_alphas.append(200)
    else:
        column_alphas.append(140)

pixels = [[(255, 255, 255, column_alphas[x]) for x in range(W)] for _ in range(H)]

OUT = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "assets", "projecttitancore",
    "textures", "entity", "titan_beam.png",
)
OUT = os.path.normpath(OUT)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "wb") as f:
    f.write(build_png(pixels, W, H))
print(f"Wrote {OUT}")
