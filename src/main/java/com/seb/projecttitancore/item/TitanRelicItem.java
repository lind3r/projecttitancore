package com.seb.projecttitancore.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Trophy items — flagged Rarity.EPIC at registration; the appendHoverText override
// here just adds the holy flair tooltip lines. Each relic uses two translation keys:
//   tooltip.projecttitancore.<slug>_1 — the bold proclamation line
//   tooltip.projecttitancore.<slug>_2 — the italic flavor line
public class TitanRelicItem extends Item {
    private final String slug;

    public TitanRelicItem(Properties properties, String slug) {
        super(properties);
        this.slug = slug;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.projecttitancore." + slug + "_1")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.projecttitancore." + slug + "_2")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
