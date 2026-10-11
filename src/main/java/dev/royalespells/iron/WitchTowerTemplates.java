// SPDX-License-Identifier: MIT
package dev.royalespells.iron;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Runtime adaptation of the installed Iron structure. The addon ships no copy
 * of Iron's building NBT, and never mutates the original template or its cache. */
public final class WitchTowerTemplates {
    public static boolean owns(ResourceLocation id){
        return id.getNamespace().equals("royalespells")&&id.getPath().startsWith("witch_tower/");
    }
    public static ResourceLocation source(ResourceLocation id){
        return ResourceLocation.fromNamespaceAndPath("irons_spellbooks","evoker_fort/"+id.getPath().substring("witch_tower/".length()));
    }
    public static CompoundTag adapt(CompoundTag input,ResourceLocation id){
        CompoundTag root=input.copy();
        remap(root);
        ListTag entities=root.getList("entities",Tag.TAG_COMPOUND);
        for(Tag raw:entities){
            CompoundTag entry=(CompoundTag)raw,old=entry.getCompound("nbt");
            String kind=old.getString("id");
            CompoundTag next=null;
            if(kind.equals("irons_spellbooks:archevoker"))next=mob("royalespells:witch");
            else if(kind.equals("minecraft:villager")||kind.equals("minecraft:wandering_trader")){
                boolean vindicator=id.getPath().contains("bible");
                next=mob(vindicator?"minecraft:vindicator":"minecraft:pillager");
                next.putBoolean("CanPickUpLoot",false);
                ListTag hands=new ListTag();CompoundTag weapon=new CompoundTag();
                weapon.putString("id",vindicator?"minecraft:iron_axe":"minecraft:crossbow");weapon.putInt("count",1);
                hands.add(weapon);hands.add(new CompoundTag());next.put("HandItems",hands);
            }else if(kind.equals("irons_spellbooks:magehunter_vindicator")||kind.equals("minecraft:pillager")
                    ||kind.equals("minecraft:ravager")||kind.equals("minecraft:evoker"))next=mob("royalespells:witch_skeleton");
            if(next!=null)entry.put("nbt",next);
        }
        for(Tag raw:root.getList("blocks",Tag.TAG_COMPOUND)){
            CompoundTag b=((CompoundTag)raw).getCompound("nbt");
            if(!b.getString("id").equals("minecraft:mob_spawner")&&!b.getString("id").equals("minecraft:spawner"))continue;
            if(!b.getCompound("SpawnData").getCompound("entity").getString("id").equals("minecraft:vex"))continue;
            CompoundTag spawn=new CompoundTag(),skeleton=new CompoundTag();skeleton.putString("id","royalespells:witch_skeleton");
            spawn.put("entity",skeleton);b.put("SpawnData",spawn);b.put("SpawnPotentials",new ListTag());
            b.putShort("SpawnCount",(short)3);b.putShort("MinSpawnDelay",(short)300);b.putShort("MaxSpawnDelay",(short)600);
            b.putShort("RequiredPlayerRange",(short)12);
        }
        if(id.getPath().equals("witch_tower/tower")){
            // Clear courtyard cells. The old (16,1,16) candidate is a candle
            // plinth in Iron 3.16.3 and would embed a patrol in cobblestone.
            for(double[] cell:new double[][]{{2.5,5.5},{16.5,5.5},{2.5,16.5},{13.5,14.5}}){
                double x=cell[0],z=cell[1];
                CompoundTag entry=new CompoundTag();ListTag p=new ListTag(),bp=new ListTag();
                for(double v:new double[]{x,1,z}){p.add(DoubleTag.valueOf(v));bp.add(IntTag.valueOf((int)v));}
                CompoundTag guard=mob("royalespells:witch_skeleton");ListTag tags=new ListTag();tags.add(StringTag.valueOf("RoyaleWitchGroundPatrol"));guard.put("Tags",tags);
                entry.put("pos",p);entry.put("blockPos",bp);entry.put("nbt",guard);entities.add(entry);
            }
        }
        root.put("entities",entities);return root;
    }
    private static CompoundTag mob(String id){CompoundTag n=new CompoundTag();n.putString("id",id);n.putBoolean("PersistenceRequired",true);return n;}
    private static Tag remap(Tag t){
        if(t instanceof StringTag s)return StringTag.valueOf(s.getAsString().replace("irons_spellbooks:evoker_fort/","royalespells:witch_tower/"));
        if(t instanceof CompoundTag c)for(String key:c.getAllKeys().toArray(String[]::new))c.put(key,remap(c.get(key)));
        else if(t instanceof ListTag l)for(int i=0;i<l.size();i++)l.set(i,remap(l.get(i)));
        return t;
    }
    private WitchTowerTemplates(){}
}
