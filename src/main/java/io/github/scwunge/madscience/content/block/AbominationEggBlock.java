package io.github.scwunge.madscience.content.block;

import com.mojang.serialization.MapCodec;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Abomination Egg: laid where the Abomination's victims fall. It falls like sand, incubates for five minutes, then hatches
 * a new Abomination when a player comes within five blocks. Breaking it bursts it into Liquid Mutant DNA.
 */
public class AbominationEggBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);
    public static final MapCodec<AbominationEggBlock> CODEC = simpleCodec(AbominationEggBlock::new);

    public AbominationEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.playSound(null, pos, ModSounds.ABOMINATION_EGG.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 5);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 5);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() > level.getMinBuildHeight()) {
            net.minecraft.world.entity.item.FallingBlockEntity.fall(level, pos, state);
        }
    }

    /** Bursts into mutant DNA when broken by a player or blown up, like the original. */
    private static void burst(Level level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.ABOMINATION_EGGPOP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        level.setBlockAndUpdate(pos, ModFluids.MUTANT_DNA.block.get().defaultBlockState());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (!level.isClientSide) {
            ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
        }
        return result;
    }

    @Override
    public void destroy(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level real && !real.isClientSide) {
            burst(real, pos);
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            burst(level, pos);
        }
    }

    @Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                          @Nullable BlockEntity blockEntity, @Nullable net.minecraft.world.entity.Entity breaker, ItemStack tool) {
        return 15 + level.getRandom().nextInt(15) + level.getRandom().nextInt(15);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbominationEggBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ABOMINATION_EGG.get(),
                level.isClientSide ? AbominationEggBlockEntity::clientTick : AbominationEggBlockEntity::serverTick);
    }
}
