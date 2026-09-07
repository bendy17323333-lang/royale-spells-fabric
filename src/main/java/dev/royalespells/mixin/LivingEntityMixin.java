package dev.royalespells.mixin;

import dev.royalespells.RoyaleSpells;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements dev.royalespells.VisualState {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="hurt")
    private boolean royaleSkeletonImpact(DamageSource source,float amount,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        LivingEntity self=(LivingEntity)(Object)this;
        if(dev.royalespells.SummonOrders.smallSkeleton(source.getEntity()))return dev.royalespells.CombatImpact.withoutKnockback(self,()->original.call(source,amount));
        return original.call(source,amount);
    }
    @Inject(method="knockback",at=@At("HEAD"),cancellable=true)
    private void royaleNoIncidentalImpulse(double strength,double x,double z,CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;
        if(dev.royalespells.CombatImpact.suppressed(self)||self instanceof dev.royalespells.entity.ArmySkeleton army&&army.blocksConversionKnockback())ci.cancel();
    }
    @org.spongepowered.asm.mixin.Unique
    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> ROYALE_VISUAL=net.minecraft.network.syncher.SynchedEntityData.defineId(LivingEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BYTE);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void initVisualData(net.minecraft.network.syncher.SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(ROYALE_VISUAL,(byte)0);}
    public byte royaleVisualFlags(){return ((LivingEntity)(Object)this).getEntityData().get(ROYALE_VISUAL);}
    @Inject(method="tick",at=@At("TAIL"))
    private void syncVisualData(CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;if(self.level().isClientSide)return;
        byte bits=(byte)((self.hasEffect(RoyaleSpells.RAGED)?1:0)|(self.hasEffect(RoyaleSpells.FROZEN)?2:0)|(self.hasEffect(RoyaleSpells.ROOTED)?4:0)|(self.hasEffect(RoyaleSpells.CLONED)?8:0));
        self.getEntityData().set(ROYALE_VISUAL,bits);
    }
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void royaleFreeze(Vec3 input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(self.hasEffect(RoyaleSpells.STUN)) {self.setDeltaMovement(Vec3.ZERO);ci.cancel();}
    }
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void royaleBlockFrozenAttack(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir) {
        if((source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO)) && source.getEntity() instanceof LivingEntity attacker && attacker.hasEffect(RoyaleSpells.STUN))cir.setReturnValue(false);
    }
}
