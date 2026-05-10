package com.seb.projecttitancore.menu;

import com.seb.projecttitancore.ProjectTitanCore;
import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class TitanCoreMenu extends AbstractContainerMenu {
    private static final int MACHINE_INPUT_START = 0;
    private static final int MACHINE_INPUT_END   = TitanCoreBlockEntity.INPUT_SLOTS;       // 9
    private static final int MACHINE_OUTPUT      = TitanCoreBlockEntity.OUTPUT_SLOT;       // 9
    private static final int PLAYER_INV_START    = TitanCoreBlockEntity.TOTAL_SLOTS;       // 10
    private static final int PLAYER_INV_END      = PLAYER_INV_START + 27;                  // 37
    private static final int HOTBAR_START        = PLAYER_INV_END;                         // 37
    private static final int HOTBAR_END          = HOTBAR_START + 9;                       // 46

    /** Button id for the void-fluid button. Sent via {@link net.minecraft.client.multiplayer.MultiPlayerGameMode#handleInventoryButtonClick}. */
    public static final int BUTTON_VOID_FLUID = 0;

    private final ContainerData data;
    private final BlockPos blockPos;

    // Client-side constructor — called by IMenuTypeExtension from the network buffer.
    public TitanCoreMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                new ItemStackHandler(TitanCoreBlockEntity.TOTAL_SLOTS),
                new SimpleContainerData(TitanCoreBlockEntity.CONTAINER_DATA_COUNT),
                buf.readBlockPos());
    }

    // Server-side constructor — called from TitanCoreBlockEntity.createMenu
    public TitanCoreMenu(int containerId, Inventory playerInventory,
                         IItemHandler itemHandler, ContainerData data, BlockPos blockPos) {
        super(ProjectTitanCore.TITAN_CORE_MENU.get(), containerId);
        this.data = data;
        this.blockPos = blockPos;
        checkContainerDataCount(data, TitanCoreBlockEntity.CONTAINER_DATA_COUNT);

        // Input slots: 3x3 grid starting at pixel (8, 17), 18px spacing
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(itemHandler, row * 3 + col, 8 + col * 18, 17 + row * 18));
            }
        }

        // Output slot: right of centre
        addSlot(new OutputSlot(itemHandler, TitanCoreBlockEntity.OUTPUT_SLOT, 116, 35));

        // Player inventory (rows 1-3, skipping hotbar)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    public BlockPos getBlockPos()     { return blockPos; }
    public int getEnergy()            { return data.get(0); }
    public int getMaxEnergy()         { return data.get(1); }
    public int getFluidAmount()       { return data.get(2); }
    public int getFluidCapacity()     { return data.get(3); }
    public int getCraftingProgress()  { return data.get(4); }
    public int getMaxCraftingProgress() { return data.get(5); }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_VOID_FLUID) {
            if (player.level().getBlockEntity(blockPos) instanceof TitanCoreBlockEntity be) {
                be.fluidTank.setFluid(FluidStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        if (index == MACHINE_OUTPUT) {
            // Output → player main inv, then hotbar
            if (!moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, false) &&
                !moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < MACHINE_INPUT_END) {
            // Machine input → player main inv + hotbar
            if (!moveItemStackTo(slotStack, PLAYER_INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            // Player main inv → machine input; overflow to hotbar
            if (!moveItemStackTo(slotStack, MACHINE_INPUT_START, MACHINE_INPUT_END, false) &&
                !moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Hotbar → machine input; overflow to player main inv
            if (!moveItemStackTo(slotStack, MACHINE_INPUT_START, MACHINE_INPUT_END, false) &&
                !moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        return original;
    }

    private static class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
