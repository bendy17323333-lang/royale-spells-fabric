package dev.royalespells;

import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Three bounded passes over a fixed footprint. Crack damage is shared by overlapping casts. */
public final class EarthquakeDestruction {
    public static final TagKey<Block> BREAKABLE=TagKey.create(Registries.BLOCK,RoyaleSpells.id("earthquake_breakable"));
    public static final TagKey<Block> STONE=TagKey.create(Registries.BLOCK,RoyaleSpells.id("earthquake_stone"));
    public static final int MAX_BLOCKS=2048,PER_TICK=128,RETENTION_TICKS=100;
    private static final Map<ServerLevel,Map<BlockPos,Crack>> CRACKS=new WeakHashMap<>();
    private static int nextId=Integer.MIN_VALUE/2;
    private static final class Crack {
        final int id=nextId++;final BlockState state;double damage;long expires;
        Crack(BlockState state){this.state=state;}
    }
    private final List<BlockPos> blocks=new ArrayList<>();
    private int wave,cursor;
    private EarthquakeDestruction(){}
    public EarthquakeDestruction(ServerLevel world,Vec3 center) {
        int base=Mth.floor(center.y);double radius=Spell.EARTHQUAKE.radius;
        for(BlockPos p:BlockPos.betweenClosed(Mth.floor(center.x-radius),Math.max(world.getMinBuildHeight(),base-3),Mth.floor(center.z-radius),
                Mth.floor(center.x+radius),Math.min(world.getMaxBuildHeight()-1,base+32),Mth.floor(center.z+radius))) {
            double dx=p.getX()+.5-center.x,dz=p.getZ()+.5-center.z;
            if(dx*dx+dz*dz<=radius*radius&&world.hasChunkAt(p)&&world.getWorldBorder().isWithinBounds(p)&&eligible(world.getBlockState(p)))blocks.add(p.immutable());
        }
        blocks.sort(Comparator.comparingDouble(p->p.distToCenterSqr(center)));
        if(blocks.size()>MAX_BLOCKS)blocks.subList(MAX_BLOCKS,blocks.size()).clear();
    }
    private static boolean eligible(BlockState state){return state.is(BREAKABLE)||state.is(STONE);}
    public int tick(ServerLevel world,UUID owner,int tick) {
        int current=Math.min(3,1+(tick-1)/20);
        if(current!=wave){wave=current;cursor=0;}
        int broken=0,checked=0;var ownerEntity=CombatCompatibility.resolve(world,owner);
        var player=ownerEntity instanceof net.minecraft.server.level.ServerPlayer p?p:null;
        var ledger=CRACKS.computeIfAbsent(world,w->new HashMap<>());
        while(cursor<blocks.size()&&checked++<PER_TICK){
            BlockPos pos=blocks.get(cursor++);
            if(!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos))continue;
            var state=world.getBlockState(pos);
            if(!eligible(state)||state.getDestroySpeed(world,pos)<0)continue;
            if(player!=null&&!world.mayInteract(player,pos))continue;
            if(player!=null&&NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world,pos,state,player)).isCanceled())continue;
            Crack crack=ledger.get(pos);
            if(crack!=null&&(crack.state!=state||crack.expires<=world.getGameTime())){world.destroyBlockProgress(crack.id,pos,-1);ledger.remove(pos);crack=null;}
            if(crack==null){if(ledger.size()>=8192)continue;crack=new Crack(state);ledger.put(pos,crack);}
            crack.damage+=state.is(BREAKABLE)?1d/3:.30;
            crack.expires=world.getGameTime()+Math.max(0,60-tick)+RETENTION_TICKS;
            if(crack.damage>=.999){
                world.destroyBlockProgress(crack.id,pos,-1);ledger.remove(pos);
                if(world.destroyBlock(pos,true,player)){broken++;}
            }else world.destroyBlockProgress(crack.id,pos,Math.min(9,(int)Math.round(crack.damage*10)));
        }
        return broken;
    }
    public static double progress(ServerLevel world,BlockPos pos){var map=CRACKS.get(world);var c=map==null?null:map.get(pos);return c==null?0:c.damage;}
    public static void clearRegion(ServerLevel world,AABB bounds){
        var map=CRACKS.get(world);if(map==null)return;
        map.entrySet().removeIf(entry->{if(!bounds.contains(Vec3.atCenterOf(entry.getKey())))return false;world.destroyBlockProgress(entry.getValue().id,entry.getKey(),-1);return true;});
    }
    public static void tickAll(MinecraftServer server){for(var world:server.getAllLevels()){
        var map=CRACKS.get(world);if(map==null)continue;
        map.entrySet().removeIf(entry->{var c=entry.getValue();var pos=entry.getKey();
            if(c.expires<=world.getGameTime()||!world.hasChunkAt(pos)||world.getBlockState(pos)!=c.state){world.destroyBlockProgress(c.id,pos,-1);return true;}
            if(world.getGameTime()%20==0)world.destroyBlockProgress(c.id,pos,Math.min(9,(int)Math.round(c.damage*10)));
            return false;
        });
    }}
    public static void clear(){CRACKS.clear();}
    public CompoundTag writeNbt(){var nbt=new CompoundTag();nbt.putLongArray("Blocks",blocks.stream().mapToLong(BlockPos::asLong).toArray());nbt.putInt("Wave",wave);nbt.putInt("Cursor",cursor);return nbt;}
    public static EarthquakeDestruction fromNbt(CompoundTag nbt){var work=new EarthquakeDestruction();for(long pos:nbt.getLongArray("Blocks")){if(work.blocks.size()>=MAX_BLOCKS)break;work.blocks.add(BlockPos.of(pos));}work.wave=nbt.getInt("Wave");work.cursor=Mth.clamp(nbt.getInt("Cursor"),0,work.blocks.size());return work;}
}
