# Core Mod TODOs

## 1. Rescale Titan Core recipe FE/t costs (target: T10 = 10 GFE/t)

The current tier curve tops out at **200,000,000 FE/t** for T10 — well below what a properly tuned Mekanism Fission Reactor outputs (1–3 GFE/t for a single optimized reactor + boiler + turbine). This makes the apex tier trivially coverable by mid-game power and renders Mekanism Fusion / Draconic Reactor decorative rather than required.

**Target:** rescale so **T10 demands ~10,000,000,000 FE/t (10 GFE/t)** — on par with a high-end Fusion Reactor or mid-range Draconic Reactor. T1 stays small (current 1,000 FE/t is fine as the entry point).

**Current per-tier numbers** (from `src/main/resources/data/projecttitancore/recipe/titan_core_t{1..10}.json`, `energy_per_tick` field):

| Tier | Current FE/t |
|---|---:|
| T1  | 1,000 |
| T10 | 200,000,000 |

Tiers in between scale geometrically. Going from T1=1,000 to T10=10,000,000,000 across 9 steps ≈ **6× per tier**. Pick whatever curve feels right — could keep the same shape and just multiply T1–T10 by a constant, or steepen the late tiers (T7+) so reactors become *necessary* at the top end while early tiers stay accessible.

**Don't forget:** also update `clones/project-titan/balance/energy.md` (the demand-target reference doc) so quest/mod numbers stay in lockstep.

**Same idea applies to Draconic Reactor pacing** — at 10 GFE/t for T10, a single Draconic Reactor still trivializes it. If we want the late tiers to actually require either Fusion *or* Draconic Reactor, T9–T10 may need to push higher (~50–100 GFE/t) or rely on a sustained-spike pattern that buffers can't trivially absorb.

## 2. Add thousands separators to JEI recipe energy display

JEI currently renders the Titan Core recipe energy as e.g. `~200000000 RF/t` — at the scale we're working at (millions to billions), unbroken digit strings are unreadable. Add `.` (or `,` — pick one) thousands separators so it reads `~200.000.000 RF/t`.

**File:** `src/main/java/com/seb/projecttitancore/compat/jei/TitanCoreCraftingCategory.java:95`

Current:
```java
guiGraphics.drawString(font,
        "~" + recipe.energyPerTick() + " RF/t",
        1, 78, 0xFF6600, false);
```

Fix: format `recipe.energyPerTick()` with a locale or explicit pattern (`String.format("%,d", n)` with `Locale.GERMAN` for `.`, or `Locale.ENGLISH` for `,`). Same treatment probably wants to apply to the fluid `mB` line two calls above (line 91) and any other numeric overlay we add later.
