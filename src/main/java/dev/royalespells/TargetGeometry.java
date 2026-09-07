package dev.royalespells;

import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Shared mathematical shapes: the rolling preview is the actual swept damage circle. */
public final class TargetGeometry {
    public static List<Vec3> circle(Vec3 center,double radius) {
        var points=new ArrayList<Vec3>();
        for(int i=0;i<=64;i++){double a=i*Math.PI/32;points.add(center.add(Math.cos(a)*radius,0,Math.sin(a)*radius));}
        return points;
    }
    public static List<Vec3> rolling(Vec3 start,Vec3 forward,double length,double radius) {
        forward=SpellEngine.horizontal(forward);Vec3 end=start.add(forward.scale(length)),side=new Vec3(forward.z,0,-forward.x);
        var points=new ArrayList<Vec3>();
        for(int i=0;i<=24;i++){double a=-Math.PI/2+i*Math.PI/24;points.add(end.add(forward.scale(Math.cos(a)*radius)).add(side.scale(Math.sin(a)*radius)));}
        for(int i=0;i<=24;i++){double a=Math.PI/2+i*Math.PI/24;points.add(start.add(forward.scale(Math.cos(a)*radius)).add(side.scale(Math.sin(a)*radius)));}
        points.add(points.getFirst());return points;
    }
    public static Vec3 decoyTarget(net.minecraft.world.level.Level world,Vec3 target,Vec3 direction) {
        return SpellEngine.ground(world,target.add(SpellEngine.horizontal(direction).cross(new Vec3(0,1,0)).scale(5)));
    }
    private TargetGeometry(){}
}
