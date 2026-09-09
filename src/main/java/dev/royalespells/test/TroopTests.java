package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

@GameTestHolder("royalespells")
@PrefixGameTestTemplate(false)
public class TroopTests {
    private static final String EMPTY_STRUCTURE="empty";
    private Vec3 at(GameTestHelper c){return Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(2,2,2)));}
    // 1.21's empty-template enclosure is too small for the 3.2-block-wide hut.
    private Vec3 hutFloor(GameTestHelper c){
        var floor=BlockPos.containing(at(c)).above(11);
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=0;y<=9;y++)
            c.getLevel().setBlockAndUpdate(floor.offset(x,y,z),(y==0?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
        return Vec3.atBottomCenterOf(floor.above());
    }
    private List<Mob> units(GameTestHelper c,UUID owner){return c.getLevel().getEntitiesOfClass(Mob.class,new AABB(at(c).add(-40,-10,-40),at(c).add(40,20,40)),e->e instanceof Summoned s&&owner.equals(s.ownerId())&&e.isAlive());}
    private void cleanup(GameTestHelper c,UUID owner){for(var e:com.google.common.collect.ImmutableList.copyOf(c.getLevel().getAllEntities()))if(e instanceof Summoned s&&owner.equals(s.ownerId()))e.discard();}
    @GameTest(template=EMPTY_STRUCTURE,batch="retained-cards")
    public void retainedCardsAndOriginalSkeleton(GameTestHelper c){
        c.assertTrue(RoyaleSpells.TROOP_ITEMS.keySet().equals(EnumSet.of(TroopCard.BARBARIAN_HUT,TroopCard.INFERNO_DRAGON)),"Hut and newly requested Inferno Dragon are the two retained troop cards");
        c.assertTrue(c.getLevel().getRecipeManager().byKey(RoyaleSpells.id("barbarian_hut")).isPresent()!=IronSpellSystem.loaded,"Hut uses native scroll progression when Iron's is installed");
        for(String name:List.of("golem","night_witch","golemite","royale_bat","bat")){
            c.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(RoyaleSpells.id(name)),"Removed troop entity: "+name);
            c.assertTrue(!BuiltInRegistries.ITEM.containsKey(RoyaleSpells.id(name)),"Removed troop card: "+name);
            c.assertTrue(SpellEngine.summon(c.getLevel(),null,at(c),name,false)==null,"Removed troop must not fall back to a zombie");
        }
        UUID owner=UUID.randomUUID();var skeleton=SpellEngine.summon(c.getLevel(),owner,at(c),"skeleton",false);
        c.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD),"Skeleton still uses a real stone sword");
        c.assertTrue(Math.abs(skeleton.getBbHeight()-1.4)<.001&&skeleton.getMaxHealth()==6,"Smaller vanilla skeleton hitbox with the existing low health");
        c.assertTrue(skeleton.getAttributeValue(Attributes.ATTACK_DAMAGE)==1.5,"Keep weakened damage after model rollback");
        var barbarian=(AllyZombie)SpellEngine.summon(c.getLevel(),owner,at(c).add(3,0,0),"barbarian",false);
        c.assertTrue(barbarian.getMainHandItem().is(Items.IRON_SWORD),"Barbarian has real equipment for the held-item renderer");
        barbarian.tick();var nbt=new CompoundTag();barbarian.saveWithoutId(nbt);
        c.assertTrue(nbt.getBoolean("DeploymentPlayed"),"Persist deploy playback so loading does not replay it");
        cleanup(c,owner);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="hut-spawner")
    public void hutProducesWavesReloadsAndExpiresOnce(GameTestHelper c){
        UUID owner=UUID.randomUUID();var hut=(RoyaleUnit)SpellEngine.summon(c.getLevel(),owner,hutFloor(c).add(0,3,0),"barbarian_hut",false);c.assertTrue(hut!=null,"Hut fixture must have space");hut.lockFacing(123);hut.setNoGravity(true);Vec3 initial=hut.position();hut.setDeltaMovement(2,2,2);
        for(int i=0;i<302;i++)hut.tick();
        c.assertTrue(units(c,owner).stream().filter(e->e.getType()==RoyaleSpells.BARBARIAN).count()==6,"Two waves of three barbarians");
        c.assertTrue(hut.position().distanceTo(initial)<.01&&hut.getYRot()==123&&hut.yBodyRot==123,"Hut stays fixed in position and orientation");
        var nbt=new CompoundTag();hut.saveWithoutId(nbt);hut.discard();var loaded=RoyaleSpells.BARBARIAN_HUT.create(c.getLevel());loaded.load(nbt);c.getLevel().addFreshEntity(loaded);
        loaded.tick();c.assertTrue(loaded.remainingLife()==297&&loaded.getYRot()==123,"Reload preserves lifetime and facing");
        c.assertTrue(units(c,owner).stream().filter(e->e.getType()==RoyaleSpells.BARBARIAN).count()==6,"Reload does not duplicate first wave");
        for(int i=0;i<297;i++)loaded.tick();loaded.die(c.getLevel().damageSources().generic());
        c.assertTrue(loaded.isRemoved()&&units(c,owner).stream().filter(e->e.getType()==RoyaleSpells.BARBARIAN).count()==7,"Expiry and repeated death callback produce exactly one final barbarian");
        cleanup(c,owner);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="retained-clone")
    public void cloneKeepsBarbarianEquipmentAndExcludesHut(GameTestHelper c){
        UUID owner=UUID.randomUUID();var center=hutFloor(c);SpellEngine.summon(c.getLevel(),owner,center,"barbarian",false);var hut=SpellEngine.summon(c.getLevel(),owner,center.add(-2,3,0),"barbarian_hut",false);
        c.assertTrue(hut!=null,"Clone fixture must have space for the hut");
        SpellEngine.cloneAllies(c.getLevel(),owner,center,7);
        var clone=units(c,owner).stream().filter(e->e instanceof Summoned s&&s.isClone()).findFirst().orElseThrow();
        c.assertTrue(clone.getType()==RoyaleSpells.BARBARIAN&&clone.getMaxHealth()==1&&clone.getMainHandItem().is(Items.IRON_SWORD),"Clone retains Barbarian kind, one HP, and held sword");
        c.assertTrue(units(c,owner).stream().filter(e->e instanceof RoyaleUnit).count()==1,"Hut cannot be cloned");
        cleanup(c,owner);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="hut-mirror")
    public void mirrorBoostsHutAndItsBarbarians(GameTestHelper c){
        var floor=BlockPos.containing(at(c)).above(12);
        for(int x=-8;x<=8;x++)for(int z=-6;z<=6;z++){
            c.getLevel().setBlockAndUpdate(floor.offset(x,0,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for(int y=1;y<=8;y++)c.getLevel().setBlockAndUpdate(floor.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        var player=TestPlayers.create(c);player.setPos(Vec3.atBottomCenterOf(floor.above(6)));player.setXRot(90);UUID owner=player.getUUID();
        // Advance the card cooldown clock independently of world ticks.
        for(int i=0;i<11;i++)SpellEngine.tick(c.getLevel().getServer());
        c.assertTrue(SpellEngine.deploy(player,TroopCard.BARBARIAN_HUT),"Hut card deploys");for(int i=0;i<11;i++)SpellEngine.tick(c.getLevel().getServer());
        player.setPos(player.position().add(5,0,0));
        c.assertTrue(SpellEngine.cast(player,Spell.MIRROR),"Mirror copies the Hut card");
        var boosted=(RoyaleUnit)units(c,owner).stream().filter(e->e instanceof RoyaleUnit u&&CardBalance.level(u)==12).findFirst().orElseThrow();
        c.assertTrue(Math.abs(boosted.getMaxHealth()-CardBalance.convert(1278))<.001,"Mirrored hut gains one level of health");boosted.tick();
        c.assertTrue(units(c,owner).stream().anyMatch(e->e.getType()==RoyaleSpells.BARBARIAN&&Math.abs(e.getMaxHealth()-CardBalance.convert(786))<.001),"Produced barbarians inherit Mirror's one-level boost");
        cleanup(c,owner);player.discard();c.succeed();
    }
}
