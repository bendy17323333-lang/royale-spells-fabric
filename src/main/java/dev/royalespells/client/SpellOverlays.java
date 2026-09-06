package dev.royalespells.client;

import dev.royalespells.RoyaleSpells;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

public final class SpellOverlays {
    public static void block(MatrixStack m,VertexConsumerProvider v,int light,Block block,double x,double y,double z,float w,float h,float d){
        m.push();m.translate(x,y,z);m.scale(w,h,d);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(block.getDefaultState(),m,v,light,OverlayTexture.DEFAULT_UV);m.pop();
    }
    public static void render(LivingEntity e,float delta,MatrixStack m,VertexConsumerProvider v,int light){
        if(e.isInvisible())return;
        float width=e.getWidth()+.16f,height=e.getHeight()+.12f;
        if(dev.royalespells.VisualState.frozen(e)) {
            // Thin ice shell retains visibility of the frozen creature through the ice.
            block(m,v,light,Blocks.ICE,-width/2,0,-width/2,width,height,.07f);
            block(m,v,light,Blocks.ICE,-width/2,0,width/2-.07,width,height,.07f);
            block(m,v,light,Blocks.ICE,-width/2,0,-width/2,.07f,height,width);
            block(m,v,light,Blocks.ICE,width/2-.07,0,-width/2,.07f,height,width);
            block(m,v,light,Blocks.ICE,-width/2,height,-width/2,width,.06f,width);
            for(int i=0;i<5;i++){m.push();m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i*72));m.translate(width*.48,0,0);m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-17));block(m,v,light,Blocks.PACKED_ICE,-.06,0,-.06,.12f,.28f+i*.035f,.12f);m.pop();}
        }
        if(dev.royalespells.VisualState.rooted(e)) {
            double radius=width*.56;
            for(int vine=0;vine<3;vine++)for(int i=0;i<16;i++) {
                double y=i*height/17,a=i*.65+vine*Math.PI*2/3;
                double x=Math.cos(a)*radius,z=Math.sin(a)*radius;
                Vec3d from=new Vec3d(x,y,z),to=new Vec3d(Math.cos(a+.65)*radius,(i+1)*height/17,Math.sin(a+.65)*radius);
                Vec3d direction=to.subtract(from);
                m.push();m.translate(from.x,from.y,from.z);
                m.multiply(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),direction.normalize().toVector3f()));
                block(m,v,light,Blocks.MOSS_BLOCK,-.065,0,-.065,.13f,(float)direction.length()+.025f,.13f);m.pop();
                if(i%4==0){m.push();m.translate(x,y,z);m.multiply(RotationAxis.POSITIVE_Y.rotation((float)-a));m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30));block(m,v,light,Blocks.GREEN_TERRACOTTA,0,0,-.035,.26f,.1f,.07f);m.pop();}
            }
        }
    }
}

