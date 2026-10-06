package io.github.scwunge.madscience.content;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Genetically modified organisms: what the Computer Mainframe makes by merging two genomes. Colours are the two parents'
 * spawn egg colours, used on the combined genome reel and on the creature's spawn egg, as in the original.
 */
public enum Gmo implements StringRepresentable {
    WEREWOLF(0x4E362E, 0xC3BFBF),
    MEAT_CUBE(0x499138, 0xCE8E8B),
    CREEPER_COW(0x0C970A, 0x403324),
    ENDERSLIME(0x151515, 0x499138),
    WOOLY_COW(0x403324, 0xC6C6C6),
    SHOGGOTH(0x499138, 0x1F3546),
    ABOMINATION(0x151515, 0x2F2923),
    WITHER_SKELETON(0x151515, 0xA6A6A6),
    ZOMBIE_VILLAGER(0x4E362E, 0x009F9F),
    SKELETON_HORSE(0xAE8F71, 0xA6A6A6),
    ZOMBIE_HORSE(0xAE8F71, 0x009F9F),
    ENDER_SQUID(0x151515, 0x1F3546);

    private final int primaryColor;
    private final int secondaryColor;

    Gmo(int primaryColor, int secondaryColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public int primaryColor() {
        return primaryColor;
    }

    public int secondaryColor() {
        return secondaryColor;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
