package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public class InfernoDragonTests {
    private Vec3 floor(GameTestHelper c){var p=c.absolutePos(new BlockPos(7,15,7));for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-1;y<11;y++)c.getLevel().setBlockAndUpdate(p.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState());return Vec3.atBottomCenterOf(p);}
    private Mob dummy(GameTestHelper c,Vec3 at){var e=EntityType.COW.create(c.getLevel());e.moveTo(at);e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);c.getLevel().addFreshEntity(e);return e;}
    private InfernoDragon dragon(GameTestHelper c,Vec3 at,UUID owner){var e=RoyaleSpells.INFERNO_DRAGON.create(c.getLevel());e.setup(owner,4000,false);e.moveTo(at);e.setNoAi(true);c.getLevel().addFreshEntity(e);for(int t=0;t<InfernoDragon.DEPLOY_TICKS;t++)e.tick();return e;}
    private void ticks(InfernoDragon e,int ticks){for(int i=0;i<ticks;i++)e.tick();}
    @GameTest(template="empty",batch="inferno-tiers")
    public void actualBeamDealsEveryPointFourSecondHitAtThreeHeatTiers(GameTestHelper c){
        var at=floor(c);var e=dragon(c,at.add(0,1,0),UUID.randomUUID());var victim=dummy(c,at.add(0,1,2.8));var nearby=dummy(c,at.add(.9,1,2.8));e.setTarget(victim);
        victim.invulnerableTime=17;victim.setDeltaMovement(Vec3.ZERO);
        ticks(e,40);c.assertTrue(Math.abs(victim.getHealth()-495)<.01,"First two seconds: five complete 1-point hits; hp="+victim.getHealth());
        ticks(e,40);c.assertTrue(Math.abs(victim.getHealth()-470.85714)<.02,"Second tier: five complete proportionally buffed hits; hp="+victim.getHealth());
        ticks(e,40);c.assertTrue(Math.abs(victim.getHealth()-407.28571)<.03,"Final tier: five complete proportionally buffed hits; hp="+victim.getHealth());
        c.assertTrue(victim.invulnerableTime>=17,"Existing vanilla hurt immunity remains for unrelated attackers");
        c.assertTrue(victim.getDeltaMovement().lengthSqr()<1e-9,"Beam damage never adds incidental knockback");
        c.assertTrue(nearby.getHealth()==500,"A neighbour is not splash-damaged");e.discard();victim.discard();nearby.discard();c.succeed();
    }
    @GameTest(template="empty",batch="inferno-reset")
    public void SwitchingTargetsStunsFreezeAndKnockbackResetBaseDragonHeat(GameTestHelper c){
        var at=floor(c);var e=dragon(c,at.add(0,1,0),UUID.randomUUID());var a=dummy(c,at.add(0,1,2.8));var b=dummy(c,at.add(.7,1,2.8));e.setTarget(a);ticks(e,88);c.assertTrue(e.heatTicks()>80,"Lock reaches maximum tier");
        e.setTarget(b);e.tick();c.assertTrue(e.heatTicks()==1&&e.beamTargetId()==b.getId(),"A new target begins a new low-power lock");
        e.addEffect(new MobEffectInstance(RoyaleSpells.STUN,30));e.tick();c.assertTrue(e.heatTicks()==0&&e.beamTargetId()==0,"Zap-style stun cancels both heat and beam");e.removeEffect(RoyaleSpells.STUN);
        ticks(e,50);e.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,100));e.tick();c.assertTrue(e.heatTicks()==0,"Freeze resets charge");e.removeEffect(RoyaleSpells.FROZEN);
        ticks(e,50);e.knockback(.4,1,0);c.assertTrue(e.heatTicks()==0,"Explicit knockback interrupts the base card");
        e.discard();a.discard();b.discard();c.succeed();
    }
    @GameTest(template="empty",batch="inferno-electric-reset")
    public void ElectricShockImmediatelyClearsHeatThenRestartsAtTierOne(GameTestHelper c){
        var at=floor(c);var e=dragon(c,at.add(0,3.5,0),UUID.randomUUID());var victim=dummy(c,at.add(0,0,2.8));e.setTarget(victim);
        ticks(e,88);c.assertTrue(e.heatTicks()>80,"Precondition: genuinely charged to tier three");
        SpellEngine.electricStun(e,10);
        c.assertTrue(e.heatTicks()==0&&e.beamTargetId()==0&&e.getTarget()==victim,"Electric effect immediately clears only the beam, retaining AI target");
        float hp=victim.getHealth();Vec3 held=e.position();
        for(int i=0;i<5;i++){e.setDeltaMovement(.1,-.1,.1);e.travel(new Vec3(1,1,1));e.tick();}
        c.assertTrue(e.heatTicks()==0&&victim.getHealth()==hp&&e.position().distanceToSqr(held)<1e-8,"Stunned dragon neither drifts nor damages");
        e.removeEffect(RoyaleSpells.ELECTRICAL_STUN);ticks(e,8);
        c.assertTrue(e.heatTicks()==8&&Math.abs(victim.getHealth()-(hp-1))<.001,"First resumed hit is tier one, with a fresh eight-tick hit clock");
        e.discard();victim.discard();c.succeed();
    }
    @GameTest(template="empty",batch="inferno-occlusion")
    public void WallsAndRangeLossStopDamageImmediately(GameTestHelper c){
        var at=floor(c);var e=dragon(c,at.add(0,1,0),UUID.randomUUID());var victim=dummy(c,at.add(0,1,2.8));e.setTarget(victim);ticks(e,48);float hp=victim.getHealth();
        var base=BlockPos.containing(at);for(int x=-2;x<=2;x++)for(int y=0;y<6;y++)c.getLevel().setBlockAndUpdate(base.offset(x,y,1),Blocks.STONE.defaultBlockState());
        ticks(e,20);c.assertTrue(e.heatTicks()==0&&e.beamTargetId()==0&&victim.getHealth()==hp,"Solid terrain stops all beam damage and synced effects");
        for(int x=-2;x<=2;x++)for(int y=0;y<6;y++)c.getLevel().setBlockAndUpdate(base.offset(x,y,1),Blocks.AIR.defaultBlockState());
        e.tick();c.assertTrue(e.heatTicks()==1,"Reappearing victim starts from zero");victim.setPos(at.add(0,1,8));ticks(e,24);
        c.assertTrue(e.heatTicks()==0&&victim.getHealth()==hp,"Out-of-range targets receive no remote damage");e.discard();victim.discard();c.succeed();
    }
    @GameTest(template="empty",batch="inferno-persistence")
    public void OwnerLifePowerAndCloneTypeSurviveTheNormalSaveAndClonePaths(GameTestHelper c){
        var at=floor(c);UUID owner=UUID.randomUUID();var e=dragon(c,at.add(0,1,0),owner);SpellEngine.empower(e,1.2f);
        var nbt=new CompoundTag();e.saveWithoutId(nbt);var loaded=RoyaleSpells.INFERNO_DRAGON.create(c.getLevel());loaded.load(nbt);
        c.assertTrue(owner.equals(loaded.ownerId())&&Math.abs(loaded.getMaxHealth()-57.6)<.01&&Math.abs(loaded.getAttributeValue(Attributes.ATTACK_DAMAGE)-1.2)<.001,"Ownership and empowered attributes persist");
        c.assertTrue(loaded.heatTicks()==0,"An unloaded beam cannot retain a stale entity-id lock");
        SpellEngine.cloneAllies(c.getLevel(),owner,e.position(),2,1);
        var clones=c.getLevel().getEntitiesOfClass(InfernoDragon.class,e.getBoundingBox().inflate(4),InfernoDragon::isClone);
        c.assertTrue(clones.size()==1&&clones.getFirst().getMaxHealth()==1,"Clone remains a one-health Inferno Dragon, not a barbarian");
        loaded.setup(owner,1,false);loaded.tick();c.assertTrue(loaded.isRemoved(),"Summon expires without drops or a permanent leftover");
        e.discard();clones.forEach(Entity::discard);c.succeed();
    }
    @GameTest(template="empty",batch="inferno-flight",timeoutTicks=220)
    public void FlyingDragonClimbsToAnAirborneEnemyAndIgnoresPeacefulBystanders(GameTestHelper c){
        var at=floor(c);var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var p=new net.minecraft.world.level.ChunkPos(chunk.x+x,chunk.z+z);if(c.getLevel().setChunkForced(p.x,p.z,true))forced.add(p);}
        var owner=TestPlayers.create(c);owner.setPos(at.add(-5,0,-3));var e=dragon(c,at.add(0,1,0),owner.getUUID());e.setNoAi(false);
        var neutral=dummy(c,at.add(1,0,0));var enemy=EntityType.HUSK.create(c.getLevel());enemy.moveTo(at.add(0,6,5));enemy.setNoAi(true);enemy.setNoGravity(true);enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);enemy.setHealth(500);c.getLevel().addFreshEntity(enemy);
        SummonOrders.order(owner,enemy);
        c.runAfterDelay(180,()->{
            c.assertTrue(e.getY()>at.y+3&&enemy.getHealth()<500,"Real aerial navigation climbs and fires at an airborne target; y="+e.getY()+" hp="+enemy.getHealth()+" heat="+e.heatTicks());
            c.assertTrue(neutral.getHealth()==500,"Unprovoked peaceful mobs remain unharmed");
            e.discard();enemy.discard();neutral.discard();owner.server.getPlayerList().remove(owner);owner.discard();forced.forEach(p->c.getLevel().setChunkForced(p.x,p.z,false));c.succeed();
        });
    }
    @GameTest(template="empty",batch="inferno-placement")
    public void DeploymentUsesAnOpenBodyVolumeAndCannotSpawnInsideARoof(GameTestHelper c){
        var at=floor(c);var e=SpellEngine.summon(c.getLevel(),UUID.randomUUID(),at,"inferno_dragon",false);
        c.assertTrue(e instanceof InfernoDragon&&c.getLevel().noCollision(e)&&e.getY()>=at.y+3.5,"Card factory creates a collision-free dragon above iron-golem head height");e.discard();
        var base=BlockPos.containing(at);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<7;y++)c.getLevel().setBlockAndUpdate(base.offset(x,y,z),Blocks.STONE.defaultBlockState());
        c.assertTrue(SpellEngine.summon(c.getLevel(),UUID.randomUUID(),at,"inferno_dragon",false)==null,"A solid filled room rejects deployment");c.succeed();
    }
    @GameTest(template="empty",batch="inferno-rank-bonus")
    public void RequestedMaxRankDamageBonusesScaleIntoEveryRank(GameTestHelper c){
        var at=floor(c);
        // Expected values are the requested old rank-three totals plus 2 / 1,
        // proportionally converted to each rank, including actual hurt calls.
        double[][] expected={{1,4.82857143,12.71428571},{1.2,5.79428571,15.25714286},{1.4,6.76,17.8}};
        for(int rank=0;rank<3;rank++){
            var e=dragon(c,at.add(0,3.5,0),UUID.randomUUID());SpellEngine.empower(e,1+rank*.2f);
            var target=dummy(c,at.add(0,0,3.8));e.setTarget(target);
            for(int tier=0;tier<3;tier++){
                float before=target.getHealth();ticks(e,40);double dealt=before-target.getHealth();
                c.assertTrue(Math.abs(dealt-expected[rank][tier]*5)<.015,"Rank "+(rank+1)+" tier "+tier+" actual damage="+dealt);
            }
            e.discard();target.discard();
        }
        c.succeed();
    }
    @GameTest(template="empty",batch="inferno-hover-ground",timeoutTicks=240)
    public void AILiftsFromGroundAndHoldsHeightFacingAndRangeWhileFiring(GameTestHelper c){
        var at=floor(c);var owner=TestPlayers.create(c);owner.setPos(at.add(-4,0,-3));
        var e=dragon(c,at.add(0,.15,0),owner.getUUID());e.setNoAi(false);
        var enemy=dummy(c,at.add(0,0,3.8));enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000);enemy.setHealth(4000);SummonOrders.order(owner,enemy);
        for(int tick=75;tick<=155;tick+=5){final int sample=tick;
            c.runAfterDelay(tick,()->{
                c.assertTrue(e.getY()>=at.y+3.42,"Active AI maintains at least 3.5 blocks of flight clearance; y="+(e.getY()-at.y));
                double dx=enemy.getX()-e.getX(),dz=enemy.getZ()-e.getZ();float desired=(float)Math.toDegrees(Math.atan2(-dx,dz));
                c.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(e.yBodyRot-desired))<.5,"Locked body keeps facing its actual victim");
                c.assertTrue(e.heatTicks()>0,"Elevated ground target remains in the widened beam range");
                enemy.setPos(at.add(Math.sin(sample*.04)*.35,0,3.8));
            });
        }
        c.runAfterDelay(165,()->{
            c.assertTrue(enemy.getHealth()<3980,"Elevated AI actually damages the ground victim, not just the visual beam");
            c.assertTrue(e.position().subtract(at).horizontalDistance()<.35,"A locked dragon does not drift across the victim");
            enemy.setPos(at.add(0,0,4.1));e.tick();c.assertTrue(e.heatTicks()==0,"Targets outside four horizontal blocks release the lock");
            enemy.setPos(at.add(0,15,3));e.tick();c.assertTrue(e.heatTicks()==0,"Height allowance is bounded and cannot shoot arbitrary distant airborne targets");
            e.discard();enemy.discard();owner.server.getPlayerList().remove(owner);owner.discard();c.succeed();
        });
    }
    @GameTest(template="empty",batch="inferno-hover-air",timeoutTicks=280)
    public void AnAirborneLockDoesNotDescendAndRepeatedlyResetItsHeat(GameTestHelper c){
        var at=floor(c);var owner=TestPlayers.create(c);owner.setPos(at.add(-4,0,-3));
        var e=dragon(c,at.add(0,3.5,0),owner.getUUID());e.setNoAi(false);var enemy=dummy(c,at.add(0,9,3));
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000);enemy.setHealth(4000);SummonOrders.order(owner,enemy);
        c.runAfterDelay(150,()->c.assertTrue(e.heatTicks()>80,"An air lock stays uninterrupted as altitude adjusts"));
        c.runAfterDelay(200,()->{
            c.assertTrue(e.heatTicks()>80&&e.getY()>at.y+7.5,"The dragon holds its aerial attack height instead of sinking to the floor");
            c.assertTrue(enemy.getHealth()<3900,"Aerial high-tier damage keeps applying");
            e.discard();enemy.discard();owner.server.getPlayerList().remove(owner);owner.discard();c.succeed();
        });
    }
}
