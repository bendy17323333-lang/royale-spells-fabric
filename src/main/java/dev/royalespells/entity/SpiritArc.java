package dev.royalespells.entity;

import dev.royalespells.RoyaleSpells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** One visual link at an actual chain hit. It never performs damage or finds targets. */
public final class SpiritArc extends Entity {
    public static final int DURATION=7;
    private static final EntityDataAccessor<Vector3f> END=SynchedEntityData.defineId(SpiritArc.class,EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(SpiritArc.class,EntityDataSerializers.INT);
    private long expires;
    public SpiritArc(EntityType<? extends SpiritArc> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(END,new Vector3f());b.define(AGE,0);}
    public Vec3 end(){return new Vec3(entityData.get(END));}
    public int age(){return entityData.get(AGE);}
    public static void link(ServerLevel world,Vec3 from,Vec3 to){
        if(from.distanceToSqr(to)>64)return;
        var arc=RoyaleSpells.SPIRIT_ARC.create(world);if(arc==null)return;
        arc.setPos(from);arc.entityData.set(END,to.subtract(from).toVector3f());
        arc.expires=world.getGameTime()+DURATION;world.addFreshEntity(arc);
    }
    @Override public void tick(){super.tick();if(!level().isClientSide){
        if(expires==0)expires=level().getGameTime()+DURATION-age();
        entityData.set(AGE,Math.max(age()+1,(int)(DURATION-(expires-level().getGameTime()))));
        if(age()>=DURATION)discard();
    }}
    @Override public AABB getBoundingBoxForCulling(){return new AABB(position(),position().add(end())).inflate(.5);}
    @Override public boolean isPickable(){return false;}
    @Override protected void addAdditionalSaveData(CompoundTag n){n.putInt("Age",age());n.putLong("Expires",expires);n.putDouble("EndX",end().x);n.putDouble("EndY",end().y);n.putDouble("EndZ",end().z);}
    @Override protected void readAdditionalSaveData(CompoundTag n){entityData.set(AGE,Math.max(0,n.getInt("Age")));expires=n.getLong("Expires");var v=new Vec3(n.getDouble("EndX"),n.getDouble("EndY"),n.getDouble("EndZ"));entityData.set(END,(v.lengthSqr()<=64?v:Vec3.ZERO).toVector3f());}
}
