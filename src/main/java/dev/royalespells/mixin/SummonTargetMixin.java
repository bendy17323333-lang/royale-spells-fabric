package dev.royalespells.mixin;
import dev.royalespells.CombatCompatibility;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class SummonTargetMixin {
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void royaleTarget(LivingEntity target,CallbackInfo ci){var mob=(Mob)(Object)this;var permitted=CombatCompatibility.target(mob,target);if(permitted!=target){ci.cancel();mob.setTarget(permitted);}}
}
