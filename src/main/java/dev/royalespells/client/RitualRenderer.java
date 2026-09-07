package dev.royalespells.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.royalespells.entity.RitualEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Multi-stage vortex, ascending rune cages, converging filaments and a final coronation pulse. */
public final class RitualRenderer extends EntityRenderer<RitualEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public RitualRenderer(EntityRendererProvider.Context c){super(c);items=c.getItemRenderer();}
    private static void vertex(PoseStack m,VertexConsumer v,Vec3 p,float r,float g,float b,float a){v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,a);}
    private static void stroke(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,double width,float r,float g,float blue,float opacity){
        var d=b.subtract(a).normalize();var side=d.cross(new Vec3(0,1,0));if(side.lengthSqr()<.01)side=new Vec3(1,0,0);side=side.normalize().scale(width);
        vertex(m,v,a.subtract(side),r,g,blue,opacity);vertex(m,v,b.subtract(side),r,g,blue,opacity);vertex(m,v,b.add(side),r,g,blue,opacity);vertex(m,v,a.add(side),r,g,blue,opacity);
        side=d.cross(side).normalize().scale(width);vertex(m,v,a.subtract(side),r,g,blue,opacity);vertex(m,v,b.subtract(side),r,g,blue,opacity);vertex(m,v,b.add(side),r,g,blue,opacity);vertex(m,v,a.add(side),r,g,blue,opacity);
    }
    private static Vec3 radial(double a,double r,double h){return new Vec3(Math.cos(a)*r,h,Math.sin(a)*r);}
    private static void glow(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,double width,float alpha){
        stroke(m,v,a,b,width*3,.48f,.03f,.9f,alpha*.12f);stroke(m,v,a,b,width*1.6,.72f,.12f,1,alpha*.28f);stroke(m,v,a,b,width,.87f,.4f,1,alpha*.7f);stroke(m,v,a,b,width*.23,1,.88f,1,alpha);
    }
    private static void ring(PoseStack m,VertexConsumer v,double radius,double height,double rotation,double width,float alpha){for(int i=0;i<96;i++){double a=rotation+i*Math.PI/48;stroke(m,v,radial(a,radius,height),radial(a+Math.PI/48,radius,height),width,.75f,.27f,1,alpha);}}
    @Override public void render(RitualEntity e,float yaw,float partial,PoseStack m,MultiBufferSource buffers,int light){
        float t=e.age()+partial,open=Mth.clamp(t/30,0,1),fade=Mth.clamp((140-t)/18,0,1),burst=Mth.clamp((t-104)/22,0,1);
        float rise=(float)Mth.smoothstep(Mth.clamp((t-25)/70,0,1));double height=.2+rise*1.6;
        var v=buffers.getBuffer(SpellLayers.EFFECT);
        ring(m,v,(1.18+.10*Math.sin(t*.12))*open,.025,t*.045,.018,fade*.8f);
        ring(m,v,1.4*open,.045,-t*.03,.008,fade*.45f);
        ring(m,v,1.18*open,.022,0,.075,fade*.15f);
        for(int j=0;j<12;j++){
            double a=j*Math.PI/6+t*.02;var p=radial(a,1.32*open,.06);var q=radial(a+.055,1.5*open,.06);var k=radial(a-.055,1.5*open,.06);
            stroke(m,v,p,q,.013,1,.72f,1,open*fade);stroke(m,v,p,k,.013,.63f,.2f,1,open*fade);
        }
        for(int strand=0;strand<6;strand++)for(int i=0;i<40;i++){
            double f=i/40.0,g=(i+1)/40.0,a=strand*Math.PI/3+t*.075+f*Math.PI*2.4,b=strand*Math.PI/3+t*.075+g*Math.PI*2.4;
            double radius=(1.15*(1-f)+.1)*open*(1-burst*.82),radius2=(1.15*(1-g)+.1)*open*(1-burst*.82);
            glow(m,v,radial(a,radius,height*f),radial(b,radius2,height*g),.012+.012*f,open*fade*(.25f+.65f*(float)f));
        }
        if(t>40){m.pushPose();m.translate(0,height,0);m.mulPose(Axis.YP.rotation(t*.035f));m.mulPose(Axis.XP.rotation(.45f));ring(m,v,.55,0,0,.012,fade*.65f);m.mulPose(Axis.ZP.rotationDegrees(90));ring(m,v,.65,0,0,.01,fade*.45f);m.popPose();}
        for(int i=0;i<32;i++){
            double cycle=(t*.018+i*.6180339)%1,a=i*2.4-t*.06,r=(1-cycle)*1.45,h=cycle*height;
            var p=radial(a,r,h);double s=.022+.018*Math.sin(i);stroke(m,v,p.add(-s,0,0),p.add(s,0,0),s*.35,.88f,.64f,1,open*fade*(float)cycle);stroke(m,v,p.add(0,-s,0),p.add(0,s,0),s*.35,.8f,.4f,1,open*fade*(float)cycle);
        }
        if(t>=104){
            float pulse=Mth.clamp(1-(t-104)/36,0,1);
            ring(m,v,.1+burst*2.8,.10,0,.018,pulse*.9f);ring(m,v,.1+burst*2.3,.13,0,.045,pulse*.35f);
            for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.01;stroke(m,v,new Vec3(0,height,0),radial(a,.1+burst*1.25,height+.15*Math.sin(a*3)),.012,1,.83f,1,pulse*.9f);}
            glow(m,v,new Vec3(0,.05,0),new Vec3(0,height+3.8*(1-burst),0),.065,pulse*.8f);
        }
        if(t>25)for(int i=0;i<6;i++){
            double a=i*Math.PI/3-t*.025,r=.7+.12*Math.sin(t*.09+i),h=height+.2*Math.sin(a);var p=radial(a,r,h);
            Vec3 top=p.add(0,.24,0),left=p.add(-.075,0,0),bottom=p.add(0,-.24,0),right=p.add(.075,0,0);
            vertex(m,v,top,.95f,.8f,1,fade*.8f);vertex(m,v,left,.5f,.08f,.85f,fade*.7f);vertex(m,v,bottom,.68f,.22f,1,fade*.7f);vertex(m,v,right,1,.72f,1,fade*.8f);
        }
        m.pushPose();m.translate(0,height,0);m.mulPose(Axis.YP.rotation(t*.04f));m.mulPose(Axis.ZP.rotation((float)Math.sin(t*.06)*.12f));m.scale(1.15f,1.15f,1.15f);
        items.renderStatic(e.display(),ItemDisplayContext.GROUND,15728880,OverlayTexture.NO_OVERLAY,m,buffers,e.level(),e.getId());m.popPose();
        super.render(e,yaw,partial,m,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(RitualEntity e){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
}
