"""
Generate block textures for Project Titan Core - holy theme.

The block uses a multi-cuboid "glass cage" model — gold edge frame around 6
inset translucent glass panels — so this script emits two texture sets:

  1. Cage textures (the live ones referenced by titan_core.json):
       titan_core_frame.png  — solid gold metal, used on the 12 edge cuboids
       titan_core_glass.png  — translucent ivory, used on the 6 inset panes

  2. Legacy single-face textures (titan_core.png / titan_core_active.png):
       still emitted for the block's particle texture (so break particles read
       as gold-and-ivory) and for previewing the centre motif. The block model
       itself no longer references titan_core.png on any face.

Run: python scripts/gen_block_texture.py

The single face is built in layers:
  rim/edge frame → halo disc at centre → motif (selectable) →
  diagonal sunburst rays → outer halo bloom → marble background.

Five motifs are bundled — see MOTIFS below. To swap which one is "live"
(written to titan_core.png / titan_core_active.png), change MOTIF and re-run.
Every motif also emits an inactive preview to `scripts/preview_titan_core/`
so they can be compared side by side without rebuilding the mod.
"""

import math
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


def cross_body(active: bool) -> tuple:
    return blend(GOLD, GOLDHI, 0.35) if active else GOLD


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
    if (x * 3 + y * 5) % 17 == 0 and (x + y * 2) % 13 == 0: return IVORY_SH
    if (x * 7 + y * 3) % 23 == 1 and (x + y) % 5 == 0:     return IVORY_SH
    return IVORY


# ---------------------------------------------------------------------------
# Motifs — each returns the gold-coloured pixel for the centre design, or
# None when the pixel falls outside the motif (caller falls through to the
# halo bloom + marble layers).
# ---------------------------------------------------------------------------
def in_vertical_arm(x: int, y: int) -> bool:
    return 14 <= x <= 17 and 4 <= y <= 27


def in_horizontal_arm(x: int, y: int) -> bool:
    return 14 <= y <= 17 and 4 <= x <= 27


def motif_cross(x: int, y: int, active: bool):
    """Greek cross — original design, retained for reference."""
    in_v = in_vertical_arm(x, y)
    in_h = in_horizontal_arm(x, y)
    if not (in_v or in_h):
        return None
    body = cross_body(active)
    if in_v and in_h:
        return GOLDHI
    if in_v:
        if y == 4:  return GOLDHI
        if y == 27: return GOLDSH
        if x == 14: return GOLDHI
        if x == 17: return GOLDSH
        return body
    if x == 4:  return GOLDHI
    if x == 27: return GOLDSH
    if y == 14: return GOLDHI
    if y == 17: return GOLDSH
    return body


def _ring(d: float, inner: float, outer: float) -> bool:
    return inner <= d < outer


def motif_rings(x: int, y: int, active: bool):
    """Two concentric gold rings around the central halo."""
    d = dist_center(x, y)
    body = cross_body(active)
    if _ring(d, 11.0, 12.5):
        return GOLDSH if d > 12.0 else body
    if _ring(d, 6.8, 8.0):
        return body
    return None


def motif_eye(x: int, y: int, active: bool):
    """Single bold ring framing the halo — divine eye."""
    d = dist_center(x, y)
    body = cross_body(active)
    if _ring(d, 9.5, 12.5):
        if d > 12.0 or d < 10.0:
            return GOLDSH
        return body
    return None


def motif_rosette(x: int, y: int, active: bool):
    """Outer ring + 6 dots arranged on an inner circle (rose window)."""
    d = dist_center(x, y)
    dx, dy = x - CX, y - CY
    body = cross_body(active)
    if _ring(d, 11.0, 12.5):
        return GOLDSH if d > 12.0 else body
    for i in range(6):
        ang = i * math.pi / 3
        ox = math.cos(ang) * 8.0
        oy = math.sin(ang) * 8.0
        dd = (dx - ox) * (dx - ox) + (dy - oy) * (dy - oy)
        if dd < 1.6:
            return GOLDHI if dd < 0.5 else body
    return None


def motif_sunwheel(x: int, y: int, active: bool):
    """Outer ring + 8 spokes radiating from behind the halo."""
    d = dist_center(x, y)
    dx, dy = x - CX, y - CY
    body = cross_body(active)
    if _ring(d, 11.0, 12.5):
        return GOLDSH if d > 12.0 else body
    if 4.5 < d < 10.8:
        ang = math.atan2(dy, dx)
        seg = math.pi / 4  # 8 spokes, every 45°
        nearest = round(ang / seg) * seg
        delta = abs(((ang - nearest) + math.pi) % (math.pi * 2) - math.pi)
        if delta * d < 0.75:
            return body
    return None


MOTIFS = {
    "cross":    motif_cross,
    "rings":    motif_rings,
    "eye":      motif_eye,
    "rosette":  motif_rosette,
    "sunwheel": motif_sunwheel,
}

# Active motif written to the canonical titan_core*.png filenames.
# Change to swap which design ships in the JAR, then re-run the script.
MOTIF = "rosette"


# ---------------------------------------------------------------------------
# Pixel renderer — same texture used on every face of the block.
# ---------------------------------------------------------------------------
def face_pixel(x: int, y: int, motif_fn, active: bool = False) -> tuple:
    if x == 0 or x == SIZE - 1 or y == 0 or y == SIZE - 1: return RIM
    if x == 1 or x == SIZE - 2 or y == 1 or y == SIZE - 2: return EDGE

    halo = halo_disc(x, y, active)
    if halo is not None:
        return halo

    motif = motif_fn(x, y, active)
    if motif is not None:
        return motif

    dx = x - CX
    dy = y - CY
    d = (dx * dx + dy * dy) ** 0.5
    halo_mid = 5.5 if active else 4.5

    if halo_mid < d < 14.0 and abs(abs(dx) - abs(dy)) < 1.2:
        target = RAY_A if active else RAY
        t = 0.65 if active else 0.5
        return blend(marble(x, y), target, t)

    bloom_r = 13.0 if active else 10.0
    if d < bloom_r:
        target = HALO_A if active else HALO
        t = (bloom_r - d) / max(0.001, bloom_r - halo_mid) * 0.55
        return blend(marble(x, y), target, t)

    return marble(x, y)


def generate_face(motif_fn, active: bool = False) -> list:
    return [[face_pixel(x, y, motif_fn, active) for x in range(SIZE)] for y in range(SIZE)]


# ---------------------------------------------------------------------------
# Cage textures — the live block model uses these.
# Frame: solid gold metal with subtle highlight/shadow band so 2-px-thick
# edges read as bevelled rather than flat. Tileable.
# Glass: translucent ivory, slight reflective streak, mostly clear.
# ---------------------------------------------------------------------------
FRAME_SIZE = 32
GLASS_SIZE = 16


def frame_pixel(x: int, y: int) -> tuple:
    """Ivory-marble tile — same family as the holy_bricks palette so the
    cage reads as marble pillars rather than wooden trim. Top edge highlight,
    bottom edge shadow, gentle veining inside, with a single thin gold pinstripe
    halfway up to keep the holy accent. Size scales with FRAME_SIZE."""
    vein_spacing = max(4, FRAME_SIZE // 4)         # 4 veins across the tile
    if y == 0:
        return HALO_HI
    if y == FRAME_SIZE - 1:
        return IVORY_SH
    if y == FRAME_SIZE // 2:                       # gold pinstripe inlay
        return GOLD
    if x == 0:
        return blend(IVORY, HALO_HI, 0.4)
    if x == FRAME_SIZE - 1:
        return blend(IVORY, IVORY_SH, 0.4)
    if x % vein_spacing == 0:
        return blend(IVORY, IVORY_SH, 0.3)        # subtle vein
    if (x * 3 + y * 5) % 17 == 0:
        return IVORY_SH                            # marble fleck
    if (x + y) % 7 == 0:
        return blend(IVORY, HALO_HI, 0.25)         # subtle highlight
    return IVORY


def glass_pixel(x: int, y: int) -> tuple:
    """16×16 translucent ivory. Base ~15% opacity with a faint diagonal
    highlight band at ~30% to suggest reflection. Edges slightly tinted gold
    so adjacent panes read as having a thin gilded inner border."""
    base_alpha = 38           # ~15% opacity
    streak_alpha = 75         # ~30% opacity
    edge_alpha = 90           # subtle gold rim
    # Inner gold rim — 1-px frame inside the texture.
    if x == 0 or x == GLASS_SIZE - 1 or y == 0 or y == GLASS_SIZE - 1:
        return (GOLDHI[0], GOLDHI[1], GOLDHI[2], edge_alpha)
    # Diagonal highlight streak.
    if abs((x + y) - 14) <= 1 or abs((x + y) - 6) <= 0:
        return (HALO_HI[0], HALO_HI[1], HALO_HI[2], streak_alpha)
    return (IVORY[0], IVORY[1], IVORY[2], base_alpha)


def generate_frame() -> list:
    return [[frame_pixel(x, y) for x in range(FRAME_SIZE)] for y in range(FRAME_SIZE)]


def generate_glass() -> list:
    return [[glass_pixel(x, y) for x in range(GLASS_SIZE)] for y in range(GLASS_SIZE)]


def write_png(path: str, pixels: list, size: int = SIZE) -> None:
    with open(path, "wb") as f:
        f.write(build_png(pixels, size=size))


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    texture_dir = os.path.join(
        script_dir, "..", "src", "main", "resources",
        "assets", "projecttitancore", "textures", "block"
    )
    preview_dir = os.path.join(script_dir, "preview_titan_core")
    os.makedirs(texture_dir, exist_ok=True)
    os.makedirs(preview_dir, exist_ok=True)

    # Previews — every motif, inactive only (for quick comparison).
    for name, fn in MOTIFS.items():
        write_png(os.path.join(preview_dir, f"{name}.png"), generate_face(fn, False))
        print(f"  Preview: {name}")

    if MOTIF not in MOTIFS:
        raise SystemExit(f"MOTIF '{MOTIF}' not in MOTIFS — choose one of: {', '.join(MOTIFS)}")
    fn = MOTIFS[MOTIF]

    canonicals = [
        ("titan_core",        generate_face(fn, False)),
        ("titan_core_active", generate_face(fn, True)),
    ]
    for name, pixels in canonicals:
        out_path = os.path.join(texture_dir, f"{name}.png")
        write_png(out_path, pixels)
        print(f"  Live ({MOTIF}): {os.path.relpath(out_path, script_dir)}")

    # Cage textures — referenced by the live block model.
    cage_outputs = [
        ("titan_core_frame", generate_frame(), FRAME_SIZE),
        ("titan_core_glass", generate_glass(), GLASS_SIZE),
    ]
    for name, pixels, size in cage_outputs:
        out_path = os.path.join(texture_dir, f"{name}.png")
        write_png(out_path, pixels, size=size)
        print(f"  Cage:           {os.path.relpath(out_path, script_dir)}")


if __name__ == "__main__":
    main()
