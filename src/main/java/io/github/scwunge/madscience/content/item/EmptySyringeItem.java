package io.github.scwunge.madscience.content.item;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Hit a mob with an empty syringe to draw its blood, or use it on yourself to draw your own (which counts as villager
 * DNA). Mobs that are already mutants (zombified piglins by default) give mutant DNA.
 */
public class EmptySyringeItem extends TooltipItem {
    public static final TagKey<EntityType<?>> MUTANT_SOURCES = TagKey.create(Registries.ENTITY_TYPE, MadScience.id("dna_source/mutant"));

    public EmptySyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getAbilities().instabuild) {
            return InteractionResultHolder.pass(stack);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SYRINGE_STAB_PLAYER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!level.isClientSide) {
            player.hurt(level.damageSources().generic(), 2.0F);
            player.causeFoodExhaustion(5.0F);
            give(player, new ItemStack(ModItems.syringe(Species.VILLAGER)));
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        Item filled = filledFor(entity.getType());
        if (filled == null) {
            return false;
        }
        entity.playSound(ModSounds.SYRINGE_STAB_MOB.get(), 1.0F, 1.0F);
        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            give(player, new ItemStack(filled));
        }
        // let the hit go through like the original did
        return false;
    }

    /** The filled syringe an entity type gives, or null if its blood is no use. */
    public static Item filledFor(EntityType<?> type) {
        if (type.is(MUTANT_SOURCES)) {
            return ModItems.MUTANT_SYRINGE.get();
        }
        for (Species species : Species.values()) {
            if (species.hasSyringe() && type.is(species.sourceTag())) {
                return ModItems.syringe(species);
            }
        }
        return null;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
