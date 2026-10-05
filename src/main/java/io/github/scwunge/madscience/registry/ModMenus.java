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

    private ModMenus() {
    }

    private static DeferredHolder<MenuType<?>, MenuType<MachineMenu>> machine(String name) {
        DeferredHolder<MenuType<?>, MenuType<MachineMenu>>[] self = new DeferredHolder[1];
        self[0] = REGISTER.register(name, () -> IMenuTypeExtension.create(
                (id, inventory, buf) -> new MachineMenu(self[0].get(), id, inventory, buf.readBlockPos())));
        return self[0];
    }
}
