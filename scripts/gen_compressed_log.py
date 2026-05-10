"""
Generate the Compressed Log block texture.

Approach: pull vanilla `minecraft:block/oak_log` straight out of the
Minecraft client jar and stamp a 2-px darkened border around it. This is
how Extra Utilities did its "compressed" series — the texture has to read
as obviously-the-same-material with one visual tell that says "stuffed
nine-into-one." Synthesising bark from scratch (the previous approach)
read as a stripey rectangle next to real wood and broke the illusion.

Vanilla oak_log.png is 4-bit indexed-colour, so the PNG decoder below
handles palette + bit-depth-4 unpacking in pure stdlib (no Pillow). If
NeoForge bumps Minecraft's version the jar path changes — update
CLIENT_JAR.

Run: python scripts/gen_compressed_log.py
Output: src/main/resources/assets/projecttitancore/textures/block/compressed_log.png
"""

import os
import struct
import zipfile
import zlib

CLIENT_JAR = (
    r"C:\Users\lind3\AppData\Roaming\PrismLauncher\libraries\net\minecraft\client"
    r"\1.21.1-20240808.144430\client-1.21.1-20240808.144430-extra.jar"
)
TEXTURE_PATH_IN_JAR = "assets/minecraft/textures/block/oak_log.png"

RIM_COLOR    = (26, 18, 10, 255)   # outermost 1-px pitch-dark frame
GROOVE_COLOR = (54, 38, 22, 255)   # inner 1-px slightly-lighter dark


# --- PNG decode (palette + RGB/RGBA, no Pillow) ----------------------------

def _iter_chunks(data: bytes):
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    pos = 8
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        ctype = data[pos + 4:pos + 8]
        cdata = data[pos + 8:pos + 8 + length]
        pos += 8 + length + 4
        yield ctype, cdata


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def decode_png_to_rgba(data: bytes):
    width = height = bit_depth = color_type = 0
    palette = []
    trns = None
    idat = b""

    for ctype, cdata in _iter_chunks(data):
        if ctype == b"IHDR":
            width, height, bit_depth, color_type = struct.unpack(">IIBB", cdata[:10])
        elif ctype == b"PLTE":
            palette = [tuple(cdata[i:i + 3]) for i in range(0, len(cdata), 3)]
        elif ctype == b"tRNS":
            trns = cdata
        elif ctype == b"IDAT":
            idat += cdata
        elif ctype == b"IEND":
            break

    raw = zlib.decompress(idat)

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}.get(color_type)
    if channels is None:
        raise ValueError(f"unsupported color_type {color_type}")
    bits_per_pixel = bit_depth * channels
    bpp = max(1, bits_per_pixel // 8)
    scanline_bytes = (width * bits_per_pixel + 7) // 8

    rows = bytearray()
    prev = bytes(scanline_bytes)
    pos = 0
    for _ in range(height):
        ftype = raw[pos]
        pos += 1
        row = bytearray(raw[pos:pos + scanline_bytes])
        pos += scanline_bytes
        if ftype == 1:
            for i in range(scanline_bytes):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + left) & 0xFF
        elif ftype == 2:
            for i in range(scanline_bytes):
                row[i] = (row[i] + prev[i]) & 0xFF
        elif ftype == 3:
            for i in range(scanline_bytes):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + (left + prev[i]) // 2) & 0xFF
        elif ftype == 4:
            for i in range(scanline_bytes):
                left = row[i - bpp] if i >= bpp else 0
                up_left = prev[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + _paeth(left, prev[i], up_left)) & 0xFF
        elif ftype != 0:
            raise ValueError(f"bad filter {ftype}")
        rows += row
        prev = bytes(row)

    pixels = [[(0, 0, 0, 255)] * width for _ in range(height)]
    if color_type == 3 and bit_depth == 4:
        for y in range(height):
            base = y * scanline_bytes
            for x in range(width):
                b = rows[base + x // 2]
                idx = (b >> 4) if x % 2 == 0 else (b & 0x0F)
                r, g, bl = palette[idx]
                a = trns[idx] if trns and idx < len(trns) else 255
                pixels[y][x] = (r, g, bl, a)
    elif color_type == 3 and bit_depth == 8:
        for y in range(height):
            for x in range(width):
                idx = rows[y * scanline_bytes + x]
                r, g, bl = palette[idx]
                a = trns[idx] if trns and idx < len(trns) else 255
                pixels[y][x] = (r, g, bl, a)
    elif color_type == 2 and bit_depth == 8:
        for y in range(height):
            for x in range(width):
                p = y * scanline_bytes + x * 3
                pixels[y][x] = (rows[p], rows[p + 1], rows[p + 2], 255)
    elif color_type == 6 and bit_depth == 8:
        for y in range(height):
            for x in range(width):
                p = y * scanline_bytes + x * 4
                pixels[y][x] = (rows[p], rows[p + 1], rows[p + 2], rows[p + 3])
    else:
        raise ValueError(f"unsupported PNG: color_type={color_type} bit_depth={bit_depth}")
    return pixels


# --- PNG encode (RGBA8) ----------------------------------------------------

def _make_chunk(ctype: bytes, cdata: bytes) -> bytes:
    payload = ctype + cdata
    return struct.pack(">I", len(cdata)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def encode_rgba_png(pixels) -> bytes:
    h = len(pixels)
    w = len(pixels[0])
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = _make_chunk(b"IHDR", struct.pack(">II", w, h) + bytes([8, 6, 0, 0, 0]))
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)
    idat = _make_chunk(b"IDAT", zlib.compress(raw, 9))
    iend = _make_chunk(b"IEND", b"")
    return sig + ihdr + idat + iend


# --- Border overlay --------------------------------------------------------

def apply_border(pixels):
    h = len(pixels)
    w = len(pixels[0])
    for y in range(h):
        for x in range(w):
            if x == 0 or x == w - 1 or y == 0 or y == h - 1:
                pixels[y][x] = RIM_COLOR
            elif x == 1 or x == w - 2 or y == 1 or y == h - 2:
                pixels[y][x] = GROOVE_COLOR
    return pixels


def main():
    if not os.path.exists(CLIENT_JAR):
        raise SystemExit(
            f"Vanilla client jar not found:\n  {CLIENT_JAR}\n"
            "Update CLIENT_JAR if NeoForge bumped its Minecraft version."
        )
    with zipfile.ZipFile(CLIENT_JAR) as z:
        png_bytes = z.read(TEXTURE_PATH_IN_JAR)

    pixels = decode_png_to_rgba(png_bytes)
    pixels = apply_border(pixels)

    script_dir = os.path.dirname(os.path.abspath(__file__))
    out_path = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "block", "compressed_log.png",
    )
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as f:
        f.write(encode_rgba_png(pixels))
    print(f"  Written: {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()
