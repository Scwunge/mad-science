package io.github.scwunge.madscience.content.entity;

import io.github.scwunge.madscience.MadConfig;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.EventHooks;

/** The Abomination lays an egg where anything it kills dies. Needs the config option and mobGriefing. */
@EventBusSubscriber(modid = MadScience.MODID)
public final class AbominationEvents {
    private AbominationEvents() {
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide || !MadConfig.ABOMINATION_LAYS_EGGS.get()
                || !(event.getSource().getEntity() instanceof AbominationEntity abomination)) {
            return;
        }
        if (!EventHooks.canEntityGrief(level, abomination) || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos pos = victim.blockPosition();
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlockAndUpdate(pos, ModBlocks.ABOMINATION_EGG.get().defaultBlockState());
        }
    }
}
