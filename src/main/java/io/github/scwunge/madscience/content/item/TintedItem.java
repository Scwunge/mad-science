package io.github.scwunge.madscience.content.item;

/** Items drawn from greyscale layers tinted per species, like vanilla spawn eggs. */
public interface TintedItem {
    /** @return ARGB/RGB tint for the given model layer, or -1 for no tint. */
    int tint(int layer);
}
