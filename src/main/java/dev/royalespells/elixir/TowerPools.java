package dev.royalespells.elixir;
import dev.royalespells.IronSpellSystem;
import dev.royalespells.mixin.PoolTemplateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import java.util.*;

/** Appends only to newly generated tower chunks, after the original pieces and loot are placed. */
public final class TowerPools {
    public static final ResourceLocation TOWER=ResourceLocation.fromNamespaceAndPath("irons_spellbooks","pyromancer_tower");
    public static final BlockPos BASEMENT_BASIN=new BlockPos(5,-8,18);
    public static boolean roll(long seed,int x,int z,boolean dark,double chance) {
        return RandomSource.create(seed ^ (x*341873128712L) ^ (z*132897987541L) ^ (dark?0xD4E11A9BL:0x51E11A9BL)).nextDouble()<chance;
    }
    private static String template(PoolElementStructurePiece p) {
        return p.getElement() instanceof SinglePoolElement single?((PoolTemplateAccess)single).royaleTemplate().left().map(ResourceLocation::toString).orElse(""):"";
    }
    public static BlockPos basementCenter(PoolElementStructurePiece p) {
        return StructureTemplate.transform(BASEMENT_BASIN,Mirror.NONE,p.getRotation(),BlockPos.ZERO).offset(p.getPosition());
    }
    public static void place(StructureStart start,WorldGenLevel level,ChunkGenerator generator,BoundingBox clip) {
        if(!IronSpellSystem.loaded || !TOWER.equals(level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure())))return;
        var chunk=start.getChunkPos();
        if(roll(level.getSeed(),chunk.x,chunk.z,true,ElixirConfig.DARK_CHANCE.get())) {
            for(var piece:start.getPieces())if(piece instanceof PoolElementStructurePiece p && template(p).equals("irons_spellbooks:pyromancer_tower/basement"))
                crypt(level,p,clip);
        }
        if(roll(level.getSeed(),chunk.x,chunk.z,false,ElixirConfig.ELIXIR_CHANCE.get())) {
            var site=surfaceSite(start,level,generator);
            if(site!=null)basin(level,site,clip,false);
        }
    }
    public static BlockPos surfaceSite(StructureStart start,WorldGenLevel level,ChunkGenerator generator) {
        for(var piece:start.getPieces())if(piece instanceof PoolElementStructurePiece p && template(p).equals("irons_spellbooks:pyromancer_tower/tower")) {
            var b=piece.getBoundingBox();var all=start.getBoundingBox();int cx=b.getCenter().getX(),cz=b.getCenter().getZ();
            var candidates=new ArrayList<>(List.of(new BlockPos(b.maxX()+5,0,cz),new BlockPos(b.minX()-5,0,cz),new BlockPos(cx,0,b.maxZ()+5),new BlockPos(cx,0,b.minZ()-5)));
            for(int offset:new int[]{-6,6}){candidates.add(new BlockPos(b.maxX()+5,0,cz+offset));candidates.add(new BlockPos(b.minX()-5,0,cz+offset));candidates.add(new BlockPos(cx+offset,0,b.maxZ()+5));candidates.add(new BlockPos(cx+offset,0,b.minZ()-5));}
            Collections.rotate(candidates,(int)(level.getSeed() ^ start.getChunkPos().toLong())&3);
            for(var at:candidates) {
                // Keep the full basin inside the structure's existing chunk references.
                if(at.getX()-3<all.minX() || at.getX()+3>all.maxX() || at.getZ()-3<all.minZ() || at.getZ()+3>all.maxZ())continue;
                int y=height(level,generator,at)-1,min=y,max=y;
                for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}) {int h=height(level,generator,at.offset(dx,0,dz))-1;min=Math.min(min,h);max=Math.max(max,h);}
                if(max-min>3 || min<generator.getSeaLevel() || y<=level.getMinBuildHeight()+4)continue;
                var box=new BoundingBox(at.getX()-3,y-1,at.getZ()-3,at.getX()+3,y+2,at.getZ()+3);
                if(start.getPieces().stream().anyMatch(other->other.getBoundingBox().intersects(box)))continue;
                return new BlockPos(at.getX(),y,at.getZ());
            }
        }
        return null;
    }
    private static int height(WorldGenLevel level,ChunkGenerator generator,BlockPos at) {
        return generator.getBaseHeight(at.getX(),at.getZ(),Heightmap.Types.WORLD_SURFACE_WG,level,level.getLevel().getChunkSource().randomState());
    }
    public static void basin(WorldGenLevel level,BlockPos center,BoundingBox clip,boolean dark) {
        int radius=dark?2:3;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
            int d=x*x+z*z;if(!dark && d>10)continue;
            boolean inside=dark?Math.abs(x)<=1 && Math.abs(z)<=1:d<=4;
            BlockPos at=center.offset(x,0,z);
            BlockState lining=dark?Blocks.POLISHED_DEEPSLATE.defaultBlockState():Blocks.CALCITE.defaultBlockState();
            put(level,at.below(),clip,dark && inside?Blocks.CRYING_OBSIDIAN.defaultBlockState():lining);
            var source=(dark?ElixirContent.DARK:ElixirContent.ELIXIR).block.get().defaultBlockState();
            if(dark)source=source.setValue(DarkPoolBlock.NATURAL,true);
            put(level,at,clip,inside?source:lining);
            // Original basement room was measured to be empty above its 3x3 pool.
            // Do not erase shelves, containers or any other block above the original floor.
            if(!dark)for(int y=1;y<=3;y++)put(level,at.above(y),clip,Blocks.AIR.defaultBlockState());
        }
    }
    /** Sealed sub-basement, eight blocks below the old basin, with a lit ladder entrance. */
    public static void crypt(WorldGenLevel level,PoolElementStructurePiece piece,BoundingBox clip) {
        for(int x=1;x<=9;x++)for(int z=13;z<=23;z++)for(int y=-9;y<=-3;y++) {
            boolean shell=x==1||x==9||z==13||z==23||y==-9||y==-3;
            var state=shell?Blocks.DEEPSLATE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState();
            if(shell && y==-9 && (x+z)%4==0)state=Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            local(level,piece,new BlockPos(x,y,z),clip,state);
        }
        for(int x:new int[]{2,8})for(int z:new int[]{14,22}) {
            for(int y=-8;y<=-4;y++)local(level,piece,new BlockPos(x,y,z),clip,Blocks.POLISHED_BASALT.defaultBlockState());
            local(level,piece,new BlockPos(x,-4,z),clip,Blocks.SHROOMLIGHT.defaultBlockState());
        }
        // The measured original room has no containers in this 1x2 access shaft.
        for(int y=-8;y<=0;y++) {
            local(level,piece,new BlockPos(4,y,14),clip,Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            local(level,piece,new BlockPos(5,y,14),clip,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,net.minecraft.core.Direction.EAST));
            local(level,piece,new BlockPos(6,y,14),clip,Blocks.AIR.defaultBlockState());
        }
        for(int x=5;x<=6;x++)for(int z=14;z<=16;z++)for(int y=1;y<=3;y++)
            local(level,piece,new BlockPos(x,y,z),clip,Blocks.AIR.defaultBlockState());
        basin(level,basementCenter(piece),clip,true);
    }
    private static void local(WorldGenLevel level,PoolElementStructurePiece piece,BlockPos relative,BoundingBox clip,BlockState state) {
        put(level,StructureTemplate.transform(relative,Mirror.NONE,piece.getRotation(),BlockPos.ZERO).offset(piece.getPosition()),clip,state.rotate(piece.getRotation()));
    }
    private static void put(WorldGenLevel level,BlockPos at,BoundingBox clip,BlockState state) {
        if(!clip.isInside(at) || level.getBlockState(at).hasBlockEntity())return;
        level.setBlock(at,state,Block.UPDATE_CLIENTS);
        if(!state.getFluidState().isEmpty())level.scheduleTick(at,state.getFluidState().getType(),state.getFluidState().getType().getTickDelay(level));
    }
    private TowerPools(){}
}
