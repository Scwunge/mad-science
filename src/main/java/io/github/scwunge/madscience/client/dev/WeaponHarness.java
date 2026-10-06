package io.github.scwunge.madscience.client.dev;

import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.content.weapon.RifleState;
import io.github.scwunge.madscience.content.weapon.RifleTrigger;
import io.github.scwunge.madscience.registry.ModDataComponents;
import io.github.scwunge.madscience.registry.ModItems;
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
}
