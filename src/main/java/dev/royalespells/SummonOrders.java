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
            &&target.isAlive()&&target.isAttackable()&&SpellEngine.enemy(player.getUUID(),target)&&mob.distanceToSqr(target)<=32*32)return target;
        return null;
    }
    /** All decisions are made here; navigation belongs to goals, never to this selector. */
    public static final String ESCORT="RoyaleWitchEscort";
    private static final double ACQUIRE_RANGE=16, KEEP_RANGE=24;

    public static boolean allowed(Mob mob,ServerPlayer player,LivingEntity target){
        if(!eligible(mob,player.getUUID(),target))return false;
        if(target==priority(mob,player)||recentAttacker(mob,target)||recentAttacker(player,target))return true;
        if(target instanceof Mob enemy&&enemy.getTarget()!=null&&SpellEngine.friendly(player.getUUID(),enemy.getTarget()))return true;
        if(target instanceof NeutralMob)return false;
        UUID owner=CombatCompatibility.ownerOf(target);
        if(owner!=null&&CombatCompatibility.resolve((ServerLevel)mob.level(),owner) instanceof net.minecraft.world.entity.player.Player)return false;
        return target instanceof Enemy;
    }

    private static boolean recentAttacker(LivingEntity protectedEntity,LivingEntity candidate){
        return candidate==protectedEntity.getLastHurtByMob()
            &&protectedEntity.tickCount-protectedEntity.getLastHurtByMobTimestamp()<=ORDER_TICKS;
    }

    private static boolean eligible(Mob mob,UUID owner,LivingEntity target){
        return target!=null&&target!=mob&&target.isAttackable()&&SpellEngine.enemy(owner,target)
            &&!(target instanceof ArmySkeleton army&&army.ghost())
            &&(!(target instanceof net.minecraft.world.entity.player.Player)||mob.getServer().isPvpAllowed())
            &&!mob.isAlliedTo(target);
    }

    /** Escort links are separate from the first-wave healing authorization. */
    public static LivingEntity leader(Mob mob){
        if(!(mob.level() instanceof ServerLevel world))return null;
        var tag=data(mob);
        Entity escort=tag.hasUUID(ESCORT)?world.getEntity(tag.getUUID(ESCORT)):null;
        if(escort instanceof LivingEntity living&&living.isAlive())return living;
        Entity master=CombatCompatibility.resolve(world,CombatCompatibility.ownerOf(mob));
        return master instanceof LivingEntity living&&living.isAlive()?living:null;
    }

    public static boolean escort(Mob mob){return data(mob).hasUUID(ESCORT);}

    /** A mob leader can explicitly designate an enemy; otherwise guards defend against hostiles. */
    private static boolean allowedForLeader(Mob mob,UUID owner,LivingEntity leader,LivingEntity target){
        if(!eligible(mob,owner,target)||target==leader)return false;
        if(leader instanceof ServerPlayer player)return allowed(mob,player,target);
        if(leader instanceof Mob commander&&commander.getTarget()==target)return true;
        if(recentAttacker(mob,target)||leader!=null&&recentAttacker(leader,target))return true;
        if(target instanceof Mob enemy&&(enemy.getTarget()==mob||leader!=null&&enemy.getTarget()==leader))return true;
        return target instanceof Enemy&&!(target instanceof NeutralMob)&&CombatCompatibility.ownerOf(target)==null;
    }

    /** Priority command > leader's fight > retaliation > retained target > nearest visible hostile. */
    public static LivingEntity select(Mob mob,UUID owner){
        var world=(ServerLevel)mob.level();
        Entity master=CombatCompatibility.resolve(world,owner);
        LivingEntity leader=leader(mob);
        if(owner!=null&&master==null&&leader==null&&!(mob instanceof ArmySkeleton army&&army.neutralArmy()))return null;
        return select(mob,owner,master,leader,false);
    }
    private static LivingEntity select(Mob mob,UUID owner,Entity master,LivingEntity leader,boolean guard){
        var world=(ServerLevel)mob.level();
        java.util.function.Predicate<LivingEntity> permitted=target->master instanceof ServerPlayer player
            ?allowed(mob,player,target)
            :leader!=null||guard?allowedForLeader(mob,owner,leader,target):eligible(mob,owner,target);
        if(master instanceof ServerPlayer player){
            LivingEntity order=priority(mob,player);
            if(order!=null&&permitted.test(order))return order;
        }
        LivingEntity designated=leader instanceof Mob commander?commander.getTarget():null;
        if(designated!=null&&mob.distanceToSqr(designated)<=32*32&&permitted.test(designated))return designated;
        LivingEntity attacker=mob.getLastHurtByMob();
        if(attacker!=null&&recentAttacker(mob,attacker)&&mob.distanceToSqr(attacker)<=KEEP_RANGE*KEEP_RANGE&&permitted.test(attacker))return attacker;
        LivingEntity current=mob.getTarget();
        // Losing sight for a moment is not a reason to abandon a valid pursuit.
        if(current!=null&&mob.distanceToSqr(current)<=KEEP_RANGE*KEEP_RANGE&&permitted.test(current))return current;
        return SpellEngine.targets(world,owner,mob.position(),ACQUIRE_RANGE,false).stream()
            .filter(permitted).filter(mob::hasLineOfSight)
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    public static void tick(Mob mob,UUID owner){
        if(!(mob.level() instanceof ServerLevel)||mob.tickCount%5!=0)return;
        LivingEntity selected=select(mob,owner);
        if(mob.getTarget()!=selected)mob.setTarget(selected);
    }
    public static void tickGuard(Mob mob,LivingEntity commander){
        if(!(mob.level() instanceof ServerLevel)||mob.tickCount%5!=0)return;
        LivingEntity selected=select(mob,CombatCompatibility.ownerOf(mob),null,commander,true);
        if(mob.getTarget()!=selected)mob.setTarget(selected);
    }
    private static net.minecraft.nbt.CompoundTag data(Entity entity){return entity.getPersistentData();}
    private SummonOrders(){}
}
