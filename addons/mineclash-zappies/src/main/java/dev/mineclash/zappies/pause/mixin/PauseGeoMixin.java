package dev.mineclash.zappies.pause.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mineclash.zappies.pause.PauseClock;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
/** No bone setters, no controller restart, no global animation-speed mutation. */
@Pseudo @Mixin(targets="software.bernie.geckolib.model.GeoModel",remap=false)
public abstract class PauseGeoMixin {
    @WrapMethod(method="handleAnimations",remap=false)
    private void localAnimationTime(GeoAnimatable a,long id,AnimationState<?> state,float partial,Operation<Void> original){
        if(!(a instanceof LivingEntity e)){original.call(a,id,state,partial);return;}
        int real=e.tickCount;double local=PauseClock.sample(e,partial);
        // GeoEntityRenderer supplies its own time ticket BEFORE calling this
        // method; changing Entity.tickCount alone would not pause any animation.
        Double realTicket=state.getData(DataTickets.TICK);
        state.setData(DataTickets.TICK,Math.floor(local));
        e.tickCount=(int)Math.floor(local);
        try{original.call(a,id,state,(float)(local-Math.floor(local)));}
        finally{e.tickCount=real;if(realTicket==null)state.getExtraData().remove(DataTickets.TICK);else state.setData(DataTickets.TICK,realTicket);}
    }
}
