package dev.mineclash.zappies;
import dev.mineclash.zappies.pause.ElectricPause;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.liziyowo.mineclash.*;
import org.liziyowo.mineclash.entity.mob.Zappies;
import java.util.*;
public final class ZappyCombat {
    private static final ThreadLocal<Set<LivingEntity>> IMPACT=ThreadLocal.withInitial(HashSet::new);
    public static boolean noKnockback(LivingEntity target){return IMPACT.get().contains(target);}
    public static boolean shoot(Zappies from,LivingEntity target){
        if(!(from.level() instanceof ServerLevel w)||ElectricPause.active(from)||!ZappyBrain.of(from).canShoot(target))return false;
        int immunity=target.invulnerableTime;boolean landed;
        var set=IMPACT.get();boolean added=set.add(target);
        // Three separate machines each deal their hit. Preserve other sources' current
        // immunity window, and let ordinary damage-cancellation/armor events run.
        target.invulnerableTime=0;
        try{landed=target.hurt(CRDamageSources.electricity(from),(float)from.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));}
        finally{target.invulnerableTime=Math.max(immunity,target.invulnerableTime);if(added)set.remove(target);}
        if(landed&&target.isAlive())target.addEffect(new MobEffectInstance(ZappiesAddon.ELECTRICAL_STUN,10,0,false,false,true));
        from.playSound(CRSoundEvents.ZAP_ATTACK.get(),.65f,1);
        Vec3 end=target.getBoundingBox().getCenter();
        // An authoritative shot event drives the client's short connected arc.
        // No persistent dust trail or continuous laser is left between volleys.
        ((ZappyAccess)from).zappyDischarge(end);
        w.sendParticles(ParticleTypes.ELECTRIC_SPARK,end.x,end.y,end.z,5,.18,.2,.18,.025);
        return landed;
    }
}
