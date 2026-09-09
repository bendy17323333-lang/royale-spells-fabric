package dev.mineclash.zappies.mixin;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Used by opt-in GameTests to exercise MineClash's real windup goal. */
@Mixin(Mob.class)
public interface MobGoalAccess {
    @Accessor("goalSelector") GoalSelector zappyGoals();
    @Accessor("targetSelector") GoalSelector zappyTargets();
}
