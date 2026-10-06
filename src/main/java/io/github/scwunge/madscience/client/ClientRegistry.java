package io.github.scwunge.madscience.client;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.client.model.DnaExtractorModel;
import io.github.scwunge.madscience.client.model.IncubatorModel;
import io.github.scwunge.madscience.client.model.MainframeModel;
import io.github.scwunge.madscience.client.model.SanitizerModel;
import io.github.scwunge.madscience.client.model.SequencerModel;
import io.github.scwunge.madscience.client.render.MachineItemRenderer;
import io.github.scwunge.madscience.client.render.MachineRenderer;
import io.github.scwunge.madscience.client.screen.DnaExtractorScreen;
import io.github.scwunge.madscience.client.screen.IncubatorScreen;
import io.github.scwunge.madscience.client.screen.MainframeScreen;
import io.github.scwunge.madscience.client.screen.SanitizerScreen;
import io.github.scwunge.madscience.client.screen.SequencerScreen;
import io.github.scwunge.madscience.content.machine.incubator.IncubatorBlockEntity;
import io.github.scwunge.madscience.content.machine.mainframe.MainframeBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModMenus;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Screens, block entity renderers, model layers and machine item renderers. */
@EventBusSubscriber(modid = MadScience.MODID, value = Dist.CLIENT)
public final class ClientRegistry {
    public static final ModelLayerLocation DNA_EXTRACTOR = layer("dna_extractor");
    public static final ModelLayerLocation SANITIZER = layer("sanitizer");
    public static final ModelLayerLocation SEQUENCER = layer("sequencer");
    public static final ModelLayerLocation MAINFRAME = layer("mainframe");
    public static final ModelLayerLocation INCUBATOR = layer("incubator");

    /** Machine items drawn with their block model: item, layer, texture. */
    private record MachineItem(Supplier<? extends Block> block, ModelLayerLocation layer, ResourceLocation texture) {
    }

    private static final List<MachineItem> MACHINE_ITEMS = List.of(
            new MachineItem(ModBlocks.DNA_EXTRACTOR, DNA_EXTRACTOR, modelTexture("dna_extractor", "idle")),
            new MachineItem(ModBlocks.SANITIZER, SANITIZER, modelTexture("sanitizer", "idle")),
            new MachineItem(ModBlocks.SEQUENCER, SEQUENCER, modelTexture("sequencer", "idle")),
            new MachineItem(ModBlocks.MAINFRAME, MAINFRAME, modelTexture("mainframe", "off")),
            new MachineItem(ModBlocks.INCUBATOR, INCUBATOR, modelTexture("incubator", "idle")));

    private ClientRegistry() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(MadScience.id(name), "main");
    }

    /** Texture of a machine model: {@code textures/model/<machine>/<name>.png}, the original's animation frames. */
    public static ResourceLocation modelTexture(String machine, String name) {
        return MadScience.id("textures/model/" + machine + "/" + name + ".png");
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DNA_EXTRACTOR, DnaExtractorModel::create);
        event.registerLayerDefinition(SANITIZER, SanitizerModel::create);
        event.registerLayerDefinition(SEQUENCER, SequencerModel::create);
        event.registerLayerDefinition(MAINFRAME, MainframeModel::create);
        event.registerLayerDefinition(INCUBATOR, IncubatorModel::create);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        idleOrWorking(event, ModBlockEntities.DNA_EXTRACTOR.get(), DNA_EXTRACTOR, "dna_extractor", "work_", 12, 25);
        idleOrWorking(event, ModBlockEntities.SANITIZER.get(), SANITIZER, "sanitizer", "work_", 10, 15);
        idleOrWorking(event, ModBlockEntities.SEQUENCER.get(), SEQUENCER, "sequencer", "work_", 10, 15);

        ResourceLocation mainframeOff = modelTexture("mainframe", "off");
        ResourceLocation[] mainframeIdle = frames("mainframe", "idle_", 2);
        ResourceLocation[] mainframeWork = frames("mainframe", "work_", 9);
        ResourceLocation[] mainframeWarning = frames("mainframe", "warning_", 6);
        ResourceLocation[] mainframeNoWater = frames("mainframe", "no_water_", 5);
        event.registerBlockEntityRenderer(ModBlockEntities.MAINFRAME.get(), ctx -> new MachineRenderer<MainframeBlockEntity>(ctx, MAINFRAME,
                (be, pt) -> switch (be.state()) {
                    case OFF -> mainframeOff;
                    // the original blinks its idle screen for one tick in every ten
                    case POWERED -> mainframeIdle[be.getLevel() != null && be.getLevel().getGameTime() % 10 == 0 ? 0 : 1];
                    case ACTIVE -> mainframeWork[MachineRenderer.frame(be, 9, 3)];
                    case OVERHEATING -> mainframeWarning[MachineRenderer.frame(be, 6, 5)];
                    case NO_WATER -> mainframeNoWater[MachineRenderer.frame(be, 5, 8)];
                }));

        ResourceLocation incubatorIdle = modelTexture("incubator", "idle");
        ResourceLocation incubatorPowered = modelTexture("incubator", "powered");
        ResourceLocation incubatorReady = modelTexture("incubator", "ready");
        ResourceLocation[] incubatorWork = frames("incubator", "work_", 5);
        event.registerBlockEntityRenderer(ModBlockEntities.INCUBATOR.get(), ctx -> new MachineRenderer<IncubatorBlockEntity>(ctx, INCUBATOR,
                (be, pt) -> switch (be.state()) {
                    case IDLE -> incubatorIdle;
                    case POWERED -> incubatorPowered;
                    case READY -> incubatorReady;
                    case WORKING -> incubatorWork[MachineRenderer.frame(be, 5, 5)];
                }));
    }

    /** Renderer for a machine that shows "idle" when stopped and cycles work frames while active. */
    private static <T extends MachineBlockEntity> void idleOrWorking(EntityRenderersEvent.RegisterRenderers event, BlockEntityType<T> type,
                                                                     ModelLayerLocation layer, String machine, String prefix, int frames, int ticksPerFrame) {
        ResourceLocation[] work = frames(machine, prefix, frames);
        ResourceLocation idle = modelTexture(machine, "idle");
        event.registerBlockEntityRenderer(type, ctx -> new MachineRenderer<T>(ctx, layer,
                (be, pt) -> be.isActive() ? work[MachineRenderer.frame(be, frames, ticksPerFrame)] : idle));
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.DNA_EXTRACTOR.get(), DnaExtractorScreen::new);
        event.register(ModMenus.SANITIZER.get(), SanitizerScreen::new);
        event.register(ModMenus.SEQUENCER.get(), SequencerScreen::new);
        event.register(ModMenus.MAINFRAME.get(), MainframeScreen::new);
        event.register(ModMenus.INCUBATOR.get(), IncubatorScreen::new);
    }

    private static MachineItemRenderer itemRenderer;

    /** Shared renderer for machine items; created on first use, once the game's model set exists. */
    static MachineItemRenderer itemRenderer() {
        if (itemRenderer == null) {
            itemRenderer = new MachineItemRenderer();
            for (MachineItem entry : MACHINE_ITEMS) {
                itemRenderer.add(entry.block().get().asItem(), entry.layer(), entry.texture(), 1.0F);
            }
        }
        return itemRenderer;
    }

    /** Items drawn by {@link #itemRenderer()}. */
    static Item[] machineItems() {
        List<Item> items = new ArrayList<>();
        for (MachineItem entry : MACHINE_ITEMS) {
            items.add(entry.block().get().asItem());
        }
        return items.toArray(Item[]::new);
    }

    static ResourceLocation[] frames(String machine, String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = modelTexture(machine, prefix + i);
        }
        return frames;
    }
}
