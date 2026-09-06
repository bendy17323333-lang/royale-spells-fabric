package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Texture-free dark field, hot rim, and descending per-victim beams. */
public final class VoidRenderer {
    private static void vertex(PoseStack m,VertexConsumer v,double x,double y,double z,float r,float g,float b,float a){
        v.addVertex(m.last().pose(),(float)x,(float)y,(float)z).setColor(r,g,b,a);
    }
    private static void ring(PoseStack m,VertexConsumer v,double x,double y,double z,double inner,double outer,float r,float g,float b,float alpha){
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,c=(i+1)*Math.PI/32;
            vertex(m,v,x+Math.cos(a)*inner,y,z+Math.sin(a)*inner,r,g,b,alpha);
            vertex(m,v,x+Math.cos(c)*inner,y,z+Math.sin(c)*inner,r,g,b,alpha);
            vertex(m,v,x+Math.cos(c)*outer,y,z+Math.sin(c)*outer,r,g,b,alpha);
            vertex(m,v,x+Math.cos(a)*outer,y,z+Math.sin(a)*outer,r,g,b,alpha);
        }
    }
    private static void beam(PoseStack m,VertexConsumer v,Vec3 point,float age,float width,float r,float g,float b,float alpha) {
        double bottom=12*Math.max(0,1-age/2.0),top=12;
        for(int segment=0;segment<8;segment++) {
            double y0=bottom+(top-bottom)*segment/8,y1=bottom+(top-bottom)*(segment+1)/8;
            double bend0=Math.sin(segment*1.7)*.09,bend1=Math.sin((segment+1)*1.7)*.09;
            double w0=width*(1-y0/18),w1=width*(1-y1/18);
            for(int side=0;side<4;side++) {
                double a=side*Math.PI/2+Math.PI/4,c=a+Math.PI/2;
                vertex(m,v,point.x+bend0+Math.cos(a)*w0,point.y+y0,point.z+Math.sin(a)*w0,r,g,b,alpha);
                vertex(m,v,point.x+bend0+Math.cos(c)*w0,point.y+y0,point.z+Math.sin(c)*w0,r,g,b,alpha);
                vertex(m,v,point.x+bend1+Math.cos(c)*w1,point.y+y1,point.z+Math.sin(c)*w1,r,g,b,alpha);
                vertex(m,v,point.x+bend1+Math.cos(a)*w1,point.y+y1,point.z+Math.sin(a)*w1,r,g,b,alpha);
            }
        }
    }
    public static void render(SpellEntity e,float delta,PoseStack m,MultiBufferSource consumers) {
        float time=e.time()+delta;double radius=e.spell().radius;
        ring(m,consumers.getBuffer(SpellLayers.EFFECT),0,.07,0,0,radius,.05f,.009f,.003f,.8f);
        VertexConsumer glow=consumers.getBuffer(SpellLayers.EFFECT);
        float pulse=.65f+.2f*(float)Math.sin(time*.25);
        ring(m,glow,0,.085,0,radius-.09,radius+.06,1,.12f,.015f,pulse);
        ring(m,glow,0,.09,0,radius+.06,radius+.22,1,.07f,.005f,.2f);
        float age=time-e.voidStrikeTick();
        if(e.voidStrikeTick()==0 || age<0 || age>9)return;
        float fade=Mth.clamp((9-age)/5,0,1),width=.13f+.12f*e.voidStrength();
        for(Vec3 worldPoint:e.voidStrikePoints()) {
            Vec3 point=worldPoint.subtract(e.target());
            // Draw the hot core first so transparent outer faces cannot depth-occlude it.
            beam(m,glow,point,age,width*.30f,1,.88f,.5f,fade);
            beam(m,glow,point,age,width,1,.16f,.015f,.32f*fade);
            beam(m,glow,point,age,width*2,1,.035f,.005f,.13f*fade);
            if(age>=2) {
                ring(m,glow,point.x,.12,point.z,.15+age*.08,.3+age*.11,1,.2f,.02f,.7f*fade);
                // A bright flare crosses the beam at impact height.
                m.pushPose();m.translate(point.x,point.y,point.z);m.mulPose(Axis.XP.rotationDegrees(90));
                ring(m,glow,0,0,0,0,width*1.4,1,.62f,.1f,.6f*fade);m.popPose();
            }
        }
    }
}




