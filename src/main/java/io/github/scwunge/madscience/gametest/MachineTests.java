package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.dnaextractor.DnaExtractorBlockEntity;
import io.github.scwunge.madscience.content.Gmo;
import io.github.scwunge.madscience.content.machine.incubator.IncubatorBlockEntity;
import io.github.scwunge.madscience.content.machine.mainframe.MainframeBlockEntity;
import io.github.scwunge.madscience.content.machine.sanitizer.SanitizerBlockEntity;
import io.github.scwunge.madscience.content.machine.sequencer.SequencerBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MadScience.MODID)
@PrefixGameTestTemplate(false)
public final class MachineTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private MachineTests() {
    }

    /** Places a machine and fills its energy buffer. */
    @SuppressWarnings("unchecked")
    static <T extends MachineBlockEntity> T place(GameTestHelper helper, Block block) {
        helper.setBlock(POS, block);
        T machine = (T) helper.getBlockEntity(POS);
        if (machine.energy() != null) {
            machine.energy().setEnergy(machine.energy().getMaxEnergyStored());
        }
        return machine;
    }

    static void assertSlot(GameTestHelper helper, MachineBlockEntity machine, int slot, ItemStack expected) {
        ItemStack actual = machine.items().getStackInSlot(slot);
        helper.assertTrue(ItemStack.isSameItem(actual, expected) && actual.getCount() == expected.getCount(),
                "slot " + slot + ": expected " + expected + " but found " + actual);
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 400)
    public static void dnaExtractorExtractsSyringe(GameTestHelper helper) {
        DnaExtractorBlockEntity extractor = place(helper, ModBlocks.DNA_EXTRACTOR.get());
        extractor.items().setStackInSlot(DnaExtractorBlockEntity.INPUT, new ItemStack(ModItems.syringe(Species.COW)));
        helper.succeedWhen(() -> {
            assertSlot(helper, extractor, DnaExtractorBlockEntity.SAMPLE_OUT, new ItemStack(ModItems.sample(Species.COW)));
            assertSlot(helper, extractor, DnaExtractorBlockEntity.DIRTY_OUT, new ItemStack(ModItems.DIRTY_SYRINGE.get()));
            helper.assertTrue(extractor.items().getStackInSlot(DnaExtractorBlockEntity.INPUT).isEmpty(), "input not consumed");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 400)
    public static void dnaExtractorTakesItems(GameTestHelper helper) {
        DnaExtractorBlockEntity extractor = place(helper, ModBlocks.DNA_EXTRACTOR.get());
        extractor.items().setStackInSlot(DnaExtractorBlockEntity.INPUT, new ItemStack(Items.FEATHER));
        helper.succeedWhen(() -> {
            assertSlot(helper, extractor, DnaExtractorBlockEntity.SAMPLE_OUT, new ItemStack(ModItems.sample(Species.CHICKEN)));
            helper.assertTrue(extractor.items().getStackInSlot(DnaExtractorBlockEntity.DIRTY_OUT).isEmpty(), "a feather should not leave a dirty syringe");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 1200)
    public static void dnaExtractorFillsMutantBucket(GameTestHelper helper) {
        DnaExtractorBlockEntity extractor = place(helper, ModBlocks.DNA_EXTRACTOR.get());
        extractor.items().setStackInSlot(DnaExtractorBlockEntity.INPUT, new ItemStack(ModItems.MUTANT_SYRINGE.get()));
        extractor.items().setStackInSlot(DnaExtractorBlockEntity.BUCKET_IN, new ItemStack(Items.BUCKET));
        helper.succeedWhen(() -> assertSlot(helper, extractor, DnaExtractorBlockEntity.BUCKET_OUT, new ItemStack(ModFluids.MUTANT_DNA.bucket.get())));
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 400)
    public static void sanitizerCleansSyringe(GameTestHelper helper) {
        SanitizerBlockEntity sanitizer = place(helper, ModBlocks.SANITIZER.get());
        sanitizer.items().setStackInSlot(SanitizerBlockEntity.WATER_IN, new ItemStack(Items.WATER_BUCKET));
        sanitizer.items().setStackInSlot(SanitizerBlockEntity.DIRTY_IN, new ItemStack(ModItems.DIRTY_SYRINGE.get()));
        helper.succeedWhen(() -> {
            assertSlot(helper, sanitizer, SanitizerBlockEntity.CLEAN_OUT, new ItemStack(ModItems.EMPTY_SYRINGE.get()));
            assertSlot(helper, sanitizer, SanitizerBlockEntity.BUCKET_OUT, new ItemStack(Items.BUCKET));
            helper.assertTrue(sanitizer.tank().getFluidAmount() == 800, "200 ticks should use 200 mB, tank has " + sanitizer.tank().getFluidAmount());
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 300)
    public static void sanitizerNeedsWater(GameTestHelper helper) {
        SanitizerBlockEntity sanitizer = place(helper, ModBlocks.SANITIZER.get());
        sanitizer.items().setStackInSlot(SanitizerBlockEntity.DIRTY_IN, new ItemStack(ModItems.DIRTY_SYRINGE.get()));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(sanitizer.items().getStackInSlot(SanitizerBlockEntity.CLEAN_OUT).isEmpty(), "cleaned without water");
            helper.succeed();
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 800)
    public static void sequencerStartsAndRepairsGenome(GameTestHelper helper) {
        SequencerBlockEntity sequencer = place(helper, ModBlocks.SEQUENCER.get());
        sequencer.items().setStackInSlot(SequencerBlockEntity.SAMPLE_IN, new ItemStack(ModItems.sample(Species.PIG), 2));
        sequencer.items().setStackInSlot(SequencerBlockEntity.REEL_IN, new ItemStack(ModItems.EMPTY_DATA_REEL.get()));
        helper.succeedWhen(() -> {
            ItemStack out = sequencer.items().getStackInSlot(SequencerBlockEntity.OUTPUT);
            helper.assertTrue(out.is(ModItems.genome(Species.PIG)), "no pig genome yet");
            helper.assertTrue(out.getDamageValue() == out.getMaxDamage(), "a new genome should start fully unfinished");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 800)
    public static void sequencerCompletesGenome(GameTestHelper helper) {
        SequencerBlockEntity sequencer = place(helper, ModBlocks.SEQUENCER.get());
        ItemStack almostDone = new ItemStack(ModItems.genome(Species.PIG));
        almostDone.setDamageValue(1);
        sequencer.items().setStackInSlot(SequencerBlockEntity.REEL_IN, almostDone);
        sequencer.items().setStackInSlot(SequencerBlockEntity.SAMPLE_IN, new ItemStack(ModItems.sample(Species.PIG)));
        helper.succeedWhen(() -> {
            ItemStack out = sequencer.items().getStackInSlot(SequencerBlockEntity.OUTPUT);
            helper.assertTrue(out.is(ModItems.genome(Species.PIG)) && !out.isDamaged(), "genome not completed");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 8000)
    public static void mainframeMergesGenomes(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        MainframeBlockEntity mainframe = place(helper, ModBlocks.MAINFRAME.get());
        mainframe.tank().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        mainframe.items().setStackInSlot(MainframeBlockEntity.GENOME_A, new ItemStack(ModItems.genome(Species.VILLAGER)));
        mainframe.items().setStackInSlot(MainframeBlockEntity.GENOME_B, new ItemStack(ModItems.genome(Species.WOLF)));
        mainframe.items().setStackInSlot(MainframeBlockEntity.REEL_IN, new ItemStack(ModItems.EMPTY_DATA_REEL.get()));
        helper.succeedWhen(() -> {
            assertSlot(helper, mainframe, MainframeBlockEntity.OUTPUT, new ItemStack(ModItems.combinedGenome(Gmo.WEREWOLF)));
            helper.assertTrue(!mainframe.items().getStackInSlot(MainframeBlockEntity.GENOME_A).isEmpty(), "input genomes should be kept");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 300)
    public static void mainframeNeedsRedstone(GameTestHelper helper) {
        MainframeBlockEntity mainframe = place(helper, ModBlocks.MAINFRAME.get());
        mainframe.tank().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        mainframe.items().setStackInSlot(MainframeBlockEntity.GENOME_A, new ItemStack(ModItems.genome(Species.VILLAGER)));
        mainframe.items().setStackInSlot(MainframeBlockEntity.GENOME_B, new ItemStack(ModItems.genome(Species.WOLF)));
        mainframe.items().setStackInSlot(MainframeBlockEntity.REEL_IN, new ItemStack(ModItems.EMPTY_DATA_REEL.get()));
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(mainframe.state() == MainframeBlockEntity.State.OFF, "mainframe ran without a redstone signal");
            helper.succeed();
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 8000)
    public static void incubatorHatchesSpawnEgg(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        IncubatorBlockEntity incubator = place(helper, ModBlocks.INCUBATOR.get());
        incubator.items().setStackInSlot(IncubatorBlockEntity.EGG_IN, new ItemStack(Items.EGG, 2));
        incubator.items().setStackInSlot(IncubatorBlockEntity.GENOME_IN, new ItemStack(ModItems.genome(Species.COW)));
        helper.succeedWhen(() -> assertSlot(helper, incubator, IncubatorBlockEntity.OUTPUT, new ItemStack(Items.COW_SPAWN_EGG)));
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 300)
    public static void incubatorRejectsUnfinishedGenome(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        IncubatorBlockEntity incubator = place(helper, ModBlocks.INCUBATOR.get());
        ItemStack unfinished = new ItemStack(ModItems.genome(Species.COW));
        unfinished.setDamageValue(5);
        incubator.items().setStackInSlot(IncubatorBlockEntity.EGG_IN, new ItemStack(Items.EGG));
        incubator.items().setStackInSlot(IncubatorBlockEntity.GENOME_IN, unfinished);
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(incubator.state() != IncubatorBlockEntity.State.WORKING, "incubated an unfinished genome");
            helper.succeed();
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 300)
    public static void dnaExtractorNeedsPower(GameTestHelper helper) {
        DnaExtractorBlockEntity extractor = place(helper, ModBlocks.DNA_EXTRACTOR.get());
        extractor.energy().setEnergy(0);
        extractor.items().setStackInSlot(DnaExtractorBlockEntity.INPUT, new ItemStack(Items.FEATHER));
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(extractor.items().getStackInSlot(DnaExtractorBlockEntity.SAMPLE_OUT).isEmpty(), "worked without power");
            helper.succeed();
        });
    }
}
