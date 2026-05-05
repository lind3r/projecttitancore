package com.seb.projecttitancore.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seb.projecttitancore.ProjectTitanCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record TitanCoreRecipe(
        List<SizedIngredient> inputs,
        FluidStack fluidIngredient,
        int energyPerTick,
        int craftingTime,
        ItemStack result
) implements Recipe<TitanCoreRecipeInput> {

    public static final int INPUT_COUNT = 9;

    @Override
    public boolean matches(TitanCoreRecipeInput input, Level level) {
        if (inputs.size() != INPUT_COUNT) return false;
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (!inputs.get(i).test(input.getItem(i))) return false;
        }
        FluidStack inTank = input.fluidTank().getFluid();
        return inTank.getFluid() == fluidIngredient.getFluid()
                && inTank.getAmount() >= fluidIngredient.getAmount();
    }

    @Override
    public ItemStack assemble(TitanCoreRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProjectTitanCore.TITAN_CORE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ProjectTitanCore.TITAN_CORE_RECIPE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<TitanCoreRecipe> {
        private static final MapCodec<TitanCoreRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        SizedIngredient.FLAT_CODEC.listOf().fieldOf("inputs").forGetter(TitanCoreRecipe::inputs),
                        FluidStack.CODEC.fieldOf("fluid").forGetter(TitanCoreRecipe::fluidIngredient),
                        Codec.INT.fieldOf("energy_per_tick").forGetter(TitanCoreRecipe::energyPerTick),
                        Codec.INT.fieldOf("crafting_time").forGetter(TitanCoreRecipe::craftingTime),
                        ItemStack.CODEC.fieldOf("result").forGetter(TitanCoreRecipe::result)
                ).apply(instance, TitanCoreRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TitanCoreRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), TitanCoreRecipe::inputs,
                        FluidStack.STREAM_CODEC,                             TitanCoreRecipe::fluidIngredient,
                        ByteBufCodecs.VAR_INT,                               TitanCoreRecipe::energyPerTick,
                        ByteBufCodecs.VAR_INT,                               TitanCoreRecipe::craftingTime,
                        ItemStack.STREAM_CODEC,                              TitanCoreRecipe::result,
                        TitanCoreRecipe::new
                );

        @Override
        public MapCodec<TitanCoreRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TitanCoreRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
