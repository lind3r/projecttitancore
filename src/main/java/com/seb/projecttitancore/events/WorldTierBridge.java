package com.seb.projecttitancore.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.seb.projecttitancore.ProjectTitanCore;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridges Titan Shard FTB Quest completion to Apotheosis' WorldTier system, so the player's
 * Apothic tier (Haven → Pinnacle) advances automatically and non-optionally as they progress
 * the shard quest chain. Pinnacle lands at T8 Soul so Mythic loot is in hand for the Titan Trial at T10.
 *
 * <p>Mapping is data-driven — see {@code data/projecttitancore/world_tier_quests.json}.
 * Reload-safe; updates pick up on {@code /reload}.
 *
 * <p>Two trigger paths cover both online and offline team members:
 * <ul>
 *   <li>{@link ObjectCompletedEvent#QUEST} — fires server-side at the moment of completion;
 *       applies tier to currently-online team members.</li>
 *   <li>{@link PlayerEvent.PlayerLoggedIn} — derives the highest-earned tier from the player's
 *       team data on join, so an offline teammate catches up when they next connect.
 *       (FTB Quests does not replay completion events on data sync.)</li>
 * </ul>
 *
 * <p>{@link WorldTier#setTier} bypasses the unlock check, so granting a tier server-side
 * also bypasses the Ctrl+T menu — activation is non-optional, exactly as desired.
 */
@EventBusSubscriber(modid = ProjectTitanCore.MODID)
public final class WorldTierBridge {
    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation MAPPING_FILE =
            ResourceLocation.fromNamespaceAndPath(ProjectTitanCore.MODID, "world_tier_quests.json");

    // LinkedHashMap preserves JSON file order, which by convention is lowest tier first.
    // The login-derive path doesn't actually rely on order — it picks the max-ordinal
    // matched tier — but readability of the loaded mapping benefits from it.
    private static volatile Map<Long, WorldTier> mappings = Map.of();

    private WorldTierBridge() {}

    /**
     * Called once from {@link ProjectTitanCore} constructor to wire up the FTB Quests
     * (Architectury) event. NeoForge events are auto-discovered via {@link EventBusSubscriber}.
     */
    public static void registerArchitecturyEvents() {
        ObjectCompletedEvent.QUEST.register(WorldTierBridge::onQuestCompleted);
    }

    private static EventResult onQuestCompleted(ObjectCompletedEvent.QuestEvent event) {
        Quest quest = event.getQuest();
        WorldTier tier = mappings.get(quest.id);
        if (tier != null) {
            for (ServerPlayer member : event.getOnlineMembers()) {
                WorldTier.setTier(member, tier);
            }
        }
        return EventResult.pass();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        applyHighestEarnedTier(sp);
    }

    private static void applyHighestEarnedTier(ServerPlayer player) {
        if (mappings.isEmpty()) return;
        TeamData td;
        try {
            td = TeamData.get(player);
        } catch (Exception ex) {
            // Player team not yet resolved — FTB Quests will fire other events later if needed.
            return;
        }
        if (td == null) return;

        WorldTier highest = null;
        for (Map.Entry<Long, WorldTier> entry : mappings.entrySet()) {
            QuestObject obj = td.getFile().get(entry.getKey());
            if (!(obj instanceof Quest q)) continue;
            if (td.isCompleted(q)) {
                if (highest == null || entry.getValue().ordinal() > highest.ordinal()) {
                    highest = entry.getValue();
                }
            }
        }
        if (highest != null) {
            WorldTier.setTier(player, highest);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) WorldTierBridge::reload);
    }

    private static void reload(ResourceManager resources) {
        Map<Long, WorldTier> next = new LinkedHashMap<>();
        var resOpt = resources.getResource(ResourceLocation.fromNamespaceAndPath(
                ProjectTitanCore.MODID, "world_tier_quests.json"));
        if (resOpt.isEmpty()) {
            LOG.warn("[WorldTierBridge] {} not found — Apothic tier bridge disabled.", MAPPING_FILE);
            mappings = next;
            return;
        }
        try (var reader = new InputStreamReader(resOpt.get().open())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue; // skip comment fields
                long id;
                try {
                    id = Long.parseUnsignedLong(key, 16);
                } catch (NumberFormatException nfe) {
                    LOG.warn("[WorldTierBridge] Skipping non-hex key: {}", key);
                    continue;
                }
                WorldTier tier = parseTier(entry.getValue().getAsString());
                if (tier == null) {
                    LOG.warn("[WorldTierBridge] Unknown tier name '{}' for quest {}", entry.getValue().getAsString(), key);
                    continue;
                }
                next.put(id, tier);
            }
        } catch (Exception ex) {
            LOG.error("[WorldTierBridge] Failed to read {}", MAPPING_FILE, ex);
        }
        mappings = next;
        LOG.info("[WorldTierBridge] Loaded {} quest→tier mapping(s)", next.size());
    }

    private static WorldTier parseTier(String name) {
        for (WorldTier t : WorldTier.values()) {
            if (t.getSerializedName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
