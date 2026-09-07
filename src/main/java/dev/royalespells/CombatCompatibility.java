package dev.royalespells;

import dev.royalespells.entity.Summoned;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import java.util.UUID;

/** Resolves ownership without taking over either mod's summon AI or damage pipeline. */
public final class CombatCompatibility {
    private static boolean ironLoaded;

    public static void install() {
        ironLoaded=ModList.get().isLoaded("irons_spellbooks");
        NeoForge.EVENT_BUS.addListener(CombatCompatibility::targetChanged);
    }

    public static UUID ownerOf(Entity entity) {
        if(entity instanceof Summoned summon)return summon.ownerId();
        if(entity instanceof OwnableEntity ownable)return ownable.getOwnerUUID();
        return ironLoaded?IronBridge.ownerOf(entity):null;
    }
    public static boolean magicSummon(Entity entity){return ironLoaded&&IronBridge.magicSummon(entity);}
    public static boolean nativeSkeleton(Entity entity){return ironLoaded&&IronBridge.nativeSkeleton(entity);}

    public static Entity resolve(ServerLevel level,UUID uuid) {
        if(uuid==null)return null;
        Entity result=level.getEntity(uuid);
        return result!=null?result:level.getServer().getPlayerList().getPlayer(uuid);
    }

    private static void targetChanged(LivingChangeTargetEvent event) {
        LivingEntity attacker=event.getEntity(),target=event.getNewAboutToBeSetTarget();
        if(target==null)return;
        if(attacker instanceof net.minecraft.world.entity.Mob mob&&(attacker instanceof Summoned||magicSummon(attacker))&&attacker.level() instanceof ServerLevel world
            &&resolve(world,ownerOf(attacker)) instanceof net.minecraft.server.level.ServerPlayer player){
            var priority=SummonOrders.priority(mob,player);
            if(priority!=null){event.setNewAboutToBeSetTarget(priority);return;}
            if(!SummonOrders.allowed(mob,player,target)){event.setNewAboutToBeSetTarget(null);return;}
        }
        // Restrict this hook to our units; unrelated Iron's combat keeps its own rules.
        if(attacker instanceof Summoned ours && SpellEngine.friendly(ours.ownerId(),target)
            || target instanceof Summoned theirs && SpellEngine.friendly(theirs.ownerId(),attacker))
            event.setNewAboutToBeSetTarget(null);
    }

    /** Loaded only after ModList confirms the optional mod is present. */
    private static final class IronBridge {
        static boolean magicSummon(Entity e){return e instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;}
        static boolean nativeSkeleton(Entity e){return e instanceof io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton;}
        static UUID ownerOf(Entity entity) {
            if(entity instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon summon) {
                Entity owner=summon.getSummoner();return owner==null?null:owner.getUUID();
            }
            return null;
        }
    }
    private CombatCompatibility() {}
}
