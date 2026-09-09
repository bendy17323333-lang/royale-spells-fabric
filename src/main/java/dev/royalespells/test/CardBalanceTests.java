// SPDX-License-Identifier: MIT
package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Exercise complete casts and actual melee damage, not just a copy of the
 * constants. Run both standalone and with Iron/MineClash/Zappies installed. */
@GameTestHolder("royalespells")
@PrefixGameTestTemplate(false)
public class CardBalanceTests {
    private static Vec3 at(GameTestHelper c){return Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(8,8,8)));}
    private static net.minecraft.world.entity.animal.IronGolem victim(GameTestHelper c){
        c.getLevel().setBlockAndUpdate(BlockPos.containing(at(c)).below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var e=EntityType.IRON_GOLEM.create(c.getLevel());e.setNoAi(true);e.setNoGravity(true);e.moveTo(at(c));
        e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);e.setHealth(1000);c.getLevel().addFreshEntity(e);return e;
    }
    private static void near(GameTestHelper c,double actual,double expected,String why){c.assertTrue(Math.abs(actual-expected)<.002,why+" expected="+expected+" actual="+actual);}
    private static void clean(GameTestHelper c,UUID owner){
        var list=new ArrayList<Entity>();for(var e:c.getLevel().getAllEntities())if(e instanceof Summoned s&&owner.equals(s.ownerId())||e instanceof SpellEntity fx&&owner.equals(fx.ownerId))list.add(e);
        list.forEach(Entity::discard);
    }
    @GameTest(template="empty",batch="card-damage-budget")
    public void fullCastsUseLatestPerHitAndTotalDamage(GameTestHelper c){
        Map<Spell,Integer> totals=new EnumMap<>(Spell.class);
        totals.put(Spell.ARROWS,366);totals.put(Spell.FIREBALL,688);totals.put(Spell.ZAP,192);totals.put(Spell.ZAP_EVOLUTION,384);
        totals.put(Spell.LIGHTNING,1057);totals.put(Spell.ROCKET,1484);totals.put(Spell.POISON,736);totals.put(Spell.FREEZE,148);
        totals.put(Spell.RAGE,179);totals.put(Spell.THE_LOG,268);totals.put(Spell.TORNADO,168);totals.put(Spell.EARTHQUAKE,243);
        totals.put(Spell.GIANT_SNOWBALL,179);totals.put(Spell.GIANT_SNOWBALL_EVOLUTION,179);totals.put(Spell.BARBARIAN_BARREL,232);
        totals.put(Spell.ROYAL_DELIVERY,384);totals.put(Spell.GOBLIN_CURSE,210);totals.put(Spell.VOID,2088);totals.put(Spell.VINES,306);
        for(var entry:totals.entrySet()){
            var target=victim(c);UUID owner=UUID.randomUUID();var at=target.position();
            var fx=SpellEntity.create(c.getLevel(),entry.getKey(),owner,at,at);
            for(int t=0;t<fx.duration();t++)fx.tick();
            near(c,1000-target.getHealth(),entry.getValue()/27.0,"Complete "+entry.getKey()+" cast");
            float health=target.getHealth();for(int t=0;t<25;t++)fx.tick();near(c,target.getHealth(),health,"Removed effect cannot deliver extra ticks");
            target.discard();clean(c,owner);
        }c.succeed();
    }
    @GameTest(template="empty",batch="card-profile-boundary")
    public void cardAndIronProfilesRemainIndependentAfterReload(GameTestHelper c){
        var at=at(c);var card=SpellEntity.create(c.getLevel(),Spell.FREEZE,UUID.randomUUID(),at,at);card.setCardLevel(12);
        var iron=SpellEntity.create(c.getLevel(),Spell.FREEZE,UUID.randomUUID(),at,at);iron.setIronSpell("royalespells:freeze",3,1);
        c.assertTrue(card.duration()==70&&iron.duration()==100,"Card freezes 3.5 seconds, Iron still 5 seconds");
        near(c,card.hitAmount(2),162/27.0,"Mirror uses exact level 12 hit");near(c,iron.hitAmount(2),2,"Iron uses its existing level/power pipeline");
        var n=new CompoundTag();card.saveWithoutId(n);var loaded=RoyaleSpells.SPELL.create(c.getLevel());loaded.load(n);
        c.assertTrue(loaded.cardLevel()==12&&loaded.duration()==70,"Card profile and mirror survive save/load");
        var fire=SpellEntity.create(c.getLevel(),Spell.FIREBALL,UUID.randomUUID(),at,at);near(c,fire.radius(),2.5,"Card radius");fire.setIronSpell("royalespells:fireball",1,1);near(c,fire.radius(),2.8,"Iron radius");c.succeed();
    }
    @GameTest(template="empty",batch="card-troop-weapons")
    public void cardTroopWeaponsDoNotAddHiddenVanillaDamage(GameTestHelper c){
        UUID owner=UUID.randomUUID();var target=victim(c);
        String[] kinds={"skeleton","zombie","barbarian","hero","recruit"};int[] health={81,202,716,716,547},damage={81,125,192,192,133};
        for(int i=0;i<kinds.length;i++){
            var mob=SpellEngine.summon(c.getLevel(),owner,at(c).add(2,0,0),kinds[i],false);CardBalance.apply(mob,kinds[i],false,11);mob.setNoAi(true);mob.tick();
            near(c,mob.getMaxHealth(),health[i]/27.0,"Health "+kinds[i]);near(c,mob.getAttributeValue(Attributes.ATTACK_DAMAGE),damage[i]/27.0,"Equipped attribute "+kinds[i]);
            float before=target.getHealth();target.invulnerableTime=0;c.assertTrue(mob.doHurtTarget(target),"Melee lands "+kinds[i]);near(c,before-target.getHealth(),damage[i]/27.0,"Actual melee "+kinds[i]);mob.discard();
        }target.discard();clean(c,owner);c.succeed();
    }
    @GameTest(template="empty",batch="card-shield")
    public void recruitShieldBreaksWithoutSpillingDamageIntoHealth(GameTestHelper c){
        UUID owner=UUID.randomUUID();var recruit=(AllyZombie)SpellEngine.summon(c.getLevel(),owner,at(c),"recruit",false);CardBalance.apply(recruit,"recruit",false,11);
        float hp=recruit.getHealth();near(c,recruit.cardShield(),240/27.0,"Independent card shield");recruit.hurt(c.getLevel().damageSources().generic(),100);
        near(c,recruit.cardShield(),0,"Shield broken");near(c,recruit.getHealth(),hp,"Overkill absorbed by shield");
        c.assertTrue(recruit.getOffhandItem().isEmpty(),"Broken shield disappears");recruit.invulnerableTime=0;recruit.hurt(c.getLevel().damageSources().generic(),3);near(c,recruit.getHealth(),hp-3,"Next hit reaches unarmored health");recruit.discard();c.succeed();
    }
    @GameTest(template="empty",batch="card-curse-profile")
    public void curseConversionInheritsTheCastingCardLevel(GameTestHelper c){
        var victim=victim(c);UUID owner=UUID.randomUUID();SpellEngine.curse(owner,victim,1,"",1,12);victim.hurt(c.getLevel().damageSources().generic(),2000);
        var babies=c.getLevel().getEntitiesOfClass(AllyZombie.class,victim.getBoundingBox().inflate(8),e->owner.equals(e.ownerId()));
        c.assertTrue(babies.size()==1,"Exactly one converted baby");near(c,babies.getFirst().getMaxHealth(),221/27.0,"Mirrored curse health");near(c,babies.getFirst().getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),137/27.0,"Mirrored curse attack");clean(c,owner);victim.discard();c.succeed();
    }
    @GameTest(template="empty",batch="card-clone-profile")
    public void mirroredCloneUsesCloneCardLevelAndScaledOneHitpoint(GameTestHelper c){
        UUID owner=UUID.randomUUID();var original=SpellEngine.summon(c.getLevel(),owner,at(c),"barbarian",false);CardBalance.apply(original,"barbarian",false,11);
        SpellEngine.cloneAllies(c.getLevel(),owner,at(c),3,1,"",1,12);
        var clones=c.getLevel().getEntitiesOfClass(AllyZombie.class,original.getBoundingBox().inflate(5),e->owner.equals(e.ownerId())&&e.isClone());
        c.assertTrue(clones.size()==1,"One clone");near(c,clones.getFirst().getMaxHealth(),1,"Minecraft health attribute floor is 1");near(c,clones.getFirst().getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),210/27.0,"Mirrored clone uses level 12 attack");
        near(c,original.getMaxHealth(),716/27.0,"Original not changed");clean(c,owner);c.succeed();
    }
    @GameTest(template="empty",batch="card-army-health")
    public void armyUsesThreeHealthAndShieldIncludingOldSaveMigration(GameTestHelper c){
        var general=RoyaleSpells.ARMY_SKELETON.create(c.getLevel());general.enlist(UUID.randomUUID(),UUID.randomUUID(),true,10,3,600);
        near(c,general.getMaxHealth(),3,"Every summon level uses 3 HP");near(c,general.shield(),3,"General shield 3 HP");
        var n=new CompoundTag();general.getAttribute(Attributes.MAX_HEALTH).setBaseValue(12);general.setHealth(9);general.saveWithoutId(n);n.putFloat("ArmyShield",12);
        var loaded=RoyaleSpells.ARMY_SKELETON.create(c.getLevel());loaded.load(n);near(c,loaded.getMaxHealth(),3,"Old general max HP migrated");near(c,loaded.getHealth(),3,"Old current HP capped");near(c,loaded.shield(),3,"Old shield capped");c.succeed();
    }
    @GameTest(template="empty",batch="card-snow-movement")
    public void cardSnowSlowAndNativeSnowSlowDoNotShareBalance(GameTestHelper c){
        var target=victim(c);double speed=target.getAttributeValue(Attributes.MOVEMENT_SPEED);SnowballChill.apply(target,true);
        near(c,target.getAttributeValue(Attributes.MOVEMENT_SPEED),speed*.65,"Card slow is 35 percent");c.assertTrue(target.getEffect(RoyaleSpells.SNOWBOUND).getDuration()==60,"Both snow cards slow 3 seconds");
        target.removeEffect(RoyaleSpells.CARD_SLOW);SnowballChill.apply(target,false);near(c,target.getAttributeValue(Attributes.MOVEMENT_SPEED),speed*.85,"Iron movement slow unchanged");target.discard();c.succeed();
    }
}
