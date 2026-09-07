package dev.royalespells.army;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** World-owned leases survive logout, dimensional travel, entity unloading and server restarts. */
public final class ArmyLedger extends SavedData {
    public record Entry(UUID army,UUID general,long expires,long cooldown) {}
    private final Map<UUID,Entry> entries=new HashMap<>();
    public static ArmyLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ArmyLedger::new,ArmyLedger::load),"royale_skeleton_armies");}
    public static long now(MinecraftServer server){return server.overworld().getGameTime();}
    public Entry entry(UUID owner){return entries.get(owner);}
    public boolean active(UUID owner,long now){var e=entries.get(owner);return e!=null&&e.army()!=null&&now<e.expires();}
    public boolean valid(UUID owner,UUID army,long now){var e=entries.get(owner);return army!=null&&active(owner,now)&&army.equals(e.army());}
    public long remaining(UUID owner,long now){var e=entries.get(owner);return e==null?0:Math.max(0,e.cooldown()-now);}
    public void start(UUID owner,UUID army,UUID general,long expires,long cooldown){entries.put(owner,new Entry(army,general,expires,cooldown));setDirty();}
    public void finish(UUID owner,UUID army){var e=entries.get(owner);if(e!=null&&Objects.equals(e.army(),army)){entries.put(owner,new Entry(null,null,0,e.cooldown()));setDirty();}}
    public static ArmyLedger load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new ArmyLedger();for(var t:tag.getList("Armies",Tag.TAG_COMPOUND)) {
            var e=(CompoundTag)t;if(!e.hasUUID("Owner"))continue;
            data.entries.put(e.getUUID("Owner"),new Entry(e.hasUUID("Army")?e.getUUID("Army"):null,e.hasUUID("General")?e.getUUID("General"):null,e.getLong("Expires"),e.getLong("Cooldown")));
        }return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();entries.forEach((owner,e)->{var t=new CompoundTag();t.putUUID("Owner",owner);if(e.army()!=null)t.putUUID("Army",e.army());if(e.general()!=null)t.putUUID("General",e.general());t.putLong("Expires",e.expires());t.putLong("Cooldown",e.cooldown());list.add(t);});tag.put("Armies",list);return tag;
    }
}
