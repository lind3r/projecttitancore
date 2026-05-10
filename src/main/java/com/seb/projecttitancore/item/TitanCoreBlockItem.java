package com.seb.projecttitancore.item;

import com.seb.projecttitancore.ProjectTitanCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** BlockItem variant that surfaces the preserved Titan projection tier as a tooltip line. */
public class TitanCoreBlockItem extends BlockItem {
    public TitanCoreBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Integer tier = stack.get(ProjectTitanCore.TITAN_TIER_COMPONENT.get());
        if (tier != null && tier > 0) {
            tooltip.add(Component.translatable("tooltip.projecttitancore.titan_core.tier", tier)
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
