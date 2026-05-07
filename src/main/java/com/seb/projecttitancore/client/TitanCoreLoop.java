package com.seb.projecttitancore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Looping positional sound bound to a Titan Core. Stays alive until either
 * {@link #requestStop()} is called or the BE stops bumping {@link #observe()}
 * for {@link #STALE_TICKS} ticks (chunk unload, dimension change).
 */
final class TitanCoreLoop extends AbstractTickableSoundInstance {
    /** Tolerance window before assuming the source BE is gone. Generous enough to ride out a paused world tick. */
    private static final int STALE_TICKS = 4;

    private long lastObservedTick;

    TitanCoreLoop(SoundEvent event, BlockPos pos, float volume) {
        super(event, SoundSource.BLOCKS, RandomSource.create());
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.volume = volume;
        this.pitch = 1.0f;
        this.looping = true;
        this.delay = 0;
        observe();
    }

    /** Bumped by the manager every client tick the BE is alive and the sound's predicate still holds. */
    void observe() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) lastObservedTick = mc.level.getGameTime();
    }

    void requestStop() {
        stop();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() - lastObservedTick > STALE_TICKS) {
            stop();
        }
    }
}
