package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.dnaextractor.DnaExtractorBlockEntity;
import io.github.scwunge.madscience.content.machine.sanitizer.SanitizerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MadScience.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DnaExtractorBlockEntity>> DNA_EXTRACTOR = REGISTER.register("dna_extractor",
            () -> BlockEntityType.Builder.of(DnaExtractorBlockEntity::new, ModBlocks.DNA_EXTRACTOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SanitizerBlockEntity>> SANITIZER = REGISTER.register("sanitizer",
            () -> BlockEntityType.Builder.of(SanitizerBlockEntity::new, ModBlocks.SANITIZER.get()).build(null));

    private ModBlockEntities() {
    }
}
