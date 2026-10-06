package io.github.scwunge.madscience.client.dev;

import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.content.weapon.RifleState;
import io.github.scwunge.madscience.content.weapon.RifleTrigger;
import io.github.scwunge.madscience.registry.ModDataComponents;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.content.machine.MachineMenu;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

import static io.github.scwunge.madscience.client.dev.DevHarness.add;
import static io.github.scwunge.madscience.client.dev.DevHarness.check;
import static io.github.scwunge.madscience.client.dev.DevHarness.level;
import static io.github.scwunge.madscience.client.dev.DevHarness.player;
import static io.github.scwunge.madscience.client.dev.DevHarness.server;
import static io.github.scwunge.madscience.client.dev.DevHarness.shot;
import static io.github.scwunge.madscience.client.dev.HarnessScript.Y;
import static io.github.scwunge.madscience.client.dev.HarnessScript.look;

/** The pulse rifle in first and third person, firing, in an item frame and on the ground. */
final class WeaponHarness {
    private WeaponHarness() {
    }

    static void rifle() {
        server(20, server -> {
            var player = player(server);
            look(player, 40.5, Y, 0.5, 0, 0);
            ItemStack rifle = new ItemStack(ModItems.PULSE_RIFLE.get());
            rifle.set(ModDataComponents.RIFLE.get(), new RifleState(42, 3, false, true, false));
            player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        });
        shot("rifle-first-person");
        server(7, server -> RifleTrigger.setHeld(player(server), true));
        shot("rifle-firing");
        add((mc, server) -> {
            server.execute(() -> {
                var player = player(server);
                int rounds = PulseRifleItem.state(player.getMainHandItem()).rounds();
                check("pulse rifle fired rounds (" + rounds + " left)", rounds < 42);
                RifleTrigger.setHeld(player, false);
                check("pulse rifle stops firing on release", !PulseRifleItem.state(player.getMainHandItem()).firing());
            });
            return 10;
        });
        add((mc, server) -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            return 10;
        });
        shot("rifle-third-person-front");
        add((mc, server) -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            return 10;
        });
        shot("rifle-third-person-back");
        add((mc, server) -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            return 5;
        });

        // every weapon item dropped on the ground, and the rifle in an item frame
        List<DeferredItem<? extends Item>> items = List.of(ModItems.PULSE_RIFLE, ModItems.MAGAZINE, ModItems.ROUND, ModItems.GRENADE,
                ModItems.RIFLE_BARREL, ModItems.RIFLE_BOLT, ModItems.RIFLE_RECEIVER, ModItems.RIFLE_TRIGGER, ModItems.BULLET_CASING,
                ModItems.GRENADE_CASING);
        server(40, server -> {
            server.setDifficulty(Difficulty.NORMAL, true);
            var level = level(server);
            for (int i = 0; i < items.size(); i++) {
                ItemEntity drop = new ItemEntity(level, 36.5 + i, Y, 6.5, new ItemStack(items.get(i).get()), 0, 0, 0);
                drop.setNeverPickUp();
                drop.setUnlimitedLifetime();
                level.addFreshEntity(drop);
            }
            BlockPos wall = new BlockPos(43, Y + 1, 9);
            level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            ItemFrame frame = new ItemFrame(level, wall.north(), Direction.NORTH);
            frame.setItem(new ItemStack(ModItems.PULSE_RIFLE.get()));
            level.addFreshEntity(frame);
            // side view of the rifle held up, as a player holds it (a husk's arms are raised the same way)
            Husk husk = new Husk(EntityType.HUSK, level);
            husk.moveTo(39.5, Y, 7.5, -90, 0);
            husk.setYHeadRot(-90);
            husk.setYBodyRot(-90);
            husk.setNoAi(true);
            husk.setPersistenceRequired();
            husk.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PULSE_RIFLE.get()));
            level.addFreshEntity(husk);
            player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            look(player(server), 41, Y + 1.2, 3, 0, 25);
        });
        shot("weapons-on-ground");
    }

    /** The Magazine Loader and CnC Machine at work, their GUIs, and their items in the hotbar. */
    static void machines() {
        BlockPos loaderPos = new BlockPos(52, Y, 6);
        BlockPos cncPos = new BlockPos(55, Y, 6);
        server(20, server -> {
            var level = level(server);
            var player = player(server);
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(30, Y - 2, 0, 60, Y + 6, 12),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
            for (BlockPos pos : List.of(loaderPos, cncPos)) {
                level.setBlockAndUpdate(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
            var loaderBlock = io.github.scwunge.madscience.registry.ModBlocks.MAGAZINE_LOADER.get();
            var loader = (io.github.scwunge.madscience.content.machine.magloader.MagazineLoaderBlockEntity) HarnessScript.place(level, loaderPos, loaderBlock, Direction.NORTH);
            loaderBlock.setPlacedBy(level, loaderPos, level.getBlockState(loaderPos), player, ItemStack.EMPTY);
            loader.items().setStackInSlot(2, new ItemStack(ModItems.ROUND.get(), 64));
            loader.items().setStackInSlot(3, new ItemStack(ModItems.ROUND.get(), 64));
            loader.items().setStackInSlot(0, new ItemStack(ModItems.MAGAZINE.get(), 4));
            var cncBlock = io.github.scwunge.madscience.registry.ModBlocks.CNC_MACHINE.get();
            var cnc = (io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity) HarnessScript.place(level, cncPos, cncBlock, Direction.NORTH);
            cncBlock.setPlacedBy(level, cncPos, level.getBlockState(cncPos), player, ItemStack.EMPTY);
            cnc.tank().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            cnc.items().setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.IRON_BLOCK, 4));
            ItemStack book = new ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
            book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT, new net.minecraft.world.item.component.WrittenBookContent(
                    net.minecraft.server.network.Filterable.passThrough("Barrel"), "Scientist", 0,
                    List.of(net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal(
                            io.github.scwunge.madscience.content.recipe.CncRecipe.toBinary("pulse rifle barrel")))), true));
            cnc.items().setStackInSlot(2, book);
            player.getInventory().setItem(0, new ItemStack(loaderBlock));
            player.getInventory().setItem(1, new ItemStack(cncBlock));
            player.getInventory().selected = 0;
            look(player, 53.5, Y + 0.8, 3.2, 0, 15);
        });
        shot("loader-cnc-start");
        server(60, server -> look(player(server), cncPos.getX() + 0.5, Y + 1.1, cncPos.getZ() - 1.4, 0, 30));
        shot("cnc-pressing");
        add((mc, server) -> 400);
        shot("cnc-water");
        add((mc, server) -> {
            var cnc = (io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity) mc.level.getBlockEntity(cncPos);
            check("client sees the CnC cutting (" + cnc.status() + ", progress " + cnc.progress() + ", iron " + cnc.hasIron() + ")",
                    cnc.status() == io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity.Status.WORKING && cnc.progress() > 0);
            return 1;
        });
        server(5, server -> {
            var cnc = (io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity) level(server).getBlockEntity(cncPos);
            check("CnC machine is cutting (" + cnc.status() + ", step " + cnc.stage() + ")",
                    cnc.status() == io.github.scwunge.madscience.content.machine.cnc.CncMachineBlockEntity.Status.WORKING);
            look(player(server), loaderPos.getX() + 0.5, Y + 1.1, loaderPos.getZ() - 1.4, 0, 30);
        });
        shot("loader-closeup");
        server(20, server -> MachineMenu.open(player(server), (io.github.scwunge.madscience.content.machine.MachineBlockEntity) level(server).getBlockEntity(loaderPos)));
        shot("gui-magazine_loader");
        add((mc, server) -> {
            mc.player.closeContainer();
            return 5;
        });
        server(20, server -> MachineMenu.open(player(server), (io.github.scwunge.madscience.content.machine.MachineBlockEntity) level(server).getBlockEntity(cncPos)));
        shot("gui-cnc_machine");
        add((mc, server) -> {
            mc.player.closeContainer();
            return 5;
        });
    }

    /** A wall of warning signs, and the lab coat on an armour stand and on the player. */
    static void lab() {
        server(30, server -> {
            var level = level(server);
            var player = player(server);
            var types = io.github.scwunge.madscience.content.sign.WarningSignType.values();
            for (int i = 0; i < 9; i++) {
                for (int row = 0; row < 3; row++) {
                    BlockPos wall = new BlockPos(60 + i, Y + row, 12);
                    level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                    int index = row * 9 + i;
                    if (index < types.length) {
                        var sign = new io.github.scwunge.madscience.content.sign.WarningSignEntity(level, wall.north(), Direction.NORTH, player);
                        sign.setSignType(types[index]);
                        level.addFreshEntity(sign);
                    }
                }
            }
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, 58.5, Y, 9.5);
            stand.setYRot(180);
            stand.setYBodyRot(180);
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.LAB_COAT_GOGGLES.get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.LAB_COAT_BODY.get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.LAB_COAT_LEGGINGS.get()));
            level.addFreshEntity(stand);
            look(player, 62, Y + 1.6, 6, 0, 8);
        });
        shot("warning-signs");
        server(10, server -> {
            var player = player(server);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.LAB_COAT_GOGGLES.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.LAB_COAT_BODY.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.LAB_COAT_LEGGINGS.get()));
            look(player, 55.5, Y, 5.5, 0, 0);
        });
        add((mc, server) -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            return 10;
        });
        shot("lab-coat");
        add((mc, server) -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            return 5;
        });
    }
}
