package io.github.scwunge.madscience.content.item;

import io.github.scwunge.madscience.content.Species;
import net.minecraft.world.item.ItemStack;

/**
 * A sequenced genome on a data reel. Freshly started genomes are fully "damaged" and every DNA sample fed into the Gene
 * Sequencer repairs one point; a genome with no damage left is complete.
 */
public class GenomeItem extends TooltipItem implements TintedItem {
    public static final int MAX_PROGRESS = 63;

    private final Species species;

    public GenomeItem(Properties properties, Species species) {
        super(properties.durability(MAX_PROGRESS));
        this.species = species;
    }

    public Species species() {
        return species;
    }

    public static boolean isComplete(ItemStack stack) {
        return !stack.isDamaged();
    }

    @Override
    public int tint(int layer) {
        return layer == 0 ? species.primaryColor() : layer == 1 ? species.secondaryColor() : -1;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
