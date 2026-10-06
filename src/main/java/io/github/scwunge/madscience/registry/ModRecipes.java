package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.recipe.MergingRecipe;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MadScience.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MadScience.MODID);

    /** DNA Extractor: syringe or item → DNA sample (+ dirty syringe). "time" is added to the decay-based base time. */
    public static final Processing DNA_EXTRACTING = processing("dna_extracting", 0);
    /** Syringe Sanitizer: dirty syringe → clean syringe. */
    public static final Processing SANITIZING = processing("sanitizing", 200);
    /** Gene Sequencer: DNA sample → genome (started fresh, then repaired one point per sample). */
    public static final Processing SEQUENCING = processing("sequencing", 200);
    /** Genome Incubator: complete genome → spawn egg (or a creature block, like the meat cube). */
    public static final Processing INCUBATING = processing("incubating", 2600);
    /** Thermosonic Bonder: component → upgraded component, with a gold nugget. */
    public static final Processing BONDING = processing("bonding", 2600);
    /** Clay Furnace: ore block → metal block over a long smoulder. */
    public static final Processing CLAY_SMELTING = processing("clay_smelting", 0);

    /** Computer Mainframe: two genomes → combined genome. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MergingRecipe>> MERGING =
            TYPES.register("genome_merging", () -> RecipeType.simple(MadScience.id("genome_merging")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MergingRecipe>> MERGING_SERIALIZER =
            SERIALIZERS.register("genome_merging", MergingRecipe.Serializer::new);

    private ModRecipes() {
    }

    /** A recipe type plus its serializer for {@link ProcessingRecipe}. */
    public static final class Processing {
        public final DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>> type;
        public final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingRecipe>> serializer;

        @SuppressWarnings({"unchecked", "rawtypes"})
        Processing(String name, int defaultTime) {
            this.type = TYPES.register(name, () -> RecipeType.simple(MadScience.id(name)));
            DeferredHolder[] self = new DeferredHolder[1];
            this.serializer = SERIALIZERS.register(name, () -> new ProcessingRecipe.Serializer(
                    () -> (RecipeType) type.get(), () -> (RecipeSerializer) self[0].get(), defaultTime));
            self[0] = serializer;
        }

        public Optional<RecipeHolder<ProcessingRecipe>> find(Level level, ItemStack input) {
            if (input.isEmpty()) {
                return Optional.empty();
            }
            RecipeManager manager = level.getRecipeManager();
            return manager.getRecipeFor(type.get(), new SingleRecipeInput(input), level);
        }
    }

    private static Processing processing(String name, int defaultTime) {
        return new Processing(name, defaultTime);
    }
}
