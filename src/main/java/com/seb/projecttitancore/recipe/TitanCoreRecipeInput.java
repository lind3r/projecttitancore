package com.seb.projecttitancore.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public record TitanCoreRecipeInput(ItemStackHandler itemHandler, FluidTank fluidTank) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public int size() {
        return itemHandler.getSlots();
    }
}
