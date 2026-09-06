package dev.royalespells.client;

import dev.royalespells.VisualState;
import dev.royalespells.entity.Summoned;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

/** Per-call buffers, with no shared renderer state or exclusive model call hook. */
public final class SpellTint {
    private static final boolean QA=Boolean.getBoolean("royalespells.visualSmoke");
    private static int cloneBuffers,rageBuffers,frozenBuffers;
    public static void verifyVisualCoverage() {
        if(cloneBuffers==0 || rageBuffers==0 || frozenBuffers==0)
            throw new IllegalStateException("Missing tint buffers: clone="+cloneBuffers+" rage="+rageBuffers+" frozen="+frozenBuffers);
        System.out.println("ROYALE_TINT_BUFFERS_COMPLETE clone="+cloneBuffers+" rage="+rageBuffers+" frozen="+frozenBuffers);
    }
    private static final RenderLayer COLOR=RenderLayer.getEntityTranslucent(new Identifier("minecraft","textures/block/white_concrete.png"));
    public static VertexConsumerProvider wrap(VertexConsumerProvider original,LivingEntity entity) {
        boolean clone=entity instanceof Summoned s && s.isClone();
        if(clone)return new Buffers(original,.08f,.8f,1,.32f);
        if(VisualState.frozen(entity))return new Buffers(original,.48f,.83f,1,1);
        if(VisualState.raged(entity))return new Buffers(original,.86f,.25f,1,1);
        return original;
    }
    public static VertexConsumerProvider unwrap(VertexConsumerProvider buffers) {
        return buffers instanceof Buffers b?b.original:buffers;
    }
    private record Buffers(VertexConsumerProvider original,float red,float green,float blue,float alpha) implements VertexConsumerProvider {
        public VertexConsumer getBuffer(RenderLayer layer) {
            VertexConsumer delegate=original.getBuffer(layer);
            // Text, shadows, leashes and world overlays retain their own colors.
            if(layer.getVertexFormat()!=VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)return delegate;
            if(QA){if(alpha<1)cloneBuffers++;else if(red<.5f)frozenBuffers++;else rageBuffers++;}
            return new TintedConsumer(delegate,red,green,blue,alpha);
        }
    }
    private record TintedConsumer(VertexConsumer delegate,float red,float green,float blue,float alpha) implements VertexConsumer {
        public VertexConsumer vertex(double x,double y,double z){delegate.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){delegate.color((int)(r*red),(int)(g*green),(int)(b*blue),(int)(a*alpha));return this;}
        public VertexConsumer texture(float u,float v){delegate.texture(u,v);return this;}
        public VertexConsumer overlay(int u,int v){delegate.overlay(u,v);return this;}
        public VertexConsumer light(int u,int v){delegate.light(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){delegate.normal(x,y,z);return this;}
        public void next(){delegate.next();}
        public void fixedColor(int r,int g,int b,int a){delegate.fixedColor((int)(r*red),(int)(g*green),(int)(b*blue),(int)(a*alpha));}
        public void unfixColor(){delegate.unfixColor();}
    }
    /** Fabric's feature hook supplies the already animated model and pose. */
    public static final class ColorFeature<T extends LivingEntity,M extends EntityModel<T>> extends FeatureRenderer<T,M> {
        public ColorFeature(FeatureRendererContext<T,M> context){super(context);}
        public void render(MatrixStack matrices,VertexConsumerProvider buffers,int light,T entity,float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch) {
            if(entity.isInvisible())return;
            boolean clone=entity instanceof Summoned s && s.isClone();
            if(!clone && !VisualState.raged(entity))return;
            getContextModel().render(matrices,unwrap(buffers).getBuffer(COLOR),light,OverlayTexture.DEFAULT_UV,
                clone?.02f:.65f,clone?.8f:.08f,1,clone?.35f:.36f);
        }
    }
}
