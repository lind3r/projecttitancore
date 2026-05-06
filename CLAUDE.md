# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Project Titan Core** is a NeoForge Minecraft mod (mod ID: `projecttitancore`) targeting Minecraft 1.21.1 with NeoForge 21.1.228. The mod source lives at the repo root. Java 21 is required.

### How this mod fits into the modpack

There are two related repos. Don't confuse them:

- **Project Titan** — the *modpack*, at `C:\Users\lind3\clones\project-titan\`. This is the shipped product.
- **Project Titan Core** (this repo) — a custom *mod* developed specifically for the modpack. The "Core" in the name means it's the **core / anchor mod of the pack**, not a reference to the in-game `titan_core` block. (The block happens to share the name; the mod is named for its role in the pack.)

The full third-party modlist is now packwiz-managed in the modpack repo at `clones/project-titan/mods/*.pw.toml`. The actual JARs still live in the Prism dev instance at `C:\Users\lind3\AppData\Roaming\PrismLauncher\instances\projecttitan\minecraft\mods\` — that's where to look when a decision here would benefit from knowing what other mods are loaded (compat, integration hooks, recipe overlap). For the structured list, packwiz workflow, junction setup, and quest editing, see `clones/project-titan/CLAUDE.md`.

When this mod gets a published release, wire it into the modpack via `packwiz url add` from the modpack repo (currently deferred — `projecttitancore` is the only mod the modpack ships with from the live install but doesn't track in packwiz).

**Modpack goal:** the player progresses through tiers via **FTB Quests**, with the final tier as the endgame goal. The endgame tier itself is not yet implemented. This mod's planned trophy → quest hook (see "Planned Content" below) is the bridge between this mod and that progression.

## Build & Deploy Workflow

After **any** code or resource/config change, always run `deployToInstance` immediately so the user can test right away. Do not wait to be asked.

## Keeping This File Current

When you add a new dev utility — texture/asset generator, gradle task, helper script under `scripts/`, or any other workflow tool — update CLAUDE.md in the same change so future sessions can find and reuse it. A one-line entry under the matching section (e.g. **Texture Generation**, **Build Commands**) is enough; mention the script path, what it produces, and how to extend it.

## Git Workflow

You have standing authorization to `git commit` and `git push` (to `master`) when a coherent chunk of work is done. Granted by the user 2026-05-06 — "feel free to commit (including push) when you feel it makes sense going forward."

Apply judgment:
- Commit when the changes form a sensible unit (one feature, one fix, one batch of related edits) — not after every micro-edit.
- Standard git safety still applies: never `--no-verify`, never force-push, never `git add -A` blindly (stage by name), never commit files that look like secrets, never commit `.claude/settings.local.json` or other clearly-local state unless explicitly asked.
- Never delete branches, force-push, or rewrite published history without asking first.
- If a pre-commit hook fails, fix the underlying issue and create a NEW commit — do not amend or `--no-verify`.

## Build Commands

Run from the repo root (Git Bash):

```bash
./gradlew deployToInstance   # Always use this — builds and copies JAR to Prism instance
./gradlew runClient          # Launch Minecraft client with the mod loaded
./gradlew runServer          # Launch dedicated server (headless)
./gradlew runData            # Run data generators (output to src/generated/resources/)
./gradlew runGameTestServer  # Run NeoForge GameTests and exit
./gradlew clean
./gradlew --refresh-dependencies
```

> `deployToInstance` reads the mods path from `local.properties` (`prism.instance.mods`). Gitignored, already configured.

## Key File Locations

- Language strings: `src/main/resources/assets/projecttitancore/lang/en_us.json`
- Mod metadata: `src/main/templates/META-INF/neoforge.mods.toml` (edit this, not a generated copy)
- Loot tables: `src/main/resources/data/projecttitancore/loot_table/blocks/` — **singular** `loot_table`, not `loot_tables` (1.21 rename)
- Data generator output: `src/generated/resources/` (auto-included in JAR)

## Visual Theme

Project Titan Core uses a **holy** palette — ivory marble base with gold accents and a divine glow at the centre. Reuse these colours in any new texture or GUI; do not introduce new hues unless the user asks for it.

| Role | Hex | RGB |
|---|---|---|
| Ivory marble (bg) | `#ECE4D0` | 236, 228, 208 |
| Ivory shaded (veining / GUI slot well variant) | `#D0C6AE` | 208, 198, 174 |
| Frame — outer dark gold rim | `#463612` | 70, 54, 18 |
| Border — gold trim, dividers, slot/gauge borders | `#8A6620` | 138, 102, 32 |
| Gold (cross body / main accent) | `#DCAC2A` | 220, 172, 42 |
| Gold highlight (energy fill, arrow fill, lit cross) | `#FFE054` | 255, 224, 84 |
| Gold shadow | `#946E12` | 148, 110, 18 |
| Halo (inactive) | `#FFF8D7` | 255, 248, 215 |
| Halo bright (inactive) | `#FFFFF5` | 255, 255, 245 |
| Halo (active, warmer) | `#FFFCAF` | 255, 252, 175 |
| Halo bright (active) | `#FFFFEB` | 255, 255, 235 |
| Sunburst ray | `#F5E0A2` | 245, 224, 162 |
| Holy blue (fluid gauge) | `#8AB6D8` | 138, 182, 216 |
| Slot well (dark warm) | `#2A1F08` | 42, 31, 8 |

**Where it's used:**
- Block textures — `scripts/gen_block_texture.py` (ivory base, Greek cross, halo disc, sunburst on top/bottom).
- GUI — `screen/TitanCoreScreen.java` (`COLOR_*` constants).

## Texture Generation

**Trophy items** — `scripts/gen_trophy_texture.py`. To add a tier: add a palette to `TIERS` and a grid to `PIXEL_GRIDS`, then run. Writes to `textures/item/`.

**Titan Core block** — `scripts/gen_block_texture.py`. Generates `titan_core.png` and `titan_core_active.png` (32×32) — the same texture is mapped to every face via `cube_all`. The centre motif is pluggable: `MOTIFS` registers `cross`, `rings`, `eye`, `rosette`, `sunwheel`. Change `MOTIF` near the top of the script and re-run to swap the live design. Every run also writes inactive previews of *all* motifs to `scripts/preview_titan_core/` (gitignored) so you can compare without rebuilding. To add a new motif, write a `motif_<name>(x, y, active)` function returning the gold-coloured pixel for any pixel inside the design (or `None` outside) and add it to `MOTIFS`. Do not hand-edit the PNGs.

**Building blocks (tileable)** — `scripts/gen_building_block_texture.py`. Variant-keyed: each entry in `VARIANTS` maps a name to a `render(x, y) -> RGBA` function and produces a 32×32 tileable PNG in `textures/block/`. To add a chiseled/etched sibling, write a new `render_<name>` function (you can reuse the brick layout in `render_holy_bricks` as a base) and add it to `VARIANTS`.

## Diagnosing Crashes

Primary log: `C:\Users\lind3\AppData\Roaming\PrismLauncher\instances\projecttitan\minecraft\logs\latest.log`

Search for `FATAL` or `ERROR` — the **first** one is always the root cause. Everything after (`"Cowardly refusing to send event"`) is cascading noise.

## Planned Content

### Titan Core — Remaining Steps

1. ✅ Block + BlockEntity shell, placeholder GUI
2. ✅ Full inventory (9+1 slots), EnergyStorage, FluidTank, capabilities, GUI with bars
3. ✅ Custom RecipeType + Tier 1 recipe JSON + crafting tick logic
4. ✅ Custom trophy item (`TrophyItem`, `trophy_tier_1`), texture via gen script
5. ⬜ FTB Quests integration (trophy in inventory → quest completion event)
6. ⬜ Structure building in world (design TBD)

### Holy Bricks — Chisel Mod Compat (TODO)

The 4 holy_bricks variants currently convert into each other via **vanilla stonecutter only** (12 stonecutting recipes, full cross-matrix). Popular chisel mods do **not** auto-pick up vanilla stonecutting — each one has its own mechanism:

- **Chipped** (most likely candidate for 1.21 NeoForge) — uses its own workbenches (Mason's, Carpenter's, …) and a JSON recipe format. Add a Chipped-format datapack in our mod gated on the mod being loaded.
- **Chisel** (tterrag) — registers "carving groups" via its API at mod load. Last 1.21 NeoForge status unknown; only relevant if updated.
- **Chisels & Bits** — voxel editing, not variant-swap. Not applicable.
- **Quark chisel** — partly reads vanilla stonecutting; mostly handles rotated variants.

Decide which chisel mod(s) ship in the modpack first, then add per-mod compat (gated by mod presence). Stonecutter remains the universal baseline regardless.

### Known Issues

- **Crafting beam culls when block is offscreen.** Despite `shouldRenderOffScreen=true` on the BER and a 30-block-tall `getRenderBoundingBox()` on the BE, the beam disappears the moment the core leaves the camera frustum (e.g. standing next to the core and tilting up — beam goes invisible as soon as the core slips off the bottom of the screen). Vanilla beacon stays visible from far away.

  **Investigation so far (not yet verified end-to-end, pick up here):**
  - Vanilla 1.21.1 `LevelRenderer.renderBlockEntities` iterates `globalBlockEntities` every frame regardless of section visibility, and `BlockEntityRenderDispatcher.render` only does a distance check via `shouldRender` (no frustum). So `shouldRenderOffScreen=true` *should* be sufficient on paper.
  - Vanilla `BlockEntity` (decompiled mojmap 1.21.1) has **no** `getRenderBoundingBox` method — it's a Forge/NeoForge addition. The fix recommended on the [1.18 Forge forum thread](https://forums.minecraftforge.net/topic/108050-solved-1181-blockentityrenderer-only-renders-when-the-source-block-is-within-the-players-viewport/) (override on the BE) is what we already do at `TitanCoreBlockEntity.java:150`.
  - Strong lead from a delegated research pass: in NeoForge 1.21 the hook moved off the BE and onto the **renderer** — `BlockEntityRenderer#getRenderBoundingBox(T tile)`. Mekanism's `MultiblockTileEntityRenderer` overrides it on the renderer, not the BE. If true, our BE-side override is dead code and NeoForge's frustum check sees only the default 1×1×1 block AABB, which gets culled the instant the block leaves the frustum. **Not yet confirmed against NeoForge 1.21.1 patched sources** — verify by `javap`-ing `BlockEntityRenderer.class` from the NeoForge-compiled jar or reading the NeoForge patch file before changing anything.
  - No optimization/culling mods are installed in the dev instance (just Mekanism, EnderIO, FTB, EMI, Generator Galore), so it's not Sodium/EntityCulling.

  **Likely fix to try first:** move the override from `TitanCoreBlockEntity.getRenderBoundingBox()` to `TitanCoreRenderer.getRenderBoundingBox(TitanCoreBlockEntity be)`, returning the same 30-tall column. If that still culls at extreme angles, swap to `AABB.INFINITE` (what vanilla beacon effectively is, via mojmap). Delete the dead BE-side override either way.
