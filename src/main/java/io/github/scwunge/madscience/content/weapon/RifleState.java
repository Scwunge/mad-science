package io.github.scwunge.madscience.content.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What's loaded in a pulse rifle: rounds in the inserted magazine (up to 99), grenades in the launcher (up to 4), which
 * mode it fires in, whether a magazine is in it at all, and whether the trigger is held (so every client can animate the
 * bolt, muzzle flash and launcher pump).
 */
public record RifleState(int rounds, int grenades, boolean grenadeMode, boolean magazineInserted, boolean firing) {
    public static final int MAX_ROUNDS = 99;
    public static final int MAX_GRENADES = 4;
    public static final RifleState EMPTY = new RifleState(0, 0, false, false, false);

    public static final Codec<RifleState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, MAX_ROUNDS).fieldOf("rounds").forGetter(RifleState::rounds),
            Codec.intRange(0, MAX_GRENADES).fieldOf("grenades").forGetter(RifleState::grenades),
            Codec.BOOL.fieldOf("grenade_mode").forGetter(RifleState::grenadeMode),
            Codec.BOOL.fieldOf("magazine").forGetter(RifleState::magazineInserted),
            Codec.BOOL.optionalFieldOf("firing", false).forGetter(RifleState::firing)
    ).apply(i, RifleState::new));
    public static final StreamCodec<ByteBuf, RifleState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RifleState::rounds,
            ByteBufCodecs.VAR_INT, RifleState::grenades,
            ByteBufCodecs.BOOL, RifleState::grenadeMode,
            ByteBufCodecs.BOOL, RifleState::magazineInserted,
            ByteBufCodecs.BOOL, RifleState::firing,
            RifleState::new);

    public RifleState withRounds(int value) {
        return new RifleState(value, grenades, grenadeMode, magazineInserted, firing);
    }

    public RifleState withGrenades(int value) {
        return new RifleState(rounds, value, grenadeMode, magazineInserted, firing);
    }

    public RifleState withGrenadeMode(boolean value) {
        return new RifleState(rounds, grenades, value, magazineInserted, firing);
    }

    public RifleState withFiring(boolean value) {
        return new RifleState(rounds, grenades, grenadeMode, magazineInserted, value);
    }

    public RifleState withMagazine(boolean inserted, int value) {
        return new RifleState(value, grenades, grenadeMode, inserted, firing);
    }
}
