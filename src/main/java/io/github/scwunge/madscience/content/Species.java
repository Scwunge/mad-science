package io.github.scwunge.madscience.content;

import io.github.scwunge.madscience.MadScience;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;

/**
 * The creatures whose DNA can be collected and sequenced. Colours are the original two-tone tints used on the syringe,
 * DNA sample and genome reel icons.
 */
public enum Species implements StringRepresentable {
    BAT(0x4C3E30, 0x0F0F0F, true, true),
    CAVE_SPIDER(0x0C424E, 0xA80E0E, true, true),
    CHICKEN(0xA1A1A1, 0xFF0000, true, true),
    COW(0x443626, 0xA1A1A1, true, true),
    CREEPER(0x0DA70B, 0x000000, true, true),
    ENDERMAN(0x161616, 0x000000, true, true),
    GHAST(0xF9F9F9, 0xBCBCBC, false, true),
    HORSE(0xC09E7D, 0xEEE500, true, true),
    MUSHROOM_COW(0xA00F10, 0xB7B7B7, true, true),
    OCELOT(0xEFDE7D, 0x564434, true, true),
    PIG(0xF0A5A2, 0xDB635F, true, true),
    /** Zombie pigman is a mutant: its blood is mutant DNA, and its genome is made by merging zombie and pig genomes. */
    PIG_ZOMBIE(0xEA9393, 0x4C7129, false, false),
    SHEEP(0xE7E7E7, 0xFFB5B5, true, true),
    SKELETON(0xC1C1C1, 0x494949, false, true),
    SLIME(0x51A03E, 0x7EBF6E, false, true),
    SPIDER(0x342D27, 0xA80E0E, true, true),
    SQUID(0x223B4D, 0x708899, true, true),
    VILLAGER(0x563C33, 0xBD8B72, true, true),
    WITCH(0x340000, 0x51A03E, true, true),
    WOLF(0xD7D3D3, 0xCEAF96, true, true),
    ZOMBIE(0x00AFAF, 0x799C65, true, true);

    private final int primaryColor;
    private final int secondaryColor;
    private final boolean hasSyringe;
    private final boolean hasSample;
    private final TagKey<EntityType<?>> sourceTag;

    Species(int primaryColor, int secondaryColor, boolean hasSyringe, boolean hasSample) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.hasSyringe = hasSyringe;
        this.hasSample = hasSample;
        this.sourceTag = TagKey.create(Registries.ENTITY_TYPE, MadScience.id("dna_source/" + getSerializedName()));
    }

    public int primaryColor() {
        return primaryColor;
    }

    public int secondaryColor() {
        return secondaryColor;
    }

    /** Whether a filled syringe exists for this species (drawn from a living mob). */
    public boolean hasSyringe() {
        return hasSyringe;
    }

    /** Whether a DNA sample item exists for this species. */
    public boolean hasSample() {
        return hasSample;
    }

    /** Entity types an empty syringe draws this species' blood from. */
    public TagKey<EntityType<?>> sourceTag() {
        return sourceTag;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
