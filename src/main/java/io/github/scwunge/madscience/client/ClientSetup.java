package io.github.scwunge.madscience.client;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.item.TintedItem;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = MadScience.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
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
