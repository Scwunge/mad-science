package io.github.scwunge.madscience.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.scwunge.madscience.client.ClientRegistry;
import io.github.scwunge.madscience.content.machine.magloader.MagazineLoaderBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Magazine Loader at the original's half scale: the pusher slides along the feed as the load goes on, taking the
 * rounds in front of it with it, and the magazine shows while there is one in the machine.
 */
public class MagazineLoaderRenderer extends MachineRenderer<MagazineLoaderBlockEntity> {
    private static final ResourceLocation FULL = ClientRegistry.modelTexture("magazine_loader", "full");
    private static final ResourceLocation EMPTY = ClientRegistry.modelTexture("magazine_loader", "empty");

    private final ModelPart[] pushers = new ModelPart[6];
    private final ModelPart[] rounds = new ModelPart[5];
    private final ModelPart magazine;
    private final ModelPart magazineBase;

    public MagazineLoaderRenderer(BlockEntityRendererProvider.Context context) {
        super(context, ClientRegistry.MAGAZINE_LOADER, (be, pt) -> be.hasRounds() ? FULL : EMPTY);
        for (int i = 0; i < pushers.length; i++) {
            pushers[i] = model.getChild("p" + i + "_push" + i);
        }
        for (int i = 0; i < rounds.length; i++) {
            rounds[i] = model.getChild("b" + i + "_bullet" + i);
        }
        magazine = model.getChild("mag_magazine");
        magazineBase = model.getChild("magbase_magazinebase");
    }

    @Override
    protected void place(MagazineLoaderBlockEntity machine, PoseStack pose) {
        pose.translate(0.5, 0.25, 0.5);
        pose.scale(0.5F, 0.5F, 0.5F);
    }

    @Override
    protected void animate(MagazineLoaderBlockEntity loader, float partialTick) {
        int stage = loader.pushStage();
        for (int i = 0; i < pushers.length; i++) {
            pushers[i].visible = stage == i + 1;
        }
        // stage 1 still shows all five rounds; each later stage has pushed one more into the magazine
        for (int i = 0; i < rounds.length; i++) {
            rounds[i].visible = stage > 0 && i >= stage - 1;
        }
        magazine.visible = loader.showMagazine();
        magazineBase.visible = loader.showMagazine();
    }
}
