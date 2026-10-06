package io.github.scwunge.madscience.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.scwunge.madscience.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Computer Mainframe: two complete genomes (either order) are computed into a combined genome over {@code time} ticks.
 * The two input genomes are not used up; only an empty data reel is.
 */
public record MergingRecipe(Ingredient first, Ingredient second, ItemStack result, int time) implements Recipe<MergingRecipe.Input> {
    public static final MapCodec<MergingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("first").forGetter(MergingRecipe::first),
            Ingredient.CODEC_NONEMPTY.fieldOf("second").forGetter(MergingRecipe::second),
            ItemStack.CODEC.fieldOf("result").forGetter(MergingRecipe::result),
            Codec.INT.optionalFieldOf("time", 2600).forGetter(MergingRecipe::time)
    ).apply(i, MergingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MergingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MergingRecipe::first,
            Ingredient.CONTENTS_STREAM_CODEC, MergingRecipe::second,
            ItemStack.STREAM_CODEC, MergingRecipe::result,
            ByteBufCodecs.VAR_INT, MergingRecipe::time,
            MergingRecipe::new);

    /** The two genomes in the mainframe. */
    public record Input(ItemStack a, ItemStack b) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? a : b;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        // unfinished (damaged) genomes don't count, like the original's exact-damage match
        if (input.a().isDamaged() || input.b().isDamaged()) {
            return false;
        }
        return first.test(input.a()) && second.test(input.b()) || first.test(input.b()) && second.test(input.a());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, first, second);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MERGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MERGING.get();
    }

    public static final class Serializer implements RecipeSerializer<MergingRecipe> {
        @Override
        public MapCodec<MergingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MergingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
