// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.royalespells.RoyaleSpells;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/** Bounded world-volume integration, deliberately separate from screen effects.
 * A closed box only invokes fragments where a volume can exist. Exit surfaces
 * are rasterized once, supporting a camera inside the fog. The copied scene
 * depth terminates every ray at real terrain/entities. Fog never writes depth.
 * Shader registration is resource-reload-aware; Uniform objects are not cached.
 */
final class WitchVolume {
    private static final Logger LOG=LoggerFactory.getLogger("royalespells-witch-volume");
    private static final BufferBuilder PROXY=new BufferBuilder(1536);
    private static ShaderInstance shader;
    private static TextureTarget depth;
    private static boolean invalidateDepth=true;
    private static long frames,draws;private static boolean activeLogged;
    private WitchVolume(){}

    static void install(){
        CoreShaderRegistrationCallback.EVENT.register(registry->registry.register(RoyaleSpells.id("witch_volume"),DefaultVertexFormat.POSITION,reloaded->{
            shader=reloaded;invalidateDepth=true;activeLogged=false;
            LOG.info("WITCH_VOLUME_SHADER_READY: bounded 3D density integration, SceneDepth sampler, V3");
        }));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->{if(depth!=null){depth.destroyBuffers();depth=null;}});
    }

    static void render(WorldRenderContext event,List<WitchMagicRenderer.Volume> volumes){
        if(shader==null||volumes.isEmpty())return;
        Minecraft mc=Minecraft.getInstance();RenderTarget main=mc.getMainRenderTarget();
        if(main.width<1||main.height<1||main.getDepthTextureId()<0)return;
        // Flush deferred geometry BEFORE copying depth, not between volumes.
        mc.renderBuffers().bufferSource().endBatch();
        State state=new State();
        int rendered=0,stepTotal=0;double sampleBudget=0;
        try{
            // Depth-target allocation binds textures on the active unit. Keep
            // every such operation on unit zero, the unit saved by State.
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            if(depth==null||invalidateDepth||depth.width!=main.width||depth.height!=main.height){
                if(depth!=null)depth.destroyBuffers();
                depth=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);invalidateDepth=false;
            }
            // A distinct texture avoids sampling the depth attachment of the
            // FBO being written. copyDepthFrom binds framebuffer zero afterward.
            depth.copyDepthFrom(main);main.bindWrite(false);
            RenderSystem.viewport(0,0,main.width,main.height);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
            RenderSystem.enableCull();GL11.glCullFace(GL11.GL_FRONT);
            RenderSystem.enableBlend();
            // Match JSON even if BlendMode's last-applied cache skips its
            // upload after another renderer restored GL state manually.
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);
            RenderSystem.setShader(()->shader);
            shader.setSampler("SceneDepth",depth.getDepthTextureId());
            shader.safeGetUniform("Projection").set(event.projectionMatrix());
            shader.safeGetUniform("InverseProjection").set(new Matrix4f(event.projectionMatrix()).invert());
            shader.safeGetUniform("TargetSize").set((float)main.width,(float)main.height);
            boolean reduced=mc.options.particles().get()!=ParticleStatus.ALL;
            for(WitchMagicRenderer.Volume v:volumes){
                if(rendered>=40)break;
                double distance=Math.sqrt(v.distance());
                int steps=distance<12?36:distance<26?26:18;if(reduced)steps=Math.max(14,steps-8);
                Vec3 size=v.max().subtract(v.min());
                double pixels=Math.min((double)main.width*main.height,main.height*main.height*size.lengthSqr()/(Math.max(2,distance*distance)*2.2));
                if(sampleBudget>13_000_000)steps=14;
                if(sampleBudget>26_000_000&&distance>18)continue;
                sampleBudget+=pixels*steps;
                shader.safeGetUniform("VolumeToView").set(v.pose());
                shader.safeGetUniform("ViewToVolume").set(new Matrix4f(v.pose()).invert());
                shader.safeGetUniform("BoundsMin").set((float)v.min().x,(float)v.min().y,(float)v.min().z);
                shader.safeGetUniform("BoundsMax").set((float)v.max().x,(float)v.max().y,(float)v.max().z);
                shader.safeGetUniform("Params").set(v.mode(),v.time(),v.phase(),v.strength());
                shader.safeGetUniform("Shape").set(v.radius(),(float)steps,v.evolved()?1f:0f,(float)v.trail().length);
                for(int n=0;n<7;n++){
                    Vec3 p=n<v.trail().length?v.trail()[n]:Vec3.ZERO;
                    // Tail radii taper continuously along segments, never a
                    // collection of visible spherical particle surfaces.
                    float u=n/(float)Math.max(1,v.trail().length-1);
                    float radius=v.radius()*(.78f-.53f*u);
                    shader.safeGetUniform("Tail"+n).set((float)p.x,(float)p.y,(float)p.z,radius);
                }
                cube(v.min(),v.max());WitchMagicRenderer.submitted(v);rendered++;stepTotal+=steps;draws++;
            }
            frames++;
            if(rendered>0&&(!activeLogged||(Boolean.getBoolean("royalespells.witchSmoke")&&frames%120==0))){
                LOG.info("WITCH_VOLUME_ACTIVE: frame={}, volumes={}, marchSteps={}, totalDraws={}, depth={}x{}, sampledDepthTexture={}",frames,rendered,stepTotal,draws,depth.width,depth.height,depth.getDepthTextureId());activeLogged=true;
            }
        }finally{state.restore();}
    }

    /** Outward CCW faces; front culling leaves exactly the exit surface. */
    private static void cube(Vec3 lo,Vec3 hi){
        double x=lo.x,y=lo.y,z=lo.z,X=hi.x,Y=hi.y,Z=hi.z;
        PROXY.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
        quad(x,y,z,x,Y,z,X,Y,z,X,y,z); // -Z
        quad(x,y,Z,X,y,Z,X,Y,Z,x,Y,Z); // +Z
        quad(x,y,z,x,y,Z,x,Y,Z,x,Y,z); // -X
        quad(X,y,z,X,Y,z,X,Y,Z,X,y,Z); // +X
        quad(x,y,z,X,y,z,X,y,Z,x,y,Z); // -Y
        quad(x,Y,z,x,Y,Z,X,Y,Z,X,Y,z); // +Y
        BufferUploader.drawWithShader(PROXY.end());
    }
    private static void quad(double... xyz){for(int i=0;i<12;i+=3)PROXY.vertex(xyz[i],xyz[i+1],xyz[i+2]).endVertex();}

    /** Snapshot actual GL state once per pass. RenderSystem methods keep its
     * own caches in sync; framebuffer/cull-face modes are restored explicitly.
     * This also preserves renderer FBOs on Fabulous and nonstandard windows. */
    private static final class State {
        final boolean depthTest=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        final int cullMode=GL11.glGetInteger(GL11.GL_CULL_FACE_MODE),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int equationRgb=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),equationAlpha=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final int[] viewport=new int[4];final ShaderInstance priorShader=RenderSystem.getShader();final int texture0;
        State(){GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);RenderSystem.activeTexture(GL13.GL_TEXTURE0);texture0=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);RenderSystem.activeTexture(active);}
        void restore(){
            RenderSystem.depthMask(depthWrite);if(depthTest)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            GL11.glCullFace(cullMode);if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);GL20.glBlendEquationSeparate(equationRgb,equationAlpha);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);RenderSystem.bindTexture(texture0);RenderSystem.activeTexture(active);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);RenderSystem.setShader(()->priorShader);
        }
    }
}
