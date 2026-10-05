package io.github.scwunge.madscience;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config. Defaults are the original 1.6.4 values; energy numbers are the original Universal Electricity values
 * multiplied by {@link #ENERGY_SCALE} when converted to FE.
 */
public final class MadConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ENERGY_SCALE = BUILDER
            .comment("FE per original Universal Electricity unit. Applied to every machine's capacity, input and use.",
                    "The original values are tiny (100k storage, 200/t in, 1/t use), so the default is 10.")
            .defineInRange("energyScale", 10, 1, 1000);

    public static final ModConfigSpec.BooleanValue DECAY_BLOODWORK;
    public static final ModConfigSpec.IntValue DECAY_DELAY_SECONDS;
    public static final ModConfigSpec.IntValue CLAY_FURNACE_SECONDS;
    public static final ModConfigSpec.BooleanValue ABOMINATION_LAYS_EGGS;
    public static final ModConfigSpec.BooleanValue ABOMINATION_TELEPORTS;
    public static final ModConfigSpec.BooleanValue PULSE_RIFLE_ENABLED;
    public static final ModConfigSpec.BooleanValue BULLETS_DAMAGE_WORLD;

    static {
        BUILDER.push("bloodwork");
        DECAY_BLOODWORK = BUILDER.comment("Filled syringes, DNA samples and unfinished genomes decay over time unless kept cold.")
                .define("decay", true);
        DECAY_DELAY_SECONDS = BUILDER.comment("Seconds between decay steps.")
                .defineInRange("decayDelaySeconds", 30, 1, 3600);
        BUILDER.pop();

        BUILDER.push("machines");
        CLAY_FURNACE_SECONDS = BUILDER.comment("How long the Clay Furnace smoulders before the ore is done.")
                .defineInRange("clayFurnaceSeconds", 420, 1, 36000);
        BUILDER.pop();

        BUILDER.push("mobs");
        ABOMINATION_LAYS_EGGS = BUILDER.comment("The Abomination lays an egg when it kills a mob (needs mobGriefing).")
                .define("abominationLaysEggs", true);
        ABOMINATION_TELEPORTS = BUILDER.define("abominationTeleports", true);
        BUILDER.pop();

        BUILDER.push("weapons");
        PULSE_RIFLE_ENABLED = BUILDER.comment("Allow crafting and firing the pulse rifle.")
                .define("pulseRifleEnabled", true);
        BULLETS_DAMAGE_WORLD = BUILDER.comment("Pulse rifle rounds break glass and similar blocks (still respects claims).")
                .define("bulletsDamageWorld", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MadConfig() {
    }

    /** Converts an original UE energy amount to FE using {@link #ENERGY_SCALE}. */
    public static int fe(long original) {
        long scaled = original * (SPEC.isLoaded() ? ENERGY_SCALE.get() : ENERGY_SCALE.getDefault());
        return (int) Math.min(Integer.MAX_VALUE, scaled);
    }
}
