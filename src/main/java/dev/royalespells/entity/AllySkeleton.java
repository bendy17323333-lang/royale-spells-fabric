package dev.royalespells.entity;
import net.minecraft.entity.data.DataTracker;

import dev.royalespells.SpellEngine;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import java.util.UUID;

public class AllySkeleton extends SkeletonEntity implements Summoned {
    private UUID owner;
    private int life=400;
    private static final net.minecraft.entity.data.TrackedData<Boolean> CLONED=net.minecraft.entity.data.DataTracker.registerData(AllySkeleton.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Override protected void initDataTracker(DataTracker.Builder builder){super.initDataTracker(builder);builder.add(CLONED,false);}
    public AllySkeleton(EntityType<? extends SkeletonEntity> type,World world){super(type,world);experiencePoints=0;}
    @Override protected void initGoals() {
        goalSelector.add(0,new SwimGoal(this));goalSelector.add(2,new RageMeleeGoal(this));
        goalSelector.add(7,new WanderAroundFarGoal(this,0.65));goalSelector.add(8,new LookAroundGoal(this));
    }
    @Override public void updateAttackType(){}
    @Override protected boolean isAffectedByDaylight(){return false;}
    @Override protected boolean isDisallowedInPeaceful(){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropXp(){return false;}
    @Override public boolean tryAttack(Entity target) {
        if(!(getWorld() instanceof net.minecraft.server.world.ServerWorld world) || !(target instanceof LivingEntity victim)
            || hasStatusEffect(dev.royalespells.RoyaleSpells.STUN) || !SpellEngine.enemy(owner,victim))return false;
        if(victim instanceof net.minecraft.entity.player.PlayerEntity && !world.getServer().isPvpEnabled())return false;
        int previous=victim.timeUntilRegen;
        victim.timeUntilRegen=0;
        try {
            // The sword is visual equipment; explicit low damage avoids its vanilla attack bonus.
            return victim.damage(world.getDamageSources().mobAttack(this),(float)getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
        } finally {victim.timeUntilRegen=Math.max(previous,victim.timeUntilRegen);}
    }
    @Override public boolean damage(DamageSource source,float amount) {
        if(!getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker && SpellEngine.friendly(owner,attacker)) return false;
        return super.damage(source,amount);
    }
    @Override public void tick(){super.tick();if(!getWorld().isClient){if(--life<=0){discard();return;}SpellEngine.unitTick(this,owner);}}
    public UUID ownerId(){return owner;}
    public boolean isClone(){return dataTracker.get(CLONED);}
    public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;dataTracker.set(CLONED,clone);setPersistent();setCanPickUpLoot(false);}
    @Override public void writeCustomDataToNbt(NbtCompound nbt){super.writeCustomDataToNbt(nbt);if(owner!=null)nbt.putUuid("SpellOwner",owner);nbt.putInt("SpellLife",life);nbt.putBoolean("SpellClone",isClone());}
    @Override public void readCustomDataFromNbt(NbtCompound nbt){super.readCustomDataFromNbt(nbt);owner=nbt.containsUuid("SpellOwner")?nbt.getUuid("SpellOwner"):null;life=nbt.getInt("SpellLife");dataTracker.set(CLONED,nbt.getBoolean("SpellClone"));}
}



