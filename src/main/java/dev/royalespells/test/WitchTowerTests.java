// SPDX-License-Identifier: MIT
package dev.royalespells.test;

import dev.royalespells.entity.*;
import dev.royalespells.iron.WitchContent;
import dev.royalespells.iron.TowerWitch;
import dev.royalespells.RoyaleSpells;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class WitchTowerTests {
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void runtimeVariantDoesNotMutateOriginalTemplate(GameTestHelper c){
        var manager=c.getLevel().getStructureManager();
        var original=manager.getOrCreate(ResourceLocation.parse("irons_spellbooks:evoker_fort/tower"));
        var before=original.save(new CompoundTag());
        var id=RoyaleSpells.id("witch_tower/tower");
        var variant=manager.getOrCreate(id);
        c.assertTrue(original!=variant&&variant==manager.getOrCreate(id),"Separate variant is cached per world");
        c.assertTrue(original.save(new CompoundTag()).equals(before),"Original tower NBT unchanged");
        c.assertTrue(c.getLevel().getServer().getResourceManager().getResource(RoyaleSpells.id("structure/witch_tower/tower.nbt")).isEmpty(),"Variant must work without bundled Iron building NBT");
        c.assertTrue(variant.getSize().equals(original.getSize()),"Runtime clone preserves structure size");c.succeed();
    }
    private static TowerWitch witch(GameTestHelper c){
        var at=c.absolutePos(new BlockPos(7,3,7));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)c.getLevel().setBlockAndUpdate(at.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        var w=WitchContent.WITCH.create(c.getLevel());w.setPos(Vec3.atBottomCenterOf(at));w.setNoAi(true);c.getLevel().addFreshEntity(w);return w;
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void residentIsAnIronWizardNotAPlayerSpell(GameTestHelper c){
        var w=witch(c);c.assertTrue(w instanceof NeutralWizard,"Real Iron wizard superclass required");
        var registry=io.redspace.ironsspellbooks.api.registry.SpellRegistry.REGISTRY;
        c.assertFalse(registry.keySet().stream().anyMatch(id->id.getNamespace().equals("royalespells")&&id.getPath().contains("witch")),"Neither Witch variant is a learnable Iron spell or scroll");
        var sheep=EntityType.SHEEP.create(c.getLevel());sheep.setPos(w.position().add(2,0,0));c.getLevel().addFreshEntity(sheep);
        c.assertFalse(w.validEnemy(sheep),"Unprovoked peaceful creature must be safe");
        for(int i=0;i<1900;i++)w.tick();
        c.assertTrue(w.isAlive()&&!w.isRemoved(),"World resident must not expire like a card troop");
        c.assertTrue(w.waveCount()==0,"Idle resident must not accumulate minions");w.discard();sheep.discard();c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void populationCapAndHomePersist(GameTestHelper c){
        var w=witch(c);w.restrictTo(w.blockPosition(),6);
        for(int i=0;i<8;i++)w.summonWave();
        var minions=c.getLevel().getEntitiesOfClass(WitchSkeleton.class,w.getBoundingBox().inflate(30),e->w.getUUID().equals(e.ownerId()));
        c.assertTrue(!minions.isEmpty()&&minions.size()<=12,"Per witch population bounded at twelve");
        CompoundTag save=new CompoundTag();w.saveWithoutId(save);var loaded=WitchContent.WITCH.create(c.getLevel());loaded.load(save);
        c.assertTrue(loaded.hasRestriction()&&loaded.getRestrictCenter().equals(w.getRestrictCenter()),"Roof home must survive reload");
        for(var e:minions)e.discard();w.discard();c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void patrolIsPersistentAndDoesNotAttackItsWitch(GameTestHelper c){
        var w=witch(c);var s=WitchContent.SKELETON.create(c.getLevel());s.setPos(w.position().add(2,0,0));c.getLevel().addFreshEntity(s);s.tickOrders();
        c.assertFalse(s.doHurtTarget(w),"Patrol must protect its faction");
        CompoundTag save=new CompoundTag();s.saveWithoutId(save);var loaded=WitchContent.SKELETON.create(c.getLevel());loaded.load(save);
        c.assertTrue(save.getInt("SpellLife")>100000,"Natural patrol is not a twenty-second graveyard summon");
        c.assertTrue(loaded.getMainHandItem().is(net.minecraft.world.item.Items.STONE_SWORD),"Patrol keeps its sword");
        s.discard();w.discard();c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void originalAndVariantBothRegistered(GameTestHelper c){
        var structures=c.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        c.assertTrue(structures.containsKey(ResourceLocation.parse("irons_spellbooks:evoker_fort")),"Original fort preserved");
        c.assertTrue(structures.containsKey(RoyaleSpells.id("witch_tower")),"Natural Witch tower registered");
        var sets=c.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        c.assertTrue(sets.containsKey(RoyaleSpells.id("witch_tower")),"Witch tower has natural placement");c.succeed();
    }
    @GameTest(template="witch_tower_test",templateNamespace="royalespells",batch="witch-template",timeoutTicks=300)
    public void actualTowerContainsRoofWitchAndSkeletonSpawner(GameTestHelper c){
        var level=c.getLevel();var pos=c.absolutePos(new BlockPos(5,2,5));
        var template=level.getStructureManager().getOrCreate(RoyaleSpells.id("witch_tower/tower"));
        c.assertTrue(template.getSize().getY()==34,"Full upstream tower loaded");
        c.assertTrue(template.placeInWorld(level,pos,pos,new StructurePlaceSettings().setIgnoreEntities(false),level.random,2),"Tower placed");
        var spawner=level.getBlockEntity(pos.offset(13,0,14));c.assertTrue(spawner instanceof SpawnerBlockEntity,"Basement spawner retained");
        var n=spawner.saveWithFullMetadata(level.registryAccess());
        c.assertTrue(n.getCompound("SpawnData").getCompound("entity").getString("id").equals("royalespells:witch_skeleton"),"Vex spawner replaced at source");
        var bounds=new AABB(Vec3.atLowerCornerOf(pos),Vec3.atLowerCornerOf(pos.offset(19,35,19)));
        c.runAfterDelay(3,()->{
            var witches=level.getEntitiesOfClass(TowerWitch.class,bounds);
            c.assertTrue(witches.size()==1&&witches.getFirst().getY()>=pos.getY()+31,"Exactly one Witch on rooftop");
            var guards=level.getEntitiesOfClass(WitchSkeleton.class,bounds.inflate(2));
            c.assertTrue(guards.size()>=4,"Ground and interior skeleton guards present");
            var ground=guards.stream().filter(e->e.getTags().contains("RoyaleWitchGroundPatrol")).toList();
            c.assertTrue(ground.size()==4,"Four distinct courtyard patrols");
            c.assertTrue(ground.stream().allMatch(e->level.noCollision(e,e.getBoundingBox())),"Courtyard spawns must not intersect plinths or walls");
            c.assertTrue(guards.stream().allMatch(e->e.getMainHandItem().is(net.minecraft.world.item.Items.STONE_SWORD)),"Structure patrols retain swords");
            for(var e:witches)e.discard();for(var e:guards)e.discard();c.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void cageTemplatesReplaceVillagersWithBothIllagers(GameTestHelper c){
        var l=c.getLevel();var p=c.absolutePos(new BlockPos(3,3,3));
        for(String name:new String[]{"villager_normal","villager_bible"}){
            var t=l.getStructureManager().getOrCreate(RoyaleSpells.id("witch_tower/villager_spawns/"+name));
            t.placeInWorld(l,p,p,new StructurePlaceSettings().setIgnoreEntities(false),l.random,2);p=p.offset(4,0,0);
        }
        var a=new AABB(Vec3.atLowerCornerOf(c.absolutePos(BlockPos.ZERO)),Vec3.atLowerCornerOf(c.absolutePos(new BlockPos(16,16,16))));
        var mobs=l.getEntitiesOfClass(Mob.class,a);
        c.assertTrue(mobs.stream().anyMatch(e->e.getType()==EntityType.PILLAGER),"Pillager captive");
        c.assertTrue(mobs.stream().anyMatch(e->e.getType()==EntityType.VINDICATOR),"Vindicator captive");
        c.assertFalse(mobs.stream().anyMatch(e->e.getType()==EntityType.VILLAGER),"No old villager captives");
        mobs.forEach(Entity::discard);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void evolvedResidentKeepsFourAuthenticatedSoulReturns(GameTestHelper c){
        var w=witch(c);var n=new CompoundTag();w.saveWithoutId(n);n.putBoolean("WitchEvolved",true);w.load(n);w.summonWave();
        var first=c.getLevel().getEntitiesOfClass(WitchSkeleton.class,w.getBoundingBox().inflate(20),e->w.getUUID().equals(e.ownerId()));
        c.assertTrue(first.size()==4&&w.eligibleSkeletons().size()==4,"Four genuine first-wave skeletons");
        c.assertFalse(w.acceptSoul(java.util.UUID.randomUUID(),java.util.UUID.randomUUID()),"Fake soul cannot heal");
        float before=w.getHealth();
        for(var s:first){s.hurt(c.getLevel().damageSources().genericKill(),1000);WitchBehavior.onSkeletonDeath(s);}
        c.assertTrue(w.pendingSoulCount()==4,"Duplicate death event does not double souls");
        var souls=c.getLevel().getEntitiesOfClass(WitchBolt.class,w.getBoundingBox().inflate(20),b->b.kind()==WitchBolt.HEAL_SOUL);
        for(var b:souls)for(int i=0;i<20;i++)b.tick();
        c.assertTrue(w.healedSouls()==4&&Math.abs(w.getHealth()-before-4*w.soulHealing())<.001,"Actual arriving souls heal once each");
        for(var b:souls)b.discard();w.discard();first.forEach(Entity::discard);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="witch-world")
    public void leftPalmProjectileDamagesEnemyWithoutFriendlySplash(GameTestHelper c){
        var w=witch(c);w.setYRot(0);w.setYBodyRot(0);w.setYHeadRot(0);
        var target=EntityType.IRON_GOLEM.create(c.getLevel());target.setNoAi(true);target.setPos(w.position().add(0,0,4));c.getLevel().addFreshEntity(target);w.setTarget(target);
        var ally=WitchContent.SKELETON.create(c.getLevel());ally.setNoAi(true);ally.setPos(target.position().add(.65,0,0));c.getLevel().addFreshEntity(ally);
        float a=ally.getHealth(),hp=target.getHealth();var b=WitchBolt.launch(c.getLevel(),w,target);
        c.assertTrue(b.position().x>w.getX()&&b.position().z>w.getZ(),"Left-hand release is ahead and on the caster's left");
        for(int i=0;i<15&&!b.impacted();i++)b.tick();
        c.assertTrue(b.impacted()&&target.getHealth()<hp,"Travelling projectile hits real enemy");
        c.assertTrue(ally.getHealth()==a,"No friendly splash on the screeners");
        w.discard();target.discard();ally.discard();b.discard();c.succeed();
    }
}
