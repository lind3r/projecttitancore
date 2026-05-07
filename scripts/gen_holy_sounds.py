"""
Generate ambient sound assets for Project Titan Core.

Run from the repo root: python scripts/gen_holy_sounds.py

Synthesises two short loopable clips with pure Python stdlib (math, wave) and
shells out to ffmpeg to convert WAV -> Vorbis OGG (Minecraft requires OGG).

  - titan_core_idle.ogg      Subtle organ-like hum, plays whenever the Core's
                             projection is visible. ~4.0s, perfectly loopable.
  - titan_core_crafting.ogg  Same hum voicing transposed up a perfect fifth so
                             it layers cleanly with the idle hum while crafting.
                             ~4.0s, perfectly loopable.

Both clips are mono, 44.1 kHz, 16-bit. They're designed for clean integer-cycle
looping: every voice's frequency is snapped to an integer number of cycles per
loop duration, and the LFO is one full cycle per loop — so the last sample lines
up with the first and looping is sample-perfect.

If ffmpeg isn't on PATH, WAVs are still written to scripts/sounds_wav/ and the
script prints an install hint (Windows: `winget install Gyan.FFmpeg`).

To retune: edit `HUM_VOICES` to change the chord, or the per-clip `synth_idle` /
`synth_crafting` wrapper for pitch / duration / depth, then re-run. OGGs land
in src/main/resources/assets/projecttitancore/sounds/.
"""

from __future__ import annotations

import math
import os
import shutil
import struct
import subprocess
import sys
import wave

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOUNDS_OUT = os.path.join(
    REPO_ROOT, "src", "main", "resources", "assets", "projecttitancore", "sounds"
)
WAV_TMP = os.path.join(REPO_ROOT, "scripts", "sounds_wav")

SAMPLE_RATE = 44100


# ---------- helpers ----------

def write_wav(path: str, samples: list[float]) -> None:
    """Write a mono float-in-[-1,1] sample list as 16-bit PCM WAV."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        # Clip and quantise.
        frames = bytearray()
        for s in samples:
            v = max(-1.0, min(1.0, s))
            frames += struct.pack("<h", int(v * 32767))
        w.writeframes(bytes(frames))


def normalize(samples: list[float], peak: float) -> list[float]:
    """Scale samples so |max| == peak."""
    m = max(abs(x) for x in samples) or 1.0
    g = peak / m
    return [x * g for x in samples]


def crossfade_loop(samples: list[float], fade_ms: float = 25.0) -> list[float]:
    """Crossfade the tail with the head so loop seam is clickless even when
    integer-cycle alignment is imperfect (e.g. due to ADSR nonlinearity)."""
    n = len(samples)
    fade = int(SAMPLE_RATE * fade_ms / 1000.0)
    if fade <= 0 or fade * 2 >= n:
        return samples
    out = list(samples)
    for i in range(fade):
        # Equal-power crossfade.
        a = math.cos(0.5 * math.pi * i / fade)
        b = math.sin(0.5 * math.pi * i / fade)
        # Blend end[fade-i] backwards into start[i] proportionally.
        head = samples[i]
        tail = samples[n - fade + i]
        out[i] = head * b + tail * a
        out[n - fade + i] = tail * b + head * a
    return out


# ---------- choir-pad hum ----------
#
# Both clips are choir-pad drones built from layered detuned sines. Voicing is
# a hollow fifth (root + fifth + octave) — open and airy rather than bright or
# major, which matches the "holy but reserved" feel. The crafting clip is the
# same voice set transposed up a perfect fifth, so the two layer cleanly when
# the player is in earshot of an actively crafting Core.

# Voices: (harmonic_multiplier, detune_cents, gain).
# Detune adds chorus thickness; cents -> ratio is 2^(cents/1200).
HUM_VOICES = [
    (1.0, -2.0, 1.00),  # root
    (1.0, +2.0, 0.85),  # root, slightly detuned (chorus)
    (1.5, -1.0, 0.65),  # perfect fifth
    (1.5, +1.0, 0.55),  # perfect fifth, detuned
    (2.0, -0.5, 0.45),  # octave
    (2.0, +0.5, 0.40),  # octave, detuned
    (3.0,  0.0, 0.18),  # subtle 3rd harmonic for "presence"
]


def synth_hum(duration_s: float, root_hz: float, lfo_depth: float = 0.22,
              peak: float = 0.45) -> list[float]:
    """Generate a loopable choir-pad drone at the given root pitch.

    Frequencies are snapped to the nearest integer-cycle-per-loop value so the
    last sample's phase matches the first — the loop seam is sample-perfect."""
    n = int(SAMPLE_RATE * duration_s)
    out = [0.0] * n

    for mult, cents, gain in HUM_VOICES:
        f = root_hz * mult * (2.0 ** (cents / 1200.0))
        cycles = round(f * duration_s)
        f = cycles / duration_s
        two_pi_f = 2.0 * math.pi * f
        for i in range(n):
            t = i / SAMPLE_RATE
            out[i] += gain * math.sin(two_pi_f * t)

    # Slow tremolo LFO — one full cycle per loop, integer-aligned, no seam click.
    lfo_hz = 1.0 / duration_s
    two_pi_lfo = 2.0 * math.pi * lfo_hz
    for i in range(n):
        t = i / SAMPLE_RATE
        out[i] *= 1.0 - lfo_depth * (0.5 - 0.5 * math.cos(two_pi_lfo * t))

    return normalize(out, peak=peak)


# Idle = base hum at A2.
def synth_idle() -> list[float]:
    return synth_hum(duration_s=4.0, root_hz=110.0, lfo_depth=0.22, peak=0.45)


# Crafting = same voicing transposed up a perfect fifth (E3) for perceived
# "lift" while keeping the timbre familiar. Volume gain happens on the playback
# side (CRAFT_VOLUME in TitanCoreSoundEffect.java); here we only normalise to
# the same peak, then layering above the idle hum gives the louder feel.
def synth_crafting() -> list[float]:
    return synth_hum(duration_s=4.0, root_hz=165.0, lfo_depth=0.22, peak=0.45)


# ---------- pipeline ----------

def convert_to_ogg(wav_path: str, ogg_path: str, ffmpeg: str) -> None:
    os.makedirs(os.path.dirname(ogg_path), exist_ok=True)
    # -q:a 4 ~ 128 kbps Vorbis, fine for ambient game sound.
    subprocess.run(
        [ffmpeg, "-y", "-loglevel", "error", "-i", wav_path,
         "-c:a", "libvorbis", "-q:a", "4", ogg_path],
        check=True,
    )


def main() -> int:
    print("Synthesising idle hum...")
    idle = synth_idle()
    idle_wav = os.path.join(WAV_TMP, "titan_core_idle.wav")
    write_wav(idle_wav, idle)

    print("Synthesising crafting hum...")
    craft = synth_crafting()
    craft_wav = os.path.join(WAV_TMP, "titan_core_crafting.wav")
    write_wav(craft_wav, craft)

    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        print()
        print("ffmpeg not found on PATH. WAVs were written to:")
        print(f"  {idle_wav}")
        print(f"  {craft_wav}")
        print("Minecraft requires OGG Vorbis. Install ffmpeg and re-run, e.g.:")
        print("  winget install Gyan.FFmpeg")
        print("Or convert manually:")
        print(f"  ffmpeg -i in.wav -c:a libvorbis -q:a 4 out.ogg")
        return 1

    print("Converting WAV -> OGG via ffmpeg...")
    convert_to_ogg(idle_wav,  os.path.join(SOUNDS_OUT, "titan_core_idle.ogg"),     ffmpeg)
    convert_to_ogg(craft_wav, os.path.join(SOUNDS_OUT, "titan_core_crafting.ogg"), ffmpeg)
    print(f"Wrote OGGs to {SOUNDS_OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
