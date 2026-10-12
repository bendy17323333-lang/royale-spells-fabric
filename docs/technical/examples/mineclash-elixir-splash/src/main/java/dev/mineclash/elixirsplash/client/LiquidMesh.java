// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Closed low-poly liquid volumes. Face lighting and a restrained wet highlight
 * remain visible without bloom. There is no camera-facing geometry. */
final class LiquidMesh {
    private static final int SIDES = 10, RINGS = 6;
    private static final Vec3 KEY = new Vec3(-.35, .85, -.4).normalize();
    private final VertexConsumer out;
    private final Matrix4f matrix;
    private final Vec3 camera;
    private final float light;
    int faces;
    LiquidMesh(VertexConsumer out, Matrix4f matrix, Vec3 camera, float light) {
        this.out = out; this.matrix = matrix; this.camera = camera; this.light = light;
    }

    void drop(Vec3 center, Vec3 direction, double radius, double length, double wobble, float tint, float alpha) {
        Vec3 up = direction.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : direction.normalize();
        Vec3 right = up.cross(Math.abs(up.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 forward = right.cross(up).normalize();
        Vec3[][] grid = new Vec3[RINGS + 1][SIDES];
        float[][][] colors = new float[RINGS + 1][SIDES][];
        for (int j = 0; j <= RINGS; j++) {
            double polar = j * Math.PI / RINGS;
            // Pinched trailing end and a fuller leading bead: elixir, not rocks.
            double y = Math.cos(polar), taper = 1 + .28 * y;
            for (int i = 0; i < SIDES; i++) {
                double a = i * Math.PI * 2 / SIDES;
                double r = Math.sin(polar) * radius * taper * (1 + .035 * Math.sin(a * 3 + wobble));
                grid[j][i] = center.add(up.scale(y * length)).add(right.scale(Math.cos(a) * r)).add(forward.scale(Math.sin(a) * r));
                Vec3 normal = up.scale(y / Math.max(.001, length))
                        .add(right.scale(Math.cos(a) * Math.sin(polar) / Math.max(.001, radius)))
                        .add(forward.scale(Math.sin(a) * Math.sin(polar) / Math.max(.001, radius))).normalize();
                colors[j][i] = shade(grid[j][i], normal, tint);
            }
        }
        for (int j = 0; j < RINGS; j++) for (int i = 0; i < SIDES; i++) {
            int n = (i + 1) % SIDES;
            colored(grid[j][i], colors[j][i], alpha); colored(grid[j+1][i], colors[j+1][i], alpha);
            colored(grid[j+1][n], colors[j+1][n], alpha); colored(grid[j][n], colors[j][n], alpha);
            faces++;
        }
    }

    void ligament(Vec3 start, Vec3 end, Vec3 bend, double radius, float alpha, float tint) {
        Vec3 axis = end.subtract(start).normalize();
        Vec3 right = axis.cross(Math.abs(axis.y) > .95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(axis).normalize();
        Vec3[] previous = null;
        for (int j = 0; j <= 6; j++) {
            double f = j / 6.0;
            Vec3 center = start.lerp(end, f).add(bend.scale(Math.sin(Math.PI * f)));
            // Narrow liquid neck, with a rounded tip that joins the flying drop.
            double r = radius * (.2 + .8 * Math.sin(Math.PI * f));
            Vec3[] ring = new Vec3[6];
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * i / 3;
                ring[i] = center.add(right.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
            }
            if (previous != null) for (int i = 0; i < 6; i++) {
                int n = (i + 1) % 6;
                quad(previous[i], ring[i], ring[n], previous[n], start.lerp(end, (f + (j - 1) / 6.0) / 2), tint, alpha);
            }
            previous = ring;
        }
    }

    void stain(Splash.Stain stain, float age) {
        float t = age - stain.born();
        if (t < 0 || t >= stain.life()) return;
        float fade = 1 - Mth.clamp((t - stain.life() * .45f) / (stain.life() * .55f), 0, 1);
        double spread = .65 + .35 * (1 - Math.exp(-t * .7));
        double shrink = .7 + .3 * fade;
        Vec3 center = stain.center(), dome = center.add(0, .025 * fade, 0);
        Vec3[] ring = stain.ring();
        float[] middleColor = shade(dome, new Vec3(0, 1, 0), .65f);
        for (int i = 0; i < ring.length; i++) {
            Vec3 a = ring[i], b = ring[(i + 1) % ring.length];
            if (a == null || b == null) continue;
            a = center.lerp(a, spread * shrink); b = center.lerp(b, spread * shrink);
            Vec3 innerA = dome.lerp(a, .70), innerB = dome.lerp(b, .70);
            // A thick meniscus and slightly domed centre. Separate rings give
            // the liquid an edge and a wet interior without a decal sprite.
            Vec3 normalA = a.subtract(center).normalize().scale(.4).add(0, 1, 0).normalize();
            Vec3 normalB = b.subtract(center).normalize().scale(.4).add(0, 1, 0).normalize();
            float[] edgeA = shade(a, normalA, .15f), edgeB = shade(b, normalB, .15f);
            colored(dome, middleColor, .89f * fade); colored(innerA, middleColor, .89f * fade);
            colored(innerB, middleColor, .89f * fade); colored(dome, middleColor, .89f * fade); faces++;
            colored(innerA, middleColor, .89f * fade); colored(a, edgeA, .89f * fade);
            colored(b, edgeB, .89f * fade); colored(innerB, middleColor, .89f * fade); faces++;
            quad(a, a.add(0, -.010, 0), b.add(0, -.010, 0), b, center, .02f, .64f * fade);
        }
    }

    private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 center, float tint, float alpha) {
        // Primitive arithmetic here avoids hundreds of thousands of short-lived
        // vector objects per frame during a large simultaneous army death.
        double ux=b.x-a.x, uy=b.y-a.y, uz=b.z-a.z;
        double vx=c.x-a.x, vy=c.y-a.y, vz=c.z-a.z;
        double nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
        double mx=(a.x+b.x+c.x+d.x)*.25, my=(a.y+b.y+c.y+d.y)*.25, mz=(a.z+b.z+c.z+d.z)*.25;
        double norm=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(norm<1e-10) { nx=mx-center.x; ny=my-center.y; nz=mz-center.z; norm=Math.max(1e-10,Math.sqrt(nx*nx+ny*ny+nz*nz)); }
        if(nx*(mx-center.x)+ny*(my-center.y)+nz*(mz-center.z)<0)norm=-norm;
        nx/=norm; ny/=norm; nz/=norm;
        float diffuse=(float)(.61+.39*Math.max(0,nx*KEY.x+ny*KEY.y+nz*KEY.z));
        double hx=camera.x-mx, hy=camera.y-my, hz=camera.z-mz;
        double length=Math.max(1e-10,Math.sqrt(hx*hx+hy*hy+hz*hz));
        hx=hx/length+KEY.x; hy=hy/length+KEY.y; hz=hz/length+KEY.z;
        length=Math.max(1e-10,Math.sqrt(hx*hx+hy*hy+hz*hz));
        float wet=(float)Math.pow(Math.max(0,(nx*hx+ny*hy+nz*hz)/length),22)*.28f;
        // Magenta elixir: dark violet underneath, pink facets and a pale wet rim.
        float r = Mth.clamp((.73f + tint * .17f) * diffuse * light + wet, 0, 1);
        float g = Mth.clamp((.055f + tint * .16f) * diffuse * light + wet * .85f, 0, 1);
        float blue = Mth.clamp((.70f + tint * .14f) * diffuse * light + wet, 0, 1);
        vertex(a, r, g, blue, alpha); vertex(b, r, g, blue, alpha); vertex(c, r, g, blue, alpha); vertex(d, r, g, blue, alpha);
        faces++;
    }
    private void vertex(Vec3 p, float r, float g, float b, float a) {
        out.addVertex(matrix, (float) (p.x-camera.x), (float) (p.y-camera.y), (float) (p.z-camera.z)).setColor(r, g, b, a);
    }
    private float[] shade(Vec3 p, Vec3 n, float tint) {
        Vec3 view = camera.subtract(p).normalize();
        Vec3 half = KEY.add(view).normalize();
        float diffuse = (float) (.48 + .52 * Math.max(0, n.dot(KEY)));
        float wet = (float) Math.pow(Math.max(0, n.dot(half)), 19) * .50f;
        float rim = (float) Math.pow(1 - Math.abs(n.dot(view)), 4) * .07f;
        return new float[]{
                Mth.clamp((.73f + tint * .17f) * diffuse * light + wet + rim, 0, 1),
                Mth.clamp((.055f + tint * .16f) * diffuse * light + wet * .85f + rim * .4f, 0, 1),
                Mth.clamp((.70f + tint * .14f) * diffuse * light + wet + rim, 0, 1)};
    }
    private void colored(Vec3 p, float[] color, float alpha) { vertex(p, color[0], color[1], color[2], alpha); }
}
