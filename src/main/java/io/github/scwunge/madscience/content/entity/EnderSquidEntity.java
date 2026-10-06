package io.github.scwunge.madscience.content.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Ender Squid (enderman + squid genomes): an enderman with a squid's love of water. It stalks players who look at it,
 * like an enderman, but water doesn't hurt it and it never picks up blocks.
 */
public class EnderSquidEntity extends EnderMan {
    public EnderSquidEntity(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // no block carrying in the original
        goalSelector.getAvailableGoals().removeIf(goal -> goal.getGoal().getClass().getEnclosingClass() == EnderMan.class
                && goal.getGoal().getClass().getSimpleName().contains("Block"));
    }
}
