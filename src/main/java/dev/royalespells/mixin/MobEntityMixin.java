package dev.royalespells.mixin;

import dev.royalespells.RoyaleSpells;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void royaleStopAi(CallbackInfo ci) {
        if(((Mob)(Object)this).hasEffect(RoyaleSpells.STUN))ci.cancel();
    }
}
