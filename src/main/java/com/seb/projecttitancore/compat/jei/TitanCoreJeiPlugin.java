package com.seb.projecttitancore.compat.jei;

import com.seb.projecttitancore.ProjectTitanCore;
import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public class TitanCoreJeiPlugin implements IModPlugin {

    public static final RecipeType<TitanCoreRecipe> TITAN_CORE_TYPE =
            RecipeType.create(ProjectTitanCore.MODID, "titan_core", TitanCoreRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ProjectTitanCore.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new TitanCoreCraftingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ProjectTitanCore.TITAN_CORE_BLOCK_ITEM.get()),
                TITAN_CORE_TYPE
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<TitanCoreRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ProjectTitanCore.TITAN_CORE_RECIPE_TYPE.get())
                .stream().map(RecipeHolder::value).toList();
        registration.addRecipes(TITAN_CORE_TYPE, recipes);
    }
}
