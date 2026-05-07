package com.seb.projecttitancore.events;

import com.seb.projecttitancore.ProjectTitanCore;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

// Mob with NBT tag projecttitancore_cronos is treated as Cronos. On death by a player,
// grant the cronos_slain advancement so FTB Quests / datapacks can hook into it without
// caring about the underlying entity type or how Cronos was spawned.
@EventBusSubscriber(modid = ProjectTitanCore.MODID)
public class CronosEvents {
    private static final String CRONOS_TAG = "projecttitancore_cronos";
    private static final ResourceLocation CRONOS_SLAIN =
            ResourceLocation.fromNamespaceAndPath(ProjectTitanCore.MODID, "cronos_slain");

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.getTags().contains(CRONOS_TAG)) return;
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        AdvancementHolder holder = serverLevel.getServer().getAdvancements().get(CRONOS_SLAIN);
        if (holder == null) {
            ProjectTitanCore.LOGGER.warn("[Cronos] cronos_slain advancement missing - kill not credited.");
            return;
        }
        killer.getAdvancements().award(holder, "impossible");
    }
}
