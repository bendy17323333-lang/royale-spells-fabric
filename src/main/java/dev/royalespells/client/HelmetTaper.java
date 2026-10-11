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
    @Override public VertexConsumer vertex(double x,double y,double z){
        this.x=(float)x;this.y=(float)y;this.z=(float)z;
        scale=.98f+.06f*Mth.clamp(((float)y-TOP)/(BOTTOM-TOP),0,1);
        target.vertex(pose.pose(),(float)x*scale,(float)y,(float)z*scale);return this;
    }
    @Override public VertexConsumer normal(float nx,float ny,float nz){
        // Inverse-transpose of the taper's Jacobian keeps lighting on the sloped walls.
        float slope=y>=TOP&&y<=BOTTOM?.06f/(BOTTOM-TOP):0;
        float tx=nx/scale,ty=ny-slope*(x*nx+z*nz)/scale,tz=nz/scale;
        float length=Mth.sqrt(tx*tx+ty*ty+tz*tz);
        target.normal(pose.normal(),tx/length,ty/length,tz/length);return this;
    }
    @Override public VertexConsumer color(int r,int g,int b,int a){target.color(r,g,b,a);return this;}
    @Override public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
    @Override public VertexConsumer overlayCoords(int u,int v){target.overlayCoords(u,v);return this;}
    @Override public VertexConsumer uv2(int u,int v){target.uv2(u,v);return this;}
    @Override public void endVertex(){target.endVertex();}
    @Override public void defaultColor(int r,int g,int b,int a){target.defaultColor(r,g,b,a);}
    @Override public void unsetDefaultColor(){target.unsetDefaultColor();}
}
