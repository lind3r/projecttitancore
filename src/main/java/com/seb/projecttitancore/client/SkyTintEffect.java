package com.seb.projecttitancore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Holy gold sky tint that follows the Titan Core's crafting state.
 *
 * <p>While any nearby loaded Core has CRAFTING=true, the tint ramps up over ~0.5s.
 * When no Core is crafting, it decays to nothing over ~5s. The slow decay is
 * deliberate — it absorbs rapid start/stop flicker (e.g. recipe oscillating because
 * the buffer keeps emptying when input RF/t is borderline) without strobing the sky.
 *
 * <p>Driven by the client-side Titan Core ticker (see {@code TitanCoreBlock.getTicker}):
 * each loaded Core reports its CRAFTING blockstate every client tick. When a BE stops
 * ticking (chunk/dimension unload) its observation goes stale within one tick and the
 * tint decays naturally.
 */
public final class SkyTintEffect {
    private static final double RISE_TICKS = 10.0;
    private static final double DECAY_TICKS = 100.0;

    private static final float GOLD_R = 1.000f;
    private static final float GOLD_G = 0.878f;
    private static final float GOLD_B = 0.329f;

    private static final float MAX_MIX = 0.7f;

    private static final Set<BlockPos> activeCores = new HashSet<>();
    private static long lastObservationTick = Long.MIN_VALUE;

    private static double currentIntensity = 0.0;
    private static double lastUpdateTime = -1.0;

    private SkyTintEffect() {}

    /** Called from the client-side Titan Core ticker each tick, per loaded Core. */
    public static void observe(BlockPos pos, boolean crafting) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long tick = mc.level.getGameTime();
        if (tick != lastObservationTick) {
            activeCores.clear();
            lastObservationTick = tick;
        }
        if (crafting) activeCores.add(pos);
    }

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            currentIntensity = 0.0;
            lastUpdateTime = -1.0;
            return;
        }

        long tick = mc.level.getGameTime();
        // If no Core has ticked recently (BE unloaded, dimension change, paused world)
        // treat as inactive so the tint decays. The 1-tick grace covers the window
        // between the render frame and the upcoming BE tick of the same game tick.
        boolean anyActive = !activeCores.isEmpty() && (tick - lastObservationTick) <= 1;

        double now = tick + event.getPartialTick();
        double dt = lastUpdateTime < 0 ? 0 : Math.max(0, now - lastUpdateTime);
        lastUpdateTime = now;

        double target = anyActive ? 1.0 : 0.0;
        double rate = anyActive ? (1.0 / RISE_TICKS) : (1.0 / DECAY_TICKS);
        double step = dt * rate;
        if (currentIntensity < target) {
            currentIntensity = Math.min(target, currentIntensity + step);
        } else if (currentIntensity > target) {
            currentIntensity = Math.max(target, currentIntensity - step);
        }

        if (currentIntensity <= 0.0) return;

        float intensity = (float) (currentIntensity * MAX_MIX);
        event.setRed(lerp(event.getRed(), GOLD_R, intensity));
        event.setGreen(lerp(event.getGreen(), GOLD_G, intensity));
        event.setBlue(lerp(event.getBlue(), GOLD_B, intensity));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
