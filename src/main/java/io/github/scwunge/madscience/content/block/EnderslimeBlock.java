package io.github.scwunge.madscience.content.block;

import io.github.scwunge.madscience.content.item.TooltipItem;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Block of Enderslime: nine pieces of enderslime, used to build the Soniclocator's thumpers. "Oh god, it's slimy..." */
public class EnderslimeBlock extends Block {
    public EnderslimeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.playSound(null, pos, ModSounds.ABOMINATION_EGG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TooltipItem.addLore(getDescriptionId(), tooltip);
    }
}
