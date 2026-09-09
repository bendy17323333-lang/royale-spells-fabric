package dev.mineclash.zappies.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import dev.mineclash.zappies.client.AnimationProbe;

/** Read-only opt-in diagnostics; inactive in normal play. */
@Mixin(value=GeoModel.class,remap=false)
public abstract class AnimationProbeMixin {
    @Inject(method="handleAnimations",at=@At("TAIL"))
    private void zappies$observe(GeoAnimatable a,long id,AnimationState<?> state,float partial,CallbackInfo ci){
        if(AnimationProbe.ENABLED)AnimationProbe.observe(a,id,(GeoModel<?>)(Object)this);
    }
}
