package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Gmo;
import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.content.item.DecayingItem;
import io.github.scwunge.madscience.content.item.EmptySyringeItem;
import io.github.scwunge.madscience.content.item.GenomeItem;
import io.github.scwunge.madscience.content.item.MemoryReelItem;
import io.github.scwunge.madscience.content.item.TooltipItem;
import io.github.scwunge.madscience.content.item.TwoToneItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(MadScience.MODID);

    /** Every item in creative-tab order. */
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    // components
    public static final DeferredItem<Item> CASE = simple("component_case");
    public static final DeferredItem<Item> CPU = simple("component_cpu");
    public static final DeferredItem<Item> FAN = simple("component_fan");
    public static final DeferredItem<Item> FUSED_QUARTZ = simple("component_fused_quartz");
    public static final DeferredItem<Item> MAGNETIC_TAPE = simple("component_magnetic_tape");
    public static final DeferredItem<Item> POWER_SUPPLY = simple("component_power_supply");
    public static final DeferredItem<Item> RAM = simple("component_ram");
    public static final DeferredItem<Item> SCREEN = simple("component_screen");
    public static final DeferredItem<Item> SILICON_WAFER = simple("component_silicon_wafer");
    public static final DeferredItem<Item> TRANSISTOR = simple("component_transistor");
    public static final DeferredItem<Item> COMPUTER = simple("component_computer");
    public static final DeferredItem<Item> THUMPER = simple("component_thumper");
    public static final DeferredItem<Item> ENDERSLIME = simple("component_enderslime");

    // circuits
    public static final DeferredItem<Item> CIRCUIT_COMPARATOR = simple("circuit_comparator");
    public static final DeferredItem<Item> CIRCUIT_DIAMOND = simple("circuit_diamond");
    public static final DeferredItem<Item> CIRCUIT_EMERALD = simple("circuit_emerald");
    public static final DeferredItem<Item> CIRCUIT_ENDER_EYE = simple("circuit_ender_eye");
    public static final DeferredItem<Item> CIRCUIT_ENDER_PEARL = simple("circuit_ender_pearl");
    public static final DeferredItem<Item> CIRCUIT_GLOWSTONE = simple("circuit_glowstone");
    public static final DeferredItem<Item> CIRCUIT_REDSTONE = simple("circuit_redstone");
    public static final DeferredItem<Item> CIRCUIT_SPIDER_EYE = simple("circuit_spider_eye");

    // bloodwork
    public static final DeferredItem<Item> EMPTY_SYRINGE = add(REGISTER.register("syringe_empty",
            () -> new EmptySyringeItem(new Item.Properties())));
    public static final DeferredItem<Item> DIRTY_SYRINGE = simple("syringe_dirty");
    public static final DeferredItem<DecayingItem> MUTANT_SYRINGE = add(REGISTER.register("syringe_mutant",
            () -> new DecayingItem(new Item.Properties(), null, 0x51A03E, 0x7EBF6E, ModItems.DIRTY_SYRINGE)));
    private static final Map<Species, DeferredItem<DecayingItem>> SYRINGES = new EnumMap<>(Species.class);
    private static final Map<Species, DeferredItem<DecayingItem>> SAMPLES = new EnumMap<>(Species.class);
    private static final Map<Species, DeferredItem<GenomeItem>> GENOMES = new EnumMap<>(Species.class);

    static {
        for (Species species : Species.values()) {
            if (species.hasSyringe()) {
                SYRINGES.put(species, add(REGISTER.register("syringe_" + species.getSerializedName(),
                        () -> new DecayingItem(new Item.Properties(), species, species.primaryColor(), species.secondaryColor(), ModItems.DIRTY_SYRINGE))));
            }
        }
        for (Species species : Species.values()) {
            if (species.hasSample()) {
                SAMPLES.put(species, add(REGISTER.register("dna_" + species.getSerializedName(),
                        () -> new DecayingItem(new Item.Properties(), species, species.primaryColor(), species.secondaryColor(), () -> Items.SLIME_BALL))));
            }
        }
    }

    public static final DeferredItem<Item> EMPTY_DATA_REEL = add(REGISTER.register("data_reel_empty",
            () -> new TwoToneItem(new Item.Properties(), 0x35A5C8, 0x35A5C8)));

    private static final Map<Gmo, DeferredItem<TwoToneItem>> COMBINED_GENOMES = new EnumMap<>(Gmo.class);

    static {
        for (Species species : Species.values()) {
            GENOMES.put(species, add(REGISTER.register("genome_" + species.getSerializedName(),
                    () -> new GenomeItem(new Item.Properties(), species))));
        }
        for (Gmo gmo : Gmo.values()) {
            COMBINED_GENOMES.put(gmo, add(REGISTER.register("genome_" + gmo.getSerializedName(),
                    () -> new TwoToneItem(new Item.Properties().stacksTo(1), gmo.primaryColor(), gmo.secondaryColor()))));
        }
    }

    private static final Map<MemoryReelItem.Memory, DeferredItem<MemoryReelItem>> MEMORIES = new EnumMap<>(MemoryReelItem.Memory.class);

    static {
        for (MemoryReelItem.Memory memory : MemoryReelItem.Memory.values()) {
            MEMORIES.put(memory, add(REGISTER.register(memory.id(), () -> new MemoryReelItem(new Item.Properties(), memory))));
        }
    }

    public static MemoryReelItem memory(MemoryReelItem.Memory memory) {
        return MEMORIES.get(memory).get();
    }

    public static TwoToneItem combinedGenome(Gmo gmo) {
        return COMBINED_GENOMES.get(gmo).get();
    }

    private ModItems() {
    }

    public static DecayingItem syringe(Species species) {
        return SYRINGES.get(species).get();
    }

    public static DecayingItem sample(Species species) {
        return SAMPLES.get(species).get();
    }

    public static GenomeItem genome(Species species) {
        return GENOMES.get(species).get();
    }

    public static Map<Species, DeferredItem<DecayingItem>> syringes() {
        return SYRINGES;
    }

    public static Map<Species, DeferredItem<DecayingItem>> samples() {
        return SAMPLES;
    }

    public static Map<Species, DeferredItem<GenomeItem>> genomes() {
        return GENOMES;
    }

    private static DeferredItem<Item> simple(String name) {
        return add(REGISTER.register(name, () -> new TooltipItem(new Item.Properties())));
    }

    private static <T extends Item> DeferredItem<T> add(DeferredItem<T> item) {
        TAB_ORDER.add(item);
        return item;
    }
}
