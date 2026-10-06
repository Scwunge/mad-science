package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes machine inventories, energy buffers and tanks to pipes and cables from any mod. */
@EventBusSubscriber(modid = MadScience.MODID)
public final class ModCapabilities {
    private ModCapabilities() {
    }

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        machine(event, ModBlockEntities.DNA_EXTRACTOR.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.DNA_EXTRACTOR.get(), (be, side) -> be.fluidHandler(side));
        machine(event, ModBlockEntities.SANITIZER.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.SANITIZER.get(), (be, side) -> be.fluidHandler(side));
        machine(event, ModBlockEntities.SEQUENCER.get());
        machine(event, ModBlockEntities.MAINFRAME.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MAINFRAME.get(), (be, side) -> be.fluidHandler(side));
        machine(event, ModBlockEntities.INCUBATOR.get());
        machine(event, ModBlockEntities.FREEZER.get());
        machine(event, ModBlockEntities.DUPLICATOR.get());
        machine(event, ModBlockEntities.BONDER.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CLAY_FURNACE.get(), MachineBlockEntity::sidedItemHandler);
        machine(event, ModBlockEntities.CRYOTUBE.get());
        machine(event, ModBlockEntities.SONICLOCATOR.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MEAT_CUBE.get(), MachineBlockEntity::sidedItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MEAT_CUBE.get(), (be, side) -> be.fluidHandler(side));
    }

    /** Items (side rules applied) and energy for a machine. */
    private static <T extends MachineBlockEntity> void machine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, MachineBlockEntity::sidedItemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> be.energy());
    }
}
