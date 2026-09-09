package dev.royalespells.pause.mixin;
import dev.royalespells.pause.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class PauseLivingMixin implements PauseState {
    @Unique private static final EntityDataAccessor<Boolean> ELECTRICAL_PAUSE=SynchedEntityData.defineId(LivingEntity.class,EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void init(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(ELECTRICAL_PAUSE,false);}
    public boolean electricallyPaused(){return ((LivingEntity)(Object)this).getEntityData().get(ELECTRICAL_PAUSE);}
    @Inject(method="tick",at=@At("TAIL"))
    private void sync(CallbackInfo ci){var e=(LivingEntity)(Object)this;if(!e.level().isClientSide)e.getEntityData().set(ELECTRICAL_PAUSE,ElectricPause.active(e));}
    @Inject(method={"updateSwingTime","updatingUsingItem"},at=@At("HEAD"),cancellable=true)
    private void holdActionTimers(CallbackInfo ci){if(ElectricPause.active((LivingEntity)(Object)this))ci.cancel();}
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void holdMovement(Vec3 input,CallbackInfo ci){
        var e=(LivingEntity)(Object)this;
        // Keep the navigation path and target. Neither Goal.stop nor cancelCast is called.
        if(ElectricPause.active(e)){e.setDeltaMovement(Vec3.ZERO);ci.cancel();}
    }
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void stopDirectAttack(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        var e=(LivingEntity)(Object)this;
        if(source.getEntity() instanceof LivingEntity a && a!=e && source.getDirectEntity()==a && ElectricPause.active(a))cir.setReturnValue(false);
    }
}
