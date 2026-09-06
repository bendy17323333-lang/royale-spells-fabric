package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

@GameTestHolder("royalespells")
@PrefixGameTestTemplate(false)
public class SpellGameTests {
    private static final String EMPTY_STRUCTURE="empty";
    private static Vec3 pos(GameTestHelper c){return Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(2,2,2)));}
    private static SpellEntity cast(GameTestHelper c,Spell spell,UUID owner,Vec3 at) {
        var e=SpellEntity.create(c.getLevel(),spell,owner,at.add(0,0,-4),at);c.getLevel().addFreshEntity(e);return e;
    }
    private static void finish(GameTestHelper c,UUID owner,Entity... targets) {
        for(Entity e:targets)e.discard();
        var doomed=new ArrayList<Entity>();
        for(Entity e:c.getLevel().getAllEntities())if(e instanceof Summoned s && owner.equals(s.ownerId()) || e instanceof SpellEntity fx && owner.equals(fx.ownerId))doomed.add(e);
        doomed.forEach(Entity::discard);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE)
    public void graveyardSkeletonHasStoneSwordAndSixMaxHealth(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var skeleton=SpellEngine.summon(c.getLevel(),owner,pos(c),"skeleton",false);
        c.assertTrue(skeleton instanceof AllySkeleton,"Must be a skeleton");
        c.assertTrue(skeleton.getMaxHealth()==6 && skeleton.getHealth()==6,"Maximum and current health must both be 6");
        c.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD),"Must hold stone sword");
        CompoundTag nbt=new CompoundTag();skeleton.saveWithoutId(nbt);
        var loaded=RoyaleSpells.SKELETON.create(c.getLevel());loaded.load(nbt);
        c.assertTrue(loaded.getMaxHealth()==6 && owner.equals(loaded.ownerId()),"Reload must preserve owner and max health");finish(c,owner,skeleton);
    }
    @GameTest(template=EMPTY_STRUCTURE)
    public void summonedZombiesAreBabiesAndAllied(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var zombie=SpellEngine.summon(c.getLevel(),owner,pos(c),"zombie",false);
        c.assertTrue(zombie.isBaby(),"Goblin replacement must be a baby zombie");
        c.assertTrue(SpellEngine.friendly(owner,zombie),"Owner must regard summon as friendly");
        c.assertFalse(SpellEngine.enemy(owner,zombie),"Friendly summon cannot be selected for damage");
        float before=zombie.getHealth();SpellEngine.hit(c.getLevel(),owner,zombie,100);
        c.assertTrue(zombie.getHealth()==before,"Owner spells must not damage summon");finish(c,owner,zombie);
    }
    @GameTest(template=EMPTY_STRUCTURE,timeoutTicks=60)
    public void evolvedZapHitsTwice(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);target.setNoGravity(true);
        float before=target.getHealth();cast(c,Spell.ZAP_EVOLUTION,owner,target.position());
        c.runAfterDelay(25,()->{c.assertTrue(Math.abs(target.getHealth()-(before-8))<0.01,"Evolved Zap must deliver two 4-damage hits");finish(c,owner,target);});
    }
    @GameTest(template=EMPTY_STRUCTURE,timeoutTicks=60)
    public void arrowVolleysAllApply(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);target.setNoGravity(true);
        float before=target.getHealth();cast(c,Spell.ARROWS,owner,target.position());
        c.runAfterDelay(25,()->{c.assertTrue(Math.abs(target.getHealth()-(before-9))<0.01,"All three Arrows volleys must apply");finish(c,owner,target);});
    }
    @GameTest(template=EMPTY_STRUCTURE,timeoutTicks=60)
    public void lightningSelectsThreeHighestHealthTargets(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var mobs=new ArrayList<Mob>();
        for(int i=0;i<4;i++){var m=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,1+i*.3f,2,2);m.setNoAi(true);m.setNoGravity(true);m.setHealth(40+i*15);mobs.add(m);}
        cast(c,Spell.LIGHTNING,owner,mobs.get(1).position());
        c.runAfterDelay(25,()->{
            c.assertTrue(mobs.get(0).getHealth()==40,"Lowest health target must remain unharmed");
            for(int i=1;i<4;i++)c.assertTrue(Math.abs(mobs.get(i).getHealth()-(40+i*15-22))<.01,"Exactly highest three receive 22 damage");
            finish(c,owner,mobs.toArray(Entity[]::new));
        });
    }
    @GameTest(template=EMPTY_STRUCTURE,timeoutTicks=120)
    public void freezeBlocksMovementAndExpires(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var target=c.spawnWithNoFreeWill(EntityType.HUSK,2,2,2);target.setNoGravity(true);
        SpellEngine.stun(target,15);Vec3 before=target.position();target.setDeltaMovement(1,0,0);
        c.runAfterDelay(8,()->{c.assertTrue(target.position().distanceToSqr(before)<.1,"Frozen mob must not move");});
        c.runAfterDelay(25,()->{c.assertFalse(target.hasEffect(RoyaleSpells.STUN),"Stun must expire");c.assertFalse(target.isNoAi(),"Native AI flag remains intact");finish(c,owner,target);});
    }
    @GameTest(template=EMPTY_STRUCTURE)
    public void curseDeathCreatesOwnedBabyZombie(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var victim=c.spawnWithNoFreeWill(EntityType.COW,2,2,2);
        SpellEngine.curse(owner,victim);SpellEngine.hit(c.getLevel(),owner,victim,100);
        var zombies=c.getLevel().getEntitiesOfClass(AllyZombie.class,new AABB(victim.position().add(-3,-3,-3),victim.position().add(3,4,3)),e->owner.equals(e.ownerId()));
        c.assertTrue(zombies.size()==1 && zombies.get(0).isBaby(),"A cursed death creates exactly one owned baby zombie");finish(c,owner,victim);
    }
    @GameTest(template=EMPTY_STRUCTURE)
    public void cloneHasOneHealthAndCannotCloneAgain(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var original=SpellEngine.summon(c.getLevel(),owner,pos(c),"skeleton",false);
        SpellEngine.cloneAllies(c.getLevel(),owner,original.position(),4);
        var clones=c.getLevel().getEntitiesOfClass(AllySkeleton.class,original.getBoundingBox().inflate(5),e->owner.equals(e.ownerId()) && e.isClone());
        c.assertTrue(clones.size()==1 && clones.get(0).getMaxHealth()==1,"Clone has 1 maximum health");
        original.discard();SpellEngine.cloneAllies(c.getLevel(),owner,clones.get(0).position(),4);
        c.assertTrue(c.getLevel().getEntitiesOfClass(AllySkeleton.class,clones.get(0).getBoundingBox().inflate(5),e->owner.equals(e.ownerId())).size()==1,"Cannot recursively clone clones");finish(c,owner);
    }
    @GameTest(template=EMPTY_STRUCTURE,timeoutTicks=80)
    public void evolvedSnowballCarriesAndReleases(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,3,2);target.setNoAi(true);target.setNoGravity(true);
        Vec3 before=target.position();cast(c,Spell.GIANT_SNOWBALL_EVOLUTION,owner,before);
        c.runAfterDelay(46,()->{c.assertTrue(target.getZ()>before.z+2.5,"Snowball carries target forward");c.assertFalse(target.hasEffect(RoyaleSpells.STUN),"Snowball releases target");finish(c,owner,target);});
    }
    @GameTest(template=EMPTY_STRUCTURE,timeoutTicks=240)
    public void graveyardSpawnsOverTimeAndCleansEffect(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var effect=cast(c,Spell.GRAVEYARD,owner,pos(c));
        c.runAfterDelay(205,()->{
            var units=c.getLevel().getEntitiesOfClass(AllySkeleton.class,new AABB(pos(c).add(-12,-20,-12),pos(c).add(12,10,12)),e->owner.equals(e.ownerId()));
            c.assertTrue(!units.isEmpty(),"Graveyard creates real skeletons");
            for(var unit:units)c.assertTrue(unit.getMaxHealth()==6 && unit.getMainHandItem().is(Items.STONE_SWORD),"Every summon uses requested stats");
            c.assertTrue(effect.isRemoved(),"Finished field removed");finish(c,owner);
        });
    }
    @GameTest(template=EMPTY_STRUCTURE)
    public void effectNbtPreservesFlightAndOwnership(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var original=SpellEntity.create(c.getLevel(),Spell.GOBLIN_BARREL_EVOLUTION,owner,pos(c),pos(c).add(2,0,4));original.decoy=true;
        CompoundTag nbt=new CompoundTag();original.saveWithoutId(nbt);var loaded=RoyaleSpells.SPELL.create(c.getLevel());loaded.load(nbt);
        c.assertTrue(loaded.spell()==Spell.GOBLIN_BARREL_EVOLUTION && loaded.decoy && owner.equals(loaded.ownerId),"Save preserves evolution, decoy and owner");
        c.assertTrue(loaded.target().distanceToSqr(original.target())<1e-8,"Target preserves precision");finish(c,owner);
    }
}
