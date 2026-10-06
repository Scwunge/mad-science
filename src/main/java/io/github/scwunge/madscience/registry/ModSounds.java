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

    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_PLACE = register("soniclocator.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_IDLE = register("soniclocator.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_IDLE_CHARGED = register("soniclocator.idle_charged");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_THUMP_START = register("soniclocator.thump_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_THUMP_CHARGE = register("soniclocator.thump_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_THUMP = register("soniclocator.thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_FINISH = register("soniclocator.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_EMPTY = register("soniclocator.empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_COOLDOWN = register("soniclocator.cooldown");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_COOLDOWN_BEEP = register("soniclocator.cooldown_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONICLOCATOR_EXPLODE = register("soniclocator.explode");

    public static final DeferredHolder<SoundEvent, SoundEvent> MEAT_CUBE_MEATSLAP = register("meat_cube.meatslap");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEAT_CUBE_MOO = register("meat_cube.moo");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEAT_CUBE_IDLE = register("meat_cube.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEAT_CUBE_HEARTBEAT = register("meat_cube.heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEAT_CUBE_BELLY = register("meat_cube.belly");

    public static final DeferredHolder<SoundEvent, SoundEvent> VOX_BOX_CHIME = register("vox_box.chime");

    public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOADER_INSERT = register("magazine_loader.insert_magazine");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOADER_LOADING = register("magazine_loader.loading");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOADER_PUSH_START = register("magazine_loader.push_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOADER_PUSH_STEP = register("magazine_loader.push_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE_LOADER_PUSH_STOP = register("magazine_loader.push_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_FINISH_CRUSHING = register("cnc_machine.finish_crushing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_FINISHED = register("cnc_machine.finished");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_INSERT_IRON_BLOCK = register("cnc_machine.insert_iron_block");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_INVALID_BOOK = register("cnc_machine.invalid_book");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_POWER_ON = register("cnc_machine.power_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_PRESS = register("cnc_machine.press");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_PRESS_STOP = register("cnc_machine.press_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_PRESSING_WORK = register("cnc_machine.pressing_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_WATER_FLOW = register("cnc_machine.water_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> CNC_WATER_WORK = register("cnc_machine.water_work");

    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_FIRE = register("pulse_rifle.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_EMPTY = register("pulse_rifle.empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_RELOAD = register("pulse_rifle.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_UNLOAD = register("pulse_rifle.unload");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_MAGAZINE_RELOAD = register("pulse_rifle.magazine_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_MAGAZINE_UNLOAD = register("pulse_rifle.magazine_unload");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_FIRE_GRENADE = register("pulse_rifle.fire_grenade");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_RELOAD_GRENADE = register("pulse_rifle.reload_grenade");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_CHAMBER_GRENADE = register("pulse_rifle.chamber_grenade");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_GRENADE_EXPLODE = register("pulse_rifle.grenade_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_RICOCHET = register("pulse_rifle.ricochet");

    private ModSounds() {
    }

    static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(MadScience.id(name)));
    }
}
