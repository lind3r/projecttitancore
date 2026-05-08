"""
Downscale the in-world Titan Core screenshot into the JEI / hotbar / inventory item icon.

The cage block model reads as a confusing "frame with a window" when rendered through
the default block→item iso transforms (the cage is mostly empty space; the BER sphere
and projection that fill it in-world don't run for items). So instead of fighting the
3D model, we ship a flat 2D sprite — a smoothly downscaled in-world screenshot of the
block.

Workflow:
  1. In-game: place the Core, F1 to hide HUD, take a 3/4-angle screenshot against a
     plain backdrop. Crop tightly to the block and alpha out the background in any
     image editor (the existing source was hand-prepped, but ImageMagick / GIMP / etc.
     all work).
  2. Drop the prepped PNG at scripts/titan_core_source.png (overwrites the previous one).
  3. Run this script.
  4. ./gradlew deployToInstance

Output: src/main/resources/assets/projecttitancore/textures/item/titan_core.png (64×64).
The item model (models/item/titan_core.json) parents from minecraft:item/generated and
points layer0 at this texture, so JEI / hotbar / inventory / item-frames all use the
flat sprite while the placed block keeps its 3D glass-cage model.
"""

import os
from PIL import Image

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SOURCE_PATH = os.path.join(SCRIPT_DIR, "titan_core_source.png")
OUTPUT_PATH = os.path.join(
    SCRIPT_DIR, "..", "src", "main", "resources",
    "assets", "projecttitancore", "textures", "item", "titan_core.png",
)

# 32×32 with nearest-neighbour gives chunky pixel-art edges that match the shard
# and crown icons. Bump to 64 + Lanczos if you want a smooth-render machine look.
TARGET_SIZE = 32


def main() -> None:
    if not os.path.exists(SOURCE_PATH):
        raise SystemExit(f"Source image not found: {SOURCE_PATH}")

    src = Image.open(SOURCE_PATH).convert("RGBA")

    # Pad to square first so the aspect ratio is preserved when we resize. The hand-
    # prepped source is already square, but a re-screenshot might not be.
    side = max(src.size)
    if src.size != (side, side):
        canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        canvas.paste(src, ((side - src.size[0]) // 2, (side - src.size[1]) // 2))
        src = canvas

    icon = src.resize((TARGET_SIZE, TARGET_SIZE), Image.Resampling.NEAREST)
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    icon.save(OUTPUT_PATH, "PNG", optimize=True)
    print(f"Wrote {os.path.relpath(OUTPUT_PATH, SCRIPT_DIR)} ({TARGET_SIZE}×{TARGET_SIZE})")


if __name__ == "__main__":
    main()
