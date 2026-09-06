package dev.royalespells.entity;
import dev.royalespells.RoyaleSpells;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.PathAwareEntity;
public class RageMeleeGoal extends MeleeAttackGoal {
    public RageMeleeGoal(PathAwareEntity mob){super(mob,1.0,true);}
    @Override protected int getMaxCooldown(){return mob.hasStatusEffect(RoyaleSpells.RAGED)?Math.max(1,(int)(super.getMaxCooldown()/1.35)):super.getMaxCooldown();}
}
