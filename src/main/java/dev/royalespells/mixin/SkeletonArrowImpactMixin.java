package dev.royalespells.mixin;
import dev.royalespells.SummonOrders;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class SkeletonArrowImpactMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private void royaleNoArrowBonus(LivingEntity target,double x,double y,double z,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        if(SummonOrders.smallSkeleton(((AbstractArrow)(Object)this).getOwner())||target instanceof dev.royalespells.entity.ArmySkeleton army&&army.ghost())return;
        original.call(target,x,y,z);
    }
}
