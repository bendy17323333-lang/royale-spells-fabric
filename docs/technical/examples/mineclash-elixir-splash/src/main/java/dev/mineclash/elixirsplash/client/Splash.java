// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Tick-based liquid motion, interpolated by the renderer at display frame rate.
 * The original reference uses 21 ballistic drops and three longer ground marks.
 * No particle sprites, blocks, fluids, entities or persistent world data are created. */
final class Splash {
    static final double GRAVITY = .033;
    final Vec3 base, origin;
    final double scale;
    final long seed;
    final List<Drop> drops = new ArrayList<>();
    final List<Stain> stains = new ArrayList<>();
    int age;

    Splash(ClientLevel world, Vec3 feet, float width, float height, long seed) {
        this.seed = seed;
        base = feet;
        scale = Mth.clamp(Math.sqrt(width * height) * .78, .6, 1.65);
        origin = feet.add(0, Mth.clamp(height * .39, .23, 1.05), 0);
        var random = new Random(seed);
        double phase = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 21; i++) {
            // Even angular coverage with restrained irregularity avoids random clumps.
            double angle = phase + i * 2.3999632297 + (random.nextDouble() - .5) * .24;
            double speed = (.07 + random.nextDouble() * .12) * scale;
            double up = (.10 + random.nextDouble() * .23) * Math.sqrt(scale);
            boolean major = i < 7;
            double size = (major ? .088 + random.nextDouble() * .028 : .028 + random.nextDouble() * .038) * scale;
            Vec3 velocity = new Vec3(Math.cos(angle) * speed, up, Math.sin(angle) * speed);
            Vec3 position = origin.add(Math.cos(angle) * .065 * scale, (random.nextDouble() - .5) * .12 * scale, Math.sin(angle) * .065 * scale);
            drops.add(new Drop(position, velocity, size, major, random.nextFloat(), major ? 18 + random.nextInt(4) : 7 + random.nextInt(13)));
        }
        for (int i = 0; i < 3; i++) {
            double angle = phase + i * Math.PI * 2 / 3;
            double offset = i == 0 ? 0 : .69 * scale;
            Vec3 point = feet.add(Math.cos(angle) * offset, .3, Math.sin(angle) * offset);
            double radius = i == 0 ? (.43 + random.nextDouble() * .035) * scale : (.16 + random.nextDouble() * .035) * scale;
            addStain(world, point, radius, 0, 64 + random.nextInt(15), seed + i * 53);
        }
    }

    void tick(ClientLevel world) {
        age++;
        int initial = drops.size();
        for (int i = 0; i < initial; i++) {
            Drop drop = drops.get(i);
            drop.previous = drop.position;
            if (drop.dead) continue;
            drop.age++;
            Vec3 end = drop.position.add(drop.velocity);
            var hit = world.clip(new ClipContext(drop.position, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            if (hit.getType() != HitResult.Type.MISS) {
                drop.position = hit.getLocation();
                drop.dead = true;
                if (hit.getDirection() == Direction.UP) {
                    addStain(world, hit.getLocation().add(0, .08, 0), drop.size * (drop.major ? 1.6 : 1.25), age, 34, seed + i * 971L);
                    if (drop.major && drops.size() < 35) {
                        // Two small secondary splashes; never recursively split.
                        for (int j = 0; j < 2; j++) {
                            double a = i * 2.39996 + j * Math.PI;
                            drops.add(new Drop(hit.getLocation().add(0, .018, 0), new Vec3(Math.cos(a) * .044, .075, Math.sin(a) * .044), drop.size * .32, false, drop.tint, 7));
                        }
                    }
                }
            } else {
                drop.position = end;
                drop.velocity = drop.velocity.multiply(.97, .98, .97).add(0, -GRAVITY, 0);
            }
            if (drop.age >= drop.life) drop.dead = true;
        }
    }

    private void addStain(ClientLevel world, Vec3 above, double size, int born, int life, long randomSeed) {
        if (stains.size() >= 24) return;
        var hit = world.clip(new ClipContext(above, above.add(0, -1.4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS || hit.getDirection() != Direction.UP) return;
        Vec3 center = hit.getLocation().add(0, .012, 0);
        for (Stain previous : stains) {
            if (Math.abs(previous.center().y - center.y) < .08
                    && previous.center().distanceToSqr(center) < Math.pow((previous.radius() + size) * .92, 2)) return;
        }
        Vec3[] ring = new Vec3[20];
        var random = new Random(randomSeed);
        double phase = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < ring.length; i++) {
            double a = i * Math.PI * 2 / ring.length;
            double r = size * (1 + .17 * Math.sin(3 * a + phase) + .10 * Math.sin(5 * a - phase));
            Vec3 edge = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            var sample = world.clip(new ClipContext(edge.add(0, .13, 0), edge.add(0, -.14, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            // No floating stain across a ledge or a pool stretched down a cliff.
            if (sample.getType() != HitResult.Type.MISS && sample.getDirection() == Direction.UP
                    && Math.abs(sample.getLocation().y - (center.y - .012)) < .075) ring[i] = sample.getLocation().add(0, .012, 0);
        }
        boolean fluid = !world.getFluidState(net.minecraft.core.BlockPos.containing(hit.getLocation().add(0, -.04, 0))).isEmpty();
        stains.add(new Stain(center, ring, born, fluid ? 18 : life, phase, size));
    }

    static final class Drop {
        Vec3 previous, position, velocity;
        final Vec3 initial, launch;
        final double size;
        final boolean major;
        final float tint;
        final int life;
        int age;
        boolean dead;
        Drop(Vec3 at, Vec3 speed, double size, boolean major, float tint, int life) {
            previous = position = initial = at;
            velocity = launch = speed;
            this.size = size; this.major = major; this.tint = tint; this.life = life;
        }
    }
    record Stain(Vec3 center, Vec3[] ring, int born, int life, double phase, double radius) {}
}
