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
 * Pulse rifle grenade: a heavy lobbed shell (base damage 50 on a direct hit) that bursts into a fiery explosion on
 * impact. It only breaks blocks when mobGriefing and the bulletsDamageWorld config are on, and claim mods can stop the blast.
 */
public class PulseRifleGrenade extends AbstractArrow {
    public PulseRifleGrenade(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public PulseRifleGrenade(Level level, LivingEntity shooter, ItemStack weapon) {
        super(ModEntities.PULSE_RIFLE_GRENADE.get(), shooter, level, new ItemStack(ModItems.GRENADE.get()), weapon);
        pickup = Pickup.DISALLOWED;
        setBaseDamage(50.0);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.GRENADE.get());
    }

    private void burst(Vec3 at) {
        if (level().isClientSide) {
            return;
        }
        playSound(ModSounds.PULSE_RIFLE_GRENADE_EXPLODE.get(), 25.0F, 1.0F);
        boolean breakBlocks = MadConfig.BULLETS_DAMAGE_WORLD.get() && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level().explode(this, at.x, at.y, at.z, 1.5F, true, breakBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        burst(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        burst(result.getLocation());
    }
}
