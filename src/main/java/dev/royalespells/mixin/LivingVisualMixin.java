package dev.royalespells.mixin;

import dev.royalespells.entity.Summoned;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.client.SpellOverlays;
import dev.royalespells.client.SpellTint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingVisualMixin {
    // Decorate the argument instead of exclusively redirecting EntityModel.render.
    // Other material renderers can keep their own model invocation.
    @ModifyVariable(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At("HEAD"),argsOnly=true)
    private MultiBufferSource royaleTintBuffers(MultiBufferSource original,LivingEntity entity,float yaw,float delta,PoseStack matrices,MultiBufferSource vertices,int light) {
        return SpellTint.wrap(original,entity);
    }
    @Inject(method="getRenderType",at=@At("HEAD"),cancellable=true)
    private void translucentClone(LivingEntity entity,boolean body,boolean translucent,boolean outline,CallbackInfoReturnable<RenderType> cir) {
        if(entity instanceof Summoned s && s.isClone() && (body||translucent))
            cir.setReturnValue(RenderType.entityTranslucent(((LivingEntityRenderer)(Object)this).getTextureLocation(entity)));
    }
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("TAIL"))
    private void overlays(LivingEntity entity,float yaw,float delta,PoseStack matrices,MultiBufferSource vertices,int light,CallbackInfo ci) {
        SpellOverlays.render(entity,delta,matrices,SpellTint.unwrap(vertices),light);
    }
}
