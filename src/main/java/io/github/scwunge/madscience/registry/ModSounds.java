package io.github.scwunge.madscience.registry;

import io.github.scwunge.madscience.MadScience;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events. What each one actually plays is decided in {@code assets/madscience/sounds.json}. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, MadScience.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SYRINGE_STAB_PLAYER = register("syringe.stab_player");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYRINGE_STAB_MOB = register("syringe.stab_mob");
    public static final DeferredHolder<SoundEvent, SoundEvent> DNA_EXTRACTOR_IDLE = register("dna_extractor.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DNA_EXTRACTOR_FINISH = register("dna_extractor.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> SANITIZER_IDLE = register("sanitizer.idle");

    private ModSounds() {
    }

    static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(MadScience.id(name)));
    }
}
