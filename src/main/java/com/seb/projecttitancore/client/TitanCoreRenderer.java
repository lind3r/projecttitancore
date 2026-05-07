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
    // Beam colours are shifted away from the holy palette's GOLDHI/HALO_A constants
    // because the beacon shader darkens the tint along the beam's vertical stripes;
    // shades of (255, 224, 84) land in olive territory at ~50% brightness. Bumping
    // red and trimming green keeps the darkened variants warm gold instead of green.
    /** Crafting beam — warm holy gold. */
    private static final int BEAM_COLOR_ACTIVE = 0xFFB84A;
    /** Idle beam — pale warm halo, matches the idle sphere so the visual reads as one continuous glow. */
    private static final int BEAM_COLOR_IDLE = 0xFFE0AC;
    private static final float INNER_RADIUS = 0.30f;
    private static final float OUTER_RADIUS = 0.44f;
    /** Idle beam, only while the projection is visible. ~50% the radii of the active beam. */
    private static final float IDLE_INNER_RADIUS = 0.16f;
    private static final float IDLE_OUTER_RADIUS = 0.32f;
    /** Beam emanates from the centre of the block (sphere's centre), not the bottom. */
    private static final float BEAM_Y_OFFSET = 0.5f;

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

    /**
     * Additive RenderType for the inner glow sphere — overlapping fragments
     * brighten cumulatively (LIGHTNING_TRANSPARENCY: srcRGB·srcAlpha + dstRGB),
     * so the pole-degenerate quads at the top/bottom of the UV sphere read as
     * a soft hot core rather than a banding artifact.
     */
    private static final RenderType SPHERE_TYPE = RenderType.create(
            "titan_core_sphere",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            512,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderType.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .createCompositeState(false));

    // Sphere geometry / animation constants. Lives inside the 12×12×12 inner
    // volume of the cage (frame edges are 2px → inside is 0.125..0.875).
    private static final float SPHERE_BASE_RADIUS = 0.22f;
    private static final float SPHERE_PULSE_AMPLITUDE = 0.018f;
    /** Outer glow radius as a multiple of the (pulsed) inner radius. */
    private static final float SPHERE_GLOW_SCALE = 1.55f;
    private static final int SPHERE_LON = 16;
    private static final int SPHERE_LAT = 10;

    // Sphere colours intentionally match BEAM_COLOR_IDLE / BEAM_COLOR_ACTIVE so the
    // beam reads as light emerging from the sphere. Same warm-gold shift as the beams
    // (away from green-leaning halo/goldhi) — additive blending exaggerates green-tint
    // even more than the beacon shader does.
    private static final int COLOR_IDLE_HALO = 0xFFE0AC;
    private static final int COLOR_GOLD_HIGHLIGHT = 0xFFB84A;

    public TitanCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(TitanCoreBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;

        long gameTime = be.getLevel().getGameTime();
        boolean crafting = be.getBlockState().getValue(TitanCoreBlock.CRAFTING);
        boolean projectionShown = be.titanTier > 0 && TitanProjection.get().maxTier > 0;

        if (crafting) {
            renderBeamPass(pose, buffers, partialTick, gameTime, 0f,  INNER_RADIUS, OUTER_RADIUS, BEAM_COLOR_ACTIVE);
            renderBeamPass(pose, buffers, partialTick, gameTime, 45f, INNER_RADIUS, OUTER_RADIUS, BEAM_COLOR_ACTIVE);
        } else if (projectionShown) {
            // Idle beam only renders when the projection is visible — otherwise the beam would
            // point at empty air, which reads as broken rather than dormant.
            // Two passes (yaw 0° + 45°) — the beam quad cross-section is square, and a single
            // pass reads as a square at any radius. Crossing two squares produces an 8-pointed
            // star that reads as round/cylindrical.
            renderBeamPass(pose, buffers, partialTick, gameTime, 0f,  IDLE_INNER_RADIUS, IDLE_OUTER_RADIUS, BEAM_COLOR_IDLE);
            renderBeamPass(pose, buffers, partialTick, gameTime, 45f, IDLE_INNER_RADIUS, IDLE_OUTER_RADIUS, BEAM_COLOR_IDLE);
        }

        if (projectionShown) {
            renderProjection(be.titanTier, partialTick, gameTime, pose, buffers);
        }

        renderSphere(be.titanTier, crafting, partialTick, gameTime, pose, buffers);
    }

    private static void renderBeamPass(PoseStack pose, MultiBufferSource buffers,
                                       float partialTick, long gameTime, float yawDegrees,
                                       float innerRadius, float outerRadius, int color) {
        pose.pushPose();
        pose.translate(0.5, BEAM_Y_OFFSET, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        pose.translate(-0.5, 0.0, -0.5);
        BeaconRenderer.renderBeaconBeam(
                pose, buffers, BEAM_TEXTURE,
                partialTick, 1.0f, gameTime,
                0, BEAM_HEIGHT, color,
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
     * Pulsing sphere inside the glass cage. Two layers — a brighter inner core
     * and a softer outer halo, both additive. Tier drives the colour from cool
     * halo white toward warm gold; crafting jumps to full gold highlight; pulse
     * rides the same {@link #PULSE_PERIOD_TICKS} clock as the projection so all
     * the slow visuals breathe together.
     */
    private static void renderSphere(int titanTier, boolean crafting, float partialTick, long gameTime,
                                     PoseStack pose, MultiBufferSource buffers) {
        float t = gameTime + partialTick;
        float pulse = (float) Math.sin(t * (Math.PI * 2.0 / PULSE_PERIOD_TICKS));
        float innerRadius = SPHERE_BASE_RADIUS + SPHERE_PULSE_AMPLITUDE * pulse;
        float outerRadius = innerRadius * SPHERE_GLOW_SCALE;

        // Tier 0 = pure halo, tier 10 = halfway to gold. Crafting overrides to full gold.
        float tierT = Math.min(1f, Math.max(0f, titanTier / 10f));
        int rgb = crafting
                ? COLOR_GOLD_HIGHLIGHT
                : lerpColor(COLOR_IDLE_HALO, COLOR_GOLD_HIGHLIGHT, tierT * 0.5f);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // Brightness: dim while cold (tier 0, !crafting), brighter as tiers rise,
        // brightest while crafting. Pulse adds a small breathe on top.
        float baseBrightness;
        if (crafting)             baseBrightness = 1.00f;
        else if (titanTier == 0)  baseBrightness = 0.35f;
        else                      baseBrightness = 0.55f + tierT * 0.30f;
        float breathe = 1f + 0.10f * pulse;
        float brightness = baseBrightness * breathe;

        int innerAlpha = clampByte(Math.round(220f * brightness));
        int outerAlpha = clampByte(Math.round(110f * brightness));

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        VertexConsumer buf = buffers.getBuffer(SPHERE_TYPE);
        Matrix4f matrix = pose.last().pose();

        emitSphere(buf, matrix, outerRadius, r, g, b, outerAlpha);
        emitSphere(buf, matrix, innerRadius, r, g, b, innerAlpha);

        pose.popPose();
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF,   tg = (to >> 8) & 0xFF,   tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int clampByte(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    /**
     * UV-sphere emitter — SPHERE_LAT × SPHERE_LON quads centred at the current
     * pose origin. Pole rings produce degenerate quads (top/bottom rings
     * collapse to a point); under additive blending these read as a hot core
     * rather than artifacts.
     */
    private static void emitSphere(VertexConsumer buf, Matrix4f m, float radius,
                                   int r, int g, int b, int a) {
        for (int lat = 0; lat < SPHERE_LAT; lat++) {
            float theta1 = (float) Math.PI * lat / SPHERE_LAT;
            float theta2 = (float) Math.PI * (lat + 1) / SPHERE_LAT;
            float sinT1 = (float) Math.sin(theta1), cosT1 = (float) Math.cos(theta1);
            float sinT2 = (float) Math.sin(theta2), cosT2 = (float) Math.cos(theta2);
            for (int lon = 0; lon < SPHERE_LON; lon++) {
                float phi1 = (float) (2 * Math.PI * lon / SPHERE_LON);
                float phi2 = (float) (2 * Math.PI * (lon + 1) / SPHERE_LON);
                float cosP1 = (float) Math.cos(phi1), sinP1 = (float) Math.sin(phi1);
                float cosP2 = (float) Math.cos(phi2), sinP2 = (float) Math.sin(phi2);

                float x1 = radius * sinT1 * cosP1, y1 = radius * cosT1, z1 = radius * sinT1 * sinP1;
                float x2 = radius * sinT1 * cosP2, y2 = radius * cosT1, z2 = radius * sinT1 * sinP2;
                float x3 = radius * sinT2 * cosP2, y3 = radius * cosT2, z3 = radius * sinT2 * sinP2;
                float x4 = radius * sinT2 * cosP1, y4 = radius * cosT2, z4 = radius * sinT2 * sinP1;

                buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
                buf.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
                buf.addVertex(m, x3, y3, z3).setColor(r, g, b, a);
                buf.addVertex(m, x4, y4, z4).setColor(r, g, b, a);
            }
        }
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
