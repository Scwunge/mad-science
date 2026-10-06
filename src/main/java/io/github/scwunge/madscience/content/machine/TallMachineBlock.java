package io.github.scwunge.madscience.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A machine two or three blocks tall (the original's "ghost blocks"). Part 0 is the machine itself with the block
 * entity and renderer; the parts above are invisible placeholders that pass clicks and breaking down to it.
 */
public class TallMachineBlock extends MachineBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

    private final int height;

    public TallMachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type, int height) {
        super(properties, type, Shapes.block(), false);
        this.height = height;
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    public int height() {
        return height;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    public static BlockPos basePos(BlockState state, BlockPos pos) {
        return pos.below(state.getValue(PART));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (int i = 1; i < height; i++) {
            BlockPos above = pos.above(i);
            if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above).canBeReplaced(context)) {
                return null;
            }
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        for (int i = 1; i < height; i++) {
            level.setBlock(pos.above(i), state.setValue(PART, i), 3);
        }
    }

    /** Each part needs the one below it (and part 0 needs the one above): otherwise it falls apart, like a door. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int part = state.getValue(PART);
        if (direction == Direction.DOWN && part > 0 && !(neighbor.is(this) && neighbor.getValue(PART) == part - 1)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction == Direction.UP && part < height - 1 && !(neighbor.is(this) && neighbor.getValue(PART) == part + 1)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Breaking an upper part breaks the machine itself, so it drops properly. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        int part = state.getValue(PART);
        if (!level.isClientSide && part > 0) {
            BlockPos base = basePos(state, pos);
            if (level.getBlockState(base).is(this)) {
                level.destroyBlock(base, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos base = basePos(state, pos);
        return super.useWithoutItem(level.getBlockState(base), level, base, player, hit);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == 0 ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? super.newBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(PART) == 0 ? super.getTicker(level, state, type) : null;
    }
}
