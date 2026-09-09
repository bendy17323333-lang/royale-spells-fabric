package dev.mineclash.zappies.test;
import dev.mineclash.zappies.*;
import dev.mineclash.zappies.pause.ElectricPause;
import dev.mineclash.zappies.mixin.MobGoalAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.liziyowo.mineclash.CREntities;
import org.liziyowo.mineclash.entity.mob.Zappies;
import org.liziyowo.mineclash.entity.goal.GenericAttackGoal;
import java.util.*;

@GameTestHolder("zappiesaddon") @PrefixGameTestTemplate(false)
public final class ZappyTests {
    private static void floor(GameTestHelper h){for(int x=0;x<14;x++)for(int z=0;z<14;z++){h.setBlock(x,1,z,Blocks.STONE);for(int y=2;y<6;y++)h.setBlock(x,y,z,Blocks.AIR);}}
    private static Zappies car(GameTestHelper h,float x,float z){var e=h.spawnWithNoFreeWill(CREntities.ZAPPIES.get(),x,2,z);e.setNoGravity(true);ZappyBrain.of(e).configure(11,null,null,0,0);return e;}
    private static Mob dummy(GameTestHelper h,float x,float z){var e=h.spawnWithNoFreeWill(EntityType.COW,x,2,z);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);e.setHealth(1000);return e;}
    private static void finish(GameTestHelper h,Entity...entities){for(var e:entities)e.discard();h.succeed();}
    @GameTest(template="empty") public void levelStatsPreserveOriginalRatios(GameTestHelper h){
        floor(h);var z=car(h,3,3);for(int rank=3;rank<=16;rank++){ZappyBrain.of(z).configure(rank,null,null,0,0);
            h.assertTrue(Math.abs(z.getMaxHealth()-ZappiesConfig.HP[rank-3]*ZappiesConfig.scale())<.001,"Scaled original HP at "+rank);
            h.assertTrue(Math.abs(z.getAttributeValue(Attributes.ATTACK_DAMAGE)-ZappiesConfig.DAMAGE[rank-3]*ZappiesConfig.scale())<.001,"Scaled original damage at "+rank);}
        finish(h,z);
    }
    @GameTest(template="empty") public void squadHasThreeDistinctBodiesAndSavedOwner(GameTestHelper h){
        floor(h);UUID owner=UUID.randomUUID();var list=ZappySquad.spawn(h.getLevel(),Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6,2,6))),0,owner,11);
        h.assertTrue(list.size()==3,"All three cars placed");var team=ZappyBrain.of(list.getFirst()).squad;
        for(int i=0;i<3;i++){var z=list.get(i);var b=ZappyBrain.of(z);h.assertTrue(b.deploy==20+i*2&&team.equals(b.squad)&&owner.equals(b.owner),"Stagger, squad and owner");
            for(int j=0;j<i;j++)h.assertFalse(z.getBoundingBox().intersects(list.get(j).getBoundingBox()),"Physical footprints cannot overlap");
            var tag=new CompoundTag();z.saveWithoutId(tag);var copy=CREntities.ZAPPIES.get().create(h.getLevel());copy.load(tag);
            h.assertTrue(owner.equals(ZappyBrain.of(copy).owner)&&team.equals(ZappyBrain.of(copy).squad)&&ZappyBrain.of(copy).slot==i,"NBT ownership and formation persist");}
        finish(h,list.toArray(Entity[]::new));
    }
    @GameTest(template="empty") public void obstructedSquadDoesNotPartiallySpawn(GameTestHelper h){
        floor(h);var at=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6,2,6)));
        for(int x=3;x<=9;x++)for(int z=3;z<=9;z++)for(int y=2;y<7;y++)h.setBlock(x,y,z,Blocks.STONE);
        h.assertTrue(ZappySquad.spawn(h.getLevel(),at,0,null,11).isEmpty(),"Reject a blocked deployment as one transaction");h.succeed();
    }
    @GameTest(template="empty") public void threeElectricHitsSurviveVanillaImmunityWithoutKnockback(GameTestHelper h){
        floor(h);var a=car(h,4,2);var b=car(h,3,2);var c=car(h,5,2);var target=dummy(h,4,5);Vec3 velocity=target.getDeltaMovement();float before=target.getHealth();
        for(var z:List.of(a,b,c))h.assertTrue(ZappyCombat.shoot(z,target),"Each machine's real hit lands");
        h.assertTrue(Math.abs(before-target.getHealth()-13.5)<.001,"Three independent 4.5 hits");
        h.assertTrue(target.getDeltaMovement().equals(velocity),"Electricity must not apply incidental knockback");
        h.assertTrue(ElectricPause.active(target)&&target.getEffect(ZappiesAddon.ELECTRICAL_STUN).getDuration()==10,"Refresh half-second stun; never add durations");finish(h,a,b,c,target);
    }
    @GameTest(template="empty") public void rangeSightAirAndFriendlyGates(GameTestHelper h){
        floor(h);var z=car(h,3,3);var t=dummy(h,3,7);h.assertTrue(ZappyBrain.of(z).canShoot(t),"Four-block visible range");
        t.setPos(t.position().add(0,3,0));h.assertTrue(ZappyBrain.of(z).canShoot(t),"Air troops in arena range");
        t.setPos(t.position().add(0,0,1));h.assertFalse(ZappyCombat.shoot(z,t),"No hit beyond 4.5 horizontal blocks");
        t.setPos(t.position().add(0,-3,-1));for(int y=2;y<7;y++)h.setBlock(3,y,5,Blocks.STONE);h.assertFalse(ZappyCombat.shoot(z,t),"No hits through a solid wall");
        ZappyBrain.of(z).owner=t.getUUID();h.assertFalse(ZappyCombat.shoot(z,t),"Never hit the owner");finish(h,z,t);
    }
    @GameTest(template="empty") public void firstHitAndFollowupHaveExactCadence(GameTestHelper h){
        floor(h);var z=car(h,3,3);var t=dummy(h,3,6);z.setTarget(t);var b=ZappyBrain.of(z);List<Integer> hits=new ArrayList<>();
        for(int i=0;i<160;i++){int before=b.shots;b.tick();if(b.shots>before)hits.add(i);}
        h.assertTrue(hits.size()>=3&&hits.getFirst()==16,"First discharge after 16 windup ticks: "+hits);
        for(int i=1;i<hits.size();i++)h.assertTrue(hits.get(i)-hits.get(i-1)==ZappiesConfig.interval(),"No hidden native cooldown mixed into the interval: "+hits);finish(h,z,t);
    }
    @GameTest(template="empty") public void shockDoesNotResetZappyWindupOrTarget(GameTestHelper h){
        floor(h);var z=car(h,3,3);var t=dummy(h,3,6);z.setTarget(t);var b=ZappyBrain.of(z);for(int i=0;i<8;i++)b.tick();int progress=b.windup;
        z.addEffect(new MobEffectInstance(ZappiesAddon.ELECTRICAL_STUN,10));for(int i=0;i<10;i++)b.tick();
        h.assertTrue(b.windup==progress&&z.getTarget()==t&&b.shots==0,"Pause the existing charge, preserve target");
        z.removeEffect(ZappiesAddon.ELECTRICAL_STUN);for(int i=progress;i<16;i++)b.tick();h.assertTrue(b.shots==1,"Complete the remaining windup only");finish(h,z,t);
    }
    private static class Windup extends GenericAttackGoal<Mob>{int hits;Windup(Mob m){super(m,10,25,6,10,1);}
        protected void performAttack(LivingEntity t){hits++;}int progress(){return stateTicks;}}
    @GameTest(template="empty",timeoutTicks=100,batch="actual-ground-movement") public void actualNativeNavigationMovesAtMediumSpeed(GameTestHelper h){
        floor(h);var z=h.spawn(CREntities.ZAPPIES.get(),3,2,3);ZappyBrain.of(z).configure(11,null,null,0,0);var t=dummy(h,3,12);
        for(int x=0;x<14;x++)for(int zz=0;zz<14;zz++)h.setBlock(x,21,zz,Blocks.STONE);
        z.setPos(z.position().add(0,20,0));t.setPos(t.position().add(0,20,0));
        var w=h.getLevel();var origin=new net.minecraft.world.level.ChunkPos(z.blockPosition());var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=-1;x<=1;x++)for(int zz=-1;zz<=1;zz++){var cp=new net.minecraft.world.level.ChunkPos(origin.x+x,origin.z+zz);if(w.setChunkForced(cp.x,cp.z,true))forced.add(cp);}
        z.setNoAi(false);z.setNoGravity(false);z.setTarget(t);Vec3 start=z.position();
        h.runAfterDelay(65,()->{double distance=z.position().subtract(start).horizontalDistance();
            h.assertTrue(distance>2&&distance<3.5,"Native movement="+distance+", target="+z.getTarget()+", nav="+z.getNavigation().isDone()+", pos="+z.position());
            for(var cp:forced)w.setChunkForced(cp.x,cp.z,false);
            finish(h,z,t);});
    }
    @GameTest(template="empty",timeoutTicks=100,batch="native-windup") public void realMineClashGoalFreezesThenContinues(GameTestHelper h){
        floor(h);var z=car(h,3,3);var t=dummy(h,3,6);var access=(MobGoalAccess)z;access.zappyGoals().removeAllGoals(g->true);access.zappyTargets().removeAllGoals(g->true);
        var goal=new Windup(z);access.zappyGoals().addGoal(1,goal);z.setNoAi(false);z.setTarget(t);
        h.runAfterDelay(12,()->{h.assertTrue(goal.isPreparing(),"Native goal really entered PRE_ATTACK");int progress=goal.progress();
            z.addEffect(new MobEffectInstance(ZappiesAddon.ELECTRICAL_STUN,10));
            h.runAfterDelay(5,()->h.assertTrue(goal.progress()==progress&&goal.hits==0&&z.getTarget()==t,"Native state counter held without Goal.stop or target loss"));
            h.runAfterDelay(30,()->{h.assertTrue(goal.hits==1,"One resumed hit, not a restarted charge");finish(h,z,t);});});
    }
    @GameTest(template="empty",timeoutTicks=280,batch="actual-three-car-combat") public void everyRearCarReachesFiringRange(GameTestHelper h){
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)h.setBlock(x,21,z,Blocks.STONE);
        var w=h.getLevel();var center=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,22,3)));
        var origin=new net.minecraft.world.level.ChunkPos(BlockPos.containing(center));var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var cp=new net.minecraft.world.level.ChunkPos(origin.x+x,origin.z+z);if(w.setChunkForced(cp.x,cp.z,true))forced.add(cp);}
        var group=ZappySquad.spawn(w,center,35,null,11);h.assertTrue(group.size()==3,"The actual squad deployed");
        var target=dummy(h,5,11);target.setPos(target.position().add(0,20,0));for(var z:group)z.setTarget(target);
        h.runAfterDelay(250,()->{
            for(var z:group)h.assertTrue(ZappyBrain.of(z).shots>=2,"Every car attacks after real pathing; slot="+ZappyBrain.of(z).slot+", distance="+z.distanceTo(target));
            for(var cp:forced)w.setChunkForced(cp.x,cp.z,false);for(var z:group)z.discard();finish(h,target);
        });
    }
    @GameTest(template="empty",timeoutTicks=40,batch="rain-preserved") public void nativeRainStillDealsDamage(GameTestHelper h){
        floor(h);var z=car(h,3,3);var w=h.getLevel();float health=z.getHealth();
        // The test harness places a transparent cap above its structure. Rain
        // uses MOTION_BLOCKING, so put the car above that cap, in actual open sky.
        z.setPos(z.position().add(0,24,0));
        // GameTest's default void biome has no rain. Make this test's local column
        // a real rainy biome, then test the unchanged native exposure path.
        var p=z.blockPosition();
        w.getServer().getCommands().performPrefixedCommand(w.getServer().createCommandSourceStack().withSuppressedOutput(),
            "fillbiome "+(p.getX()-4)+" -64 "+(p.getZ()-4)+" "+(p.getX()+4)+" 319 "+(p.getZ()+4)+" minecraft:plains");
        w.setWeatherParameters(0,200,true,false);w.setRainLevel(1);
        h.runAfterDelay(15,()->{try{h.assertTrue(z.isInWaterRainOrBubble(),"Rain exposure: raining="+w.isRaining()+", sky="+w.canSeeSky(p)+", height="+w.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,p)+", biome="+w.getBiome(p).unwrapKey()+", precipitation="+w.getBiome(p).value().getPrecipitationAt(p));h.assertTrue(z.getHealth()<health,"Native rain self-damage remains");}finally{w.setWeatherParameters(10000,0,false,false);w.setRainLevel(0);z.discard();}h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=40,batch="water-preserved") public void nativeWaterDamageContinuesDuringShock(GameTestHelper h){
        floor(h);var z=car(h,3,3);h.setBlock(3,2,3,Blocks.WATER);float health=z.getHealth();z.addEffect(new MobEffectInstance(ZappiesAddon.ELECTRICAL_STUN,20));
        h.runAfterDelay(15,()->{h.assertTrue(z.getHealth()<health,"Water self-damage is not treated as a paused outgoing attack");finish(h,z);});
    }
}
