package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.royalespells.entity.SpellEntity;
import dev.royalespells.FieldAnimation;
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
    private static void gradient(PoseStack m,VertexConsumer v,double inner,double outer,double y,float[] a,float[] b,float opacity,float time) {
        for(int i=0;i<96;i++) {
            double t0=i*Math.PI/48,t1=(i+1)*Math.PI/48;
            double wave0=1+.012*Math.sin(t0*7+time*.09),wave1=1+.012*Math.sin(t1*7+time*.09);
            float light=.94f+.06f*(float)Math.sin(t0*4-time*.12);
            vertex(m,v,Math.cos(t0)*inner*wave0,y,Math.sin(t0)*inner*wave0,a[0]*light,a[1]*light,a[2]*light,a[3]*opacity);
            vertex(m,v,Math.cos(t1)*inner*wave1,y,Math.sin(t1)*inner*wave1,a[0]*light,a[1]*light,a[2]*light,a[3]*opacity);
            vertex(m,v,Math.cos(t1)*outer*wave1,y,Math.sin(t1)*outer*wave1,b[0]*light,b[1]*light,b[2]*light,b[3]*opacity);
            vertex(m,v,Math.cos(t0)*outer*wave0,y,Math.sin(t0)*outer*wave0,b[0]*light,b[1]*light,b[2]*light,b[3]*opacity);
        }
    }
    public static void render(SpellEntity e,float delta,PoseStack m,MultiBufferSource consumers) {
        float time=e.time()+delta,opacity=FieldAnimation.opacity(e.spell(),time,e.duration());
        double radius=e.spell().radius*FieldAnimation.radius(e.spell(),time,e.duration());
        VertexConsumer glow=consumers.getBuffer(SpellLayers.EFFECT);
        // Broad wine-dark interior, crimson transition, ember rim and diffuse orange falloff.
        // Interpolated vertex colours avoid opaque concentric bands and retain the terrain underneath.
        gradient(m,glow,0,radius*.66,.075,new float[]{.065f,.006f,.035f,.58f},new float[]{.19f,.007f,.04f,.55f},opacity,time);
        gradient(m,glow,radius*.66,radius*.91,.077,new float[]{.19f,.007f,.04f,.55f},new float[]{.5f,.025f,.055f,.54f},opacity,time);
        gradient(m,glow,radius*.91,radius,.08,new float[]{.5f,.025f,.055f,.54f},new float[]{1,.25f,.065f,.86f},opacity,time);
        gradient(m,glow,radius,radius*1.055,.082,new float[]{1,.25f,.065f,.86f},new float[]{.96f,.07f,.025f,0},opacity,time);
        float opening=FieldAnimation.opening(e.spell(),time);
        if(time<14)ring(m,glow,0,.1,0,Math.max(0,radius-.07),radius+.1,1,.46f,.14f,opacity*(1-opening)*.85f);
        float breath=.12f+.05f*(float)Math.sin(time*.19);
        gradient(m,glow,radius*.76,radius*.97,.09,new float[]{.8f,.028f,.12f,0},new float[]{.93f,.08f,.075f,breath},opacity,time+10);
        float age=time-e.voidStrikeTick();
        if(e.voidStrikeTick()==0 || age<0 || age>9)return;
        float fade=Mth.clamp((9-age)/5,0,1)*opacity,width=.13f+.12f*e.voidStrength();
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



