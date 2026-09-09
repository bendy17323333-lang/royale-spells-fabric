package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.InfernoDragon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import java.util.*;

/** Exactly one original continuous jet per visible lock; quiet, spaced wingbeats. */
public final class InfernoDragonAudio {
    private static final Map<Integer,BeamLoop> LOOPS=new HashMap<>();
    public static void install(){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->tick());}
    private static void tick(){
        var c=Minecraft.getInstance();
        if(c.isPaused())return;
        LOOPS.values().removeIf(sound->{boolean gone=!sound.active()||sound.dragon.level()!=c.level;if(gone)c.getSoundManager().stop(sound);return gone;});
        if(c.level==null||c.player==null)return;
        for(var entity:c.level.entitiesForRendering())if(entity instanceof InfernoDragon dragon){
            if(dragon.isAlive()&&!VisualState.frozen(dragon)&&!dev.royalespells.pause.ElectricPause.active(dragon)&&dragon.deployTicks()==0&&dragon.tickCount%20==3&&dragon.distanceToSqr(c.player)<48*48){
                var cue=SpellSounds.cues().get("inferno_dragon/wing");
                c.level.playLocalSound(dragon.getX(),dragon.getY(),dragon.getZ(),cue.sound(),SoundSource.PLAYERS,cue.volume(),1,false);
            }
            if(dragon.heatTicks()>0&&dragon.beamTarget()!=null&&!VisualState.frozen(dragon)&&!dev.royalespells.pause.ElectricPause.active(dragon)){
                var previous=LOOPS.get(dragon.getId());
                if(previous==null){var sound=new BeamLoop(dragon);LOOPS.put(dragon.getId(),sound);c.getSoundManager().play(sound);}
                if(dragon.tickCount%3==0){
                    var target=dragon.beamTarget();var p=target.getBoundingBox().getCenter();
                    c.level.addParticle(ParticleTypes.FLAME,p.x,p.y,p.z,(c.level.random.nextDouble()-.5)*.04,.02,(c.level.random.nextDouble()-.5)*.04);
                    if(InfernoDragon.tier(dragon.heatTicks())>0)c.level.addParticle(ParticleTypes.SMOKE,p.x,p.y+.12,p.z,0,.025,0);
                }
            }
        }
    }
    public static int activeLoops(){return LOOPS.size();}
    private static final class BeamLoop extends AbstractTickableSoundInstance {
        final InfernoDragon dragon;
        BeamLoop(InfernoDragon e){super(SpellSounds.cues().get("inferno_dragon/beam").sound(),SoundSource.PLAYERS,SoundInstance.createUnseededRandom());dragon=e;looping=true;delay=0;relative=false;attenuation=Attenuation.LINEAR;volume=.2f;tick();}
        boolean active(){return dragon.isAlive()&&dragon.heatTicks()>0&&dragon.beamTarget()!=null&&!VisualState.frozen(dragon)&&!dev.royalespells.pause.ElectricPause.active(dragon);}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){
            if(!active()){stop();return;}
            var p=InfernoDragonRenderer.mouth(dragon,1);x=p.x;y=p.y;z=p.z;
            float heat=Math.min(1,dragon.heatTicks()/90f);
            volume=SpellSounds.cues().get("inferno_dragon/beam").volume()*(.60f+.40f*heat);pitch=.91f+.16f*heat;
        }
    }
    private InfernoDragonAudio(){}
}
