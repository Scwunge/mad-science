package io.github.scwunge.madscience.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.scwunge.madscience.client.ClientRegistry;
import io.github.scwunge.madscience.content.machine.clayfurnace.ClayFurnaceBlockEntity;
import io.github.scwunge.madscience.content.machine.clayfurnace.ClayFurnaceBlockEntity.Phase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Clay Furnace shrinks to a small kiln while it's a furnace, then shows only the molten block (red hot, then a cooled
 * shell) once the clay is broken off, as in the original.
 */
public class ClayFurnaceRenderer extends MachineRenderer<ClayFurnaceBlockEntity> {
    private static final String[] FURNACE_PARTS = {"base1", "base2", "base3", "base4", "top1", "top2", "top3", "top4", "post1", "post2", "post3", "post4"};

    private static final ResourceLocation IDLE = ClientRegistry.modelTexture("clay_furnace", "idle");
    private static final ResourceLocation DONE = ClientRegistry.modelTexture("clay_furnace", "done");
    private static final ResourceLocation SHELL = ClientRegistry.modelTexture("clay_furnace", "shell");
    private static final ResourceLocation[] WORK = frames("work", 4);
    private static final ResourceLocation[] RED_HOT = frames("redhot", 5);

    public ClayFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        super(context, ClientRegistry.CLAY_FURNACE, (be, pt) -> switch (be.phase()) {
            case IDLE -> IDLE;
            case BURNING -> WORK[frame(be, 4, 25)];
            case SMOULDERING -> DONE;
            case RED_HOT -> RED_HOT[be.coolFrame()];
            case COOLED -> SHELL;
        });
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = ClientRegistry.modelTexture("clay_furnace", prefix + i);
        }
        return frames;
    }

    private static boolean isKiln(Phase phase) {
        return phase == Phase.IDLE || phase == Phase.BURNING || phase == Phase.SMOULDERING;
    }

    @Override
    protected void place(ClayFurnaceBlockEntity furnace, PoseStack pose) {
        if (isKiln(furnace.phase())) {
            pose.translate(0.5, 0.34, 0.5);
            pose.scale(0.6F, 0.68F, 0.6F);
        } else {
            pose.translate(0.5, 0.5, 0.5);
        }
    }

    @Override
    protected void animate(ClayFurnaceBlockEntity furnace, float partialTick) {
        Phase phase = furnace.phase();
        for (String name : FURNACE_PARTS) {
            setVisible(name, isKiln(phase));
        }
        setVisible("moltenblock", phase == Phase.RED_HOT);
        setVisible("moltenblockshell", phase == Phase.COOLED);
    }

    private void setVisible(String name, boolean visible) {
        if (model.hasChild(name)) {
            ModelPart part = model.getChild(name);
            part.visible = visible;
        }
    }
}
