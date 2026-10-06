package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.dnaextractor.DnaExtractorBlockEntity;
import io.github.scwunge.madscience.content.Gmo;
import io.github.scwunge.madscience.content.item.DecayingItem;
import io.github.scwunge.madscience.content.machine.bonder.BonderBlockEntity;
import io.github.scwunge.madscience.content.item.MemoryReelItem;
import io.github.scwunge.madscience.content.machine.TallMachineBlock;
import io.github.scwunge.madscience.content.machine.clayfurnace.ClayFurnaceBlockEntity;
import io.github.scwunge.madscience.content.machine.cryotube.CryotubeBlockEntity;
import io.github.scwunge.madscience.content.machine.soniclocator.SoniclocatorBlockEntity;
import io.github.scwunge.madscience.content.machine.duplicator.DuplicatorBlockEntity;
import io.github.scwunge.madscience.content.machine.freezer.FreezerBlockEntity;
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

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 400)
    public static void freezerRefreshesBloodwork(GameTestHelper helper) {
        FreezerBlockEntity freezer = place(helper, ModBlocks.FREEZER.get());
        ItemStack old = new ItemStack(ModItems.sample(Species.COW));
        DecayingItem.setDecay(old, 5);
        freezer.items().setStackInSlot(FreezerBlockEntity.STORAGE_START, old);
        freezer.items().setStackInSlot(FreezerBlockEntity.FUEL, new ItemStack(Items.SNOWBALL, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(DecayingItem.getDecay(freezer.items().getStackInSlot(FreezerBlockEntity.STORAGE_START)) == 4, "sample was not chilled");
            assertSlot(helper, freezer, FreezerBlockEntity.FUEL, new ItemStack(Items.SNOWBALL, 1));
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 3000)
    public static void duplicatorCopiesGenome(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        DuplicatorBlockEntity duplicator = place(helper, ModBlocks.DUPLICATOR.get());
        duplicator.items().setStackInSlot(DuplicatorBlockEntity.SOURCE, new ItemStack(ModItems.combinedGenome(Gmo.SHOGGOTH)));
        duplicator.items().setStackInSlot(DuplicatorBlockEntity.BLANK, new ItemStack(ModItems.EMPTY_DATA_REEL.get()));
        helper.succeedWhen(() -> {
            assertSlot(helper, duplicator, DuplicatorBlockEntity.OUTPUT, new ItemStack(ModItems.combinedGenome(Gmo.SHOGGOTH)));
            assertSlot(helper, duplicator, DuplicatorBlockEntity.SOURCE, new ItemStack(ModItems.combinedGenome(Gmo.SHOGGOTH)));
            helper.assertTrue(duplicator.items().getStackInSlot(DuplicatorBlockEntity.BLANK).isEmpty(), "blank reel not used");
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 8000)
    public static void bonderMakesTransistors(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        BonderBlockEntity bonder = place(helper, ModBlocks.BONDER.get());
        bonder.items().setStackInSlot(BonderBlockEntity.GOLD_IN, new ItemStack(Items.GOLD_NUGGET));
        bonder.items().setStackInSlot(BonderBlockEntity.COMPONENT_IN, new ItemStack(ModItems.SILICON_WAFER.get()));
        helper.succeedWhen(() -> assertSlot(helper, bonder, BonderBlockEntity.OUTPUT, new ItemStack(ModItems.TRANSISTOR.get(), 16)));
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 10000)
    public static void clayFurnaceCooksOre(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.CLAY_FURNACE.get());
        ClayFurnaceBlockEntity furnace = (ClayFurnaceBlockEntity) helper.getBlockEntity(POS);
        furnace.items().setStackInSlot(ClayFurnaceBlockEntity.COAL, new ItemStack(Items.COAL_BLOCK));
        furnace.items().setStackInSlot(ClayFurnaceBlockEntity.ORE, new ItemStack(Items.IRON_ORE));
        helper.assertTrue(furnace.light(), "could not light the furnace");
        helper.succeedWhen(() -> {
            if (furnace.phase() == ClayFurnaceBlockEntity.Phase.SMOULDERING) {
                furnace.breakShell();
            }
            helper.assertTrue(furnace.phase() == ClayFurnaceBlockEntity.Phase.COOLED, "still " + furnace.phase());
            helper.assertTrue(furnace.resultBlock() != null && furnace.resultBlock().is(Blocks.IRON_BLOCK), "should give an iron block");
        });
    }

    /** Places a tall machine the way a player would, so its upper parts exist. */
    static <T extends MachineBlockEntity> T placeTall(GameTestHelper helper, TallMachineBlock block) {
        helper.setBlock(POS, block);
        BlockPos abs = helper.absolutePos(POS);
        block.setPlacedBy(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null, ItemStack.EMPTY);
        @SuppressWarnings("unchecked")
        T machine = (T) helper.getBlockEntity(POS);
        if (machine.energy() != null) {
            machine.energy().setEnergy(machine.energy().getMaxEnergyStored());
        }
        return machine;
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void tallMachinesComeApartTogether(GameTestHelper helper) {
        placeTall(helper, ModBlocks.CRYOTUBE.get());
        helper.assertBlockPresent(ModBlocks.CRYOTUBE.get(), POS.above(2));
        helper.setBlock(POS.above(), Blocks.AIR);
        helper.assertBlockNotPresent(ModBlocks.CRYOTUBE.get(), POS);
        helper.assertBlockNotPresent(ModBlocks.CRYOTUBE.get(), POS.above(2));
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 30000)
    public static void cryotubeGrowsSubjectAndRecordsMemory(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        CryotubeBlockEntity tube = placeTall(helper, ModBlocks.CRYOTUBE.get());
        tube.energy().setEnergy(0);
        tube.items().setStackInSlot(CryotubeBlockEntity.EGG_IN, new ItemStack(Items.VILLAGER_SPAWN_EGG, 16));
        tube.items().setStackInSlot(CryotubeBlockEntity.REEL_IN, new ItemStack(ModItems.EMPTY_DATA_REEL.get()));
        tube.items().setStackInSlot(CryotubeBlockEntity.STAR, new ItemStack(Items.NETHER_STAR));
        boolean[] madePower = {false};
        helper.onEachTick(() -> madePower[0] |= tube.energy().getEnergyStored() > 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(tube.items().getStackInSlot(CryotubeBlockEntity.MEMORY_OUT).getItem() instanceof MemoryReelItem, "no memory reel yet");
            helper.assertTrue(madePower[0], "the living subject should have made power");
            helper.assertTrue(!tube.items().getStackInSlot(CryotubeBlockEntity.STAR).isEmpty(), "the nether star is a catalyst, not used up");
        });
    }

    // separate batches: two Soniclocators running at once would blow each other up (the conflict rule)
    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 1200, batch = "soniclocator_a")
    public static void soniclocatorPullsTargetFromChunk(GameTestHelper helper) {
        BlockPos ore = POS.below();
        helper.setBlock(ore, Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        SoniclocatorBlockEntity sonic = placeTall(helper, ModBlocks.SONICLOCATOR.get());
        sonic.items().setStackInSlot(SoniclocatorBlockEntity.GRAVEL_IN, new ItemStack(Items.GRAVEL, 4));
        sonic.items().setStackInSlot(SoniclocatorBlockEntity.TARGET, new ItemStack(Items.IRON_ORE));
        helper.succeedWhen(() -> {
            helper.assertTrue(!sonic.items().getStackInSlot(SoniclocatorBlockEntity.OUTPUT).isEmpty(), "state " + sonic.state() + " charge " + sonic.charge()
                    + " energy " + sonic.energyStored() + " redstone " + sonic.isRedstonePowered() + " removed " + sonic.isRemoved());
            assertSlot(helper, sonic, SoniclocatorBlockEntity.OUTPUT, new ItemStack(Items.IRON_ORE));
            helper.assertBlockPresent(Blocks.GRAVEL, ore);
            assertSlot(helper, sonic, SoniclocatorBlockEntity.GRAVEL_IN, new ItemStack(Items.GRAVEL, 3));
        });
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 1200, batch = "soniclocator_b")
    public static void soniclocatorRespectsProtection(GameTestHelper helper) {
        BlockPos ore = POS.below();
        helper.setBlock(ore, Blocks.GOLD_ORE);
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.REDSTONE_BLOCK);
        SoniclocatorBlockEntity sonic = placeTall(helper, ModBlocks.SONICLOCATOR.get());
        sonic.items().setStackInSlot(SoniclocatorBlockEntity.GRAVEL_IN, new ItemStack(Items.GRAVEL, 4));
        sonic.items().setStackInSlot(SoniclocatorBlockEntity.TARGET, new ItemStack(Items.GOLD_ORE));
        BlockPos absOre = helper.absolutePos(ore);
        // stand-in for a claim mod: nobody may break this block
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> guard = event -> {
            if (event.getPos().equals(absOre)) {
                event.setCanceled(true);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(guard);
        helper.runAfterDelay(MAX_SONIC_WAIT, () -> {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(guard);
            helper.assertBlockPresent(Blocks.GOLD_ORE, ore);
            helper.assertTrue(sonic.items().getStackInSlot(SoniclocatorBlockEntity.OUTPUT).isEmpty(), "took a protected block");
            helper.succeed();
        });
    }

    private static final int MAX_SONIC_WAIT = SoniclocatorBlockEntity.MAX_CHARGE + 60;

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

