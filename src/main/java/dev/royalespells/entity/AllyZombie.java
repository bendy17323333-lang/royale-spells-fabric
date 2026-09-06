package dev.royalespells.entity;

import dev.royalespells.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.UUID;

public class AllyZombie extends ZombieEntity implements Summoned {
    private UUID owner;
    private int life=600;
    private boolean deploymentPlayed;
    private int lastFootstepAge=-8;
    private static final net.minecraft.entity.data.TrackedData<Boolean> CLONED=net.minecraft.entity.data.DataTracker.registerData(AllyZombie.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(){super.initDataTracker();dataTracker.startTracking(CLONED,false);}
    public boolean hero;
    public long nextReroll;
    public AllyZombie(EntityType<? extends ZombieEntity> type,World world) { super(type,world);experiencePoints=0; }
    @Override protected void initGoals() {
        goalSelector.add(0,new SwimGoal(this));
        goalSelector.add(2,new RageMeleeGoal(this));
        goalSelector.add(7,new WanderAroundFarGoal(this,0.65));
        goalSelector.add(8,new LookAroundGoal(this));
    }
    @Override protected boolean burnsInDaylight() { return false; }
    @Override protected boolean canConvertInWater() { return false; }
    @Override protected boolean isDisallowedInPeaceful(){return false;}
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean shouldDropXp() { return false; }
    private boolean barbarian(){return getType()==RoyaleSpells.BARBARIAN;}
    private boolean human(){return barbarian()||getType()==RoyaleSpells.RECRUIT;}
    @Override protected SoundEvent getAmbientSound(){return human()?null:super.getAmbientSound();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return barbarian()?null:human()?SoundEvents.ENTITY_PLAYER_HURT:super.getHurtSound(source);}
    @Override protected SoundEvent getDeathSound(){return barbarian()?UnitSounds.BARBARIAN_DEATH:human()?SoundEvents.ENTITY_PLAYER_DEATH:super.getDeathSound();}
    @Override public float getSoundPitch(){return human()?1:super.getSoundPitch();}
    @Override public SoundCategory getSoundCategory(){return human()?SoundCategory.NEUTRAL:super.getSoundCategory();}
    @Override protected void playStepSound(BlockPos pos,BlockState state){
        if(barbarian()){
            if(!getWorld().isClient && age-lastFootstepAge>=8){
                lastFootstepAge=age;playSound(UnitSounds.BARBARIAN_STEP,.12f,1);
            }
        }
        else if(human())playSound(state.getSoundGroup().getStepSound(),.15f,1);
        else super.playStepSound(pos,state);
    }
    @Override public boolean tryAttack(Entity target){
        boolean hit=super.tryAttack(target);
        if(hit&&barbarian())playSound(UnitSounds.BARBARIAN_ATTACK,.8f,1);
        return hit;
    }
    @Override public boolean damage(DamageSource source,float amount) {
        if(!getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker && SpellEngine.friendly(owner,attacker)) return false;
        return super.damage(source,amount);
    }
    @Override public void tick() {
        super.tick();
        if(!getWorld().isClient) {
            if(!deploymentPlayed){deploymentPlayed=true;if(barbarian()&&!isClone())playSound(UnitSounds.BARBARIAN_DEPLOY,.8f,1);}
            if(--life<=0) {discard();return;} SpellEngine.unitTick(this,owner);
        }
    }
    public UUID ownerId(){return owner;}
    public boolean isClone(){return dataTracker.get(CLONED);}
    public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;dataTracker.set(CLONED,clone);setPersistent();setCanPickUpLoot(false);}
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);if(owner!=null) nbt.putUuid("SpellOwner",owner);
        nbt.putInt("SpellLife",life);nbt.putBoolean("SpellClone",isClone());nbt.putBoolean("Hero",hero);nbt.putLong("NextReroll",nextReroll);
        nbt.putBoolean("DeploymentPlayed",deploymentPlayed);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);owner=nbt.containsUuid("SpellOwner")?nbt.getUuid("SpellOwner"):null;
        life=nbt.getInt("SpellLife");dataTracker.set(CLONED,nbt.getBoolean("SpellClone"));hero=nbt.getBoolean("Hero");nextReroll=nbt.getLong("NextReroll");
        deploymentPlayed=!nbt.contains("DeploymentPlayed")||nbt.getBoolean("DeploymentPlayed");
    }
}



