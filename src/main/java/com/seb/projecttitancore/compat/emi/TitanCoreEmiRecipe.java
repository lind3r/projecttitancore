package com.seb.projecttitancore.compat.emi;

import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

public class TitanCoreEmiRecipe implements EmiRecipe {

    private final ResourceLocation id;
    private final TitanCoreRecipe recipe;
    private final List<EmiIngredient> itemInputs;
    private final EmiIngredient fluidInput;
    private final List<EmiStack> outputs;

    public TitanCoreEmiRecipe(RecipeHolder<TitanCoreRecipe> holder) {
        this.id = holder.id();
        this.recipe = holder.value();

        this.itemInputs = new ArrayList<>(TitanCoreRecipe.INPUT_COUNT);
        for (SizedIngredient si : recipe.inputs()) {
            this.itemInputs.add(NeoForgeEmiIngredient.of(si));
        }
        this.fluidInput = NeoForgeEmiIngredient.of(recipe.fluidIngredient());
        this.outputs = List.of(EmiStack.of(recipe.result()));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return TitanCoreEmiPlugin.CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        List<EmiIngredient> all = new ArrayList<>(itemInputs.size() + 1);
        all.addAll(itemInputs);
        all.add(fluidInput);
        return all;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 134;
    }

    @Override
    public int getDisplayHeight() {
        return 90;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                widgets.addSlot(itemInputs.get(row * 3 + col), 1 + col * 18, 1 + row * 18);
            }
        }

        widgets.addFillingArrow(60, 19, recipe.craftingTime() * 50);

        widgets.addSlot(outputs.get(0), 96, 14)
                .large(true)
                .recipeContext(this);

        widgets.addSlot(fluidInput, 1, 61);

        widgets.addText(
                Component.literal(recipe.fluidIngredient().amount() + " mB").getVisualOrderText(),
                22, 65, 0x3F76E4, false);

        widgets.addText(
                Component.literal(recipe.energyPerTick() + " RF/t").getVisualOrderText(),
                1, 80, 0xFF6600, false);

        widgets.addText(
                Component.literal((recipe.craftingTime() / 20) + "s").getVisualOrderText(),
                90, 80, 0x606060, false);
    }
}
