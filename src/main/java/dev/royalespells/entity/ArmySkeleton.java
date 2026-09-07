package dev.royalespells.entity;

import dev.royalespells.*;
import dev.royalespells.army.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class ArmySkeleton extends AllySkeleton {
    private static final EntityDataAccessor<Boolean> GENERAL=SynchedEntityData.defineId(ArmySkeleton.class,EntityDataSerializers.BOOLEAN),GHOST=SynchedEntityData.defineId(ArmySkeleton.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SHIELD=SynchedEntityData.defineId(ArmySkeleton.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(ArmySkeleton.class,EntityDataSerializers.INT),DISSOLVE=SynchedEntityData.defineId(ArmySkeleton.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRIKE=SynchedEntityData.defineId(ArmySkeleton.class,EntityDataSerializers.INT);
    private UUID army,strikeTarget;private int ghostFlash,nextAttack;private boolean neutralArmy;
    public boolean neutralArmy(){return neutralArmy;}
    public void setNeutralArmy(){neutralArmy=true;}
    public boolean blocksConversionKnockback(){return ghost()||!general()&&getHealth()<=0&&supported();}
    public ArmySkeleton(EntityType<? extends Skeleton> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(GENERAL,false);b.define(GHOST,false);b.define(SHIELD,0f);b.define(AGE,0);b.define(DISSOLVE,0);b.define(STRIKE,0);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true){
            @Override protected void checkAndPerformAttack(LivingEntity target){if(age()>=nextAttack&&entityData.get(STRIKE)==0&&isWithinMeleeAttackRange(target)&&getSensing().hasLineOfSight(target))beginStrike(target);}
            @Override public boolean canUse(){return age()>18&&dissolve()==0&&super.canUse();}
            @Override public boolean canContinueToUse(){return dissolve()==0&&super.canContinueToUse();}
        });
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    public boolean general(){return entityData.get(GENERAL);}public boolean ghost(){return entityData.get(GHOST);}public float shield(){return entityData.get(SHIELD);}public int age(){return entityData.get(AGE);}public int dissolve(){return entityData.get(DISSOLVE);}public UUID armyId(){return army;}
    public void enlist(UUID owner,UUID army,boolean general,float health,float attack,int life){
        setup(owner,life,false);this.army=army;entityData.set(GENERAL,general);entityData.set(SHIELD,general?health:0f);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);setHealth(health);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attack);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.26);for(var slot:EquipmentSlot.values())setDropChance(slot,0);
        if(!general){var sword=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD);sword.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);setItemSlot(EquipmentSlot.MAINHAND,sword);}
    }
    public boolean supported(){return level() instanceof ServerLevel world&&ownerId()!=null&&ArmyLedger.get(world.getServer()).valid(ownerId(),army,ArmyLedger.now(world.getServer()));}
    public int strikeDuration(){return hasEffect(RoyaleSpells.RAGED)?(general()?13:12):(general()?18:16);}
    public float strikeProgress(float partial){int strike=entityData.get(STRIKE);return strike==0?0:Math.min(1,(strike+partial)/strikeDuration());}
    public boolean beginStrike(LivingEntity target){
        if(level().isClientSide||age()<18||dissolve()>0||entityData.get(STRIKE)!=0||age()<nextAttack||!SpellEngine.enemy(ownerId(),target)||hasEffect(RoyaleSpells.FROZEN)||hasEffect(RoyaleSpells.STUN))return false;
        strikeTarget=target.getUUID();entityData.set(STRIKE,1);nextAttack=age()+(hasEffect(RoyaleSpells.RAGED)?(general()?15:16):(general()?20:22));return true;
    }
    @Override public boolean isWithinMeleeAttackRange(LivingEntity target){return general()?getBoundingBox().inflate(1.35,.2,1.35).intersects(target.getBoundingBox()):super.isWithinMeleeAttackRange(target);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(!level().isClientSide&&source.getEntity() instanceof LivingEntity attacker&&SpellEngine.friendly(ownerId(),attacker))return false;
        if(amount<=0||dissolve()>0)return false;
        if(!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if(ghost())return false;
            if(general()&&shield()>0){entityData.set(SHIELD,Math.max(0,shield()-amount));hurtTime=10;if(shield()==0)playSound(ArmySounds.SHIELD_BREAK,.7f,1);else playSound(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,.2f,1.35f);return true;}
        }
        return super.hurt(source,amount);
    }
    @Override public void die(DamageSource source){
        if(!level().isClientSide&&!general()&&!ghost()&&supported()&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            setHealth(getMaxHealth());entityData.set(GHOST,true);setItemSlot(EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY);ghostFlash=18;
            playSound(ArmySounds.CHANGE,.32f,1.05f);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.22);
            if(level() instanceof ServerLevel world)for(var mob:world.getEntitiesOfClass(Mob.class,getBoundingBox().inflate(32)))if(mob.getTarget()==this)mob.setTarget(null);
            return;
        }
        finish();super.die(source);
    }
    private void finish(){if(general()&&ownerId()!=null&&level() instanceof ServerLevel world)ArmyLedger.get(world.getServer()).finish(ownerId(),army);}
    @Override public void remove(RemovalReason reason){if(reason.shouldDestroy())finish();super.remove(reason);}
    @Override public boolean doHurtTarget(Entity target){
        if(!(target instanceof LivingEntity victim)||age()<18||dissolve()>0||hasEffect(RoyaleSpells.STUN)||hasEffect(RoyaleSpells.FROZEN)||!SpellEngine.enemy(ownerId(),victim))return false;
        if(victim instanceof net.minecraft.world.entity.player.Player&&level() instanceof ServerLevel w&&!w.getServer().isPvpAllowed())return false;
        return victim.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE));
    }
    @Override public boolean isAttackable(){return !ghost()&&dissolve()==0&&super.isAttackable();}
    @Override public boolean canBeSeenAsEnemy(){return !ghost()&&dissolve()==0&&super.canBeSeenAsEnemy();}
    @Override public boolean isPushable(){return !ghost()&&super.isPushable();}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        if(hasEffect(RoyaleSpells.FROZEN)&&supported())return;
        entityData.set(AGE,age()+1);if(ghostFlash>0)ghostFlash--;
        int strike=entityData.get(STRIKE);
        if(strike>0){
            if(dissolve()>0||hasEffect(RoyaleSpells.FROZEN)||hasEffect(RoyaleSpells.STUN)){entityData.set(STRIKE,0);strikeTarget=null;}
            else {
                if(strike==Math.round(strikeDuration()*.44f)&&level() instanceof ServerLevel world&&world.getEntity(strikeTarget) instanceof LivingEntity target&&target.isAlive()&&isWithinMeleeAttackRange(target)&&getSensing().hasLineOfSight(target)){
                    doHurtTarget(target);playSound(ArmySounds.ATTACK,.18f,1.05f);
                }
                entityData.set(STRIKE,strike>=strikeDuration()?0:strike+1);
            }
        }
        if(!supported()){
            setTarget(null);getNavigation().stop();entityData.set(DISSOLVE,dissolve()+1);if(dissolve()>=20)discard();
        }
        if(age()<18||dissolve()>0){getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);}
    }
    @Override protected SoundEvent getAmbientSound(){return null;}
    @Override protected SoundEvent getDeathSound(){return general()?ArmySounds.DEATH:ArmySounds.SKELETON_DEATH;}
    @Override protected SoundEvent getHurtSound(DamageSource s){return null;}
    @Override protected void playStepSound(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState s){if(!ghost()&&random.nextInt(3)==0)playSound(ArmySounds.STEP,.08f,1.05f);}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(army!=null)n.putUUID("Army",army);n.putBoolean("NeutralArmy",neutralArmy);n.putBoolean("General",general());n.putBoolean("Ghost",ghost());n.putFloat("ArmyShield",shield());n.putInt("ArmyAge",age());n.putInt("ArmyDissolve",dissolve());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);army=n.hasUUID("Army")?n.getUUID("Army"):null;neutralArmy=n.getBoolean("NeutralArmy");entityData.set(GENERAL,n.getBoolean("General"));entityData.set(GHOST,n.getBoolean("Ghost"));entityData.set(SHIELD,n.getFloat("ArmyShield"));entityData.set(AGE,n.getInt("ArmyAge"));entityData.set(DISSOLVE,n.getInt("ArmyDissolve"));}
}
