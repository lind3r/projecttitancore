# Holy Palette

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

## Where it's used

- **Block textures** — `gen_block_texture.py` produces the cage textures (`titan_core_frame.png`, `titan_core_glass.png`) plus the legacy single-face texture (kept for particles + motif previews).
- **Block model** — `models/block/titan_core.json` is a multi-cuboid "glass cage": 12 gold edge frames + 6 inset translucent ivory panels (`render_type: translucent`). The blockstate maps both `crafting=false` and `crafting=true` to this same model — the active/idle visual difference is now driven entirely by the BER (sphere brightness/colour) rather than two model variants. `titan_core_active.json` is intentionally absent.
- **Pulsing inner sphere** — `client/TitanCoreRenderer.java` (`renderSphere`, `SPHERE_TYPE`). Two additive layers (inner core + outer halo) drawn as a UV sphere centred in the cage; colour lerps from halo-bright → gold highlight as `titanTier` rises and snaps to full gold while crafting; pulse rides the same `PULSE_PERIOD_TICKS` clock as the projection. Adjust `SPHERE_BASE_RADIUS`, `SPHERE_GLOW_SCALE`, or the `baseBrightness` ladder at the top of `renderSphere` to retune.
- **GUI** — `screen/TitanCoreScreen.java` (`COLOR_*` constants).

## Texture generators

| Asset | Script | Notes |
|---|---|---|
| Titan Shards (10 tiers) | `gen_shard_texture.py` | Single `SHARD_GRID` silhouette; palette-only progression (dim → fiery → cool → divine; T8-T10 use the holy palette). Edit a `TIERS` entry to retune. |
| Titan Relics | `gen_relic_texture.py` | Same grid+palette pattern, keyed by slug in `RELICS`. Currently `crown_of_the_titan.png` (Titan Trial drop). |
| Titan Core block | `gen_block_texture.py` | Emits `titan_core_frame.png`, `titan_core_glass.png`, and the legacy `titan_core.png`/`titan_core_active.png` motif (still used for particle texture + `scripts/preview_titan_core/`). Motif is pluggable via `MOTIFS`/`MOTIF` (cross, rings, eye, rosette, sunwheel). |
| Building blocks (tileable) | `gen_building_block_texture.py` | Variant-keyed: each `VARIANTS` entry maps a name to a `render(x, y) -> RGBA`. Add a `render_<name>` to extend. |
| Compressed Log | `gen_compressed_log.py` | **Wood palette, not holy** — used as a tier-recipe ingredient for the Core. Pulls vanilla `minecraft:block/oak_log` from the client jar and stamps a 2-px darkened border (the Extra Utilities "compressed" tell). Lives outside `gen_building_block_texture.py` to keep the holy palette pure. Includes a stdlib palette-PNG decoder so no Pillow is needed; if NeoForge bumps Minecraft's version, update `CLIENT_JAR` at the top of the script. |
| Beam | `gen_beam_texture.py` | Pure-white 16×16 at `textures/entity/titan_beam.png`. Bundled because a pack mod overrides vanilla `beacon_beam.png` teal — never reuse the vanilla path. |
| Titan projection structure | `gen_titan_structure.py` | Builds `data/projecttitancore/structure/titan.json` + cumulative previews `tier01.png`…`tier10.png` in `scripts/preview_titan/` (gitignored). Anatomy in part-functions, slabbing in `TIER_Y_RANGES`. Coords: +X = titan's right (sword side), +Y = up, +Z = forward; origin = top of crafting beam. |

## Sound generators

`gen_holy_sounds.py` — pure-stdlib procedural synth (no numpy). Writes two short loopable mono OGGs to `assets/projecttitancore/sounds/`:

- `titan_core_idle.ogg` (~4.0s) — organ-pad hum (root + fifth + octave, detuned voices, slow tremolo). Played whenever a Core's projection is visible.
- `titan_core_crafting.ogg` (~2.0s) — drone + periodic bell chimes with inharmonic partials. Layered on top while `CRAFTING=true`.

Generates WAVs first to `scripts/sounds_wav/` (gitignored), then shells out to `ffmpeg -c:a libvorbis` for the OGG step (Minecraft requires Vorbis; install via `winget install Gyan.FFmpeg`). Both clips use integer-cycle alignment + crossfade tail for clean loops — preserve that invariant when changing duration.

Playback wiring: `Sounds` are registered as `SoundEvent` holders in `ProjectTitanCore.java`; `client/TitanCoreSoundEffect.observe(pos, crafting, projectionShown)` is called from the client-side ticker (`TitanCoreBlock.getTicker`) and manages a `TitanCoreLoop` per Core per channel. Loops self-stop via a staleness timeout when the BE stops being observed (chunk unload, dimension change).
