package com.seb.projecttitancore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seb.projecttitancore.block.TitanCoreBlock;
import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TitanCoreRenderer implements BlockEntityRenderer<TitanCoreBlockEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int BEAM_HEIGHT = TitanCoreBlockEntity.BEAM_RENDER_HEIGHT;
    private static final int BEAM_COLOR = 0xFFE054;
    private static final float INNER_RADIUS = 0.15f;
    private static final float OUTER_RADIUS = 0.22f;

    public TitanCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(TitanCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        if (!be.getBlockState().getValue(TitanCoreBlock.CRAFTING)) return;

        long gameTime = be.getLevel().getGameTime();

        renderPass(pose, buffers, partialTick, gameTime, 0f);
        renderPass(pose, buffers, partialTick, gameTime, 45f);
    }

    private static void renderPass(PoseStack pose, MultiBufferSource buffers,
                                   float partialTick, long gameTime, float yawDegrees) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        pose.translate(-0.5, 0.0, -0.5);
        BeaconRenderer.renderBeaconBeam(
                pose, buffers, BEAM_TEXTURE,
                partialTick, 1.0f, gameTime,
                0, BEAM_HEIGHT, BEAM_COLOR,
                INNER_RADIUS, OUTER_RADIUS
        );
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TitanCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
