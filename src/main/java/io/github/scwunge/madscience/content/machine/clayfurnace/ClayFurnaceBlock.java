package io.github.scwunge.madscience.content.machine.clayfurnace;

import io.github.scwunge.madscience.content.machine.MachineBlock;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * The Clay Furnace block: lit with flint and steel, harvested by hitting it once the burn is over (see
 * {@link ClayFurnaceBlockEntity}). Like the original it can't sit under a solid block.
 */
public class ClayFurnaceBlock extends MachineBlock {
    public ClayFurnaceBlock(Properties properties) {
        super(properties, ModBlockEntities.CLAY_FURNACE, Shapes.block(), false);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.FLINT_AND_STEEL) && level.getBlockEntity(pos) instanceof ClayFurnaceBlockEntity furnace) {
            if (!level.isClientSide && furnace.light()) {
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // the inventory can only be opened before it is lit
        if (level.getBlockEntity(pos) instanceof ClayFurnaceBlockEntity furnace && furnace.phase() != ClayFurnaceBlockEntity.Phase.IDLE) {
            return InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide || player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof ClayFurnaceBlockEntity furnace)) {
            return;
        }
        switch (furnace.phase()) {
            case SMOULDERING -> furnace.breakShell();
            case RED_HOT -> {
                // too soon: the molten metal spills out
                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            }
            case COOLED -> {
                BlockState result = furnace.resultBlock();
                if (result != null) {
                    furnace.takeResult();
                    level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.setBlockAndUpdate(pos, result);
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.above()).isSolid();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
