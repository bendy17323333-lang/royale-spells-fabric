package dev.royalespells.mixin;

import dev.royalespells.VisualState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.util.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="software.bernie.geckolib.renderer.GeoEntityRenderer",remap=false)
public abstract class IronGeoVisualMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",remap=false)
    private void frozenGeoRender(Entity entity,float yaw,float delta,com.mojang.blaze3d.vertex.PoseStack matrices,MultiBufferSource vertices,int light,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        if(entity instanceof LivingEntity living){try(var scope=dev.royalespells.client.FrozenRender.begin(living,delta)){original.call(entity,scope.yaw(yaw),scope.delta(),matrices,vertices,light);}}
        else original.call(entity,yaw,delta,matrices,vertices,light);
    }
    @Inject(method="getRenderColor(Lnet/minecraft/world/entity/Entity;FI)Lsoftware/bernie/geckolib/util/Color;",at=@At("RETURN"),cancellable=true,remap=false)
    private void royaleColor(Entity entity,float delta,int light,CallbackInfoReturnable<Color> cir) {
        if(!(entity instanceof LivingEntity living))return;
        if(VisualState.cloned(living))cir.setReturnValue(Color.ofRGBA(.08f,.8f,1,.32f));
        else if(VisualState.frozen(living))cir.setReturnValue(Color.ofRGBA(.48f,.83f,1,1));
        else if(VisualState.snowbound(living))cir.setReturnValue(Color.ofRGBA(.32f,.62f,1,1));
        else if(VisualState.raged(living))cir.setReturnValue(Color.ofRGBA(.86f,.25f,1,1));
    }
    @Inject(method="getRenderType(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/MultiBufferSource;F)Lnet/minecraft/client/renderer/RenderType;",at=@At("RETURN"),cancellable=true,remap=false)
    private void royaleTranslucent(Entity entity,ResourceLocation texture,MultiBufferSource buffers,float delta,CallbackInfoReturnable<RenderType> cir) {
        if(entity instanceof LivingEntity living && VisualState.cloned(living))cir.setReturnValue(RenderType.entityTranslucent(texture));
    }
}
