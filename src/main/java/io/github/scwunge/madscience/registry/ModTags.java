package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Every genome reel, sequenced or combined: what the Mainframe and Incubator accept. */
    public static final TagKey<Item> GENOMES = TagKey.create(Registries.ITEM, MadScience.id("genomes"));
    /** Filled syringes and DNA samples: what the Cryogenic Freezer keeps fresh. */
    public static final TagKey<Item> BLOODWORK = TagKey.create(Registries.ITEM, MadScience.id("bloodwork"));

    private ModTags() {
    }
}
