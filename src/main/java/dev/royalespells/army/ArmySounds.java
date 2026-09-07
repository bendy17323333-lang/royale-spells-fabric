package dev.royalespells.army;
import dev.royalespells.RoyaleSpells;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
public final class ArmySounds {
    public static net.minecraft.core.Holder<net.minecraft.world.item.Instrument> INSTRUMENT;
    public static final SoundEvent DEPLOY=sound("army_deploy"),HORN=sound("army_horn"),CHANGE=sound("army_change"),DEATH=sound("army_death"),STEP=sound("skeleton_step"),ATTACK=sound("skeleton_attack"),SKELETON_DEATH=sound("skeleton_death");
    private static SoundEvent sound(String name){return name.equals("army_deploy")||name.equals("army_horn")?SoundEvent.createFixedRangeEvent(RoyaleSpells.id(name),dev.royalespells.SpellSounds.RANGE):SoundEvent.createVariableRangeEvent(RoyaleSpells.id(name));}
    public static final SoundEvent SHIELD_BREAK=SoundEvent.createFixedRangeEvent(RoyaleSpells.id("army_shield_break"),32);
    public static void register(){for(var sound:new SoundEvent[]{DEPLOY,HORN,CHANGE,DEATH,STEP,ATTACK,SKELETON_DEATH,SHIELD_BREAK})Registry.register(BuiltInRegistries.SOUND_EVENT,sound.getLocation(),sound);}
    private ArmySounds(){}
}
