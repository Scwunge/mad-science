package io.github.scwunge.madscience.registry;

import com.mojang.serialization.Codec;
import io.github.scwunge.madscience.MadScience;
import io.github.scwunge.madscience.content.weapon.RifleState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MadScience.MODID);

    /**
     * How far a syringe or DNA sample has decayed, 0 (fresh) to {@link io.github.scwunge.madscience.content.item.DecayingItem#MAX_DECAY}.
     * A component instead of item damage so that bloodwork still stacks to 64 like in the original.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DECAY = REGISTER.registerComponentType("decay",
            builder -> builder.persistent(Codec.intRange(0, 10)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Rounds in a pulse rifle magazine, 0 to 99. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROUNDS = REGISTER.registerComponentType("rounds",
            builder -> builder.persistent(Codec.intRange(0, RifleState.MAX_ROUNDS)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** What is loaded in a pulse rifle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RifleState>> RIFLE = REGISTER.registerComponentType("rifle",
            builder -> builder.persistent(RifleState.CODEC).networkSynchronized(RifleState.STREAM_CODEC));

    private ModDataComponents() {
    }
}
