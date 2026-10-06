package io.github.scwunge.madscience.content.block;

import io.github.scwunge.madscience.content.entity.AbominationEntity;
import io.github.scwunge.madscience.registry.ModBlockEntities;
import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Incubation timer for an Abomination Egg (6000 ticks), then hatching once a player is within 5 blocks. */
public class AbominationEggBlockEntity extends BlockEntity {
    public static final int INCUBATION_TICKS = 6000;
    private static final double HATCH_RANGE = 5.0;
    private static final int MAX_NEARBY = 6;

    private int incubation;

    public AbominationEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ABOMINATION_EGG.get(), pos, state);
    }

    public int incubation() {
        return incubation;
    }

    public void setIncubation(int ticks) {
        incubation = ticks;
        setChanged();
    }

    private boolean playerNearby(Level level) {
        return level.hasNearbyAlivePlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, HATCH_RANGE);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AbominationEggBlockEntity egg) {
        if (egg.playerNearby(level)) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + level.random.nextFloat(), pos.getY() + level.random.nextFloat(),
                    pos.getZ() + level.random.nextFloat(), 0, 0, 0);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbominationEggBlockEntity egg) {
        if (egg.incubation < INCUBATION_TICKS) {
            egg.incubation++;
            if (egg.incubation % 100 == 0) {
                egg.setChanged();
            }
            return;
        }
        if (!egg.playerNearby(level)) {
            return;
        }
        int nearby = level.getEntitiesOfClass(AbominationEntity.class, new AABB(pos).inflate(2, 4, 2)).size();
        if (nearby >= MAX_NEARBY) {
            return;
        }
        AbominationEntity hatchling = ModEntities.ABOMINATION.get().create(level);
        if (hatchling == null) {
            return;
        }
        hatchling.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        hatchling.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
        level.addFreshEntity(hatchling);
        hatchling.playAmbientSound();
        level.levelEvent(2004, pos, 0);
        level.playSound(null, pos, ModSounds.ABOMINATION_EGGHATCH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        level.removeBlock(pos, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Incubation", incubation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        incubation = tag.getInt("Incubation");
    }
}
