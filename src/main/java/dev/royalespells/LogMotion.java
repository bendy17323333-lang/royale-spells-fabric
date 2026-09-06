package dev.royalespells;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public final class LogMotion {
    public static Quaternionf rotation(Vec3d direction,double distance) {
        Vec3d travel=SpellEngine.horizontal(direction);
        return new Quaternionf().rotationY((float)-Math.atan2(travel.z,travel.x)).rotateZ((float)(-distance/.72));
    }
}
