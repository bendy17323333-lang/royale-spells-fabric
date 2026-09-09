package dev.royalespells.client;

import dev.royalespells.*;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Separate per-entity age and gait clocks. Weak keys release unloaded mobs.
 * Never pass a renderer/model/bone as a key: those objects are shared. */
public final class ChillAnimation {
    private static final Map<LivingEntity,AnimationTimeline> AGES=new WeakHashMap<>(),GAITS=new WeakHashMap<>();
    public static double age(LivingEntity e,double input){return sample(AGES,e,input);}
    public static float gait(LivingEntity e,float input){return (float)sample(GAITS,e,input);}
    private static double sample(Map<LivingEntity,AnimationTimeline> clocks,LivingEntity e,double input) {
        boolean slow=VisualState.snowbound(e)&&e.isAlive();
        if(!slow&&!clocks.containsKey(e))return input;
        return clocks.computeIfAbsent(e,k->new AnimationTimeline()).sample(input,slow?SnowballChill.ANIMATION_RATE:1);
    }
    private ChillAnimation(){}
}
