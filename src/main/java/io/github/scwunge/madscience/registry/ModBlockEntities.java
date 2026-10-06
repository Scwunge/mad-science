package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.dnaextractor.DnaExtractorBlockEntity;
import io.github.scwunge.madscience.content.machine.incubator.IncubatorBlockEntity;
import io.github.scwunge.madscience.content.machine.mainframe.MainframeBlockEntity;
import io.github.scwunge.madscience.content.machine.sanitizer.SanitizerBlockEntity;
import io.github.scwunge.madscience.content.machine.sequencer.SequencerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MadScience.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DnaExtractorBlockEntity>> DNA_EXTRACTOR =
            register("dna_extractor", DnaExtractorBlockEntity::new, ModBlocks.DNA_EXTRACTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SanitizerBlockEntity>> SANITIZER =
            register("sanitizer", SanitizerBlockEntity::new, ModBlocks.SANITIZER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SequencerBlockEntity>> SEQUENCER =
            register("sequencer", SequencerBlockEntity::new, ModBlocks.SEQUENCER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframeBlockEntity>> MAINFRAME =
            register("mainframe", MainframeBlockEntity::new, ModBlocks.MAINFRAME);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IncubatorBlockEntity>> INCUBATOR =
            register("incubator", IncubatorBlockEntity::new, ModBlocks.INCUBATOR);

    private ModBlockEntities() {
    }

    @SuppressWarnings("DataFlowIssue")
    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        return REGISTER.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }
}
