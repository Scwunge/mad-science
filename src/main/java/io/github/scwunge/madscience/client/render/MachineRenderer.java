package io.github.scwunge.madscience.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.scwunge.madscience.content.machine.MachineBlock;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

/**
 * Draws a machine from its converted Techne model the same way the original renderer did: centred in the block,
 * turned to face the placer, flipped like the original Techne loader, with a texture picked from the machine's state
 * (the original animates by swapping whole textures).
 */
public class MachineRenderer<T extends MachineBlockEntity> implements BlockEntityRenderer<T> {
    protected final ModelPart model;
    private final BiFunction<T, Float, ResourceLocation> texture;

    public MachineRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer, BiFunction<T, Float, ResourceLocation> texture) {
        this.model = context.bakeLayer(layer);
        this.texture = texture;
    }

    /** Y rotation the original applied for a placer facing {@code playerFacing}. */
    public static float originalYaw(Direction playerFacing) {
        return -90.0F * playerFacing.get2DDataValue();
    }

    public static Direction playerFacing(BlockState state) {
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING).getOpposite() : Direction.SOUTH;
    }

    @Override
    public void render(T machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(originalYaw(playerFacing(machine.getBlockState()))));
        pose.scale(-1.0F, -1.0F, 1.0F); // the original Techne loader's flip
        animate(machine, partialTick);
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(texture.apply(machine, partialTick))), light, overlay);
        renderExtras(machine, partialTick, pose, buffers, light, overlay);
        pose.popPose();
    }

    /** Moves model parts before drawing (spinning reels, thumpers...). */
    protected void animate(T machine, float partialTick) {
    }

    /** Draws anything beyond the model, in model space. */
    protected void renderExtras(T machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    }

    /** Frame {@code 0..frames-1} of an animation that advances every {@code ticksPerFrame} game ticks. */
    public static int frame(MachineBlockEntity machine, int frames, int ticksPerFrame) {
        long time = machine.getLevel() == null ? 0 : machine.getLevel().getGameTime();
        return (int) ((time / ticksPerFrame) % frames);
    }
}
