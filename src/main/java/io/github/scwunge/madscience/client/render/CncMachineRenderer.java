package io.github.scwunge.madscience.client.render;

import io.github.scwunge.madscience.client.ClientRegistry;
import io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity;
import io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity.Status;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The CnC Machine's cut, step by step as in the original: the press comes down and squashes the iron block, lifts, then
 * the water jet works across the cut pieces one by one.
 */
public class CncMachineRenderer extends MachineRenderer<CncMachineBlockEntity> {
    private static final ResourceLocation OFF = ClientRegistry.modelTexture("cnc_machine", "off");
    private static final ResourceLocation POWERED = ClientRegistry.modelTexture("cnc_machine", "powered");
    private static final ResourceLocation READY = ClientRegistry.modelTexture("cnc_machine", "ready");
    private static final ResourceLocation[] WORK = ClientRegistry.frames("cnc_machine", "work", 7);
    /** Which water-jet model goes with each cutting step (the original's order). */
    private static final int[] WATER_FOR_STAGE = {0, 1, 2, 5, 4, 3, 6, 7, 8};

    private final ModelPart[] press = new ModelPart[4];
    private final ModelPart[] compressed = new ModelPart[4];
    private final ModelPart[] cut = new ModelPart[9];
    private final ModelPart[] water = new ModelPart[9];

    public CncMachineRenderer(BlockEntityRendererProvider.Context context) {
        super(context, ClientRegistry.CNC_MACHINE, (be, pt) -> switch (be.status()) {
            case OFF -> OFF;
            case OFFLINE -> POWERED;
            case WORKING -> be.stage() >= 7 ? WORK[frame(be, 7, 15)] : READY;
            default -> READY;
        });
        for (int i = 0; i < 4; i++) {
            press[i] = model.getChild("press" + i + "_press" + i);
            compressed[i] = model.getChild("comp" + i + "_compressedblock" + i);
        }
        for (int i = 0; i < 9; i++) {
            cut[i] = model.getChild("cut" + i + "_cutblock" + i);
            water[i] = model.getChild("water" + i + "_water" + i);
        }
    }

    @Override
    protected void animate(CncMachineBlockEntity machine, float partialTick) {
        for (ModelPart[] group : new ModelPart[][]{press, compressed, cut, water}) {
            for (ModelPart part : group) {
                part.visible = false;
            }
        }
        boolean working = machine.status() == Status.WORKING && machine.progress() > 0;
        int stage = machine.stage();
        if (!working) {
            press[0].visible = true;
            compressed[0].visible = machine.hasIron();
            return;
        }
        if (stage <= 3) {
            press[stage].visible = true;
            compressed[stage].visible = true;
        } else if (stage <= 7) {
            // the press lifts back up over the cut block
            press[7 - stage].visible = true;
            for (ModelPart piece : cut) {
                piece.visible = true;
            }
        } else {
            press[0].visible = true;
            int jet = Math.min(8, stage - 8);
            // the water jet works through the pieces, leaving the cut part behind
            for (int i = jet; i < cut.length; i++) {
                cut[i].visible = true;
            }
            water[WATER_FOR_STAGE[jet]].visible = true;
        }
    }
}
