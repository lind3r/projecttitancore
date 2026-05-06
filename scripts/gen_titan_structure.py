"""
Generate the Titan projection structure data + cumulative preview images.

The Titan Core projects a translucent holographic statue above the crafting
beam. Each tier crafted unlocks one cumulative section of the silhouette:

  Tier 1: plinth, feet, lower legs (knee down)
  Tier 2: upper legs, hips, gold belt
  Tier 3: torso (waist + chest), arms, pauldrons
  Tier 4: head, sword (held forward)

Run: python scripts/gen_titan_structure.py

Outputs:
  src/main/resources/data/projecttitancore/structure/titan.json
  scripts/preview_titan/tier1.png   - front view, T1 only
  scripts/preview_titan/tier2.png   - front view, T1+T2
  scripts/preview_titan/tier3.png   - front view, T1+T2+T3
  scripts/preview_titan/tier4.png   - front view, full
  scripts/preview_titan/side.png    - side view, full
  scripts/preview_titan/top.png     - top view, full

Coordinate system (anchor = top of crafting beam):
  +X = titan's right (sword side)
  +Y = up
  +Z = forward (toward camera in front view)

To tweak proportions: each body part is its own function returning a list of
(x, y, z, color) tuples. Modify those, re-run, inspect the previews. JSON only
gets written if previews look right.
"""

import json
import os
import struct
import zlib

# --------------------------------------------------------------------------- #
# Paths                                                                       #
# --------------------------------------------------------------------------- #

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JSON_OUT = os.path.join(
    ROOT, "src", "main", "resources", "data", "projecttitancore",
    "structure", "titan.json",
)
PREVIEW_DIR = os.path.join(ROOT, "scripts", "preview_titan")

# --------------------------------------------------------------------------- #
# Palette (matches CLAUDE.md "Visual Theme" - holy ivory/gold)                #
# --------------------------------------------------------------------------- #

PALETTE = {
    "ivory":  (236, 228, 208),
    "shadow": (138, 102,  32),
    "gold":   (255, 224,  84),
}

# --------------------------------------------------------------------------- #
# Voxel helpers                                                               #
# --------------------------------------------------------------------------- #

def cube(xr, yr, zr, color):
    """Solid filled cuboid. Args are (start, end_exclusive) tuples."""
    return [
        (x, y, z, color)
        for x in range(xr[0], xr[1])
        for y in range(yr[0], yr[1])
        for z in range(zr[0], zr[1])
    ]


def overlay(base, top):
    """Apply `top` voxels on top of `base`, replacing color where positions match."""
    by_pos = {(x, y, z): c for (x, y, z, c) in base}
    for (x, y, z, c) in top:
        by_pos[(x, y, z)] = c
    return [(x, y, z, c) for (x, y, z), c in by_pos.items()]


# --------------------------------------------------------------------------- #
# TIER 1 - feet up to knee                                                    #
# --------------------------------------------------------------------------- #

def t1_plinth():
    base = cube((-3, 4), (0, 2), (-3, 4), "ivory")
    # gold corners only - subtle "altar" feel without dominating the silhouette
    corners = [(x, 1, z, "gold") for x in (-3, 3) for z in (-3, 3)]
    return overlay(base, corners)


def t1_feet():
    voxels = []
    for x_start in (-3, 1):  # left foot, right foot (1-block gap at x=0)
        voxels += cube((x_start, x_start + 3), (2, 4), (-2, 3), "ivory")
        # gold toe-tip strip on front face
        voxels += cube((x_start, x_start + 3), (2, 3), (2, 3), "gold")
    return voxels


def t1_lower_legs():
    voxels = []
    for x_start in (-3, 1):
        voxels += cube((x_start, x_start + 3), (4, 12), (-1, 2), "ivory")
        # outer-side shadow stripe (one column on the body's outer edge)
        outer_x = x_start if x_start == -3 else x_start + 2
        voxels += cube((outer_x, outer_x + 1), (4, 12), (-1, 2), "shadow")
    return voxels


def tier1_voxels():
    return t1_plinth() + t1_feet() + t1_lower_legs()


# --------------------------------------------------------------------------- #
# TIER 2 - upper legs, hips, belt                                             #
# --------------------------------------------------------------------------- #

def t2_upper_legs():
    voxels = []
    for x_start in (-3, 1):
        voxels += cube((x_start, x_start + 3), (12, 21), (-1, 2), "ivory")
        outer_x = x_start if x_start == -3 else x_start + 2
        voxels += cube((outer_x, outer_x + 1), (12, 21), (-1, 2), "shadow")
    return voxels


def t2_hips_belt():
    voxels = []
    voxels += cube((-3, 4), (21, 23), (-2, 3), "ivory")    # hips
    voxels += cube((-3, 4), (23, 24), (-2, 3), "gold")     # belt
    voxels += cube((-3, 4), (24, 25), (-2, 3), "shadow")   # under-belt seam
    return voxels


def tier2_voxels():
    return t2_upper_legs() + t2_hips_belt()


# --------------------------------------------------------------------------- #
# TIER 3 - torso, arms, pauldrons                                             #
# --------------------------------------------------------------------------- #

def t3_waist():
    return cube((-3, 4), (25, 30), (-2, 3), "ivory")


def t3_chest():
    voxels = cube((-4, 5), (30, 35), (-2, 3), "ivory")     # broader shoulders
    voxels += cube((-4, 5), (35, 36), (-2, 3), "shadow")   # clavicle seam
    return voxels


def t3_arms():
    voxels = []
    # Each arm: 2 wide, 12 tall, 3 deep. Sits flush against hip/belt at hip
    # height, and overlaps the (broader) chest's outer column at chest height -
    # which naturally fuses arm into shoulder. Outer column gets shadow stripe.
    for x_start in (-5, 4):
        voxels += cube((x_start, x_start + 2), (22, 34), (-1, 2), "ivory")
        outer_x = x_start if x_start == -5 else x_start + 1
        voxels += cube((outer_x, outer_x + 1), (22, 34), (-1, 2), "shadow")

    # Pauldrons - 3-wide caps overhanging the arm by 1 block outboard.
    for x_start in (-6, 4):
        voxels += cube((x_start, x_start + 3), (34, 35), (-2, 3), "ivory")
        voxels += cube((x_start, x_start + 3), (34, 35), (2, 3), "gold")
    return voxels


def tier3_voxels():
    return t3_waist() + t3_chest() + t3_arms()


# --------------------------------------------------------------------------- #
# TIER 4 - head, sword                                                        #
# --------------------------------------------------------------------------- #

def t4_neck_head():
    voxels = []
    voxels += cube((-1, 2), (36, 37), (-1, 2), "ivory")    # neck
    voxels += cube((-2, 3), (37, 42), (-2, 3), "ivory")    # head
    # eyes (gold) on the front face, centered
    voxels += [(-1, 39, 2, "gold"), (1, 39, 2, "gold")]
    # mouth/jaw shadow line
    voxels += cube((-1, 2), (37, 38), (2, 3), "shadow")
    return voxels


def t4_sword():
    """Held in the right hand at hip height, blade pointing forward (+Z).

    Hilt sits one block outboard of the right arm (x=6) at hand height (y=22).
    Pommel is tucked behind the hand; crossguard runs across X; blade extends
    straight forward in +Z. The right arm's outermost column at hand height is
    painted gold to read as fingers gripping the hilt.
    """
    voxels = []
    voxels.append((6, 22, -1, "gold"))                      # pommel (behind hand)
    voxels.append((6, 22, 0, "shadow"))                     # leather grip
    voxels += [(x, 22, 1, "gold") for x in range(5, 8)]     # crossguard (3 wide along X)
    voxels += cube((6, 8), (22, 23), (2, 18), "ivory")      # blade body (2 wide, 16 long)
    voxels.append((6, 22, 18, "ivory"))                     # blade tip
    # finger - paint the rightmost arm column at hand height
    voxels.append((5, 22, 0, "gold"))
    return voxels


def tier4_voxels():
    return t4_neck_head() + t4_sword()


# --------------------------------------------------------------------------- #
# Assembly + JSON output                                                      #
# --------------------------------------------------------------------------- #

TIER_BUILDERS = {
    1: tier1_voxels,
    2: tier2_voxels,
    3: tier3_voxels,
    4: tier4_voxels,
}


def bucket_by_color(voxels):
    """Group voxels by color so the renderer can build one VertexBuffer per color."""
    out = {"ivory": [], "shadow": [], "gold": []}
    for x, y, z, color in voxels:
        out[color].append([x, y, z])
    return out


def write_json(tiers):
    os.makedirs(os.path.dirname(JSON_OUT), exist_ok=True)
    payload = {
        "tiers": [
            {"tier": n, "colors": bucket_by_color(tiers[n])}
            for n in sorted(tiers)
        ],
    }
    with open(JSON_OUT, "w") as f:
        json.dump(payload, f, separators=(",", ":"))
    print(f"wrote {os.path.relpath(JSON_OUT, ROOT)}")
    for n in sorted(tiers):
        print(f"  tier {n}: {len(tiers[n])} voxels")
    print(f"  total: {sum(len(v) for v in tiers.values())} voxels")


# --------------------------------------------------------------------------- #
# PNG preview encoder (matches gen_block_texture.py style - no PIL dep)       #
# --------------------------------------------------------------------------- #

def _png_chunk(chunk_type, data):
    payload = chunk_type + data
    return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)


def write_png(path, pixels, width, height):
    """pixels is a flat list of (r, g, b) tuples in row-major top-to-bottom order."""
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = _png_chunk(b"IHDR", struct.pack(">II", width, height) + bytes([8, 2, 0, 0, 0]))  # 8-bit RGB
    raw = b""
    for row in range(height):
        raw += b"\x00"
        for col in range(width):
            raw += bytes(pixels[row * width + col])
    idat = _png_chunk(b"IDAT", zlib.compress(raw, 9))
    iend = _png_chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(sig + ihdr + idat + iend)


# --------------------------------------------------------------------------- #
# Preview renderer                                                            #
# --------------------------------------------------------------------------- #

PIXELS_PER_VOXEL = 6
BG = (24, 26, 38)         # near-black for projection feel
GRID = (38, 40, 56)       # subtle voxel-cell border


def _draw_voxel(canvas, w, h, ux, vy, color):
    base_x = ux * PIXELS_PER_VOXEL
    base_y = vy * PIXELS_PER_VOXEL
    for dy in range(PIXELS_PER_VOXEL):
        for dx in range(PIXELS_PER_VOXEL):
            px = base_x + dx
            py = base_y + dy
            if 0 <= px < w and 0 <= py < h:
                # darker on outer edges so voxel cells stay readable
                if dx == 0 or dy == 0:
                    canvas[py * w + px] = tuple(int(c * 0.55) for c in color)
                else:
                    canvas[py * w + px] = color


def render_view(voxels, view, path):
    """view: 'front' (XY plane, looking -Z), 'side' (ZY, looking -X), 'top' (XZ, looking -Y)."""
    if view == "front":
        u_idx, v_idx, depth_idx = 0, 1, 2
        depth_sign = +1   # +Z is closer to viewer
    elif view == "side":
        u_idx, v_idx, depth_idx = 2, 1, 0
        depth_sign = +1   # +X is closer
    else:  # top
        u_idx, v_idx, depth_idx = 0, 2, 1
        depth_sign = +1   # +Y is closer

    us = [v[u_idx] for v in voxels]
    vs = [v[v_idx] for v in voxels]
    u_min, u_max = min(us) - 1, max(us) + 1
    v_min, v_max = min(vs) - 1, max(vs) + 1

    width = (u_max - u_min + 1) * PIXELS_PER_VOXEL
    height = (v_max - v_min + 1) * PIXELS_PER_VOXEL
    canvas = [BG] * (width * height)

    # subtle grid
    for y in range(0, height, PIXELS_PER_VOXEL):
        for x in range(width):
            canvas[y * width + x] = GRID
    for x in range(0, width, PIXELS_PER_VOXEL):
        for y in range(height):
            canvas[y * width + x] = GRID

    # draw far-to-near so closer voxels paint over farther ones
    sorted_voxels = sorted(voxels, key=lambda v: depth_sign * v[depth_idx])

    for v in sorted_voxels:
        ux = v[u_idx] - u_min
        vy = v[v_idx] - v_min
        # flip vertical so +Y/+Z appears at the top of the image
        vy = (v_max - v_min) - vy
        _draw_voxel(canvas, width, height, ux, vy, PALETTE[v[3]])

    write_png(path, canvas, width, height)
    print(f"wrote {os.path.relpath(path, ROOT)}  ({width}x{height})")


def write_previews(tiers):
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    cumulative = []
    for n in sorted(tiers):
        cumulative = cumulative + tiers[n]
        render_view(cumulative, "front", os.path.join(PREVIEW_DIR, f"tier{n}.png"))

    full = cumulative
    render_view(full, "side", os.path.join(PREVIEW_DIR, "side.png"))
    render_view(full, "top",  os.path.join(PREVIEW_DIR, "top.png"))


# --------------------------------------------------------------------------- #
# Main                                                                        #
# --------------------------------------------------------------------------- #

if __name__ == "__main__":
    tiers = {n: builder() for n, builder in TIER_BUILDERS.items()}
    write_json(tiers)
    write_previews(tiers)
