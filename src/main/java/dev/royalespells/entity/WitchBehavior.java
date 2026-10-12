// SPDX-License-Identifier: MIT
package dev.royalespells.entity;

import dev.royalespells.*;
import dev.royalespells.pause.ElectricPause;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Shared combat, summoning and soul ledger. The host decides card vs resident policy.
 * This class has no Iron's dependency and never handles client rendering. */
public final class WitchBehavior {
    public static final int LIFE_TICKS=1800, DEPLOY_TICKS=20, ATTACK_TICKS=22,
        WINDUP_TICKS=14, SUMMON_TICKS=140, SOUL_TICKS=20;
    public static final double RANGE=5.5, SPLASH_RADIUS=1.5;
    public static final String MOTHER_TAG="RoyaleWitchMother";
    public record Data(EntityDataAccessor<Boolean> EVO,EntityDataAccessor<Boolean> CLONE,
                       EntityDataAccessor<Integer> CARD_LEVEL,EntityDataAccessor<Integer> ATTACK_AGE,
                       EntityDataAccessor<Integer> SUMMON_AGE,EntityDataAccessor<Integer> HEAL_FLASH){
        public static Data create(Class<? extends Entity> host){return new Data(
            SynchedEntityData.defineId(host,EntityDataSerializers.BOOLEAN),SynchedEntityData.defineId(host,EntityDataSerializers.BOOLEAN),
            SynchedEntityData.defineId(host,EntityDataSerializers.INT),SynchedEntityData.defineId(host,EntityDataSerializers.INT),
            SynchedEntityData.defineId(host,EntityDataSerializers.INT),SynchedEntityData.defineId(host,EntityDataSerializers.INT));}
        public void define(SynchedEntityData.Builder b){b.define(EVO,false);b.define(CLONE,false);b.define(CARD_LEVEL,11);b.define(ATTACK_AGE,0);b.define(SUMMON_AGE,0);b.define(HEAL_FLASH,0);}
    }
    private final PathfinderMob mob;
    private final Data keys;
    private final SynchedEntityData entityData;
    private final boolean resident;
    public WitchBehavior(PathfinderMob mob,Data keys,boolean resident){this.mob=mob;this.keys=keys;this.entityData=mob.getEntityData();this.resident=resident;}
    public boolean resident(){return resident;}
    public void setEvolved(boolean value){entityData.set(keys.EVO,value);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH,CardBalance.convert(839)).add(Attributes.ATTACK_DAMAGE,CardBalance.convert(135))
        .add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,24);}
    private UUID owner;
    private int life=LIFE_TICKS, deploy=DEPLOY_TICKS, summonClock, waves, soulReturns;
    private double actionFraction;
    private boolean deploymentSound;
    // First-wave members and pending soul UUIDs are persisted. A duplicated
    // death event, unrelated skeleton, cloned skeleton or fake soul cannot heal.
    private final Set<UUID> firstWave=new HashSet<>();
    private final Map<UUID,UUID> pendingSouls=new HashMap<>();

    public UUID ownerId(){return owner;}
    public boolean isClone(){return entityData.get(keys.CLONE);}
    public boolean evolved(){return entityData.get(keys.EVO);}
    public int cardLevel(){return entityData.get(keys.CARD_LEVEL);}
    public int attackAge(){return entityData.get(keys.ATTACK_AGE);}
    public int summonAge(){return entityData.get(keys.SUMMON_AGE);}
    public int healFlash(){return entityData.get(keys.HEAL_FLASH);}
    public int waveCount(){return waves;}
    public int healedSouls(){return soulReturns;}
    public int pendingSoulCount(){return pendingSouls.size();}
    public int summonClock(){return summonClock;}
    public Set<UUID> eligibleSkeletons(){return Set.copyOf(firstWave);}
    public float castProgress(float partial){return attackAge()==0?0:Mth.clamp((attackAge()+partial)/WINDUP_TICKS,0,1);}
    public float attackProgress(float partial){return attackAge()==0?0:(attackAge()+partial)/ATTACK_TICKS;}
    public float initialHealth(){return CardBalance.convert(CardBalance.value(cardLevel(),839));}
    public float soulHealing(){return CardBalance.convert(CardBalance.value(cardLevel(),153));}
    public float evolutionHealthCap(){return CardBalance.convert(CardBalance.value(cardLevel(),1451));}
    public float projectileDamage(){return (float)mob.getAttributeValue(Attributes.ATTACK_DAMAGE);}
    public Vec3 muzzle(){
        Vec3 front=SpellEngine.horizontal(mob.getViewVector(1));
        // Matches the actual LEFT palm at release, after the taller native rig.
        // Right-hand staff is a planted support, never the muzzle.
        return mob.position().add(0,1.382,0).add(front.scale(.548)).add(front.z*.344,0,-front.x*.344);
    }
    public boolean inAttackRange(LivingEntity victim){Vec3 d=victim.position().subtract(mob.position());return d.horizontalDistanceSqr()<=RANGE*RANGE&&Math.abs(d.y)<=6;}
    public void configure(UUID player,boolean evolved,int level){
        setup(player,LIFE_TICKS,false);entityData.set(keys.EVO,evolved);entityData.set(keys.CARD_LEVEL,CardLevels.unitLevel(level));
        mob.getPersistentData().putInt(CardBalance.TAG,cardLevel());
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(initialHealth());mob.setHealth(initialHealth());
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(CardBalance.convert(CardBalance.value(cardLevel(),135)));
    }
    public void setup(UUID player,int duration,boolean clone){owner=player;life=duration;entityData.set(keys.CLONE,clone);
        mob.setPersistenceRequired();mob.setCanPickUpLoot(false);if(clone){firstWave.clear();pendingSouls.clear();mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);mob.setHealth(1);}}
    public boolean paused(){return mob.hasEffect(RoyaleSpells.STUN)||mob.hasEffect(RoyaleSpells.FROZEN)||ElectricPause.active(mob);}
    public void tick(){
        if(mob.level().isClientSide||!mob.isAlive())return;
        if(!resident&&--life<=0){mob.discard();return;}
        if(!deploymentSound){deploymentSound=true;WitchSounds.play(mob,evolved()?WitchSounds.EVO_DEPLOY:WitchSounds.DEPLOY,.62f,1);if(evolved()){WitchSounds.play(mob,WitchSounds.EVO_VOICE,.52f,1);EvolutionBurst.deploy((ServerLevel)mob.level(),mob.position(),1.2f);}}
        if(paused()){mob.getNavigation().stop();mob.setDeltaMovement(Vec3.ZERO);return;}
        double speed=mob.hasEffect(RoyaleSpells.SNOWBOUND)?.65:mob.hasEffect(RoyaleSpells.CARD_SLOW)?.95:1;
        if(mob.hasEffect(RoyaleSpells.RAGED)||mob.hasEffect(RoyaleSpells.CARD_SPEED))speed*=1.35;
        actionFraction+=speed;int steps=Math.min(2,(int)actionFraction);actionFraction-=steps;
        for(int i=0;i<steps;i++)actionTick();
    }
    private void actionTick(){
        if(healFlash()>0)entityData.set(keys.HEAL_FLASH,healFlash()-1);
        if(summonAge()>0)entityData.set(keys.SUMMON_AGE,summonAge()>=24?0:summonAge()+1);
        if(deploy>0){if(--deploy==0&&!resident)summonWave();mob.getNavigation().stop();return;}
        if(resident){
            if(!mob.hasRestriction())mob.restrictTo(mob.blockPosition(),6);
            if(validEnemy(mob.getTarget())&&(waves==0||++summonClock>=SUMMON_TICKS)){summonClock=0;summonWave();}
        }else{
            if(++summonClock>=SUMMON_TICKS){summonClock=0;summonWave();}
            if(!mob.isNoAi())SummonOrders.tick(mob,owner);
        }
        LivingEntity victim=mob.getTarget();
        if(!validEnemy(victim)){entityData.set(keys.ATTACK_AGE,0);return;}
        mob.getLookControl().setLookAt(victim,45,45);
        float yaw=(float)Math.toDegrees(Math.atan2(victim.getZ()-mob.getZ(),victim.getX()-mob.getX()))-90;
        mob.setYRot(yaw);mob.setYBodyRot(yaw);mob.setYHeadRot(yaw);
        if(inAttackRange(victim)&&mob.hasLineOfSight(victim)){
            mob.getNavigation().stop();mob.setXxa(0);mob.setZza(0);
            int age=attackAge()+1;if(age>ATTACK_TICKS)age=1;entityData.set(keys.ATTACK_AGE,age);
            if(age==WINDUP_TICKS){WitchBolt.launch((ServerLevel)mob.level(),(WitchActor)mob,victim);WitchSounds.play(mob,evolved()?WitchSounds.EVO_ATTACK:WitchSounds.ATTACK,.46f,1);}
        }else{
            entityData.set(keys.ATTACK_AGE,0);
            if(!mob.isNoAi()&&!mob.hasEffect(RoyaleSpells.ROOTED)&&mob.tickCount%5==0)mob.getNavigation().moveTo(victim,1);
        }
    }
    public boolean friendly(Entity target){
        if(target==mob)return true;
        if(resident)return target instanceof WitchSkeleton || target instanceof WitchActor witch&&witch.behavior().resident();
        return target instanceof LivingEntity living&&SpellEngine.friendly(owner,living);
    }
    public boolean validEnemy(LivingEntity victim){
        if(victim==null||!victim.isAlive()||!victim.isAttackable()||friendly(victim))return false;
        if(resident)return ((WitchActor)mob).residentEnemy(victim);
        return SpellEngine.enemy(owner,victim)&&(!(victim instanceof net.minecraft.world.entity.player.Player)||mob.getServer().isPvpAllowed());
    }
    private UUID childOwner(){return resident?mob.getUUID():owner;}
    private boolean capacity(ServerLevel world){
        int owned=0,total=0;
        for(Entity entity:world.getAllEntities())if(entity instanceof Summoned s){
            total++;if(Objects.equals(childOwner(),s.ownerId()))owned++;
        }
        return owned<(resident?12:SpellEngine.MAX_UNITS_PER_OWNER)&&total<256;
    }
    /** Four independent collision-checked points; failed spawns are not eligible souls. */
    public void summonWave(){
        if(!(mob.level() instanceof ServerLevel world)||!mob.isAlive())return;
        boolean first=waves==0;int spawned=0;
        Vec3 forward=SpellEngine.horizontal(mob.getViewVector(1));Vec3 side=new Vec3(forward.z,0,-forward.x);
        for(int i=0;i<4;i++){
            if(!capacity(world))break;
            double lateral=(i%2==0?-1:1)*(i<2?.95:1.25),ahead=i<2?1.2:-.5;
            Vec3 at=SpellEngine.ground(world,mob.position().add(forward.scale(ahead)).add(side.scale(lateral)));
            if(!world.hasChunkAt(BlockPos.containing(at))||!world.getWorldBorder().isWithinBounds(BlockPos.containing(at))
                ||!world.noCollision(AABB.ofSize(at.add(0,.7,0),.48,1.4,.48)))continue;
            AllySkeleton skeleton=((WitchActor)mob).createSkeleton(world);if(skeleton==null)continue;
            skeleton.setPos(at);skeleton.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
            skeleton.setDropChance(EquipmentSlot.MAINHAND,0);
            CardBalance.apply(skeleton,"skeleton",false,cardLevel());skeleton.setup(childOwner(),Math.min(life,1200),isClone());
            if(!world.addFreshEntity(skeleton))continue;
            if(isClone()){skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);skeleton.setHealth(1);}
            skeleton.getPersistentData().putUUID(SummonOrders.ESCORT,mob.getUUID());
            skeleton.setTarget(mob.getTarget());
            if(first&&evolved()&&!isClone()){
                firstWave.add(skeleton.getUUID());skeleton.getPersistentData().putUUID(MOTHER_TAG,mob.getUUID());
            }
            // A short burst at the actual spawn point, never screen-space.
            world.sendParticles(RoyaleSpells.GRAVE_MOTE,at.x,at.y+.08,at.z,9,.25,.16,.25,.02);spawned++;
        }
        // The first wave is a one-shot rule, even in a cramped space; later
        // waves cannot replenish missed first-wave healing authorizations.
        waves++;entityData.set(keys.SUMMON_AGE,1);
        if(spawned>0)WitchSounds.play(mob,evolved()?WitchSounds.EVO_SUMMON:WitchSounds.SUMMON,.52f,1);
    }
    public static void onSkeletonDeath(LivingEntity dead){
        if(!(dead instanceof AllySkeleton skeleton)||skeleton.isClone()||!(dead.level() instanceof ServerLevel world))return;
        CompoundTag tag=dead.getPersistentData();if(!tag.hasUUID(MOTHER_TAG))return;
        if(world.getEntity(tag.getUUID(MOTHER_TAG)) instanceof WitchActor mother)mother.behavior().releaseSoul(skeleton);
    }
    private void releaseSoul(AllySkeleton dead){
        if(!evolved()||isClone()||!mob.isAlive()||dead.isAlive()||!Objects.equals(childOwner(),dead.ownerId())||!firstWave.contains(dead.getUUID())
            ||pendingSouls.containsKey(dead.getUUID()))return;
        WitchBolt soul=WitchBolt.soul((ServerLevel)mob.level(),(WitchActor)mob,dead);if(soul==null)return;
        pendingSouls.put(dead.getUUID(),soul.getUUID());
        if(!mob.level().addFreshEntity(soul))pendingSouls.remove(dead.getUUID());
    }
    /** Called only on a physical soul's arrival. UUID is consumed atomically. */
    public boolean acceptSoul(UUID skeleton,UUID soul){
        if(!mob.isAlive()||!evolved()||isClone()||soulReturns>=4||!firstWave.contains(skeleton)
            ||!soul.equals(pendingSouls.get(skeleton)))return false;
        if(!(mob.level() instanceof ServerLevel world)||world.getEntity(mob.getUUID())!=mob||!(world.getEntity(soul) instanceof WitchBolt orb)
            ||orb.kind()!=WitchBolt.HEAL_SOUL||orb.age()<SOUL_TICKS||orb.impacted()
            ||!mob.getUUID().equals(orb.recipient())||!skeleton.equals(orb.skeletonId())
            ||orb.position().distanceToSqr(mob.position().add(0,1.3,0))>.25)return false;
        firstWave.remove(skeleton);pendingSouls.remove(skeleton);soulReturns++;
        float health=Math.min(evolutionHealthCap(),mob.getHealth()+soulHealing());
        // Vanilla clamps health to MAX_HEALTH. Raise only the earned overflow,
        // so a freshly deployed evolved witch starts with an honest full bar.
        if(health>mob.getMaxHealth())mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        mob.setHealth(health);entityData.set(keys.HEAL_FLASH,10);WitchSounds.play(mob,WitchSounds.HEAL,.48f,1);return true;
    }
    public void abandonSoul(UUID skeleton,UUID soul){if(soul.equals(pendingSouls.get(skeleton))){pendingSouls.remove(skeleton);firstWave.remove(skeleton);}}
    public void save(CompoundTag n){
        if(owner!=null)n.putUUID("SpellOwner",owner);n.putInt("SpellLife",life);n.putBoolean("SpellClone",isClone());
        n.putBoolean("WitchEvolved",evolved());n.putInt("WitchLevel",cardLevel());n.putInt("Deploy",deploy);n.putInt("SummonClock",summonClock);
        n.putInt("Waves",waves);n.putInt("SoulReturns",soulReturns);n.putInt("AttackAge",attackAge());n.putInt("SummonAge",summonAge());
        n.putDouble("ActionFraction",actionFraction);n.putBoolean("DeploymentSound",deploymentSound);
        ListTag ids=new ListTag();for(UUID id:firstWave){CompoundTag row=new CompoundTag();row.putUUID("Skeleton",id);if(pendingSouls.containsKey(id))row.putUUID("Soul",pendingSouls.get(id));ids.add(row);}n.put("FirstWave",ids);
    }
    public void load(CompoundTag n){
        owner=n.hasUUID("SpellOwner")?n.getUUID("SpellOwner"):resident?null:mob.getUUID();life=n.contains("SpellLife")?Mth.clamp(n.getInt("SpellLife"),1,LIFE_TICKS):LIFE_TICKS;
        if(n.contains("WitchEvolved"))entityData.set(keys.EVO,n.getBoolean("WitchEvolved"));entityData.set(keys.CLONE,n.getBoolean("SpellClone"));entityData.set(keys.CARD_LEVEL,CardLevels.unitLevel(n.getInt("WitchLevel")));
        deploy=Mth.clamp(n.getInt("Deploy"),0,DEPLOY_TICKS);summonClock=Mth.clamp(n.getInt("SummonClock"),0,SUMMON_TICKS-1);waves=Math.max(0,n.getInt("Waves"));soulReturns=Mth.clamp(n.getInt("SoulReturns"),0,4);
        entityData.set(keys.ATTACK_AGE,Mth.clamp(n.getInt("AttackAge"),0,ATTACK_TICKS));entityData.set(keys.SUMMON_AGE,Mth.clamp(n.getInt("SummonAge"),0,24));
        actionFraction=Mth.clamp(n.getDouble("ActionFraction"),0,.999);deploymentSound=n.getBoolean("DeploymentSound");firstWave.clear();pendingSouls.clear();
        if(evolved()&&!isClone())for(Tag value:n.getList("FirstWave",Tag.TAG_COMPOUND)){CompoundTag row=(CompoundTag)value;if(firstWave.size()>=4-soulReturns)break;if(!row.hasUUID("Skeleton"))continue;UUID id=row.getUUID("Skeleton");firstWave.add(id);if(row.hasUUID("Soul"))pendingSouls.put(id,row.getUUID("Soul"));}
    }
}
