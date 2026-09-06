package dev.royalespells;

import dev.royalespells.entity.RoyaleUnit;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** NoAI also disables vanilla travel; spell forces still use the ordinary collision solver. */
public final class SpellMotion {
    private record Push(Vec3d velocity,int ticks){}
    private static final Map<LivingEntity,Push> PUSHES=new HashMap<>();
    private static boolean anchored(LivingEntity e){return e instanceof RoyaleUnit unit&&unit.building();}
    private static boolean needsPhysics(LivingEntity e){return e instanceof MobEntity mob&&mob.isAiDisabled();}
    public static void cancel(LivingEntity e){PUSHES.remove(e);}
    public static void impulse(LivingEntity e,Vec3d velocity){
        if(anchored(e))return;
        if(needsPhysics(e)){PUSHES.put(e,new Push(velocity,16));e.setVelocity(Vec3d.ZERO);}
        else{e.addVelocity(velocity);e.velocityModified=true;}
    }
    public static void pull(LivingEntity e,Vec3d velocity){
        if(anchored(e))return;
        if(needsPhysics(e)){PUSHES.remove(e);e.move(MovementType.SELF,velocity);e.setVelocity(Vec3d.ZERO);e.velocityModified=true;}
        else{e.setVelocity(velocity);e.velocityModified=true;}
    }
    public static void tick(MinecraftServer server){
        var iterator=PUSHES.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var e=entry.getKey();var push=entry.getValue();
            if(e.isRemoved()||!e.isAlive()||e.hasStatusEffect(RoyaleSpells.STUN)||!needsPhysics(e)||push.ticks<=0){iterator.remove();continue;}
            e.move(MovementType.SELF,push.velocity);e.velocityModified=true;
            Vec3d next=push.velocity.multiply(.72,.98,.72).add(0,e.hasNoGravity()?0:-.08,0);
            if(e.isOnGround()&&next.y<0)next=new Vec3d(next.x,0,next.z);
            entry.setValue(new Push(next,push.ticks-1));
        }
    }
    public static void clear(){PUSHES.clear();}
}
