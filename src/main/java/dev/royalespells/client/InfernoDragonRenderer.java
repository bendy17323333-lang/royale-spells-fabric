package dev.royalespells.client;

import com.mojang.blaze3d.vertex.*;
import dev.royalespells.*;
import dev.royalespells.entity.InfernoDragon;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Blockbench-authored dragon and a depth-tested, non-depth-writing heat beam. */
public final class InfernoDragonRenderer extends MobRenderer<InfernoDragon,TroopModel<InfernoDragon>> {
    private static final ResourceLocation TEXTURE=RoyaleSpells.id("textures/entity/inferno_dragon.png");
    public InfernoDragonRenderer(EntityRendererProvider.Context context){
        super(context,new TroopModel<>("inferno_dragon"),.55f);
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<InfernoDragon,TroopModel<InfernoDragon>>(this){
            @Override public void render(PoseStack m,MultiBufferSource buffers,int light,InfernoDragon e,float limb,float amount,float delta,float age,float yaw,float pitch){
                if(!e.isInvisible()&&e.heatTicks()>0)getParentModel().renderEmissive(m,buffers.getBuffer(RenderType.eyes(TEXTURE)));
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(InfernoDragon dragon){return TEXTURE;}
    public static Vec3 mouth(InfernoDragon e,float partial){
        // Rest-pose point exported from head space (0,17.3,-14.4), transformed
        // through +22 degree head / -35 degree body. It lies inside the open jaws.
        double yaw=Math.toRadians(Mth.rotLerp(partial,e.yBodyRotO,e.yBodyRot));
        return e.getPosition(partial).add(-Math.sin(yaw)*1.048,.690,Math.cos(yaw)*1.048);
    }
    public static void beam(InfernoDragon e,float partial,PoseStack m,MultiBufferSource buffers){
        var target=e.beamTarget();if(target==null||e.heatTicks()==0||VisualState.frozen(e)||dev.royalespells.pause.ElectricPause.active(e)||e.isInvisible())return;
        Vec3 at=e.getPosition(partial),a=mouth(e,partial).subtract(at),b=target.getPosition(partial).add(0,target.getBbHeight()*.5,0).subtract(at);
        int tier=InfernoDragon.tier(e.heatTicks());float age=e.tickCount+partial;
        float pulse=.92f+Mth.sin(age*2.7f)*.08f,width=new float[]{.016f,.04f,.077f}[tier]*pulse;
        var v=buffers.getBuffer(SpellLayers.EFFECT);
        strip(m,v,a,b,width*4+.035f,1,.24f,.018f,.14f);
        strip(m,v,a,b,width*2.1f,1,.48f,.035f,.60f);
        strip(m,v,a,b,width,1,.94f,.50f,.98f);
        Vec3 forward=b.subtract(a).normalize();
        Vec3 u=forward.cross(Math.abs(forward.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        Vec3 w=forward.cross(u);
        // Small rotating tongues make a continuous jet read as heat, not electric arcs.
        for(int i=0;i<6;i++){
            double t=((age*.042+i/6d)%1),angle=t*19-age*.30;
            Vec3 offset=u.scale(Math.cos(angle)*width*1.6).add(w.scale(Math.sin(angle)*width*1.6));
            Vec3 p=a.lerp(b,t).add(offset),q=a.lerp(b,Math.min(1,t+.07)).add(offset.scale(.4));
            strip(m,v,p,q,width*.7f,1,.50f+.12f*tier,.06f,.7f);
        }
        flare(m,v,a,u,w,.12f+width*1.6f,age,.8f);
        flare(m,v,b,u,w,.14f+width*2.7f,-age,.95f);
    }
    private static void flare(PoseStack m,VertexConsumer v,Vec3 p,Vec3 u,Vec3 w,float size,float age,float opacity){
        for(int i=0;i<4;i++){
            double angle=i*Math.PI/4+age*.16;Vec3 side=u.scale(Math.cos(angle)*size).add(w.scale(Math.sin(angle)*size));
            strip(m,v,p.subtract(side),p.add(side),size*.20f,1,.66f,.12f,opacity*.4f);
        }
        strip(m,v,p.subtract(u.scale(size*.32)),p.add(u.scale(size*.32)),size*.24f,1,1,.76f,opacity);
    }
    private static void strip(PoseStack m,VertexConsumer v,Vec3 a,Vec3 b,float width,float r,float g,float blue,float alpha){
        if(a.distanceToSqr(b)<1e-10)return;
        Vec3 direction=b.subtract(a).normalize(),cross=direction.cross(Math.abs(direction.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width),other=direction.cross(cross);
        for(Vec3 side:new Vec3[]{cross,other})for(Vec3 p:new Vec3[]{a.add(side),a.subtract(side),b.subtract(side),b.add(side)})
            v.addVertex(m.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,blue,alpha);
    }
}
