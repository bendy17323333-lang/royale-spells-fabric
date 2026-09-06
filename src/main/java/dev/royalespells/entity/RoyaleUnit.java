package dev.royalespells.entity;
import dev.royalespells.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.UUID;

/** The retained Barbarian Hut. No troop movement or looking-around goals. */
public class RoyaleUnit extends PathAwareEntity implements Summoned {
    private static final TrackedData<Float> FACING=DataTracker.registerData(RoyaleUnit.class,TrackedDataHandlerRegistry.FLOAT);
    private UUID owner;private int life=600,spawnClock;private boolean deathSpawned;private float power=1;
    public RoyaleUnit(EntityType<? extends PathAwareEntity> type,World world){super(type,world);experiencePoints=0;}
    public String kind(){return "barbarian_hut";}
    public boolean building(){return true;}
    public static DefaultAttributeContainer.Builder attributes(){return createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,65).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1);}
    @Override protected void initDataTracker(DataTracker.Builder builder){super.initDataTracker(builder);builder.add(FACING,0f);}
    public void lockFacing(float yaw){dataTracker.set(FACING,yaw);setYaw(yaw);setHeadYaw(yaw);setBodyYaw(yaw);}
    @Override protected void initGoals(){}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropXp(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override protected void pushAway(Entity other){}
    @Override public void travel(Vec3d input){setVelocity(Vec3d.ZERO);}
    @Override public boolean tryAttack(Entity target){return false;}
    @Override public boolean damage(DamageSource source,float amount){if(source.getAttacker() instanceof LivingEntity attacker&&SpellEngine.friendly(owner,attacker))return false;return super.damage(source,amount);}
    @Override public void tick(){
        super.tick();float yaw=dataTracker.get(FACING);setYaw(yaw);setHeadYaw(yaw);setBodyYaw(yaw);prevBodyYaw=yaw;
        if(getWorld().isClient||!isAlive())return;
        if(--life<=0){onDeath(getDamageSources().generic());discard();return;}
        setVelocity(Vec3d.ZERO);getNavigation().stop();setTarget(null);
        if(!hasStatusEffect(RoyaleSpells.STUN)&&spawnClock++%300==0)spawnWave(3);
    }
    public void spawnWave(int count){
        if(!(getWorld() instanceof ServerWorld world))return;
        double angle=Math.toRadians(getYaw());Vec3d forward=new Vec3d(-Math.sin(angle),0,Math.cos(angle));
        for(int i=0;i<count;i++){
            double side=(i-(count-1)*.5)*.8;
            Vec3d at=SpellEngine.ground(world,getPos().add(forward.multiply(2.7)).add(forward.z*side,0,-forward.x*side));
            SpellEngine.empower(SpellEngine.summon(world,owner,at,"barbarian",false),power);
        }
        playSound(SoundEvents.BLOCK_WOODEN_DOOR_OPEN,.6f,.9f);
    }
    @Override public void onDeath(DamageSource source){if(!getWorld().isClient&&!deathSpawned){deathSpawned=true;spawnWave(1);}super.onDeath(source);}
    public UUID ownerId(){return owner;}public boolean isClone(){return false;}
    public void setPower(float power){this.power=power;}public float power(){return power;}public int remainingLife(){return life;}
    public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;setPersistent();setCanPickUpLoot(false);}
    @Override public void writeCustomDataToNbt(NbtCompound nbt){super.writeCustomDataToNbt(nbt);if(owner!=null)nbt.putUuid("SpellOwner",owner);nbt.putInt("SpellLife",life);nbt.putInt("SpawnClock",spawnClock);nbt.putBoolean("DeathSpawned",deathSpawned);nbt.putFloat("TroopPower",power);nbt.putFloat("HutFacing",dataTracker.get(FACING));}
    @Override public void readCustomDataFromNbt(NbtCompound nbt){super.readCustomDataFromNbt(nbt);owner=nbt.containsUuid("SpellOwner")?nbt.getUuid("SpellOwner"):null;life=nbt.contains("SpellLife")?nbt.getInt("SpellLife"):600;spawnClock=nbt.getInt("SpawnClock");deathSpawned=nbt.getBoolean("DeathSpawned");power=nbt.contains("TroopPower")?nbt.getFloat("TroopPower"):1;dataTracker.set(FACING,nbt.getFloat("HutFacing"));}
}
