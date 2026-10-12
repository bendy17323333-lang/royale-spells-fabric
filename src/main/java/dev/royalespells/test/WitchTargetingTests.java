// SPDX-License-Identifier: MIT
package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** Real target-change events and navigation, including the owner-removal boundary. */
@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public final class WitchTargetingTests {
    private Vec3 arena(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(12,12,12));
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-1;y<=5;y++)
            h.getLevel().setBlockAndUpdate(p.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState());
        return Vec3.atBottomCenterOf(p);
    }
    private <T extends Mob>T spawn(GameTestHelper h,EntityType<T> type,Vec3 at){
        T m=type.create(h.getLevel());m.setPos(at);m.setNoAi(true);m.setPersistenceRequired();
        h.getLevel().addFreshEntity(m);return m;
    }
    private AllySkeleton child(GameTestHelper h,RoyalWitch mother,Vec3 at){
        var s=spawn(h,RoyaleSpells.SKELETON,at);s.setup(mother.ownerId(),1200,false);
        s.getPersistentData().putUUID(SummonOrders.ESCORT,mother.getUUID());return s;
    }
    private void cleanup(GameTestHelper h,Vec3 at){
        for(var e:h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(at,at).inflate(24)))e.discard();
    }
    @GameTest(template="empty",batch="witch-threat-melee",timeoutTicks=100)
    public void meleeChoosesNearestAndRechecksAfterPositionsChange(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.WITCH,at.add(0,0,8));var s=child(h,w,at.add(0,0,2));
            var enemy=spawn(h,EntityType.HUSK,at);enemy.setTarget(w);
            h.assertTrue(enemy.getTarget()==s,"Melee enemy must attack the nearer skeleton, not prefer the witch");
            s.setPos(at.add(0,0,9));w.setPos(at.add(0,0,3));
            enemy.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);enemy.setNoAi(false);
            h.runAfterDelay(15,()->{h.assertTrue(enemy.getTarget()==w,"A moving front line must be re-evaluated without a new hit");cleanup(h,at);h.succeed();});
        });
    }
    @GameTest(template="empty",batch="witch-threat-ranged",timeoutTicks=100)
    public void archerKeepsFirstVisibleTargetUntilItDies(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.WITCH,at.add(0,0,7));var s=child(h,w,at.add(0,0,3));
            var archer=spawn(h,EntityType.SKELETON,at);archer.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
            archer.setTarget(w);archer.setLastHurtByMob(s);archer.setTarget(s);
            h.assertTrue(archer.getTarget()==w,"A closer skeleton's retaliation must not steal the archer's first lock");
            w.discard();archer.setTarget(s);h.assertTrue(archer.getTarget()==s,"Death releases the first lock");
            cleanup(h,at);h.succeed();
        });
    }
    @GameTest(template="empty",batch="witch-threat-weapon",timeoutTicks=100)
    public void swordSkeletonIsMeleeDespiteInheritedRangedInterface(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.WITCH,at.add(0,0,7));var s=child(h,w,at.add(0,0,3));
            var enemy=spawn(h,RoyaleSpells.SKELETON,at);enemy.setup(java.util.UUID.randomUUID(),1200,false);
            enemy.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));enemy.setTarget(w);
            h.assertTrue(enemy.getTarget()==s,"Our sword skeleton inherits RangedAttackMob but fights in melee");
            cleanup(h,at);h.succeed();
        });
    }
    @GameTest(template="empty",batch="witch-threat-visibility",timeoutTicks=100)
    public void hiddenSkeletonDoesNotStealTargetAndIdleAnimalsStayIdle(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.WITCH,at.add(5,0,0));var s=child(h,w,at.add(0,0,3));
            var enemy=spawn(h,EntityType.HUSK,at);var cow=spawn(h,EntityType.COW,at.add(-3,0,0));
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(at).offset(x,y,1),Blocks.STONE.defaultBlockState());
            enemy.setTarget(w);h.assertTrue(enemy.getTarget()==w,"Cannot select a closer skeleton through a wall");
            h.runAfterDelay(15,()->{h.assertTrue(cow.getTarget()==null,"The policy must not provoke peaceful mobs");cleanup(h,at);h.succeed();});
        });
    }
    @GameTest(template="empty",batch="witch-orphan-combat",timeoutTicks=240)
    public void eggWitchDeathDoesNotStopPursuitOrReacquisition(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.EVOLVED_WITCH,at.add(-4,0,0));var s=child(h,w,at);
            var first=spawn(h,EntityType.HUSK,at.add(0,0,5));first.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);first.setHealth(200);
            s.setNoAi(false);s.setTarget(first);w.hurt(h.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
            h.runAfterDelay(70,()->{
                h.assertTrue(w.isRemoved(),"Test must cross real death removal, not just a missing target");
                h.assertTrue(s.getTarget()==first&&first.getHealth()<200,"Orphan must reach and damage its retained enemy");
                first.discard();var second=spawn(h,EntityType.HUSK,s.position().add(0,0,4));second.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);second.setHealth(200);
                h.runAfterDelay(70,()->{h.assertTrue(s.getTarget()==second&&second.getHealth()<200,"Orphan must find and hit a new enemy after its first dies");cleanup(h,at);h.succeed();});
            });
        });
    }
    @GameTest(template="empty",batch="witch-orphan-save",timeoutTicks=100)
    public void reloadedOrphanKeepsFactionAndDoesNotHuntAnimals(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.WITCH,at);var s=child(h,w,at.add(0,0,2));var sibling=child(h,w,at.add(1,0,2));
            var save=new CompoundTag();s.saveWithoutId(save);s.discard();w.discard();
            var loaded=RoyaleSpells.SKELETON.create(h.getLevel());loaded.load(save);h.getLevel().addFreshEntity(loaded);
            var cow=spawn(h,EntityType.COW,at.add(1,0,3));var hostile=spawn(h,EntityType.HUSK,at.add(0,0,7));
            loaded.tickCount=10;SummonOrders.tick(loaded,loaded.ownerId());
            h.assertTrue(loaded.getTarget()==hostile,"Saved orphan must independently acquire a hostile");
            h.assertTrue(SpellEngine.friendly(loaded.ownerId(),sibling),"Keep the dead mother's UUID for faction membership");
            hostile.discard();loaded.tickCount+=5;SummonOrders.tick(loaded,loaded.ownerId());
            h.assertTrue(loaded.getTarget()==null,"Neither a sibling nor a peaceful cow is a fallback enemy");
            cleanup(h,at);h.succeed();
        });
    }
    @GameTest(template="empty",batch="witch-threat-player-order",timeoutTicks=100)
    public void explicitPlayerOrderOverridesNearestEnemyAndSurvivesMotherDeath(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var p=TestPlayers.create(h);p.setPos(at.add(-7,0,0));
            var own=spawn(h,RoyaleSpells.WITCH,at.add(-5,0,0));own.configure(p.getUUID(),false,11);
            var s=child(h,own,at);var other=spawn(h,RoyaleSpells.WITCH,at.add(0,0,7));var shield=child(h,other,at.add(0,0,2));
            SummonOrders.order(p,other);s.setTarget(shield);
            h.assertTrue(s.getTarget()==other,"An explicit player order outranks the nearest-enemy policy");
            own.discard();s.tickCount=10;SummonOrders.tick(s,p.getUUID());
            h.assertTrue(s.getTarget()==other&&s.ownerId().equals(p.getUUID()),"Losing the mother must not lose the player's ownership/order");
            cleanup(h,at);p.server.getPlayerList().remove(p);p.discard();h.succeed();
        });
    }
    @GameTest(template="empty",batch="witch-threat-sight-grace",timeoutTicks=160)
    public void rangedLockToleratesBriefOcclusionButReleasesLostEnemy(GameTestHelper h){
        var at=arena(h);h.runAfterDelay(5,()->{
            var w=spawn(h,RoyaleSpells.WITCH,at.add(0,0,7));var s=child(h,w,at.add(4,0,0));
            var archer=spawn(h,EntityType.SKELETON,at);archer.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));archer.setTarget(w);
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(at).offset(x,y,2),Blocks.STONE.defaultBlockState());
            archer.setTarget(s);h.assertTrue(archer.getTarget()==w,"A brief occlusion must not steal a valid ranged lock");
            h.runAfterDelay(70,()->{archer.setTarget(s);h.assertTrue(archer.getTarget()==s,"An enemy hidden for over three seconds releases the lock");cleanup(h,at);h.succeed();});
        });
    }
}
