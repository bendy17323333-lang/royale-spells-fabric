package dev.royalespells.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import dev.royalespells.client.FrozenRender;

@Mixin(ModelPart.class)
public abstract class FrozenModelPartMixin {
    @WrapMethod(method="translateAndRotate")
    private void royalePose(PoseStack matrices,Operation<Void> original){
        ModelPart part=(ModelPart)(Object)this;
        float[] actual={part.x,part.y,part.z,part.xRot,part.yRot,part.zRot,part.xScale,part.yScale,part.zScale};
        float[] held=FrozenRender.pose(part,actual);
        apply(part,held);try{original.call(matrices);}finally{apply(part,actual);}
    }
    @org.spongepowered.asm.mixin.Unique private static void apply(ModelPart p,float[] v){p.x=v[0];p.y=v[1];p.z=v[2];p.xRot=v[3];p.yRot=v[4];p.zRot=v[5];p.xScale=v[6];p.yScale=v[7];p.zScale=v[8];}
}
