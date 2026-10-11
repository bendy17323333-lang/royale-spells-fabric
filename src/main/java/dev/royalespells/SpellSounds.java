package dev.royalespells;
import com.google.gson.JsonParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Original card-specific cues, emitted once at actual gameplay phases. */
public final class SpellSounds {
    public static final float RANGE=64f,VOLUME_SCALE=.90f;
    public record Cue(SoundEvent sound,float volume) {}
    private static final Map<String,Cue> CUES=new LinkedHashMap<>();
    public static void initialize() {
        try(var stream=SpellSounds.class.getResourceAsStream("/assets/royalespells/spell_audio.json")) {
            var root=JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            root.entrySet().forEach(card->card.getValue().getAsJsonObject().entrySet().forEach(phase->{
                var config=phase.getValue().getAsJsonObject();String id=config.get("event").getAsString();
                SoundEvent event=id.equals("graveyard_deploy")?RoyaleSpells.GRAVEYARD_DEPLOY:
                    Registry.register(BuiltInRegistries.SOUND_EVENT,RoyaleSpells.id(id),SoundEvent.createFixedRangeEvent(RoyaleSpells.id(id),RANGE));
                CUES.put(card.getKey()+"/"+phase.getKey(),new Cue(event,config.get("volume").getAsFloat()*VOLUME_SCALE));
            }));
            for(Spell spell:Spell.values())if(!CUES.containsKey(spell.id()+"/deploy"))throw new IllegalStateException("Missing original audio profile: "+spell);
        }catch(java.io.IOException e){throw new IllegalStateException("Could not load spell sound profiles",e);}
    }
    public static Map<String,Cue> cues(){return Collections.unmodifiableMap(CUES);}
    public static void play(Level world,Vec3 at,Spell spell,String phase){play(world,at,spell.id(),phase);}
    public static void play(Level world,Vec3 at,String card,String phase) {
        Cue cue=CUES.get(card+"/"+phase);
        if(cue!=null && !world.isClientSide)world.playSound(null,at.x,at.y,at.z,cue.sound,SoundSource.PLAYERS,cue.volume,1);
    }
    private SpellSounds(){}
}
