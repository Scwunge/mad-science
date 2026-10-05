package io.github.scwunge.madscience.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Liquid Mutant DNA is corrosive: it hurts anything standing in it, like in the original. */
public class MutantDnaBlock extends LiquidBlock {
    public MutantDnaBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            entity.hurt(level.damageSources().generic(), 5.0F);
        }
    }
}
