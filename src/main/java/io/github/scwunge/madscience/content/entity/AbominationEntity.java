package io.github.scwunge.madscience.content.entity;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.Set;

/**
 * The Abomination (enderman + spider genomes): a fast, wall-climbing, fire-proof horror that attacks players and other
 * creatures, blinks away when hurt (always from projectiles, half the time otherwise), and lays an egg where its victims
 * die (see {@link AbominationEvents}).
 */
public class AbominationEntity extends Spider {
    /** The original's authors, whom it refuses to attack. */
    private static final Set<String> FRIENDS = Set.of("ronwolf", "FoxDiller", "Prowlerwolf");

    public AbominationEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 42.0)
                .add(Attributes.MOVEMENT_SPEED, 0.666666666666)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 42.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Mob.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, false, true,
                target -> !(target instanceof AbominationEntity)));
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target instanceof Player player && FRIENDS.contains(player.getGameProfile().getName())) {
            if (level().getGameTime() % 15 == 0) {
                level().addParticle(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 0, 0.2, 0);
            }
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        boolean dodge = source.getDirectEntity() instanceof Projectile || random.nextBoolean();
        if (dodge && !level().isClientSide) {
            for (int i = 0; i < 64; i++) {
                if (teleportRandomly()) {
                    return true;
                }
            }
        }
        return super.hurt(source, amount);
    }

    private boolean teleportRandomly() {
        if (!MadConfig.ABOMINATION_TELEPORTS.get()) {
            return false;
        }
        double x = getX() + (random.nextDouble() - 0.5) * 64.0;
        double y = getY() + (random.nextInt(64) - 32);
        double z = getZ() + (random.nextDouble() - 0.5) * 64.0;
        return teleportTo(this, x, y, z);
    }

    /** Ender-style teleport: lands on solid ground, honours EntityTeleportEvent, leaves a portal trail. */
    static boolean teleportTo(LivingEntity entity, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        Level level = entity.level();
        while (pos.getY() > level.getMinBuildHeight() && !level.getBlockState(pos).blocksMotion()) {
            pos.move(0, -1, 0);
        }
        if (!level.getBlockState(pos).blocksMotion() || level.getBlockState(pos.above()).liquid()) {
            return false;
        }
        EntityTeleportEvent.EnderEntity event = EventHooks.onEnderTeleport(entity, x, pos.getY() + 1, z);
        if (event.isCanceled()) {
            return false;
        }
        Vec3 from = entity.position();
        boolean moved = entity.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true);
        if (moved) {
            level.playSound(null, from.x, from.y, from.z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, entity.getSoundSource(), 1.0F, 1.0F);
            entity.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        return moved;
    }

    @Override
    public void aiStep() {
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.PORTAL, getRandomX(0.5), getRandomY() - 0.25, getRandomZ(0.5),
                        (random.nextDouble() - 0.5) * 2.0, -random.nextDouble(), (random.nextDouble() - 0.5) * 2.0);
            }
        } else if (getTarget() == null && random.nextInt(100) == 0 && level().getNearestPlayer(this, 16) != null) {
            playSound(ModSounds.ABOMINATION_GROWL.get(), 1.0F, 0.5F);
        }
        super.aiStep();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ABOMINATION_HISS.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ABOMINATION_PAIN.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ABOMINATION_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.ABOMINATION_STEP.get(), 1.0F, 1.0F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
