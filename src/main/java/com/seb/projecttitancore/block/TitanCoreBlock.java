package com.seb.projecttitancore.block;

import com.mojang.serialization.MapCodec;
import com.seb.projecttitancore.ProjectTitanCore;
import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import com.seb.projecttitancore.client.SkyTintEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class TitanCoreBlock extends BaseEntityBlock {
    public static final MapCodec<TitanCoreBlock> CODEC = simpleCodec(TitanCoreBlock::new);
    public static final BooleanProperty CRAFTING = BooleanProperty.create("crafting");

    public TitanCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CRAFTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRAFTING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TitanCoreBlockEntity be) {
                for (int i = 0; i < be.itemHandler.getSlots(); i++) {
                    Block.popResource(level, pos, be.itemHandler.getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TitanCoreBlockEntity tbe) {
                    FluidUtil.interactWithFluidHandler(player, hand, tbe.fluidTank);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof TitanCoreBlockEntity be) {
            if (!level.isClientSide()) {
                player.openMenu(be);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TitanCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            // Client-side ticker only observes the CRAFTING blockstate to drive the holy sky tint.
            // Lambda body — and the SkyTintEffect class — only loads on the client because this
            // branch is never entered on the dedicated server.
            return createTickerHelper(type, ProjectTitanCore.TITAN_CORE_BLOCK_ENTITY.get(),
                    (lvl, pos, st, be) -> SkyTintEffect.observe(pos, st.getValue(CRAFTING)));
        }
        return createTickerHelper(type, ProjectTitanCore.TITAN_CORE_BLOCK_ENTITY.get(),
                TitanCoreBlockEntity::tick);
    }
}
