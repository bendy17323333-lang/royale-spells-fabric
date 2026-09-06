package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import java.util.*;

public class SpellGameTests implements FabricGameTest {
    private static Vec3d pos(TestContext c){return Vec3d.ofBottomCenter(c.getAbsolutePos(new BlockPos(2,2,2)));}
    private static SpellEntity cast(TestContext c,Spell spell,UUID owner,Vec3d at) {
        var e=SpellEntity.create(c.getWorld(),spell,owner,at.add(0,0,-4),at);c.getWorld().spawnEntity(e);return e;
    }
    private static void finish(TestContext c,UUID owner,Entity... targets) {
        for(Entity e:targets)e.discard();
        var doomed=new ArrayList<Entity>();
        for(Entity e:c.getWorld().iterateEntities())if(e instanceof Summoned s && owner.equals(s.ownerId()) || e instanceof SpellEntity fx && owner.equals(fx.ownerId))doomed.add(e);
        doomed.forEach(Entity::discard);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void graveyardSkeletonHasStoneSwordAndSixMaxHealth(TestContext c) {
        UUID owner=UUID.randomUUID();var skeleton=SpellEngine.summon(c.getWorld(),owner,pos(c),"skeleton",false);
        c.assertTrue(skeleton instanceof AllySkeleton,"Must be a skeleton");
        c.assertTrue(skeleton.getMaxHealth()==6 && skeleton.getHealth()==6,"Maximum and current health must both be 6");
        c.assertTrue(skeleton.getMainHandStack().isOf(Items.STONE_SWORD),"Must hold stone sword");
        NbtCompound nbt=new NbtCompound();skeleton.writeNbt(nbt);
        var loaded=RoyaleSpells.SKELETON.create(c.getWorld());loaded.readNbt(nbt);
        c.assertTrue(loaded.getMaxHealth()==6 && owner.equals(loaded.ownerId()),"Reload must preserve owner and max health");finish(c,owner,skeleton);
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void summonedZombiesAreBabiesAndAllied(TestContext c) {
        UUID owner=UUID.randomUUID();var zombie=SpellEngine.summon(c.getWorld(),owner,pos(c),"zombie",false);
        c.assertTrue(zombie.isBaby(),"Goblin replacement must be a baby zombie");
        c.assertTrue(SpellEngine.friendly(owner,zombie),"Owner must regard summon as friendly");
        c.assertFalse(SpellEngine.enemy(owner,zombie),"Friendly summon cannot be selected for damage");
        float before=zombie.getHealth();SpellEngine.hit(c.getWorld(),owner,zombie,100);
        c.assertTrue(zombie.getHealth()==before,"Owner spells must not damage summon");finish(c,owner,zombie);
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=60)
    public void evolvedZapHitsTwice(TestContext c) {
        UUID owner=UUID.randomUUID();var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);target.setNoGravity(true);
        float before=target.getHealth();cast(c,Spell.ZAP_EVOLUTION,owner,target.getPos());
        c.waitAndRun(25,()->{c.assertTrue(Math.abs(target.getHealth()-(before-8))<0.01,"Evolved Zap must deliver two 4-damage hits");finish(c,owner,target);});
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=60)
    public void arrowVolleysAllApply(TestContext c) {
        UUID owner=UUID.randomUUID();var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);target.setNoGravity(true);
        float before=target.getHealth();cast(c,Spell.ARROWS,owner,target.getPos());
        c.waitAndRun(25,()->{c.assertTrue(Math.abs(target.getHealth()-(before-9))<0.01,"All three Arrows volleys must apply");finish(c,owner,target);});
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=60)
    public void lightningSelectsThreeHighestHealthTargets(TestContext c) {
        UUID owner=UUID.randomUUID();var mobs=new ArrayList<MobEntity>();
        for(int i=0;i<4;i++){var m=c.spawnMob(EntityType.IRON_GOLEM,1+i*.3f,2,2);m.setAiDisabled(true);m.setNoGravity(true);m.setHealth(40+i*15);mobs.add(m);}
        cast(c,Spell.LIGHTNING,owner,mobs.get(1).getPos());
        c.waitAndRun(25,()->{
            c.assertTrue(mobs.get(0).getHealth()==40,"Lowest health target must remain unharmed");
            for(int i=1;i<4;i++)c.assertTrue(Math.abs(mobs.get(i).getHealth()-(40+i*15-22))<.01,"Exactly highest three receive 22 damage");
            finish(c,owner,mobs.toArray(Entity[]::new));
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120)
    public void freezeBlocksMovementAndExpires(TestContext c) {
        UUID owner=UUID.randomUUID();var target=c.spawnMob(EntityType.HUSK,2,2,2);target.setNoGravity(true);
        SpellEngine.stun(target,15);Vec3d before=target.getPos();target.setVelocity(1,0,0);
        c.waitAndRun(8,()->{c.assertTrue(target.getPos().squaredDistanceTo(before)<.1,"Frozen mob must not move");});
        c.waitAndRun(25,()->{c.assertFalse(target.hasStatusEffect(RoyaleSpells.STUN),"Stun must expire");c.assertFalse(target.isAiDisabled(),"Native AI flag remains intact");finish(c,owner,target);});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void curseDeathCreatesOwnedBabyZombie(TestContext c) {
        UUID owner=UUID.randomUUID();var victim=c.spawnMob(EntityType.COW,2,2,2);
        SpellEngine.curse(owner,victim);SpellEngine.hit(c.getWorld(),owner,victim,100);
        var zombies=c.getWorld().getEntitiesByClass(AllyZombie.class,new Box(victim.getPos().add(-3,-3,-3),victim.getPos().add(3,4,3)),e->owner.equals(e.ownerId()));
        c.assertTrue(zombies.size()==1 && zombies.get(0).isBaby(),"A cursed death creates exactly one owned baby zombie");finish(c,owner,victim);
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void cloneHasOneHealthAndCannotCloneAgain(TestContext c) {
        UUID owner=UUID.randomUUID();var original=SpellEngine.summon(c.getWorld(),owner,pos(c),"skeleton",false);
        SpellEngine.cloneAllies(c.getWorld(),owner,original.getPos(),4);
        var clones=c.getWorld().getEntitiesByClass(AllySkeleton.class,original.getBoundingBox().expand(5),e->owner.equals(e.ownerId()) && e.isClone());
        c.assertTrue(clones.size()==1 && clones.get(0).getMaxHealth()==1,"Clone has 1 maximum health");
        original.discard();SpellEngine.cloneAllies(c.getWorld(),owner,clones.get(0).getPos(),4);
        c.assertTrue(c.getWorld().getEntitiesByClass(AllySkeleton.class,clones.get(0).getBoundingBox().expand(5),e->owner.equals(e.ownerId())).size()==1,"Cannot recursively clone clones");finish(c,owner);
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
    public void evolvedSnowballCarriesAndReleases(TestContext c) {
        UUID owner=UUID.randomUUID();var target=c.spawnMob(EntityType.IRON_GOLEM,2,3,2);target.setAiDisabled(true);target.setNoGravity(true);
        Vec3d before=target.getPos();cast(c,Spell.GIANT_SNOWBALL_EVOLUTION,owner,before);
        c.waitAndRun(46,()->{c.assertTrue(target.getZ()>before.z+2.5,"Snowball carries target forward");c.assertFalse(target.hasStatusEffect(RoyaleSpells.STUN),"Snowball releases target");finish(c,owner,target);});
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=240)
    public void graveyardSpawnsOverTimeAndCleansEffect(TestContext c) {
        UUID owner=UUID.randomUUID();var effect=cast(c,Spell.GRAVEYARD,owner,pos(c));
        c.waitAndRun(205,()->{
            var units=c.getWorld().getEntitiesByClass(AllySkeleton.class,new Box(pos(c).add(-12,-20,-12),pos(c).add(12,10,12)),e->owner.equals(e.ownerId()));
            c.assertTrue(!units.isEmpty(),"Graveyard creates real skeletons");
            for(var unit:units)c.assertTrue(unit.getMaxHealth()==6 && unit.getMainHandStack().isOf(Items.STONE_SWORD),"Every summon uses requested stats");
            c.assertTrue(effect.isRemoved(),"Finished field removed");finish(c,owner);
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void effectNbtPreservesFlightAndOwnership(TestContext c) {
        UUID owner=UUID.randomUUID();var original=SpellEntity.create(c.getWorld(),Spell.GOBLIN_BARREL_EVOLUTION,owner,pos(c),pos(c).add(2,0,4));original.decoy=true;
        NbtCompound nbt=new NbtCompound();original.writeNbt(nbt);var loaded=RoyaleSpells.SPELL.create(c.getWorld());loaded.readNbt(nbt);
        c.assertTrue(loaded.spell()==Spell.GOBLIN_BARREL_EVOLUTION && loaded.decoy && owner.equals(loaded.ownerId),"Save preserves evolution, decoy and owner");
        c.assertTrue(loaded.target().squaredDistanceTo(original.target())<1e-8,"Target preserves precision");finish(c,owner);
    }
}
