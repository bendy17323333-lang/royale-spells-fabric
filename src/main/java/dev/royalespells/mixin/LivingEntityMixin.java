package dev.royalespells.mixin;

import dev.royalespells.RoyaleSpells;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements dev.royalespells.VisualState {
    @org.spongepowered.asm.mixin.Unique
    private static final net.minecraft.entity.data.TrackedData<Byte> ROYALE_VISUAL=net.minecraft.entity.data.DataTracker.registerData(LivingEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BYTE);
    @Inject(method="initDataTracker",at=@At("TAIL"))
    private void initVisualData(net.minecraft.entity.data.DataTracker.Builder builder,CallbackInfo ci){builder.add(ROYALE_VISUAL,(byte)0);}
    public byte royaleVisualFlags(){return ((LivingEntity)(Object)this).getDataTracker().get(ROYALE_VISUAL);}
    @Inject(method="tick",at=@At("TAIL"))
    private void syncVisualData(CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;if(self.getWorld().isClient)return;
        byte bits=(byte)((self.hasStatusEffect(RoyaleSpells.RAGED)?1:0)|(self.hasStatusEffect(RoyaleSpells.FROZEN)?2:0)|(self.hasStatusEffect(RoyaleSpells.ROOTED)?4:0));
        self.getDataTracker().set(ROYALE_VISUAL,bits);
    }
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void royaleFreeze(Vec3d input,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(self.hasStatusEffect(RoyaleSpells.STUN)) {self.setVelocity(Vec3d.ZERO);ci.cancel();}
    }
    @Inject(method="damage",at=@At("HEAD"),cancellable=true)
    private void royaleBlockFrozenAttack(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir) {
        if((source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_ATTACK)||source.isOf(net.minecraft.entity.damage.DamageTypes.MOB_ATTACK)||source.isOf(net.minecraft.entity.damage.DamageTypes.MOB_ATTACK_NO_AGGRO)) && source.getAttacker() instanceof LivingEntity attacker && attacker.hasStatusEffect(RoyaleSpells.STUN))cir.setReturnValue(false);
    }
}

