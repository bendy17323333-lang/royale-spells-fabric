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
    @Inject(method="doKnockback",at=@At("HEAD"),cancellable=true)
    private void royaleNoArrowBonus(LivingEntity target,DamageSource source,CallbackInfo ci){
        if(SummonOrders.smallSkeleton(((AbstractArrow)(Object)this).getOwner())||target instanceof dev.royalespells.entity.ArmySkeleton army&&army.ghost())ci.cancel();
    }
}
