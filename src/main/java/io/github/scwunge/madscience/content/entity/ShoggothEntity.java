package io.github.scwunge.madscience.content.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Shoggoth (slime + squid genomes): a dark slime that happily swims. Feed it slimeballs to make it grow (up to size 3);
 * it spills ink as it splits.
 */
public class ShoggothEntity extends Slime {
    private static final int MAX_FED_SIZE = 3;

    public ShoggothEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SLIME_BALL) && !player.getAbilities().instabuild) {
            if (!level().isClientSide) {
                stack.shrink(1);
                if (getSize() < MAX_FED_SIZE) {
                    setSize(getSize() + 1, true);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && getSize() > 1 && isDeadOrDying()) {
            int pieces = 2 + random.nextInt(3);
            for (int i = 0; i < pieces; i++) {
                spawnAtLocation(new ItemStack(Items.INK_SAC));
            }
        }
        super.remove(reason);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F * getSize();
    }
}
