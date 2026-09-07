package dev.royalespells.client;
import com.mojang.blaze3d.vertex.*;
import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Animated ground haze, bubbling poison and readable pulses instead of unrelated potion sprites. */
final class AmbientSpellRenderer {
    static boolean render(SpellEntity e,float delta,PoseStack m,MultiBufferSource buffers) {
        Spell spell=e.spell();
        if(spell!=Spell.POISON && spell!=Spell.GOBLIN_CURSE && spell!=Spell.HEAL && spell!=Spell.WARMTH && spell!=Spell.CLONE && spell!=Spell.MIRROR && spell!=Spell.EARTHQUAKE)return false;
        float time=e.time()+delta,alpha=FieldAnimation.opacity(spell,time,e.duration());
        double radius=spell.radius*FieldAnimation.opening(spell,time);
        var v=buffers.getBuffer(SpellLayers.EFFECT);
        if(spell==Spell.EARTHQUAKE) {
            float age=time%20;if(age<12) {
                double r=spell.radius*Math.min(1,age/9);
                ring(m,v,r-.12,r+.14,.08,.79f,.56f,.25f,(1-age/12)*.62f,0);
                ring(m,v,r*.7-.05,r*.7+.05,.1,.91f,.76f,.44f,(1-age/12)*.4f,0);
            }return true;
        }
        boolean poison=spell==Spell.POISON,curse=spell==Spell.GOBLIN_CURSE;
        float red=poison?1:curse?.36f:spell==Spell.CLONE||spell==Spell.MIRROR?.18f:1;
        float green=poison?.53f:curse?.76f:spell==Spell.CLONE||spell==Spell.MIRROR?.88f:spell==Spell.HEAL?.84f:.57f;
        float blue=poison?.045f:curse?.035f:spell==Spell.CLONE||spell==Spell.MIRROR?1:.2f;
        ring(m,v,0,radius*.94,.085,red*.65f,green*.67f,blue,.11f*alpha,.2f*alpha);
        ring(m,v,radius*.94,radius,.09,red,green,blue,.2f*alpha,.34f*alpha);
        ring(m,v,radius,radius+.10,.09,red,green,blue,.34f*alpha,0);
        float pulse=(time%20)/20;
        ring(m,v,radius*pulse-.055,radius*pulse+.065,.11,red,green,blue,(1-pulse)*.24f*alpha,0);
        var rotation=Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        var right=new Vector3f(1,0,0).rotate(rotation);var up=new Vector3f(0,1,0).rotate(rotation);
        Vec3 r=new Vec3(right),u=new Vec3(up);
        int count=poison?28:curse?18:10;
        for(int i=0;i<count;i++) {
            double seed=i*2.399963+e.getId()*.017;
            float life=Mth.frac(time/(poison?48:32)+i*.618034f);
            double distance=Math.sqrt((i+.5)/count)*radius*.88,angle=seed+time*.006;
            Vec3 at=new Vec3(Math.cos(angle)*distance,.18+life*(poison?1.5:1.15),Math.sin(angle)*distance);
            float fade=(float)Math.sin(life*Math.PI)*alpha;
            if(poison||curse) {
                disk(m,v,at,r,u,(.6+life*.6)*(poison?1:.65),red,green*(.86f+(i%3)*.13f),blue,fade*.3f);
                if(i%3==0)bubble(m,v,at,r,u,.07+life*.085,red,Math.min(1,green+.21f),blue+.13f,fade*.68f);
            } else {
                // Small rising crosses for healing/warmth, icy glints for cloning/mirroring.
                Vec3 horizontal=r.scale(.07),vertical=u.scale(.07);
                quad(m,v,at.subtract(horizontal).subtract(u.scale(.018)),at.add(horizontal).subtract(u.scale(.018)),at.add(horizontal).add(u.scale(.018)),at.subtract(horizontal).add(u.scale(.018)),red,green,blue,fade*.7f);
                quad(m,v,at.subtract(vertical).subtract(r.scale(.018)),at.add(vertical).subtract(r.scale(.018)),at.add(vertical).add(r.scale(.018)),at.subtract(vertical).add(r.scale(.018)),red,green,blue,fade*.7f);
            }
        }
        return true;
    }
    private static void bubble(PoseStack m,VertexConsumer v,Vec3 at,Vec3 r,Vec3 u,double size,float red,float green,float blue,float alpha) {
        for(int i=0;i<20;i++) {
            double a=i*Math.PI/10,b=(i+1)*Math.PI/10;
            Vec3 one=r.scale(Math.cos(a)).add(u.scale(Math.sin(a))),two=r.scale(Math.cos(b)).add(u.scale(Math.sin(b)));
            quad(m,v,at.add(one.scale(size*.82)),at.add(two.scale(size*.82)),at.add(two.scale(size)),at.add(one.scale(size)),red,green,blue,alpha);
        }
    }
    private static void disk(PoseStack m,VertexConsumer v,Vec3 at,Vec3 r,Vec3 u,double size,float red,float green,float blue,float alpha) {
        for(int i=0;i<16;i++) {
            double a=i*Math.PI/8,b=(i+1)*Math.PI/8;
            Vec3 one=at.add(r.scale(Math.cos(a)*size)).add(u.scale(Math.sin(a)*size*.62));
            Vec3 two=at.add(r.scale(Math.cos(b)*size)).add(u.scale(Math.sin(b)*size*.62));
            point(m,v,at,red,green,blue,alpha);point(m,v,one,red,green,blue,0);point(m,v,two,red,green,blue,0);point(m,v,at,red,green,blue,alpha);
        }
    }
    private static void ring(PoseStack m,VertexConsumer v,double inner,double outer,double y,float r,float g,float b,float in,float out){ring(m,v,inner,outer,y,r,g,b,in,out,0,0);}
    private static void ring(PoseStack m,VertexConsumer v,double inner,double outer,double y,float r,float g,float b,float in,float out,double x,double z) {
        inner=Math.max(0,inner);outer=Math.max(0,outer);
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,c=(i+1)*Math.PI/32;
            point(m,v,new Vec3(x+Math.cos(a)*inner,y,z+Math.sin(a)*inner),r,g,b,in);
            point(m,v,new Vec3(x+Math.cos(c)*inner,y,z+Math.sin(c)*inner),r,g,b,in);
            point(m,v,new Vec3(x+Math.cos(c)*outer,y,z+Math.sin(c)*outer),r,g,b,out);
            point(m,v,new Vec3(x+Math.cos(a)*outer,y,z+Math.sin(a)*outer),r,g,b,out);
        }
    }
    private static void quad(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float r,float g,float blue,float alpha){point(m,v,a,r,g,blue,alpha);point(m,v,b,r,g,blue,alpha);point(m,v,c,r,g,blue,alpha);point(m,v,d,r,g,blue,alpha);}
    private static void point(PoseStack m,VertexConsumer v,Vec3 p,float r,float g,float b,float a){v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,a);}
    private AmbientSpellRenderer(){}
}
