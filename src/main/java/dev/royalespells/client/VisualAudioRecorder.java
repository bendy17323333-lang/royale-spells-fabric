package dev.royalespells.client;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.world.phys.Vec3;

/** Records resolved original sound samples at actual game event times. This
 * optional video-export listener never changes the sound engine or gameplay.
 * Offline mixing avoids capturing unrelated desktop audio or a muted endpoint.
 */
final class VisualAudioRecorder implements SoundEventListener {
    private static final List<Map<String,Object>> events=new ArrayList<>();
    private static final Map<TickableSoundInstance,Map<String,Object>> loops=new IdentityHashMap<>();
    private static final Path dir=Minecraft.getInstance().gameDirectory.toPath();
    private static boolean active;
    static void start(){active=true;}
    static void tick(){
        long now=System.currentTimeMillis();
        loops.entrySet().removeIf(e->{if(e.getKey().isStopped()){e.getValue().put("end_ms",now);return true;}return false;});
    }
    static void finish()throws Exception{
        active=false;long now=System.currentTimeMillis();loops.values().forEach(e->e.put("end_ms",now));loops.clear();
        Files.writeString(dir.resolve("recorded-sounds.json"),new GsonBuilder().setPrettyPrinting().create().toJson(events));
    }
    @Override public void onPlaySound(SoundInstance sound,WeighedSoundEvents choices,float radius){
        if(!active||sound.getSource()==net.minecraft.sounds.SoundSource.MUSIC)return;
        try{
            var c=Minecraft.getInstance();var path=sound.getSound().getPath();
            String file=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(path.toString().getBytes(StandardCharsets.UTF_8)))+".ogg";
            Files.createDirectories(dir.resolve("audio-samples"));Path target=dir.resolve("audio-samples").resolve(file);
            if(!Files.exists(target))try(var in=c.getResourceManager().getResourceOrThrow(path).open()){Files.copy(in,target);}
            double distance=c.gameRenderer.getMainCamera().getPosition().distanceTo(new Vec3(sound.getX(),sound.getY(),sound.getZ()));
            double attenuation=sound.isRelative()||sound.getAttenuation()==SoundInstance.Attenuation.NONE?1:Math.max(0,1-distance/radius);
            var row=new LinkedHashMap<String,Object>();row.put("epoch_ms",System.currentTimeMillis());row.put("event",sound.getLocation().toString());row.put("resource",path.toString());row.put("file",file);row.put("volume",sound.getVolume());row.put("pitch",sound.getPitch());row.put("attenuation",attenuation);row.put("loop",sound.isLooping());events.add(row);
            if(sound.isLooping()&&sound instanceof TickableSoundInstance tickable)loops.put(tickable,row);
        }catch(Exception e){throw new IllegalStateException("Cannot archive actual game sound for video",e);}
    }
}
