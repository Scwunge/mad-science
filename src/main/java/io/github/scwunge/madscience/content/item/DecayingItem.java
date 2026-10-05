package io.github.scwunge.madscience.content.item;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.registry.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * Filled syringes and DNA samples. They slowly go bad outside of a Cryogenic Freezer: every {@code decayDelaySeconds} they
 * gain one point of decay, and once fully decayed a syringe turns into a dirty syringe and a sample into a slimeball.
 */
public class DecayingItem extends TooltipItem implements TintedItem {
    public static final int MAX_DECAY = 10;

    private final Species species;
    private final int primaryColor;
    private final int secondaryColor;
    private final Supplier<? extends net.minecraft.world.item.Item> expiresInto;

    public DecayingItem(Properties properties, Species species, int primaryColor, int secondaryColor,
                        Supplier<? extends net.minecraft.world.item.Item> expiresInto) {
        super(properties.component(ModDataComponents.DECAY.get(), 0));
        this.species = species;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.expiresInto = expiresInto;
    }

    /** The species this bloodwork came from, or null for mutant DNA. */
    public Species species() {
        return species;
    }

    @Override
    public int tint(int layer) {
        return layer == 0 ? primaryColor : layer == 1 ? secondaryColor : -1;
    }

    public static int getDecay(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DECAY, 0);
    }

    public static void setDecay(ItemStack stack, int decay) {
        stack.set(ModDataComponents.DECAY, Mth.clamp(decay, 0, MAX_DECAY));
    }

    private static boolean isDecayTick(Level level) {
        return MadConfig.DECAY_BLOODWORK.get()
                && level.getGameTime() % (MadConfig.DECAY_DELAY_SECONDS.get() * 20L) == 0L;
    }

    /**
     * Advances decay by one step.
     *
     * @return the stack to put in its place: the same stack one point further gone, or the expired item once fully decayed
     */
    public ItemStack decayStep(ItemStack stack) {
        int decay = getDecay(stack);
        if (decay >= MAX_DECAY) {
            return new ItemStack(expiresInto.get(), stack.getCount());
        }
        setDecay(stack, decay + 1);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !isDecayTick(level)) {
            return;
        }
        ItemStack result = decayStep(stack);
        if (result != stack && entity instanceof Player player && player.getInventory().getItem(slot) == stack) {
            player.getInventory().setItem(slot, result);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide && isDecayTick(level)) {
            entity.setItem(decayStep(stack.copy()));
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDecay(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - getDecay(stack) * 13.0F / MAX_DECAY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float health = 1.0F - getDecay(stack) / (float) MAX_DECAY;
        return Mth.hsvToRgb(health / 3.0F, 1.0F, 1.0F);
    }
}
