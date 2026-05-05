# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Project Titan Core** is a NeoForge Minecraft mod (mod ID: `projecttitancore`) targeting Minecraft 1.21.1 with NeoForge 21.1.228. The mod source lives at the repo root. Java 21 is required.

## Build & Deploy Workflow

After **any** code or resource/config change, always run `deployToInstance` immediately so the user can test right away. Do not wait to be asked.

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

**Titan Core block** — `scripts/gen_block_texture.py`. Generates `titan_core.png` and `titan_core_active.png` (32×32) in `textures/block/`. Edit colours/layout in the script, then run it. Do not hand-edit the PNGs.

## Diagnosing Crashes

Primary log: `C:\Users\lind3\AppData\Roaming\PrismLauncher\instances\projecttitan\minecraft\logs\latest.log`

Search for `FATAL` or `ERROR` — the **first** one is always the root cause. Everything after (`"Cowardly refusing to send event"`) is cascading noise.

## Planned Content

### Titan Core — Remaining Steps

1. ✅ Block + BlockEntity shell, placeholder GUI
2. ✅ Full inventory (9+1 slots), EnergyStorage, FluidTank, capabilities, GUI with bars
3. ✅ Custom RecipeType + Tier 1 recipe JSON + crafting tick logic
4. ✅ Custom trophy item (`TrophyItem`, `trophy_tier_1`), texture via gen script
5. ⬜ Better Questing integration (trophy in inventory → quest completion event)
6. ⬜ Structure building in world (design TBD)

### Deferred Integrations (do not implement yet)

| Integration | Notes |
|---|---|
| JEI recipe display | `@JeiPlugin` in `compat/jei/`; JEI dep already commented out in `build.gradle` |
| Pipe item access | Expose a capability wrapper that restricts extraction from input slots |

All compat code goes in `com.seb.projecttitancore.compat.<modid>`. Core machine logic must never import from `compat/`.
