package dev.royalespells;

import dev.royalespells.entity.RoyaleUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** NoAI also disables vanilla travel; spell forces still use the ordinary collision solver. */
public final class SpellMotion {
    private record Push(Vec3 velocity,int ticks){}
    private static final Map<LivingEntity,Push> PUSHES=new HashMap<>();
    private static boolean anchored(LivingEntity e){return e instanceof RoyaleUnit unit&&unit.building();}
    private static boolean needsPhysics(LivingEntity e){return e instanceof Mob mob&&mob.isNoAi();}
    public static void cancel(LivingEntity e){PUSHES.remove(e);}
    public static void impulse(LivingEntity e,Vec3 velocity){
        if(anchored(e))return;
        if(needsPhysics(e)){PUSHES.put(e,new Push(velocity,16));e.setDeltaMovement(Vec3.ZERO);}
        else{e.push(velocity);e.hurtMarked=true;}
    }
    public static void pull(LivingEntity e,Vec3 velocity){
        if(anchored(e))return;
        if(needsPhysics(e)){PUSHES.remove(e);e.move(MoverType.SELF,velocity);e.setDeltaMovement(Vec3.ZERO);e.hurtMarked=true;}
        else{e.setDeltaMovement(velocity);e.hurtMarked=true;}
    }
    public static void tick(MinecraftServer server){
        var iterator=PUSHES.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var e=entry.getKey();var push=entry.getValue();
            if(e.isRemoved()||!e.isAlive()||e.hasEffect(RoyaleSpells.STUN)||!needsPhysics(e)||push.ticks<=0){iterator.remove();continue;}
            e.move(MoverType.SELF,push.velocity);e.hurtMarked=true;
            Vec3 next=push.velocity.multiply(.72,.98,.72).add(0,e.isNoGravity()?0:-.08,0);
            if(e.onGround()&&next.y<0)next=new Vec3(next.x,0,next.z);
            entry.setValue(new Push(next,push.ticks-1));
        }
    }
    public static void clear(){PUSHES.clear();}
}
