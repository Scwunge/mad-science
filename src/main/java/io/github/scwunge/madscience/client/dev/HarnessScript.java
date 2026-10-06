package io.github.scwunge.madscience.client.dev;

import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.content.machine.MachineBlock;
import io.github.scwunge.madscience.content.machine.MachineBlockEntity;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import io.github.scwunge.madscience.content.machine.dnaextractor.DnaExtractorBlockEntity;
import io.github.scwunge.madscience.content.machine.sanitizer.SanitizerBlockEntity;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModCreativeTabs;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Method;
import java.util.List;

import static io.github.scwunge.madscience.client.dev.DevHarness.add;
import static io.github.scwunge.madscience.client.dev.DevHarness.check;
import static io.github.scwunge.madscience.client.dev.DevHarness.level;
import static io.github.scwunge.madscience.client.dev.DevHarness.player;
import static io.github.scwunge.madscience.client.dev.DevHarness.server;
import static io.github.scwunge.madscience.client.dev.DevHarness.shot;

/** The scripted in-game checks run by {@link DevHarness}. */
final class HarnessScript {
    /** Flat world surface: machines stand on y = -60. */
    static final int Y = -60;

    private HarnessScript() {
    }

    static void build() {
        machineRow();
        guiTour();
        dnaExtractor();
        sanitizer();
        inventory();
    }

    /** Places {@code block} facing {@code facing} (the side towards the viewer) with a full energy buffer. */
    static MachineBlockEntity place(ServerLevel level, BlockPos pos, Block block, Direction facing) {
        level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(MachineBlock.FACING, facing));
        MachineBlockEntity machine = (MachineBlockEntity) level.getBlockEntity(pos);
        if (machine != null && machine.energy() != null) {
            machine.energy().setEnergy(machine.energy().getMaxEnergyStored());
        }
        return machine;
    }

    static void look(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch);
    }

    /** Every machine facing each way, to check models, orientation and textures. */
    /** Every machine block, in placement order. */
    static List<Block> machines() {
        return List.of(ModBlocks.DNA_EXTRACTOR.get(), ModBlocks.SANITIZER.get(), ModBlocks.SEQUENCER.get(), ModBlocks.MAINFRAME.get(),
                ModBlocks.INCUBATOR.get(), ModBlocks.FREEZER.get(), ModBlocks.DUPLICATOR.get(), ModBlocks.BONDER.get(),
                ModBlocks.CLAY_FURNACE.get());
    }

    /** Opens each machine's GUI in turn and takes a screenshot. */
    private static void guiTour() {
        BlockPos pos = new BlockPos(-6, Y, 10);
        for (Block block : machines()) {
            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
            server(30, server -> {
                ServerLevel level = level(server);
                level.removeBlock(pos, false);
                place(level, pos, block, Direction.NORTH);
                look(player(server), pos.getX() + 0.5, Y + 0.6, pos.getZ() - 1.6, 0, 25);
            });
            shot("closeup-" + name);
            server(20, server -> MachineMenu.open(player(server), (MachineBlockEntity) level(server).getBlockEntity(pos)));
            shot("gui-" + name);
            add((mc, server) -> {
                mc.setScreen(null);
                return 2;
            });
        }
        server(2, server -> level(server).removeBlock(pos, false));
    }

    private static void machineRow() {
        List<Block> machines = machines();
        server(80, server -> {
            ServerLevel level = level(server);
            Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int m = 0; m < machines.size(); m++) {
                for (int i = 0; i < facings.length; i++) {
                    place(level, new BlockPos(i * 2, Y, 4 + m * 2), machines.get(m), facings[i]);
                }
            }
            ServerPlayer player = player(server);
            player.setGameMode(GameType.CREATIVE);
            look(player, 3.5, Y + 2.5, 0.5, 0, 30);
        });
        shot("row-facings-n-e-s-w");
        server(40, server -> look(player(server), 3.5, Y + 2.5, 6.5 + machines.size() * 2, 180, 30));
        shot("row-facings-back");
        server(5, server -> {
            ServerLevel level = level(server);
            for (int m = 0; m < machines.size(); m++) {
                for (int i = 0; i < 4; i++) {
                    level.removeBlock(new BlockPos(i * 2, Y, 4 + m * 2), false);
                }
            }
        });
    }

    private static void sanitizer() {
        BlockPos pos = new BlockPos(3, Y, 10);
        server(40, server -> {
            SanitizerBlockEntity sanitizer = (SanitizerBlockEntity) place(level(server), pos, ModBlocks.SANITIZER.get(), Direction.NORTH);
            sanitizer.items().setStackInSlot(SanitizerBlockEntity.WATER_IN, new ItemStack(Items.WATER_BUCKET));
            sanitizer.items().setStackInSlot(SanitizerBlockEntity.DIRTY_IN, new ItemStack(ModItems.DIRTY_SYRINGE.get(), 3));
            look(player(server), 3.5, Y + 1, 7.5, 0, 20);
        });
        shot("sanitizer-working");
        server(30, server -> MachineMenu.open(player(server), (SanitizerBlockEntity) level(server).getBlockEntity(pos)));
        shot("sanitizer-gui");
        add((mc, server) -> {
            mc.setScreen(null);
            return 240;
        });
        server(5, server -> {
            SanitizerBlockEntity sanitizer = (SanitizerBlockEntity) level(server).getBlockEntity(pos);
            check("sanitizer cleaned a syringe", sanitizer.items().getStackInSlot(SanitizerBlockEntity.CLEAN_OUT).is(ModItems.EMPTY_SYRINGE.get()));
            check("sanitizer returned the empty bucket", sanitizer.items().getStackInSlot(SanitizerBlockEntity.BUCKET_OUT).is(Items.BUCKET));
            check("sanitizer used water", sanitizer.tank().getFluidAmount() < 1000 && sanitizer.tank().getFluidAmount() > 0);
        });
    }

    private static void dnaExtractor() {
        BlockPos pos = new BlockPos(0, Y, 10);
        server(40, server -> {
            ServerLevel level = level(server);
            DnaExtractorBlockEntity extractor = (DnaExtractorBlockEntity) place(level, pos, ModBlocks.DNA_EXTRACTOR.get(), Direction.NORTH);
            extractor.items().setStackInSlot(DnaExtractorBlockEntity.INPUT, new ItemStack(ModItems.syringe(Species.COW), 4));
            extractor.items().setStackInSlot(DnaExtractorBlockEntity.BUCKET_IN, new ItemStack(Items.BUCKET));
            look(player(server), 0.5, Y + 1, 7.5, 0, 20);
        });
        shot("dna-extractor-working");
        server(30, server -> MachineMenu.open(player(server), (DnaExtractorBlockEntity) level(server).getBlockEntity(pos)));
        shot("dna-extractor-gui");
        add((mc, server) -> {
            mc.setScreen(null);
            return 200;
        });
        server(5, server -> {
            DnaExtractorBlockEntity extractor = (DnaExtractorBlockEntity) level(server).getBlockEntity(pos);
            check("DNA extractor produced cow DNA", extractor.items().getStackInSlot(DnaExtractorBlockEntity.SAMPLE_OUT).is(ModItems.sample(Species.COW)));
            check("DNA extractor left dirty syringes", extractor.items().getStackInSlot(DnaExtractorBlockEntity.DIRTY_OUT).is(ModItems.DIRTY_SYRINGE.get()));
            check("DNA extractor is animating", extractor.isActive());
        });
    }

    /** All mod items in the survival inventory, to check icons, tints and machine item models. */
    private static void inventory() {
        server(10, server -> player(server).setGameMode(GameType.CREATIVE));
        add((mc, server) -> {
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
            mc.setScreen(screen);
            try {
                Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(screen, ModCreativeTabs.MAIN.get());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return 10;
        });
        add((mc, server) -> {
            check("creative tab lists every item", ModCreativeTabs.MAIN.get().getDisplayItems().size() == ModItems.TAB_ORDER.size());
            return 1;
        });
        int rows = (ModItems.TAB_ORDER.size() + 8) / 9;
        int pages = Math.max(1, (rows - 5 + 4) / 5 + 1);
        for (int page = 0; page < pages; page++) {
            float scroll = pages == 1 ? 0 : page / (float) (pages - 1);
            add((mc, server) -> {
                if (mc.screen instanceof CreativeModeInventoryScreen screen) {
                    screen.getMenu().scrollTo(scroll);
                }
                return 5;
            });
            shot("creative-tab-" + (page + 1));
        }
        add((mc, server) -> {
            mc.setScreen(null);
            return 5;
        });
    }
}
