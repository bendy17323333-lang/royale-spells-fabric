// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.client;

import dev.mineclash.elixirsplash.SplashConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.*;

public final class SplashEffects {
    static final List<Splash> ACTIVE = new ArrayList<>();
    private static final Set<LivingEntity> SEEN = Collections.newSetFromMap(new WeakHashMap<>());
    private static ClientLevel world;
    public static int totalEmitted, duplicatePackets;

    public static boolean onDeathPoof(LivingEntity entity) {
        if (!entity.level().isClientSide || !SplashConfig.ENABLED.get()
                || !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("mineclash")) return false;
        ensureWorld();
        if (!SEEN.add(entity)) { duplicatePackets++; return true; }
        var client = Minecraft.getInstance();
        double distance = SplashConfig.DISTANCE.get();
        if (world == null || client.gameRenderer.getMainCamera().getPosition().distanceToSqr(entity.position()) > distance * distance) return true;
        while (ACTIVE.size() >= SplashConfig.MAX_BURSTS.get()) ACTIVE.removeFirst();
        ACTIVE.add(new Splash(world, entity.position(), entity.getBbWidth(), entity.getBbHeight(),
                entity.getUUID().getLeastSignificantBits() ^ world.getGameTime()));
        totalEmitted++;
        if (Boolean.getBoolean("elixirsplash.visualTest"))
            System.out.println("ELIXIR_DEATH type=" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " id=" + entity.getId() + " deathTime=" + entity.deathTime + " active=" + ACTIVE.size());
        return true;
    }

    public static void tick(ClientTickEvent.Post event) {
        ensureWorld();
        if (world == null || Minecraft.getInstance().isPaused()) return;
        for (Splash splash : ACTIVE) splash.tick(world);
        ACTIVE.removeIf(splash -> splash.age > 86);
        if (!SplashConfig.ENABLED.get()) ACTIVE.clear();
    }

    private static void ensureWorld() {
        var current = Minecraft.getInstance().level;
        if (world != current) { ACTIVE.clear(); SEEN.clear(); world = current; }
    }
    public static int activeCount() { return ACTIVE.size(); }
    private SplashEffects() {}
}
