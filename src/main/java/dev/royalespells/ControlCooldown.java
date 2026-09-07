package dev.royalespells;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One persistent 15-second control pool across cards, scrolls, books and Mirror. */
public final class ControlCooldown extends SavedData {
    public static final int TICKS=300;
    private final Map<UUID,Long> until=new HashMap<>();
    public static boolean controls(Spell spell){return spell==Spell.FREEZE||spell==Spell.VINES;}
    public static ControlCooldown get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ControlCooldown::new,ControlCooldown::load),"royale_control_cooldown");}
    public static long remaining(ServerPlayer player){return Math.max(0,get(player.server).until.getOrDefault(player.getUUID(),0L)-player.server.overworld().getGameTime());}
    public static void start(ServerPlayer player){var data=get(player.server);data.until.put(player.getUUID(),player.server.overworld().getGameTime()+TICKS);data.setDirty();sync(player);}
    public static void sync(ServerPlayer player){int ticks=(int)remaining(player);for(var spell:new Spell[]{Spell.FREEZE,Spell.VINES}){var item=RoyaleSpells.ITEMS.get(spell);if(ticks>0)player.getCooldowns().addCooldown(item,ticks);else player.getCooldowns().removeCooldown(item);}if(IronSpellSystem.loaded)dev.royalespells.iron.SharedControl.sync(player,ticks);}
    public static void install(){
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer player)sync(player);});
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e)->{if(e.getEntity() instanceof ServerPlayer player)sync(player);});
    }
    public static void clear(ServerPlayer player){var data=get(player.server);data.until.remove(player.getUUID());data.setDirty();}
    public static ControlCooldown load(CompoundTag tag,HolderLookup.Provider registries){var data=new ControlCooldown();for(var item:tag.getList("Cooldowns",Tag.TAG_COMPOUND)){var t=(CompoundTag)item;if(t.hasUUID("Player"))data.until.put(t.getUUID("Player"),t.getLong("Until"));}return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();until.forEach((id,time)->{var t=new CompoundTag();t.putUUID("Player",id);t.putLong("Until",time);list.add(t);});tag.put("Cooldowns",list);return tag;}
}
