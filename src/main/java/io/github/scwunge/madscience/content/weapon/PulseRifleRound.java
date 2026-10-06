package io.github.scwunge.madscience.content.weapon;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pulse rifle round: an explosive-tipped bullet (base damage 4.2, scaled by speed like an arrow). It pops on impact;
 * one round in a hundred ricochets with a bigger blast that can break blocks, if the server allows it (config and
 * mobGriefing). Explosions go through the normal explosion events, so claim mods can stop them.
 */
public class PulseRifleRound extends AbstractArrow {
    public PulseRifleRound(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public PulseRifleRound(Level level, LivingEntity shooter, ItemStack weapon) {
        super(ModEntities.PULSE_RIFLE_ROUND.get(), shooter, level, new ItemStack(ModItems.ROUND.get()), weapon);
        pickup = Pickup.DISALLOWED;
        setBaseDamage(4.2);
        setNoGravity(false);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.ROUND.get());
    }

    private void impact(Vec3 at) {
        if (level().isClientSide) {
            return;
        }
        if (random.nextInt(1000) < 10) {
            boolean breakBlocks = MadConfig.BULLETS_DAMAGE_WORLD.get() && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            level().explode(this, at.x, at.y, at.z, 1.5F, breakBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
            playSound(ModSounds.PULSE_RIFLE_RICOCHET.get(), 25.0F, 1.0F);
        } else {
            level().explode(this, at.x, at.y, at.z, 1.0F, Level.ExplosionInteraction.NONE);
        }
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        impact(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        impact(result.getLocation());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 200) {
            discard();
        }
    }
}
