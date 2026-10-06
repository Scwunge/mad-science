package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, MadScience.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> DNA_EXTRACTOR = machine("dna_extractor");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SANITIZER = machine("sanitizer");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SEQUENCER = machine("sequencer");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MAINFRAME = machine("mainframe");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> INCUBATOR = machine("incubator");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> FREEZER = machine("freezer");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> DUPLICATOR = machine("duplicator");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> BONDER = machine("thermosonic_bonder");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CLAY_FURNACE = machine("clay_furnace");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CRYOTUBE = machine("cryotube");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SONICLOCATOR = machine("soniclocator");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MEAT_CUBE = machine("meat_cube");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> VOX_BOX = machine("vox_box");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MAGAZINE_LOADER = machine("magazine_loader");
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CNC_MACHINE = machine("cnc_machine");

    private ModMenus() {
    }

    private static DeferredHolder<MenuType<?>, MenuType<MachineMenu>> machine(String name) {
        DeferredHolder<MenuType<?>, MenuType<MachineMenu>>[] self = new DeferredHolder[1];
        self[0] = REGISTER.register(name, () -> IMenuTypeExtension.create(
                (id, inventory, buf) -> new MachineMenu(self[0].get(), id, inventory, buf)));
        return self[0];
    }
}
