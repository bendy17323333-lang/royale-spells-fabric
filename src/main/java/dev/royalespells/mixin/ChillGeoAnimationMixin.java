package dev.royalespells.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.royalespells.client.ChillAnimation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

/** This independent, composable wrapper remains active with the Zappies driver.
 * Only the animation call sees local time; AI, effects and death keep real time. */
@Pseudo @Mixin(targets="software.bernie.geckolib.model.GeoModel",remap=false)
public abstract class ChillGeoAnimationMixin {
    @WrapMethod(method="handleAnimations",remap=false)
    private void slowedAnimation(GeoAnimatable animatable,long id,AnimationState<?> state,float partial,Operation<Void> original) {
        if(!(animatable instanceof LivingEntity e)){original.call(animatable,id,state,partial);return;}
        int real=e.tickCount;Double ticket=state.getData(DataTickets.TICK);
        double input=(ticket==null?real:ticket)+partial,local=ChillAnimation.age(e,input);
        dev.royalespells.client.VisualUpdateRecording.observe(e,input,local,"gecko");
        if(local==input){original.call(animatable,id,state,partial);return;}
        e.tickCount=(int)Math.floor(local);state.setData(DataTickets.TICK,Math.floor(local));
        try{original.call(animatable,id,state,(float)(local-Math.floor(local)));}
        finally{e.tickCount=real;if(ticket==null)state.getExtraData().remove(DataTickets.TICK);else state.setData(DataTickets.TICK,ticket);}
    }
}
