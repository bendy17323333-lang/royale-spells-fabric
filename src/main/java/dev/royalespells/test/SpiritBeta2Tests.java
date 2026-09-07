package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import dev.royalespells.spirit.SpiritElement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Regression cases reproduce flight and melee in a real ticking world. */
@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public class SpiritBeta2Tests {
    private Vec3 floor(GameTestHelper c){
        var p=c.absolutePos(new BlockPos(7,15,7));
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-1;y<7;y++)c.getLevel().setBlockAndUpdate(p.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState());
        return Vec3.atBottomCenterOf(p);
    }
    private List<net.minecraft.world.level.ChunkPos> force(GameTestHelper c,Vec3 at){
        var center=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var p=new net.minecraft.world.level.ChunkPos(center.x+x,center.z+z);if(c.getLevel().setChunkForced(p.x,p.z,true))chunks.add(p);}return chunks;
    }
    private ElementalSpirit spirit(GameTestHelper c,Vec3 at,SpiritElement kind,UUID owner){
        var e=RoyaleSpells.ELEMENTAL_SPIRIT.create(c.getLevel());e.configure(owner,kind,1);e.moveTo(at);c.getLevel().addFreshEntity(e);return e;
    }
    private Mob skeleton(GameTestHelper c,Vec3 at,UUID owner){var e=SpellEngine.summon(c.getLevel(),owner,at,"skeleton",false);e.setNoAi(true);return e;}
    private void oneLeapKills(GameTestHelper c,SpiritElement kind){
        var at=floor(c);var chunks=force(c,at);var targets=new ArrayList<Mob>();var spirits=new ArrayList<ElementalSpirit>();
        double[] distances={.45,1.25,2.85};
        for(int i=0;i<3;i++){
            var start=at.add((i-1)*5,0,0);var target=skeleton(c,start.add(0,0,distances[i]),UUID.randomUUID());
            c.assertTrue(target.getHealth()==6,"Uses the real six-health graveyard summon");
            var e=spirit(c,start,kind,UUID.randomUUID());targets.add(target);spirits.add(e);
            c.assertTrue(e.beginLeap(target),"Actual close/medium/far leap began");
        }
        c.runAfterDelay(60,()->{
            for(int i=0;i<targets.size();i++)c.assertFalse(targets.get(i).isAlive(),kind+" must kill a base small skeleton in one real leap at distance "+distances[i]+"; hp="+targets.get(i).getHealth()+" spirit="+spirits.get(i).position());
            c.assertTrue(spirits.stream().allMatch(Entity::isRemoved),"Every one-use spirit finishes");
            targets.forEach(Entity::discard);spirits.forEach(Entity::discard);chunks.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();
        });
    }
    @GameTest(template="empty",batch="beta2-fire-hit",timeoutTicks=90) public void fireLeapsKillSmallSkeletonsAtThreeDistances(GameTestHelper c){oneLeapKills(c,SpiritElement.FIRE);}
    @GameTest(template="empty",batch="beta2-ice-hit",timeoutTicks=90) public void iceLeapsKillSmallSkeletonsAtThreeDistances(GameTestHelper c){oneLeapKills(c,SpiritElement.ICE);}
    @GameTest(template="empty",batch="beta2-electro-hit",timeoutTicks=90) public void electroLeapsKillSmallSkeletonsAtThreeDistances(GameTestHelper c){oneLeapKills(c,SpiritElement.ELECTRO);}
    @GameTest(template="empty",batch="beta2-heal-hit",timeoutTicks=90) public void healLeapsKillSmallSkeletonsAtThreeDistances(GameTestHelper c){oneLeapKills(c,SpiritElement.HEAL);}
    @GameTest(template="empty",batch="beta2-running-takeoff",timeoutTicks=160)
    public void allFourSpiritsRunUpAndHitInsteadOfLandingShort(GameTestHelper c){
        var at=floor(c).add(0,0,-.5);var chunks=force(c,at);var owner=TestPlayers.create(c);owner.setPos(at.add(0,4,-5));
        var targets=new ArrayList<Mob>();var spirits=new ArrayList<ElementalSpirit>();int i=0;
        for(var kind:SpiritElement.values()){
            double x=(i++-1.5)*4;var target=EntityType.HUSK.create(c.getLevel());target.moveTo(at.add(x,0,4));target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.getAttribute(Attributes.ARMOR).setBaseValue(0);target.setHealth(100);c.getLevel().addFreshEntity(target);targets.add(target);
            var e=spirit(c,at.add(x,0,-2),kind,owner.getUUID());e.setTarget(target);spirits.add(e);
        }
        c.runAfterDelay(120,()->{
            for(int k=0;k<4;k++){var e=spirits.get(k);c.assertTrue(targets.get(k).getHealth()<=93,e.element()+" completes a real six-block run-up and leap; hp="+targets.get(k).getHealth()+" end="+e.position()+" leap="+e.leapTicks());}
            c.assertTrue(spirits.get(2).chainedTargets()==1,"Electro physically contacts its own victim and starts its chain");
            targets.forEach(Entity::discard);spirits.forEach(Entity::discard);owner.server.getPlayerList().remove(owner);owner.discard();chunks.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();
        });
    }
    @GameTest(template="empty",batch="beta2-flight-retarget",timeoutTicks=90)
    public void deadOriginalTargetDoesNotSwallowContactWithTheNextEnemy(GameTestHelper c){
        var at=floor(c);var chunks=force(c,at);var first=skeleton(c,at.add(0,0,2.5),UUID.randomUUID());var next=skeleton(c,at.add(0,0,1.8),UUID.randomUUID());
        var e=spirit(c,at,SpiritElement.ELECTRO,UUID.randomUUID());c.assertTrue(e.beginLeap(first),"Leap committed");first.discard();
        c.runAfterDelay(55,()->{c.assertFalse(next.isAlive(),"In-flight collision still starts the chain after the original target dies");c.assertTrue(e.chainedTargets()>=1,"Electro chain actually began");next.discard();e.discard();chunks.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();});
    }
    @GameTest(template="empty",batch="beta2-swept-flight",timeoutTicks=90)
    public void fastLeapSweepsBetweenTicksInsteadOfSkippingASmallBody(GameTestHelper c){
        var at=floor(c);var chunks=force(c,at);var victim=skeleton(c,at.add(0,0,1.25),UUID.randomUUID());var e=spirit(c,at,SpiritElement.ELECTRO,UUID.randomUUID());
        e.beginLeap(victim);e.setDeltaMovement(0,.1,3.3);
        c.runAfterDelay(35,()->{c.assertFalse(victim.isAlive(),"Enemy crossed between tick endpoints receives the first discharge");victim.discard();e.discard();chunks.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();});
    }
    @GameTest(template="empty",batch="beta2-flight-wall",timeoutTicks=90)
    public void MidFlightWallAndFriendlyBodiesCannotCauseRemoteDischarge(GameTestHelper c){
        var at=floor(c);var chunks=force(c,at);UUID owner=UUID.randomUUID();var victim=skeleton(c,at.add(0,0,2.8),UUID.randomUUID());
        var e=spirit(c,at,SpiritElement.ELECTRO,owner);e.beginLeap(victim);var ally=skeleton(c,at.add(.42,0,0),owner);
        var pos=BlockPos.containing(at);for(int x=-2;x<=2;x++)for(int y=0;y<5;y++)c.getLevel().setBlockAndUpdate(pos.offset(x,y,1),Blocks.STONE.defaultBlockState());
        c.runAfterDelay(55,()->{c.assertTrue(victim.getHealth()==6&&ally.getHealth()==6&&e.chainedTargets()==0,"Collision recovery neither electrocutes allies nor crosses a wall: enemy="+victim.getHealth()+" ally="+ally.getHealth()+" links="+e.chainedTargets());victim.discard();ally.discard();e.discard();chunks.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();});
    }
    @GameTest(template="empty",batch="beta2-general-no-retreat",timeoutTicks=110)
    public void generalHoldsGroundAndAttacksWhenAnEnemyClosesIn(GameTestHelper c){
        var at=floor(c);var chunks=force(c,at);var units=ArmyFormation.neutral(c.getLevel(),at,0);var general=units.getFirst();general.moveTo(at);
        for(var e:units){e.setNoAi(true);if(!e.general())e.moveTo(at.add((e.formationSlot()%5-2)*.7,0,-3));}
        var victim=EntityType.HUSK.create(c.getLevel());victim.moveTo(at.add(0,0,1.15));victim.setNoAi(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);victim.getAttribute(Attributes.ARMOR).setBaseValue(0);victim.setHealth(500);c.getLevel().addFreshEntity(victim);general.setNoAi(false);
        double[] farthestBack={0};for(int t=1;t<=70;t++)c.runAfterDelay(t,()->farthestBack[0]=Math.max(farthestBack[0],at.z-general.getZ()));
        c.runAfterDelay(75,()->{c.assertTrue(farthestBack[0]<.35,"General never retreats to the soldiers behind it: "+farthestBack[0]);c.assertTrue(victim.getHealth()<498,"General actually strikes the nearby enemy");units.forEach(Entity::discard);victim.discard();chunks.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();});
    }
    @GameTest(template="empty",batch="beta2-living-survivors",timeoutTicks=110)
    public void livingSoldiersSurviveGeneralDeathAndCannotBecomeGhostsAfterward(GameTestHelper c){
        var at=floor(c);var units=ArmyFormation.neutral(c.getLevel(),at,0);var general=units.getFirst();var living=units.get(1);var ghost=units.get(2);
        units.forEach(e->{e.setNoAi(true);e.setNoGravity(true);for(int t=0;t<20;t++)e.tick();});
        ghost.hurt(c.getLevel().damageSources().generic(),100);c.assertTrue(ghost.ghost(),"Supported soldier converts");
        general.hurt(c.getLevel().damageSources().generic(),100);general.hurt(c.getLevel().damageSources().generic(),100);
        for(int t=0;t<25;t++){living.tick();ghost.tick();}
        c.assertTrue(living.isAlive()&&!living.isRemoved()&&!living.ghost()&&living.dissolve()==0,"Living soldier stays alive after general death");c.assertTrue(ghost.isRemoved(),"Unsupported ghost dissipates");
        var victim=skeleton(c,living.position().add(0,0,.5),UUID.randomUUID());c.assertTrue(living.doHurtTarget(victim)&&victim.getHealth()<6,"Survivor remains a real attacker");
        var n=new CompoundTag();living.saveWithoutId(n);living.discard();var reload=RoyaleSpells.ARMY_SKELETON.create(c.getLevel());reload.load(n);c.getLevel().addFreshEntity(reload);for(int t=0;t<25;t++)reload.tick();
        c.assertTrue(reload.isAlive()&&reload.dissolve()==0&&!reload.supported(),"Save/reload preserves living survivor without restoring ghost support");
        reload.hurt(c.getLevel().damageSources().generic(),100);c.assertTrue(!reload.isAlive()&&!reload.ghost(),"Next lethal blow kills normally");
        units.forEach(Entity::discard);reload.discard();victim.discard();c.succeed();
    }
}
