package dev.royalespells.client;
import com.mojang.blaze3d.vertex.*;
import dev.royalespells.entity.ArmySkeleton;
import net.minecraft.client.renderer.*;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.util.Mth;

/** Vanilla skeleton mesh, texture and held-item transforms, with a smaller overall scale. */
public final class RoyaleSkeletonRenderer<T extends Skeleton> extends MobRenderer<T,RoyaleSkeletonRenderer.Model<T>> {
    public static final ResourceLocation TEXTURE=ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
    public RoyaleSkeletonRenderer(EntityRendererProvider.Context context){
        super(context,new Model<>(context.bakeLayer(ModelLayers.SKELETON)),.25f);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()){
            @Override public void render(PoseStack p,MultiBufferSource b,int light,T e,float a,float d,float tick,float age,float yaw,float pitch){if(!(e instanceof ArmySkeleton army&&army.general()))super.render(p,b,light,e,a,d,tick,age,yaw,pitch);}
        });
        addLayer(new RenderLayer<>(this){
            private final GeneralEquipment gear=new GeneralEquipment();
            @Override public void render(PoseStack p,MultiBufferSource b,int light,T e,float a,float d,float tick,float age,float yaw,float pitch){if(e instanceof ArmySkeleton army&&army.general())gear.render(army,model,p,b,light,tick);}
        });
    }
    @Override public void render(T entity,float yaw,float delta,PoseStack matrices,MultiBufferSource buffers,int light){
        matrices.pushPose();if(entity instanceof ArmySkeleton army){float rise=net.minecraft.util.Mth.clamp((army.age()+delta)/18,0,1);matrices.translate(0,-(1-rise)*.85,0);}
        super.render(entity,yaw,delta,matrices,buffers,light);matrices.popPose();
    }
    @Override protected void scale(T e,PoseStack p,float delta){p.scale(.7f,.7f,.7f);}
    @Override protected RenderType getRenderType(T e,boolean visible,boolean translucent,boolean outline){return e instanceof ArmySkeleton army&&(army.ghost()||army.dissolve()>0)?SpellLayers.SPECTRAL:super.getRenderType(e,visible,translucent,outline);}
    @Override public ResourceLocation getTextureLocation(T e){return TEXTURE;}
    public static final class Model<T extends Skeleton> extends SkeletonModel<T>{
        private float opacity=1;private boolean ghost;
        Model(ModelPart root){super(root);}
        @Override public void setupAnim(T e,float stride,float speed,float age,float yaw,float pitch){
            super.setupAnim(e,stride,speed,age,yaw,pitch);opacity=1;ghost=false;
            if(e instanceof ArmySkeleton army){
                ghost=army.ghost();opacity=(ghost?.42f:1)*(1-Mth.clamp(army.dissolve()/20f,0,1));
                float t=army.strikeProgress(age-e.tickCount),wind=wind(t),hit=thrust(t);
                if(army.general()){
                    rightArm.xRot=-.05f+wind*.28f-hit*1.2f;rightArm.yRot=-wind*.12f;rightArm.zRot=.03f;
                    leftArm.xRot=-.4f;leftArm.yRot=.12f;leftArm.zRot=-.06f;
                }else if(t>0){rightArm.xRot=-wind*2.2f-hit*.5f;rightArm.yRot=-wind*.2f+hit*.3f;leftArm.xRot=Mth.cos(stride*.6662f)*speed*.5f;}
            }
        }
        static float wind(float t){return (float)Mth.smoothstep(Mth.clamp(t<.3f?t/.3f:1-(t-.3f)/.14f,0,1));}
        static float thrust(float t){return (float)Mth.smoothstep(Mth.clamp(t<.44f?(t-.3f)/.14f:(1-t)/.56f,0,1));}
        @Override public void renderToBuffer(PoseStack p,VertexConsumer v,int light,int overlay,int color){int alpha=Math.round((color>>>24)*opacity);int rgb=ghost?0xB684FF:color&0xffffff;super.renderToBuffer(p,v,light,overlay,(alpha<<24)|rgb);}
    }
}
