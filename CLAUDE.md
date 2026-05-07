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
- Block textures — `scripts/gen_block_texture.py` produces the cage textures (`titan_core_frame.png`, `titan_core_glass.png`) plus the legacy single-face texture (kept for particles + motif previews).
- Block model — `models/block/titan_core.json` is a multi-cuboid "glass cage": 12 gold edge frames + 6 inset translucent ivory panels (`render_type: translucent`). The blockstate maps both `crafting=false` and `crafting=true` to this same model — the active/idle visual difference is now driven entirely by the BER (sphere brightness/colour) rather than two model variants. `titan_core_active.json` is intentionally absent.
- Pulsing inner sphere — `client/TitanCoreRenderer.java` (`renderSphere`, `SPHERE_TYPE`). Two additive layers (inner core + outer halo) drawn as a UV sphere centred in the cage; colour lerps from halo-bright → gold highlight as `titanTier` rises and snaps to full gold while crafting; pulse rides the same `PULSE_PERIOD_TICKS` clock as the projection. Adjust `SPHERE_BASE_RADIUS`, `SPHERE_GLOW_SCALE`, or the `baseBrightness` ladder at the top of `renderSphere` to retune.
- GUI — `screen/TitanCoreScreen.java` (`COLOR_*` constants).

## Texture Generation

**Titan Shards (10 tiers)** — `scripts/gen_shard_texture.py`. All 10 shards share a single `SHARD_GRID` silhouette; the visual progression is palette-only (dim → fiery → cool → divine, with tiers 8-10 pulling from the holy palette). To retune a tier, edit its `TIERS` entry; to add a tier, add the palette and reference the slug. Writes to `textures/item/`.

**Titan Core block** — `scripts/gen_block_texture.py`. Generates four textures:
- `titan_core_frame.png` (16×16) — solid gold metal, used on the 12 edge cuboids of the cage model.
- `titan_core_glass.png` (16×16) — translucent ivory (~15% alpha base, ~30% on a faint reflection streak, gold inner border), used on the 6 inset panes.
- `titan_core.png` / `titan_core_active.png` (32×32) — the legacy single-face motif texture, no longer wired into the live block model but still emitted because the block's particle texture and `scripts/preview_titan_core/` (gitignored) reference it. The centre motif is pluggable: `MOTIFS` registers `cross`, `rings`, `eye`, `rosette`, `sunwheel`; change `MOTIF` near the top of the script and re-run to swap the live design. To add a new motif, write a `motif_<name>(x, y, active)` function returning the gold-coloured pixel for any pixel inside the design (or `None` outside) and add it to `MOTIFS`. Do not hand-edit the PNGs.

**Building blocks (tileable)** — `scripts/gen_building_block_texture.py`. Variant-keyed: each entry in `VARIANTS` maps a name to a `render(x, y) -> RGBA` function and produces a 32×32 tileable PNG in `textures/block/`. To add a chiseled/etched sibling, write a new `render_<name>` function (you can reuse the brick layout in `render_holy_bricks` as a base) and add it to `VARIANTS`.

**Beam texture** — `scripts/gen_beam_texture.py`. Generates `textures/entity/titan_beam.png` — a 16×16 pure-white texture (faint per-column alpha variation) that `TitanCoreRenderer` feeds into `BeaconRenderer.renderBeaconBeam`. Bundled because some mod in the Project Titan modpack ships its own `assets/minecraft/textures/entity/beacon_beam.png` that overrides the vanilla beam with a teal-tinted variant, which biased every tint we sent (white tint read teal; saturated colours read closer to true). Any new beam-style effect in this mod should reuse this texture, not the vanilla path.

**Titan projection structure** — `scripts/gen_titan_structure.py`. Builds the holographic-statue voxel data emitted as `data/projecttitancore/structure/titan.json` plus cumulative front-elevation previews `tier01.png` … `tier10.png` in `scripts/preview_titan/` (gitignored). Geometry and tiering are decoupled: anatomy is described once by part-functions (`plinth`, `feet`, `lower_legs`, `upper_legs`, `hips_belt`, `waist`, `chest`, `arms`, `neck_head`, `sword`) that return `(x, y, z, color)` voxel tuples; `TIER_Y_RANGES` then slabs the full body into 10 cumulative horizontal slices (T1 = plinth/feet up through T10 = head/crown). Coordinate frame: +X = titan's right (sword side), +Y = up, +Z = forward; origin = top of crafting beam (`y = corePos.y + BEAM_RENDER_HEIGHT`). Three colors only — `ivory`, `shadow`, `gold`, matching the holy palette. To re-pace tiers, edit `TIER_Y_RANGES`. To tweak proportions, edit a part function. Re-run, inspect the previews (all share a fixed canvas so they line up frame-to-frame), then `deployToInstance`.

## Sound Generation

**Titan Core ambient loops** — `scripts/gen_holy_sounds.py`. Pure-stdlib procedural synth (no numpy) that writes two short loopable mono OGGs to `src/main/resources/assets/projecttitancore/sounds/`:

- `titan_core_idle.ogg` — subtle organ-pad hum (root + fifth + octave, detuned voices, slow tremolo). ~4.0s, played whenever a Core's projection is visible (`titanTier > 0`).
- `titan_core_crafting.ogg` — drone + periodic bell chimes with inharmonic partials. ~2.0s, layered on top of the idle hum while `CRAFTING=true`.

The script generates WAVs first (intermediate output to `scripts/sounds_wav/`, gitignored) and shells out to `ffmpeg -c:a libvorbis` for the OGG step. Minecraft requires OGG Vorbis, so ffmpeg is required — install on Windows with `winget install Gyan.FFmpeg`. To retune a clip: edit the `voices` / `bell_partials` tables or duration in `synth_idle` / `synth_crafting` and re-run. Both clips are designed for clean loops (integer-cycle alignment + crossfade tail), so don't break that invariant when changing duration.

Playback wiring: `Sounds` are registered as `SoundEvent` holders in `ProjectTitanCore.java`; `client/TitanCoreSoundEffect.observe(pos, crafting, projectionShown)` is called from the client-side ticker (`TitanCoreBlock.getTicker`) and manages a `TitanCoreLoop` (`AbstractTickableSoundInstance`) per Core per channel. Sounds stop themselves via a staleness timeout when the BE stops being observed (chunk unload, dimension change).

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
7. ✅ Titan projection — 10 cumulative slabs. `gen_titan_structure.py` decouples geometry from tiering: body parts (`plinth`, `feet`, `lower_legs`, `upper_legs`, `hips_belt`, `waist`, `chest`, `arms`, `neck_head`, `sword`) describe the full statue once, then `TIER_Y_RANGES` slabs the voxels into 10 horizontal Y-bands (mostly 4 tall, T10 is 5). Re-pace by editing `TIER_Y_RANGES` only — anatomy stays put. Translucent rotating holographic statue grows tier-by-tier above the crafting beam; `TitanCoreRenderer.renderProjection` reads `TitanCoreBlockEntity.titanTier` each frame and submits cumulative voxels via a custom `POSITION_COLOR` translucent `RenderType`. The renderer reads tier numbers dynamically from the JSON, so going from 4→10 tiers required no Java changes. Slow Y rotation (~24°/sec) and slow alpha breathe (~4s cycle) baked into the renderer. **Not yet wired:** the BE-side projection is render-only — no structure ever materializes as real blocks.

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

All 10 shards are instances of `TitanShardItem` (a thin `Item` subclass — no per-tier behavior on the class side; differences live in the recipe and texture). Recipe JSONs are `data/projecttitancore/recipe/titan_core_t{1..10}.json`. Fluids progress by rarity tier: `water` (T1-T2) → `lava` (T3) → `c:molten_copper` (T4) → `c:molten_gold` (T5) → `c:honey` (T6) → `c:molten_steel` (T7) → `c:experience` (T8) → `enderio:vapor_of_levity` (T9) → `enderio:liquid_sunshine` (T10). Amounts scale 4k → 100k and **must stay ≤ `FLUID_CAPACITY` (100,000 mB)** — the tank is static, no dynamic resize, so a recipe that requests more than 100k can never be filled. Energy/time both scale ×~2 per tier. The shard input for T2-T10 sits at index 4 of the `inputs` array — that maps to the centre slot of the 3×3 GUI grid. `titan_core_test.json` is preserved as a low-cost log-input variant of T1 for in-dev recipe verification.

**Current ingredient costs are placeholder** — every input slot in T1-T10 is `count: 64`, including the prior-shard slot. This is intentional for the initial wiring; balance pass comes later (likely shard count → 1, bulk counts varied per tier).

When adding an 11th tier or renaming, update **all five** in lockstep: `ProjectTitanCore.java` (registry + creative tab), `lang/en_us.json`, `models/item/{slug}.json`, `gen_shard_texture.py` (palette entry), and the matching `recipe/titan_core_t{N}.json` (with the prior shard at index 4). Capstone idea on the table for later: tier 10 could optionally consume *all* 8 prior shards (mote through soul) instead of just the ascendant — gives players who hoarded a reason they did. Not implemented; mention if/when revisiting.

### Holy Bricks — Chisel Mod Compat

The holy_bricks variants (currently 6: `holy_bricks`, `chiseled_holy_bricks`, `holy_brick_pillar`, `holy_brick_tiles`, `gilded_holy_bricks`, `engraved_holy_bricks`) convert into each other in-hand using a **Chisel Modern** chisel (no workbench/recipe — the chisel item cycles between blocks in the same carving group, and right-clicking a placed block changes its variant).

Chisel Modern discovers carving groups by scanning **block tags whose namespace is literally `chisel` and whose path starts with `carving/`** (see `CarvingHelper.getCarvingGroup` — the filter is `tag.namespace == "chisel" && tag.path.startsWith("carving/")`). So the tag files live under our mod's resources but in the `chisel` namespace:

- `data/chisel/tags/block/carving/holy_bricks.json` — every variant block ID.
- `data/chisel/tags/item/carving/holy_bricks.json` — every variant item ID (used when chiseling an item in inventory rather than a placed block).

When adding another holy_bricks variant, update in lockstep: `ProjectTitanCore.java` (block + blockitem registration + creative tab), `lang/en_us.json`, blockstate + block model + item model JSONs, the loot table, both `data/chisel/.../carving/holy_bricks.json` files, and `gen_building_block_texture.py` (add a `render_<name>` and a `VARIANTS` entry, then re-run).

If we later add another carving group (e.g. holy_marble), repeat the same pattern: one parallel block+item tag pair under `data/chisel/tags/.../carving/<group>.json`. Do **not** put the tags under `data/projecttitancore/tags/...` — chisel will not see them, only the `chisel` namespace is scanned.

### World Response on Craft / Tier-Up

A craft completion at the Titan Core should feel like the world reacts. Effects are split into **per-craft** (every successful craft, any tier) and **per-tier-up** (escalating world changes that compound).

**Implemented — holy sky tint while crafting.** While any nearby loaded Titan Core has `TitanCoreBlock.CRAFTING=true`, the sky/fog biases toward holy gold (`#FFE054`); when no Core is crafting it decays to nothing over ~5s. The slow decay deliberately absorbs rapid start/stop flicker (e.g. recipe oscillating because input RF/t is borderline) without strobing the sky. No network packet — the BE's blockstate is already client-synced, so a client-side ticker registered in `TitanCoreBlock.getTicker` reports each loaded Core's CRAFTING state every tick into `SkyTintEffect.observe(pos, crafting)`. `SkyTintEffect.onComputeFogColor` (subscribed in `ClientEvents`) biases `ViewportEvent.ComputeFogColor` against a smoothed `currentIntensity` that ramps over `RISE_TICKS` (10 = ~0.5s rise) and decays over `DECAY_TICKS` (100 = ~5s). When the BE unloads it stops ticking, the observation goes stale within one tick, and the tint decays naturally. Tweak constants at the top of `SkyTintEffect` to retune.

**Implemented — block light emission tracks state.** The Titan Core block emits light 15 while `CRAFTING=true`, 8 while idle but with the projection visible (`titanTier > 0`), and 0 when freshly placed and never crafted. The tier-aware idle glow needs position context (BE is the source of truth for `titanTier`), so `TitanCoreBlock` overrides NeoForge's `IBlockExtension#getLightEmission(state, level, pos)` and returns `true` from `hasDynamicLightEmission` for non-CRAFTING states. When a craft completes, the CRAFTING blockstate transitions true→false in the same tick the BE bumps `titanTier` from 0→1, and the lighting engine's recompute on that blockstate change picks up the new emission of 8.

**Implemented — ambient sound loops.** Two looping clips, gated to mirror the visual layers exactly: `titan_core_idle.ogg` plays whenever the projection is visible; `titan_core_crafting.ogg` layers on top while crafting. Both are observed once per client tick via the same `TitanCoreBlock.getTicker` hook that drives the sky tint, then handed to `TitanCoreSoundEffect.observe(pos, crafting, projectionShown)`. The manager keeps one `TitanCoreLoop` per Core per channel; loops self-stop after a short staleness window when their BE stops ticking. To retune: edit volumes at the top of `TitanCoreSoundEffect`, or regenerate the OGGs via `scripts/gen_holy_sounds.py` (see "Sound Generation" above).

**Planned — per-tier-up world response.** The brainstorm (2026-05-07) proposes the world growing progressively hostile / sublime as tiers advance, with each tier *stacking* on previous. Doable inside this mod plus already-installed pack mods — no new deps required. Sketch:

| Tier | Theme | Mechanism |
|---|---|---|
| 1 Mote | World stirs | Permanent holy beam (already exists); ambient choir hum within ~32 blocks |
| 2 Ember | Mobs harden | Hostile mob HP +10% globally — Apothic Attributes modifier or `AttributeModifier` registered from this mod |
| 3 Spark | Nights lengthen | Night-tick rate scaled |
| 4 Pulse | Affixed mobs roam | Apotheosis affix-rarity bumped one tier |
| 5 Echo | Storms answer | Lightning frequency + thunderstorm chance up; Apothic Spawners in dungeons buffed |
| 6 Will | Hunt begins | Rare nightly "Wraith of the Titan" spawn (Citadel-based or repurposed Vex) that pathfinds to player |
| 7 Voice | Gateways tear open | Random Gateways to Eternity opens within ~200 blocks every few in-game days (KubeJS scheduler or BE tick) |
| 8 Soul | Blood moons | Every 5 nights — sky red, mob HP +50%, damage +25%, light level halved |
| 9 Ascendant | Heralds appear | Cataclysm miniboss spawns once at a marked location after tier-up |
| 10 Heart | Apotheosis | Eternal stormy twilight + roughly 2× mob stats — but player gets permanent divine buff (Resistance/Regen + retribution) |

Implementation hook: add `onTierAdvanced(int newTier)` on `TitanCoreBlockEntity` that fires once per upgrade. Persist applied tier-effects in level data so they survive reload. Use FTB Quests reward commands for the *narrative* side; use the BE/level data for the *mechanical* side. Most "scheduler" tier effects (gateway spawns, blood moons) should live in custom code rather than KubeJS so state is coherent with the BE — KubeJS is fine for iteration but not for load-bearing state.

**Mods leveraged:** Apotheosis (affixes), Apothic Attributes (global modifiers), Apothic Spawners, Gateways to Eternity, L_Ender's Cataclysm (endgame bosses), Citadel + Alex's Mobs (mob roster), KubeJS (pack-side iteration). Full modlist is at `clones/project-titan/mods/*.pw.toml`.

### Known Issues

- **Frustum culling of beam + projection** — fixed 2026-05-06. The bounding-box override needs to live on the renderer (`IBlockEntityRendererExtension#getRenderBoundingBox`), not on the `BlockEntity`; in NeoForge 1.21.1 the BE has no such method, and `LevelRenderer` consults the renderer via `ClientHooks.isBlockEntityRendererVisible`. Override now sits on `TitanCoreRenderer` returning the `±10 × 85 × ±10` AABB. If extreme camera angles ever cull again, escalate to `AABB.INFINITE` (vanilla beacon's effective behavior via `BeaconRenderer.shouldRenderOffScreen`).
