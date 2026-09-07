package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.iron.*;
import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
@PrefixGameTestTemplate(false)
public class IronBalanceTests {
    private ServerPlayer player(GameTestHelper c){var p=TestPlayers.create(c);p.setPos(Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(3,15,3))));return p;}
    private Mob target(GameTestHelper c,Vec3 at){var e=EntityType.PIG.create(c.getLevel());e.setPos(at);e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);e.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);c.getLevel().addFreshEntity(e);return e;}
    private void cleanup(ServerPlayer p){for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof SpellEntity f && p.getUUID().equals(f.ownerId) || e instanceof Summoned s && p.getUUID().equals(s.ownerId()))e.discard();p.server.getPlayerList().remove(p);p.discard();}
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-balance-frames")
    public void spellsRespectHitImmunityButGraveyardKeepsItsWeakRapidHits(GameTestHelper c) {
        var p=player(c);var at=p.position().add(3,0,0);var mob=target(c,at);var fx=SpellEntity.create(c.getLevel(),Spell.ZAP,p.getUUID(),at,at);fx.setIronSpell("royalespells:zap",1,1);
        IronSpellSystem.damage(fx,mob,4);float health=mob.getHealth();IronSpellSystem.damage(fx,mob,4);
        c.assertTrue(Math.abs(health-496)<.01 && mob.getHealth()==health && mob.invulnerableTime>0,"Repeated equal spell hits do not bypass immunity");
        var skeleton=SpellEngine.summon(c.getLevel(),p.getUUID(),at.add(3,0,0),"skeleton",false);skeleton.setNoAi(true);IronSpellSystem.summon(skeleton,p.getUUID(),"royalespells:graveyard",1);
        skeleton.doHurtTarget(mob);skeleton.doHurtTarget(mob);
        c.assertTrue(Math.abs(mob.getHealth()-(health-3))<.01,"Two graveyard attacks remain 1.5 each through existing immunity");mob.discard();cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-balance-weapon",timeoutTicks=80)
    public void visibleBarbarianSwordNoLongerDoublesActualDamage(GameTestHelper c) {
        var p=player(c);var mob=SpellEngine.summon(c.getLevel(),p.getUUID(),p.position().add(4,0,0),"barbarian",false);mob.setNoAi(true);mob.setNoGravity(true);IronSpellSystem.summon(mob,p.getUUID(),"royalespells:barbarian_barrel",1);
        c.runAtTickTime(20,()->{var victim=target(c,mob.position().add(1,0,0));mob.doHurtTarget(victim);c.assertTrue(Math.abs(500-victim.getHealth()-5)<.01,"Real post-equipment attack is 5, not 11: "+(500-victim.getHealth()));c.assertFalse(mob.getMainHandItem().isEmpty(),"Sword stays visible");victim.discard();cleanup(p);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-balance-damage",timeoutTicks=210)
    public void actualDamageAtFirstAndLastLevelMatchesTheBalancedTable(GameTestHelper c) {
        var p=player(c);var profiles=List.of(IronSpellProfile.FIREBALL,IronSpellProfile.ROCKET,IronSpellProfile.POISON,IronSpellProfile.VOID,IronSpellProfile.ARROWS,IronSpellProfile.ZAP_EVOLUTION);
        var targets=new ArrayList<Mob>();var expected=new ArrayList<Float>();int i=0;
        for(var profile:profiles)for(int level:new int[]{1,profile.maxLevel()}) {
            var spell=(RoyaleIronSpell)IronIntegration.spell(profile);var at=p.position().add(15+(i++)*16,0,0);for(int cx=(int)Math.floor(at.x/16)-1;cx<=(int)Math.floor(at.x/16)+1;cx++)for(int cz=(int)Math.floor(at.z/16)-1;cz<=(int)Math.floor(at.z/16)+1;cz++)c.getLevel().setChunkForced(cx,cz,true);var victim=target(c,at);targets.add(victim);
            var fx=SpellEntity.create(c.getLevel(),profile.card(),p.getUUID(),at.add(0,4,-4),at);fx.setPower(spell.power(level,p));fx.setIronSpell(spell.getSpellId(),level,1);c.getLevel().addFreshEntity(fx);
            float base=switch(profile){case FIREBALL->12;case ROCKET->30;case POISON->16;case VOID->36;case ARROWS->9;default->8;};expected.add(base*spell.power(level,p));
            System.out.println("ROYALE_BALANCE_SAMPLE "+profile+" level="+level+" expected="+expected.getLast()+" mana="+spell.getManaCost(level));
        }
        c.runAtTickTime(170,()->{for(int n=0;n<targets.size();n++) {float actual=500-targets.get(n).getHealth();c.assertTrue(Math.abs(actual-expected.get(n))<.04,"Actual timed hits sample "+n+": expected "+expected.get(n)+" got "+actual);targets.get(n).discard();}cleanup(p);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-balance-growth")
    public void nativeEquipmentScalesOutputAndControlHasAnExplicitCap(GameTestHelper c) {
        var p=player(c);
        for(var profile:IronSpellProfile.values()) {
            var spell=(RoyaleIronSpell)IronIntegration.spell(profile);
            for(int level=1;level<=spell.getMaxLevel();level++)c.assertTrue(spell.getManaCost(level)>0 && spell.power(level,p)>0,"All 29 profiles have valid level costs");
        }
        var fire=(RoyaleIronSpell)IronIntegration.spell(IronSpellProfile.FIREBALL);
        c.assertTrue(Math.abs(fire.power(6,p)*12-30)<.01,"Late fireball keeps up with native 30 damage");
        var freeze=(RoyaleIronSpell)IronIntegration.spell(IronSpellProfile.FREEZE);var vines=(RoyaleIronSpell)IronIntegration.spell(IronSpellProfile.VINES);
        c.assertTrue(Math.abs(freeze.durationScale(1,p)*5-5)<.01 && Math.abs(vines.durationScale(1,p)*2.5-1.5)<.01,"Freeze lasts 5s and Vines retains 1.5s at base level");
        p.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(20);
        c.assertTrue(freeze.durationScale(5,p)*5==5 && vines.durationScale(5,p)*2.5<=2.5,"Freeze stays at 5s and Vines retains its prior cap with heavy gear");cleanup(p);c.succeed();
    }
}
