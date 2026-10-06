package io.github.scwunge.madscience.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.scwunge.madscience.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * CnC Machine: cuts a block of iron into {@code result} when the written book in it names {@code code} on its first page,
 * either in plain text or, as the original wanted, in binary ASCII ("01110000 01110101..." with or without spaces).
 */
public record CncRecipe(String code, ItemStack result) implements Recipe<CncRecipe.Input> {
    public record Input(String text) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        /** The recipe manager skips empty inputs; a book is only empty if its page is. */
        @Override
        public boolean isEmpty() {
            return text.isBlank();
        }
    }

    public static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT).trim();
    }

    /** The original's encoding: each byte as eight binary digits, run together. */
    public static String toBinary(String text) {
        StringBuilder binary = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.US_ASCII)) {
            for (int i = 7; i >= 0; i--) {
                binary.append((b >> i) & 1);
            }
        }
        return binary.toString();
    }

    /** Decodes binary ASCII back to text, or returns null if {@code text} isn't binary. */
    public static String fromBinary(String text) {
        String bits = text.replaceAll("\\s+", "");
        if (bits.isEmpty() || bits.length() % 8 != 0 || !bits.matches("[01]+")) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bits.length(); i += 8) {
            out.append((char) Integer.parseInt(bits.substring(i, i + 8), 2));
        }
        return out.toString();
    }

    /** What a page says, decoded from binary if it is binary. */
    public static String decode(String page) {
        String text = normalise(page);
        String decoded = fromBinary(text);
        return decoded == null ? text : normalise(decoded);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return decode(input.text()).equals(normalise(code));
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
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CNC_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CNC.get();
    }

    public static class Serializer implements RecipeSerializer<CncRecipe> {
        private static final MapCodec<CncRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("code").forGetter(CncRecipe::code),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CncRecipe::result)
        ).apply(i, CncRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, CncRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, CncRecipe::code,
                ItemStack.STREAM_CODEC, CncRecipe::result,
                CncRecipe::new);

        @Override
        public MapCodec<CncRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CncRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
