package io.github.scwunge.madscience.content.entity;

import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Enderslime (enderman + slime genomes): a slime that blinks away when hurt like an enderman, shrugs off fire and
 * potions, and sheds Enderslime (used for the Soniclocator's thumpers) as it splits.
 */
public class EnderslimeEntity extends Slime {
    public EnderslimeEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && (source.getDirectEntity() instanceof Projectile || random.nextBoolean())
                && io.github.scwunge.madscience.MadConfig.ABOMINATION_TELEPORTS.get()) {
            for (int i = 0; i < 64; i++) {
                double x = getX() + (random.nextDouble() - 0.5) * 64.0;
                double y = getY() + (random.nextInt(64) - 32);
                double z = getZ() + (random.nextDouble() - 0.5) * 64.0;
                if (AbominationEntity.teleportTo(this, x, y, z)) {
                    return true;
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        // the original dropped a piece of enderslime for every smaller slime it split into
        if (!level().isClientSide && getSize() > 1 && isDeadOrDying()) {
            int pieces = 2 + random.nextInt(3);
            for (int i = 0; i < pieces; i++) {
                spawnAtLocation(new ItemStack(ModItems.ENDERSLIME.get()));
            }
        }
        super.remove(reason);
    }

    @Override
    public void aiStep() {
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.PORTAL, getRandomX(0.5), getRandomY() - 0.25, getRandomZ(0.5),
                        (random.nextDouble() - 0.5) * 2.0, -random.nextDouble(), (random.nextDouble() - 0.5) * 2.0);
            }
        }
        super.aiStep();
    }

    @Override
    public void playerTouch(net.minecraft.world.entity.player.Player player) {
        float health = player.getHealth();
        super.playerTouch(player);
        if (player.getHealth() < health) {
            playSound(SoundEvents.ENDERMAN_SCREAM, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F * getSize();
    }
}
