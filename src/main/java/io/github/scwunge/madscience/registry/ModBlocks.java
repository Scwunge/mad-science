package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.MachineBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(MadScience.MODID);

    public static final DeferredBlock<MachineBlock> DNA_EXTRACTOR = machine("dna_extractor",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.DNA_EXTRACTOR));
    public static final DeferredBlock<MachineBlock> SANITIZER = machine("sanitizer",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.SANITIZER));
    public static final DeferredBlock<MachineBlock> SEQUENCER = machine("sequencer",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.SEQUENCER));
    public static final DeferredBlock<MachineBlock> MAINFRAME = machine("mainframe",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.MAINFRAME));
    public static final DeferredBlock<MachineBlock> INCUBATOR = machine("incubator",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.INCUBATOR));

    private ModBlocks() {
    }

    public static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion();
    }

    private static <B extends Block> DeferredBlock<B> machine(String name, Supplier<B> block) {
        DeferredBlock<B> holder = REGISTER.register(name, block);
        DeferredItem<BlockItem> item = ModItems.REGISTER.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        ModItems.TAB_ORDER.add(item);
        return holder;
    }
}
