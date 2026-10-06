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
    public static final DeferredHolder<SoundEvent, SoundEvent> SEQUENCER_START = register("sequencer.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEQUENCER_WORK = register("sequencer.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEQUENCER_FINISH = register("sequencer.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINFRAME_START = register("mainframe.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINFRAME_WORK = register("mainframe.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINFRAME_IDLE = register("mainframe.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINFRAME_FINISH = register("mainframe.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINFRAME_OVERHEAT = register("mainframe.overheat");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_START = register("incubator.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_WORK = register("incubator.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_FINISH = register("incubator.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> FREEZER_IDLE = register("cryo_freezer.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUPLICATOR_START = register("data_duplicator.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUPLICATOR_IDLE = register("data_duplicator.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUPLICATOR_WORK = register("data_duplicator.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUPLICATOR_FINISH = register("data_duplicator.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONDER_IDLE = register("thermosonic_bonder.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONDER_LASER_START = register("thermosonic_bonder.laser_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONDER_LASER_WORKING = register("thermosonic_bonder.laser_working");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONDER_STAMP = register("thermosonic_bonder.stamp");

    public static final DeferredHolder<SoundEvent, SoundEvent> WEREWOLF_ATTACK = register("werewolf.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEREWOLF_DEATH = register("werewolf.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEREWOLF_SNARL = register("werewolf.snarl");
    public static final DeferredHolder<SoundEvent, SoundEvent> CREEPER_COW_ATTACK = register("creeper_cow.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_GROWL = register("abomination.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_HISS = register("abomination.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_PAIN = register("abomination.pain");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_DEATH = register("abomination.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_STEP = register("abomination.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_EGG = register("abomination.egg");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_EGGPOP = register("abomination.eggpop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABOMINATION_EGGHATCH = register("abomination.egghatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOLY_COW_SAY = register("wooly_cow.say");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOLY_COW_HURT = register("wooly_cow.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOLY_COW_STEP = register("wooly_cow.step");

    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_IDLE = register("cryo_tube.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_OFF = register("cryo_tube.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_CRACK_EGG = register("cryo_tube.crack_egg");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_HATCHING = register("cryo_tube.hatching");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_HATCH = register("cryo_tube.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_WORK = register("cryo_tube.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOTUBE_STILLBIRTH = register("cryo_tube.stillbirth");

    private ModSounds() {
    }

    static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(MadScience.id(name)));
    }
}
