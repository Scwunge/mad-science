package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.Species;
import io.github.scwunge.madscience.content.item.DecayingItem;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MadScience.MODID)
@PrefixGameTestTemplate(false)
public final class ItemTests {
    static final String EMPTY = "gametest/empty";

    private ItemTests() {
    }

    private static void stab(GameTestHelper helper, EntityType<? extends Mob> type, ItemStack expected) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Mob mob = helper.spawnWithNoFreeWill(type, 1, 1, 1);
        ItemStack syringe = new ItemStack(ModItems.EMPTY_SYRINGE.get(), 2);
        syringe.getItem().onLeftClickEntity(syringe, player, mob);
        helper.assertTrue(syringe.getCount() == 1, "syringe was not used up");
        helper.assertTrue(player.getInventory().contains(expected), "player did not get " + expected.getHoverName().getString());
    }

    @GameTest(template = EMPTY)
    public static void syringeDrawsCowBlood(GameTestHelper helper) {
        stab(helper, EntityType.COW, new ItemStack(ModItems.syringe(Species.COW)));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void syringeTellsCaveSpiderFromSpider(GameTestHelper helper) {
        stab(helper, EntityType.CAVE_SPIDER, new ItemStack(ModItems.syringe(Species.CAVE_SPIDER)));
        stab(helper, EntityType.SPIDER, new ItemStack(ModItems.syringe(Species.SPIDER)));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zombifiedPiglinGivesMutantBlood(GameTestHelper helper) {
        stab(helper, EntityType.ZOMBIFIED_PIGLIN, new ItemStack(ModItems.MUTANT_SYRINGE.get()));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void syringeIgnoresUselessMobs(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Mob golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 1, 1, 1);
        ItemStack syringe = new ItemStack(ModItems.EMPTY_SYRINGE.get());
        syringe.getItem().onLeftClickEntity(syringe, player, golem);
        helper.assertTrue(syringe.getCount() == 1, "syringe was used on a mob with no DNA");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void bloodworkDecays(GameTestHelper helper) {
        DecayingItem syringeItem = ModItems.syringe(Species.PIG);
        ItemStack syringe = new ItemStack(syringeItem, 5);
        for (int i = 0; i < DecayingItem.MAX_DECAY; i++) {
            syringe = syringeItem.decayStep(syringe);
        }
        helper.assertTrue(DecayingItem.getDecay(syringe) == DecayingItem.MAX_DECAY, "syringe should be fully decayed");
        ItemStack expired = syringeItem.decayStep(syringe);
        helper.assertTrue(expired.is(ModItems.DIRTY_SYRINGE.get()) && expired.getCount() == 5, "expired syringe should become 5 dirty syringes");

        DecayingItem sampleItem = ModItems.sample(Species.PIG);
        ItemStack sample = new ItemStack(sampleItem);
        DecayingItem.setDecay(sample, DecayingItem.MAX_DECAY);
        helper.assertTrue(sampleItem.decayStep(sample).is(Items.SLIME_BALL), "expired DNA sample should become a slimeball");
        helper.succeed();
    }
}
