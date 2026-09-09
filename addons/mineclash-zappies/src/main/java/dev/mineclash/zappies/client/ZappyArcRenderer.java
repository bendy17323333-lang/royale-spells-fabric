package dev.mineclash.zappies.client;

import com.mojang.blaze3d.vertex.*;
import dev.mineclash.zappies.ZappyAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.liziyowo.mineclash.entity.mob.Zappies;
import java.util.*;

/** A connected, short-lived discharge, drawn from the original model's top coil.
 * This layer uses no texture assets and never writes depth over a transparent pixel. */
public abstract class ZappyArcRenderer extends RenderType {
    private ZappyArcRenderer(){super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
    private static final RenderType ARC=create("zappies_discharge",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,true,
        CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final Map<Zappies,Integer> SEEN=new WeakHashMap<>();
    public static int visibleArcs;
    public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var c=Minecraft.getInstance();if(c.level==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera=event.getCamera().getPosition();PoseStack pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=c.renderBuffers().bufferSource();VertexConsumer out=null;visibleArcs=0;
        for(var entity:c.level.entitiesForRendering()){
            if(!(entity instanceof Zappies z)||!z.isAlive())continue;
            var shot=((ZappyAccess)z).zappyArc();if(shot.isEmpty())continue;
            double age=c.level.getGameTime()+partial-shot.getLong("at");if(age<0||age>=5)continue;
            if(out==null)out=buffers.getBuffer(ARC);visibleArcs++;
            Vec3 from=z.getPosition(partial).add(0,1.16,0),end=new Vec3(shot.getDouble("x"),shot.getDouble("y"),shot.getDouble("z"));
            if(from.distanceToSqr(camera)>4096)continue;
            Vec3 axis=end.subtract(from),side=axis.cross(new Vec3(0,1,0)).normalize(),up=side.cross(axis).normalize();
            if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
            var random=new Random(((long)z.getId()<<32)^shot.getInt("seq")*9137L^(int)(age*2));
            float fade=(float)Math.pow(1-age/5,1.4);Vec3 prev=from;
            for(int i=1;i<=12;i++){
                double f=i/12.0,amount=Math.sin(Math.PI*f)*.17;
                Vec3 next=from.add(axis.scale(f)).add(side.scale((random.nextDouble()*2-1)*amount)).add(up.scale((random.nextDouble()*2-1)*amount));
                ribbon(pose,out,prev,next,camera,.075,.2f,.63f,1,.38f*fade);
                ribbon(pose,out,prev,next,camera,.027,.65f,.9f,1,.88f*fade);
                ribbon(pose,out,prev,next,camera,.009,1,1,1,fade);
                if(i==5||i==9){Vec3 branch=next.add(axis.scale(.12)).add(side.scale((i==5?1:-1)*.3));ribbon(pose,out,next,branch,camera,.012,.6f,.85f,1,.65f*fade);}
                prev=next;
            }
            if(AnimationProbe.ENABLED&&SEEN.getOrDefault(z,-1)!=shot.getInt("seq")){
                SEEN.put(z,shot.getInt("seq"));System.out.println("ZAPPIES_QA_ARC entity="+z.getId()+" seq="+shot.getInt("seq")+" length="+axis.length());
            }
        }
        if(out!=null)buffers.endBatch(ARC);
    }
    private static void ribbon(PoseStack p,VertexConsumer out,Vec3 a,Vec3 b,Vec3 camera,double width,float r,float g,float blue,float alpha){
        Vec3 side=b.subtract(a).cross(camera.subtract(a.add(b).scale(.5))).normalize().scale(width);
        for(Vec3 v:List.of(a.add(side),b.add(side),b.subtract(side),a.subtract(side)))out.addVertex(p.last().pose(),(float)v.x,(float)v.y,(float)v.z).setColor(r,g,blue,alpha);
    }
}
