package io.github.scwunge.madscience.client.render;

import io.github.scwunge.madscience.client.ClientRegistry;
import io.github.scwunge.madscience.content.machine.meatcube.MeatCubeBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Shows one chunk of meat per level of meat left, with the original's twitching textures. */
public class MeatCubeRenderer extends MachineRenderer<MeatCubeBlockEntity> {
    private static final ResourceLocation[] FRAMES = new ResourceLocation[10];

    static {
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = ClientRegistry.modelTexture("meat_cube", "meatcube_" + i);
        }
    }

    private final ModelPart[] pieces = new ModelPart[MeatCubeBlockEntity.MAX_MEAT];

    public MeatCubeRenderer(BlockEntityRendererProvider.Context context) {
        super(context, ClientRegistry.MEAT_CUBE, (be, pt) -> FRAMES[Math.min(9, be.twitchFrame(be.getLevel() == null ? 0 : be.getLevel().getGameTime()))]);
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = model.getChild("piece" + (i + 1) + "_meat" + (i + 1));
        }
    }

    @Override
    protected void animate(MeatCubeBlockEntity cube, float partialTick) {
        for (int i = 0; i < pieces.length; i++) {
            pieces[i].visible = cube.meat() > i;
        }
    }
}
