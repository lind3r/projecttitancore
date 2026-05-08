package com.seb.projecttitancore.events;

import com.seb.projecttitancore.ProjectTitanCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Adds a red warning to the Titan Trial gate pearl tooltip. Other gateway pearls untouched.
// We can't compile against Gateways/Placebo (no build-time dep), so the gateway ID is
// pulled out via reflection on Placebo's DynamicHolder#getId. If the API ever changes the
// reflection silently no-ops and the warning just doesn't show — gameplay unaffected.
@EventBusSubscriber(modid = ProjectTitanCore.MODID, value = Dist.CLIENT)
public class TitanTrialPearlTooltip {
    private static final ResourceLocation GATEWAY_PEARL =
            ResourceLocation.fromNamespaceAndPath("gateways", "gate_pearl");
    private static final ResourceLocation GATEWAY_COMPONENT =
            ResourceLocation.fromNamespaceAndPath("gateways", "gateway");
    private static final ResourceLocation TITAN_TRIAL =
            ResourceLocation.fromNamespaceAndPath(ProjectTitanCore.MODID, "titan_trial");

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!GATEWAY_PEARL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return;
        if (!TITAN_TRIAL.equals(extractGatewayId(stack))) return;
        var lines = event.getToolTip();
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.projecttitancore.titan_trial_warning_1")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        lines.add(Component.translatable("tooltip.projecttitancore.titan_trial_warning_2")
                .withStyle(ChatFormatting.RED));
    }

    private static ResourceLocation extractGatewayId(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(GATEWAY_COMPONENT);
        if (type == null) return null;
        Object holder = stack.get(type);
        if (holder == null) return null;
        try {
            Object id = holder.getClass().getMethod("getId").invoke(holder);
            return id instanceof ResourceLocation rl ? rl : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
