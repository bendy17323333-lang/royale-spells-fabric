package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.royalespells.RoyaleSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class SpellOverlays {
    public static void block(PoseStack m,MultiBufferSource v,int light,Block block,double x,double y,double z,float w,float h,float d){
        m.pushPose();m.translate(x,y,z);m.scale(w,h,d);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),m,v,light,OverlayTexture.NO_OVERLAY);m.popPose();
    }
    public static void render(LivingEntity e,float delta,PoseStack m,MultiBufferSource v,int light){
        if(e.isInvisible())return;
        float width=e.getBbWidth()+.16f,height=e.getBbHeight()+.12f;
        if(dev.royalespells.VisualState.frozen(e)) {
            // Thin ice shell retains visibility of the frozen creature through the ice.
            block(m,v,light,Blocks.ICE,-width/2,0,-width/2,width,height,.07f);
            block(m,v,light,Blocks.ICE,-width/2,0,width/2-.07,width,height,.07f);
            block(m,v,light,Blocks.ICE,-width/2,0,-width/2,.07f,height,width);
            block(m,v,light,Blocks.ICE,width/2-.07,0,-width/2,.07f,height,width);
            block(m,v,light,Blocks.ICE,-width/2,height,-width/2,width,.06f,width);
            for(int i=0;i<5;i++){m.pushPose();m.mulPose(Axis.YP.rotationDegrees(i*72));m.translate(width*.48,0,0);m.mulPose(Axis.ZP.rotationDegrees(-17));block(m,v,light,Blocks.PACKED_ICE,-.06,0,-.06,.12f,.28f+i*.035f,.12f);m.popPose();}
        }
        if(dev.royalespells.VisualState.rooted(e)) {
            double radius=width*.56;
            for(int vine=0;vine<3;vine++)for(int i=0;i<16;i++) {
                double y=i*height/17,a=i*.65+vine*Math.PI*2/3;
                double x=Math.cos(a)*radius,z=Math.sin(a)*radius;
                Vec3 from=new Vec3(x,y,z),to=new Vec3(Math.cos(a+.65)*radius,(i+1)*height/17,Math.sin(a+.65)*radius);
                Vec3 direction=to.subtract(from);
                m.pushPose();m.translate(from.x,from.y,from.z);
                m.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),direction.normalize().toVector3f()));
                block(m,v,light,Blocks.MOSS_BLOCK,-.065,0,-.065,.13f,(float)direction.length()+.025f,.13f);m.popPose();
                if(i%4==0){m.pushPose();m.translate(x,y,z);m.mulPose(Axis.YP.rotation((float)-a));m.mulPose(Axis.ZP.rotationDegrees(30));block(m,v,light,Blocks.GREEN_TERRACOTTA,0,0,-.035,.26f,.1f,.07f);m.popPose();}
            }
        }
    }
}

