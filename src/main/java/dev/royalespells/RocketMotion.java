package dev.royalespells;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

/** The model's +Y nose and -Y exhaust follow the same arc as the projectile. */
public final class RocketMotion {
    private RocketMotion() {}
    public static Vec3d position(Vec3d start,Vec3d target,double progress) {
        double p=MathHelper.clamp(progress,0,1);
        return start.lerp(target,p).add(0,6*Math.sin(Math.PI*p),0);
    }
    public static Vec3d direction(Vec3d start,Vec3d target,double progress) {
        double p=MathHelper.clamp(progress,0,1);
        Vec3d tangent=target.subtract(start).add(0,6*Math.PI*Math.cos(Math.PI*p),0);
        // A strictly vertical shot has zero speed at its apex; choose its next direction.
        if(tangent.lengthSquared()<1.0e-12)return new Vec3d(0,p<.5?1:-1,0);
        return tangent.normalize();
    }
    public static Quaternionf rotation(Vec3d start,Vec3d target,double progress) {
        Vec3d d=direction(start,target,progress);
        // Yaw about world Y, then tip local +Y towards travel. Stable even when exactly vertical.
        float yaw=(float)Math.atan2(d.x,d.z);
        float tip=(float)Math.atan2(d.horizontalLength(),d.y);
        return new Quaternionf().rotationY(yaw).rotateX(tip);
    }
    public static Vec3d exhaust(Vec3d start,Vec3d target,double progress) {
        return position(start,target,progress).subtract(direction(start,target,progress).multiply(1.5));
    }
}
