package dev.royalespells.entity;
import dev.royalespells.RoyaleSpells;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
public class RageMeleeGoal extends MeleeAttackGoal {
    public RageMeleeGoal(PathfinderMob mob){super(mob,1.0,true);}
    @Override protected int getAttackInterval(){return mob.hasEffect(RoyaleSpells.RAGED)?Math.max(1,(int)(super.getAttackInterval()/1.35)):super.getAttackInterval();}
}
