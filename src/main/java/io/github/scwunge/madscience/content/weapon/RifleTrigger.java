package io.github.scwunge.madscience.content.weapon;

import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The client tells the server when the attack button goes down or up while holding a pulse rifle; the server fires. */
@EventBusSubscriber(modid = MadScience.MODID)
public final class RifleTrigger {
    /** Players holding the trigger, with how many ticks they have held it. */
    private static final Map<UUID, Integer> HELD = new ConcurrentHashMap<>();
    /** Players who launched a grenade during this pull; the next one is chambered when they let go. */
    private static final Set<UUID> CHAMBER = ConcurrentHashMap.newKeySet();

    private RifleTrigger() {
    }

    public record Payload(boolean held) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(MadScience.id("rifle_trigger"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = ByteBufCodecs.BOOL.map(Payload::new, Payload::held);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Payload.TYPE, Payload.CODEC, RifleTrigger::handle);
    }

    private static void handle(Payload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            setHeld(player, payload.held());
        }
    }

    private static void release(ServerPlayer player) {
        HELD.remove(player.getUUID());
        setFiring(player.getMainHandItem(), false);
        if (CHAMBER.remove(player.getUUID())) {
            PulseRifleItem.chamberGrenade(player);
        }
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Integer ticks = HELD.get(player.getUUID());
        if (ticks == null) {
            return;
        }
        ItemStack rifle = player.getMainHandItem();
        if (!(rifle.getItem() instanceof PulseRifleItem) || player.isSpectator()) {
            release(player);
            return;
        }
        if (PulseRifleItem.triggerTick(player, rifle, ticks == 0) && PulseRifleItem.state(rifle).grenadeMode()) {
            CHAMBER.add(player.getUUID());
        }
        HELD.put(player.getUUID(), ticks + 1);
    }

    @SubscribeEvent
    static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().getUUID());
        CHAMBER.remove(event.getEntity().getUUID());
    }

    /** The rifle shows the trigger state so every client can animate it. */
    private static void setFiring(ItemStack rifle, boolean firing) {
        if (rifle.getItem() instanceof PulseRifleItem && PulseRifleItem.state(rifle).firing() != firing) {
            rifle.set(ModDataComponents.RIFLE.get(), PulseRifleItem.state(rifle).withFiring(firing));
        }
    }

    public static boolean isHeld(net.minecraft.world.entity.Entity entity) {
        return HELD.containsKey(entity.getUUID());
    }

    /** For tests: pull or release the trigger as if the client had. */
    public static void setHeld(ServerPlayer player, boolean held) {
        if (held) {
            if (HELD.putIfAbsent(player.getUUID(), 0) == null) {
                setFiring(player.getMainHandItem(), true);
            }
        } else {
            release(player);
        }
    }
}
