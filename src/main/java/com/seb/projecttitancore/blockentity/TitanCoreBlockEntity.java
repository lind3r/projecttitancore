package com.seb.projecttitancore.blockentity;

import com.seb.projecttitancore.ProjectTitanCore;
import com.seb.projecttitancore.block.TitanCoreBlock;
import com.seb.projecttitancore.menu.TitanCoreMenu;
import com.seb.projecttitancore.recipe.TitanCoreRecipe;
import com.seb.projecttitancore.recipe.TitanCoreRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TitanCoreBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;
    /** Buffer holds this many ticks of the active recipe's RF/t. Smooths sub-second jitter; too small to AFK-fill. */
    public static final int BUFFER_TICKS = 40;
    /** External networks may push at most this multiple of recipe RF/t per tick. Stops burst-charging from capacitor banks. */
    public static final int INPUT_RATE_MULTIPLIER = 2;
    /** Buffer when no recipe is active — large enough to be visible in the GUI, too small to matter. */
    public static final int IDLE_CAPACITY = 1000;
    public static final int IDLE_MAX_RECEIVE = 1000;
    public static final int FLUID_CAPACITY = 100_000;
    public static final int CONTAINER_DATA_COUNT = 6;
    public static final int BEAM_RENDER_HEIGHT = 15;

    public final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public int craftingProgress = 0;
    public int maxCraftingProgress = 0;

    /** Highest tier ever crafted at this core. Drives the cumulative Titan projection. */
    public int titanTier = 0;

    public final InternalEnergyStorage energyStorage = new InternalEnergyStorage(IDLE_CAPACITY, IDLE_MAX_RECEIVE);

    /** EnergyStorage that blocks external extraction, lets the machine drain its own buffer, and supports per-recipe resizing. */
    public static class InternalEnergyStorage extends EnergyStorage {
        public InternalEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        public boolean consume(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            return true;
        }

        public void configure(int newCapacity, int newMaxReceive) {
            this.capacity = newCapacity;
            this.maxReceive = newMaxReceive;
            if (this.energy > this.capacity) this.energy = this.capacity;
        }
    }

    public final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null) level.invalidateCapabilities(worldPosition);
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> fluidTank.getFluidAmount();
                case 3 -> fluidTank.getCapacity();
                case 4 -> craftingProgress;
                case 5 -> maxCraftingProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return CONTAINER_DATA_COUNT;
        }
    };

    public TitanCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ProjectTitanCore.TITAN_CORE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.projecttitancore.titan_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TitanCoreMenu(containerId, playerInventory, itemHandler, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("TitanTier", titanTier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, (IntTag) tag.get("Energy"));
        }
        fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        titanTier = tag.getInt("TitanTier");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TitanCoreBlockEntity be) {
        if (level.isClientSide) return;

        TitanCoreRecipeInput input = new TitanCoreRecipeInput(be.itemHandler, be.fluidTank);
        Optional<RecipeHolder<TitanCoreRecipe>> match = level.getRecipeManager()
                .getAllRecipesFor(ProjectTitanCore.TITAN_CORE_RECIPE_TYPE.get())
                .stream()
                .filter(h -> h.value().matches(input, level))
                .findFirst();

        if (match.isEmpty()) {
            be.energyStorage.configure(IDLE_CAPACITY, IDLE_MAX_RECEIVE);
            be.resetProgress();
            syncCraftingState(level, pos, state, false);
            return;
        }

        TitanCoreRecipe recipe = match.get().value();
        int rfPerTick = recipe.energyPerTick();
        be.energyStorage.configure(rfPerTick * BUFFER_TICKS, rfPerTick * INPUT_RATE_MULTIPLIER);

        // Check output slot has space
        ItemStack outputSlot = be.itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack recipeOutput = recipe.result();
        if (!outputSlot.isEmpty() &&
                !(ItemStack.isSameItem(outputSlot, recipeOutput) &&
                  outputSlot.getCount() + recipeOutput.getCount() <= outputSlot.getMaxStackSize())) {
            be.resetProgress();
            syncCraftingState(level, pos, state, false);
            return;
        }

        be.maxCraftingProgress = recipe.craftingTime();

        // Drain energy: success advances progress, failure decays it at the same rate.
        // Net-zero when underpowered means players can never finish without sustaining >= recipe RF/t on average.
        if (be.energyStorage.consume(rfPerTick)) {
            be.craftingProgress++;
            be.setChanged();
            syncCraftingState(level, pos, state, true);
        } else {
            if (be.craftingProgress > 0) {
                be.craftingProgress--;
                be.setChanged();
            }
            syncCraftingState(level, pos, state, false);
            return;
        }

        if (be.craftingProgress >= be.maxCraftingProgress) {
            // Consume inputs
            for (int i = 0; i < INPUT_SLOTS; i++) {
                be.itemHandler.extractItem(i, recipe.inputs().get(i).count(), false);
            }
            be.fluidTank.drain(recipe.fluidIngredient().amount(), IFluidHandler.FluidAction.EXECUTE);

            // Place output
            if (outputSlot.isEmpty()) {
                be.itemHandler.setStackInSlot(OUTPUT_SLOT, recipeOutput.copy());
            } else {
                outputSlot.grow(recipeOutput.getCount());
            }

            // Bump the cumulative titan tier and push a BE data packet so the
            // client renderer extends the projection immediately.
            if (recipe.tier() > be.titanTier) {
                be.titanTier = recipe.tier();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }

            be.craftingProgress = 0;
            be.maxCraftingProgress = 0;
            be.setChanged();
            syncCraftingState(level, pos, state, false);
        }
    }

    private static void syncCraftingState(Level level, BlockPos pos, BlockState state, boolean crafting) {
        if (state.getValue(TitanCoreBlock.CRAFTING) != crafting) {
            level.setBlock(pos, state.setValue(TitanCoreBlock.CRAFTING, crafting), Block.UPDATE_CLIENTS);
        }
    }

    private void resetProgress() {
        if (craftingProgress != 0 || maxCraftingProgress != 0) {
            craftingProgress = 0;
            maxCraftingProgress = 0;
            setChanged();
        }
    }
}
