package dev.royalespells.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.royalespells.client.ChillAnimation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Transform animation inputs rather than entity age: hurt/death tilt, targeting
 * and other render layers must still receive the real age and partial tick. */
@Mixin(LivingEntityRenderer.class)
public abstract class ChillVanillaAnimationMixin {
    @ModifyExpressionValue(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getBob(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float slowedAge(float age,LivingEntity entity){float local=(float)ChillAnimation.age(entity,age);dev.royalespells.client.VisualUpdateRecording.observe(entity,age,local,"vanilla");return local;}
    @ModifyExpressionValue(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/WalkAnimationState;position(F)F"))
    private float slowedGait(float position,LivingEntity entity){return ChillAnimation.gait(entity,position);}
}
