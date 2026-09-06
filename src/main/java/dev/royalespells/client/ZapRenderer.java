package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.royalespells.Spell;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Short descending leader, bright return stroke, branching arcs, then drifting sparks. */
final class ZapRenderer {
    static void render(SpellEntity e,float delta,PoseStack m,MultiBufferSource buffers){
        float time=e.time()+delta;
        boolean second=e.spell()==Spell.ZAP_EVOLUTION&&time>=21;
        float age=time-(second?21:1);
        if(age<0||age>7)return;
        double radius=SpellEntity.zapRadius(second?21:1);
        long seed=e.getId()*7919L+(second?613:0);
        int frame=(int)(age*2.2f);
        float fade=Mth.clamp(1-age/6.5f,0,1);
        float pulse=(.7f+.3f*Mth.cos(age*8))*fade;
        var v=buffers.getBuffer(SpellLayers.EFFECT);
        // Warm white cores with an icy blue corona; the evolved second stroke has a violet fringe.
        float[] halo=second?new float[]{.52f,.45f,1}:new float[]{.2f,.6f,1};
        Vec3 top=new Vec3(.25,5.8,0),impact=new Vec3(0,.14,0);
        Vec3[] trunk=path(top,impact,13,.27,seed+frame*37);
        float reach=Mth.clamp((age+.18f)/.85f,0,1);
        if(age<4.8f){
            int segments=Math.max(1,Math.min(13,(int)Math.ceil(reach*13)));
            for(int i=0;i<segments;i++){
                Vec3 end=trunk[i+1];
                if(i==segments-1)end=trunk[i].lerp(end,Mth.clamp(reach*13-i,0,1));
                arc(m,v,trunk[i],end,.033f,pulse,halo);
            }
        }
        if(age<.55f)return;
        float discharge=Mth.clamp((age-.55f)*3,0,1)*fade;
        // Real victim positions make the forks connect to troops instead of forming four poles.
        List<Vec3> victims=e.zapPoints();
        for(int i=0;i<Math.min(8,victims.size());i++){
            Vec3 end=victims.get(i).subtract(e.target());
            if(end.lengthSqr()>36)continue;
            Vec3 from=trunk[9+(i%3)];
            stroke(m,v,from,end,5,.14,seed+i*71+frame*11,.018f,discharge,halo);
        }
        for(int i=0;i<7;i++){
            double angle=i*2.399963+noise(seed+i)*.45;
            double spread=radius*(.48+.4*noise(seed+i*17))*Math.min(1,(age-.4)*2);
            Vec3 end=new Vec3(Math.cos(angle)*spread,.12+noise(seed+i*43)*.38,Math.sin(angle)*spread);
            if(age<4.5f){
                stroke(m,v,impact,end,5,.12,seed+i*23+frame*31,.019f,discharge,halo);
                Vec3 fork=end.scale(.62).add(0,.12,0);
                Vec3 tip=end.add(Math.cos(angle+.9)*.45,.23,Math.sin(angle+.9)*.45);
                stroke(m,v,fork,tip,3,.07,seed+i+frame,.011f,discharge*.6f,halo);
            }
        }
        // A broken, expanding flash and rising electric flecks, with no static filled disc.
        for(int i=0;i<18;i++){
            double a=i*Math.PI/9+noise(seed+i)*.1;
            double r=radius*Math.min(1,.2+age*.2);
            if(age<3.5f&&i%3!=frame%3){
                Vec3 from=new Vec3(Math.cos(a)*r,.09,Math.sin(a)*r);
                Vec3 to=new Vec3(Math.cos(a+.1)*r,.09,Math.sin(a+.1)*r);
                arc(m,v,from,to,.014f,discharge*.6f,halo);
            }
            double d=radius*(.12+.72*noise(seed+i*97))+age*.045;
            double y=.16+age*(.09+.09*noise(seed+i*61));
            Vec3 p=new Vec3(Math.cos(a)*d,y,Math.sin(a)*d);
            float alpha=fade*fade*(.45f+.4f*(float)noise(seed+frame+i));
            arc(m,v,p,p.add(.025,.065,0),.012f,alpha,halo);
        }
    }
    private static double noise(long value){long n=(value^0x5deece66dL)*0x27d4eb2dL;n^=n>>>15;return (n&0xffffff)/(double)0xffffff;}
    private static Vec3[] path(Vec3 from,Vec3 to,int segments,double jitter,long seed){
        Vec3[] points=new Vec3[segments+1];points[0]=from;points[segments]=to;
        for(int i=1;i<segments;i++){
            double u=i/(double)segments;
            points[i]=from.lerp(to,u).add((noise(seed+i*79)-.5)*jitter*2,(noise(seed+i*137)-.5)*jitter*.6,(noise(seed+i*37)-.5)*jitter*2);
        }
        return points;
    }
    private static void stroke(PoseStack m,VertexConsumer v,Vec3 from,Vec3 to,int segments,double jitter,long seed,float width,float alpha,float[] halo){
        Vec3[] points=path(from,to,segments,jitter,seed);
        for(int i=0;i<segments;i++)arc(m,v,points[i],points[i+1],width*(1-i/(float)segments*.6f),alpha,halo);
    }
    private static void arc(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,float width,float alpha,float[] halo){
        if(alpha<=0||a.distanceToSqr(b)<1e-8)return;
        beam(m,v,a,b,width*3.4f,halo[0],halo[1],halo[2],alpha*.12f);
        beam(m,v,a,b,width*1.8f,.52f,.8f,1,alpha*.35f);
        beam(m,v,a,b,width,.91f,.98f,1,alpha);
    }
    private static void beam(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,float width,float r,float g,float blue,float alpha){
        Vec3 direction=b.subtract(a).normalize();
        Vec3 cross=direction.cross(Math.abs(direction.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width);
        Vec3 other=direction.cross(cross);
        for(Vec3 side:new Vec3[]{cross,other}){
            point(m,v,a.add(side),r,g,blue,alpha);point(m,v,a.subtract(side),r,g,blue,alpha);
            point(m,v,b.subtract(side),r,g,blue,alpha);point(m,v,b.add(side),r,g,blue,alpha);
        }
    }
    private static void point(PoseStack m,VertexConsumer v,Vec3 p,float r,float g,float b,float a){v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,a);}
}
