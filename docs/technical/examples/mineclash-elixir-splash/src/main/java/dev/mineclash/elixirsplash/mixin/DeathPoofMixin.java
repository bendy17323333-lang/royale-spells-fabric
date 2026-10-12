// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.mixin;

import dev.mineclash.elixirsplash.client.SplashEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class DeathPoofMixin {
    /** Event 60 is sent by the server at the end of tickDeath. Do not intercept
     * event 3, die(), damage, animation controllers, entity removal or drops. */
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void elixirsplash$death(byte event, CallbackInfo ci) {
        if (event == 60 && SplashEffects.onDeathPoof((LivingEntity) (Object) this)) ci.cancel();
    }
}
