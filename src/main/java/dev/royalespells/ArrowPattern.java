package dev.royalespells;

import net.minecraft.world.phys.Vec3;

public final class ArrowPattern {
    public static final int COUNT=48;
    public static Vec3 point(int index,int wave) {
        double angle=index*2.399963229728653+wave*.61;
        // An explicit outer row reaches the configured damage radius; the remainder fills its area evenly.
        double radius=Spell.ARROWS.radius*(index<16?1:Math.sqrt((index-15.5)/32)*.93);
        return new Vec3(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
    }
    private ArrowPattern(){}
}
