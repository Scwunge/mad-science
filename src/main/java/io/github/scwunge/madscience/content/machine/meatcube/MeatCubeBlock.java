package io.github.scwunge.madscience.content.machine.meatcube;

import io.github.scwunge.madscience.content.machine.MachineBlock;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

/** Punch the Meat Cube (without sneaking) to tear meat off it; use it to open its feeding GUI. */
public class MeatCubeBlock extends MachineBlock {
    public MeatCubeBlock(Properties properties) {
        super(properties, ModBlockEntities.MEAT_CUBE, Shapes.block(), false);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MeatCubeBlockEntity cube) {
            ItemStack meat = cube.tearMeat();
            if (!meat.isEmpty()) {
                Block.popResource(level, pos.above(), meat);
            }
        }
    }
}
