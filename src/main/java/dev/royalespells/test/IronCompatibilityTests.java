package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

/** Registered explicitly only when the real Iron's mod and its dependencies are installed. */
@PrefixGameTestTemplate(false)
public class IronCompatibilityTests {
    private static Vec3 arena(GameTestHelper c) {
        var p=c.absolutePos(new BlockPos(2,12,2));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            c.getLevel().setBlockAndUpdate(p.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<8;y++)c.getLevel().setBlockAndUpdate(p.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static Mob owner(GameTestHelper c,Vec3 at) {
        var mob=EntityType.VILLAGER.create(c.getLevel());mob.setPos(at);mob.setNoAi(true);mob.setInvulnerable(true);
        c.getLevel().addFreshEntity(mob);return mob;
    }
    private static Mob iron(GameTestHelper c,String type,Vec3 at,Entity owner) {
        var entity=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",type)).create(c.getLevel());
        c.assertTrue(entity instanceof Mob,"Real Iron's entity must be registered: "+type);
        var mob=(Mob)entity;mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);c.getLevel().addFreshEntity(mob);
        if(owner!=null)SummonManager.setOwner(mob,owner);
        return mob;
    }
    private static Mob ours(GameTestHelper c,Vec3 at,Entity owner) {
        var mob=SpellEngine.summon(c.getLevel(),owner.getUUID(),at,"barbarian",false);
        c.assertTrue(mob!=null,"Barbarian deployment must succeed");mob.setNoAi(true);return mob;
    }
    private static void cleanup(Entity... entities){for(var e:entities)if(e!=null)e.discard();}

    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-friendly")
    public void sharedOwnerProtectsBothSummonSystems(GameTestHelper c) {
        var at=arena(c);var owner=owner(c,at.add(0,0,4));var barb=ours(c,at,owner);var zombie=iron(c,"summoned_zombie",at.add(2,0,0),owner);
        c.assertTrue(SpellEngine.friendly(owner.getUUID(),zombie),"Resolve Iron's SummonManager owner");
        c.assertFalse(SpellEngine.targets(c.getLevel(),owner.getUUID(),at,4,false).contains(zombie),"Offensive area spells exclude friendly Iron's summons");
        c.assertTrue(SpellEngine.targets(c.getLevel(),owner.getUUID(),at,4,true).contains(zombie),"Support area spells include friendly Iron's summons");
        float a=barb.getHealth(),b=zombie.getHealth();
        SpellEngine.hit(c.getLevel(),owner.getUUID(),zombie,5);
        barb.hurt(c.getLevel().damageSources().mobAttack(zombie),5);
        DamageSources.applyDamage(barb,5,SpellRegistry.FIREBOLT_SPELL.get().getDamageSource(owner));
        c.assertTrue(barb.getHealth()==a&&zombie.getHealth()==b,"Both melee and Iron's spell friendly fire are blocked");
        barb.setTarget(zombie);zombie.setTarget(barb);
        c.assertTrue(barb.getTarget()==null&&zombie.getTarget()==null,"Both AI target paths reject friendly summons");
        cleanup(barb,zombie,owner);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-enemy")
    public void enemySummonsTakeRealDamageBothWays(GameTestHelper c) {
        var at=arena(c);var a=owner(c,at.add(-4,0,4));var b=owner(c,at.add(4,0,4));var barb=ours(c,at,a);var zombie=iron(c,"summoned_zombie",at.add(2,0,0),b);
        c.assertFalse(SpellEngine.friendly(a.getUUID(),zombie),"Different owners without a team remain enemies");
        barb.setTarget(zombie);zombie.setTarget(barb);
        c.assertTrue(barb.getTarget()==zombie&&zombie.getTarget()==barb,"Enemy target assignment survives compatibility hook: ours="+(barb.getTarget()==zombie)+" iron="+(zombie.getTarget()==barb));
        barb.setTarget(null);zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(io.redspace.ironsspellbooks.registries.MobEffectRegistry.TRUE_INVISIBILITY,40));
        barb.setTarget(zombie);c.assertTrue(barb.getTarget()==null,"Iron's true invisibility still blocks target acquisition");
        zombie.removeEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.TRUE_INVISIBILITY);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent> protection=e->{if(e.getEntity()==barb)e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,protection);
        try {barb.setTarget(zombie);c.assertTrue(barb.getTarget()==null,"Other mods' explicit target cancellation stays intact");}
        finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protection);}
        float ironHealth=zombie.getHealth();SpellEngine.hit(c.getLevel(),a.getUUID(),zombie,3);
        c.assertTrue(zombie.getHealth()<ironHealth,"Royale magic damages hostile Iron's summons");
        float ourHealth=barb.getHealth();DamageSources.applyDamage(barb,3,SpellRegistry.FIREBOLT_SPELL.get().getDamageSource(b));
        c.assertTrue(barb.getHealth()<ourHealth&&barb.isOnFire(),"Iron's damage pipeline and fire post-hit effect remain active");
        barb.invulnerableTime=0;ourHealth=barb.getHealth();zombie.doHurtTarget(barb);
        c.assertTrue(barb.getHealth()<ourHealth,"Iron's summon melee damages our unit");
        zombie.invulnerableTime=0;ironHealth=zombie.getHealth();barb.doHurtTarget(zombie);
        c.assertTrue(zombie.getHealth()<ironHealth,"Our sword damages Iron's summon");
        cleanup(barb,zombie,a,b);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-melee-ai",timeoutTicks=110)
    public void bothSummonAisActuallyFight(GameTestHelper c) {
        var at=arena(c);var a=owner(c,at.add(-4,0,4));var b=owner(c,at.add(4,0,4));var barb=ours(c,at,a);var zombie=iron(c,"summoned_zombie",at.add(1.4,0,0),b);
        for(var mob:List.of(barb,zombie)){mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);mob.setNoAi(false);mob.setNoGravity(false);}
        barb.setTarget(zombie);zombie.setTarget(barb);
        c.runAtTickTime(75,()->{
            c.assertTrue(barb.getHealth()<200&&zombie.getHealth()<200,"Both original melee goals must cause damage over world ticks: ours="+barb.getHealth()+" iron="+zombie.getHealth());
            cleanup(barb,zombie,a,b);c.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-snowball",timeoutTicks=240)
    public void snowballMovesAnIronCasterAfterLanding(GameTestHelper c) {
        var at=arena(c);var caster=iron(c,"cryomancer",at,null);var impact=at.add(0,0,-.5);var w=c.getLevel();
        var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();var origin=new net.minecraft.world.level.ChunkPos(caster.blockPosition());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=new net.minecraft.world.level.ChunkPos(origin.x+x,origin.z+z);if(w.setChunkForced(chunk.x,chunk.z,true))forced.add(chunk);}
        var fx=SpellEntity.create(w,Spell.GIANT_SNOWBALL,UUID.randomUUID(),impact.add(0,0,-6),impact);w.addFreshEntity(fx);
        c.succeedWhen(()->{
            c.assertTrue(fx.time()>=Spell.GIANT_SNOWBALL.duration,"Wait for actual Snowball impact");
            c.assertTrue(caster.getZ()>at.z+1,"Snowball must displace the real Iron's caster");
            cleanup(caster,fx);for(var chunk:forced)w.setChunkForced(chunk.x,chunk.z,false);
        });
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-team")
    public void scoreboardAlliesAndEnemiesStayDistinct(GameTestHelper c) {
        var at=arena(c);var a=owner(c,at.add(-4,0,4));var b=owner(c,at.add(4,0,4));var barb=ours(c,at,a);var zombie=iron(c,"summoned_zombie",at.add(2,0,0),b);
        var board=c.getLevel().getScoreboard();var team=board.addPlayerTeam("royale_"+a.getId());
        try {
            board.addPlayerToTeam(a.getScoreboardName(),team);board.addPlayerToTeam(b.getScoreboardName(),team);
            c.assertTrue(SpellEngine.friendly(a.getUUID(),zombie),"Owners' scoreboard alliance propagates across mods");
            zombie.setTarget(barb);c.assertTrue(zombie.getTarget()==null,"Allied Iron's summon does not pursue our unit");
            board.removePlayerFromTeam(b.getScoreboardName(),team);
            c.assertFalse(SpellEngine.friendly(a.getUUID(),zombie),"Leaving the team restores hostility immediately");
            zombie.setTarget(barb);c.assertTrue(zombie.getTarget()==barb,"Enemy can target us after alliance changes");
        } finally {board.removePlayerTeam(team);cleanup(barb,zombie,a,b);}
        c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-casting-freeze",timeoutTicks=80)
    public void freezePausesAnActualIronCastAndThenResumes(GameTestHelper c) throws Exception {
        var at=arena(c);var owner=owner(c,at.add(4,0,4));var barb=ours(c,at.add(3,0,0),owner);var caster=iron(c,"pyromancer",at,null);
        caster.setNoAi(false);caster.setTarget(barb);
        caster.getClass().getMethod("initiateCastSpell",AbstractSpell.class,int.class).invoke(caster,SpellRegistry.FIREBALL_SPELL.get(),1);
        var data=(MagicData)caster.getClass().getMethod("getMagicData").invoke(caster);int remaining=data.getCastDurationRemaining();
        c.assertTrue(data.isCasting()&&remaining>0,"Pyromancer must start a real Iron's fireball cast");
        SpellEngine.stun(caster,30);Vec3 before=caster.position();
        c.runAtTickTime(15,()->{
            c.assertTrue(data.getCastDurationRemaining()==remaining,"Frozen caster must not advance its cast timer");
            c.assertTrue(caster.position().distanceTo(before)<.02,"Frozen caster must not move");
        });
        c.runAtTickTime(42,()->{
            c.assertFalse(caster.hasEffect(RoyaleSpells.STUN),"Freeze expires");
            c.assertTrue(data.getCastDurationRemaining()<remaining,"Original casting AI resumes after thawing");
            cleanup(caster,barb,owner);c.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-tornado",timeoutTicks=55)
    public void tornadoMovesAnIronCaster(GameTestHelper c) {
        var at=arena(c);var caster=iron(c,"cryomancer",at,null);var center=at.add(3,0,0);
        var fx=SpellEntity.create(c.getLevel(),Spell.TORNADO,UUID.randomUUID(),center,center);c.getLevel().addFreshEntity(fx);
        c.runAtTickTime(29,()->{
            c.assertTrue(caster.getX()>at.x+2&&caster.position().distanceTo(center)<1,"Actual Tornado pulls the Iron's caster into its center");
            cleanup(caster,fx);c.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-death")
    public void ironDamageTriggersCurseExactlyOnce(GameTestHelper c) {
        var at=arena(c);var owner=owner(c,at.add(4,0,4));var target=iron(c,"pyromancer",at,null);
        SpellEngine.curse(owner.getUUID(),target);
        DamageSources.applyDamage(target,1000,SpellRegistry.FIREBOLT_SPELL.get().getDamageSource(owner));
        c.assertFalse(target.isAlive(),"Iron's actual damage pipeline kills the cursed caster");
        var list=c.getLevel().getEntitiesOfClass(AllyZombie.class,target.getBoundingBox().inflate(2),e->owner.getUUID().equals(e.ownerId()));
        c.assertTrue(list.size()==1,"Curse death hook produces exactly one baby zombie");
        list.forEach(Entity::discard);cleanup(target,owner);c.succeed();
    }
}
