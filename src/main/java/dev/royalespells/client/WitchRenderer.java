package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.RoyaleSpells;
import dev.royalespells.entity.RoyalWitch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Native Blockbench atlas; the staff and simple block hand share the arm hierarchy. */
public final class WitchRenderer extends MobRenderer<RoyalWitch,TroopModel<RoyalWitch>> {
    private final java.util.Map<RoyalWitch,dev.royalespells.AnimationTimeline> hoverClocks=new java.util.WeakHashMap<>();
    private final TroopModel<RoyalWitch> normal=new TroopModel<>("witch");
    private final TroopModel<RoyalWitch> evolved=new TroopModel<>("witch_evolution");
    public WitchRenderer(EntityRendererProvider.Context context){
        super(context,new TroopModel<>("witch"),.32f);
        addLayer(new RenderLayer<RoyalWitch,TroopModel<RoyalWitch>>(this){
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,RoyalWitch e,float limb,float amount,float partial,float age,float yaw,float pitch){
                if(!e.isInvisible()){
                    pose.pushPose();
                    if(getParentModel().transformToJoint("left_forearm",pose)){
                        pose.translate(0,4/16f,-2/16f);
                        WitchMagicRenderer.chargeAnchor(e,new org.joml.Matrix4f(pose.last().pose()));
                    }
                    pose.popPose();
                    getParentModel().renderEmissive(pose,buffers.getBuffer(RenderType.eyes(getTextureLocation(e))));
                }
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(RoyalWitch e){return RoyaleSpells.id("textures/entity/witch_materials.png");}
    @Override public void render(RoyalWitch e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        model=e.evolved()?evolved:normal;
        model.setRenderPartial(partial);
        // The soul's arrival briefly illuminates her existing surfaces, not an
        // enclosing billboard or a global shader tint that would leak to others.
        pose.pushPose();
        if(e.evolved()){
            boolean paused=dev.royalespells.VisualState.frozen(e)||dev.royalespells.pause.ElectricPause.active(e);
            double rate=paused?0:dev.royalespells.VisualState.snowbound(e)?dev.royalespells.SnowballChill.ANIMATION_RATE:1;
            double age=hoverClocks.computeIfAbsent(e,k->new dev.royalespells.AnimationTimeline()).sample(e.tickCount+partial,rate);
            pose.translate(0,Math.sin(age*.08)*.025,0);
        }
        // Vanilla body yaw can trail the actual shot heading by many degrees,
        // notably after deployment. During a cast, render the torso on that
        // heading too, so a correct left-palm muzzle does not appear at the chest.
        float body=e.yBodyRot,oldBody=e.yBodyRotO;
        boolean held=dev.royalespells.VisualState.frozen(e)||dev.royalespells.pause.ElectricPause.active(e);
        if(e.attackAge()>0&&!held){e.yBodyRot=e.getYRot();e.yBodyRotO=e.yRotO;}
        try{super.render(e,yaw,partial,pose,buffers,e.healFlash()>6?15728880:light);}
        finally{e.yBodyRot=body;e.yBodyRotO=oldBody;pose.popPose();}
    }
}
