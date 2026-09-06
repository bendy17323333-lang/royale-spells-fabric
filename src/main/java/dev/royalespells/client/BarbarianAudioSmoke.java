package dev.royalespells.client;
import dev.royalespells.RoyaleSpells;
import net.minecraft.client.MinecraftClient;
import java.util.*;

/** Opt-in playback/decode checks, installed only by VisualSmoke. */
final class BarbarianAudioSmoke {
    private static final Map<String,Integer> heard=new HashMap<>();
    private static boolean walking;
    private static int walkingSteps,walkingAttacks;
    static void install(MinecraftClient client){
        var resources=client.getResourceManager().findResources("sounds/barbarian",id->id.getNamespace().equals(RoyaleSpells.MOD_ID)&&id.getPath().endsWith(".ogg"));
        if(resources.size()!=15)throw new IllegalStateException("Missing original Barbarian recordings: "+resources.size());
        for(var entry:resources.entrySet())try(var input=entry.getValue().getInputStream();var ogg=new net.minecraft.client.sound.OggAudioStream(input)){
            var format=ogg.getFormat();var pcm=ogg.readAll();
            if(format.getChannels()!=1||pcm.remaining()==0)throw new IllegalStateException("Invalid Barbarian audio "+entry.getKey());
            if(entry.getKey().getPath().contains("barb_footstep_")){
                var folder=new java.io.File(client.runDirectory,"audio-review");folder.mkdirs();byte[] data=new byte[pcm.remaining()];pcm.get(data);
                try(var stream=new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(data),format,data.length/format.getFrameSize())){
                    javax.sound.sampled.AudioSystem.write(stream,javax.sound.sampled.AudioFileFormat.Type.WAVE,new java.io.File(folder,entry.getKey().getPath().substring(entry.getKey().getPath().lastIndexOf('/')+1).replace(".ogg",".wav")));
                }
            }
        }catch(java.io.IOException ex){throw new RuntimeException("Barbarian audio decode failed",ex);}
        System.out.println("ROYALE_BARBARIAN_AUDIO_DECODE_OK files="+resources.size());
        client.getSoundManager().registerListener((sound,set,range)->{
            var id=sound.getId();
            if(id.getNamespace().equals(RoyaleSpells.MOD_ID)&&id.getPath().startsWith("barbarian_")){
                if(sound.getPitch()!=1||sound.isRepeatable()||sound.isRelative()||sound.getSound()==net.minecraft.client.sound.SoundManager.MISSING_SOUND)
                    throw new IllegalStateException("Invalid Barbarian playback: "+id);
                int count=heard.merge(id.getPath(),1,Integer::sum);
                if(count==1)System.out.println("ROYALE_BARBARIAN_AUDIO_PLAY "+id);
                if(id.getPath().equals("barbarian_step")){
                    if(sound.getVolume()>.1201f||!sound.getSound().getIdentifier().equals(RoyaleSpells.id("barbarian/barb_footstep_03_no_vo")))
                        throw new IllegalStateException("Walking must use only the quiet recording without vocals: "+sound.getSound().getIdentifier()+" volume="+sound.getVolume());
                    if(walking&&sound.getX()>194&&sound.getX()<196)walkingSteps++;
                }
                if(walking&&id.getPath().equals("barbarian_attack")&&sound.getX()>194&&sound.getX()<196)walkingAttacks++;
            }
            if(sound.getX()>105&&sound.getX()<150&&id.getNamespace().equals("minecraft")&&id.getPath().startsWith("entity.zombie."))
                throw new IllegalStateException("Barbarian showroom played a Zombie sound: "+id);
        });
    }
    static void beginWalking(){walking=true;walkingSteps=0;walkingAttacks=0;}
    static void endWalking(){
        walking=false;
        if(walkingSteps<2||walkingSteps>11||walkingAttacks!=0)throw new IllegalStateException("Quiet walk failed: steps="+walkingSteps+" attacks="+walkingAttacks);
        System.out.println("ROYALE_QUIET_WALK_COMPLETE steps="+walkingSteps+" attacks="+walkingAttacks+" maxVolume=0.12 recording=barb_footstep_03_no_vo");
    }
    static void verify(){
        for(String event:List.of("deploy","attack","step","death"))if(!heard.containsKey("barbarian_"+event))throw new IllegalStateException("Missing real Barbarian playback: "+event);
        System.out.println("ROYALE_BARBARIAN_AUDIO_SMOKE_COMPLETE "+heard);
    }
}
