package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.army.ArmyFormation;
import dev.royalespells.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public class EvolutionTests {
    private Vec3 floor(GameTestHelper c){
        var pos=c.absolutePos(new BlockPos(6,14,6));
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){c.getLevel().setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<=4;y++)c.getLevel().setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());}
        return Vec3.atBottomCenterOf(pos);
    }
    private List<EvolutionBurst> bursts(GameTestHelper c,Vec3 at){return c.getLevel().getEntitiesOfClass(EvolutionBurst.class,AABB.ofSize(at,12,12,12));}
    @GameTest(template="empty",batch="beta-evolution-neutral")
    public void successfulNeutralArmyCreatesOneFlashButBlockedDeploymentCreatesNone(GameTestHelper c){
        var at=floor(c);var list=ArmyFormation.neutral(c.getLevel(),at,0);
        c.assertTrue(list.size()==16,"All army units deployed");c.assertTrue(bursts(c,at).size()==1,"One group flash, not sixteen overlapping flashes");
        list.forEach(Entity::discard);bursts(c,at).forEach(Entity::discard);
        // World-border rejection is independent of ground() finding a new surface above a wall.
        var failed=ArmyFormation.neutral(c.getLevel(),new Vec3(31_000_000,at.y,31_000_000),0);
        c.assertTrue(failed.isEmpty()&&bursts(c,at).isEmpty(),"Rejected deployment has no cosmetic side effect");c.succeed();
    }
    @GameTest(template="empty",batch="beta-evolution-barrel")
    public void onlyEvolvedBarrelsFlashAndBothRealAndDecoyLandingsAreIncluded(GameTestHelper c){
        var at=floor(c);var owner=UUID.randomUUID();
        for(int mode=0;mode<3;mode++){
            var spell=SpellEntity.create(c.getLevel(),mode==0?Spell.GOBLIN_BARREL:Spell.GOBLIN_BARREL_EVOLUTION,owner,at,at);spell.decoy=mode==2;
            for(int i=0;i<spell.spell().duration;i++)spell.tick();
            c.assertTrue(bursts(c,at).size()==(mode==0?0:1),"Evolution-only arrival effect for landing "+mode);
            spell.discard();bursts(c,at).forEach(Entity::discard);
            for(var unit:c.getLevel().getEntitiesOfClass(AllyZombie.class,AABB.ofSize(at,12,12,12)))if(owner.equals(unit.ownerId()))unit.discard();
        }c.succeed();
    }
    @GameTest(template="empty",batch="beta-evolution-lifetime")
    public void burstRemainsHarmlessAndSaveReloadCannotRestartIt(GameTestHelper c){
        var at=floor(c);EvolutionBurst.deploy(c.getLevel(),at,2.1f);var burst=bursts(c,at).getFirst();
        c.assertTrue(!burst.isPickable()&&burst.noPhysics,"Cosmetics never intercept targeting or collide");
        for(int i=0;i<10;i++)burst.tick();var n=new CompoundTag();burst.saveWithoutId(n);burst.discard();
        var copy=RoyaleSpells.EVOLUTION_BURST.create(c.getLevel());copy.load(n);c.assertTrue(copy.age()==10&&Math.abs(copy.radius()-2.1f)<.001,"Reload keeps animation phase and size");
        for(int i=10;i<EvolutionBurst.DURATION;i++)copy.tick();c.assertTrue(copy.isRemoved(),"Deployment cleans up after 24 ticks");
        n.putLong("Expires",c.getLevel().getGameTime()-1);var stale=RoyaleSpells.EVOLUTION_BURST.create(c.getLevel());stale.load(n);stale.tick();c.assertTrue(stale.isRemoved(),"Unloaded effects expire without replaying on return");c.succeed();
    }
}
