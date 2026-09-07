package dev.royalespells;

import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import java.util.function.Supplier;

/** Suppress only the incidental hurt impulse, leaving explicit spell push/pull intact. */
public final class CombatImpact {
    private static final ThreadLocal<Deque<LivingEntity>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
    public static boolean suppressed(LivingEntity entity){return ACTIVE.get().contains(entity);}
    public static <T> T withoutKnockback(LivingEntity entity,Supplier<T> damage){
        var stack=ACTIVE.get();stack.push(entity);try{return damage.get();}finally{stack.pop();if(stack.isEmpty())ACTIVE.remove();}
    }
    private CombatImpact(){}
}
