// SPDX-License-Identifier: MIT
package dev.royalespells.entity;

import dev.royalespells.*;
import dev.royalespells.pause.ElectricPause;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** A ranged ground troop. All clocks and healing authorizations belong to one entity.
 * August 2026 Evolution: ONLY the four skeletons from the first successful wave
 * can return a soul. It is not the obsolete global skeleton-death aura. */
public final class RoyalWitch extends PathfinderMob implements Summoned {
    public static final int LIFE_TICKS=1800, DEPLOY_TICKS=20, ATTACK_TICKS=22,
        WINDUP_TICKS=14, SUMMON_TICKS=140, SOUL_TICKS=20;
    public static final double RANGE=5.5, SPLASH_RADIUS=1.5;
    public static final String MOTHER_TAG="RoyaleWitchMother";
    private static final EntityDataAccessor<Boolean> EVO=SynchedEntityData.defineId(RoyalWitch.class,EntityDataSerializers.BOOLEAN),
        CLONE=SynchedEntityData.defineId(RoyalWitch.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CARD_LEVEL=SynchedEntityData.defineId(RoyalWitch.class,EntityDataSerializers.INT),
        ATTACK_AGE=SynchedEntityData.defineId(RoyalWitch.class,EntityDataSerializers.INT),
        SUMMON_AGE=SynchedEntityData.defineId(RoyalWitch.class,EntityDataSerializers.INT),
        HEAL_FLASH=SynchedEntityData.defineId(RoyalWitch.class,EntityDataSerializers.INT);
    private UUID owner;
    private int life=LIFE_TICKS, deploy=DEPLOY_TICKS, summonClock, waves, soulReturns;
    private double actionFraction;
    private boolean deploymentSound;
    // First-wave members and pending soul UUIDs are persisted. A duplicated
    // death event, unrelated skeleton, cloned skeleton or fake soul cannot heal.
    private final Set<UUID> firstWave=new HashSet<>();
    private final Map<UUID,UUID> pendingSouls=new HashMap<>();

    public RoyalWitch(EntityType<? extends PathfinderMob> type,Level world){super(type,world);xpReward=0;setup(getUUID(),LIFE_TICKS,false);}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes()
        .add(Attributes.MAX_HEALTH,CardBalance.convert(839)).add(Attributes.ATTACK_DAMAGE,CardBalance.convert(135))
        .add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(5,new FollowSummonerGoal(this));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(EVO,false);entityData.define(CLONE,false);
        entityData.define(CARD_LEVEL,11);entityData.define(ATTACK_AGE,0);entityData.define(SUMMON_AGE,0);entityData.define(HEAL_FLASH,0);}
    @Override public UUID ownerId(){return owner;}
    @Override public boolean isClone(){return entityData.get(CLONE);}
    public void setEvolved(boolean value){entityData.set(EVO,value);}
    public boolean evolved(){return entityData.get(EVO);}
    public int cardLevel(){return entityData.get(CARD_LEVEL);}
    public int attackAge(){return entityData.get(ATTACK_AGE);}
    public int summonAge(){return entityData.get(SUMMON_AGE);}
    public int healFlash(){return entityData.get(HEAL_FLASH);}
    public int waveCount(){return waves;}
    public int healedSouls(){return soulReturns;}
    public int pendingSoulCount(){return pendingSouls.size();}
    public int summonClock(){return summonClock;}
    public Set<UUID> eligibleSkeletons(){return Set.copyOf(firstWave);}
    public float castProgress(float partial){return attackAge()==0?0:Mth.clamp((attackAge()+partial)/WINDUP_TICKS,0,1);}
    public float attackProgress(float partial){return attackAge()==0?0:(attackAge()+partial)/ATTACK_TICKS;}
    public float initialHealth(){return CardBalance.convert(CardBalance.value(cardLevel(),839,921));}
    public float soulHealing(){return CardBalance.convert(CardBalance.value(cardLevel(),153,168));}
    public float evolutionHealthCap(){return CardBalance.convert(CardBalance.value(cardLevel(),1451,1593));}
    public float projectileDamage(){return (float)getAttributeValue(Attributes.ATTACK_DAMAGE);}
    public Vec3 muzzle(){
        Vec3 front=SpellEngine.horizontal(getViewVector(1));
        // Matches the actual LEFT palm at release, after the taller native rig.
        // Right-hand staff is a planted support, never the muzzle.
        return position().add(0,1.382,0).add(front.scale(.548)).add(front.z*.344,0,-front.x*.344);
    }
    public boolean inAttackRange(LivingEntity victim){Vec3 d=victim.position().subtract(position());return d.horizontalDistanceSqr()<=RANGE*RANGE&&Math.abs(d.y)<=6;}
    public void configure(UUID player,boolean evolved,int level){
        setup(player,LIFE_TICKS,false);entityData.set(EVO,evolved);entityData.set(CARD_LEVEL,level>=12?12:11);
        PersistentState.of(this).putInt(CardBalance.TAG,cardLevel());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(initialHealth());setHealth(initialHealth());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(CardBalance.convert(CardBalance.value(cardLevel(),135,148)));
    }
    @Override public void setup(UUID player,int duration,boolean clone){owner=player;life=duration;entityData.set(CLONE,clone);
        setPersistenceRequired();setCanPickUpLoot(false);if(clone){firstWave.clear();pendingSouls.clear();getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);setHealth(1);}}
    public boolean paused(){return hasEffect(RoyaleSpells.STUN)||hasEffect(RoyaleSpells.FROZEN)||ElectricPause.active(this);}
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive())return;
        if(--life<=0){discard();return;}
        if(!deploymentSound){deploymentSound=true;WitchSounds.play(this,evolved()?WitchSounds.EVO_DEPLOY:WitchSounds.DEPLOY,.62f,1);if(evolved()){WitchSounds.play(this,WitchSounds.EVO_VOICE,.52f,1);EvolutionBurst.deploy((ServerLevel)level(),position(),1.2f);}}
        if(paused()){navigation.stop();setDeltaMovement(Vec3.ZERO);return;}
        double speed=hasEffect(RoyaleSpells.SNOWBOUND)?.65:hasEffect(RoyaleSpells.CARD_SLOW)?.95:1;
        if(hasEffect(RoyaleSpells.RAGED)||hasEffect(RoyaleSpells.CARD_SPEED))speed*=1.35;
        actionFraction+=speed;int steps=Math.min(2,(int)actionFraction);actionFraction-=steps;
        for(int i=0;i<steps;i++)actionTick();
    }
    private void actionTick(){
        if(healFlash()>0)entityData.set(HEAL_FLASH,healFlash()-1);
        if(summonAge()>0)entityData.set(SUMMON_AGE,summonAge()>=24?0:summonAge()+1);
        if(deploy>0){if(--deploy==0)summonWave();navigation.stop();return;}
        if(++summonClock>=SUMMON_TICKS){summonClock=0;summonWave();}
        if(!isNoAi())SummonOrders.tick(this,owner);
        LivingEntity victim=getTarget();
        if(!validEnemy(victim)){entityData.set(ATTACK_AGE,0);return;}
        getLookControl().setLookAt(victim,45,45);
        float yaw=(float)Math.toDegrees(Math.atan2(victim.getZ()-getZ(),victim.getX()-getX()))-90;
        setYRot(yaw);setYBodyRot(yaw);setYHeadRot(yaw);
        if(inAttackRange(victim)&&hasLineOfSight(victim)){
            navigation.stop();setXxa(0);setZza(0);
            int age=attackAge()+1;if(age>ATTACK_TICKS)age=1;entityData.set(ATTACK_AGE,age);
            if(age==WINDUP_TICKS){WitchBolt.launch((ServerLevel)level(),this,victim);WitchSounds.play(this,evolved()?WitchSounds.EVO_ATTACK:WitchSounds.ATTACK,.46f,1);}
        }else{
            entityData.set(ATTACK_AGE,0);
            if(!isNoAi()&&!hasEffect(RoyaleSpells.ROOTED)&&tickCount%5==0)navigation.moveTo(victim,1);
        }
    }
    public boolean validEnemy(LivingEntity victim){return victim!=null&&victim!=this&&victim.isAttackable()&&SpellEngine.enemy(owner,victim)
        &&(!(victim instanceof net.minecraft.world.entity.player.Player)||level().getServer().isPvpAllowed());}
    /** Four independent collision-checked points; failed spawns are not eligible souls. */
    public void summonWave(){
        if(!(level() instanceof ServerLevel world)||!isAlive())return;
        boolean first=waves==0;int spawned=0;
        Vec3 forward=SpellEngine.horizontal(getViewVector(1));Vec3 side=new Vec3(forward.z,0,-forward.x);
        for(int i=0;i<4;i++){
            if(!CardDeployment.capacity(world,owner,1))break;
            double lateral=(i%2==0?-1:1)*(i<2?.95:1.25),ahead=i<2?1.2:-.5;
            Vec3 at=SpellEngine.ground(world,position().add(forward.scale(ahead)).add(side.scale(lateral)));
            if(!world.hasChunkAt(BlockPos.containing(at))||!world.getWorldBorder().isWithinBounds(BlockPos.containing(at))
                ||!world.noCollision(AABB.ofSize(at.add(0,.7,0),.48,1.4,.48)))continue;
            Mob created=SpellEngine.summon(world,owner,at,"skeleton",false);if(!(created instanceof AllySkeleton skeleton))continue;
            CardBalance.apply(skeleton,"skeleton",false,cardLevel());skeleton.setup(owner,Math.min(life,1200),isClone());
            if(isClone()){skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);skeleton.setHealth(1);}
            PersistentState.of(skeleton).putUUID(SummonOrders.ESCORT,getUUID());
            skeleton.setTarget(getTarget());
            if(first&&evolved()&&!isClone()){
                firstWave.add(skeleton.getUUID());PersistentState.of(skeleton).putUUID(MOTHER_TAG,getUUID());
            }
            // A short burst at the actual spawn point, never screen-space.
            world.sendParticles(RoyaleSpells.GRAVE_MOTE,at.x,at.y+.08,at.z,9,.25,.16,.25,.02);spawned++;
        }
        // The first wave is a one-shot rule, even in a cramped space; later
        // waves cannot replenish missed first-wave healing authorizations.
        waves++;entityData.set(SUMMON_AGE,1);
        if(spawned>0)WitchSounds.play(this,evolved()?WitchSounds.EVO_SUMMON:WitchSounds.SUMMON,.52f,1);
    }
    public static void onSkeletonDeath(LivingEntity dead){
        if(!(dead instanceof AllySkeleton skeleton)||skeleton.isClone()||!(dead.level() instanceof ServerLevel world))return;
        CompoundTag tag=PersistentState.of(dead);if(!tag.hasUUID(MOTHER_TAG))return;
        if(world.getEntity(tag.getUUID(MOTHER_TAG)) instanceof RoyalWitch mother)mother.releaseSoul(skeleton);
    }
    private void releaseSoul(AllySkeleton dead){
        if(!evolved()||isClone()||!isAlive()||dead.isAlive()||!Objects.equals(owner,dead.ownerId())||!firstWave.contains(dead.getUUID())
            ||pendingSouls.containsKey(dead.getUUID()))return;
        WitchBolt soul=WitchBolt.soul((ServerLevel)level(),this,dead);if(soul==null)return;
        pendingSouls.put(dead.getUUID(),soul.getUUID());
        if(!level().addFreshEntity(soul))pendingSouls.remove(dead.getUUID());
    }
    /** Called only on a physical soul's arrival. UUID is consumed atomically. */
    public boolean acceptSoul(UUID skeleton,UUID soul){
        if(!isAlive()||!evolved()||isClone()||soulReturns>=4||!firstWave.contains(skeleton)
            ||!soul.equals(pendingSouls.get(skeleton)))return false;
        if(!(level() instanceof ServerLevel world)||world.getEntity(getUUID())!=this||!(world.getEntity(soul) instanceof WitchBolt orb)
            ||orb.kind()!=WitchBolt.HEAL_SOUL||orb.age()<SOUL_TICKS||orb.impacted()
            ||!getUUID().equals(orb.recipient())||!skeleton.equals(orb.skeletonId())
            ||orb.position().distanceToSqr(position().add(0,1.3,0))>.25)return false;
        firstWave.remove(skeleton);pendingSouls.remove(skeleton);soulReturns++;
        float health=Math.min(evolutionHealthCap(),getHealth()+soulHealing());
        // Vanilla clamps health to MAX_HEALTH. Raise only the earned overflow,
        // so a freshly deployed evolved witch starts with an honest full bar.
        if(health>getMaxHealth())getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth(health);entityData.set(HEAL_FLASH,10);WitchSounds.play(this,WitchSounds.HEAL,.48f,1);return true;
    }
    public void abandonSoul(UUID skeleton,UUID soul){if(soul.equals(pendingSouls.get(skeleton))){pendingSouls.remove(skeleton);firstWave.remove(skeleton);}}
    @Override public boolean hurt(DamageSource source,float amount){if(source.getEntity() instanceof LivingEntity e&&SpellEngine.friendly(owner,e))return false;return super.hurt(source,amount);}
    @Override public boolean doHurtTarget(Entity victim){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return WitchSounds.DEATH;}
    @Override public void addAdditionalSaveData(CompoundTag n){
        super.addAdditionalSaveData(n);if(owner!=null)n.putUUID("SpellOwner",owner);n.putInt("SpellLife",life);n.putBoolean("SpellClone",isClone());
        n.putBoolean("WitchEvolved",evolved());n.putInt("WitchLevel",cardLevel());n.putInt("Deploy",deploy);n.putInt("SummonClock",summonClock);
        n.putInt("Waves",waves);n.putInt("SoulReturns",soulReturns);n.putInt("AttackAge",attackAge());n.putInt("SummonAge",summonAge());
        n.putDouble("ActionFraction",actionFraction);n.putBoolean("DeploymentSound",deploymentSound);
        ListTag ids=new ListTag();for(UUID id:firstWave){CompoundTag row=new CompoundTag();row.putUUID("Skeleton",id);if(pendingSouls.containsKey(id))row.putUUID("Soul",pendingSouls.get(id));ids.add(row);}n.put("FirstWave",ids);
    }
    @Override public void readAdditionalSaveData(CompoundTag n){
        super.readAdditionalSaveData(n);owner=n.hasUUID("SpellOwner")?n.getUUID("SpellOwner"):getUUID();life=n.contains("SpellLife")?Mth.clamp(n.getInt("SpellLife"),1,LIFE_TICKS):LIFE_TICKS;
        if(n.contains("WitchEvolved"))entityData.set(EVO,n.getBoolean("WitchEvolved"));entityData.set(CLONE,n.getBoolean("SpellClone"));entityData.set(CARD_LEVEL,n.getInt("WitchLevel")>=12?12:11);
        deploy=Mth.clamp(n.getInt("Deploy"),0,DEPLOY_TICKS);summonClock=Mth.clamp(n.getInt("SummonClock"),0,SUMMON_TICKS-1);waves=Math.max(0,n.getInt("Waves"));soulReturns=Mth.clamp(n.getInt("SoulReturns"),0,4);
        entityData.set(ATTACK_AGE,Mth.clamp(n.getInt("AttackAge"),0,ATTACK_TICKS));entityData.set(SUMMON_AGE,Mth.clamp(n.getInt("SummonAge"),0,24));
        actionFraction=Mth.clamp(n.getDouble("ActionFraction"),0,.999);deploymentSound=n.getBoolean("DeploymentSound");firstWave.clear();pendingSouls.clear();
        if(evolved()&&!isClone())for(Tag value:n.getList("FirstWave",Tag.TAG_COMPOUND)){CompoundTag row=(CompoundTag)value;if(firstWave.size()>=4-soulReturns)break;if(!row.hasUUID("Skeleton"))continue;UUID id=row.getUUID("Skeleton");firstWave.add(id);if(row.hasUUID("Soul"))pendingSouls.put(id,row.getUUID("Soul"));}
    }
}
