package dev.royalespells.mixin;

import dev.royalespells.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client presentation only. Do not slow server AI ticks or damage cadence. */
@Mixin(LivingEntity.class)
public abstract class ChillSwingMixin {
    @Unique private float royaleSwingFraction;
    @Inject(method="updateSwingTime",at=@At("HEAD"),cancellable=true)
    private void chillSwing(CallbackInfo ci) {
        var e=(LivingEntity)(Object)this;
        if(!e.level().isClientSide||!VisualState.snowbound(e)||!e.isAlive()){royaleSwingFraction=0;return;}
        if(dev.royalespells.pause.ElectricPause.active(e)||VisualState.frozen(e))return;
        royaleSwingFraction+=SnowballChill.ANIMATION_RATE;
        if(royaleSwingFraction>=1){royaleSwingFraction-=1;return;}
        e.oAttackAnim=e.attackAnim;ci.cancel();
    }
}
