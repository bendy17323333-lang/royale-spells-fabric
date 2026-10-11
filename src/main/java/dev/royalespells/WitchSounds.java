// SPDX-License-Identifier: MIT
package dev.royalespells;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Sound assets are independently licensed; see ASSETS.md. */
public final class WitchSounds {
    public static final SoundEvent DEPLOY=sound("witch_deploy"),ATTACK=sound("witch_attack"),IMPACT=sound("witch_impact"),
        SUMMON=sound("witch_summon"),HEAL=sound("witch_heal"),DEATH=sound("witch_death"),
        EVO_DEPLOY=sound("evolved_witch_deploy"),EVO_VOICE=sound("evolved_witch_deploy_voice"),EVO_ATTACK=sound("evolved_witch_attack"),EVO_IMPACT=sound("evolved_witch_impact"),EVO_SUMMON=sound("evolved_witch_summon");
    private static SoundEvent sound(String id){return SoundEvent.createFixedRangeEvent(RoyaleSpells.id(id),48);}
    public static void register(){for(SoundEvent sound:new SoundEvent[]{DEPLOY,ATTACK,IMPACT,SUMMON,HEAL,DEATH,EVO_DEPLOY,EVO_VOICE,EVO_ATTACK,EVO_IMPACT,EVO_SUMMON})Registry.register(BuiltInRegistries.SOUND_EVENT,sound.getLocation(),sound);}
    public static void play(Entity entity,SoundEvent sound,float volume,float pitch){play(entity.level(),entity.position(),sound,volume,pitch);}
    public static void play(Level world,Vec3 position,SoundEvent sound,float volume,float pitch){world.playSound(null,position.x,position.y,position.z,sound,SoundSource.HOSTILE,volume,pitch);}
    private WitchSounds(){}
}
