package io.github.scwunge.madscience.content.sign;

import io.github.scwunge.madscience.content.item.TooltipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Hangs a warning sign, owned by whoever placed it, on the side of a block. */
public class WarningSignItem extends TooltipItem {
    public WarningSignItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (face.getAxis().isVertical() || player != null && !player.mayUseItemAt(pos, face, stack)) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        WarningSignEntity sign = new WarningSignEntity(level, pos, face, player);
        if (!sign.survives()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            sign.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, sign.position());
            level.addFreshEntity(sign);
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
