package dev.royalespells.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.*;
import software.bernie.geckolib.cache.object.GeoBone;
import dev.royalespells.client.FrozenRender;

@Pseudo
@Mixin(targets="software.bernie.geckolib.util.RenderUtil",remap=false)
public abstract class IronFrozenBoneMixin {
    @WrapMethod(method={"translateMatrixToBone","rotateMatrixAroundBone","scaleMatrixForBone"},remap=false)
    private static void royaleGeoPose(PoseStack matrices,GeoBone bone,Operation<Void> original){
        float[] actual={bone.getPosX(),bone.getPosY(),bone.getPosZ(),bone.getRotX(),bone.getRotY(),bone.getRotZ(),bone.getScaleX(),bone.getScaleY(),bone.getScaleZ()};
        float[] held=FrozenRender.pose(bone,actual);apply(bone,held);try{original.call(matrices,bone);}finally{apply(bone,actual);}
    }
    @Unique private static void apply(GeoBone b,float[] v){b.updatePosition(v[0],v[1],v[2]);b.updateRotation(v[3],v[4],v[5]);b.updateScale(v[6],v[7],v[8]);}
}
