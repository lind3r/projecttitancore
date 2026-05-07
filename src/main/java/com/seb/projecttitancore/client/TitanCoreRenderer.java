package com.seb.projecttitancore.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.seb.projecttitancore.block.TitanCoreBlock;
import com.seb.projecttitancore.blockentity.TitanCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.List;

public class TitanCoreRenderer implements BlockEntityRenderer<TitanCoreBlockEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int BEAM_HEIGHT = TitanCoreBlockEntity.BEAM_RENDER_HEIGHT;
    private static final int BEAM_COLOR = 0xFFE054;
    private static final float INNER_RADIUS = 0.30f;
    private static final float OUTER_RADIUS = 0.44f;
    /** Idle beam, only while the projection is visible. ~50% the radii of the active beam. */
    private static final float IDLE_INNER_RADIUS = 0.16f;
    private static final float IDLE_OUTER_RADIUS = 0.32f;

    /** Degrees per game tick. ~0.4 deg/tick = full rotation in ~15 seconds. */
    private static final float ROTATION_SPEED_DEG_PER_TICK = 0.4f;
    /** Mid-point of the breathing alpha pulse (out of 255). */
    private static final float ALPHA_BASE = 110f;
    /** Half-amplitude of the alpha pulse. */
    private static final float ALPHA_AMPLITUDE = 30f;
    /** Pulse cycle period in ticks (~80 ticks = 4 seconds). */
    private static final float PULSE_PERIOD_TICKS = 80f;

    /**
     * Custom RenderType for the translucent projection: solid color, no texture,
     * culling off (we render both faces of the unit cubes for hologram correctness),
     * depth test on (terrain occludes), depth write off (alpha layers blend cleanly).
     */
    private static final RenderType PROJECTION_TYPE = RenderType.create(
            "titan_core_projection",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            512,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .createCompositeState(false));

    public TitanCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(TitanCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;

        long gameTime = be.getLevel().getGameTime();
        boolean projectionShown = be.titanTier > 0 && TitanProjection.get().maxTier > 0;

        if (be.getBlockState().getValue(TitanCoreBlock.CRAFTING)) {
            renderBeamPass(pose, buffers, partialTick, gameTime, 0f, INNER_RADIUS, OUTER_RADIUS);
            renderBeamPass(pose, buffers, partialTick, gameTime, 45f, INNER_RADIUS, OUTER_RADIUS);
        } else if (projectionShown) {
            // Idle beam only renders when the projection is visible — otherwise the beam would
            // point at empty air, which reads as broken rather than dormant.
            // Two passes (yaw 0° + 45°) — the beam quad cross-section is square, and a single
            // pass reads as a square at any radius. Crossing two squares produces an 8-pointed
            // star that reads as round/cylindrical.
            renderBeamPass(pose, buffers, partialTick, gameTime, 0f,  IDLE_INNER_RADIUS, IDLE_OUTER_RADIUS);
            renderBeamPass(pose, buffers, partialTick, gameTime, 45f, IDLE_INNER_RADIUS, IDLE_OUTER_RADIUS);
        }

        if (projectionShown) {
            renderProjection(be.titanTier, partialTick, gameTime, pose, buffers);
        }
    }

    private static void renderBeamPass(PoseStack pose, MultiBufferSource buffers,
                                       float partialTick, long gameTime, float yawDegrees,
                                       float innerRadius, float outerRadius) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        pose.translate(-0.5, 0.0, -0.5);
        BeaconRenderer.renderBeaconBeam(
                pose, buffers, BEAM_TEXTURE,
                partialTick, 1.0f, gameTime,
                0, BEAM_HEIGHT, BEAM_COLOR,
                innerRadius, outerRadius
        );
        pose.popPose();
    }

    private static void renderProjection(int maxTier, float partialTick, long gameTime,
                                         PoseStack pose, MultiBufferSource buffers) {
        TitanProjection projection = TitanProjection.get();
        if (projection.maxTier == 0) return;

        float t = gameTime + partialTick;
        float yawDeg = (t * ROTATION_SPEED_DEG_PER_TICK) % 360f;
        int alpha = Math.round(ALPHA_BASE + ALPHA_AMPLITUDE * (float) Math.sin(t * (Math.PI * 2.0 / PULSE_PERIOD_TICKS)));
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;

        pose.pushPose();
        pose.translate(0.5, BEAM_HEIGHT, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yawDeg));

        VertexConsumer buf = buffers.getBuffer(PROJECTION_TYPE);
        Matrix4f matrix = pose.last().pose();

        int upper = Math.min(maxTier, projection.maxTier);
        for (int tier = 1; tier <= upper; tier++) {
            for (TitanProjection.Color color : TitanProjection.Color.values()) {
                List<Vec3i> voxels = projection.voxels(tier, color);
                if (voxels.isEmpty()) continue;
                int r = (color.rgb >> 16) & 0xFF;
                int g = (color.rgb >> 8) & 0xFF;
                int b = color.rgb & 0xFF;
                for (Vec3i v : voxels) {
                    emitCube(buf, matrix,
                            v.getX() - 0.5f, v.getY(),     v.getZ() - 0.5f,
                            v.getX() + 0.5f, v.getY() + 1, v.getZ() + 0.5f,
                            r, g, b, alpha);
                }
            }
        }

        pose.popPose();
    }

    /**
     * Emit a 6-face cuboid as 24 vertices in QUADS mode with POSITION_COLOR.
     * Cull is disabled in PROJECTION_TYPE so winding order is not checked.
     */
    private static void emitCube(VertexConsumer buf, Matrix4f m,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 int r, int g, int b, int a) {
        // -Y (bottom)
        buf.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(m, x1, y0, z0).setColor(r, g, b, a);
        buf.addVertex(m, x1, y0, z1).setColor(r, g, b, a);
        buf.addVertex(m, x0, y0, z1).setColor(r, g, b, a);
        // +Y (top)
        buf.addVertex(m, x0, y1, z0).setColor(r, g, b, a);
        buf.addVertex(m, x0, y1, z1).setColor(r, g, b, a);
        buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(m, x1, y1, z0).setColor(r, g, b, a);
        // -Z (north)
        buf.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(m, x0, y1, z0).setColor(r, g, b, a);
        buf.addVertex(m, x1, y1, z0).setColor(r, g, b, a);
        buf.addVertex(m, x1, y0, z0).setColor(r, g, b, a);
        // +Z (south)
        buf.addVertex(m, x0, y0, z1).setColor(r, g, b, a);
        buf.addVertex(m, x1, y0, z1).setColor(r, g, b, a);
        buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(m, x0, y1, z1).setColor(r, g, b, a);
        // -X (west)
        buf.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(m, x0, y0, z1).setColor(r, g, b, a);
        buf.addVertex(m, x0, y1, z1).setColor(r, g, b, a);
        buf.addVertex(m, x0, y1, z0).setColor(r, g, b, a);
        // +X (east)
        buf.addVertex(m, x1, y0, z0).setColor(r, g, b, a);
        buf.addVertex(m, x1, y1, z0).setColor(r, g, b, a);
        buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(m, x1, y0, z1).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen(TitanCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    // Covers the 15-tall beam plus the ~50-tall titan projection above it,
    // with horizontal slack for the projection's slow Y rotation. NeoForge 1.21
    // calls this on the renderer (not the BE) for frustum culling — without it
    // the dispatcher sees only the unit cube and culls beam + projection the
    // moment the core leaves the camera frustum.
    @Override
    public AABB getRenderBoundingBox(TitanCoreBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        return new AABB(cx - 10, pos.getY(), cz - 10, cx + 10, pos.getY() + 85, cz + 10);
    }
}
