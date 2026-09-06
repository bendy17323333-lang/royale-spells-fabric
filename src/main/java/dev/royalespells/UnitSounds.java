package dev.royalespells;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

/** Original game recordings; sounds.json chooses among the unmodified variants. */
public final class UnitSounds {
    public static final SoundEvent BARBARIAN_DEPLOY=register("barbarian_deploy");
    public static final SoundEvent BARBARIAN_ATTACK=register("barbarian_attack");
    public static final SoundEvent BARBARIAN_STEP=register("barbarian_step");
    public static final SoundEvent BARBARIAN_DEATH=register("barbarian_death");
    private static SoundEvent register(String name){var id=RoyaleSpells.id(name);return Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id));}
    public static void initialize(){}
    private UnitSounds(){}
}
