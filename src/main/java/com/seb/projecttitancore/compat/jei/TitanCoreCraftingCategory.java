package com.seb.projecttitancore.compat.jei;

import com.seb.projecttitancore.ProjectTitanCore;
import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TitanCoreCraftingCategory implements IRecipeCategory<TitanCoreRecipe> {

    private final IDrawable icon;

    public TitanCoreCraftingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(
                new ItemStack(ProjectTitanCore.TITAN_CORE_BLOCK_ITEM.get())
        );
    }

    @Override
    public RecipeType<TitanCoreRecipe> getRecipeType() {
        return TitanCoreJeiPlugin.TITAN_CORE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.projecttitancore.titan_core");
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return 90;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TitanCoreRecipe recipe, IFocusGroup focuses) {
        // 3x3 input grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                SizedIngredient si = recipe.inputs().get(row * 3 + col);
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack s : si.ingredient().getItems()) {
                    ItemStack copy = s.copy();
                    copy.setCount(si.count());
                    stacks.add(copy);
                }
                builder.addSlot(RecipeIngredientRole.INPUT, 1 + col * 18, 1 + row * 18)
                        .addItemStacks(stacks);
            }
        }

        // Output slot — vertically centered with the grid middle row
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 19)
                .addItemStack(recipe.result());

        // Fluid slot
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 61)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(recipe.fluidIngredient().getFluids()));
    }

    @Override
    public void draw(TitanCoreRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // Arrow
        guiGraphics.drawString(font, "→", 58, 23, 0x404040, false);

        // Fluid amount
        guiGraphics.drawString(font,
                recipe.fluidIngredient().amount() + " mB",
                20, 63, 0x3F76E4, false);

        // Energy requirement
        guiGraphics.drawString(font,
                recipe.energyPerTick() + " RF/t",
                1, 78, 0xFF6600, false);

        // Crafting time in seconds
        guiGraphics.drawString(font,
                (recipe.craftingTime() / 20) + "s",
                70, 78, 0x606060, false);
    }
}
