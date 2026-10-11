package dev.royalespells;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class LogMotion {
    public static Quaternionf rotation(Vec3 direction,double distance) {
        Vec3 travel=SpellEngine.horizontal(direction);
        return new Quaternionf().rotationY((float)-Math.atan2(travel.z,travel.x)).rotateZ((float)(-distance/.72));
    }
}
