package dev.royalespells;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Original game recordings; sounds.json chooses among the unmodified variants. */
public final class UnitSounds {
    public static final SoundEvent BARBARIAN_DEPLOY=register("barbarian_deploy");
    public static final SoundEvent BARBARIAN_ATTACK=register("barbarian_attack");
    public static final SoundEvent BARBARIAN_STEP=register("barbarian_step");
    public static final SoundEvent BARBARIAN_DEATH=register("barbarian_death");
    private static SoundEvent register(String name){var id=RoyaleSpells.id(name);return SoundEvent.createVariableRangeEvent(id);}
    public static void initialize(){for(var sound:java.util.List.of(BARBARIAN_DEPLOY,BARBARIAN_ATTACK,BARBARIAN_STEP,BARBARIAN_DEATH))Registry.register(BuiltInRegistries.SOUND_EVENT,sound.getLocation(),sound);}
    private UnitSounds(){}
}
