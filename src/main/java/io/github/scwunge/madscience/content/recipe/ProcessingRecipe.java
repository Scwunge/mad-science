package io.github.scwunge.madscience.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * One input item turned into a result (plus an optional leftover, like the dirty syringe from the DNA Extractor) over
 * some ticks. Several machines use this shape; each has its own recipe type so packs can edit them separately.
 */
public class ProcessingRecipe implements Recipe<SingleRecipeInput> {
    private final Supplier<RecipeType<?>> type;
    private final Supplier<RecipeSerializer<?>> serializer;
    final Ingredient input;
    final ItemStack result;
    final ItemStack remainder;
    final int time;

    public ProcessingRecipe(Supplier<RecipeType<?>> type, Supplier<RecipeSerializer<?>> serializer,
                            Ingredient input, ItemStack result, ItemStack remainder, int time) {
        this.type = type;
        this.serializer = serializer;
        this.input = input;
        this.result = result;
        this.remainder = remainder;
        this.time = time;
    }

    public Ingredient input() {
        return input;
    }

    public ItemStack result() {
        return result;
    }

    /** Extra item given back per operation (empty if none). */
    public ItemStack remainder() {
        return remainder;
    }

    /** Ticks per operation, or extra ticks for machines whose base time depends on the input. */
    public int time() {
        return time;
    }

    @Override
    public boolean matches(SingleRecipeInput in, Level level) {
        return input.test(in.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
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
        return NonNullList.of(Ingredient.EMPTY, input);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return type.get();
    }

    /** Serializer bound to one recipe type; {@code defaultTime} is used when the JSON leaves out "time". */
    public static class Serializer implements RecipeSerializer<ProcessingRecipe> {
        private final MapCodec<ProcessingRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec;

        public Serializer(Supplier<RecipeType<?>> type, Supplier<RecipeSerializer<?>> self, int defaultTime) {
            this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ProcessingRecipe::input),
                    ItemStack.CODEC.fieldOf("result").forGetter(ProcessingRecipe::result),
                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("remainder", ItemStack.EMPTY).forGetter(ProcessingRecipe::remainder),
                    Codec.INT.optionalFieldOf("time", defaultTime).forGetter(ProcessingRecipe::time)
            ).apply(i, (in, out, rem, time) -> new ProcessingRecipe(type, self, in, out, rem, time)));
            this.streamCodec = StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, ProcessingRecipe::input,
                    ItemStack.STREAM_CODEC, ProcessingRecipe::result,
                    ItemStack.OPTIONAL_STREAM_CODEC, ProcessingRecipe::remainder,
                    ByteBufCodecs.VAR_INT, ProcessingRecipe::time,
                    (in, out, rem, time) -> new ProcessingRecipe(type, self, in, out, rem, time));
        }

        @Override
        public MapCodec<ProcessingRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
