package dev.royalespells;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** The model's +Y nose and -Y exhaust follow the same arc as the projectile. */
public final class RocketMotion {
    private RocketMotion() {}
    public static Vec3 position(Vec3 start,Vec3 target,double progress) {
        double p=Mth.clamp(progress,0,1);
        return start.lerp(target,p).add(0,6*Math.sin(Math.PI*p),0);
    }
    public static Vec3 direction(Vec3 start,Vec3 target,double progress) {
        double p=Mth.clamp(progress,0,1);
        Vec3 tangent=target.subtract(start).add(0,6*Math.PI*Math.cos(Math.PI*p),0);
        // A strictly vertical shot has zero speed at its apex; choose its next direction.
        if(tangent.lengthSqr()<1.0e-12)return new Vec3(0,p<.5?1:-1,0);
        return tangent.normalize();
    }
    public static Quaternionf rotation(Vec3 start,Vec3 target,double progress) {
        Vec3 d=direction(start,target,progress);
        // Yaw about world Y, then tip local +Y towards travel. Stable even when exactly vertical.
        float yaw=(float)Math.atan2(d.x,d.z);
        float tip=(float)Math.atan2(d.horizontalDistance(),d.y);
        return new Quaternionf().rotationY(yaw).rotateX(tip);
    }
    public static Vec3 exhaust(Vec3 start,Vec3 target,double progress) {
        return position(start,target,progress).subtract(direction(start,target,progress).scale(1.5));
    }
}
