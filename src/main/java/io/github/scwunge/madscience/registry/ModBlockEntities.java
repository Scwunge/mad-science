package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MadScience.MODID);

    private ModBlockEntities() {
    }
}
