package io.github.scwunge.madscience.content.entity;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Wooly Cow (cow + sheep genomes): a cow with a fleece. Shear it for wool, milk it with a bucket, and it regrows its
 * wool by eating grass. It can't breed, like in the original.
 */
public class WoolyCowEntity extends Sheep {
    public WoolyCowEntity(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Sheep.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.21);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET) && !isBaby()) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, Items.MILK_BUCKET.getDefaultInstance()));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public Sheep getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    /** Wool by fleece colour (when not sheared), then leather and beef, from madscience:entities/wooly_cow[/colour]. */
    @Override
    public ResourceKey<LootTable> getDefaultLootTable() {
        String path = isSheared() ? "entities/wooly_cow" : "entities/wooly_cow/" + getColor().getName();
        return ResourceKey.create(Registries.LOOT_TABLE, MadScience.id(path));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WOOLY_COW_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WOOLY_COW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WOOLY_COW_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.WOOLY_COW_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
