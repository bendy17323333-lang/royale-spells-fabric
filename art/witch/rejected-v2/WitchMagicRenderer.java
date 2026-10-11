// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.royalespells.entity.RoyalWitch;
import dev.royalespells.entity.WitchBolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Witch magic is sculpted volume, not a luminous sprite. The core is a closed,
 * slowly deforming opaque mesh; liquid limbs use capped variable-radius tubes.
 * Face normals drive broad directional lighting and plum shadows. The wake
 * follows the projectile's actual historical path through world space.
 *
 * Opaque surfaces write real depth. Fading surfaces are color-only, back-face
 * culled, sorted far-to-near. No additive white shell, crossed billboard, scene
 * overlay or complete circular impact pattern. Gameplay is entirely unchanged.
 */
public final class WitchMagicRenderer extends EntityRenderer<WitchBolt> {
    private static final Vec3 UP=new Vec3(0,1,0),RIGHT=new Vec3(1,0,0);
    private static final Vec3 LIGHT=new Vec3(-.42,.83,-.36).normalize();
    private static final Color PLUM=new Color(.24f,.034f,.36f),VIOLET=new Color(.49f,.095f,.69f),MAGENTA=new Color(.69f,.18f,.77f);
    private static final Color LILAC=new Color(.72f,.42f,.82f),PINPOINT=new Color(.92f,.77f,.93f),MINT=new Color(.59f,.85f,.60f);
    private static final Mesh SOLID=new Mesh(),FLUID=new Mesh();
    private static final WeakHashMap<WitchBolt,Trail> TRAILS=new WeakHashMap<>();
    private static long frameTick=Long.MIN_VALUE;
    private static int framePartial,frameEffects,frameQuads;
    private static final int SOFT_FRAME_BUDGET=10000,HARD_MESH_BUDGET=2304;
    private record Color(float r,float g,float b){
        Color mix(Color c,double t){float f=(float)Mth.clamp(t,0,1);return new Color(r+(c.r-r)*f,g+(c.g-g)*f,b+(c.b-b)*f);}
    }
    private record Frame(Vec3 forward,Vec3 side,Vec3 up){
        Vec3 point(double x,double y,double z){return side.scale(x).add(up.scale(y)).add(forward.scale(z));}
        Vec3 radial(double a,double r){return point(Math.cos(a)*r,Math.sin(a)*r,0);}
    }
    private static final class Trail {final ArrayDeque<Vec3> positions=new ArrayDeque<>();int tick=Integer.MIN_VALUE;}

    /** Reusable meshes avoid retaining two consumers backed by one BufferBuilder.
     * Faces are atomic; a budget never truncates a vertex. */
    private static final class Mesh {
        private float[] data=new float[7*4*768];
        private int[] order=new int[768];private double[] depths=new double[768];
        private int size;
        void clear(){size=0;}
        int faces(){return size/28;}
        void face(PoseStack pose,Vec3 a,Vec3 b,Vec3 c,Vec3 d,Vec3 outward,Color color,float alpha,float light){
            if(alpha<.012f||faces()>=HARD_MESH_BUDGET)return;
            Vec3 normal=b.subtract(a).cross(c.subtract(a));
            if(normal.lengthSqr()<1e-10)normal=c.subtract(a).cross(d.subtract(a));
            if(normal.lengthSqr()<1e-10)return;
            if(normal.dot(outward)<0){Vec3 swap=b;b=d;d=swap;normal=normal.scale(-1);}
            normal=normal.normalize();
            // Coherent broad light, not random face colors or accumulated emission.
            float shade=(float)(.47+.48*Math.max(0,normal.dot(LIGHT))+.08*Math.max(0,-normal.y));
            shade=shade*(1-light)+light;
            if(size+28>data.length)data=Arrays.copyOf(data,data.length*2);
            vertex(pose,a,color,alpha,shade);vertex(pose,b,color,alpha,shade);vertex(pose,c,color,alpha,shade);vertex(pose,d,color,alpha,shade);
        }
        private void vertex(PoseStack pose,Vec3 p,Color c,float alpha,float shade){
            var m=pose.last().pose();float x=(float)p.x,y=(float)p.y,z=(float)p.z;
            data[size++]=m.m00()*x+m.m10()*y+m.m20()*z+m.m30();
            data[size++]=m.m01()*x+m.m11()*y+m.m21()*z+m.m31();
            data[size++]=m.m02()*x+m.m12()*y+m.m22()*z+m.m32();
            data[size++]=c.r*shade;data[size++]=c.g*shade;data[size++]=c.b*shade;data[size++]=alpha;
        }
        void flush(VertexConsumer out,boolean sort){
            int count=faces();if(count==0)return;
            if(order.length<count){order=new int[count*2];depths=new double[count*2];}
            for(int f=0;f<count;f++){
                order[f]=f;
                if(sort){int p=f*28;double x=0,y=0,z=0;for(int v=0;v<4;v++){x+=data[p+v*7];y+=data[p+v*7+1];z+=data[p+v*7+2];}depths[f]=x*x+y*y+z*z;}
            }
            if(sort)sort(0,count-1);
            for(int f=0;f<count;f++)for(int v=0;v<4;v++){
                int p=order[f]*28+v*7;out.vertex(data[p],data[p+1],data[p+2]).color(data[p+3],data[p+4],data[p+5],data[p+6]).endVertex();
            }
            frameQuads+=count;
        }
        private void sort(int lo,int hi){
            int i=lo,j=hi;double pivot=depths[order[(lo+hi)>>>1]];
            while(i<=j){while(depths[order[i]]>pivot)i++;while(depths[order[j]]<pivot)j--;if(i<=j){int t=order[i];order[i++]=order[j];order[j--]=t;}}
            if(lo<j)sort(lo,j);if(i<hi)sort(i,hi);
        }
    }

    public WitchMagicRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(WitchBolt e){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
    @Override public boolean shouldRender(WitchBolt e,Frustum f,double x,double y,double z){return e.shouldRender(x,y,z)&&f.isVisible(e.getBoundingBoxForCulling().inflate(.7));}

    /** Caller translates to interpolated flight position, or the separately
     * synchronized exact impact point after collision. */
    public static void draw(WitchBolt e,float partial,PoseStack pose,MultiBufferSource buffers){
        int quality=quality(e.position(),partial);if(quality<0)return;
        SOLID.clear();FLUID.clear();Trail trail=history(e);
        float time=e.tickCount+partial;Frame frame=frame(e.flightDirection());
        if(e.impacted()){
            float age=Math.max(0,e.impactAge()-1+partial);
            if(e.kind()==WitchBolt.HEAL_SOUL)arrival(pose,age,quality);
            else rupture(pose,frame(e.impactNormal()),age,e.impactRadius(),quality);
        }else if(e.kind()==WitchBolt.HEAL_SOUL){
            wake(e,partial,pose,trail,frame,time,quality,true);
            body(pose,SOLID,Vec3.ZERO,frame,.081,.09,.19,time*.65,VIOLET,LILAC,1,quality,.08f);
            if(quality>0)streamer(pose,SOLID,frame,.095,.042,.72,.19,time*.13,-1,MAGENTA,1,quality);
        }else{
            wake(e,partial,pose,trail,frame,time,quality,false);
            // Low-frequency waves shift mass between lobes instead of jittering.
            body(pose,SOLID,Vec3.ZERO,frame,.218,.195,.285,time*.17,PLUM,VIOLET,1,quality,0);
            if(quality>0){
                streamer(pose,SOLID,frame,.21,.052,.15,.96,time*.105,1,VIOLET,1,quality);
                streamer(pose,SOLID,frame,.238,.036,2.28,.61,-time*.083,-1,MAGENTA,1,quality);
                if(quality>1)streamer(pose,SOLID,frame,.178,.045,4.56,.82,time*.068,1,PLUM.mix(VIOLET,.6),1,quality);
            }
            // A tiny pale surface glint, never a luminous inner sphere.
            body(pose,SOLID,frame.point(-.053,.068,.255),frame,.015,.020,.018,0,LILAC,PINPOINT,1,0,.6f);
        }
        flush(buffers);
    }
    private static Trail history(WitchBolt e){
        Trail trail=TRAILS.computeIfAbsent(e,ignored->new Trail());
        if(trail.tick!=e.tickCount){
            trail.tick=e.tickCount;
            if(!e.impacted()&&(trail.positions.isEmpty()||trail.positions.peekFirst().distanceToSqr(e.position())>.0001)){
                trail.positions.addFirst(e.position());while(trail.positions.size()>9)trail.positions.removeLast();
            }
        }
        return trail;
    }

    /** Catmull-Rom historical spine and slowly rolling transverse mass. */
    private static void wake(WitchBolt e,float partial,PoseStack pose,Trail trail,Frame frame,float time,int quality,boolean soul){
        List<Vec3> history=new ArrayList<>();history.add(Vec3.ZERO);
        Vec3 shown=e.getPosition(partial);double distance=0,max=soul?1.35:1.85;
        for(Vec3 p:trail.positions){
            Vec3 local=p.subtract(shown),previous=history.get(history.size()-1);
            // Skip a network point ahead of the interpolated head; no folded tail.
            if(local.dot(frame.forward)>.015||local.distanceToSqr(previous)<.0025)continue;
            double segment=local.distanceTo(previous);
            if(distance+segment>max){history.add(previous.lerp(local,(max-distance)/segment));break;}
            history.add(local);distance+=segment;
        }
        if(history.size()<2)history.add(frame.forward.scale(-.28));
        int points=quality>1?15:quality==1?10:6;
        List<Vec3> path=new ArrayList<>();double[] radii=new double[points];
        for(int i=0;i<points;i++){
            double u=i/(double)(points-1),index=u*(history.size()-1);int n=Math.min(history.size()-2,(int)index);double t=index-n;
            Vec3 p0=history.get(Math.max(0,n-1)),p1=history.get(n),p2=history.get(n+1),p3=history.get(Math.min(history.size()-1,n+2));
            Vec3 at=catmull(p0,p1,p2,p3,t);double sway=Math.sin(Math.PI*u)*u;
            at=at.add(frame.side.scale(Math.sin(time*.115-u*3.7)*.19*sway)).add(frame.up.scale(Math.sin(time*.075+u*4.1)*.12*sway));path.add(at);
            double lobes=.70+.26*Math.cos(u*7.8-time*.09);
            radii[i]=(soul?.065:.127)*Math.pow(1-u,1.10)*lobes+.003;
        }
        // Dense front owns depth; fading rear cannot leave invisible depth.
        int seam=Math.max(2,points/3);
        sweep(pose,SOLID,path.subList(0,seam+1),Arrays.copyOfRange(radii,0,seam+1),PLUM.mix(VIOLET,.45),MAGENTA,1,quality,0,true);
        sweep(pose,FLUID,path.subList(seam,points),Arrays.copyOfRange(radii,seam,points),PLUM,VIOLET,soul?.58f:.65f,quality,.04f,false);
        if(quality>0)for(int k=0;k<2;k++){
            int begin=k==0?points/3:points/2,end=Math.min(points,begin+5);
            List<Vec3> detached=new ArrayList<>();double[] sizes=new double[end-begin];
            for(int i=begin;i<end;i++){
                double u=(i-begin)/(double)Math.max(1,end-begin-1),offset=(k==0?1:-1)*(.085+u*.13);
                detached.add(path.get(i).add(frame.side.scale(offset)).add(frame.up.scale(Math.sin(u*2.3+k)*.06)));
                sizes[i-begin]=(soul?.022:.037)*Math.pow(Math.sin(Math.PI*u),.6)+.004;
            }
            sweep(pose,FLUID,detached,sizes,k==0?VIOLET:PLUM,LILAC,.52f,quality,0,false);
        }
    }

    /** Unequal, independently moving closed fluid arcs, never billboards. */
    private static void streamer(PoseStack pose,Mesh mesh,Frame frame,double orbit,double thickness,double phase,double arc,double time,int curl,Color color,float alpha,int quality){
        int count=quality>1?12:8;List<Vec3> path=new ArrayList<>();double[] radii=new double[count];
        for(int i=0;i<count;i++){
            double u=i/(double)(count-1),a=phase+time+u*arc*Math.PI*curl,r=orbit*(.87+.16*Math.sin(u*3.1+phase));
            path.add(frame.radial(a,r).add(frame.forward.scale(.20-u*.63+.07*Math.sin(u*4.7+phase))));radii[i]=thickness*Math.pow(Math.sin(Math.PI*u),.60)+.004;
        }
        sweep(pose,mesh,path,radii,color,color.mix(LILAC,.24),alpha,quality,0,mesh==SOLID);
    }

    private static void rupture(PoseStack pose,Frame surface,float age,double radius,int quality){
        if(age>=16)return;double r=Mth.clamp(radius,.35,2.25),t=age/16;
        Vec3 center=surface.forward.scale(.035);
        if(age<4.2){
            double crush=1-age/4.2;
            body(pose,SOLID,center,surface,.27*crush+.015,.22*crush+.015,.14*crush+.010,age*.20,PLUM,MAGENTA,1,quality,0);
            if(age<1.8)body(pose,SOLID,center.add(surface.forward.scale(.16)),surface,.021,.024,.015,0,LILAC,PINPOINT,1,0,.4f);
        }
        // Unequal delays and elevations: outward pressure, curling, detachment.
        // None of these limbs form a ring or a repeated array of identical blobs.
        double[] angles={.35,2.02,3.51,5.30,4.33},lengths={.89,.74,1.0,.66,.82},rise={.24,.61,-.12,.37,-.26},delays={0,.38,1.10,.77,1.62};
        int limbs=quality>1?5:quality==1?3:2;
        for(int k=0;k<limbs;k++){
            double local=Math.max(0,age-delays[k]);if(local==0&&k>0)continue;
            double grown=ease(Math.min(1,local/6.0)),coil=ease(Mth.clamp((local-2)/11,0,1)),reach=r*lengths[k]*grown;
            Vec3 out=new Vec3(Math.cos(angles[k]),0,Math.sin(angles[k])),side=new Vec3(-out.z,0,out.x);
            int count=quality>1?13:8;List<Vec3> path=new ArrayList<>();double[] sizes=new double[count];
            double start=Mth.clamp((age-6)/16,0,.58),decay=Math.pow(1-t,.62);
            for(int i=0;i<count;i++){
                double u=start+(1-start)*i/(double)(count-1),hook=ease(Mth.clamp((u-.45)/.55,0,1));
                Vec3 at=center.add(out.scale(reach*u*(1-.19*hook*coil)))
                    .add(side.scale(Math.sin(u*Math.PI)*reach*.14+hook*hook*coil*r*(k%2==0?.27:-.21)))
                    .add(0,reach*rise[k]*u+Math.sin(u*Math.PI)*r*.16+hook*coil*r*.16,0);path.add(at);
                sizes[i]=(.055+r*.12)*Math.pow(Math.sin(Math.PI*i/(count-1)),.66)*decay*(.88+.12*Math.cos(u*5+k))+.003;
            }
            Mesh material=age<3.0&&k<2?SOLID:FLUID;float alpha=material==SOLID?1:(float)(.76*Math.pow(1-t,.68));
            sweep(pose,material,path,sizes,k%2==0?PLUM:VIOLET,k%2==0?MAGENTA:LILAC,alpha,quality,0,material==SOLID);
        }
        // Detached anisotropic tears: no spheres, center dots or star flakes.
        int fragments=quality>1?6:quality==1?3:1;
        for(int i=0;i<fragments;i++){
            double birth=.5+(i%3)*.6,life=Math.max(0,age-birth);if(life<=0)continue;
            double angle=i*2.399963+.29,travel=r*(.16+Math.min(.9,life*.058))*(.70+(i%3)*.11);
            Vec3 at=center.add(Math.cos(angle)*travel,.035*life-.0016*life*life+(i%3-1)*.095,Math.sin(angle)*travel);
            Vec3 direction=new Vec3(Math.cos(angle+.4),.35+Math.sin(life*.13)*.25,Math.sin(angle+.4));double size=(.036+(i%3)*.018)*Math.pow(1-t,.6);
            body(pose,FLUID,at,frame(direction),size,size*(.74+(i%2)*.38),size*(1.9+(i%3)*.5),i+life*.14,PLUM,LILAC,(float)(.72*(1-t)),Math.min(1,quality),0);
        }
    }
    private static void arrival(PoseStack pose,float age,int quality){
        if(age>=10)return;double t=age/10,fade=1-t;
        for(int i=0;i<(quality>0?3:1);i++){
            double a=i*2.13,tall=.12+age*.014;Frame frame=frame(new Vec3(Math.cos(a)*.22,1,Math.sin(a)*.22));
            body(pose,FLUID,new Vec3(Math.cos(a)*.11*t,age*.009,Math.sin(a)*.11*t),frame,.024*fade,.035*fade,tall*fade,i+age*.1,MINT,MINT.mix(PINPOINT,.18),(float)(.65*fade),Math.min(1,quality),.12f);
        }
    }

    /** Small local currents; whole-body heal tint belongs to the model renderer. */
    public static void drawWitch(RoyalWitch e,float partial,PoseStack pose,MultiBufferSource buffers){
        if(e.summonAge()<=0&&e.healFlash()<=0)return;int quality=quality(e.position(),partial);if(quality<0)return;
        SOLID.clear();FLUID.clear();
        if(e.summonAge()>0){
            float age=e.summonAge()-1+partial;double fade=ease(Mth.clamp(age/4,0,1))*Mth.clamp((24-age)/9,0,1);
            for(int k=0;k<(quality>0?4:2);k++){
                double angle=k*1.71+Math.toRadians(e.yBodyRot),radius=.44+(k%2)*.08;List<Vec3> path=new ArrayList<>();double[] sizes=new double[7];
                for(int j=0;j<7;j++){
                    double u=j/6d,a=angle+u*.65+.07*Math.sin(age*.17+k);
                    path.add(new Vec3(Math.cos(a)*radius*(1-u*.3),.04+u*(.38+(k%3)*.06),Math.sin(a)*radius*(1-u*.3)));sizes[j]=.026*Math.pow(Math.sin(Math.PI*u),.7)+.002;
                }
                sweep(pose,FLUID,path,sizes,PLUM,MAGENTA,(float)(fade*.70),Math.min(1,quality),0,false);
            }
        }
        if(e.healFlash()>0){
            float fade=Mth.clamp((e.healFlash()-partial)/8,0,1);
            for(int i=0;i<3;i++){
                double a=i*2.399963;Vec3 at=new Vec3(Math.cos(a)*.24,.85+i*.10,Math.sin(a)*.24);
                body(pose,FLUID,at,frame(UP),.017,.023,.08,0,MINT,MINT.mix(PINPOINT,.15),fade*.55f,0,.10f);
            }
        }
        flush(buffers);
    }

    /** Closed low-frequency deformation; normals derive from displaced faces. */
    private static void body(PoseStack pose,Mesh mesh,Vec3 at,Frame frame,double rx,double ry,double rz,double phase,Color low,Color high,float alpha,int quality,float selfLight){
        if(alpha<.012||Math.max(rx,Math.max(ry,rz))<.002)return;
        int sides=quality>1?12:8,rings=quality>1?7:quality==1?5:4;Vec3[][] points=new Vec3[rings+1][sides];
        for(int j=0;j<=rings;j++)for(int i=0;i<sides;i++){
            double latitude=j*Math.PI/rings,longitude=i*Math.PI*2/sides,s=Math.sin(latitude),c=Math.cos(latitude);
            double lobes=1+.14*Math.sin(longitude*2+phase)*s*s+.085*Math.cos(latitude*3-longitude+phase*.63)*s,twist=.19*Math.sin(latitude*2+phase*.53);
            double x=Math.cos(longitude+twist)*s*rx*lobes,y=Math.sin(longitude+twist)*s*ry*lobes,z=c*rz;
            x+=.11*rx*Math.sin(latitude*2+phase*.43)*s;y+=.08*ry*Math.cos(latitude*3+phase*.35)*s;points[j][i]=at.add(frame.point(x,y,z));
        }
        for(int j=0;j<rings;j++)for(int i=0;i<sides;i++){
            int n=(i+1)%sides;Vec3 a=points[j][i],b=points[j][n],c=points[j+1][n],d=points[j+1][i];Vec3 outward=a.add(b).add(c).add(d).scale(.25).subtract(at);
            double band=.40+.30*Math.sin(i*Math.PI*2/sides+phase*.28)+.16*Math.cos(j*Math.PI/rings*2-phase*.18);
            mesh.face(pose,a,b,c,d,outward,low.mix(high,band),mesh==SOLID?1:alpha,selfLight);
        }
    }

    /** Parallel-transport cross sections do not twist on vertical bends. */
    private static void sweep(PoseStack pose,Mesh mesh,List<Vec3> path,double[] radii,Color low,Color high,float alpha,int quality,float selfLight,boolean opaque){
        if(path.size()<2||alpha<.012)return;
        int sides=quality>1?7:5,count=path.size();Vec3[][] ring=new Vec3[count][sides];Vec3[] tangent=new Vec3[count];Vec3 carried=null;
        for(int j=0;j<count;j++){
            Vec3 previous=path.get(Math.max(0,j-1)),next=path.get(Math.min(count-1,j+1)),direction=safe(next.subtract(previous),UP);tangent[j]=direction;
            if(carried==null)carried=frame(direction).side;else carried=safe(carried.subtract(direction.scale(carried.dot(direction))),frame(direction).side);
            Vec3 up=direction.cross(carried).normalize();
            for(int i=0;i<sides;i++){
                double a=i*Math.PI*2/sides,r=Math.max(.001,radii[j]),oval=1+.10*Math.sin(a*2+j*.27);
                ring[j][i]=path.get(j).add(carried.scale(Math.cos(a)*r*oval)).add(up.scale(Math.sin(a)*r*.86));
            }
        }
        for(int j=0;j<count-1;j++)for(int i=0;i<sides;i++){
            int n=(i+1)%sides;Vec3 a=ring[j][i],b=ring[j][n],c=ring[j+1][n],d=ring[j+1][i];Vec3 center=path.get(j).add(path.get(j+1)).scale(.5),outward=a.add(b).add(c).add(d).scale(.25).subtract(center);
            double blend=.35+.23*Math.sin(i*Math.PI*2/sides+.5)+.20*Math.sin(j*.52);float endFade=opaque?1:(float)(.92-.22*j/Math.max(1d,count-1));
            mesh.face(pose,a,b,c,d,outward,low.mix(high,blend),opaque?1:alpha*endFade,selfLight);
        }
        for(int i=0;i<sides;i++){
            int n=(i+1)%sides;
            mesh.face(pose,path.get(0),ring[0][n],ring[0][i],path.get(0),tangent[0].scale(-1),low,opaque?1:alpha,selfLight);
            mesh.face(pose,path.get(count-1),ring[count-1][i],ring[count-1][n],path.get(count-1),tangent[count-1],high,opaque?1:alpha*.70f,selfLight);
        }
    }

    private static void flush(MultiBufferSource buffers){
        if(SOLID.size>0)SOLID.flush(buffers.getBuffer(SpellLayers.WITCH_SOLID),false);
        if(FLUID.size>0)FLUID.flush(buffers.getBuffer(SpellLayers.WITCH_GLOW),true);
    }
    private static int quality(Vec3 at,float partial){
        var client=Minecraft.getInstance();if(client.level==null)return -1;long tick=client.level.getGameTime();int bits=Float.floatToIntBits(partial);
        if(tick!=frameTick||bits!=framePartial){frameTick=tick;framePartial=bits;frameEffects=frameQuads=0;}
        double distance=at.distanceToSqr(client.gameRenderer.getMainCamera().getPosition());if(distance>64*64)return -1;int index=frameEffects++;
        if(distance>30*30||index>22||frameQuads>SOFT_FRAME_BUDGET||client.options.particles().get()==ParticleStatus.MINIMAL)return 0;
        return distance>13*13||index>10||client.options.particles().get()==ParticleStatus.DECREASED?1:2;
    }
    private static Frame frame(Vec3 direction){Vec3 f=safe(direction,new Vec3(0,0,1)),side=f.cross(Math.abs(f.y)>.92?RIGHT:UP).normalize();return new Frame(f,side,f.cross(side).normalize());}
    private static Vec3 safe(Vec3 vector,Vec3 fallback){return vector.lengthSqr()<1e-10?fallback:vector.normalize();}
    private static double ease(double t){return t*t*(3-2*t);}
    private static Vec3 catmull(Vec3 a,Vec3 b,Vec3 c,Vec3 d,double t){
        double t2=t*t,t3=t2*t;
        return b.scale(2).add(c.subtract(a).scale(t)).add(a.scale(2).subtract(b.scale(5)).add(c.scale(4)).subtract(d).scale(t2)).add(a.scale(-1).add(b.scale(3)).subtract(c.scale(3)).add(d).scale(t3)).scale(.5);
    }
}
