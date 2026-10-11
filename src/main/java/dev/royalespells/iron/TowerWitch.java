// SPDX-License-Identifier: MIT
package dev.royalespells.iron;
import dev.royalespells.entity.*;
import dev.royalespells.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Optional Iron world resident; combat is composed, not copied from the card troop. */
public final class TowerWitch extends io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard implements WitchActor {
    public static final int LIFE_TICKS=WitchBehavior.LIFE_TICKS,DEPLOY_TICKS=WitchBehavior.DEPLOY_TICKS,
        ATTACK_TICKS=WitchBehavior.ATTACK_TICKS,WINDUP_TICKS=WitchBehavior.WINDUP_TICKS,
        SUMMON_TICKS=WitchBehavior.SUMMON_TICKS,SOUL_TICKS=WitchBehavior.SOUL_TICKS;
    public static final double RANGE=WitchBehavior.RANGE,SPLASH_RADIUS=WitchBehavior.SPLASH_RADIUS;
    public static final String MOTHER_TAG=WitchBehavior.MOTHER_TAG;
    private static final WitchBehavior.Data DATA=WitchBehavior.Data.create(TowerWitch.class);
    private final WitchBehavior behavior=new WitchBehavior(this,DATA,true);
    public TowerWitch(EntityType<? extends PathfinderMob> type,Level level){super(type,level);xpReward=15;setPersistenceRequired();}
    public WitchBehavior behavior(){return behavior;}
    public UUID ownerId(){return behavior.ownerId();}
    public boolean isClone(){return behavior.isClone();}
    public void setup(UUID owner,int life,boolean clone){behavior.setup(owner,life,clone);}
    public static AttributeSupplier.Builder attributes(){return WitchBehavior.attributes();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);DATA.define(b);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.75){@Override public boolean canUse(){return getTarget()==null&&super.canUse();}});
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,net.minecraft.world.entity.player.Player.class,8));
        targetSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.player.Player.class,10,true,false,this::isHostileTowards));}
    @Override public void tick(){super.tick();if(!level().isClientSide&&!hasRestriction())restrictTo(blockPosition(),6);behavior.tick();}
    @Override public boolean residentEnemy(LivingEntity victim){
        if(victim instanceof net.minecraft.world.entity.player.Player p)return !p.isCreative()&&!p.isSpectator()&&(victim==getTarget()||isHostileTowards(victim));
        return victim==getTarget()||victim==getLastHurtByMob()||victim instanceof Mob m&&m.getTarget()==this;
    }
    @Override public AllySkeleton createSkeleton(net.minecraft.server.level.ServerLevel world){return WitchContent.SKELETON.create(world);}
    @Override public boolean hurt(DamageSource source,float amount){return !behavior.friendly(source.getEntity())&&super.hurt(source,amount);}
    @Override public boolean doHurtTarget(Entity target){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return null;}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return WitchSounds.DEATH;}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);behavior.save(n);if(hasRestriction())n.putLong("WitchHome",getRestrictCenter().asLong());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);behavior.load(n);if(n.contains("WitchHome"))restrictTo(net.minecraft.core.BlockPos.of(n.getLong("WitchHome")),6);}
}
