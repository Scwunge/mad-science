package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity;
import io.github.scwunge.madscience.content.machine.magloader.MagazineLoaderBlockEntity;
import io.github.scwunge.madscience.content.recipe.CncRecipe;
import io.github.scwunge.madscience.content.weapon.MagazineItem;
import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.scwunge.madscience.gametest.MachineTests.assertSlot;
import static io.github.scwunge.madscience.gametest.MachineTests.placeTall;

@GameTestHolder(MadScience.MODID)
@PrefixGameTestTemplate(false)
public final class WeaponMachineTests {
    private static final BlockPos REDSTONE = new BlockPos(1, 1, 0);

    private WeaponMachineTests() {
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 400)
    public static void magazineLoaderLoads95Rounds(GameTestHelper helper) {
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        MagazineLoaderBlockEntity loader = placeTall(helper, ModBlocks.MAGAZINE_LOADER.get());
        loader.items().setStackInSlot(MagazineLoaderBlockEntity.STORAGE, new ItemStack(ModItems.ROUND.get(), 64));
        loader.items().setStackInSlot(MagazineLoaderBlockEntity.STORAGE + 5, new ItemStack(ModItems.ROUND.get(), 36));
        loader.items().setStackInSlot(MagazineLoaderBlockEntity.INPUT, new ItemStack(ModItems.MAGAZINE.get(), 2));
        helper.assertTrue(!loader.items().insertItem(MagazineLoaderBlockEntity.INPUT, PulseRifleItem.magazine(5), true).isEmpty(),
                "accepted a magazine that already has rounds");
        helper.succeedWhen(() -> {
            ItemStack out = loader.items().getStackInSlot(MagazineLoaderBlockEntity.OUTPUT);
            helper.assertTrue(out.is(ModItems.MAGAZINE.get()) && MagazineItem.rounds(out) == MagazineLoaderBlockEntity.LOAD_ROUNDS,
                    "no loaded magazine yet: " + out);
            helper.assertTrue(loader.storedRounds() == 5, "used the wrong number of rounds: " + loader.storedRounds() + " left");
            assertSlot(helper, loader, MagazineLoaderBlockEntity.INPUT, new ItemStack(ModItems.MAGAZINE.get(), 1));
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 300)
    public static void magazineLoaderWaitsForAFullLoad(GameTestHelper helper) {
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        MagazineLoaderBlockEntity loader = placeTall(helper, ModBlocks.MAGAZINE_LOADER.get());
        loader.items().setStackInSlot(MagazineLoaderBlockEntity.STORAGE, new ItemStack(ModItems.ROUND.get(), 64));
        loader.items().setStackInSlot(MagazineLoaderBlockEntity.STORAGE + 1, new ItemStack(ModItems.ROUND.get(), 30));
        loader.items().setStackInSlot(MagazineLoaderBlockEntity.INPUT, new ItemStack(ModItems.MAGAZINE.get()));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(loader.items().getStackInSlot(MagazineLoaderBlockEntity.OUTPUT).isEmpty(), "loaded with only 94 rounds");
            helper.succeed();
        });
    }

    static ItemStack book(String page) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Schematic"), "Scientist", 0,
                List.of(Filterable.passThrough(Component.literal(page))), true));
        return book;
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 1200)
    public static void cncMachineCutsPartFromBinaryBook(GameTestHelper helper) {
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        CncMachineBlockEntity cnc = placeTall(helper, ModBlocks.CNC_MACHINE.get());
        cnc.tank().fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        cnc.items().setStackInSlot(CncMachineBlockEntity.IRON, new ItemStack(Items.IRON_BLOCK, 2));
        // the original's way: the part's name in binary ASCII
        cnc.items().setStackInSlot(CncMachineBlockEntity.BOOK, book(CncRecipe.toBinary("pulse rifle barrel")));
        helper.succeedWhen(() -> {
            assertSlot(helper, cnc, CncMachineBlockEntity.OUTPUT, new ItemStack(ModItems.RIFLE_BARREL.get()));
            assertSlot(helper, cnc, CncMachineBlockEntity.IRON, new ItemStack(Items.IRON_BLOCK, 1));
            helper.assertTrue(cnc.items().getStackInSlot(CncMachineBlockEntity.BOOK).is(Items.WRITTEN_BOOK), "the book should be kept");
            helper.assertTrue(cnc.tank().getFluidAmount() < 2000, "cutting should use water");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 100)
    public static void cncMachineRejectsUnknownBook(GameTestHelper helper) {
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        CncMachineBlockEntity cnc = placeTall(helper, ModBlocks.CNC_MACHINE.get());
        cnc.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        cnc.items().setStackInSlot(CncMachineBlockEntity.IRON, new ItemStack(Items.IRON_BLOCK));
        cnc.items().setStackInSlot(CncMachineBlockEntity.BOOK, book("a banana"));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(cnc.status() == CncMachineBlockEntity.Status.INVALID_BOOK, "status was " + cnc.status());
            helper.assertTrue(cnc.progress() == 0, "started cutting from a book with no recipe");
            helper.succeed();
        });
    }

    /** A book made with /give or /data (pages are JSON text, as for any written book) reads the same as one written in game. */
    @GameTest(template = ItemTests.EMPTY)
    public static void cncReadsCommandMadeBooks(GameTestHelper helper) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var tag = net.minecraft.nbt.TagParser.parseTag(
                "{id:\"minecraft:written_book\",count:1,components:{\"minecraft:written_book_content\":{title:\"Bolt\",author:\"Test\",pages:['\"pulse rifle bolt\"']}}}");
        ItemStack book = ItemStack.parseOptional(helper.getLevel().registryAccess(), tag);
        String page = CncMachineBlockEntity.firstPage(book);
        helper.assertTrue(CncRecipe.decode(page).equals("pulse rifle bolt"), "page read as [" + page + "]");
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void cncBooksReadPlainTextOrBinary(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (String page : List.of("Pulse Rifle Bolt", "  pulse rifle bolt\n", CncRecipe.toBinary("pulse rifle bolt"),
                "01110000 01110101 01101100 01110011 01100101 00100000 01110010 01101001 01100110 01101100 01100101 00100000 01100010 01101111 01101100 01110100")) {
            var recipe = manager.getRecipeFor(ModRecipes.CNC.get(), new CncRecipe.Input(page), helper.getLevel());
            helper.assertTrue(recipe.isPresent() && recipe.get().value().result().is(ModItems.RIFLE_BOLT.get()), "no bolt for \"" + page + "\"");
        }
        helper.succeed();
    }
}
