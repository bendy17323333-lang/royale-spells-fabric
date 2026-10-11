// SPDX-License-Identifier: MIT
package dev.royalespells.client;
import dev.royalespells.iron.WitchContent;
final class WitchClient {
    static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(WitchContent.WITCH,WitchRenderer::new);
        e.registerEntityRenderer(WitchContent.SKELETON,RoyaleSkeletonRenderer::new);
    }
}
