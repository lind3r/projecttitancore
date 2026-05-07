package com.seb.projecttitancore.client;

import com.seb.projecttitancore.ProjectTitanCore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages two ambient loops per Titan Core, observed once per client tick by
 * {@code TitanCoreBlock.getTicker} (same hook that drives {@link SkyTintEffect}):
 *
 * <ul>
 *   <li><b>Idle hum</b> — plays whenever the projection is visible (titanTier &gt; 0).
 *       Mirrors the idle beam and idle glow gating.</li>
 *   <li><b>Crafting shimmer</b> — plays only while CRAFTING=true. Layers on top of
 *       the idle hum so the world reacts to crafting without pausing the ambient.</li>
 * </ul>
 *
 * <p>Per BE, an instance is started on first observation and stopped when the
 * predicate goes false; the BE itself dropping out of observation (chunk unload)
 * is handled inside {@link TitanCoreLoop} via the staleness timeout.
 */
public final class TitanCoreSoundEffect {
    private static final float IDLE_VOLUME = 0.20f;
    private static final float CRAFT_VOLUME = 0.45f;

    private static final Map<BlockPos, TitanCoreLoop> idleLoops = new HashMap<>();
    private static final Map<BlockPos, TitanCoreLoop> craftLoops = new HashMap<>();

    private TitanCoreSoundEffect() {}

    /** Called from the client-side Titan Core ticker each tick, per loaded Core. */
    public static void observe(BlockPos pos, boolean crafting, boolean projectionShown) {
        if (projectionShown) {
            ensure(idleLoops, pos, ProjectTitanCore.SOUND_TITAN_CORE_IDLE.get(), IDLE_VOLUME);
        } else {
            stop(idleLoops, pos);
        }

        if (crafting) {
            ensure(craftLoops, pos, ProjectTitanCore.SOUND_TITAN_CORE_CRAFTING.get(), CRAFT_VOLUME);
        } else {
            stop(craftLoops, pos);
        }

        gcStopped();
    }

    private static void ensure(Map<BlockPos, TitanCoreLoop> map, BlockPos pos, SoundEvent event, float volume) {
        TitanCoreLoop loop = map.get(pos);
        if (loop != null && !loop.isStopped()) {
            loop.observe();
            return;
        }
        TitanCoreLoop fresh = new TitanCoreLoop(event, pos, volume);
        map.put(pos, fresh);
        Minecraft.getInstance().getSoundManager().play(fresh);
    }

    private static void stop(Map<BlockPos, TitanCoreLoop> map, BlockPos pos) {
        TitanCoreLoop loop = map.remove(pos);
        if (loop != null) loop.requestStop();
    }

    /** Drop entries the SoundManager has already cleaned up so the maps don't accumulate ghosts. */
    private static void gcStopped() {
        gc(idleLoops);
        gc(craftLoops);
    }

    private static void gc(Map<BlockPos, TitanCoreLoop> map) {
        Iterator<Map.Entry<BlockPos, TitanCoreLoop>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isStopped()) it.remove();
        }
    }
}
