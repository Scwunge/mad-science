package io.github.scwunge.madscience.content.item;

/** A plain item drawn from the shared two-layer reel/syringe textures with fixed colours. */
public class TwoToneItem extends TooltipItem implements TintedItem {
    private final int primaryColor;
    private final int secondaryColor;

    public TwoToneItem(Properties properties, int primaryColor, int secondaryColor) {
        super(properties);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    @Override
    public int tint(int layer) {
        return layer == 0 ? primaryColor : layer == 1 ? secondaryColor : -1;
    }
}
