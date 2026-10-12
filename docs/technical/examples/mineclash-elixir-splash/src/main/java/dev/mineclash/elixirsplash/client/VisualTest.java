// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.client;

import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;

/** Opt-in development acceptance check. Uses genuine server damage and client
 * entity-event packets, never a mock burst. Creates only disposable new saves. */
public final class VisualTest {
    private static final String[] TYPES = {"mini_pekka", "skeleton", "zappies", "golem", "ice_spirit", "pekka", "royal_recruits", "elite_musketeer", "skeleton"};
    private static int phase, ticks, step, scene = -1, checkpoint;
    private static long start;
    private static volatile LivingEntity victim;
    private static final Set<String> frames = new LinkedHashSet<>();
    private static boolean duplicateChecked, cleared;
    private static int observedScenes;
    private static volatile int registeredMobCount;
    private static boolean allRegistered;
    private static final List<Double> renderMillis = new ArrayList<>();

    public static void install() {
        start = System.currentTimeMillis();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && SplashRenderer.lastFaces > 0)
                renderMillis.add(SplashRenderer.lastRenderNanos / 1000000.0);
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || scene < 0 || scene >= TYPES.length) return;
            var c = Minecraft.getInstance();
            for (Splash s : SplashEffects.ACTIVE) {
                String prefix = scene + "-" + TYPES[scene];
                if (s.age >= 2 && s.age <= 3) shot(c, prefix + "-burst");
                if (s.age >= 6 && s.age <= 7) shot(c, prefix + "-spray");
                if (s.age >= 14 && s.age <= 15) shot(c, prefix + "-falling");
                if (s.age >= 32 && s.age <= 33) shot(c, prefix + "-ground");
                if (s.age >= 62 && s.age <= 63) shot(c, prefix + "-fade");
                break;
            }
        });
    }
    private static void shot(Minecraft c, String name) {
        if (frames.add(name)) Screenshot.grab(c.gameDirectory, "elixir-" + name + ".png", c.getMainRenderTarget(), m -> System.out.println("ELIXIR_FRAME " + name));
    }
    private static void tick(Minecraft c) {
        ticks++; c.options.pauseOnLostFocus = false;
        if (System.currentTimeMillis() - start > 300000) { System.out.println("ELIXIR_VISUAL_TIMEOUT"); c.stop(); return; }
        if (phase == 0 && ticks > 30 && c.screen instanceof TitleScreen && c.getOverlay() == null) {
            phase = 1; c.options.fov().set(48); c.options.renderDistance().set(5); c.options.framerateLimit().set(60);
            var rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            String name = "Elixir-Disposable-QA-" + System.currentTimeMillis();
            c.createWorldOpenFlows().createFreshLevel(name, new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT), new WorldOptions(121100L, false, false), r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
        } else if (phase == 1 && c.player != null && c.getSingleplayerServer() != null) {
            phase = 2; c.options.hideGui = true;
            server(c, () -> {
                world(c).setDayTime(6000);
                for (int x = -15; x <= 15; x++) for (int z = -15; z <= 15; z++) world(c).setBlockAndUpdate(new BlockPos(x, 79, z), ((x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState());
                player(c).setGameMode(GameType.SPECTATOR);
                camera(c, new Vec3(4.2, 82.8, -4.8), new Vec3(0, 80.8, 0));
            });
        } else if (phase == 2 && c.player != null) {
            step++;
            if (c.screen != null) c.setScreen(null);
            if (step >= 30 && (step - 30) % 145 == 0 && scene + 1 < TYPES.length) {
                scene++; checkpoint = SplashEffects.totalEmitted;
                server(c, () -> setupScene(c));
            }
            if (scene >= 0 && scene < TYPES.length) {
                int t = (step - 30) % 145;
                if (t == 25) shot(c, scene + "-" + TYPES[scene] + "-alive");
                if (t == 30) server(c, () -> victim.hurt(world(c).damageSources().fellOutOfWorld(), 100000));
                if (t == 120) {
                    boolean expected = SplashEffects.totalEmitted == checkpoint + 1;
                    System.out.println("ELIXIR_SCENE " + TYPES[scene] + " oneBurst=" + expected);
                    if (expected) observedScenes++;
                }
            }
            if (step == 30 + TYPES.length * 145) {
                scene = TYPES.length;
                checkpoint = SplashEffects.totalEmitted;
                server(c, () -> {
                    clean(c);
                    var vanilla = EntityType.ZOMBIE.create(world(c)); vanilla.setPos(0, 80, 0); vanilla.setNoAi(true); world(c).addFreshEntity(vanilla);
                    vanilla.hurt(world(c).damageSources().fellOutOfWorld(), 100000);
                    var alive = spawn(c, "zappies", new Vec3(2, 80, 0));
                    alive.discard(); // Unload/despawn must never look like a death.
                });
            }
            if (step == 30 + TYPES.length * 145 + 70) {
                boolean scoped = SplashEffects.totalEmitted == checkpoint;
                System.out.println("ELIXIR_SCOPE vanillaAndDiscardIgnored=" + scoped);
                // Explicit duplicate packet test after native deaths have passed.
                var probe = (LivingEntity) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("mineclash:skeleton")).create(c.level);
                probe.setPos(0, 80, 0);
                probe.handleEntityEvent((byte) 60); int first = SplashEffects.totalEmitted;
                probe.handleEntityEvent((byte) 60);
                duplicateChecked = SplashEffects.totalEmitted == first && SplashEffects.duplicatePackets > 0;
                System.out.println("ELIXIR_DUPLICATE suppressed=" + duplicateChecked);
                if (!scoped) throw new IllegalStateException("Visual effect leaked to vanilla/despawn");
            }
            if (step == 30 + TYPES.length * 145 + 175) {
                cleared = SplashEffects.activeCount() == 0;
                System.out.println("ELIXIR_LIFETIME empty=" + cleared);
                checkpoint = SplashEffects.totalEmitted;
                server(c, () -> {
                    clean(c); camera(c, new Vec3(8, 88, -9), new Vec3(0, 80.5, 0));
                    for (int i = 0; i < 45; i++) {
                        var mob = spawn(c, "skeleton", new Vec3(i % 9 * .6 - 2.4, 80, i / 9 * .6 - 1.2));
                        mob.hurt(world(c).damageSources().fellOutOfWorld(), 100000);
                    }
                });
            }
            if (step == 30 + TYPES.length * 145 + 207) shot(c, "stress-45-deaths");
            if (step == 30 + TYPES.length * 145 + 285) {
                checkpoint = SplashEffects.totalEmitted;
                // Some 0.7.5 registry entries are unfinished internal placeholders
                // with no client renderer. Inspect on the client thread before
                // spawning; never create these invalid entries in the QA world.
                Set<EntityType<?>> renderable = new LinkedHashSet<>();
                for (var type : BuiltInRegistries.ENTITY_TYPE) {
                    var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                    if (!id.getNamespace().equals("mineclash")) continue;
                    var probe = type.create(c.level);
                    if (!(probe instanceof Mob)) continue;
                    if (c.getEntityRenderDispatcher().getRenderer(probe) != null) renderable.add(type);
                    else System.out.println("ELIXIR_REGISTRY_NO_RENDERER " + id);
                }
                server(c, () -> {
                    clean(c); world(c).setDayTime(6000);
                    int i=0;
                    for (var type: renderable) {
                        var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
                        if (!id.getNamespace().equals("mineclash")) continue;
                        var entity=type.create(world(c));
                        if (!(entity instanceof Mob mob)) continue;
                        mob.setPos((i%6)*.85-2.1,80,(i/6)*.85-1.7); mob.setNoAi(true);mob.setSilent(true);
                        world(c).addFreshEntity(mob);mob.hurt(world(c).damageSources().fellOutOfWorld(),100000);
                        i++;System.out.println("ELIXIR_REGISTRY_EXPECT "+id);
                    }
                    registeredMobCount=i;
                });
            }
            if (step == 30 + TYPES.length * 145 + 440) {
                allRegistered=SplashEffects.totalEmitted-checkpoint==registeredMobCount;
                System.out.println("ELIXIR_REGISTRY_ALL expected="+registeredMobCount+" actual="+(SplashEffects.totalEmitted-checkpoint)+" pass="+allRegistered);
                boolean pass = observedScenes == TYPES.length && duplicateChecked && cleared && SplashEffects.activeCount() == 0;
                Collections.sort(renderMillis);
                if (!renderMillis.isEmpty()) System.out.println("ELIXIR_RENDER_CPU_MS p50=" + renderMillis.get(renderMillis.size()/2) + " p95=" + renderMillis.get((int)(renderMillis.size()*.95)) + " samples=" + renderMillis.size());
                System.out.println("ELIXIR_VISUAL_" + (pass && allRegistered ? "PASS" : "FAIL") + " scenes=" + observedScenes + " frames=" + frames.size() + " renderFrames=" + SplashRenderer.renderedFrames + " peakFaces=" + SplashRenderer.peakFaces + " registeredMobs=" + registeredMobCount);
                phase = 3; c.stop();
            }
        }
    }
    private static void setupScene(Minecraft c) {
        clean(c);
        world(c).setDayTime(scene==7?18000:6000);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 80; y <= 82; y++) world(c).setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        Vec3[] eyes = {new Vec3(3.8, 82.6, -4.3), new Vec3(4, 81.6, 0), new Vec3(-3.6, 82.8, 4.4), new Vec3(4.5, 83.7, -5.6), new Vec3(3.6, 82, -4.4), new Vec3(0, 84.5, -2.7), new Vec3(3.8, 82.5, -4.5), new Vec3(3.8, 82.6, -4.3), new Vec3(3.8, 83.4, -4.6)};
        Vec3 at = new Vec3(0, 80, 0);
        if (scene == 4) { at = at.add(0, .5, 0); world(c).setBlockAndUpdate(new BlockPos(0, 80, 0), Blocks.STONE_SLAB.defaultBlockState()); }
        if (scene == 6) for (int y = 80; y <= 81; y++) for (int z = -2; z <= 2; z++) world(c).setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE_BRICKS.defaultBlockState());
        if (scene == 8) {
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) world(c).setBlockAndUpdate(new BlockPos(x,80,z),Blocks.WATER.defaultBlockState());
            at=at.add(0,1,0);
        }
        camera(c, eyes[scene], at.add(0, scene == 3 ? 1.15 : .7, 0));
        victim = spawn(c, TYPES[scene], at);
        if(scene==8)victim.setNoGravity(true);
        // Living bystander demonstrates that no shared model pose gets touched.
        spawn(c, TYPES[scene], new Vec3(2.5, 80, 1.8));
    }
    private static LivingEntity spawn(Minecraft c, String id, Vec3 at) {
        var mob = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mineclash", id)).create(world(c));
        if (mob == null) throw new IllegalStateException("Missing MineClash mob: " + id);
        mob.setPos(at); mob.setNoAi(true); mob.setSilent(true); mob.setYRot(15); world(c).addFreshEntity(mob); return mob;
    }
    private static void clean(Minecraft c) { for (var e : com.google.common.collect.ImmutableList.copyOf(world(c).getAllEntities())) if (e instanceof Mob) e.discard(); }
    private static void server(Minecraft c, Runnable action) { c.getSingleplayerServer().execute(action); }
    private static ServerLevel world(Minecraft c) { return c.getSingleplayerServer().overworld(); }
    private static ServerPlayer player(Minecraft c) { return c.getSingleplayerServer().getPlayerList().getPlayers().getFirst(); }
    private static void camera(Minecraft c, Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye); var p = player(c);
        p.teleportTo(world(c), eye.x, eye.y - p.getEyeHeight(), eye.z, (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90, (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance())));
        p.getAbilities().flying = true; p.onUpdateAbilities();
    }
    private VisualTest() {}
}
