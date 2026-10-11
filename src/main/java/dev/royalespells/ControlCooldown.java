package dev.royalespells;


import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One persistent 15-second control pool across cards and Mirror. */
public final class ControlCooldown extends SavedData {
    public static final int TICKS=300;
    private final Map<UUID,Long> until=new HashMap<>();
    public static boolean controls(Spell spell){return spell==Spell.FREEZE||spell==Spell.VINES;}
    public static ControlCooldown get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(ControlCooldown::load,ControlCooldown::new,"royale_control_cooldown");}
    public static long remaining(ServerPlayer player){return Math.max(0,get(player.server).until.getOrDefault(player.getUUID(),0L)-player.server.overworld().getGameTime());}
    public static void start(ServerPlayer player){var data=get(player.server);data.until.put(player.getUUID(),player.server.overworld().getGameTime()+TICKS);data.setDirty();sync(player);}
    public static void sync(ServerPlayer player){int ticks=(int)remaining(player);for(var spell:new Spell[]{Spell.FREEZE,Spell.VINES}){var item=RoyaleSpells.ITEMS.get(spell);if(ticks>0)player.getCooldowns().addCooldown(item,ticks);else player.getCooldowns().removeCooldown(item);}}
    public static void install(){
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{sync(handler.player);CardDeployment.syncArmyCooldown(handler.player);});
            net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{sync(newPlayer);CardDeployment.syncArmyCooldown(newPlayer);});
        }
    public static void clear(ServerPlayer player){var data=get(player.server);data.until.remove(player.getUUID());data.setDirty();}
    public static ControlCooldown load(CompoundTag tag){var data=new ControlCooldown();for(var item:tag.getList("Cooldowns",Tag.TAG_COMPOUND)){var t=(CompoundTag)item;if(t.hasUUID("Player"))data.until.put(t.getUUID("Player"),t.getLong("Until"));}return data;}
    @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();until.forEach((id,time)->{var t=new CompoundTag();t.putUUID("Player",id);t.putLong("Until",time);list.add(t);});tag.put("Cooldowns",list);return tag;}
}
