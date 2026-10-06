package io.github.scwunge.madscience.client;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.weapon.PulseRifleItem;
import io.github.scwunge.madscience.content.weapon.RifleTrigger;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Turns the attack button into the pulse rifle's trigger instead of punching or mining. */
@EventBusSubscriber(modid = MadScience.MODID, value = Dist.CLIENT)
public final class RifleInput {
    private static boolean held;

    private RifleInput() {
    }

    private static boolean holdingRifle(Minecraft mc) {
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof PulseRifleItem;
    }

    @SubscribeEvent
    static void attackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && holdingRifle(Minecraft.getInstance())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean now = holdingRifle(mc) && mc.screen == null && mc.options.keyAttack.isDown();
        if (now != held && mc.getConnection() != null) {
            held = now;
            PacketDistributor.sendToServer(new RifleTrigger.Payload(now));
        }
    }
}
