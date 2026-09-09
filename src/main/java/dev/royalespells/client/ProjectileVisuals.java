package dev.royalespells.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.*;

/** Faceted, textured fire core and curved animated flame wake. All translucent
 * shells, wake and impacts use COLOR_WRITE, never an invisible depth occluder. */
public final class ProjectileVisuals {
    private record Impact(ClientLevel world,Vec3 at,Spell spell,double radius,long born,int seed){}
    private static final Deque<Impact> IMPACTS=new ArrayDeque<>();
    private static final Deque<Impact> FIRE_BLASTS=new ArrayDeque<>();
    public static void impact(SpellEntity e,byte status) {
        if(!(e.level() instanceof ClientLevel world))return;
        Vec3 at=status==65?e.position():e.target();
        if(e.spell()==Spell.FIREBALL){
            FireballParticles.impact(world,at,e.radius());
            if(FIRE_BLASTS.size()>=96)FIRE_BLASTS.removeFirst();
            FIRE_BLASTS.addLast(new Impact(world,at,e.spell(),e.radius(),world.getGameTime(),e.getId()));return;
        }
        if(IMPACTS.size()>=96)IMPACTS.removeFirst();
        IMPACTS.addLast(new Impact(world,at,e.spell(),e.radius(),world.getGameTime(),e.getId()));
    }
    public static void core(SpellEntity e,float delta,PoseStack m,MultiBufferSource buffers) {
        m.pushPose();m.mulPose(Axis.YP.rotationDegrees((e.time()+delta)*7));m.mulPose(Axis.ZP.rotationDegrees(17));
        var v=buffers.getBuffer(SpellLayers.FIRE_CORE);
        // Eight angular sides and five cross sections give a bevelled stone
        // silhouette, instead of the previous single block or a smooth sphere.
        float[] ys={-.56f,-.39f,.20f,.47f,.57f},rs={.20f,.50f,.56f,.38f,.16f};
        for(int j=0;j<ys.length-1;j++)for(int i=0;i<8;i++){
            double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
            Vec3 p=new Vec3(Math.cos(a)*rs[j],ys[j],Math.sin(a)*rs[j]);
            Vec3 q=new Vec3(Math.cos(b)*rs[j],ys[j],Math.sin(b)*rs[j]);
            Vec3 r=new Vec3(Math.cos(b)*rs[j+1],ys[j+1],Math.sin(b)*rs[j+1]);
            Vec3 s=new Vec3(Math.cos(a)*rs[j+1],ys[j+1],Math.sin(a)*rs[j+1]);
            textured(m,v,p,q,r,s,i/8f,(i+1)/8f,j/4f,(j+1)/4f);
        }
        for(int end:new int[]{0,4})for(int i=0;i<8;i++){
            double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
            textured(m,v,new Vec3(0,ys[end],0),new Vec3(Math.cos(a)*rs[end],ys[end],Math.sin(a)*rs[end]),new Vec3(Math.cos(b)*rs[end],ys[end],Math.sin(b)*rs[end]),new Vec3(0,ys[end],0),0,1,0,1);
        }
        m.popPose();
    }
    private static void textured(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float u0,float u1,float v0,float v1) {
        var pose=m.last();
        Vec3[] points={a,b,c,d};float[][] uv={{u0,v0},{u1,v0},{u1,v1},{u0,v1}};
        for(int i=0;i<4;i++)v.addVertex(pose.pose(),(float)points[i].x,(float)points[i].y,(float)points[i].z).setUv(uv[i][0],uv[i][1]).setColor(255,255,255,255);
    }
    public static void flight(SpellEntity e,float delta,PoseStack m,MultiBufferSource buffers) {
        if(e.spell()!=Spell.FIREBALL)return;
        float age=e.time()+delta;Vec3 position=e.visualPosition(delta);
        var rotation=Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        Vec3 right=new Vec3(new Vector3f(1,0,0).rotate(rotation)),up=new Vec3(new Vector3f(0,1,0).rotate(rotation));
        Vec3 forward=e.visualPosition(delta+.2f).subtract(e.visualPosition(delta-.2f)).normalize();
        if(forward.lengthSqr()<.1)forward=e.castDirection();
        Vec3 side=forward.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        Vec3 normal=side.cross(forward).normalize();var v=buffers.getBuffer(SpellLayers.FIRE_TRAIL);
        // Birth-anchored compact billows: no long planes, ribbon outlines or
        // shared converging tips. Half-tick spacing forms a filled volume.
        float phase=(age*2)%1;
        for(int i=27;i>=0;i--) {
            float elapsed=(i+phase)*.5f;
            if(elapsed>age)continue;
            float p=elapsed/14f,fade=1-p*p*(3-2*p);
            int birth=(int)(age*2)-i;
            double seed=birth*2.399963+e.getId()*.71;
            Vec3 center=e.visualPosition(delta-elapsed).subtract(position)
                .add(side.scale(Math.sin(seed+elapsed*.31)*.18*p))
                .add(normal.scale(Math.cos(seed*.73+elapsed*.24)*.18*p));
            float size=(float)((.69*Math.pow(1-p,.62)+.05)*(1+.13*Math.sin(seed+elapsed*.4)));
            int frame=Math.floorMod(birth+(int)(elapsed*.75),4);
            puff(m,v,center,right,up,size,seed+elapsed*.11,frame,.88f*fade+.04f*(1-p));
            if(i%3==0&&p>.12f) {
                Vec3 flick=center.add(side.scale(Math.sin(seed*1.8)*(.25+p*.22)))
                    .add(normal.scale(Math.cos(seed*1.8)*(.25+p*.22)));
                puff(m,v,flick,right,up,size*.57f,-seed-elapsed*.18,(frame+1)&3,.48f*fade);
            }
        }
    }
    private static void puff(PoseStack m,VertexConsumer v,Vec3 at,Vec3 right,Vec3 up,float size,double angle,int frame,float alpha) {
        Vec3 x=right.scale(Math.cos(angle)*size).add(up.scale(Math.sin(angle)*size));
        Vec3 y=right.scale(-Math.sin(angle)*size).add(up.scale(Math.cos(angle)*size));
        float u=(frame%2)*.5f+.008f,t=(frame/2)*.5f+.008f,end=.484f;
        flamePoint(m,v,at.subtract(x).subtract(y),u,t+end,alpha);
        flamePoint(m,v,at.add(x).subtract(y),u+end,t+end,alpha);
        flamePoint(m,v,at.add(x).add(y),u+end,t,alpha);
        flamePoint(m,v,at.subtract(x).add(y),u,t,alpha);
    }
    private static void flamePoint(PoseStack m,VertexConsumer v,Vec3 at,float u,float texV,float alpha) {
        v.addVertex(m.last().pose(),(float)at.x,(float)at.y,(float)at.z).setUv(u,texV).setColor(1f,1f,1f,alpha);
    }
    public static void impacts(ClientLevel world,float partial,PoseStack m,Vec3 camera,MultiBufferSource buffers) {
        IMPACTS.removeIf(e->e.world!=world||world.getGameTime()-e.born>22);
        FIRE_BLASTS.removeIf(e->e.world!=world||world.getGameTime()-e.born>9);
        var rotation=Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        Vec3 right=new Vec3(new Vector3f(1,0,0).rotate(rotation)),up=new Vec3(new Vector3f(0,1,0).rotate(rotation));
        if(!FIRE_BLASTS.isEmpty()) {
            var flame=buffers.getBuffer(SpellLayers.FIRE_TRAIL);
            for(Impact e:FIRE_BLASTS) {
                float age=world.getGameTime()-e.born+partial,p=Mth.clamp(age/9,0,1);
                m.pushPose();m.translate(e.at.x-camera.x,e.at.y-camera.y,e.at.z-camera.z);
                // Short, irregular combustion volume. Smoke and ballistic sparks
                // survive it, without a ground ring or a screen-filling flash.
                for(int i=0;i<7;i++) {
                    double seed=i*2.39996+e.seed*.31,d=i==0?0:.22+age*.095;
                    Vec3 at=new Vec3(Math.cos(seed)*d,.32+age*.07+(i%3)*.19,Math.sin(seed)*d);
                    float size=(.69f+(i%3)*.12f)*(1+p*.35f);
                    puff(m,flame,at,right,up,size,seed+age*.055,((int)(age*.7)+i)&3,(1-p)*.83f);
                }
                m.popPose();
            }
        }
        var v=buffers.getBuffer(SpellLayers.EFFECT);
        for(Impact e:IMPACTS){
            float age=world.getGameTime()-e.born+partial,p=Mth.clamp(age/20,0,1),fade=(1-p)*(1-p);
            double radius=e.radius;
            m.pushPose();m.translate(e.at.x-camera.x,e.at.y-camera.y+.06,e.at.z-camera.z);
            float red=.65f,green=.86f,blue=1;
            double ring=radius*Math.min(1,age/9);
            for(int i=0;i<64;i++){
                double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
                quad(m,v,new Vec3(Math.cos(a)*ring,.03,Math.sin(a)*ring),new Vec3(Math.cos(b)*ring,.03,Math.sin(b)*ring),new Vec3(Math.cos(b)*(ring+.15),.03,Math.sin(b)*(ring+.15)),new Vec3(Math.cos(a)*(ring+.15),.03,Math.sin(a)*(ring+.15)),red,green,blue,fade*.65f);
            }
            for(int i=0;i<18;i++){
                double a=i*2.39996+e.seed*.13,d=(.25+Math.sqrt((i+.5)/18)*radius)*Math.sqrt(p);
                Vec3 at=new Vec3(Math.cos(a)*d,.18+Math.sin(p*Math.PI)*(.35+(i%4)*.25),Math.sin(a)*d);
                mist(m,v,at,right,up,.45+p*.35,red,green+(i%2)*.14f,blue,fade*.38f);
                if(i%2==0)quad(m,v,at.subtract(right.scale(.04)).subtract(up.scale(.04)),at.add(right.scale(.04)).subtract(up.scale(.04)),at.add(right.scale(.04)).add(up.scale(.04)),at.subtract(right.scale(.04)).add(up.scale(.04)),1,1,1,fade);
            }
            m.popPose();
        }
    }
    public static void mist(PoseStack m,VertexConsumer v,Vec3 at,Vec3 right,Vec3 up,double size,float r,float g,float b,float alpha){
        for(int i=0;i<8;i++){double a=i*Math.PI/4,c=(i+1)*Math.PI/4;tri(m,v,at.add(right.scale(Math.cos(a)*size)).add(up.scale(Math.sin(a)*size*.65)),at.add(right.scale(Math.cos(c)*size)).add(up.scale(Math.sin(c)*size*.65)),at,r,g,b,0,alpha);}
    }
    private static void quad(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float r,float g,float blue,float alpha){point(m,v,a,r,g,blue,alpha);point(m,v,b,r,g,blue,alpha);point(m,v,c,r,g,blue,alpha);point(m,v,d,r,g,blue,alpha);}
    private static void tri(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,float r,float g,float blue,float alpha,float tip){point(m,v,a,r,g,blue,alpha);point(m,v,b,r,g,blue,alpha);point(m,v,c,r,g,blue,tip);point(m,v,c,r,g,blue,tip);}
    private static void point(PoseStack m,VertexConsumer v,Vec3 p,float r,float g,float b,float a){v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,a);}
    private ProjectileVisuals(){}
}
