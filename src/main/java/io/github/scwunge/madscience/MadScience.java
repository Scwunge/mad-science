package io.github.scwunge.madscience;

import com.mojang.logging.LogUtils;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModCreativeTabs;
import io.github.scwunge.madscience.registry.ModDataComponents;
import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModMenus;
import io.github.scwunge.madscience.registry.ModRecipes;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MadScience.MODID)
public class MadScience {
    public static final String MODID = "madscience";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MadScience(IEventBus modBus, ModContainer container) {
        ModDataComponents.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModFluids.TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModCreativeTabs.REGISTER.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, MadConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
