package com.seb.projecttitancore.compat.emi;

import com.seb.projecttitancore.ProjectTitanCore;
import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

@EmiEntrypoint
public class TitanCoreEmiPlugin implements EmiPlugin {

    public static final EmiStack WORKSTATION =
            EmiStack.of(ProjectTitanCore.TITAN_CORE_BLOCK_ITEM.get());

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(ProjectTitanCore.MODID, "titan_core"),
            WORKSTATION
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, WORKSTATION);

        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        for (RecipeHolder<TitanCoreRecipe> holder :
                recipeManager.getAllRecipesFor(ProjectTitanCore.TITAN_CORE_RECIPE_TYPE.get())) {
            registry.addRecipe(new TitanCoreEmiRecipe(holder));
        }
    }
}
