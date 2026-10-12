// SPDX-License-Identifier: MIT
package dev.royalespells.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent tower patrol or a finite witch summon; no indiscriminate animal hunting. */
public final class WitchSkeleton extends AllySkeleton {
    private boolean homeSet;
    public WitchSkeleton(EntityType<? extends Skeleton> type,Level level){
        super(type,level);setup(null,Integer.MAX_VALUE,false);
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));setDropChance(EquipmentSlot.MAINHAND,0);
    }
    public boolean factionFriend(Entity target){return target instanceof WitchSkeleton||target instanceof WitchActor witch&&witch.behavior().resident();}
    @Override public boolean isAlliedTo(Entity target){return factionFriend(target)||super.isAlliedTo(target);}
    @Override protected void registerGoals(){
        super.registerGoals();
        goalSelector.addGoal(6,new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this,.75){
            @Override public boolean canUse(){return getTarget()==null&&ownerId()==null&&super.canUse();}
        });
    }
    @Override public boolean hurt(DamageSource damage,float amount){if(factionFriend(damage.getEntity()))return false;return super.hurt(damage,amount);}
    @Override public boolean doHurtTarget(Entity target){
        if(!(target instanceof LivingEntity victim)||factionFriend(target)||hasEffect(dev.royalespells.RoyaleSpells.STUN))return false;
        int immune=victim.invulnerableTime;victim.invulnerableTime=0;
        try {return dev.royalespells.CombatImpact.withoutKnockback(victim,()->victim.hurt(damageSources().mobAttack(this),(float)getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue()));}
        finally {victim.invulnerableTime=Math.max(immune,victim.invulnerableTime);}
    }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType type,SpawnGroupData data){
        var result=super.finalizeSpawn(level,difficulty,type,data);
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));setDropChance(EquipmentSlot.MAINHAND,0);return result;
    }
    public void tickOrders(){
        if(!(level() instanceof ServerLevel world)||isNoAi())return;
        if(!homeSet){if(ownerId()==null)restrictTo(blockPosition(),12);homeSet=true;}
        if(tickCount%5!=0)return;
        Mob commander=ownerId()!=null&&world.getEntity(ownerId()) instanceof Mob m&&m.isAlive()?m:null;
        if(commander!=null)getPersistentData().putUUID(dev.royalespells.SummonOrders.ESCORT,commander.getUUID());
        if(commander==null)commander=world.getEntitiesOfClass(Mob.class,getBoundingBox().inflate(24),
            m->m instanceof WitchActor w&&w.behavior().resident()&&m.isAlive()).stream()
            .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        dev.royalespells.SummonOrders.tickGuard(this,commander);
    }
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(homeSet)n.putLong("WitchPatrolHome",getRestrictCenter().asLong());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);if(!n.contains("SpellLife"))setup(null,Integer.MAX_VALUE,false);if(n.contains("WitchPatrolHome")){restrictTo(BlockPos.of(n.getLong("WitchPatrolHome")),12);homeSet=true;}}
}
