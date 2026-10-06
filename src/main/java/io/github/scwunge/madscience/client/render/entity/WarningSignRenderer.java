package io.github.scwunge.madscience.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.sign.WarningSignEntity;
import io.github.scwunge.madscience.content.sign.WarningSignType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a warning sign like the original: a block-sized board 1/32 thick, its symbol taken from a 32x32 cell of the
 * sign sheet, with a plain back and edges from a corner of the sheet.
 */
public class WarningSignRenderer extends EntityRenderer<WarningSignEntity> {
    private static final ResourceLocation TEXTURE = MadScience.id("textures/entity/warning_sign.png");
    private static final float SHEET = 256.0F;
    /** Half the board's thickness, in blocks. */
    private static final float DEPTH = 1.0F / 64.0F;

    public WarningSignRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WarningSignEntity sign, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        WarningSignType type = sign.signType();
        int light = LevelRenderer.getLightColor(sign.level(), BlockPos.containing(sign.getBoundingBox().getCenter()));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose last = pose.last();

        float u0 = type.u / SHEET, u1 = (type.u + 32) / SHEET, v0 = type.v / SHEET, v1 = (type.v + 32) / SHEET;
        // front (facing out from the wall)
        quad(last, consumer, light, 0.5F, -0.5F, -0.5F, 0.5F, -DEPTH, u0, v1, u1, v0, 0, 0, -1);
        // back and edges: the plain board corner of the sheet
        float bu0 = 192 / SHEET, bu1 = 208 / SHEET, bv0 = 0, bv1 = 16 / SHEET, edge = 0.5F / SHEET;
        quad(last, consumer, light, -0.5F, -0.5F, 0.5F, 0.5F, DEPTH, bu0, bv1, bu1, bv0, 0, 0, 1);
        edge(last, consumer, light, bu0, bu1, edge);
        pose.popPose();
        super.render(sign, entityYaw, partialTick, pose, buffers, packedLight);
    }

    /** A flat face at depth z from (x0, y0) to (x1, y1), its corners mapped to (u0, v0)..(u1, v1). */
    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int light, float x0, float y0, float x1, float y1, float z,
                             float u0, float v0, float u1, float v1, int nx, int ny, int nz) {
        vertex(pose, consumer, x0, y0, z, u0, v0, nx, ny, nz, light);
        vertex(pose, consumer, x1, y0, z, u1, v0, nx, ny, nz, light);
        vertex(pose, consumer, x1, y1, z, u1, v1, nx, ny, nz, light);
        vertex(pose, consumer, x0, y1, z, u0, v1, nx, ny, nz, light);
    }

    /** The four thin edges around the board. */
    private static void edge(PoseStack.Pose pose, VertexConsumer consumer, int light, float u0, float u1, float v) {
        float h = 0.5F;
        // top
        vertex(pose, consumer, -h, h, -DEPTH, u0, 0, 0, 1, 0, light);
        vertex(pose, consumer, -h, h, DEPTH, u0, v, 0, 1, 0, light);
        vertex(pose, consumer, h, h, DEPTH, u1, v, 0, 1, 0, light);
        vertex(pose, consumer, h, h, -DEPTH, u1, 0, 0, 1, 0, light);
        // bottom
        vertex(pose, consumer, -h, -h, DEPTH, u0, 0, 0, -1, 0, light);
        vertex(pose, consumer, -h, -h, -DEPTH, u0, v, 0, -1, 0, light);
        vertex(pose, consumer, h, -h, -DEPTH, u1, v, 0, -1, 0, light);
        vertex(pose, consumer, h, -h, DEPTH, u1, 0, 0, -1, 0, light);
        // left
        vertex(pose, consumer, -h, -h, -DEPTH, u0, 0, -1, 0, 0, light);
        vertex(pose, consumer, -h, -h, DEPTH, u0, v, -1, 0, 0, light);
        vertex(pose, consumer, -h, h, DEPTH, u1, v, -1, 0, 0, light);
        vertex(pose, consumer, -h, h, -DEPTH, u1, 0, -1, 0, 0, light);
        // right
        vertex(pose, consumer, h, -h, DEPTH, u0, 0, 1, 0, 0, light);
        vertex(pose, consumer, h, -h, -DEPTH, u0, v, 1, 0, 0, light);
        vertex(pose, consumer, h, h, -DEPTH, u1, v, 1, 0, 0, light);
        vertex(pose, consumer, h, h, DEPTH, u1, 0, 1, 0, 0, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v,
                               int nx, int ny, int nz, int light) {
        consumer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(WarningSignEntity sign) {
        return TEXTURE;
    }
}
