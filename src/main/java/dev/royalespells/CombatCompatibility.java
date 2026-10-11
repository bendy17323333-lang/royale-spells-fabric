package dev.royalespells;
import dev.royalespells.entity.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import java.util.UUID;
/** Vanilla owner/team recognition, independent of another magic system. */
public final class CombatCompatibility {
    public static UUID ownerOf(Entity e){if(e instanceof Summoned s)return s.ownerId();if(e instanceof OwnableEntity o)return o.getOwnerUUID();return null;}
    public static Entity resolve(ServerLevel w,UUID id){if(id==null)return null;Entity e=w.getEntity(id);return e!=null?e:w.getServer().getPlayerList().getPlayer(id);}
    public static LivingEntity target(Mob mob,LivingEntity target){
        if(target==null)return null;
        if(target instanceof ArmySkeleton army&&army.ghost())return null;
        if(mob instanceof Summoned s&&mob.level() instanceof ServerLevel w&&resolve(w,s.ownerId()) instanceof ServerPlayer player){
            var priority=SummonOrders.priority(mob,player);if(priority!=null)return priority;
            if(!SummonOrders.allowed(mob,player,target))return null;
        }
        if(mob instanceof Summoned s&&SpellEngine.friendly(s.ownerId(),target)||target instanceof Summoned other&&SpellEngine.friendly(other.ownerId(),mob))return null;
        return target;
    }
    private CombatCompatibility(){}
}
