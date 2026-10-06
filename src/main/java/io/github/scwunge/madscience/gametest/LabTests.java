package io.github.scwunge.madscience.gametest;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.sign.WarningSignEntity;
import io.github.scwunge.madscience.content.sign.WarningSignType;
import io.github.scwunge.madscience.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(MadScience.MODID)
@PrefixGameTestTemplate(false)
public final class LabTests {
    private LabTests() {
    }

    private static List<WarningSignEntity> signs(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(WarningSignEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(4));
    }

    /** Only the owner can change or take down a sign; anyone else just reads it. */
    @GameTest(template = ItemTests.EMPTY)
    public static void warningSignBelongsToItsOwner(GameTestHelper helper) {
        BlockPos wall = new BlockPos(1, 2, 1);
        helper.setBlock(wall, Blocks.STONE);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack item = new ItemStack(ModItems.WARNING_SIGN.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockPos absWall = helper.absolutePos(wall);
        item.useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absWall).relative(Direction.NORTH, 0.5), Direction.NORTH, absWall, false)));
        helper.assertTrue(item.isEmpty(), "placing did not use the item");
        List<WarningSignEntity> placed = signs(helper);
        helper.assertTrue(placed.size() == 1, "expected one sign, found " + placed.size());
        WarningSignEntity sign = placed.getFirst();
        helper.assertTrue(owner.getUUID().equals(sign.owner()), "sign is not owned by its placer");
        helper.assertTrue(sign.signType() == WarningSignType.GENERIC_WARNING, "a new sign should show the generic warning");

        stranger.setShiftKeyDown(true);
        sign.skipAttackInteraction(stranger);
        helper.assertTrue(sign.signType() == WarningSignType.GENERIC_WARNING, "a stranger changed the sign");
        stranger.setShiftKeyDown(false);
        sign.skipAttackInteraction(stranger);
        helper.assertTrue(sign.isAlive(), "a stranger took the sign down");

        owner.setShiftKeyDown(true);
        sign.skipAttackInteraction(owner);
        helper.assertTrue(sign.signType() == WarningSignType.MAGNETIC_FIELD_1, "sneak-hitting should cycle to the next symbol");
        owner.setShiftKeyDown(false);
        sign.skipAttackInteraction(owner);
        helper.assertTrue(!sign.isAlive(), "the owner could not take the sign down");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(4),
                e -> e.getItem().is(ModItems.WARNING_SIGN.get())).isEmpty(), "taking the sign down did not drop it");
        helper.succeed();
    }

    @GameTest(template = ItemTests.EMPTY)
    public static void labCoatFitsArmourSlots(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(player.getEquipmentSlotForItem(new ItemStack(ModItems.LAB_COAT_GOGGLES.get())) == EquipmentSlot.HEAD, "goggles slot");
        helper.assertTrue(player.getEquipmentSlotForItem(new ItemStack(ModItems.LAB_COAT_BODY.get())) == EquipmentSlot.CHEST, "coat slot");
        helper.assertTrue(player.getEquipmentSlotForItem(new ItemStack(ModItems.LAB_COAT_LEGGINGS.get())) == EquipmentSlot.LEGS, "leggings slot");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.LAB_COAT_BODY.get()));
        helper.assertTrue(player.getArmorValue() == 0, "the lab coat should give no protection, like the original");
        helper.succeed();
    }
}
