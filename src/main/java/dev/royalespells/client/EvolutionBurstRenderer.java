package dev.royalespells.client;

import com.mojang.blaze3d.vertex.*;
import dev.royalespells.entity.EvolutionBurst;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Brief violet arrival light, a spreading ground flash, then fading crystal flecks. */
public final class EvolutionBurstRenderer extends EntityRenderer<EvolutionBurst> {
    public EvolutionBurstRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public ResourceLocation getTextureLocation(EvolutionBurst e){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
    private static Vec3 radial(double angle,double radius,double y){return new Vec3(Math.cos(angle)*radius,y,Math.sin(angle)*radius);}
    private static void vertex(PoseStack m,VertexConsumer v,Vec3 p,float r,float g,float b,float a){v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,a);}
    private static void band(PoseStack m,VertexConsumer v,double inner,double outer,double y,float opacity){
        for(int i=0;i<80;i++){
            double a=i*Math.PI/40,b=(i+1)*Math.PI/40;
            vertex(m,v,radial(a,inner,y),.78f,.31f,1,0);vertex(m,v,radial(b,inner,y),.78f,.31f,1,0);
            vertex(m,v,radial(b,outer,y),.91f,.63f,1,opacity);vertex(m,v,radial(a,outer,y),.91f,.63f,1,opacity);
            vertex(m,v,radial(a,outer,y),.91f,.63f,1,opacity);vertex(m,v,radial(b,outer,y),.91f,.63f,1,opacity);
            vertex(m,v,radial(b,outer+.18,y),.50f,.06f,.95f,0);vertex(m,v,radial(a,outer+.18,y),.50f,.06f,.95f,0);
        }
    }
    private static void column(PoseStack m,VertexConsumer v,double radius,double bottom,double height,float opacity){
        for(int i=0;i<32;i++){
            double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
            vertex(m,v,radial(a,radius,bottom),.81f,.33f,1,opacity);vertex(m,v,radial(b,radius,bottom),.81f,.33f,1,opacity);
            vertex(m,v,radial(b,radius*.35,bottom+height),.58f,.10f,1,0);vertex(m,v,radial(a,radius*.35,bottom+height),.58f,.10f,1,0);
        }
    }
    private static void crystal(PoseStack m,VertexConsumer v,Vec3 at,double width,double height,double turn,float opacity){
        Vec3 side=radial(turn,width,0);
        vertex(m,v,at.add(0,height,0),1,.88f,1,opacity);vertex(m,v,at.add(side),.82f,.40f,1,opacity);
        vertex(m,v,at.add(0,-height,0),.45f,.05f,.86f,0);vertex(m,v,at.subtract(side),.64f,.16f,1,opacity*.75f);
    }
    public static void draw(EvolutionBurst e,float partial,PoseStack m,MultiBufferSource buffers){
        float t=e.age()+partial;if(t>=EvolutionBurst.DURATION)return;
        var v=buffers.getBuffer(SpellLayers.EFFECT);
        float flash=Mth.clamp(t/2,0,1)*Mth.clamp(1-(t-3)/9,0,1);
        float spread=(float)Mth.smoothstep(Mth.clamp(t/12,0,1)),fade=Mth.clamp((24-t)/13,0,1);
        double radius=e.radius(),down=Math.max(0,1-t/4)*1.6;
        column(m,v,radius*.50,.04+down,2.3-Math.min(1,t/12)*1.5,flash*.28f);
        column(m,v,radius*.19,.06+down,1.8,flash*.23f);
        band(m,v,0,radius*.65,.035,flash*.20f);
        band(m,v,Math.max(0,radius*spread-.36),radius*spread,.055,flash*.7f);
        for(int i=0;i<20;i++){
            double angle=i*2.399963,delay=(i%4)*.7,life=Mth.clamp((t-delay)/22,0,1);
            double travel=radius*(.12+.88*Math.sqrt(life)),height=.08+Math.sin(life*Math.PI)*(.45+(i%3)*.22);
            float opacity=fade*(float)(Mth.clamp((t-delay)/2,0,1)*(1-life));
            crystal(m,v,radial(angle,travel,height),.035+(i%3)*.015,.12+(i%4)*.045,angle+t*.025,opacity);
        }
    }
}
