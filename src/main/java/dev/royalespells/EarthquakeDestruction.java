package dev.royalespells;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import java.util.*;

/** One bounded, distance-ordered pass per cast; queue is rebuilt after entity reload. */
public final class EarthquakeDestruction {
    public static final TagKey<Block> BREAKABLE=TagKey.of(RegistryKeys.BLOCK,RoyaleSpells.id("earthquake_breakable"));
    public static final int MAX_BLOCKS=1024,PER_TICK=24;
    private final ArrayDeque<BlockPos> pending=new ArrayDeque<>();
    public EarthquakeDestruction(ServerWorld world,Vec3d center) {
        int base=MathHelper.floor(center.y);List<BlockPos> found=new ArrayList<>();
        for(BlockPos p:BlockPos.iterate(MathHelper.floor(center.x-3.5),Math.max(world.getBottomY(),base-3),MathHelper.floor(center.z-3.5),
                MathHelper.floor(center.x+3.5),Math.min(world.getTopY()-1,base+32),MathHelper.floor(center.z+3.5))) {
            double dx=p.getX()+.5-center.x,dz=p.getZ()+.5-center.z;
            if(dx*dx+dz*dz<=3.5*3.5 && world.isChunkLoaded(p) && world.getWorldBorder().contains(p) && world.getBlockState(p).isIn(BREAKABLE))found.add(p.toImmutable());
        }
        found.sort(Comparator.comparingDouble(p->Math.pow(p.getX()+.5-center.x,2)+Math.pow(p.getZ()+.5-center.z,2)+Math.abs(p.getY()-base)*.04));
        pending.addAll(found);
    }
    public int tick(ServerWorld world,UUID owner,int alreadyBroken) {
        int broken=0,checked=0;var player=owner==null?null:world.getServer().getPlayerManager().getPlayer(owner);
        while(!pending.isEmpty() && checked++<PER_TICK && alreadyBroken+broken<MAX_BLOCKS) {
            BlockPos pos=pending.removeFirst();if(!world.isChunkLoaded(pos)||!world.getWorldBorder().contains(pos))continue;
            var state=world.getBlockState(pos);if(!state.isIn(BREAKABLE) || state.getHardness(world,pos)<0)continue;
            if(player!=null && !world.canPlayerModifyAt(player,pos))continue;
            var blockEntity=world.getBlockEntity(pos);
            if(player!=null && !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world,player,pos,state,blockEntity))continue;
            if(world.breakBlock(pos,true,player)) {
                broken++;
                if(player!=null)PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(world,player,pos,state,blockEntity);
            }
        }
        return broken;
    }
}
