// SPDX-License-Identifier: MIT
package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public final class SummonAiTests  {
    private Vec3 arena(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(10,12,10));
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-1;y<=5;y++)
            h.getLevel().setBlockAndUpdate(p.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState());
        return Vec3.atBottomCenterOf(p);
    }
    private <T extends Mob>T spawn(GameTestHelper h,EntityType<T> type,Vec3 at){
        T m=type.create(h.getLevel());m.setPos(at);m.setNoAi(true);m.setPersistenceRequired();
        h.getLevel().addFreshEntity(m);return m;
    }
    private ServerPlayer owner(GameTestHelper h,Vec3 at){var p=TestPlayers.create(h);p.setPos(at);return p;}
    private void cleanup(ServerPlayer player,Entity... entities){for(var e:entities)e.discard();player.server.getPlayerList().remove(player);player.discard();}
    private void ready(GameTestHelper h,Vec3 at,Runnable body){h.runAfterDelay(5,body);}

    @GameTest(template="empty",batch="summon-ai-retention",timeoutTicks=220)
    public void escortAcquiresWithoutMotherTargetAndRetainsItsOwnFight(GameTestHelper h){
        var at=arena(h);ready(h,at,()->{
            var p=owner(h,at.add(-8,0,0));var commander=spawn(h,EntityType.IRON_GOLEM,at);
            var troop=(AllySkeleton)SpellEngine.summon(h.getLevel(),p.getUUID(),at.add(0,0,2),"skeleton",false);
            var hostile=spawn(h,EntityType.HUSK,at.add(0,0,8));var cow=spawn(h,EntityType.COW,at.add(1,0,2));
            troop.getPersistentData().putUUID(SummonOrders.ESCORT,commander.getUUID());troop.tickCount=10;
            SummonOrders.tick(troop,p.getUUID());h.assertTrue(troop.getTarget()==hostile,"Guard must acquire a hostile even while mother has no target");
            commander.setTarget(hostile);SummonOrders.tick(troop,p.getUUID());commander.setTarget(null);
            for(int i=0;i<20;i++){troop.tickCount+=5;SummonOrders.tick(troop,p.getUUID());h.assertTrue(troop.getTarget()==hostile,"Missing mother target must not cancel the guard's retained enemy");}
            SummonOrders.order(p,cow);SummonOrders.tick(troop,p.getUUID());h.assertTrue(troop.getTarget()==cow,"Explicit player attack outranks the retained hostile");
            cleanup(p,commander,troop,hostile,cow);h.succeed();
        });
    }
    @GameTest(template="empty",batch="summon-ai-pursuit",timeoutTicks=300)
    public void meleePursuesBeyondIdleHomeInsteadOfFollowingOwner(GameTestHelper h){
        var at=arena(h);ready(h,at,()->{
            var p=owner(h,at.add(-8,0,0));
            var troop=(AllySkeleton)SpellEngine.summon(h.getLevel(),p.getUUID(),at,"skeleton",false);
            troop.restrictTo(BlockPos.containing(at),2);
            var hostile=spawn(h,EntityType.HUSK,at.add(0,0,8));hostile.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);hostile.setHealth(200);
            SummonOrders.order(p,hostile);double initial=troop.distanceToSqr(hostile);
            h.runAfterDelay(100,()->{
                h.assertTrue(troop.getTarget()==hostile,"Target must survive idle follow and the patrol boundary");
                h.assertTrue(troop.distanceToSqr(hostile)<initial/4,"Actual navigation must close the distance");
                h.assertTrue(hostile.getHealth()<200,"Actual melee hit is required; selecting a target alone is insufficient");
                cleanup(p,troop,hostile);h.succeed();
            });
        });
    }
    @GameTest(template="empty",batch="summon-ai-zombie",timeoutTicks=300)
    public void babyZombieKeepsAttackingWhileOwnerIsFarBehind(GameTestHelper h){
        var at=arena(h);ready(h,at,()->{
            var p=owner(h,at.add(-9,0,0));var troop=SpellEngine.summon(h.getLevel(),p.getUUID(),at,"zombie",false);
            var hostile=spawn(h,EntityType.HUSK,at.add(0,0,7));hostile.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);hostile.setHealth(200);
            SummonOrders.order(p,hostile);
            for(int delay:new int[]{20,40,60,80})h.runAfterDelay(delay,()->h.assertTrue(troop.getTarget()==hostile,"Following must not steal the active enemy"));
            h.runAfterDelay(100,()->{h.assertTrue(hostile.getHealth()<200,"Baby zombie must reach and hit the enemy");cleanup(p,troop,hostile);h.succeed();});
        });
    }
    @GameTest(template="empty",batch="summon-ai-formation",timeoutTicks=300)
    public void idleEscortMovesAheadOfItsCasterWithoutHuntingAnimals(GameTestHelper h){
        var at=arena(h);ready(h,at,()->{
            var p=owner(h,at.add(-8,0,0));var commander=spawn(h,EntityType.IRON_GOLEM,at);commander.setYRot(0);commander.setYHeadRot(0);
            var troop=(AllySkeleton)SpellEngine.summon(h.getLevel(),p.getUUID(),at.add(0,0,-4),"skeleton",false);
            troop.getPersistentData().putUUID(SummonOrders.ESCORT,commander.getUUID());
            var cow=spawn(h,EntityType.COW,at.add(2,0,2));
            // Iron golems may mark a Skeleton subtype as an enemy on contact,
            // even with their movement AI disabled. An escort link is still
            // authoritative: never turn around and attack its own commander.
            commander.setTarget(troop);troop.tickCount=10;SummonOrders.tick(troop,p.getUUID());
            h.assertTrue(troop.getTarget()==null,"A commander's target must never make the escort attack itself or its leader");
            // Observe the actual approach. Once the follow goal reaches its
            // stop radius, the normal idle stroll may move the unit again;
            // an arbitrary end-frame position is not proof that following failed.
            boolean[] reachedFront={false};
            var positions=new StringBuilder();
            for(int delay=5;delay<100;delay+=5){final int t=delay;h.runAfterDelay(delay,()->{
                reachedFront[0]|=troop.getZ()>commander.getZ()+.4;
                if(t%25==0)positions.append(" t=").append(t).append(" relative=").append(troop.position().subtract(commander.position()));
            });}
            h.runAfterDelay(100,()->{
                h.assertTrue(troop.getTarget()==null&&cow.getHealth()==cow.getMaxHealth(),"Unprovoked animals are safe; target="+troop.getTarget()+" cowHP="+cow.getHealth()+" at="+troop.position()+" leader="+commander.position());
                h.assertTrue(reachedFront[0]||troop.getZ()>commander.getZ()+.4,"Escort must reach its caster's front;"+positions+" final="+troop.position().subtract(commander.position()));
                cleanup(p,commander,troop,cow);h.succeed();
            });
        });
    }
}
