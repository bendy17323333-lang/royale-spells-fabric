package dev.royalespells.entity;
import dev.royalespells.SpellEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import java.util.UUID;

public class AllySkeleton extends Skeleton implements Summoned {
    private UUID owner;
    private int life=400;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CLONED=net.minecraft.network.syncher.SynchedEntityData.defineId(AllySkeleton.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(CLONED,false);}
    public AllySkeleton(EntityType<? extends Skeleton> type,Level world){super(type,world);xpReward=0;}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new RageMeleeGoal(this));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,0.65));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    @Override public void reassessWeaponGoal(){}
    @Override protected boolean isSunBurnTick(){return false;}
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override public boolean doHurtTarget(Entity target) {
        if(!(level() instanceof net.minecraft.server.level.ServerLevel world) || !(target instanceof LivingEntity victim)
            || hasEffect(dev.royalespells.RoyaleSpells.STUN) || !SpellEngine.enemy(owner,victim))return false;
        if(victim instanceof net.minecraft.world.entity.player.Player && !world.getServer().isPvpAllowed())return false;
        int previous=victim.invulnerableTime;
        victim.invulnerableTime=0;
        try {
            // The sword is visual equipment; explicit low damage avoids its vanilla attack bonus.
            return victim.hurt(world.damageSources().mobAttack(this),(float)getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue());
        } finally {victim.invulnerableTime=Math.max(previous,victim.invulnerableTime);}
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!level().isClientSide && source.getEntity() instanceof LivingEntity attacker && SpellEngine.friendly(owner,attacker)) return false;
        return super.hurt(source,amount);
    }
    @Override public void tick(){super.tick();if(!level().isClientSide){if(--life<=0){discard();return;}SpellEngine.unitTick(this,owner);}}
    public UUID ownerId(){return owner;}
    public boolean isClone(){return entityData.get(CLONED);}
    public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;entityData.set(CLONED,clone);setPersistenceRequired();setCanPickUpLoot(false);}
    @Override public void addAdditionalSaveData(CompoundTag nbt){super.addAdditionalSaveData(nbt);if(owner!=null)nbt.putUUID("SpellOwner",owner);nbt.putInt("SpellLife",life);nbt.putBoolean("SpellClone",isClone());}
    @Override public void readAdditionalSaveData(CompoundTag nbt){super.readAdditionalSaveData(nbt);owner=nbt.hasUUID("SpellOwner")?nbt.getUUID("SpellOwner"):null;life=nbt.getInt("SpellLife");entityData.set(CLONED,nbt.getBoolean("SpellClone"));}
}



