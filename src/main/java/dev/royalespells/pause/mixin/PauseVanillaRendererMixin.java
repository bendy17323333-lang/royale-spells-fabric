package dev.royalespells.pause.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.pause.PauseClock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LivingEntityRenderer.class)
public abstract class PauseVanillaRendererMixin {
    @WrapMethod(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void localAnimationTime(LivingEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,Operation<Void> original){
        int real=e.tickCount;double local=PauseClock.sample(e,partial);e.tickCount=(int)Math.floor(local);
        try{original.call(e,yaw,(float)(local-Math.floor(local)),pose,buffers,light);}finally{e.tickCount=real;}
    }
}
