package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/** A restrained bucket flare applied in head-local coordinates, before the head pose. */
final class HelmetTaper implements VertexConsumer {
    private static final float TOP=-8.9f/16, BOTTOM=.5f/16;
    private final VertexConsumer target;
    private final PoseStack.Pose pose;
    private float x,y,z,scale;

    HelmetTaper(VertexConsumer target,PoseStack.Pose pose){this.target=target;this.pose=pose;}
    @Override public VertexConsumer addVertex(float x,float y,float z){
        this.x=x;this.y=y;this.z=z;
        scale=.98f+.06f*Mth.clamp((y-TOP)/(BOTTOM-TOP),0,1);
        target.addVertex(pose,x*scale,y,z*scale);return this;
    }
    @Override public VertexConsumer setNormal(float nx,float ny,float nz){
        // Inverse-transpose of the taper's Jacobian keeps lighting on the sloped walls.
        float slope=y>=TOP&&y<=BOTTOM?.06f/(BOTTOM-TOP):0;
        float tx=nx/scale,ty=ny-slope*(x*nx+z*nz)/scale,tz=nz/scale;
        float length=Mth.sqrt(tx*tx+ty*ty+tz*tz);
        target.setNormal(pose,tx/length,ty/length,tz/length);return this;
    }
    @Override public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r,g,b,a);return this;}
    @Override public VertexConsumer setUv(float u,float v){target.setUv(u,v);return this;}
    @Override public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
    @Override public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
}
