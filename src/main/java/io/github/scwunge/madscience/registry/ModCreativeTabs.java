package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MadScience.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.madscience"))
            .icon(() -> new ItemStack(ModItems.EMPTY_SYRINGE.get()))
            .displayItems((params, output) -> ModItems.TAB_ORDER.forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {
    }
}
