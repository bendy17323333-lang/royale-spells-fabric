package dev.royalespells.entity;

import dev.royalespells.RoyaleSpells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One short, harmless deployment flash per successful group, shared by all casting paths. */
public final class EvolutionBurst extends Entity {
    public static final int DURATION=24;
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(EvolutionBurst.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS=SynchedEntityData.defineId(EvolutionBurst.class,EntityDataSerializers.FLOAT);
    private long expires;
    public EvolutionBurst(EntityType<? extends EvolutionBurst> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(AGE,0);b.define(RADIUS,1.5f);}
    public int age(){return entityData.get(AGE);}
    public float radius(){return entityData.get(RADIUS);}
    public static void deploy(ServerLevel level,Vec3 at,float radius){
        var burst=RoyaleSpells.EVOLUTION_BURST.create(level);if(burst==null)return;
        burst.setPos(at);burst.entityData.set(RADIUS,Mth.clamp(radius,.5f,3));
        burst.expires=level.getGameTime()+DURATION;level.addFreshEntity(burst);
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        if(expires==0)expires=level().getGameTime()+DURATION-age();
        entityData.set(AGE,Math.max(age()+1,(int)(DURATION-(expires-level().getGameTime()))));
        if(age()>=DURATION)discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag n){n.putInt("Age",age());n.putFloat("Radius",radius());n.putLong("Expires",expires);}
    @Override protected void readAdditionalSaveData(CompoundTag n){entityData.set(AGE,Mth.clamp(n.getInt("Age"),0,DURATION));entityData.set(RADIUS,Mth.clamp(n.getFloat("Radius"),.5f,3));expires=n.getLong("Expires");}
    @Override public boolean isPickable(){return false;}
}
