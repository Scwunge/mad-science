package io.github.scwunge.madscience.client;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.client.render.WeaponRenderers;
import io.github.scwunge.madscience.content.item.TintedItem;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = MadScience.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    static void clientExtensions(RegisterClientExtensionsEvent event) {
        for (ModFluids.Kind kind : new ModFluids.Kind[]{ModFluids.LIQUID_DNA, ModFluids.MUTANT_DNA}) {
            ResourceLocation still = MadScience.id("block/" + kind.name + "_still");
            ResourceLocation flowing = MadScience.id("block/" + kind.name + "_flowing");
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flowing;
                }
            }, kind.type.get());
        }
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ClientRegistry.itemRenderer();
            }
        }, ClientRegistry.machineItems());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return WeaponRenderers.itemRenderer();
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                // the rifle is held up and aimed, like a loaded crossbow
                return stack.is(ModItems.PULSE_RIFLE.get()) ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
            }
        }, WeaponRenderers.items());
    }

    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item event) {
        Item[] tinted = ModItems.TAB_ORDER.stream().map(holder -> (Item) holder.get()).filter(item -> item instanceof TintedItem).toArray(Item[]::new);
        event.register((stack, layer) -> {
            int tint = ((TintedItem) stack.getItem()).tint(layer);
            return tint == -1 ? -1 : FastColor.ARGB32.opaque(tint);
        }, tinted);
    }
}
