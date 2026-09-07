package dev.royalespells.entity;
import dev.royalespells.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** The retained Barbarian Hut. No troop movement or looking-around goals. */
public class RoyaleUnit extends PathfinderMob implements Summoned {
    private static final EntityDataAccessor<Float> FACING=SynchedEntityData.defineId(RoyaleUnit.class,EntityDataSerializers.FLOAT);
    private UUID owner;private int life=600,spawnClock;private boolean deathSpawned;private float power=1;
    public RoyaleUnit(EntityType<? extends PathfinderMob> type,Level world){super(type,world);xpReward=0;}
    public String kind(){return "barbarian_hut";}
    public boolean building(){return true;}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,65).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(FACING,0f);}
    public void lockFacing(float yaw){entityData.set(FACING,yaw);setYRot(yaw);setYHeadRot(yaw);setYBodyRot(yaw);}
    @Override protected void registerGoals(){}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override protected void doPush(Entity other){}
    @Override public void travel(Vec3 input){setDeltaMovement(Vec3.ZERO);}
    @Override public boolean doHurtTarget(Entity target){return false;}
    @Override public boolean hurt(DamageSource source,float amount){if(source.getEntity() instanceof LivingEntity attacker&&SpellEngine.friendly(owner,attacker))return false;return super.hurt(source,amount);}
    @Override public void tick(){
        super.tick();float yaw=entityData.get(FACING);setYRot(yaw);setYHeadRot(yaw);setYBodyRot(yaw);yBodyRotO=yaw;
        if(level().isClientSide||!isAlive())return;
        if(tickCount==1)SpellSounds.play(level(),position(),"barbarian_hut","deploy");
        if(--life<=0){die(damageSources().generic());discard();return;}
        setDeltaMovement(Vec3.ZERO);getNavigation().stop();setTarget(null);
        if(!hasEffect(RoyaleSpells.STUN)&&spawnClock++%300==0)spawnWave(3);
    }
    public void spawnWave(int count){
        if(!(level() instanceof ServerLevel world))return;
        double angle=Math.toRadians(getYRot());Vec3 forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));
        for(int i=0;i<count;i++){
            double side=(i-(count-1)*.5)*.8;
            Vec3 at=SpellEngine.ground(world,position().add(forward.scale(2.7)).add(forward.z*side,0,-forward.x*side));
            var unit=SpellEngine.summon(world,owner,at,"barbarian",false);SpellEngine.empower(unit,power);
            IronSpellSystem.summon(unit,owner,getPersistentData().getString("RoyaleIronSpell"),getPersistentData().getInt("RoyaleIronLevel"));
        }

    }
    @Override public void die(DamageSource source){if(!level().isClientSide&&!deathSpawned){deathSpawned=true;SpellSounds.play(level(),position(),"barbarian_hut","end");spawnWave(1);}super.die(source);}
    public UUID ownerId(){return owner;}public boolean isClone(){return false;}
    public void setPower(float power){this.power=power;}public float power(){return power;}public int remainingLife(){return life;}
    public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;setPersistenceRequired();setCanPickUpLoot(false);}
    @Override public void addAdditionalSaveData(CompoundTag nbt){super.addAdditionalSaveData(nbt);if(owner!=null)nbt.putUUID("SpellOwner",owner);nbt.putInt("SpellLife",life);nbt.putInt("SpawnClock",spawnClock);nbt.putBoolean("DeathSpawned",deathSpawned);nbt.putFloat("TroopPower",power);nbt.putFloat("HutFacing",entityData.get(FACING));}
    @Override public void readAdditionalSaveData(CompoundTag nbt){super.readAdditionalSaveData(nbt);owner=nbt.hasUUID("SpellOwner")?nbt.getUUID("SpellOwner"):null;life=nbt.contains("SpellLife")?nbt.getInt("SpellLife"):600;spawnClock=nbt.getInt("SpawnClock");deathSpawned=nbt.getBoolean("DeathSpawned");power=nbt.contains("TroopPower")?nbt.getFloat("TroopPower"):1;entityData.set(FACING,nbt.getFloat("HutFacing"));}
}
