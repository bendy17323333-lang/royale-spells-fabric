package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import dev.royalespells.spirit.SpiritElement;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public class StandaloneTests implements FabricGameTest {
    private static Vec3 floor(GameTestHelper h){var at=h.absolutePos(new BlockPos(10,12,10));for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-1;y<=8;y++)h.getLevel().setBlockAndUpdate(at.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState());return Vec3.atBottomCenterOf(at);}
    private static void near(GameTestHelper h,double actual,double expected,String reason){h.assertTrue(Math.abs(actual-expected)<.003,reason+" actual="+actual+" expected="+expected);}
    private static void clean(net.minecraft.server.level.ServerPlayer p){for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof Summoned s&&p.getUUID().equals(s.ownerId()))e.discard();p.server.getPlayerList().remove(p);p.discard();}
    /** Creating terrain does not immediately make its entity sections queryable.
     * All synchronous entity/card fixtures share this gate, including adjacent
     * chunks reached by cloned units and summon offsets. No assertion is retried. */
    static void ready(GameTestHelper h,Vec3 at,Runnable test){
        var center=new net.minecraft.world.level.ChunkPos(BlockPos.containing(at));
        var area=new ArrayList<net.minecraft.world.level.ChunkPos>();var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var chunk=new net.minecraft.world.level.ChunkPos(center.x+x,center.z+z);area.add(chunk);
            if(h.getLevel().setChunkForced(chunk.x,chunk.z,true))forced.add(chunk);
        }
        h.startSequence().thenWaitUntil(()->{
            for(var chunk:area)h.assertTrue(h.getLevel().isPositionEntityTicking(new BlockPos(chunk.getMinBlockX()+8,(int)at.y,chunk.getMinBlockZ()+8)),"Fixture awaits entity-ticking chunk "+chunk);
        }).thenExecute(()->{
            try{test.run();}finally{forced.forEach(chunk->h.getLevel().setChunkForced(chunk.x,chunk.z,false));}
        });
    }
    @GameTest(template="royalespells:empty",batch="fabric-recipe-catalog") public void everyCardHasOneUnambiguousRecipeWithoutMagicDependencies(GameTestHelper h){
        h.assertFalse(FabricLoader.getInstance().isModLoaded("irons_spellbooks"),"Standalone test has no Iron's Spells installed");
        h.assertTrue(RoyaleSpells.ITEMS.size()==28&&RoyaleSpells.TROOP_ITEMS.size()==9,"28 spell cards and nine troop/building cards");
        var p=TestPlayers.create(h);var manager=h.getLevel().getRecipeManager();var input=new TransientCraftingContainer(p.inventoryMenu,3,3);
        var all=new ArrayList<Item>();all.addAll(RoyaleSpells.ITEMS.values());all.addAll(RoyaleSpells.TROOP_ITEMS.values());
        for(var item:all){
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);var recipe=manager.byKey(id).orElseThrow();input.clearContent();int slot=0;
            for(var ingredient:recipe.getIngredients())input.setItem(slot++,ingredient.getItems()[0].copy());
            var matches=manager.getAllRecipesFor(RecipeType.CRAFTING).stream().filter(candidate->candidate.matches(input,h.getLevel())).toList();
            h.assertTrue(matches.size()==1,"Exactly one recipe for "+id+"; matches="+matches.size());h.assertTrue(matches.get(0).assemble(input,h.getLevel().registryAccess()).is(item),"Crafts expected card "+id);
        }clean(p);h.succeed();
    }
    @GameTest(template="royalespells:empty",batch="fabric-spirit-card-cost",timeoutTicks=160) public void spiritCardsSpendElixirAndMirrorKeepsTheirTypeAndLevel(GameTestHelper h){
        var at=floor(h);var p=TestPlayers.create(h);p.setPos(at.add(0,2,-6));p.setYRot(0);p.setXRot(50);p.setNoGravity(true);p.setGameMode(GameType.SURVIVAL);
        // addFreshEntity can succeed before a newly generated chunk's entity
        // section becomes accessible. A same-tick query can miss the real
        // summons (the failing save contained the correct fire levels 11/12).
        // Wait for real tracking; do not weaken identity checks or alter troops.
        ready(h,at,()->{
            try{
                SpellEngine.refill(p);
                h.assertTrue(SpellEngine.deploy(p,TroopCard.FIRE_SPIRIT),"One spirit card deploys");near(h,SpellEngine.elixir(p),9,"Costs one elixir");
                var original=h.getLevel().getEntitiesOfClass(ElementalSpirit.class,p.getBoundingBox().inflate(20),e->p.getUUID().equals(e.ownerId()));
                h.assertTrue(original.size()==1&&original.get(0).element()==SpiritElement.FIRE&&original.get(0).cardLevel()==11,"Initial card creates exactly one owned level-11 fire spirit; found="+original);
                UUID originalId=original.get(0).getUUID();
                for(int i=0;i<10;i++)SpellEngine.tick(p.server);double before=SpellEngine.elixir(p);p.setYRot(35);
                h.assertTrue(SpellEngine.cast(p,Spell.MIRROR),"Mirror copies preceding troop card");near(h,before-SpellEngine.elixir(p),2,"Mirrored one-cost card costs two");
                var spirits=h.getLevel().getEntitiesOfClass(ElementalSpirit.class,p.getBoundingBox().inflate(20),e->p.getUUID().equals(e.ownerId()));
                h.assertTrue(spirits.size()==2&&spirits.stream().allMatch(e->e.element()==SpiritElement.FIRE),"Card identity retained; found="+spirits.stream().map(e->e.getUUID()+":"+e.element()+":L"+e.cardLevel()+"@"+e.position()).toList());
                h.assertTrue(spirits.stream().anyMatch(e->originalId.equals(e.getUUID())&&e.cardLevel()==11)&&spirits.stream().filter(e->e.cardLevel()==12).count()==1,"Mirror retains the original and adds one level-12 spirit");
                var mirrored=spirits.stream().filter(e->e.cardLevel()==12).findFirst().orElseThrow();near(h,mirrored.getMaxHealth(),238/27.0,"Exact level 12 health");
                var n=new CompoundTag();mirrored.saveWithoutId(n);var restored=RoyaleSpells.ELEMENTAL_SPIRIT.create(h.getLevel());restored.load(n);h.assertTrue(restored.cardLevel()==12&&CardBalance.level(restored)==12,"Card level and persistent custom NBT reload");
            }finally{clean(p);}
            h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="fabric-army-card-limit",timeoutTicks=160) public void armyCardEnforcesOneArmyAndPersistentFifteenSecondCooldown(GameTestHelper h){
        var at=floor(h);ready(h,at,()->{var p=TestPlayers.create(h);p.setPos(at.add(0,2,-6));p.setYRot(0);p.setXRot(50);p.setGameMode(GameType.SURVIVAL);SpellEngine.refill(p);
        h.assertTrue(SpellEngine.deploy(p,TroopCard.SKELETON_ARMY_EVOLUTION),"Army card deploys on clear floor");near(h,SpellEngine.elixir(p),7,"Army costs three");
        var troops=h.getLevel().getEntitiesOfClass(ArmySkeleton.class,p.getBoundingBox().inflate(32),e->p.getUUID().equals(e.ownerId()));h.assertTrue(troops.size()==16,"One general and fifteen soldiers");
        for(var e:troops)near(h,e.getMaxHealth(),3,"Every soldier/general has 3 HP");var general=troops.stream().filter(ArmySkeleton::general).findFirst().orElseThrow();near(h,general.shield(),3,"General shield has 3 HP");
        long now=ArmyLedger.now(p.server);var ledger=ArmyLedger.get(p.server);h.assertTrue(ledger.active(p.getUUID(),now)&&ledger.remaining(p.getUUID(),now)==300,"Lease and 15 second cooldown");
        var saved=ledger.save(new CompoundTag());h.assertTrue(ArmyLedger.load(saved).active(p.getUUID(),now),"Lease survives server data serialization");
        h.assertTrue(CardDeployment.deploy(p,TroopCard.SKELETON_ARMY_EVOLUTION,at.add(6,0,0),11)==null,"A second card cannot create another army");
        general.discard();h.assertTrue(CardDeployment.deploy(p,TroopCard.SKELETON_ARMY_EVOLUTION,at.add(6,0,0),11)==null,"General death cannot bypass cooldown");near(h,SpellEngine.elixir(p),7,"Rejected deployment does not spend elixir");
        clean(p);h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="fabric-clone-spirit",timeoutTicks=160) public void clonedSpiritKeepsItsElementWithoutTurningIntoABarbarian(GameTestHelper h){
        var at=floor(h);ready(h,at,()->{var p=TestPlayers.create(h);var original=(ElementalSpirit)CardDeployment.deploy(p,TroopCard.ELECTRO_SPIRIT,at,11);
        SpellEngine.cloneAllies(h.getLevel(),p.getUUID(),at,3,1,12);var copies=h.getLevel().getEntitiesOfClass(ElementalSpirit.class,original.getBoundingBox().inflate(3),e->e.isClone()&&p.getUUID().equals(e.ownerId()));
        h.assertTrue(copies.size()==1&&copies.get(0).element()==SpiritElement.ELECTRO&&copies.get(0).cardLevel()==12,"Element and mirror level retained");near(h,copies.get(0).getMaxHealth(),1,"One-hit clone");
        SpellEngine.cloneAllies(h.getLevel(),p.getUUID(),copies.get(0).position(),.2,1,11);h.assertTrue(h.getLevel().getEntitiesOfClass(ElementalSpirit.class,original.getBoundingBox().inflate(3),e->e.isClone()&&p.getUUID().equals(e.ownerId())).size()==1,"Cannot clone a clone");clean(p);h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="fabric-melee-immunity") public void simultaneousArmyMeleeIgnoresHurtWindowAndAddsNoKnockback(GameTestHelper h){
        var at=floor(h);var units=ArmyFormation.neutral(h.getLevel(),at,0);var victim=EntityType.IRON_GOLEM.create(h.getLevel());victim.setPos(at.add(0,0,3));victim.setNoAi(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);h.getLevel().addFreshEntity(victim);victim.invulnerableTime=19;victim.setDeltaMovement(Vec3.ZERO);
        units.forEach(e->e.setNoAi(true));
        h.assertTrue(units.size()==16,"Complete formation created");
        h.assertFalse(units.get(1).doHurtTarget(victim),"Deployment animation blocks early attacks");
        h.runAfterDelay(22,()->{
            victim.invulnerableTime=19;victim.setDeltaMovement(Vec3.ZERO);victim.setHealth(1000);
            for(var e:units)if(!e.general())h.assertTrue(e.doHurtTarget(victim),"Each concurrent sword hit succeeds after deployment");
            near(h,victim.getHealth(),955,"All fifteen attacks deal 45 damage despite one shared hurt window");near(h,victim.getDeltaMovement().lengthSqr(),0,"Swords do not push victim");units.forEach(Entity::discard);victim.discard();h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="fabric-control-pool") public void freezeAndVinesShareCooldownAndFreezeLastsSeventyTicks(GameTestHelper h){
        var p=TestPlayers.create(h);ControlCooldown.start(p);h.assertTrue(ControlCooldown.remaining(p)==300&&p.getCooldowns().isOnCooldown(RoyaleSpells.ITEMS.get(Spell.FREEZE))&&p.getCooldowns().isOnCooldown(RoyaleSpells.ITEMS.get(Spell.VINES)),"Both cards share one 15s pool");
        h.assertFalse(SpellEngine.cast(p,Spell.VINES),"Vines cannot bypass active freeze cooldown");var fx=SpellEntity.create(h.getLevel(),Spell.FREEZE,p.getUUID(),p.position(),p.position());h.assertTrue(fx.duration()==70,"Card freeze remains 3.5 seconds");clean(p);h.succeed();
    }
}
