// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash;

import dev.mineclash.elixirsplash.client.SplashEffects;
import dev.mineclash.elixirsplash.client.SplashRenderer;
import dev.mineclash.elixirsplash.client.VisualTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** Purely client-side: existing vanilla entity-event packets are sufficient. */
@Mod(value = ElixirSplash.ID, dist = Dist.CLIENT)
public final class ElixirSplash {
    public static final String ID = "elixirsplash";
    public ElixirSplash(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SplashConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(SplashEffects::tick);
        NeoForge.EVENT_BUS.addListener(SplashRenderer::render);
        if (Boolean.getBoolean("elixirsplash.visualTest")) VisualTest.install();
    }
}
