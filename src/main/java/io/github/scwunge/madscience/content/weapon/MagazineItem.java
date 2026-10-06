package io.github.scwunge.madscience.content.weapon;

import io.github.scwunge.madscience.content.item.TooltipItem;
import io.github.scwunge.madscience.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Pulse rifle magazine holding up to 99 rounds. Load it by crafting it with rounds, or in a Magazine Loader. */
public class MagazineItem extends TooltipItem {
    public MagazineItem(Properties properties) {
        super(properties.component(ModDataComponents.ROUNDS.get(), 0));
    }

    public static int rounds(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ROUNDS.get(), 0);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return rounds(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * rounds(stack) / RifleState.MAX_ROUNDS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFD000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.madscience.magazine_rounds", rounds(stack), RifleState.MAX_ROUNDS).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
