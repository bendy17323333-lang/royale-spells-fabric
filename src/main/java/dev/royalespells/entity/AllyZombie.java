package dev.royalespells.entity;
import dev.royalespells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public class AllyZombie extends Zombie implements Summoned {
    private UUID owner;
    private int life=600;
    private boolean deploymentPlayed;
    private int lastFootstepAge=-8;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CLONED=net.minecraft.network.syncher.SynchedEntityData.defineId(AllyZombie.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> CARD_SHIELD=net.minecraft.network.syncher.SynchedEntityData.defineId(AllyZombie.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(CLONED,false);builder.define(CARD_SHIELD,-1f);}
    public float cardShield(){return entityData.get(CARD_SHIELD);}
    public void cardShield(float value){entityData.set(CARD_SHIELD,Math.max(0,value));}
    public boolean hero;
    public long nextReroll;
    public AllyZombie(EntityType<? extends Zombie> type,Level world) { super(type,world);xpReward=0; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RageMeleeGoal(this));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,0.65));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean shouldDropExperience() { return false; }
    private boolean barbarian(){return getType()==RoyaleSpells.BARBARIAN;}
    private boolean human(){return barbarian()||getType()==RoyaleSpells.RECRUIT;}
    @Override protected SoundEvent getAmbientSound(){return human()?null:super.getAmbientSound();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return barbarian()?null:human()?SoundEvents.PLAYER_HURT:super.getHurtSound(source);}
    @Override protected SoundEvent getDeathSound(){return barbarian()?UnitSounds.BARBARIAN_DEATH:human()?SoundEvents.PLAYER_DEATH:super.getDeathSound();}
    @Override public float getVoicePitch(){return human()?1:super.getVoicePitch();}
    @Override public SoundSource getSoundSource(){return human()?SoundSource.NEUTRAL:super.getSoundSource();}
    @Override protected void playStepSound(BlockPos pos,BlockState state){
        if(barbarian()){
            if(!level().isClientSide && tickCount-lastFootstepAge>=8){
                lastFootstepAge=tickCount;playSound(UnitSounds.BARBARIAN_STEP,.12f,1);
            }
        }
        else if(human())playSound(state.getSoundType().getStepSound(),.15f,1);
        else super.playStepSound(pos,state);
    }
    @Override public boolean doHurtTarget(Entity target){
        boolean hit=super.doHurtTarget(target);
        if(hit&&barbarian())playSound(UnitSounds.BARBARIAN_ATTACK,.8f,1);
        return hit;
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!level().isClientSide && source.getEntity() instanceof LivingEntity attacker && SpellEngine.friendly(owner,attacker)) return false;
        // A CR shield absorbs the complete breaking hit; its excess damage does
        // not spill into health as vanilla Absorption would. Native Iron recruits
        // retain their original absorption/armor profile.
        if(!level().isClientSide&&amount>0&&cardShield()>0&&!isInvulnerableTo(source)&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){
            cardShield(cardShield()-amount);hurtTime=10;
            if(cardShield()==0){setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,net.minecraft.world.item.ItemStack.EMPTY);playSound(SoundEvents.SHIELD_BREAK,.55f,1);}
            return true;
        }
        return super.hurt(source,amount);
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide) {
            if(!deploymentPlayed){deploymentPlayed=true;if(barbarian()&&!isClone())playSound(UnitSounds.BARBARIAN_DEPLOY,.8f,1);}
            if(--life<=0) {discard();return;} SpellEngine.unitTick(this,owner);
        }
    }
    public UUID ownerId(){return owner;}
    public boolean isClone(){return entityData.get(CLONED);}
    public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;entityData.set(CLONED,clone);setPersistenceRequired();setCanPickUpLoot(false);}
    @Override public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);if(owner!=null) nbt.putUUID("SpellOwner",owner);
        nbt.putInt("SpellLife",life);nbt.putBoolean("SpellClone",isClone());nbt.putBoolean("Hero",hero);nbt.putLong("NextReroll",nextReroll);
        nbt.putBoolean("DeploymentPlayed",deploymentPlayed);
        nbt.putFloat("RoyaleCardShield",cardShield());
    }
    @Override public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);owner=nbt.hasUUID("SpellOwner")?nbt.getUUID("SpellOwner"):null;
        life=nbt.getInt("SpellLife");entityData.set(CLONED,nbt.getBoolean("SpellClone"));hero=nbt.getBoolean("Hero");nextReroll=nbt.getLong("NextReroll");
        deploymentPlayed=!nbt.contains("DeploymentPlayed")||nbt.getBoolean("DeploymentPlayed");
        entityData.set(CARD_SHIELD,nbt.contains("RoyaleCardShield")?nbt.getFloat("RoyaleCardShield"):-1f);
    }
}


