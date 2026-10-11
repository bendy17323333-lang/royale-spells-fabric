package dev.royalespells.entity;
import dev.royalespells.RoyaleSpells;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
public class RageMeleeGoal extends MeleeAttackGoal {
    public RageMeleeGoal(PathfinderMob mob){super(mob,1.0,true);}
    @Override public boolean canContinueToUse(){
        var target=mob.getTarget();
        // Patrol home limits idle roaming, not an already-authorized pursuit.
        return target!=null&&target.isAlive()&&target.isAttackable()&&mob.distanceToSqr(target)<=32*32
            &&!(target instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()));
    }
    @Override protected int getAttackInterval(){int interval=dev.royalespells.CardBalance.isCard(mob)?dev.royalespells.PersistentState.of(mob).getInt("RoyaleCardAttackTicks"):super.getAttackInterval();return mob.hasEffect(RoyaleSpells.RAGED)?Math.max(1,(int)(interval/1.35)):interval;}
}
