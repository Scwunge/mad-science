package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Gmo;
import io.github.scwunge.madscience.content.block.AbominationEggBlockEntity;
import io.github.scwunge.madscience.content.entity.AbominationEntity;
import io.github.scwunge.madscience.content.entity.CreeperCowEntity;
import io.github.scwunge.madscience.content.entity.ShoggothEntity;
import io.github.scwunge.madscience.content.entity.WoolyCowEntity;
import io.github.scwunge.madscience.registry.ModBlocks;
import io.github.scwunge.madscience.registry.ModEntities;
import io.github.scwunge.madscience.registry.ModFluids;
import io.github.scwunge.madscience.registry.ModItems;
import io.github.scwunge.madscience.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MadScience.MODID)
@PrefixGameTestTemplate(false)
public final class CreatureTests {
    private CreatureTests() {
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void everyCreatureSpawns(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : java.util.List.of(ModEntities.WEREWOLF.get(), ModEntities.CREEPER_COW.get(), ModEntities.ENDERSLIME.get(),
                ModEntities.WOOLY_COW.get(), ModEntities.SHOGGOTH.get(), ModEntities.ABOMINATION.get(), ModEntities.ENDER_SQUID.get())) {
            Mob mob = helper.spawnWithNoFreeWill(type, 1, 2, 1);
            helper.assertTrue(mob.isAlive(), type + " did not spawn");
            mob.discard();
        }
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void creeperCowGivesMutantDna(GameTestHelper helper) {
        CreeperCowEntity cow = helper.spawnWithNoFreeWill(ModEntities.CREEPER_COW.get(), 1, 2, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ModFluids.MUTANT_DNA.bucket.get()), "milking gave " + player.getMainHandItem());
        helper.assertTrue(cow.explosionRadius == 5, "blast radius should be 5");
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void woolyCowMilksAndShears(GameTestHelper helper) {
        WoolyCowEntity cow = helper.spawnWithNoFreeWill(ModEntities.WOOLY_COW.get(), 1, 2, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(Items.MILK_BUCKET), "no milk");
        helper.assertTrue(cow.readyForShearing(), "should be shearable");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(cow.isSheared(), "not sheared");
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void shoggothGrowsOnSlime(GameTestHelper helper) {
        ShoggothEntity shoggoth = helper.spawnWithNoFreeWill(ModEntities.SHOGGOTH.get(), 1, 2, 1);
        shoggoth.setSize(1, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SLIME_BALL, 5));
        for (int i = 0; i < 4; i++) {
            player.interactOn(shoggoth, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(shoggoth.getSize() == 3, "size " + shoggoth.getSize() + " instead of the cap of 3");
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void abominationLaysEggOnKill(GameTestHelper helper) {
        AbominationEntity abomination = helper.spawnWithNoFreeWill(ModEntities.ABOMINATION.get(), 0, 2, 0);
        Chicken chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, 1, 2, 1);
        BlockPos at = chicken.blockPosition();
        chicken.hurt(helper.getLevel().damageSources().mobAttack(abomination), 100.0F);
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getBlockState(at).is(ModBlocks.ABOMINATION_EGG.get()), "no egg where the chicken died"));
    }

    @GameTest(template = ItemTests.EMPTY, timeoutTicks = 200)
    public static void abominationEggHatches(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ABOMINATION_EGG.get());
        AbominationEggBlockEntity egg = (AbominationEggBlockEntity) helper.getBlockEntity(pos);
        egg.setIncubation(AbominationEggBlockEntity.INCUBATION_TICKS);
        Player player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(ModBlocks.ABOMINATION_EGG.get(), pos);
            helper.assertEntityPresent(ModEntities.ABOMINATION.get());
        });
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void abominationEggBurstsIntoMutantDna(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ABOMINATION_EGG.get());
        // what the server does after a player breaks a block
        ModBlocks.ABOMINATION_EGG.get().destroy(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
        helper.assertTrue(helper.getBlockState(pos).is(ModFluids.MUTANT_DNA.block.get()), "egg did not burst into mutant DNA");
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void incubatorHatchesGmoEggs(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(ModRecipes.INCUBATING.find(level, new ItemStack(ModItems.combinedGenome(Gmo.WEREWOLF)))
                .map(r -> r.value().result().is(ModEntities.WEREWOLF_EGG.get())).orElse(false), "werewolf genome should hatch a werewolf egg");
        helper.assertTrue(ModRecipes.INCUBATING.find(level, new ItemStack(ModItems.combinedGenome(Gmo.WITHER_SKELETON)))
                .map(r -> r.value().result().is(Items.WITHER_SKELETON_SPAWN_EGG)).orElse(false), "wither skeleton genome should hatch a wither skeleton egg");
        helper.succeed();
    }
}
