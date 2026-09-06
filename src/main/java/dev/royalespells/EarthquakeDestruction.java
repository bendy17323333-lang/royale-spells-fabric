package dev.royalespells;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import java.util.*;

/** Three bounded passes over a fixed footprint. Crack damage is shared by overlapping casts. */
public final class EarthquakeDestruction {
    public static final TagKey<Block> BREAKABLE=TagKey.of(RegistryKeys.BLOCK,RoyaleSpells.id("earthquake_breakable"));
    public static final TagKey<Block> STONE=TagKey.of(RegistryKeys.BLOCK,RoyaleSpells.id("earthquake_stone"));
    public static final int MAX_BLOCKS=2048,PER_TICK=128,RETENTION_TICKS=100;
    private static final Map<ServerWorld,Map<BlockPos,Crack>> CRACKS=new WeakHashMap<>();
    private static int nextId=Integer.MIN_VALUE/2;
    private static final class Crack {
        final int id=nextId++;final BlockState state;double damage;long expires;
        Crack(BlockState state){this.state=state;}
    }
    private final List<BlockPos> blocks=new ArrayList<>();
    private int wave,cursor;
    private EarthquakeDestruction(){}
    public EarthquakeDestruction(ServerWorld world,Vec3d center) {
        int base=MathHelper.floor(center.y);double radius=Spell.EARTHQUAKE.radius;
        for(BlockPos p:BlockPos.iterate(MathHelper.floor(center.x-radius),Math.max(world.getBottomY(),base-3),MathHelper.floor(center.z-radius),
                MathHelper.floor(center.x+radius),Math.min(world.getTopY()-1,base+32),MathHelper.floor(center.z+radius))) {
            double dx=p.getX()+.5-center.x,dz=p.getZ()+.5-center.z;
            if(dx*dx+dz*dz<=radius*radius&&world.isChunkLoaded(p)&&world.getWorldBorder().contains(p)&&eligible(world.getBlockState(p)))blocks.add(p.toImmutable());
        }
        blocks.sort(Comparator.comparingDouble(p->p.getSquaredDistance(center)));
        if(blocks.size()>MAX_BLOCKS)blocks.subList(MAX_BLOCKS,blocks.size()).clear();
    }
    private static boolean eligible(BlockState state){return state.isIn(BREAKABLE)||state.isIn(STONE);}
    public int tick(ServerWorld world,UUID owner,int tick) {
        int current=Math.min(3,1+(tick-1)/20);
        if(current!=wave){wave=current;cursor=0;}
        int broken=0,checked=0;var player=owner==null?null:world.getServer().getPlayerManager().getPlayer(owner);
        var ledger=CRACKS.computeIfAbsent(world,w->new HashMap<>());
        while(cursor<blocks.size()&&checked++<PER_TICK){
            BlockPos pos=blocks.get(cursor++);
            if(!world.isChunkLoaded(pos)||!world.getWorldBorder().contains(pos))continue;
            var state=world.getBlockState(pos);
            if(!eligible(state)||state.getHardness(world,pos)<0)continue;
            if(player!=null&&!world.canPlayerModifyAt(player,pos))continue;
            var blockEntity=world.getBlockEntity(pos);
            if(player!=null&&!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world,player,pos,state,blockEntity))continue;
            Crack crack=ledger.get(pos);
            if(crack!=null&&(crack.state!=state||crack.expires<=world.getTime())){world.setBlockBreakingInfo(crack.id,pos,-1);ledger.remove(pos);crack=null;}
            if(crack==null){if(ledger.size()>=8192)continue;crack=new Crack(state);ledger.put(pos,crack);}
            crack.damage+=state.isIn(BREAKABLE)?1d/3:.30;
            crack.expires=world.getTime()+Math.max(0,60-tick)+RETENTION_TICKS;
            if(crack.damage>=.999){
                world.setBlockBreakingInfo(crack.id,pos,-1);ledger.remove(pos);
                if(world.breakBlock(pos,true,player)){broken++;if(player!=null)PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(world,player,pos,state,blockEntity);}
            }else world.setBlockBreakingInfo(crack.id,pos,Math.min(9,(int)Math.round(crack.damage*10)));
        }
        return broken;
    }
    public static double progress(ServerWorld world,BlockPos pos){var map=CRACKS.get(world);var c=map==null?null:map.get(pos);return c==null?0:c.damage;}
    public static void clearRegion(ServerWorld world,Box bounds){
        var map=CRACKS.get(world);if(map==null)return;
        map.entrySet().removeIf(entry->{if(!bounds.contains(Vec3d.ofCenter(entry.getKey())))return false;world.setBlockBreakingInfo(entry.getValue().id,entry.getKey(),-1);return true;});
    }
    public static void tickAll(MinecraftServer server){for(var world:server.getWorlds()){
        var map=CRACKS.get(world);if(map==null)continue;
        map.entrySet().removeIf(entry->{var c=entry.getValue();var pos=entry.getKey();
            if(c.expires<=world.getTime()||!world.isChunkLoaded(pos)||world.getBlockState(pos)!=c.state){world.setBlockBreakingInfo(c.id,pos,-1);return true;}
            if(world.getTime()%20==0)world.setBlockBreakingInfo(c.id,pos,Math.min(9,(int)Math.round(c.damage*10)));
            return false;
        });
    }}
    public static void clear(){CRACKS.clear();}
    public NbtCompound writeNbt(){var nbt=new NbtCompound();nbt.putLongArray("Blocks",blocks.stream().mapToLong(BlockPos::asLong).toArray());nbt.putInt("Wave",wave);nbt.putInt("Cursor",cursor);return nbt;}
    public static EarthquakeDestruction fromNbt(NbtCompound nbt){var work=new EarthquakeDestruction();for(long pos:nbt.getLongArray("Blocks")){if(work.blocks.size()>=MAX_BLOCKS)break;work.blocks.add(BlockPos.fromLong(pos));}work.wave=nbt.getInt("Wave");work.cursor=MathHelper.clamp(nbt.getInt("Cursor"),0,work.blocks.size());return work;}
}
