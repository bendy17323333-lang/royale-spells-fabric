// SPDX-License-Identifier: MIT
package dev.royalespells.entity;

import dev.royalespells.SummonOrders;
import dev.royalespells.SpellEngine;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** A MOVE goal, so following cannot overwrite the active melee goal's path.
 * Start/stop distances differ to prevent oscillation at the leash boundary. */
public final class FollowSummonerGoal extends Goal {
    private final PathfinderMob mob;
    private LivingEntity leader;
    private int repath;
    public FollowSummonerGoal(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
    private Vec3 destination(){
        if(!SummonOrders.escort(mob))return leader.position();
        Vec3 forward=SpellEngine.horizontal(leader.getViewVector(1));
        double side=((mob.getId()%4)-1.5)*.8;
        return leader.position().add(forward.scale(2.5)).add(forward.z*side,0,-forward.x*side);
    }
    @Override public boolean canUse(){
        if(mob.getTarget()!=null)return false;
        leader=SummonOrders.leader(mob);
        return leader!=null&&mob.distanceToSqr(destination())>(SummonOrders.escort(mob)?9:36);
    }
    @Override public boolean canContinueToUse(){
        return mob.getTarget()==null&&leader!=null&&leader.isAlive()&&leader.level()==mob.level()
            &&mob.distanceToSqr(destination())>3;
    }
    @Override public void start(){repath=0;}
    @Override public void tick(){
        if(--repath>0)return;repath=10;
        Vec3 at=destination();mob.getNavigation().moveTo(at.x,at.y,at.z,1.1);
    }
    @Override public void stop(){mob.getNavigation().stop();leader=null;}
}
