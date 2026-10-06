package io.github.scwunge.madscience.content.weapon;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModRecipes;
import io.github.scwunge.madscience.registry.ModSounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Crafting-grid magazine handling, like the original: a magazine plus rounds loads them in (up to 99), and a loaded
 * magazine on its own unloads into rounds, leaving the emptied magazine in the grid. A stack of rounds holds 64, so a
 * fuller magazine is unloaded in two goes.
 */
@EventBusSubscriber(modid = MadScience.MODID)
public final class MagazineRecipes {
    private MagazineRecipes() {
    }

    @SubscribeEvent
    static void crafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (event.getCrafting().is(ModItems.MAGAZINE.get())) {
            player.level().playSound(null, player.blockPosition(), ModSounds.PULSE_RIFLE_MAGAZINE_RELOAD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (event.getCrafting().is(ModItems.ROUND.get())
                && event.getInventory() instanceof CraftingContainer grid && Grid.of(grid.asCraftInput()) != null) {
            player.level().playSound(null, player.blockPosition(), ModSounds.PULSE_RIFLE_MAGAZINE_UNLOAD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Finds the single magazine in the grid and counts the rounds beside it; anything else spoils the recipe. */
    private record Grid(int magazineSlot, ItemStack magazine, int rounds) {
        static Grid of(CraftingInput input) {
            int slot = -1, rounds = 0;
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.is(ModItems.MAGAZINE.get()) && slot < 0) {
                    slot = i;
                } else if (stack.is(ModItems.ROUND.get())) {
                    rounds++;
                } else {
                    return null;
                }
            }
            return slot < 0 ? null : new Grid(slot, input.getItem(slot), rounds);
        }
    }

    public static class Load extends CustomRecipe {
        public Load(CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            Grid grid = Grid.of(input);
            return grid != null && grid.rounds() > 0 && MagazineItem.rounds(grid.magazine()) + grid.rounds() <= RifleState.MAX_ROUNDS;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            Grid grid = Grid.of(input);
            return grid == null ? ItemStack.EMPTY : PulseRifleItem.magazine(MagazineItem.rounds(grid.magazine()) + grid.rounds());
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ModRecipes.MAGAZINE_LOAD.get();
        }
    }

    public static class Unload extends CustomRecipe {
        public Unload(CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            Grid grid = Grid.of(input);
            return grid != null && grid.rounds() == 0 && MagazineItem.rounds(grid.magazine()) > 0;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            Grid grid = Grid.of(input);
            if (grid == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(ModItems.ROUND.get(), Math.min(MagazineItem.rounds(grid.magazine()), ModItems.ROUND.get().getDefaultMaxStackSize()));
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            Grid grid = Grid.of(input);
            if (grid != null) {
                int left = MagazineItem.rounds(grid.magazine()) - Math.min(MagazineItem.rounds(grid.magazine()), ModItems.ROUND.get().getDefaultMaxStackSize());
                remaining.set(grid.magazineSlot(), PulseRifleItem.magazine(left));
            }
            return remaining;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 1;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ModRecipes.MAGAZINE_UNLOAD.get();
        }
    }
}
