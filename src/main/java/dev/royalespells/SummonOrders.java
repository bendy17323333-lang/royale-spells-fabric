package dev.royalespells;

import dev.royalespells.entity.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;

/** Orders affect summon AI only. They never make passive creatures immune to aimed spells. */
public final class SummonOrders {
    private static final String ORDER="RoyaleAttackOrder",UNTIL="RoyaleAttackOrderUntil";
    public static final int ORDER_TICKS=200;
    public static final net.minecraft.resources.ResourceLocation SPEED=RoyaleSpells.id("small_skeleton_speed");
    public static void install(){
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.AttackEntityEvent e)->{
            if(e.getEntity() instanceof ServerPlayer player&&e.getTarget() instanceof LivingEntity target)order(player,target);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post e)->{
            if(e.getSource().getEntity() instanceof ServerPlayer player&&e.getNewDamage()>0)order(player,e.getEntity());
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e)->{
            if(!e.getLevel().isClientSide&&e.getEntity() instanceof Mob mob)boostSkeleton(mob);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post e)->{
            if(e.getEntity() instanceof Mob mob&&!(mob instanceof Summoned)&&CombatCompatibility.magicSummon(mob))tick(mob,CombatCompatibility.ownerOf(mob));
        });
    }
    public static boolean smallSkeleton(Entity e){return e instanceof AllySkeleton||CombatCompatibility.nativeSkeleton(e);}
    public static void boostSkeleton(Mob mob){
        if(!smallSkeleton(mob))return;
        var speed=mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if(speed!=null&&!speed.hasModifier(SPEED))speed.addPermanentModifier(new AttributeModifier(SPEED,.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    public static void order(ServerPlayer player,LivingEntity target){
        if(!SpellEngine.enemy(player.getUUID(),target))return;
        var tag=player.getPersistentData();tag.putUUID(ORDER,target.getUUID());tag.putLong(UNTIL,player.serverLevel().getGameTime()+ORDER_TICKS);
    }
    public static LivingEntity priority(Mob mob,ServerPlayer player){
        var tag=player.getPersistentData();var world=(ServerLevel)mob.level();
        if(tag.hasUUID(ORDER)&&tag.getLong(UNTIL)>=world.getGameTime()&&world.getEntity(tag.getUUID(ORDER)) instanceof LivingEntity target
            &&SpellEngine.enemy(player.getUUID(),target)&&mob.distanceToSqr(target)<=32*32)return target;
        return null;
    }
    public static boolean allowed(Mob mob,ServerPlayer player,LivingEntity target){
        if(!SpellEngine.enemy(player.getUUID(),target))return false;
        if(target==priority(mob,player))return true;
        if(player.getLastHurtByMob()==target&&player.tickCount-player.getLastHurtByMobTimestamp()<=ORDER_TICKS)return true;
        if(target instanceof Mob enemy&&enemy.getTarget()!=null&&SpellEngine.friendly(player.getUUID(),enemy.getTarget()))return true;
        // An unprovoked neutral mob or another player's peaceful summon is not a wild enemy.
        if(target instanceof NeutralMob)return false;
        UUID owner=CombatCompatibility.ownerOf(target);
        if(owner!=null&&CombatCompatibility.resolve((ServerLevel)mob.level(),owner) instanceof net.minecraft.world.entity.player.Player)return false;
        return target instanceof Enemy;
    }
    public static void tick(Mob mob,UUID owner){
        if(!(mob.level() instanceof ServerLevel world)||mob.tickCount%5!=0)return;
        var master=CombatCompatibility.resolve(world,owner);
        LivingEntity selected=null;
        if(master instanceof ServerPlayer player){
            selected=priority(mob,player);
            if(selected==null){
                var current=mob.getTarget();
                if(current!=null&&mob.distanceToSqr(current)<=24*24&&allowed(mob,player,current))selected=current;
                else selected=SpellEngine.targets(world,owner,mob.position(),16,false).stream().filter(e->allowed(mob,player,e)&&mob.hasLineOfSight(e)).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            }
        }else if(owner==null||master!=null||mob instanceof ArmySkeleton army&&army.neutralArmy()){
            selected=SpellEngine.targets(world,owner,mob.position(),16,false).stream().filter(e->e!=mob&&mob.hasLineOfSight(e)).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        // Unloaded or disconnected owners do not turn their troops into feral animal hunters.
        if(mob.getTarget()!=selected)mob.setTarget(selected);
        if(selected==null&&master instanceof LivingEntity living&&master.level()==world&&mob.distanceToSqr(master)>36)mob.getNavigation().moveTo(living,1.1);
    }
    private SummonOrders(){}
}
