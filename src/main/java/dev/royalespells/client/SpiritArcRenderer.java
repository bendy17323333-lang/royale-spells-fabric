package dev.royalespells.client;

import com.mojang.blaze3d.vertex.*;
import dev.royalespells.entity.SpiritArc;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A blue-white crooked link and a short victim flash, never a circular splash. */
public final class SpiritArcRenderer extends EntityRenderer<SpiritArc> {
    public SpiritArcRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public ResourceLocation getTextureLocation(SpiritArc e){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
    public static void draw(SpiritArc e,float partial,PoseStack m,MultiBufferSource buffers){
        float age=e.age()+partial,fade=Mth.clamp((SpiritArc.DURATION-age)/SpiritArc.DURATION,0,1);
        if(fade<=0)return;
        var v=buffers.getBuffer(SpellLayers.EFFECT);var end=e.end();
        int frame=(int)(age*1.8),segments=Math.max(5,(int)(end.length()*5));
        double reach=Math.min(1,(age+.5)/1.3);Vec3 a=Vec3.ZERO;
        for(int i=1;i<=segments;i++){
            double u=Math.min(reach,i/(double)segments);
            var b=end.scale(u);
            if(i<segments&&u<reach){long seed=e.getId()*193L+frame*71L+i*13L;b=b.add((noise(seed)-.5)*.23,(noise(seed+41)-.5)*.22,(noise(seed+73)-.5)*.23);}
            stroke(m,v,a,b,fade);a=b;if(u>=reach)break;
        }
        if(age<4.5f){
            for(int i=0;i<5;i++){
                double angle=i*Math.PI*2/5+frame*.3;
                Vec3 p=end.add(Math.cos(angle)*.13,Math.sin(angle)*.13,0);
                stroke(m,v,p,end.add(Math.cos(angle)*(.22+age*.017),Math.sin(angle)*(.22+age*.017),.04),fade*.7f);
            }
        }
    }
    private static double noise(long n){n=(n^0x5deece66dL)*0x27d4eb2dL;n^=n>>>15;return(n&0xffffff)/(double)0xffffff;}
    private static void stroke(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,float fade){
        if(a.distanceToSqr(b)<1e-8)return;
        beam(m,v,a,b,.075f,.28f,.33f,1,fade*.2f);
        beam(m,v,a,b,.035f,.43f,.72f,1,fade*.85f);
        beam(m,v,a,b,.016f,.88f,.97f,1,fade);
    }
    private static void beam(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,float width,float r,float g,float blue,float alpha){
        Vec3 direction=b.subtract(a).normalize(),cross=direction.cross(Math.abs(direction.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width),other=direction.cross(cross);
        for(Vec3 side:new Vec3[]{cross,other})for(Vec3 p:new Vec3[]{a.add(side),a.subtract(side),b.subtract(side),b.add(side)})
            v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,blue,alpha);
    }
}
