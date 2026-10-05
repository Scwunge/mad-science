package io.github.scwunge.madscience.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** An item with a lore tooltip ({@code <description id>.tooltip}) that is shown while SHIFT is held. */
public class TooltipItem extends Item {
    public TooltipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addLore(getDescriptionId(), tooltip);
    }

    public static void addLore(String descriptionId, List<Component> tooltip) {
        if (FMLEnvironment.dist.isClient() && Screen.hasShiftDown()) {
            tooltip.add(Component.translatable(descriptionId + ".tooltip").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.madscience.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
