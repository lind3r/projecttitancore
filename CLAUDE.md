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

**Modpack goal:** the player progresses through 10 tiers via **FTB Quests**, with the final tier (Heart of the Titan) as the endgame goal. The endgame tier itself is not yet implemented. This mod's planned shard → quest hook (see "Planned Content" below) is the bridge between this mod and that progression.

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

**Titan Shards (10 tiers)** — `scripts/gen_shard_texture.py`. All 10 shards share a single `SHARD_GRID` silhouette; the visual progression is palette-only (dim → fiery → cool → divine, with tiers 8-10 pulling from the holy palette). To retune a tier, edit its `TIERS` entry; to add a tier, add the palette and reference the slug. Writes to `textures/item/`.

**Titan Core block** — `scripts/gen_block_texture.py`. Generates `titan_core.png` and `titan_core_active.png` (32×32) — the same texture is mapped to every face via `cube_all`. The centre motif is pluggable: `MOTIFS` registers `cross`, `rings`, `eye`, `rosette`, `sunwheel`. Change `MOTIF` near the top of the script and re-run to swap the live design. Every run also writes inactive previews of *all* motifs to `scripts/preview_titan_core/` (gitignored) so you can compare without rebuilding. To add a new motif, write a `motif_<name>(x, y, active)` function returning the gold-coloured pixel for any pixel inside the design (or `None` outside) and add it to `MOTIFS`. Do not hand-edit the PNGs.

**Building blocks (tileable)** — `scripts/gen_building_block_texture.py`. Variant-keyed: each entry in `VARIANTS` maps a name to a `render(x, y) -> RGBA` function and produces a 32×32 tileable PNG in `textures/block/`. To add a chiseled/etched sibling, write a new `render_<name>` function (you can reuse the brick layout in `render_holy_bricks` as a base) and add it to `VARIANTS`.

**Titan projection structure** — `scripts/gen_titan_structure.py`. Builds the holographic-statue voxel data emitted as `data/projecttitancore/structure/titan.json` plus cumulative front-elevation previews in `scripts/preview_titan/` (gitignored). Body is split into part-functions (`t1_plinth`, `t1_feet`, `t1_lower_legs`, `t2_upper_legs`, `t2_hips_belt`, `t3_waist`, `t3_chest`, `t3_arms`, `t4_neck_head`, `t4_sword`) that each return `(x, y, z, color)` voxel tuples; `TIER_BUILDERS` aggregates them per tier. Coordinate frame: +X = titan's right (sword side), +Y = up, +Z = forward; origin = top of crafting beam (`y = corePos.y + BEAM_RENDER_HEIGHT`). Three colors only — `ivory`, `shadow`, `gold`, matching the holy palette. To tweak proportions: edit a part function, re-run, inspect the previews, then `deployToInstance`.

## Diagnosing Crashes

Primary log: `C:\Users\lind3\AppData\Roaming\PrismLauncher\instances\projecttitan\minecraft\logs\latest.log`

Search for `FATAL` or `ERROR` — the **first** one is always the root cause. Everything after (`"Cowardly refusing to send event"`) is cascading noise.

## Planned Content

### Titan Core — Remaining Steps

1. ✅ Block + BlockEntity shell, placeholder GUI
2. ✅ Full inventory (9+1 slots), EnergyStorage, FluidTank, capabilities, GUI with bars
3. ✅ Custom RecipeType + crafting tick logic
4. ✅ Titan Shard items (`TitanShardItem` × 10 tiers), textures via `gen_shard_texture.py`
5. ✅ Tiered chain recipes — `titan_core_t1` … `titan_core_t10` JSONs; tier N≥2 consumes the previous tier's shard (placed in slot 4, the GUI's centre slot) plus 8 bulk ingredients in the surrounding slots
6. ⬜ FTB Quests integration — detect each shard in inventory and complete the matching quest (one per tier, ten total)
7. 🚧 **Titan projection — needs splitting into 10 pieces.** Currently the projection's anatomy is divided into 4 tiers (T1 = feet/lower legs, T2 = upper legs/hips/belt, T3 = torso/arms, T4 = head/sword). Now that the shard chain has 10 tiers, the projection has to grow in 10 increments instead of 4 — each tier adds a smaller, visible chunk so every craft is rewarded with a visible body change. Required: rebuild `gen_titan_structure.py` so `TIER_BUILDERS` has keys 1..10 with smaller part-functions (e.g. split lower legs into shins/calves, torso into ribcage/pectoral/shoulders, etc.). Renderer doesn't need code changes — it already reads cumulative tiers from the JSON. Translucent rotating holographic statue grows tier-by-tier above the crafting beam; `TitanCoreRenderer.renderProjection` reads `TitanCoreBlockEntity.titanTier` each frame and submits cumulative voxels via a custom `POSITION_COLOR` translucent `RenderType`. Slow Y rotation (~24°/sec) and slow alpha breathe (~4s cycle) baked into the renderer. **Not yet wired:** the BE-side projection is render-only — no structure ever materializes as real blocks.

### Titan Shards — 10-Tier Progression

The Core's progression is built around 10 named **Titan Shards**, each unlocked by feeding the previous tier's shard back into the Core along with bulk materials. The names tell the arc — early ignition, awareness, divinity:

| Tier | Item ID | Display Name | Bulk Ingredient |
|---|---|---|---|
| 1 | `mote_of_the_titan` | Mote of the Titan | `holy_bricks` |
| 2 | `ember_of_the_titan` | Ember of the Titan | `iron_block` |
| 3 | `spark_of_the_titan` | Spark of the Titan | `iron_block` |
| 4 | `pulse_of_the_titan` | Pulse of the Titan | `copper_block` |
| 5 | `echo_of_the_titan` | Echo of the Titan | `gold_block` |
| 6 | `will_of_the_titan` | Will of the Titan | `diamond_block` |
| 7 | `voice_of_the_titan` | Voice of the Titan | `diamond_block` |
| 8 | `soul_of_the_titan` | Soul of the Titan | `emerald_block` |
| 9 | `ascendant_shard` | Ascendant Shard | `netherite_block` |
| 10 | `heart_of_the_titan` | Heart of the Titan | `netherite_block` |

All 10 shards are instances of `TitanShardItem` (a thin `Item` subclass — no per-tier behavior on the class side; differences live in the recipe and texture). Recipe JSONs are `data/projecttitancore/recipe/titan_core_t{1..10}.json`. The fluid scales `water` → `lava` → `c:molten_steel`; energy/time both scale ×~2 per tier. The shard input for T2-T10 sits at index 4 of the `inputs` array — that maps to the centre slot of the 3×3 GUI grid. `titan_core_test.json` is preserved as a low-cost log-input variant of T1 for in-dev recipe verification.

**Current ingredient costs are placeholder** — every input slot in T1-T10 is `count: 64`, including the prior-shard slot. This is intentional for the initial wiring; balance pass comes later (likely shard count → 1, bulk counts varied per tier).

When adding an 11th tier or renaming, update **all five** in lockstep: `ProjectTitanCore.java` (registry + creative tab), `lang/en_us.json`, `models/item/{slug}.json`, `gen_shard_texture.py` (palette entry), and the matching `recipe/titan_core_t{N}.json` (with the prior shard at index 4). Capstone idea on the table for later: tier 10 could optionally consume *all* 8 prior shards (mote through soul) instead of just the ascendant — gives players who hoarded a reason they did. Not implemented; mention if/when revisiting.

### Holy Bricks — Chisel Mod Compat

The 4 holy_bricks variants convert into each other at any **Chipped** workbench (Mason's Table is the thematically natural choice; mechanically all chipped workbenches resolve any `chipped:workbench` recipe globally — there's no per-workbench filter in `WorkbenchMenu.updateResults`).

Wiring (all in `src/main/resources/data/projecttitancore/`):
- `tags/block/holy_bricks.json` + `tags/item/holy_bricks.json` — list all 4 variants. Chipped ships parallel block+item tags; do the same when adding new carving groups.
- `recipe/holy_bricks_chipped.json` — `chipped:workbench` recipe, single-ingredient = the tag, gated by `neoforge:mod_loaded` on `chipped`.

Vanilla stonecutter recipes were removed in this migration. If we later add another carving group (e.g. holy_marble), repeat the same pattern: add the two parallel tags + one recipe JSON. Don't override `data/chipped/recipe/mason_table.json` — it's brittle on Chipped updates.

### Known Issues

- **Frustum culling of beam + projection** — fixed 2026-05-06. The bounding-box override needs to live on the renderer (`IBlockEntityRendererExtension#getRenderBoundingBox`), not on the `BlockEntity`; in NeoForge 1.21.1 the BE has no such method, and `LevelRenderer` consults the renderer via `ClientHooks.isBlockEntityRendererVisible`. Override now sits on `TitanCoreRenderer` returning the `±10 × 85 × ±10` AABB. If extreme camera angles ever cull again, escalate to `AABB.INFINITE` (vanilla beacon's effective behavior via `BeaconRenderer.shouldRenderOffScreen`).
