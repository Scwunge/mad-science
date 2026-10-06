package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.weapon.MagazineItem;
import io.github.scwunge.madscience.content.weapon.MagazineRecipes;
import io.github.scwunge.madscience.content.weapon.PulseRifleGrenade;
import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.content.weapon.PulseRifleRound;
import io.github.scwunge.madscience.content.weapon.RifleState;
import io.github.scwunge.madscience.registry.ModDataComponents;
import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(MadScience.MODID)
@PrefixGameTestTemplate(false)
public final class WeaponTests {
    private WeaponTests() {
    }

    private static ItemStack rifle(RifleState state) {
        ItemStack rifle = new ItemStack(ModItems.PULSE_RIFLE.get());
        rifle.set(ModDataComponents.RIFLE.get(), state);
        return rifle;
    }

    private static int rounds(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(PulseRifleRound.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64)).size();
    }

    /** A server player standing in the test, aiming up and away from it. */
    @SuppressWarnings("removal")
    private static ServerPlayer shooter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.moveTo(at.x, at.y, at.z, 0, -60);
        return player;
    }

    /** Fires, runs dry, and stops altogether when the server switches the rifle off. */
    @GameTest(template = ItemTests.EMPTY)
    public static void rifleFiresRoundsUntilEmpty(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        ItemStack rifle = rifle(new RifleState(2, 0, false, true, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        int before = rounds(helper);
        helper.assertTrue(PulseRifleItem.triggerTick(player, rifle, true), "first pull did not fire");
        helper.assertTrue(PulseRifleItem.triggerTick(player, rifle, true), "second pull did not fire");
        helper.assertTrue(!PulseRifleItem.triggerTick(player, rifle, true), "fired with an empty magazine");
        helper.assertTrue(PulseRifleItem.state(rifle).rounds() == 0, "rounds were not used up");
        helper.assertTrue(rounds(helper) - before >= 2, "no round entities were launched");

        rifle.set(ModDataComponents.RIFLE.get(), new RifleState(5, 0, false, true, false));
        MadConfig.PULSE_RIFLE_ENABLED.set(false);
        try {
            helper.assertTrue(!PulseRifleItem.triggerTick(player, rifle, true), "fired while disabled in the config");
            helper.assertTrue(PulseRifleItem.state(rifle).rounds() == 5, "disabled rifle used a round");
        } finally {
            MadConfig.PULSE_RIFLE_ENABLED.set(true);
        }
        helper.succeed();
    }

    /** Grenade mode launches one grenade per pull, not one per tick held. */
    @GameTest(template = ItemTests.EMPTY)
    public static void grenadeLauncherFiresOncePerPull(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        ItemStack rifle = rifle(new RifleState(0, 3, true, false, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        helper.assertTrue(PulseRifleItem.triggerTick(player, rifle, true), "grenade did not launch");
        helper.assertTrue(!PulseRifleItem.triggerTick(player, rifle, false), "launched again while the trigger was held");
        helper.assertTrue(PulseRifleItem.state(rifle).grenades() == 2, "grenade count wrong");
        helper.succeed();
    }

    /** Use switches mode; sneak-use loads the fullest magazine, unloads it again, and loads up to four grenades. */
    @GameTest(template = ItemTests.EMPTY)
    public static void rifleReloadsAndUnloads(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack rifle = rifle(RifleState.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        player.getInventory().add(PulseRifleItem.magazine(10));
        player.getInventory().add(PulseRifleItem.magazine(60));
        player.getInventory().add(new ItemStack(ModItems.GRENADE.get(), 6));

        player.setShiftKeyDown(true);
        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PulseRifleItem.state(rifle).rounds() == 60, "did not load the fullest magazine");
        helper.assertTrue(countMagazines(player, 60) == 0 && countMagazines(player, 10) == 1, "wrong magazine taken");

        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PulseRifleItem.state(rifle).rounds() == 0 && !PulseRifleItem.state(rifle).magazineInserted(), "did not unload");
        helper.assertTrue(countMagazines(player, 60) == 1, "unloaded magazine did not keep its rounds");

        player.setShiftKeyDown(false);
        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PulseRifleItem.state(rifle).grenadeMode(), "use did not switch to the grenade launcher");
        player.setShiftKeyDown(true);
        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PulseRifleItem.state(rifle).grenades() == RifleState.MAX_GRENADES, "did not load four grenades");
        helper.assertTrue(player.getInventory().countItem(ModItems.GRENADE.get()) == 2, "took the wrong number of grenades");
        helper.succeed();
    }

    private static int countMagazines(Player player, int rounds) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.MAGAZINE.get()) && MagazineItem.rounds(stack) == rounds) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static CraftingInput grid(ItemStack... items) {
        List<ItemStack> list = new ArrayList<>(List.of(items));
        while (list.size() < 9) {
            list.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, list);
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void magazineLoadsAndUnloadsInTheGrid(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var load = new MagazineRecipes.Load(CraftingBookCategory.MISC);
        var unload = new MagazineRecipes.Unload(CraftingBookCategory.MISC);
        ItemStack round = new ItemStack(ModItems.ROUND.get());

        CraftingInput loading = grid(PulseRifleItem.magazine(97), round, round);
        helper.assertTrue(load.matches(loading, helper.getLevel()), "magazine + rounds did not match");
        helper.assertTrue(MagazineItem.rounds(load.assemble(loading, registries)) == 99, "loaded magazine has the wrong count");
        helper.assertTrue(!load.matches(grid(PulseRifleItem.magazine(98), round, round), helper.getLevel()), "overfilled a magazine");

        CraftingInput unloading = grid(PulseRifleItem.magazine(99));
        helper.assertTrue(unload.matches(unloading, helper.getLevel()), "loaded magazine did not unload");
        helper.assertTrue(unload.assemble(unloading, registries).getCount() == 64, "unloaded more than a stack of rounds");
        NonNullList<ItemStack> left = unload.getRemainingItems(unloading);
        helper.assertTrue(MagazineItem.rounds(left.get(0)) == 35, "magazine did not keep the rounds that did not fit");
        helper.assertTrue(!unload.matches(grid(PulseRifleItem.magazine(0)), helper.getLevel()), "an empty magazine unloaded");
        helper.succeed();
    }

    /**
     * A grenade breaks blocks by default, and leaves them alone with bulletsDamageWorld off. Both halves run in one test
     * because the config is shared.
     */
    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 200)
    public static void grenadeWorldDamageFollowsConfig(GameTestHelper helper) {
        PulseRifleGrenade[] grenade = new PulseRifleGrenade[1];
        int[] dirtAfterBlast = new int[1];
        helper.startSequence()
                .thenExecute(() -> {
                    dirtFloor(helper);
                    grenade[0] = dropGrenade(helper);
                })
                .thenWaitUntil(() -> helper.assertTrue(grenade[0].isRemoved(), "grenade still flying"))
                .thenExecute(() -> {
                    dirtAfterBlast[0] = dirtCount(helper);
                    helper.assertTrue(dirtAfterBlast[0] < 9, "grenade broke no blocks with world damage on");
                    dirtFloor(helper);
                    MadConfig.BULLETS_DAMAGE_WORLD.set(false);
                    grenade[0] = dropGrenade(helper);
                })
                .thenWaitUntil(() -> helper.assertTrue(grenade[0].isRemoved(), "grenade still flying"))
                .thenExecute(() -> {
                    MadConfig.BULLETS_DAMAGE_WORLD.set(true);
                    helper.assertTrue(dirtCount(helper) == 9, "grenade broke blocks with world damage off");
                })
                .thenSucceed();
    }

    private static void dirtFloor(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(x, 1, z, Blocks.DIRT);
                helper.setBlock(x, 2, z, Blocks.AIR);
            }
        }
    }

    private static int dirtCount(GameTestHelper helper) {
        int count = 0;
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                count += helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.DIRT) ? 1 : 0;
            }
        }
        return count;
    }

    private static PulseRifleGrenade dropGrenade(GameTestHelper helper) {
        PulseRifleGrenade grenade = new PulseRifleGrenade(ModEntities.PULSE_RIFLE_GRENADE.get(), helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
        grenade.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        grenade.setDeltaMovement(0, -0.8, 0);
        helper.getLevel().addFreshEntity(grenade);
        return grenade;
    }
}
