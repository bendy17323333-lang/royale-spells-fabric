package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public class InfernoIronTests {
    private ServerPlayer player(GameTestHelper c){var p=TestPlayers.create(c);var at=c.absolutePos(new BlockPos(7,15,7));for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-1;y<10;y++)c.getLevel().setBlockAndUpdate(at.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState());p.setPos(Vec3.atBottomCenterOf(at));p.setXRot(60);p.setYRot(0);p.setGameMode(GameType.SURVIVAL);p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(600);MagicData.getPlayerMagicData(p).setMana(600);return p;}
    private void finish(ServerPlayer p){var manager=new io.redspace.ironsspellbooks.capabilities.magic.MagicManager();for(int i=0;i<100&&MagicData.getPlayerMagicData(p).isCasting();i++)manager.tick(p.serverLevel());}
    private void cleanup(ServerPlayer p){for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof Summoned s&&p.getUUID().equals(s.ownerId()))e.discard();p.server.getPlayerList().remove(p);p.discard();}
    @GameTest(template="empty",templateNamespace="royalespells",batch="inferno-iron-cast")
    public void NativeBookCastPaysManaAndMirrorSummonsAtOneHigherLevel(GameTestHelper c){
        var p=player(c);var spell=IronIntegration.spell(IronSpellProfile.INFERNO_DRAGON);var magic=MagicData.getPlayerMagicData(p);
        // finish() advances the native manager within one server tick. If that
        // happens on a regen tick, its loop would repeatedly replenish mana.
        // Isolate exact spell cost from that unrelated regeneration schedule.
        p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN).setBaseValue(0);
        c.assertTrue(spell.attemptInitiateCast(ItemStack.EMPTY,1,p.level(),p,CastSource.SPELLBOOK,true,"mainhand"),"Native summon casting starts");finish(p);
        var list=p.serverLevel().getEntitiesOfClass(InfernoDragon.class,p.getBoundingBox().inflate(24),e->p.getUUID().equals(e.ownerId()));
        c.assertTrue(list.size()==1&&magic.getMana()==500,"The native spell creates one dragon and spends exactly 100 mana; count="+list.size()+" mana="+magic.getMana()+" casting="+magic.isCasting());
        c.assertTrue(magic.getPlayerCooldowns().isOnCooldown(spell)&&spell.getSpellCooldown()==800,"Native cooldown is forty seconds");
        c.assertTrue(MirrorIronSpell.history(p).getSpell()==spell,"A successful dragon cast enters Mirror history");
        list.getFirst().setPos(p.position().add(4,2,2));list.getFirst().setNoAi(true);
        var mirror=IronIntegration.spell(IronSpellProfile.MIRROR);c.assertTrue(mirror.attemptInitiateCast(ItemStack.EMPTY,1,p.level(),p,CastSource.SPELLBOOK,true,"mainhand"),"Mirror delegates the native summon");finish(p);
        var boosted=p.serverLevel().getEntitiesOfClass(InfernoDragon.class,p.getBoundingBox().inflate(24),e->e.getPersistentData().getInt("RoyaleIronLevel")==2);
        c.assertTrue(boosted.size()==1&&Math.abs(boosted.getFirst().getMaxHealth()-57.6)<.01,"Mirror produces the real level-two health and damage scaling");
        c.assertTrue((Object)boosted.getFirst() instanceof io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible,"Dragon participates in native dispelling");
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="inferno-iron-damage")
    public void FireSchoolDamageAppliesSummonEquipmentOnceAndRespectsCancellation(GameTestHelper c){
        var p=player(c);p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE).setBaseValue(1.5);
        var e=RoyaleSpells.INFERNO_DRAGON.create(c.getLevel());e.setup(p.getUUID(),600,false);e.moveTo(p.position().add(0,2,0));e.setNoAi(true);c.getLevel().addFreshEntity(e);IronIntegration.summoned(e,p.getUUID(),"royalespells:inferno_dragon",1);
        var victim=EntityType.COW.create(c.getLevel());victim.moveTo(e.position().add(0,0,2.8));victim.setNoAi(true);victim.setNoGravity(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);victim.setHealth(500);c.getLevel().addFreshEntity(victim);SummonOrders.order(p,victim);e.setTarget(victim);
        for(int i=0;i<28;i++)e.tick();c.assertTrue(Math.abs(victim.getHealth()-498.5)<.01,"A one-point hit is multiplied by 1.5 once, not twice");
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> cancel=event->{if(event.getEntity()==victim)event.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,cancel);
        try{for(int i=0;i<24;i++)e.tick();c.assertTrue(Math.abs(victim.getHealth()-498.5)<.01,"Cancelled incoming damage remains cancelled during the beam");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        victim.discard();cleanup(p);c.succeed();
    }
}
