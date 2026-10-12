// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public abstract class SplashRenderer extends RenderType {
    private SplashRenderer() { super("unused", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, () -> {}, () -> {}); }
    private static final RenderType LIQUID = create("mineclash_elixir_volume", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 65536, false, true,
            CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static int lastFaces, peakFaces, renderedFrames;
    public static long lastRenderNanos;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var client = Minecraft.getInstance();
        if (client.level == null || SplashEffects.ACTIVE.isEmpty()) { lastFaces = 0; return; }
        long begin = System.nanoTime();
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        var buffers = client.renderBuffers().bufferSource();
        var out = buffers.getBuffer(LIQUID);
        lastFaces = 0;
        for (Splash splash : SplashEffects.ACTIVE) {
            if (splash.origin.distanceToSqr(camera) > 96 * 96) continue;
            float light = .38f + .62f * client.level.getMaxLocalRawBrightness(BlockPos.containing(splash.origin)) / 15f;
            var mesh = new LiquidMesh(out, pose.last().pose(), camera, light);
            float age = splash.age + partial;
            for (Splash.Stain stain : splash.stains) mesh.stain(stain, age);
            for (Splash.Drop drop : splash.drops) {
                if (drop.dead) continue;
                float t = drop.age + partial;
                Vec3 position = drop.previous.lerp(drop.position, partial);
                float fade = Mth.clamp((drop.life - t) / 4f, 0, 1);
                double shrink = .68 + .32 * fade;
                double stretch = 1.1 + Math.min(1.5, drop.velocity.length() * 4);
                double wobble = Math.sin(t * 1.4 + drop.tint * 6) * Math.exp(-t * .08);
                mesh.drop(position, drop.velocity, drop.size * shrink * (1 + wobble * .08), drop.size * shrink * stretch, t * .8, drop.tint, .95f * fade);
                if (drop.major && t < 6.8) {
                    float neck = (float) Math.sin(Math.PI * Mth.clamp(t / 6.8, 0, 1));
                    Vec3 root = drop.initial.lerp(position, Math.max(0, (t - 2) / 5));
                    mesh.ligament(root, position, new Vec3(0, .12 * splash.scale, 0), drop.size * .58 * neck, .86f * neck, drop.tint);
                }
            }
            // A brief connected central volume breaks into the ballistic spray.
            // Gone before the falling-drop phase; no lingering luminous bubble.
            if (age < 5) {
                double envelope = Math.sin(Math.PI * age / 5);
                for (int i = 0; i < 3; i++) {
                    double a = splash.seed % 100 + i * Math.PI * 2 / 3;
                    Vec3 c = splash.origin.add(Math.cos(a) * .10 * splash.scale, -.05, Math.sin(a) * .10 * splash.scale);
                    mesh.drop(c, new Vec3(Math.cos(a) * .3, 1, Math.sin(a) * .3), .24 * splash.scale * envelope, .20 * splash.scale * envelope, a + age, .6f, (float) (.65 * envelope));
                }
            }
            lastFaces += mesh.faces;
        }
        buffers.endBatch(LIQUID);
        peakFaces = Math.max(peakFaces, lastFaces);
        renderedFrames++;
        lastRenderNanos = System.nanoTime() - begin;
    }
}
