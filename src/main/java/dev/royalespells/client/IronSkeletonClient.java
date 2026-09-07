package dev.royalespells.client;
public final class IronSkeletonClient {
    public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(io.redspace.ironsspellbooks.registries.EntityRegistry.SUMMONED_SKELETON.get(),RoyaleSkeletonRenderer::new);}
    private IronSkeletonClient(){}
}
