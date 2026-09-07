package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.army.ArmyFormation;
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

@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public class SpiritTests {
    private List<net.minecraft.world.level.ChunkPos> force(GameTestHelper c,Vec3 at){var center=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));var list=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=new net.minecraft.world.level.ChunkPos(center.x+x,center.z+z);if(c.getLevel().setChunkForced(chunk.x,chunk.z,true))list.add(chunk);}return list;}
    private Vec3 floor(GameTestHelper c){var at=c.absolutePos(new BlockPos(7,15,7));for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){c.getLevel().setBlockAndUpdate(at.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<5;y++)c.getLevel().setBlockAndUpdate(at.offset(x,y,z),Blocks.AIR.defaultBlockState());}return Vec3.atBottomCenterOf(at);}
    private Mob target(GameTestHelper c,Vec3 at){var e=EntityType.COW.create(c.getLevel());e.moveTo(at);e.setNoAi(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);c.getLevel().addFreshEntity(e);return e;}
    private ElementalSpirit spirit(GameTestHelper c,Vec3 at,SpiritElement element,UUID owner){var e=RoyaleSpells.ELEMENTAL_SPIRIT.create(c.getLevel());e.configure(owner,element,1);e.moveTo(at);e.setNoAi(true);c.getLevel().addFreshEntity(e);return e;}
    private List<ArmySkeleton> army(GameTestHelper c,Vec3 at){var list=ArmyFormation.neutral(c.getLevel(),at,0);c.assertTrue(list.size()==16,"Test has full formation");for(var e:list){e.setNoAi(true);e.setNoGravity(true);for(int t=0;t<20;t++)e.tick();}return list;}
    @GameTest(template="empty",batch="160-army-multihit")
    public void fifteenSimultaneousNormalAndGhostHitsBypassOnlyTheirOwnImmunity(GameTestHelper c){
        var at=floor(c);var list=army(c,at);var victim=target(c,at.add(0,0,2));victim.setDeltaMovement(Vec3.ZERO);victim.invulnerableTime=17;
        for(var e:list)if(!e.general())c.assertTrue(e.doHurtTarget(victim),"Every real small-skeleton hit succeeds in the same tick");
        c.assertTrue(Math.abs(victim.getHealth()-473)<.02,"15 independent hits deal 27 damage, not one 1.8 hit");
        c.assertTrue(victim.invulnerableTime>=17&&victim.getDeltaMovement().lengthSqr()<1e-10,"Preserves existing immunity and has no hit knockback");
        c.assertFalse(victim.hurt(c.getLevel().damageSources().generic(),1),"Unrelated weak hits still respect vanilla immunity");
        for(var e:list)if(!e.general()){e.hurt(c.getLevel().damageSources().generic(),100);c.assertTrue(e.ghost(),"Converted soldier");e.doHurtTarget(victim);}
        c.assertTrue(Math.abs(victim.getHealth()-446)<.03,"Ghosts retain the same independent-hit behavior");
        list.forEach(Entity::discard);victim.discard();c.succeed();
    }
    @GameTest(template="empty",batch="160-ghost-spacing",timeoutTicks=140)
    public void identicalPositionGhostsSeparateWithoutIncomingKnockback(GameTestHelper c){
        var at=floor(c);var list=army(c,at);var soldiers=list.stream().filter(e->!e.general()).toList();
        for(var e:soldiers){e.hurt(c.getLevel().damageSources().generic(),100);e.setPos(at);e.setDeltaMovement(Vec3.ZERO);e.knockback(4,1,0);c.assertTrue(e.getDeltaMovement().lengthSqr()<1e-10,"Incoming impulses remain blocked");}
        c.runAfterDelay(90,()->{
            double nearest=100;for(int i=0;i<soldiers.size();i++)for(int j=i+1;j<soldiers.size();j++)nearest=Math.min(nearest,soldiers.get(i).distanceTo(soldiers.get(j)));
            c.assertTrue(nearest>.25,"Exact stacked ghosts disperse into distinct bodies; minimum separation="+nearest);
            c.assertTrue(soldiers.stream().allMatch(e->e.ghost()&&e.isAlive()&&!e.isPushable()),"Separation does not remove immunity or turn on external pushing");list.forEach(Entity::discard);c.succeed();
        });
    }
    @GameTest(template="empty",batch="160-spirit-bursts")
    public void fireIceAndHealUseDistinctBurstsWithoutFriendlyDamageOrRepeat(GameTestHelper c){
        var at=floor(c);UUID owner=UUID.randomUUID();
        for(var kind:List.of(SpiritElement.FIRE,SpiritElement.ICE,SpiritElement.HEAL)){
            var e=spirit(c,at,kind,owner);var victim=target(c,at.add(.9,0,0));var ally=SpellEngine.summon(c.getLevel(),owner,at.add(-.9,0,0),"skeleton",false);ally.setHealth(1);ally.setNoAi(true);
            e.detonate(victim);c.assertTrue(Math.abs(victim.getHealth()-(500-kind.damage))<.02,"Element damage: "+kind);
            c.assertTrue(ally.getHealth()==(kind==SpiritElement.HEAL?5:1),"Healing and no friendly splash: "+kind);
            c.assertTrue(victim.hasEffect(RoyaleSpells.FROZEN)==(kind==SpiritElement.ICE),"Only ice freezes");
            float hp=victim.getHealth();e.detonate(victim);c.assertTrue(victim.getHealth()==hp,"A spent spirit cannot explode twice");victim.discard();ally.discard();
        }c.succeed();
    }
    @GameTest(template="empty",batch="160-electro-chain",timeoutTicks=80)
    public void electroHitsAtMostNineUniqueEnemies(GameTestHelper c){
        var at=floor(c);var victims=new ArrayList<Mob>();for(int i=0;i<12;i++)victims.add(target(c,at.add((i%4)*1.1,0,(i/4)*1.1)));
        var e=spirit(c,at,SpiritElement.ELECTRO,UUID.randomUUID());e.detonate(victims.getFirst());
        var forced=force(c,at);
        c.assertTrue(victims.stream().filter(v->v.getHealth()<500).count()==1,"Impact damages only the first target, not an instant area burst");
        c.runAfterDelay(4,()->c.assertTrue(victims.stream().filter(v->v.getHealth()<500).count()==1,"No second link before 0.25 seconds"));
        c.runAfterDelay(7,()->c.assertTrue(victims.stream().filter(v->v.getHealth()<500).count()==2,"One new link at the next cadence"));
        c.runAfterDelay(46,()->{
            c.assertTrue(victims.stream().filter(v->v.getHealth()<500).count()==9,"Nine unique victims, including the first victim");
            for(var v:victims)c.assertTrue(v.getHealth()==500||Math.abs(v.getHealth()-493)<.02,"One hit per target");
            c.assertTrue(e.isRemoved(),"Completed chain discards the invisible carrier");victims.forEach(Entity::discard);forced.forEach(cp->c.getLevel().setChunkForced(cp.x,cp.z,false));c.succeed();
        });
    }
    @GameTest(template="empty",batch="164-electro-save")
    public void electricCadenceAndVisitedTargetsSurviveReloadEvenWhenFirstVictimDies(GameTestHelper c){
        var at=floor(c);var first=target(c,at);first.setHealth(1);var next=target(c,at.add(2.5,0,0));
        var e=spirit(c,at.add(0,0,-1),SpiritElement.ELECTRO,UUID.randomUUID());e.detonate(first);c.assertFalse(first.isAlive(),"First weak target died");
        e.tick();e.tick();var n=new CompoundTag();e.saveWithoutId(n);e.discard();
        var reload=RoyaleSpells.ELEMENTAL_SPIRIT.create(c.getLevel());reload.load(n);c.getLevel().addFreshEntity(reload);
        c.assertTrue(reload.chaining()&&reload.chainedTargets()==1&&!reload.isAttackable(),"Saved chain has one visited target and cannot be attacked");
        reload.tick();reload.tick();c.assertTrue(next.getHealth()==500,"Reload preserves the remaining three ticks instead of instantly repeating");
        reload.tick();c.assertTrue(Math.abs(next.getHealth()-493)<.02&&reload.chainedTargets()==2,"Fifth tick links from the dead predecessor without hitting it twice");
        reload.detonate(next);c.assertTrue(Math.abs(next.getHealth()-493)<.02,"No second detonation during a chain");reload.discard();first.discard();next.discard();c.succeed();
    }
    @GameTest(template="empty",batch="164-electro-wall-range")
    public void chainStopsAtWallsAndThreeBlocksInsteadOfJumpingToAllNearbyUnits(GameTestHelper c){
        var at=floor(c);var a=target(c,at);var b=target(c,at.add(3.2,0,0));
        var e=spirit(c,at,SpiritElement.ELECTRO,UUID.randomUUID());e.detonate(a);for(int i=0;i<5;i++)e.tick();
        c.assertTrue(b.getHealth()==500&&e.isRemoved(),"Current original 3-tile range maps to 3 blocks, not old 4");a.discard();b.discard();
        a=target(c,at);b=target(c,at.add(0,0,2.5));e=spirit(c,at,SpiritElement.ELECTRO,UUID.randomUUID());
        var pos=BlockPos.containing(at);for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)c.getLevel().setBlockAndUpdate(pos.offset(x,y,1),Blocks.STONE.defaultBlockState());
        e.detonate(a);for(int i=0;i<5;i++)e.tick();c.assertTrue(b.getHealth()==500&&e.isRemoved(),"Arcs do not transmit damage through solid walls");a.discard();b.discard();c.succeed();
    }
    @GameTest(template="empty",batch="164-army-live",timeoutTicks=340)
    public void generalStaysBehindItsFrontLineAndConvertedSoldiersKeepAttacking(GameTestHelper c){
        var at=floor(c);var forced=force(c,at);var list=ArmyFormation.neutral(c.getLevel(),at,0);var victim=target(c,at.add(0,0,5));
        c.assertTrue(list.size()==16,"Complete live army");var general=list.getFirst();float[] before={500};int[] protectedTicks={0},samples={0};
        for(int t=35;t<=110;t+=5)c.runAfterDelay(t,()->{
            long guards=list.stream().filter(s->!s.general()&&s.distanceToSqr(victim)+.5<general.distanceToSqr(victim)).count();
            samples[0]++;if(guards>=3)protectedTicks[0]++;
        });
        c.runAfterDelay(120,()->{
            c.assertTrue(victim.getHealth()<485,"Real melee goals reached and damaged target, health="+victim.getHealth());
            c.assertTrue(protectedTicks[0]>=samples[0]*.8,"General stays behind at least three guards during approach: "+protectedTicks[0]+"/"+samples[0]);
            before[0]=victim.getHealth();for(var s:list)if(!s.general()){s.hurt(c.getLevel().damageSources().generic(),100);c.assertTrue(s.ghost(),"Live soldier converted during its attack state");}
        });
        c.runAfterDelay(240,()->{
            c.assertTrue(before[0]-victim.getHealth()>50,"Converted army continues real attack windups and deals meaningful damage: "+(before[0]-victim.getHealth()));
            c.assertTrue(list.stream().filter(s->!s.general()&&s.distanceToSqr(victim)<6).count()>=10,"Most ghosts reach the melee ring instead of idling at the rear");
            System.out.println("ROYALE_ARMY_TACTICS guards="+protectedTicks[0]+"/"+samples[0]+" ghostDamage="+(before[0]-victim.getHealth()));
            list.forEach(Entity::discard);victim.discard();forced.forEach(cp->c.getLevel().setChunkForced(cp.x,cp.z,false));c.succeed();
        });
    }
    @GameTest(template="empty",batch="164-ghost-flanking",timeoutTicks=230)
    public void ghostAtRearFlanksBlockedFrontAndStillObeysFreeze(GameTestHelper c){
        var at=floor(c);var forced=force(c,at);var list=army(c,at);var victim=target(c,at.add(0,0,4.4));
        var ghost=list.get(8);for(int i=1;i<list.size();i++)if(list.get(i)!=ghost){var s=list.get(i);s.moveTo(at.add((i%5-2)*.5,0,2+(i/5)*.5));}
        ghost.hurt(c.getLevel().damageSources().generic(),100);ghost.moveTo(at);ghost.setNoAi(false);ghost.setNoGravity(false);
        double[] lateral={0};for(int t=1;t<=180;t++)c.runAfterDelay(t,()->lateral[0]=Math.max(lateral[0],Math.abs(ghost.getX()-at.x)));
        c.runAfterDelay(150,()->{
            c.assertTrue(lateral[0]>.8,"Rear ghost actually uses a side lane, lateral="+lateral[0]);
            c.assertTrue(victim.getHealth()<498,"Ghost's AI gets around stationary front row and strikes; distance="+ghost.distanceTo(victim));
            ghost.addEffect(new net.minecraft.world.effect.MobEffectInstance(RoyaleSpells.STUN,40,0,false,false));ghost.addEffect(new net.minecraft.world.effect.MobEffectInstance(RoyaleSpells.FROZEN,40,0,false,false));
        });
        Vec3[] frozen={Vec3.ZERO};c.runAfterDelay(153,()->frozen[0]=ghost.position());
        c.runAfterDelay(175,()->{
            c.assertTrue(ghost.position().distanceToSqr(frozen[0])<.001,"Neither lane steering nor separation bypasses freeze");
            var n=new CompoundTag();ghost.saveWithoutId(n);var reload=RoyaleSpells.ARMY_SKELETON.create(c.getLevel());reload.load(n);c.assertTrue(reload.formationSlot()==ghost.formationSlot()&&reload.ghost(),"Stable lane and ghost status survive save");reload.discard();
            list.forEach(Entity::discard);victim.discard();forced.forEach(cp->c.getLevel().setChunkForced(cp.x,cp.z,false));c.succeed();
        });
    }
    @GameTest(template="empty",batch="160-spirit-navigation",timeoutTicks=150)
    public void actualAiApproachesJumpsAndBurstsOnce(GameTestHelper c){
        var at=floor(c);var forced=force(c,at);var p=TestPlayers.create(c);p.setPos(at.add(-4,0,0));var victim=target(c,at.add(0,0,5));var e=spirit(c,at,SpiritElement.FIRE,p.getUUID());e.setNoAi(false);SummonOrders.order(p,victim);
        c.succeedWhen(()->{c.assertTrue(e.isRemoved()&&victim.getHealth()<500,"Real navigation and leap reached ordered target; entity ticks="+e.tickCount);c.assertTrue(Math.abs(victim.getHealth()-490)<.02,"One detonation");victim.discard();p.server.getPlayerList().remove(p);p.discard();forced.forEach(chunk->c.getLevel().setChunkForced(chunk.x,chunk.z,false));});
    }
    @GameTest(template="empty",batch="160-spirit-save")
    public void elementOwnerLifetimeAndCommittedLeapSurviveSave(GameTestHelper c){
        var at=floor(c);UUID owner=UUID.randomUUID();var target=target(c,at.add(0,0,2));var e=spirit(c,at,SpiritElement.ICE,owner);e.beginLeap(target);var n=new CompoundTag();e.saveWithoutId(n);e.discard();
        var reload=RoyaleSpells.ELEMENTAL_SPIRIT.create(c.getLevel());reload.load(n);c.assertTrue(reload.element()==SpiritElement.ICE&&owner.equals(reload.ownerId())&&reload.leapTicks()>0,"Persistent element, owner and jump state");var again=new CompoundTag();reload.saveWithoutId(again);c.assertTrue(again.getInt("SpellLife")==n.getInt("SpellLife")&&again.getUUID("LeapTarget").equals(target.getUUID()),"Lifetime and target retained");target.discard();reload.discard();c.succeed();
    }
    @GameTest(template="empty",batch="160-heal-navigation",timeoutTicks=150)
    public void healingSpiritFindsWoundedOwnerWithoutAnEnemy(GameTestHelper c){
        var at=floor(c);var forced=force(c,at);var p=TestPlayers.create(c);p.setPos(at.add(0,0,2));p.setHealth(10);var e=spirit(c,at,SpiritElement.HEAL,p.getUUID());e.setNoAi(false);
        c.succeedWhen(()->{c.assertTrue(e.isRemoved()&&p.getHealth()>=14,"Healing AI jumps to wounded owner with no attack target");p.server.getPlayerList().remove(p);p.discard();forced.forEach(chunk->c.getLevel().setChunkForced(chunk.x,chunk.z,false));});
    }
    @GameTest(template="empty",batch="160-wall")
    public void wallsBlockTheCommittedJumpAndSplash(GameTestHelper c){
        var at=floor(c);var victim=target(c,at.add(0,0,2.7));var e=spirit(c,at,SpiritElement.ICE,UUID.randomUUID());
        var pos=BlockPos.containing(at);for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)c.getLevel().setBlockAndUpdate(pos.offset(x,y,1),Blocks.STONE.defaultBlockState());
        c.assertFalse(e.beginLeap(victim),"Jump cannot start through solid wall");e.detonate(victim);c.assertTrue(victim.getHealth()==500&&!victim.hasEffect(RoyaleSpells.FROZEN),"No damage or freeze through wall");victim.discard();c.succeed();
    }
}
