package io.github.scwunge.madscience.client.render;

import io.github.scwunge.madscience.client.ClientRegistry;
import io.github.scwunge.madscience.content.machine.soniclocator.SoniclocatorBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Soniclocator's screen shows its state, and its three thumper piles rise one after another as it charges, then
 * slam down when it thumps (the original's offsets, driven by the synced charge).
 */
public class SoniclocatorRenderer extends MachineRenderer<SoniclocatorBlockEntity> {
    /** How high each pile lifts, in blocks (the original's ceiling: max charge x 0.003). */
    private static final float LIFT = SoniclocatorBlockEntity.MAX_CHARGE * 0.003F;

    private static final ResourceLocation OFF = ClientRegistry.modelTexture("soniclocator", "off");
    private static final ResourceLocation UNDERVOLT = ClientRegistry.modelTexture("soniclocator", "undervolt");
    private static final ResourceLocation IDLE = ClientRegistry.modelTexture("soniclocator", "idle");
    private static final ResourceLocation EMPTY = ClientRegistry.modelTexture("soniclocator", "404");
    private static final ResourceLocation[] IDLE_FRAMES = frames("idle", 9);
    private static final ResourceLocation[] CHARGE = frames("charge", 13);
    private static final ResourceLocation[] COOLDOWN = frames("cooldown", 5);

    private final ModelPart[] thumpers;
    private final float restY;

    public SoniclocatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context, ClientRegistry.SONICLOCATOR, (be, pt) -> switch (be.state()) {
            case OFF -> OFF;
            case UNDERVOLT -> be.getLevel() != null && be.getLevel().getGameTime() % 20 == 0 ? IDLE : UNDERVOLT;
            case IDLE -> IDLE_FRAMES[frame(be, 9, 5)];
            case CHARGING -> CHARGE[Math.min(12, be.charge() * 13 / SoniclocatorBlockEntity.MAX_CHARGE)];
            case EMPTY -> be.getLevel() != null && be.getLevel().getGameTime() % 20 == 0 ? IDLE : EMPTY;
            case COOLDOWN -> COOLDOWN[Math.min(4, be.cooldownFrame())];
        });
        thumpers = new ModelPart[]{model.getChild("thumper1"), model.getChild("thumper2"), model.getChild("thumper3")};
        restY = thumpers[0].y;
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = ClientRegistry.modelTexture("soniclocator", prefix + i);
        }
        return frames;
    }

    @Override
    protected void animate(SoniclocatorBlockEntity machine, float partialTick) {
        float progress = machine.state() == SoniclocatorBlockEntity.State.CHARGING ? machine.charge() / (float) SoniclocatorBlockEntity.MAX_CHARGE : 0F;
        for (int i = 0; i < thumpers.length; i++) {
            float pile = Math.max(0F, Math.min(1F, progress * 3F - i));
            // model space is flipped, so lifting means a smaller y
            thumpers[i].y = restY - pile * LIFT * 16F;
        }
    }
}
