package dev.royalespells.mixin;

import dev.royalespells.RoyaleSpells;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method="tickNewAi",at=@At("HEAD"),cancellable=true)
    private void royaleStopAi(CallbackInfo ci) {
        if(((MobEntity)(Object)this).hasStatusEffect(RoyaleSpells.STUN))ci.cancel();
    }
}
