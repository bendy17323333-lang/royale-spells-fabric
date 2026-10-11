// SPDX-License-Identifier: MIT
package dev.royalespells.entity;

import dev.royalespells.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import java.util.*;

/** Authoritative swept collision projectile, also used for a first-wave healing
 * soul. Impact geometry stays at the real world hit point for sixteen ticks. */
public final class WitchBolt extends Entity {
    public static final int IMPACT_TICKS=16, ATTACK=0, HEAL_SOUL=1;
    public static final double SPEED=.72;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.INT),
        AGE=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.INT),
        IMPACT=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EVO=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> START=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.VECTOR3),
        DIRECTION=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.VECTOR3),
        NORMAL=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.VECTOR3),
        IMPACT_POINT=SynchedEntityData.defineId(WitchBolt.class,EntityDataSerializers.VECTOR3);
    private UUID owner,source,target,skeleton;
    private float damage;
    private int missingTargetTicks;
    // Entity's default lerpTo teleports straight to each network point. Queue
    // those points instead: client ticks advance a short interpolation and
    // getPosition(partial) supplies every render frame between them. These
    // fields are presentation-only and never affect server swept collision.
    private Vec3 clientLerpTarget;
    private int clientLerpSteps;
    public WitchBolt(EntityType<? extends WitchBolt> type,Level world){super(type,world);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,ATTACK);entityData.define(AGE,0);entityData.define(IMPACT,0);entityData.define(EVO,false);
        entityData.define(START,new Vector3f());entityData.define(DIRECTION,new Vector3f(0,0,1));entityData.define(NORMAL,new Vector3f(0,1,0));entityData.define(IMPACT_POINT,new Vector3f());}
    public int kind(){return entityData.get(KIND);}
    public int age(){return entityData.get(AGE);}
    public boolean evolved(){return entityData.get(EVO);}
    public boolean impacted(){return impactAge()>0;}
    public int impactAge(){return entityData.get(IMPACT);}
    public float impactRadius(){return kind()==HEAL_SOUL?.5f:(float)RoyalWitch.SPLASH_RADIUS;}
    public Vec3 flightDirection(){return new Vec3(entityData.get(DIRECTION));}
    public Vec3 impactNormal(){return new Vec3(entityData.get(NORMAL));}
    /** Metadata travels with the impact flag, avoiding a one-frame flash at the
     * preceding interpolated flight position when movement packets arrive later. */
    public Vec3 impactPosition(){return new Vec3(entityData.get(IMPACT_POINT));}
    public Vec3 start(){return new Vec3(entityData.get(START));}
    public UUID recipient(){return target;}
    public UUID skeletonId(){return skeleton;}
    public static WitchBolt launch(ServerLevel world,RoyalWitch witch,LivingEntity victim){
        WitchBolt bolt=RoyaleSpells.WITCH_BOLT.create(world);if(bolt==null)return null;
        bolt.owner=witch.ownerId();bolt.source=witch.getUUID();bolt.target=victim.getUUID();bolt.damage=witch.projectileDamage();
        bolt.entityData.set(EVO,witch.evolved());
        // An outstretched casting hand may touch a wall before the muzzle. Clip from
        // the body to it so spawning a projectile cannot skip nearby terrain.
        BlockHitResult muzzleWall=world.clip(new ClipContext(witch.position().add(0,1.382,0),witch.muzzle(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,witch));
        bolt.setPos(muzzleWall.getType()==HitResult.Type.BLOCK?muzzleWall.getLocation():witch.muzzle());bolt.entityData.set(START,bolt.position().toVector3f());
        bolt.direction(victim.getBoundingBox().getCenter().subtract(bolt.position()));
        if(!world.addFreshEntity(bolt))return null;
        if(muzzleWall.getType()==HitResult.Type.BLOCK)bolt.impact(Vec3.atLowerCornerOf(muzzleWall.getDirection().getNormal()));
        return bolt;
    }
    /** Constructed before registering its UUID in the mother's authorization ledger. */
    public static WitchBolt soul(ServerLevel world,RoyalWitch mother,AllySkeleton dead){
        WitchBolt soul=RoyaleSpells.WITCH_BOLT.create(world);if(soul==null)return null;
        soul.owner=mother.ownerId();soul.source=mother.getUUID();soul.target=mother.getUUID();soul.skeleton=dead.getUUID();
        soul.entityData.set(KIND,HEAL_SOUL);soul.entityData.set(EVO,true);
        soul.setPos(dead.position().add(0,.65,0));soul.entityData.set(START,soul.position().toVector3f());
        soul.direction(mother.position().add(0,1.3,0).subtract(soul.position()));return soul;
    }
    private void direction(Vec3 direction){if(direction.lengthSqr()>.000001)entityData.set(DIRECTION,direction.normalize().toVector3f());}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){
        Vec3 next=new Vec3(x,y,z);
        if(!level().isClientSide||teleport||position().distanceToSqr(next)>64){
            super.lerpTo(x,y,z,yaw,pitch,steps,teleport);clientLerpTarget=null;clientLerpSteps=0;return;
        }
        clientLerpTarget=next;clientLerpSteps=2;setYRot(yaw);setXRot(pitch);
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide){
            if(!impacted()&&clientLerpTarget!=null&&clientLerpSteps>0){
                setPos(position().lerp(clientLerpTarget,1.0/clientLerpSteps));clientLerpSteps--;
            }
            return;
        }
        if(impacted()){if(impactAge()>=IMPACT_TICKS)discard();else entityData.set(IMPACT,impactAge()+1);return;}
        if(kind()==HEAL_SOUL){tickSoul();return;}
        entityData.set(AGE,age()+1);if(age()>80){discard();return;}
        ServerLevel world=(ServerLevel)level();Entity victim=target==null?null:world.getEntity(target);
        if(victim instanceof LivingEntity living&&validTarget(living))direction(living.getBoundingBox().getCenter().subtract(position()));
        Vec3 from=position(),to=from.add(flightDirection().scale(SPEED));
        BlockHitResult block=world.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        double nearest=block.getType()==HitResult.Type.BLOCK?from.distanceToSqr(block.getLocation()):Double.POSITIVE_INFINITY;
        Vec3 hit=block.getType()==HitResult.Type.BLOCK?block.getLocation():null;
        Vec3 normal=block.getType()==HitResult.Type.BLOCK?Vec3.atLowerCornerOf(block.getDirection().getNormal()):flightDirection().scale(-1);
        for(LivingEntity candidate:world.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(.7),this::validTarget)){
            Optional<Vec3> crossing=candidate.getBoundingBox().inflate(.12).clip(from,to);
            Vec3 point=candidate.getBoundingBox().inflate(.12).contains(from)?from:crossing.orElse(null);
            if(point!=null&&from.distanceToSqr(point)<nearest){nearest=from.distanceToSqr(point);hit=point;normal=flightDirection().scale(-1);}
        }
        if(hit!=null){setPos(hit);impact(normal);}else{setPos(to);setDeltaMovement(to.subtract(from));}
    }
    private boolean validTarget(LivingEntity entity){return !entity.getUUID().equals(source)&&entity.isAttackable()&&SpellEngine.enemy(owner,entity)
        &&(!(entity instanceof net.minecraft.world.entity.player.Player)||level().getServer().isPvpAllowed());}
    private void impact(Vec3 normal){
        if(impacted())return;entityData.set(IMPACT_POINT,position().toVector3f());entityData.set(IMPACT,1);entityData.set(NORMAL,normal.normalize().toVector3f());setDeltaMovement(Vec3.ZERO);
        ServerLevel world=(ServerLevel)level();Entity attacker=source==null?null:world.getEntity(source);
        for(LivingEntity victim:world.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(RoyalWitch.SPLASH_RADIUS+1),this::validTarget)){
            // CR measures an area on its arena plane. Keep a bounded vertical
            // envelope for Minecraft flying units, but exact 1.5-block XZ radius.
            double dx=victim.getX()-getX(),dz=victim.getZ()-getZ();
            if(dx*dx+dz*dz>RoyalWitch.SPLASH_RADIUS*RoyalWitch.SPLASH_RADIUS
                ||getY()<victim.getBoundingBox().minY-1.5||getY()>victim.getBoundingBox().maxY+1.5)continue;
            Vec3 center=victim.getBoundingBox().getCenter();
            if(world.clip(new ClipContext(position().add(normal.scale(.025)),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)continue;
            int immune=victim.invulnerableTime;victim.invulnerableTime=0;
            try{CombatImpact.withoutKnockback(victim,()->victim.hurt(world.damageSources().indirectMagic(this,attacker),damage));}
            finally{victim.invulnerableTime=Math.max(immune,victim.invulnerableTime);}
        }
        WitchSounds.play(world,position(),evolved()?WitchSounds.EVO_IMPACT:WitchSounds.IMPACT,.52f,1);
    }
    private void tickSoul(){
        ServerLevel world=(ServerLevel)level();Entity recipient=target==null?null:world.getEntity(target);
        if(recipient==null){if(++missingTargetTicks>200)discard();return;}
        if(!(recipient instanceof RoyalWitch mother)||!mother.isAlive()){discard();return;}
        missingTargetTicks=0;entityData.set(AGE,age()+1);
        Vec3 finish=mother.position().add(0,1.3,0);double fraction=Math.min(1,age()/(double)RoyalWitch.SOUL_TICKS);
        Vec3 next=start().lerp(finish,fraction).add(0,Math.sin(fraction*Math.PI)*.7,0);
        direction(next.subtract(position()));setDeltaMovement(next.subtract(position()));setPos(next);
        if(age()>=RoyalWitch.SOUL_TICKS){
            if(mother.acceptSoul(skeleton,getUUID())){entityData.set(IMPACT_POINT,position().toVector3f());entityData.set(IMPACT,1);entityData.set(NORMAL,new Vector3f(0,1,0));setDeltaMovement(Vec3.ZERO);}
            else{mother.abandonSoul(skeleton,getUUID());discard();}
        }
    }
    @Override public boolean isPickable(){return false;}
    @Override public AABB getBoundingBoxForCulling(){return getBoundingBox().inflate(impacted()?impactRadius()+1:1.2);}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return new ClientboundAddEntityPacket(this);}
    @Override protected void addAdditionalSaveData(CompoundTag n){
        if(owner!=null)n.putUUID("Owner",owner);if(source!=null)n.putUUID("Source",source);if(target!=null)n.putUUID("Target",target);if(skeleton!=null)n.putUUID("Skeleton",skeleton);
        n.putInt("Kind",kind());n.putBoolean("Evolved",evolved());n.putInt("Age",age());n.putInt("Impact",impactAge());n.putFloat("Damage",damage);n.putInt("MissingTarget",missingTargetTicks);
        saveVector(n,"Start",start());saveVector(n,"Direction",flightDirection());saveVector(n,"Normal",impactNormal());saveVector(n,"ImpactPoint",impactPosition());
    }
    @Override protected void readAdditionalSaveData(CompoundTag n){
        owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;source=n.hasUUID("Source")?n.getUUID("Source"):null;target=n.hasUUID("Target")?n.getUUID("Target"):null;skeleton=n.hasUUID("Skeleton")?n.getUUID("Skeleton"):null;
        entityData.set(KIND,n.getInt("Kind")==HEAL_SOUL?HEAL_SOUL:ATTACK);entityData.set(EVO,n.getBoolean("Evolved"));entityData.set(AGE,Mth.clamp(n.getInt("Age"),0,80));entityData.set(IMPACT,Mth.clamp(n.getInt("Impact"),0,IMPACT_TICKS));
        damage=Mth.clamp(n.getFloat("Damage"),0,1000);missingTargetTicks=Mth.clamp(n.getInt("MissingTarget"),0,200);
        entityData.set(START,readVector(n,"Start").toVector3f());direction(readVector(n,"Direction"));entityData.set(NORMAL,readVector(n,"Normal").normalize().toVector3f());entityData.set(IMPACT_POINT,readVector(n,"ImpactPoint").toVector3f());
    }
    private static void saveVector(CompoundTag n,String key,Vec3 value){n.putDouble(key+"X",value.x);n.putDouble(key+"Y",value.y);n.putDouble(key+"Z",value.z);}
    private static Vec3 readVector(CompoundTag n,String key){return new Vec3(n.getDouble(key+"X"),n.getDouble(key+"Y"),n.getDouble(key+"Z"));}
}
