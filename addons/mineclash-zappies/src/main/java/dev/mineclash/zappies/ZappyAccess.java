package dev.mineclash.zappies;
public interface ZappyAccess {
    ZappyBrain zappyBrain();
    net.minecraft.nbt.CompoundTag zappyArc();
    void zappyDischarge(net.minecraft.world.phys.Vec3 end);
}
