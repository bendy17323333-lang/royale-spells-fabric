package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;
import net.minecraft.registry.Registries;
import java.util.*;

public class TroopTests implements FabricGameTest {
    private Vec3d at(TestContext c){return Vec3d.ofBottomCenter(c.getAbsolutePos(new BlockPos(2,2,2)));}
    private List<MobEntity> units(TestContext c,UUID owner){return c.getWorld().getEntitiesByClass(MobEntity.class,new Box(at(c).add(-40,-10,-40),at(c).add(40,20,40)),e->e instanceof Summoned s&&owner.equals(s.ownerId())&&e.isAlive());}
    private void cleanup(TestContext c,UUID owner){for(var e:c.getWorld().iterateEntities())if(e instanceof Summoned s&&owner.equals(s.ownerId()))e.discard();}
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="retained-cards")
    public void retainedCardsAndOriginalSkeleton(TestContext c){
        c.assertTrue(RoyaleSpells.TROOP_ITEMS.size()==1,"Only the requested Hut card remains");
        c.assertTrue(c.getWorld().getRecipeManager().get(RoyaleSpells.id("barbarian_hut")).isPresent(),"Hut remains craftable");
        for(String name:List.of("golem","night_witch","golemite","royale_bat","bat")){
            c.assertTrue(!Registries.ENTITY_TYPE.containsId(RoyaleSpells.id(name)),"Removed troop entity: "+name);
            c.assertTrue(!Registries.ITEM.containsId(RoyaleSpells.id(name)),"Removed troop card: "+name);
            c.assertTrue(SpellEngine.summon(c.getWorld(),null,at(c),name,false)==null,"Removed troop must not fall back to a zombie");
        }
        UUID owner=UUID.randomUUID();var skeleton=SpellEngine.summon(c.getWorld(),owner,at(c),"skeleton",false);
        c.assertTrue(skeleton.getMainHandStack().isOf(Items.STONE_SWORD),"Skeleton still uses a real stone sword");
        c.assertTrue(Math.abs(skeleton.getHeight()-1.99)<.001&&skeleton.getMaxHealth()==6,"Original skeleton proportions and low health");
        c.assertTrue(skeleton.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)==1.5,"Keep weakened damage after model rollback");
        var barbarian=(AllyZombie)SpellEngine.summon(c.getWorld(),owner,at(c).add(3,0,0),"barbarian",false);
        c.assertTrue(barbarian.getMainHandStack().isOf(Items.IRON_SWORD),"Barbarian has real equipment for the held-item renderer");
        barbarian.tick();var nbt=new NbtCompound();barbarian.writeNbt(nbt);
        c.assertTrue(nbt.getBoolean("DeploymentPlayed"),"Persist deploy playback so loading does not replay it");
        cleanup(c,owner);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hut-spawner")
    public void hutProducesWavesReloadsAndExpiresOnce(TestContext c){
        UUID owner=UUID.randomUUID();var hut=(RoyaleUnit)SpellEngine.summon(c.getWorld(),owner,at(c).add(0,3,0),"barbarian_hut",false);hut.lockFacing(123);hut.setNoGravity(true);Vec3d initial=hut.getPos();hut.setVelocity(2,2,2);
        for(int i=0;i<302;i++)hut.tick();
        c.assertTrue(units(c,owner).stream().filter(e->e.getType()==RoyaleSpells.BARBARIAN).count()==6,"Two waves of three barbarians");
        c.assertTrue(hut.getPos().distanceTo(initial)<.01&&hut.getYaw()==123&&hut.bodyYaw==123,"Hut stays fixed in position and orientation");
        var nbt=new NbtCompound();hut.writeNbt(nbt);hut.discard();var loaded=RoyaleSpells.BARBARIAN_HUT.create(c.getWorld());loaded.readNbt(nbt);c.getWorld().spawnEntity(loaded);
        loaded.tick();c.assertTrue(loaded.remainingLife()==297&&loaded.getYaw()==123,"Reload preserves lifetime and facing");
        c.assertTrue(units(c,owner).stream().filter(e->e.getType()==RoyaleSpells.BARBARIAN).count()==6,"Reload does not duplicate first wave");
        for(int i=0;i<297;i++)loaded.tick();loaded.onDeath(c.getWorld().getDamageSources().generic());
        c.assertTrue(loaded.isRemoved()&&units(c,owner).stream().filter(e->e.getType()==RoyaleSpells.BARBARIAN).count()==7,"Expiry and repeated death callback produce exactly one final barbarian");
        cleanup(c,owner);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="retained-clone")
    public void cloneKeepsBarbarianEquipmentAndExcludesHut(TestContext c){
        UUID owner=UUID.randomUUID();SpellEngine.summon(c.getWorld(),owner,at(c),"barbarian",false);SpellEngine.summon(c.getWorld(),owner,at(c).add(-2,3,0),"barbarian_hut",false);
        SpellEngine.cloneAllies(c.getWorld(),owner,at(c),7);
        var clone=units(c,owner).stream().filter(e->e instanceof Summoned s&&s.isClone()).findFirst().orElseThrow();
        c.assertTrue(clone.getType()==RoyaleSpells.BARBARIAN&&clone.getMaxHealth()==1&&clone.getMainHandStack().isOf(Items.IRON_SWORD),"Clone retains Barbarian kind, one HP, and held sword");
        c.assertTrue(units(c,owner).stream().filter(e->e instanceof RoyaleUnit).count()==1,"Hut cannot be cloned");
        cleanup(c,owner);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hut-mirror")
    public void mirrorBoostsHutAndItsBarbarians(TestContext c){
        var floor=BlockPos.ofFloored(at(c)).up(12);
        for(int x=-8;x<=8;x++)for(int z=-6;z<=6;z++){
            c.getWorld().setBlockState(floor.add(x,0,z),net.minecraft.block.Blocks.STONE.getDefaultState());
            for(int y=1;y<=8;y++)c.getWorld().setBlockState(floor.add(x,y,z),net.minecraft.block.Blocks.AIR.getDefaultState());
        }
        var player=c.createMockCreativeServerPlayerInWorld();player.setPosition(Vec3d.ofBottomCenter(floor.up(6)));player.setPitch(90);UUID owner=player.getUuid();
        // Fabric's mock player reuses its profile, including any earlier card cooldown.
        for(int i=0;i<11;i++)SpellEngine.tick(c.getWorld().getServer());
        c.assertTrue(SpellEngine.deploy(player,TroopCard.BARBARIAN_HUT),"Hut card deploys");for(int i=0;i<11;i++)SpellEngine.tick(c.getWorld().getServer());
        player.setPosition(player.getPos().add(5,0,0));
        c.assertTrue(SpellEngine.cast(player,Spell.MIRROR),"Mirror copies the Hut card");
        var boosted=(RoyaleUnit)units(c,owner).stream().filter(e->e instanceof RoyaleUnit u&&u.power()>1).findFirst().orElseThrow();
        c.assertTrue(Math.abs(boosted.getMaxHealth()-71.5)<.001,"Mirrored hut gains one level of health");boosted.tick();
        c.assertTrue(units(c,owner).stream().anyMatch(e->e.getType()==RoyaleSpells.BARBARIAN&&Math.abs(e.getMaxHealth()-22)<.001),"Produced barbarians inherit Mirror's one-level boost");
        cleanup(c,owner);player.discard();c.complete();
    }
}
