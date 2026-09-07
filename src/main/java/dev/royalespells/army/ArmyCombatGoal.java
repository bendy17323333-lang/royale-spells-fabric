package dev.royalespells.army;

import dev.royalespells.RoyaleSpells;
import dev.royalespells.SpellEngine;
import dev.royalespells.entity.ArmySkeleton;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Formation-aware melee steering. A failed path never disables a ghost's combat goal. */
public final class ArmyCombatGoal extends Goal {
    private final ArmySkeleton unit;
    private List<ArmySkeleton> squad=List.of();
    private int refresh,repath,stuck,flankTicks;
    private Vec3 last=Vec3.ZERO,flank=Vec3.ZERO;
    private UUID targetId;

    public ArmyCombatGoal(ArmySkeleton unit){this.unit=unit;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private boolean valid(LivingEntity target){return target!=null&&target.isAttackable()&&SpellEngine.enemy(unit.ownerId(),target)&&unit.distanceToSqr(target)<40*40;}
    @Override public boolean canUse(){return unit.age()>18&&unit.dissolve()==0&&valid(unit.getTarget());}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){refresh=0;repath=0;stuck=0;flankTicks=0;last=unit.position();targetId=null;unit.setAggressive(true);}
    @Override public void stop(){halt();unit.setAggressive(false);squad=List.of();}
    private void halt(){unit.getNavigation().stop();unit.getMoveControl().setWantedPosition(unit.getX(),unit.getY(),unit.getZ(),0);}
    @Override public void tick(){
        var target=unit.getTarget();if(!valid(target)){halt();return;}
        if(unit.hasEffect(RoyaleSpells.STUN)||unit.hasEffect(RoyaleSpells.FROZEN)||unit.hasEffect(RoyaleSpells.ROOTED)){halt();return;}
        if(!target.getUUID().equals(targetId)){targetId=target.getUUID();flankTicks=0;stuck=0;repath=0;}
        unit.getLookControl().setLookAt(target,30,30);
        if(--refresh<=0){
            squad=unit.level().getEntitiesOfClass(ArmySkeleton.class,unit.getBoundingBox().inflate(14,5,14),e->e!=unit&&e.isAlive()&&e.dissolve()==0&&Objects.equals(unit.armyId(),e.armyId()));
            refresh=10;
        }
        boolean canHit=unit.isWithinMeleeAttackRange(target)&&unit.getSensing().hasLineOfSight(target);
        if(canHit)unit.beginStrike(target);
        if(unit.general()){followCharge(target,canHit);return;}
        if(canHit){halt();stuck=0;flankTicks=0;return;}
        if(unit.position().distanceToSqr(last)<.0025)stuck++;else stuck=Math.max(0,stuck-2);
        last=unit.position();
        Vec3 toward=flat(target.position().subtract(unit.position()));
        var commander=squad.stream().filter(ArmySkeleton::general).findFirst().orElse(null);
        Vec3 rear=flat((commander==null?unit.position():commander.position()).subtract(target.position()));
        double angle=(unit.formationSlot()/2)*Math.toRadians(24)*(unit.formationSlot()%2==0?-1:1);
        double radius=(target.getBbWidth()+unit.getBbWidth())*.5+.27;
        Vec3 approach=target.position().add(rotate(rear,angle).scale(radius));

        // Lanes are retained for a short interval, so neighbours cannot make the
        // same ghost alternate left/right every tick. Vanilla collision remains on.
        if(flankTicks>0){
            flankTicks--;if(unit.position().distanceToSqr(flank)<.32||canHit)flankTicks=0;
        }else if(blockedBySquad(toward)||stuck>=12){
            double side=unit.formationSlot()%2==0?-1:1;
            if(stuck>50)side=-side;
            Vec3 across=new Vec3(-toward.z,0,toward.x).scale(side);
            flank=unit.position().add(across.scale(1.65)).add(toward.scale(.75));
            flankTicks=24;repath=0;
        }
        if(flankTicks>0)approach=flank;
        else if(unit.distanceToSqr(target)<10){
            // Follow the outside of the melee ring instead of cutting through
            // the target and its front row to reach a rear-side attack position.
            Vec3 current=flat(unit.position().subtract(target.position()));
            Vec3 desired=flat(approach.subtract(target.position()));
            double turn=Math.atan2(current.x*desired.z-current.z*desired.x,current.dot(desired));
            if(Math.abs(turn)>.6)approach=target.position().add(rotate(current,Math.copySign(.6,turn)).scale(radius+.8));
        }
        steer(approach,unit.ghost()?1.18:1.05);
    }
    private boolean blockedBySquad(Vec3 toward){
        for(var other:squad){
            if(other.general()||!other.isAlive())continue;
            var delta=other.position().subtract(unit.position());double ahead=delta.dot(toward);
            if(ahead>.12&&ahead<1.25&&Math.abs(delta.y)<1.1&&delta.subtract(toward.scale(ahead)).horizontalDistanceSqr()<.48*.48)return true;
        }return false;
    }
    private void followCharge(LivingEntity target,boolean canHit){
        // A commander is a slower attacker, not a fleeing ranged unit. If the
        // enemy closes the gap, hold ground and strike instead of backing away.
        if(canHit){halt();return;}
        var guards=squad.stream().filter(e->!e.general()&&e.isAlive()).sorted(Comparator.comparingDouble(e->e.distanceToSqr(target))).limit(5).toList();
        if(guards.isEmpty()){steer(target.position(),.95);return;}
        Vec3 rear=flat(unit.position().subtract(target.position()));
        double front=guards.stream().mapToDouble(e->Math.sqrt(e.position().subtract(target.position()).horizontalDistanceSqr())).average().orElse(1);
        // Soldiers have faster approach/lane steering. Let them pass when they
        // are close enough to screen us, but never generate a retreat waypoint.
        double distance=Math.sqrt(unit.position().subtract(target.position()).horizontalDistanceSqr());
        double desired=Math.max(1.25,Math.min(front+.75,distance));
        if(distance<=desired+.18){halt();return;}
        Vec3 anchor=target.position().add(rear.scale(desired));
        steer(anchor,.90);
    }
    private void steer(Vec3 at,double speed){
        // Near a flat, clear endpoint use sub-block movement, otherwise the
        // navigator rounds all fifteen attack slots to the same block centre.
        if(unit.position().distanceToSqr(at)<9&&flatWalkable(at)){
            unit.getNavigation().stop();unit.getMoveControl().setWantedPosition(at.x,unit.getY(),at.z,speed);repath=0;
        }else if(--repath<=0){unit.getNavigation().moveTo(at.x,at.y,at.z,speed);repath=8;}
    }
    private boolean flatWalkable(Vec3 at){
        if(Math.abs(at.y-unit.getY())>.3)return false;
        var delta=new Vec3(at.x-unit.getX(),0,at.z-unit.getZ());int steps=Math.max(1,(int)Math.ceil(delta.length()/.35));
        for(int i=1;i<=steps;i++){
            var offset=delta.scale((double)i/steps);var box=unit.getBoundingBox().move(offset);var below=BlockPos.containing(box.getCenter().x,box.minY-.12,box.getCenter().z);
            if(!unit.level().hasChunkAt(below)||!unit.level().getWorldBorder().isWithinBounds(below)
                ||unit.level().getBlockState(below).getCollisionShape(unit.level(),below).isEmpty()
                ||unit.level().getBlockCollisions(unit,box).iterator().hasNext())return false;
        }return true;
    }
    private static Vec3 flat(Vec3 v){double l=Math.hypot(v.x,v.z);return l<.001?new Vec3(0,0,1):new Vec3(v.x/l,0,v.z/l);}
    private static Vec3 rotate(Vec3 v,double a){double c=Math.cos(a),s=Math.sin(a);return new Vec3(v.x*c-v.z*s,0,v.x*s+v.z*c);}
}
