package dev.royalespells.mixin;

import dev.royalespells.entity.Summoned;
import dev.royalespells.client.SpellOverlays;
import dev.royalespells.client.SpellTint;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingVisualMixin {
    // Decorate the argument instead of exclusively redirecting EntityModel.render.
    // Other material renderers can keep their own model invocation.
    @ModifyVariable(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at=@At("HEAD"),argsOnly=true)
    private VertexConsumerProvider royaleTintBuffers(VertexConsumerProvider original,LivingEntity entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light) {
        return SpellTint.wrap(original,entity);
    }
    @Inject(method="getRenderLayer",at=@At("HEAD"),cancellable=true)
    private void translucentClone(LivingEntity entity,boolean body,boolean translucent,boolean outline,CallbackInfoReturnable<RenderLayer> cir) {
        if(entity instanceof Summoned s && s.isClone() && (body||translucent))
            cir.setReturnValue(RenderLayer.getEntityTranslucent(((LivingEntityRenderer)(Object)this).getTexture(entity)));
    }
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("TAIL"))
    private void overlays(LivingEntity entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light,CallbackInfo ci) {
        SpellOverlays.render(entity,delta,matrices,SpellTint.unwrap(vertices),light);
    }
}
