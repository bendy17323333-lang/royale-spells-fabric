// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/** For direct import into MineClash. Copy this file instead of ElixirSplash.java.
 * Register SplashConfig.SPEC as a CLIENT config in MineClash's mod constructor. */
@EventBusSubscriber(modid = "mineclash", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MineClashElixirClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.addListener(SplashEffects::tick);
            NeoForge.EVENT_BUS.addListener(SplashRenderer::render);
        });
    }
    private MineClashElixirClient() {}
}
