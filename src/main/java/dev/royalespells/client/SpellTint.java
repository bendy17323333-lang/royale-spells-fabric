package dev.royalespells.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.royalespells.VisualState;
import dev.royalespells.entity.Summoned;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Per-call buffers, with no shared renderer state or exclusive model call hook. */
public final class SpellTint {
    private static final boolean QA=Boolean.getBoolean("royalespells.visualSmoke");
    private static int cloneBuffers,rageBuffers,frozenBuffers;
    public static void verifyVisualCoverage() {
        if(cloneBuffers==0 || rageBuffers==0 || frozenBuffers==0)
            throw new IllegalStateException("Missing tint buffers: clone="+cloneBuffers+" rage="+rageBuffers+" frozen="+frozenBuffers);
        System.out.println("ROYALE_TINT_BUFFERS_COMPLETE clone="+cloneBuffers+" rage="+rageBuffers+" frozen="+frozenBuffers);
    }
    private static final RenderType COLOR=RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png"));
    public static MultiBufferSource wrap(MultiBufferSource original,LivingEntity entity) {
        boolean clone=entity instanceof Summoned s && s.isClone();
        if(clone)return new Buffers(original,.08f,.8f,1,.32f);
        if(VisualState.frozen(entity))return new Buffers(original,.48f,.83f,1,1);
        if(VisualState.raged(entity))return new Buffers(original,.86f,.25f,1,1);
        return original;
    }
    public static MultiBufferSource unwrap(MultiBufferSource buffers) {
        return buffers instanceof Buffers b?b.original:buffers;
    }
    private record Buffers(MultiBufferSource original,float red,float green,float blue,float alpha) implements MultiBufferSource {
        public VertexConsumer getBuffer(RenderType layer) {
            VertexConsumer delegate=original.getBuffer(layer);
            // Text, shadows, leashes and world overlays retain their own colors.
            if(layer.format()!=DefaultVertexFormat.NEW_ENTITY)return delegate;
            if(QA){if(alpha<1)cloneBuffers++;else if(red<.5f)frozenBuffers++;else rageBuffers++;}
            return new TintedConsumer(delegate,red,green,blue,alpha);
        }
    }
    private record TintedConsumer(VertexConsumer delegate,float red,float green,float blue,float alpha) implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z){delegate.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){delegate.setColor((int)(r*red),(int)(g*green),(int)(b*blue),(int)(a*alpha));return this;}
        public VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){delegate.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){delegate.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){delegate.setNormal(x,y,z);return this;}
    }
    /** NeoForge's layer hook supplies the already animated model and pose. */
    public static final class ColorFeature<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
        public ColorFeature(RenderLayerParent<T,M> context){super(context);}
        public void render(PoseStack matrices,MultiBufferSource buffers,int light,T entity,float limbAngle,float limbDistance,float delta,float animationProgress,float headYaw,float headPitch) {
            if(entity.isInvisible())return;
            boolean clone=entity instanceof Summoned s && s.isClone();
            if(!clone && !VisualState.raged(entity))return;
            getParentModel().renderToBuffer(matrices,unwrap(buffers).getBuffer(COLOR),light,OverlayTexture.NO_OVERLAY,
                clone?0x5905CCFF:0x5BA514FF);
        }
    }
}
