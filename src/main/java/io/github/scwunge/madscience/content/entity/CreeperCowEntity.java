package io.github.scwunge.madscience.content.entity;

import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creeper Cow (creeper + cow genomes): a cow that sneaks up and explodes like a creeper, with a bigger blast (radius 5,
 * or 10 when charged by lightning). Milking it with a bucket gives Liquid Mutant DNA. Explosions follow mobGriefing.
 */
public class CreeperCowEntity extends Creeper {
    public CreeperCowEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        this.explosionRadius = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET) && !player.getAbilities().instabuild) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModFluids.MUTANT_DNA.bucket.get())));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void ignite() {
        // the original mooed as it blew
        playSound(SoundEvents.COW_AMBIENT, 1.0F, 0.5F);
        super.ignite();
    }

    @Override
    public void setSwellDir(int state) {
        if (state > 0 && getSwellDir() <= 0) {
            playSound(SoundEvents.COW_AMBIENT, 1.0F, 0.5F);
        }
        super.setSwellDir(state);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CREEPER_COW_ATTACK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.COW_STEP, 0.15F, 1.0F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
