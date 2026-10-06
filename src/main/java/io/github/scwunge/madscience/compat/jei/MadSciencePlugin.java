package io.github.scwunge.madscience.compat.jei;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity;
import io.github.scwunge.madscience.content.machine.magloader.MagazineLoaderBlockEntity;
import io.github.scwunge.madscience.content.recipe.CncRecipe;
import io.github.scwunge.madscience.content.recipe.MergingRecipe;
import io.github.scwunge.madscience.content.recipe.ProcessingRecipe;
import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** JEI: a category for each machine, with the machine as its catalyst, and info pages for what has no recipe. */
@JeiPlugin
public class MadSciencePlugin implements IModPlugin {
    private static final RecipeType<RecipeHolder<ProcessingRecipe>> DNA_EXTRACTING = type("dna_extracting");
    private static final RecipeType<RecipeHolder<ProcessingRecipe>> SANITIZING = type("sanitizing");
    private static final RecipeType<RecipeHolder<ProcessingRecipe>> SEQUENCING = type("sequencing");
    private static final RecipeType<RecipeHolder<ProcessingRecipe>> INCUBATING = type("incubating");
    private static final RecipeType<RecipeHolder<ProcessingRecipe>> BONDING = type("bonding");
    private static final RecipeType<RecipeHolder<ProcessingRecipe>> CLAY_SMELTING = type("clay_smelting");
    private static final RecipeType<RecipeHolder<MergingRecipe>> MERGING = type("genome_merging");
    private static final RecipeType<RecipeHolder<CncRecipe>> CNC = type("cnc_machining");

    private static <R extends Recipe<?>> RecipeType<RecipeHolder<R>> type(String name) {
        return RecipeType.createRecipeHolderType(MadScience.id(name));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return MadScience.id("jei");
    }

    private static Ingredient of(ItemLike... items) {
        return Ingredient.of(items);
    }

    private static List<ItemStack> outputs(ProcessingRecipe recipe) {
        return recipe.remainder().isEmpty() ? List.of(recipe.result()) : List.of(recipe.result(), recipe.remainder());
    }

    private static MachineCategory.Shown processing(ProcessingRecipe recipe, List<Ingredient> extras, int ticks, Component... notes) {
        List<Ingredient> inputs = new ArrayList<>();
        inputs.add(recipe.input());
        inputs.addAll(extras);
        return new MachineCategory.Shown(inputs, outputs(recipe), ticks, List.of(notes));
    }

    /** A written book whose first page holds a CnC code, for showing in JEI. */
    static ItemStack cncBook(String code) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(code), "Mad Science", 0,
                List.of(Filterable.passThrough(Component.literal(code))), true));
        return book;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        Ingredient water = of(Items.WATER_BUCKET);
        Ingredient reel = of(ModItems.EMPTY_DATA_REEL.get());
        registration.addRecipeCategories(
                new MachineCategory<>(gui, DNA_EXTRACTING, title(ModBlocks.DNA_EXTRACTOR.get()), ModBlocks.DNA_EXTRACTOR.get(),
                        r -> processing(r, List.of(), 0, Component.translatable("jei.madscience.dna_extracting"))),
                new MachineCategory<>(gui, SANITIZING, title(ModBlocks.SANITIZER.get()), ModBlocks.SANITIZER.get(),
                        r -> processing(r, List.of(water), r.time())),
                new MachineCategory<>(gui, SEQUENCING, title(ModBlocks.SEQUENCER.get()), ModBlocks.SEQUENCER.get(),
                        r -> processing(r, List.of(reel), r.time(), Component.translatable("jei.madscience.sequencing"))),
                new MachineCategory<>(gui, INCUBATING, title(ModBlocks.INCUBATOR.get()), ModBlocks.INCUBATOR.get(),
                        r -> processing(r, List.of(of(Items.EGG)), r.time(), Component.translatable("jei.madscience.needs_redstone_heat"))),
                new MachineCategory<>(gui, BONDING, title(ModBlocks.BONDER.get()), ModBlocks.BONDER.get(),
                        r -> processing(r, List.of(of(Items.GOLD_NUGGET)), r.time(), Component.translatable("jei.madscience.needs_redstone_heat"))),
                new MachineCategory<>(gui, CLAY_SMELTING, title(ModBlocks.CLAY_FURNACE.get()), ModBlocks.CLAY_FURNACE.get(),
                        r -> processing(r, List.of(of(Items.COAL_BLOCK)), clayFurnaceTicks(),
                                Component.translatable("jei.madscience.clay_furnace"))),
                new MachineCategory<>(gui, MERGING, title(ModBlocks.MAINFRAME.get()), ModBlocks.MAINFRAME.get(),
                        r -> new MachineCategory.Shown(List.of(r.first(), r.second(), reel, water), List.of(r.result()), r.time(),
                                List.of(Component.translatable("jei.madscience.needs_redstone")))),
                new MachineCategory<>(gui, CNC, title(ModBlocks.CNC_MACHINE.get()), ModBlocks.CNC_MACHINE.get(),
                        r -> new MachineCategory.Shown(List.of(Ingredient.of(cncBook(r.code())), of(Items.IRON_BLOCK), water),
                                List.of(r.result()), CncMachineBlockEntity.CUT_TIME,
                                List.of(Component.translatable("jei.madscience.cnc_code", r.code())))));
    }

    /** The server's setting when it has been synced, else the default. */
    private static int clayFurnaceTicks() {
        try {
            return MadConfig.CLAY_FURNACE_SECONDS.get() * 20;
        } catch (IllegalStateException notLoaded) {
            return MadConfig.CLAY_FURNACE_SECONDS.getDefault() * 20;
        }
    }

    private static Component title(ItemLike block) {
        return new ItemStack(block).getHoverName();
    }

    private static <I extends RecipeInput, R extends Recipe<I>> void add(IRecipeRegistration registration, RecipeManager manager, RecipeType<RecipeHolder<R>> jeiType,
                                                  Supplier<net.minecraft.world.item.crafting.RecipeType<R>> type) {
        registration.addRecipes(jeiType, manager.getAllRecipesFor(type.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
        add(registration, manager, DNA_EXTRACTING, ModRecipes.DNA_EXTRACTING.type);
        add(registration, manager, SANITIZING, ModRecipes.SANITIZING.type);
        add(registration, manager, SEQUENCING, ModRecipes.SEQUENCING.type);
        add(registration, manager, INCUBATING, ModRecipes.INCUBATING.type);
        add(registration, manager, BONDING, ModRecipes.BONDING.type);
        add(registration, manager, CLAY_SMELTING, ModRecipes.CLAY_SMELTING.type);
        add(registration, manager, MERGING, ModRecipes.MERGING);
        add(registration, manager, CNC, ModRecipes.CNC);

        // things that have no recipe to show
        info(registration, ModItems.PULSE_RIFLE.get(), "pulse_rifle");
        info(registration, ModItems.MAGAZINE.get(), "magazine", MagazineLoaderBlockEntity.LOAD_ROUNDS);
        info(registration, ModBlocks.MAGAZINE_LOADER.get(), "magazine_loader", MagazineLoaderBlockEntity.LOAD_ROUNDS);
        info(registration, ModBlocks.CNC_MACHINE.get(), "cnc_machine");
        info(registration, ModBlocks.CRYOTUBE.get(), "cryotube");
        info(registration, ModBlocks.SONICLOCATOR.get(), "soniclocator");
        info(registration, ModBlocks.MEAT_CUBE.get(), "meat_cube");
        info(registration, ModBlocks.VOX_BOX.get(), "vox_box");
        info(registration, ModBlocks.CLAY_FURNACE.get(), "clay_furnace");
        info(registration, ModBlocks.FREEZER.get(), "freezer");
        info(registration, ModBlocks.DUPLICATOR.get(), "duplicator");
        info(registration, ModItems.EMPTY_SYRINGE.get(), "syringe");
        info(registration, ModItems.WARNING_SIGN.get(), "warning_sign");
        info(registration, ModBlocks.ABOMINATION_EGG.get(), "abomination_egg");
        registration.addIngredientInfo(List.of(PulseRifleItem.magazine(0), new ItemStack(ModItems.ROUND.get())), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.madscience.info.magazine_crafting", 99));
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key, Object... args) {
        registration.addIngredientInfo(item, Component.translatable("jei.madscience.info." + key, args));
    }

    /** The running JEI, for the dev harness's screenshots. */
    static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /** Opens JEI's page of recipes making {@code stack}, or its uses. For the dev harness. */
    public static boolean show(ItemStack stack, boolean uses) {
        if (runtime == null) {
            return false;
        }
        var focuses = runtime.getJeiHelpers().getFocusFactory();
        if (uses) {
            // what JEI's own "uses" key asks for: recipes taking it, and categories it is the machine for
            runtime.getRecipesGui().show(List.of(focuses.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack),
                    focuses.createFocus(RecipeIngredientRole.CATALYST, VanillaTypes.ITEM_STACK, stack)));
        } else {
            runtime.getRecipesGui().show(focuses.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }
        return true;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.DNA_EXTRACTOR.get(), DNA_EXTRACTING);
        registration.addRecipeCatalyst(ModBlocks.SANITIZER.get(), SANITIZING);
        registration.addRecipeCatalyst(ModBlocks.SEQUENCER.get(), SEQUENCING);
        registration.addRecipeCatalyst(ModBlocks.INCUBATOR.get(), INCUBATING);
        registration.addRecipeCatalyst(ModBlocks.BONDER.get(), BONDING);
        registration.addRecipeCatalyst(ModBlocks.CLAY_FURNACE.get(), CLAY_SMELTING);
        registration.addRecipeCatalyst(ModBlocks.MAINFRAME.get(), MERGING);
        registration.addRecipeCatalyst(ModBlocks.CNC_MACHINE.get(), CNC);
    }
}
