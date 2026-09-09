package dev.mineclash.zappies.mixin;
import dev.mineclash.zappies.ZappyCombat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class ZappyImpactMixin {
    @Inject(method="knockback",at=@At("HEAD"),cancellable=true)
    private void electricalImpact(double strength,double x,double z,CallbackInfo ci){if(ZappyCombat.noKnockback((LivingEntity)(Object)this))ci.cancel();}
}
