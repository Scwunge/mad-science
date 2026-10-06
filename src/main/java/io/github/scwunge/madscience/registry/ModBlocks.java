package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.block.AbominationEggBlock;
import io.github.scwunge.madscience.content.block.EnderslimeBlock;
import io.github.scwunge.madscience.content.machine.MachineBlock;
import io.github.scwunge.madscience.content.machine.clayfurnace.ClayFurnaceBlock;
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
    public static final DeferredBlock<MachineBlock> FREEZER = machine("freezer",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.FREEZER));
    public static final DeferredBlock<MachineBlock> DUPLICATOR = machine("duplicator",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.DUPLICATOR));
    public static final DeferredBlock<MachineBlock> BONDER = machine("thermosonic_bonder",
            () -> new MachineBlock(machineProperties(), ModBlockEntities.BONDER));
    public static final DeferredBlock<ClayFurnaceBlock> CLAY_FURNACE = machine("clay_furnace",
            () -> new ClayFurnaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.25F, 4.2F)
                    .sound(SoundType.STONE).noOcclusion()));

    public static final DeferredBlock<AbominationEggBlock> ABOMINATION_EGG = machine("abomination_egg",
            () -> new AbominationEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1.0F)
                    .ignitedByLava().noOcclusion().lightLevel(state -> 1).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final DeferredBlock<EnderslimeBlock> ENDERSLIME_BLOCK = machine("enderslime_block",
            () -> new EnderslimeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F).sound(SoundType.SLIME_BLOCK)));

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
