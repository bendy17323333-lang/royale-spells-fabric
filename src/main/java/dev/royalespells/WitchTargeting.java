// SPDX-License-Identifier: MIT
package dev.royalespells;

import dev.royalespells.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ProjectileWeaponItem;
import java.util.*;

/** Enemy arbitration for an engaged witch squad, never a global aggro goal.
 * No movement or witch attack timing is controlled here. */
public final class WitchTargeting {
    private static final int SIGHT_GRACE=60;
    private record Sight(UUID target,long tick){}
    // UUID values do not retain a world/entity through this weak-key cache.
    private static final Map<Mob,Sight> SIGHT=new WeakHashMap<>();

    private static boolean member(LivingEntity entity){
        return entity instanceof WitchActor||entity instanceof WitchSkeleton
            ||entity instanceof Mob mob&&SummonOrders.smallSkeleton(mob)&&SummonOrders.escort(mob);
    }
    private static boolean resident(LivingEntity entity){
        return entity instanceof WitchSkeleton||entity instanceof WitchActor w&&w.behavior().resident();
    }
    private static boolean sameSquad(LivingEntity a,LivingEntity b){
        if(a==b)return true;
        if(!(b instanceof WitchActor)&&!SummonOrders.smallSkeleton(b))return false;
        if(resident(a)||resident(b))return resident(a)&&resident(b);
        UUID owner=CombatCompatibility.ownerOf(a);
        return owner!=null&&owner.equals(CombatCompatibility.ownerOf(b));
    }
    private static boolean legal(Mob attacker,LivingEntity candidate){
        return candidate!=attacker&&candidate.isAlive()&&candidate.isAttackable()
            &&!(candidate instanceof ArmySkeleton army&&army.ghost())
            &&attacker.canAttack(candidate)&&!candidate.isAlliedTo(attacker)
            &&!(candidate instanceof Summoned s&&SpellEngine.friendly(s.ownerId(),attacker))
            &&!(attacker instanceof Summoned s&&SpellEngine.friendly(s.ownerId(),candidate));
    }
    private static double range(Mob mob){return Math.max(4,Math.min(48,mob.getAttributeValue(Attributes.FOLLOW_RANGE)));}
    private static boolean ranged(Mob mob){
        // AllySkeleton inherits Skeleton's interface but replaces its bow goal
        // with melee. Vanilla sword skeletons must also use the melee policy.
        if(mob instanceof AllySkeleton)return false;
        if(mob instanceof AbstractSkeleton)return mob.isHolding(stack->stack.getItem() instanceof ProjectileWeaponItem);
        return mob instanceof RangedAttackMob||mob instanceof InfernoDragon||CombatCompatibility.spellCaster(mob);
    }
    private static boolean visibleOrRecent(Mob mob,LivingEntity target){
        long now=mob.level().getGameTime();
        if(mob.hasLineOfSight(target)){SIGHT.put(mob,new Sight(target.getUUID(),now));return true;}
        Sight seen=SIGHT.get(mob);
        return seen!=null&&seen.target.equals(target.getUUID())&&now-seen.tick<=SIGHT_GRACE;
    }
    private static boolean ordered(Mob mob){
        return CombatCompatibility.resolve((ServerLevel)mob.level(),CombatCompatibility.ownerOf(mob)) instanceof ServerPlayer p
            &&SummonOrders.priority(mob,p)!=null;
    }
    /** The first visible target selected by the mob is its ranged lock.
     * Explicit player orders and the mob's null/de-aggro requests remain authoritative. */
    public static LivingEntity choose(Mob mob,LivingEntity proposed){
        if(mob instanceof WitchActor||proposed==null||ordered(mob))return proposed;
        double limit=range(mob);limit*=limit;
        LivingEntity current=mob.getTarget();
        if(ranged(mob)){
            if(member(current)&&legal(mob,current)&&mob.distanceToSqr(current)<=limit&&visibleOrRecent(mob,current))return current;
            SIGHT.remove(mob);
            if(member(proposed)&&legal(mob,proposed)&&mob.hasLineOfSight(proposed))visibleOrRecent(mob,proposed);
            return proposed;
        }
        if(!member(proposed)||!legal(mob,proposed))return proposed;
        LivingEntity nearest=proposed;double distance=mob.distanceToSqr(proposed);
        for(LivingEntity candidate:mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(range(mob)),
            e->sameSquad(proposed,e)&&legal(mob,e)&&mob.hasLineOfSight(e))){
            double d=mob.distanceToSqr(candidate);
            if(d<distance-.0001&&d<=limit){nearest=candidate;distance=d;}
        }
        return nearest;
    }
    /** Re-evaluate a changing melee front line at 4 Hz, only during a squad fight. */
    public static void tick(Mob mob){
        if(!(mob.level() instanceof ServerLevel)||mob.isNoAi()||!mob.isAlive()||mob.tickCount%5!=0
            ||mob instanceof WitchActor||!member(mob.getTarget()))return;
        LivingEntity current=mob.getTarget(),selected=choose(mob,current);
        if(selected!=current)mob.setTarget(selected);
    }
    /** An idle tower resident can defend its engaged patrol, without changing
     * its original navigation, player hostility, range or attack cadence. */
    public static LivingEntity patrolEnemy(WitchActor witch){
        Mob mother=witch.mob();LivingEntity nearest=null;double distance=24*24;
        for(AllySkeleton guard:mother.level().getEntitiesOfClass(AllySkeleton.class,mother.getBoundingBox().inflate(16),
            s->s.isAlive()&&sameSquad(mother,s))){
            LivingEntity target=guard.getTarget();
            if(target==null||witch.behavior().friendly(target)||!legal(mother,target)||!mother.hasLineOfSight(target))continue;
            double d=mother.distanceToSqr(target);
            if(d<distance){nearest=target;distance=d;}
        }
        return nearest;
    }
    private WitchTargeting(){}
}
