package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public final class SnowballVisualTests {
    private static void near(GameTestHelper h,double expected,double actual,String why){h.assertTrue(Math.abs(expected-actual)<.0001,why+": "+actual+" != "+expected);}
    @GameTest(template="empty",batch="170-snowball-visuals") public void visualClockSlowsResumesAndComposesWithPause(GameTestHelper h){
        var clock=new AnimationTimeline();near(h,100,clock.sample(100,1),"Initial time");
        near(h,106.5,clock.sample(110,.65),"65 percent animation speed");
        near(h,106.5,clock.sample(110,.65),"Upstream electrical pause holds local clock");
        near(h,107.15,clock.sample(111,.65),"Resume without catch-up");
        near(h,108.15,clock.sample(112,1),"Unchilled real rate retains offset");
        var other=new AnimationTimeline();near(h,112,other.sample(112,1),"Other entity is independent");h.succeed();
    }
    @GameTest(template="empty",batch="170-snowball-visuals") public void snowballFacingSnapshotsEvenVerticalCastAndPersists(GameTestHelper h){
        var caster=h.spawnWithNoFreeWill(EntityType.SKELETON,2,2,2);caster.setYRot(90);caster.setXRot(90);
        for(var spell:new Spell[]{Spell.GIANT_SNOWBALL,Spell.GIANT_SNOWBALL_EVOLUTION}){
            var e=SpellEntity.create(h.getLevel(),spell,caster.getUUID(),caster.position(),caster.position().add(0,-1,0));
            near(h,-1,e.castDirection().x,"Cast yaw survives looking vertically down");
            var tag=new CompoundTag();e.saveWithoutId(tag);var restored=RoyaleSpells.SPELL.create(h.getLevel());restored.load(tag);
            near(h,-1,restored.castDirection().x,"Facing survives save/load");
            caster.setYRot(180);near(h,-1,e.castDirection().x,"Later camera movement cannot steer");caster.setYRot(90);
            if(spell.evolved()){e.setPreviewTime(42);near(h,-4,e.visualPosition(0).x-e.target().x,"Evolved roll follows caster heading");}
        }
        caster.discard();h.succeed();
    }
    @GameTest(template="empty",batch="171-snowball-normal",timeoutTicks=120) public void normalSnowballBlueFlagAndExpiryMatchExistingSlow(GameTestHelper h){
        // The entire incoming arc and subsequent knockback stay inside the
        // loaded template; a start at z=-2 can land in an unticked neighbor.
        var mob=h.spawnWithNoFreeWill(EntityType.COW,8,8,8);mob.setNoGravity(true);
        var e=SpellEntity.create(h.getLevel(),Spell.GIANT_SNOWBALL,UUID.randomUUID(),mob.position().add(0,2,-4),mob.position());h.getLevel().addFreshEntity(e);
        h.runAtTickTime(26,()->{
            near(h,10-CardBalance.convert(179),mob.getHealth(),"Latest card damage");
            h.assertTrue(mob.hasEffect(RoyaleSpells.SNOWBOUND)&&VisualState.snowbound(mob),"Chill marker is synchronized");
            h.assertTrue(mob.hasEffect(RoyaleSpells.CARD_SLOW),"Precise 35 percent card slow");
            h.assertFalse(mob.hasEffect(RoyaleSpells.STUN)||mob.hasEffect(RoyaleSpells.FROZEN),"Snowball does not hard freeze");
        });
        h.runAtTickTime(88,()->{h.assertFalse(mob.hasEffect(RoyaleSpells.SNOWBOUND)||VisualState.snowbound(mob),"Chill and color expire after 60 ticks");mob.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="172-snowball-evolution",timeoutTicks=125) public void evolvedSnowballAppliesBlueSlowWhenReleased(GameTestHelper h){
        var mob=h.spawnWithNoFreeWill(EntityType.COW,8,8,8);mob.setNoGravity(true);
        var e=SpellEntity.create(h.getLevel(),Spell.GIANT_SNOWBALL_EVOLUTION,UUID.randomUUID(),mob.position().add(0,2,-4),mob.position());h.getLevel().addFreshEntity(e);
        h.runAtTickTime(29,()->{h.assertTrue(mob.hasEffect(RoyaleSpells.STUN),"Captured period unchanged");h.assertFalse(mob.hasEffect(RoyaleSpells.SNOWBOUND),"Slow starts after release");});
        h.runAtTickTime(45,()->{near(h,10-CardBalance.convert(179),mob.getHealth(),"One damage event");h.assertTrue(mob.hasEffect(RoyaleSpells.SNOWBOUND)&&VisualState.snowbound(mob),"Released troop turns blue");h.assertFalse(mob.hasEffect(RoyaleSpells.STUN),"Release restores action");});
        h.runAtTickTime(108,()->{h.assertFalse(VisualState.snowbound(mob),"Release slow expires");mob.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="173-snowball-warmth",timeoutTicks=20) public void ordinarySlownessDoesNotBlueTintAndWarmthClearsChill(GameTestHelper h){
        var mob=h.spawnWithNoFreeWill(EntityType.COW,2,2,2);mob.setNoGravity(true);mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60));
        h.runAtTickTime(3,()->{h.assertFalse(VisualState.snowbound(mob),"Generic slow is not snowball chill");SnowballChill.apply(mob);});
        h.runAtTickTime(5,()->{h.assertTrue(VisualState.snowbound(mob),"Snowball marker present");var e=SpellEntity.create(h.getLevel(),Spell.WARMTH,mob.getUUID(),mob.position(),mob.position());h.getLevel().addFreshEntity(e);});
        h.runAtTickTime(9,()->{h.assertFalse(mob.hasEffect(RoyaleSpells.SNOWBOUND)||VisualState.snowbound(mob),"Warmth cures animation slow and tint");mob.discard();h.succeed();});
    }
}
