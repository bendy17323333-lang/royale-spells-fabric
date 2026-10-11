// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.entity.RoyalWitch;
import dev.royalespells.entity.WitchBolt;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/** The projectile is a continuous three-dimensional density field. Its proxy
 * box is only a rasterization boundary; no box surface, billboard or opaque
 * blob is visible. Mechanics and collision remain entirely server-owned. */
public final class WitchMagicRenderer extends EntityRenderer<WitchBolt> {
    private static final WeakHashMap<WitchBolt,Trail> TRAILS=new WeakHashMap<>();
    private static final WeakHashMap<WitchBolt,VisualSample> VISUAL_SAMPLES=new WeakHashMap<>();
    private static final WeakHashMap<RoyalWitch,Matrix4f> CHARGE_ANCHORS=new WeakHashMap<>();
    private static Object previousLevel;
    private static long frameId;
    private record TrailPoint(Vec3 position,float time) {}
    private static final class Trail {
        final ArrayDeque<TrailPoint> positions=new ArrayDeque<>();
        float time=Float.NEGATIVE_INFINITY,impactStart=Float.NaN;
        VisualSample lastDrawn;
        Vec3 drawnVelocity=Vec3.ZERO,completionFrom;
        float completionStart,completionDuration,completionSlope;
        double receivedImpactGap=-1;
        float advance(float next){return time=Math.max(time,next);}
    }
    record Volume(Matrix4f pose,Vec3 min,Vec3 max,Vec3[] trail,float mode,float time,
                  float phase,float strength,float radius,boolean evolved,double distance,WitchBolt source,Vec3 worldAnchor) {}
    /** Actual GPU submission sample for frame-continuity QA. No secondary
     * getPosition calculation is allowed to masquerade as the rendered state. */
    public record VisualSample(Vec3 position,float time,float impactTime,boolean impacted,long frameId,
                               boolean serverImpacted,double receivedImpactGap) {}
    public static VisualSample visualSample(WitchBolt entity){return VISUAL_SAMPLES.get(entity);}
    static void submitted(Volume v){
        if(v.source()==null)return;
        Trail history=TRAILS.get(v.source());
        boolean burst=v.mode()==1||v.mode()==3;
        VisualSample sample=new VisualSample(v.worldAnchor(),v.time(),burst?v.phase()*WitchBolt.IMPACT_TICKS:-1,
                burst,frameId,v.source().impacted(),history.receivedImpactGap);
        VisualSample old=history.lastDrawn;
        if(old!=null&&!old.impacted()&&!sample.impacted()){
            float elapsed=sample.time()-old.time();
            if(elapsed>.001f&&elapsed<2)history.drawnVelocity=sample.position().subtract(old.position()).scale(1/elapsed);
        }
        history.lastDrawn=sample;
        VISUAL_SAMPLES.put(v.source(),sample);
    }

    public WitchMagicRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(WitchBolt e){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
    @Override public boolean shouldRender(WitchBolt e,Frustum f,double x,double y,double z){return e.shouldRender(x,y,z)&&f.isVisible(e.getBoundingBoxForCulling().inflate(1));}

    public static void install(){
        WitchVolume.install();
        WorldRenderEvents.START.register(event->{CHARGE_ANCHORS.clear();VISUAL_SAMPLES.clear();frameId++;});
        // LAST follows the Fabulous transparency composite as well as ordinary
        // entity rendering. Depth is copied once, never read from the write FBO.
        WorldRenderEvents.LAST.register(WitchMagicRenderer::renderWorld);
    }
    /** Captured from the real animated left-palm joint during this frame's entity
     * render. START clears it so a culled or hidden witch leaves no stale FX. */
    public static void chargeAnchor(RoyalWitch e,Matrix4f pose){
        if(e.attackAge()>0&&e.attackAge()<=14)CHARGE_ANCHORS.put(e,new Matrix4f(pose));
    }

    private static void renderWorld(WorldRenderContext event){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        if(previousLevel!=mc.level){TRAILS.clear();VISUAL_SAMPLES.clear();previousLevel=mc.level;}
        Vec3 camera=event.camera().getPosition();float partial=event.tickDelta();
        List<Volume> volumes=new ArrayList<>();PoseStack pose=event.matrixStack();
        if(Boolean.getBoolean("royalespells.witchSmoke"))for(var entry:CHARGE_ANCHORS.entrySet())if(entry.getKey().attackAge()==14){
            var e=entry.getKey();var actual=entry.getValue().transformPosition(new org.joml.Vector3f());
            var world=e.muzzle().subtract(camera);var expected=pose.last().pose().transformPosition(new org.joml.Vector3f((float)world.x,(float)world.y,(float)world.z));
            System.out.println("WITCH192_PALM entity="+e.getId()+" actual="+actual+" expected="+expected+" gap="+actual.distance(expected));
        }
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof WitchBolt)&&!(entity instanceof RoyalWitch))continue;
            double distance=entity.distanceToSqr(camera);if(distance>72*72)continue;
            if(event.frustum()!=null&&!event.frustum().isVisible(entity.getBoundingBoxForCulling().inflate(3)))continue;
            Trail history=entity instanceof WitchBolt b?TRAILS.computeIfAbsent(b,k->new Trail()):null;
            float time=history!=null?history.advance(entity.tickCount+partial):entity.tickCount+partial;
            Vec3 at=entity instanceof WitchBolt b?visualPosition(b,history,time,partial):entity.getPosition(partial);
            pose.pushPose();pose.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
            if(entity instanceof WitchBolt bolt)bolt(volumes,bolt,history,time,pose,distance,at);
            else witch(volumes,(RoyalWitch)entity,partial,pose,distance);
            pose.popPose();
        }
        // Separate volumes use standard transparency; their internal fog uses
        // front-to-back optical integration. Far volumes must composite first.
        volumes.sort(Comparator.comparingDouble(Volume::distance));
        // Budget selection is nearest-first; only compositing reverses order.
        // A distant crowd must never evict the projectile beside the player.
        if(volumes.size()>40)volumes.subList(40,volumes.size()).clear();
        java.util.Collections.reverse(volumes);
        WitchVolume.render(event,volumes);
    }

    private static Vec3 visualPosition(WitchBolt e,Trail history,float time,float partial){
        if(!e.impacted())return e.getPosition(partial);
        Vec3 hit=e.impactPosition();
        if(Float.isNaN(history.impactStart)){
            // Metadata may arrive between render frames, one tick late, or in
            // a packet batch. Calibrate once, then use the continuous local
            // clock: integer network age + local partial saws backward when
            // their tick boundaries do not agree.
            history.impactStart=time-Math.max(0,e.impactAge()-1);
            VisualSample last=history.lastDrawn;
            if(last!=null&&!last.impacted()){
                history.receivedImpactGap=hit.distanceTo(last.position());
                // Network interpolation intentionally trails the server. Do
                // not teleport its still-visible core to the impact metadata.
                // Finish that last short segment from the last *GPU draw*,
                // with a maximum two-tick (100 ms at 20 TPS) catch-up window.
                // Server damage, sounds, hit point and entity expiry stay put.
                if(history.receivedImpactGap>.025&&time-last.time()<=2){
                    history.completionFrom=last.position();
                    history.completionStart=last.time();
                    history.completionDuration=(float)Mth.clamp(history.receivedImpactGap/(WitchBolt.SPEED*1.25),.12,2);
                    Vec3 along=hit.subtract(last.position()).normalize();
                    double meanSpeed=history.receivedImpactGap/history.completionDuration;
                    history.completionSlope=(float)Mth.clamp(history.drawnVelocity.dot(along)/meanSpeed,0,1);
                }
            }
        }
        if(completing(history,time)){
            float u=Mth.clamp((time-history.completionStart)/history.completionDuration,0,1),m=history.completionSlope;
            // Monotonic Hermite progress matches the incoming forward speed,
            // then gently catches up. It cannot overshoot the real collision.
            float progress=(m-1)*u*u*u+(2-2*m)*u*u+m*u;
            return history.completionFrom.lerp(hit,progress);
        }
        return hit;
    }

    private static boolean completing(Trail history,float time){
        return history.completionFrom!=null&&time<history.completionStart+history.completionDuration;
    }

    private static void bolt(List<Volume> out,WitchBolt e,Trail history,float time,PoseStack pose,double distance,Vec3 shown){
        boolean soul=e.kind()==WitchBolt.HEAL_SOUL;
        if(e.impacted()&&!completing(history,time)){
            // Keep the original impact clock and fade deadline. Catch-up is
            // presentation only; it does not prolong the server's hit effect.
            float age=Mth.clamp(time-history.impactStart,0,WitchBolt.IMPACT_TICKS),p=age/WitchBolt.IMPACT_TICKS;
            if(p>=1)return;
            float r=soul?.3f:Math.min(1.7f,e.impactRadius());
            float bound=r*1.55f+.24f;
            out.add(new Volume(new Matrix4f(pose.last().pose()),new Vec3(-bound,-bound,-bound),new Vec3(bound,bound,bound),
                    new Vec3[0],soul?3:1,time,p,1,r,e.evolved(),distance,e,shown));
            return;
        }
        Vec3[] points=sampleTrail(history,shown,time,e.flightDirection(),soul?1.2:2.0);
        // A little buoyant drift is continuous in the density shader; the spine
        // itself is real history, so changing flight direction cannot leave a
        // straight laser disconnected from the projectile.
        double edge=soul?.33:.58;
        Vec3 min=new Vec3(-edge,-edge,-edge),maxV=new Vec3(edge,edge,edge);
        for(Vec3 point:points){min=new Vec3(Math.min(min.x,point.x-edge),Math.min(min.y,point.y-edge),Math.min(min.z,point.z-edge));
            maxV=new Vec3(Math.max(maxV.x,point.x+edge),Math.max(maxV.y,point.y+edge),Math.max(maxV.z,point.z+edge));}
        out.add(new Volume(new Matrix4f(pose.last().pose()),min,maxV,points,soul?2:0,time,0,1,soul?.18f:.34f,e.evolved(),distance,e,shown));
    }

    private static Vec3[] sampleTrail(Trail history,Vec3 shown,float time,Vec3 direction,double maximum){
        // Capture real rendered positions, including sub-tick movement, rather
        // than sampling the latest network endpoint only once per server tick.
        TrailPoint first=history.positions.peekFirst();
        if(first==null||shown.distanceToSqr(first.position())>.000025){
            // Reappearing after culling/teleport must not draw a stale streak
            // across the unseen interval.
            if(first!=null&&(time-first.time()>2||shown.distanceToSqr(first.position())>16))history.positions.clear();
            history.positions.addFirst(new TrailPoint(shown,time));
        }
        while(history.positions.size()>96)history.positions.removeLast();
        while(history.positions.size()>2&&time-history.positions.peekLast().time()>12)history.positions.removeLast();
        List<Vec3> path=new ArrayList<>();path.add(Vec3.ZERO);double length=0;
        for(TrailPoint point:history.positions){
            Vec3 local=point.position().subtract(shown),last=path.get(path.size()-1);
            double segment=local.distanceTo(last);if(segment<.0001)continue;
            if(length+segment>=maximum){path.add(last.lerp(local,(maximum-length)/segment));length=maximum;break;}
            path.add(local);length+=segment;
        }
        if(path.size()<2)return new Vec3[]{Vec3.ZERO,direction.scale(-.035)};
        // A fixed seven-point arc-length resampling prevents the shader's
        // radius/age coordinate from popping whenever another tick point is
        // inserted. Dense render history retains turns without adding GPU work.
        Vec3[] sampled=new Vec3[7];sampled[0]=Vec3.ZERO;int segment=1;double passed=0;
        for(int i=1;i<7;i++){
            double desired=length*i/6;
            while(segment<path.size()-1&&passed+path.get(segment-1).distanceTo(path.get(segment))<desired){
                passed+=path.get(segment-1).distanceTo(path.get(segment));segment++;
            }
            Vec3 a=path.get(segment-1),b=path.get(segment);
            sampled[i]=a.lerp(b,Mth.clamp((desired-passed)/Math.max(.000001,a.distanceTo(b)),0,1));
        }
        return sampled;
    }

    private static void witch(List<Volume> out,RoyalWitch e,float partial,PoseStack pose,double distance){
        Matrix4f anchor=CHARGE_ANCHORS.get(e);
        if(anchor!=null&&e.attackAge()>0&&e.attackAge()<14){
            float p=Mth.clamp((e.attackAge()+partial)/14f,0,1),r=.07f+.14f*p;
            float edge=r*1.65f;
            out.add(new Volume(anchor,new Vec3(-edge,-edge,-edge),new Vec3(edge,edge,edge),
                    new Vec3[0],5,e.tickCount+partial,p,.22f+.65f*p,r,e.evolved(),distance,null,null));
        }
        if(e.summonAge()>0){
            float p=Mth.clamp((e.summonAge()-1+partial)/24,0,1);
            float fade=Mth.clamp(p*5,0,1)*Mth.clamp((1-p)*3,0,1);
            out.add(new Volume(new Matrix4f(pose.last().pose()),new Vec3(-1.1,-.1,-1.1),new Vec3(1.1,.75,1.1),
                    new Vec3[0],4,e.tickCount+partial,p,fade*.65f,.72f,e.evolved(),distance,null,null));
        }
        // The model renderer supplies the brief mint-white body tint. A small
        // arriving soul fog sits at the chest, with real body/terrain occlusion.
        if(e.healFlash()>0){
            pose.pushPose();pose.translate(0,1.13,0);
            out.add(new Volume(new Matrix4f(pose.last().pose()),new Vec3(-.5,-.5,-.5),new Vec3(.5,.5,.5),
                    new Vec3[0],3,e.tickCount+partial,1-Mth.clamp((e.healFlash()-partial)/10,0,1),.7f,.32f,true,distance,null,null));pose.popPose();
        }
    }
}
