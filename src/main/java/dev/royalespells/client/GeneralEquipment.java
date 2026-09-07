package dev.royalespells.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.royalespells.entity.ArmySkeleton;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import java.util.*;

/** Block-built equipment attached directly to vanilla head, body and hand bones. */
final class GeneralEquipment {
    private record Piece(ModelPart part,int color,boolean glow){}
    private final List<Piece> helmet=new ArrayList<>(),staff=new ArrayList<>(),shield=new ArrayList<>(),cape=new ArrayList<>();
    private final List<List<Piece>> flags=new ArrayList<>();
    private final PoseStack helmetLocal=new PoseStack();
    private static final int STEEL=0x707B8B,EDGE=0xB2BAC8,GOLD=0xDCA437,BLUE=0x2458C5,WOOD=0x78401E,PURPLE=0x712CA8;
    private static void box(List<Piece> list,int color,float x,float y,float z,float w,float h,float d){box(list,color,x,y,z,w,h,d,false);}
    private static void box(List<Piece> list,int color,float x,float y,float z,float w,float h,float d,boolean glow){
        var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(388,388).addBox(x,y,z,w,h,d),PartPose.ZERO);
        list.add(new Piece(LayerDefinition.create(mesh,512,512).bakeRoot(),color,glow));
    }
    GeneralEquipment(){
        box(helmet,STEEL,-4.7f,-8.9f,-4.7f,9.4f,1.1f,9.4f);
        box(helmet,STEEL,-4.9f,-7.8f,-4.7f,.7f,8.15f,9.4f);box(helmet,STEEL,4.2f,-7.8f,-4.7f,.7f,8.15f,9.4f);
        box(helmet,STEEL,-4.4f,-7.8f,4,8.8f,8.15f,.7f);
        box(helmet,STEEL,-4.7f,-7.8f,-4.95f,9.4f,2.95f,.7f);
        // One continuous T opening: the unbroken eye slit joins the central mouth
        // opening. Only the cheek corners continue down around the sides of the jaw.
        box(helmet,STEEL,-4.9f,-3.45f,-5,2.4f,3.8f,.7f);box(helmet,STEEL,2.5f,-3.45f,-5,2.4f,3.8f,.7f);
        for(float x:new float[]{-5.05f,2.5f})box(helmet,EDGE,x,.05f,-5.1f,2.55f,.45f,.8f);
        for(float x:new float[]{-5.05f,4.25f})box(helmet,EDGE,x,.05f,-4.4f,.8f,.45f,9.25f);
        box(helmet,EDGE,-4.5f,.05f,4.1f,9,.45f,.75f);
        box(helmet,0x100E1C,-4.4f,-4.9f,-4.7f,8.8f,1.6f,.3f);
        for(float x:new float[]{-2.9f,1.3f}){box(helmet,0x8435E7,x,-4.65f,-4.94f,1.6f,.85f,.12f,true);box(helmet,0xF9D3FF,x+.5f,-4.55f,-5.02f,.6f,.55f,.09f,true);}
        box(helmet,GOLD,-.55f,-8.8f,-5.05f,1.1f,3.4f,.3f);box(helmet,GOLD,-.9f,-9.3f,-3.6f,1.8f,.5f,7.2f);
        box(helmet,BLUE,-.7f,-12.1f,-3.4f,1.4f,2.8f,6.8f);box(helmet,0x3C78F0,-.73f,-12.1f,-3.4f,.2f,2.4f,6.8f);
        box(staff,WOOD,-.35f,-24,0,.7f,33,.7f);box(staff,GOLD,-.6f,-24.6f,-.25f,1.2f,1.1f,1.2f);
        box(staff,EDGE,-.6f,-27,-.1f,1.2f,2.6f,.9f);box(staff,EDGE,-.28f,-28.2f,.02f,.56f,1.2f,.64f);
        box(staff,PURPLE,-4,-22,-.05f,8,.65f,.8f);
        for(float x:new float[]{-4.4f,3.6f})box(staff,GOLD,x,-22.25f,-.25f,.8f,1.15f,1.2f);
        String[] skull={"00111100","01111110","11111111","11011011","11011011","11100111","01111110","00101000"};
        for(int i=0;i<4;i++){
            var flag=new ArrayList<Piece>();box(flag,PURPLE,0,0,0,1.8f,8.5f+(i%2),.18f);box(flag,0xA652DF,0,0,-.04f,1.8f,.3f,.28f);
            for(int row=0;row<skull.length;row++)for(int col=0;col<2;col++)if(skull[row].charAt(i*2+col)=='1')box(flag,0xF2EFE3,col*.9f,1+row*.8f,-.08f,.9f,.8f,.36f);
            flags.add(flag);
        }
        box(shield,WOOD,-3.5f,-5,-1,7,8,1);box(shield,WOOD,-2.5f,3,-1,5,1,1);box(shield,WOOD,-1.5f,4,-1,3,1,1);
        box(shield,EDGE,-3.7f,-5.2f,-1.2f,7.4f,.55f,1.3f);
        for(float x:new float[]{-3.7f,3.2f})box(shield,EDGE,x,-5,-1.2f,.5f,8.2f,1.3f);
        for(int i=0;i<3;i++){float width=7-i*2;box(shield,EDGE,-width/2,2.9f+i,-1.2f,width,.5f,1.3f);}
        for(float x:new float[]{-1.7f,.1f,1.8f})box(shield,0x3E2617,x,-4.5f,-1.03f,.12f,7.2f,.15f);
        box(shield,GOLD,-1,-1.7f,-1.65f,2,2,.7f);
        box(cape,PURPLE,-3.6f,0,0,7.2f,9.2f,.3f);box(cape,GOLD,-3.6f,0,-.1f,7.2f,.5f,.5f);box(cape,GOLD,-3.6f,8.7f,-.1f,7.2f,.5f,.5f);
    }
    private void parts(List<Piece> parts,PoseStack p,VertexConsumer v,int light,float alpha){for(var part:parts)part.part.render(p,v,part.glow?15728880:light,OverlayTexture.NO_OVERLAY,((int)(255*alpha)<<24)|part.color);}
    void render(ArmySkeleton e,SkeletonModel<?> base,PoseStack p,MultiBufferSource buffers,int light,float delta){
        float alpha=1-Mth.clamp(e.dissolve()/20f,0,1),age=e.age()+delta,t=e.strikeProgress(delta),hit=RoyaleSkeletonRenderer.Model.thrust(t);
        var v=buffers.getBuffer(e.dissolve()>0?SpellLayers.ARMY_GEAR_FADE:SpellLayers.ARMY_GEAR);p.pushPose();base.head.translateAndRotate(p);parts(helmet,helmetLocal,new HelmetTaper(v,p.last()),light,alpha);p.popPose();
        p.pushPose();base.body.translateAndRotate(p);p.translate(0,.04,.15);p.mulPose(Axis.XP.rotation(.12f+Mth.sin(age*.1f)*.055f));parts(cape,p,v,light,alpha);p.popPose();
        p.pushPose();base.rightArm.translateAndRotate(p);p.translate(0,9/16f,0);p.mulPose(Axis.XP.rotation(-base.rightArm.xRot+hit*1.52f));parts(staff,p,v,light,alpha);
        for(int i=0;i<4;i++){p.pushPose();p.translate((-3.6f+i*1.8f)/16f,-21.5f/16f,0);p.mulPose(Axis.YP.rotation(Mth.sin(age*.14f-i*.5f)*.10f));p.mulPose(Axis.XP.rotation(.12f+Mth.sin(age*.12f-i*.55f)*.075f+hit*.2f));parts(flags.get(i),p,v,light,alpha);p.popPose();}p.popPose();
        if(e.shield()>0){p.pushPose();base.leftArm.translateAndRotate(p);p.translate(0,8/16f,-.07);p.mulPose(Axis.XP.rotation(.4f));parts(shield,p,v,light,alpha);p.popPose();}
    }
}
