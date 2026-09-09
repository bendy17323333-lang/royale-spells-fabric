package dev.royalespells.pause.mixin;
import dev.royalespells.pause.ElectricPause;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class PauseMobMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void holdGoalsAndBrain(CallbackInfo ci){if(ElectricPause.active((Mob)(Object)this))ci.cancel();}
}
