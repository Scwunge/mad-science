package io.github.scwunge.madscience.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Function;

/**
 * One JEI category per machine: its inputs (the recipe's own, then what else the machine needs), an arrow, the results,
 * and how long it takes.
 */
final class MachineCategory<R extends Recipe<?>> extends AbstractRecipeCategory<RecipeHolder<R>> {
    /** What a recipe shows. {@code inputs} are slots of alternatives; {@code ticks} 0 hides the time. */
    record Shown(List<Ingredient> inputs, List<ItemStack> outputs, int ticks, List<Component> notes) {
    }

    private static final int SLOT = 18;
    static final int WIDTH = 160;

    private final Function<R, Shown> layout;

    MachineCategory(IGuiHelper gui, RecipeType<RecipeHolder<R>> type, Component title, ItemLike icon, Function<R, Shown> layout) {
        super(type, title, gui.createDrawableItemLike(icon), WIDTH, 56);
        this.layout = layout;
    }

    private int arrowX(Shown shown) {
        return shown.inputs().size() * SLOT + 4;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<R> recipe, IFocusGroup focuses) {
        Shown shown = layout.apply(recipe.value());
        for (int i = 0; i < shown.inputs().size(); i++) {
            builder.addInputSlot(1 + i * SLOT, 5).setStandardSlotBackground().addIngredients(shown.inputs().get(i));
        }
        int outputX = arrowX(shown) + 30;
        for (int i = 0; i < shown.outputs().size(); i++) {
            builder.addOutputSlot(outputX + i * SLOT, 5).setOutputSlotBackground().addItemStack(shown.outputs().get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<R> recipe, IFocusGroup focuses) {
        Shown shown = layout.apply(recipe.value());
        if (shown.ticks() > 0) {
            builder.addAnimatedRecipeArrow(Math.max(1, shown.ticks())).setPosition(arrowX(shown), 5);
        } else {
            builder.addRecipeArrow().setPosition(arrowX(shown), 5);
        }
        int y = 27;
        if (shown.ticks() > 0) {
            builder.addText(Component.translatable("jei.madscience.time", String.format("%.1f", shown.ticks() / 20.0F)), WIDTH - 2, 9)
                    .setPosition(1, y).setColor(0xFF808080);
            y += 10;
        }
        for (Component note : shown.notes()) {
            // the text widget doesn't wrap, so split the note into lines that fit
            List<FormattedText> lines = Minecraft.getInstance().font.getSplitter().splitLines(note, WIDTH - 2, Style.EMPTY);
            builder.addText(lines, WIDTH - 2, lines.size() * 10).setPosition(1, y).setColor(0xFF808080);
            y += lines.size() * 10;
        }
    }
}
