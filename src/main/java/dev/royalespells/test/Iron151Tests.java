package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public class Iron151Tests {
    private ServerPlayer player(GameTestHelper c){var p=TestPlayers.create(c);var at=c.absolutePos(new BlockPos(5,15,5));for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)c.getLevel().setBlockAndUpdate(at.offset(x,-1,z),Blocks.STONE.defaultBlockState());p.setPos(Vec3.atBottomCenterOf(at));p.setXRot(55);p.setYRot(0);p.setGameMode(GameType.SURVIVAL);p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(300);MagicData.getPlayerMagicData(p).setMana(300);return p;}
    private void cleanup(ServerPlayer p){for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof SpellEntity fx&&p.getUUID().equals(fx.ownerId)||e instanceof Summoned s&&p.getUUID().equals(s.ownerId()))e.discard();p.server.getPlayerList().remove(p);p.discard();}
    @GameTest(template="empty",templateNamespace="royalespells",batch="151-control",timeoutTicks=350)
    public void freezeVinesCardsScrollsAndMirrorShareOnePersistentFifteenSecondPool(GameTestHelper c){
        var p=player(c);var freeze=IronIntegration.spell(IronSpellProfile.FREEZE);var vines=IronIntegration.spell(IronSpellProfile.VINES);var mirror=IronIntegration.spell(IronSpellProfile.MIRROR);var data=MagicData.getPlayerMagicData(p);
        freeze.castSpell(p.level(),1,p,CastSource.SCROLL,true);
        c.assertTrue(ControlCooldown.remaining(p)==300,"Successful one-use scroll starts a 15 second pool");
        c.assertTrue(data.getPlayerCooldowns().isOnCooldown(freeze)&&data.getPlayerCooldowns().isOnCooldown(vines),"Both native cooldown indicators start together");
        float mana=data.getMana();vines.castSpell(p.level(),1,p,CastSource.SPELLBOOK,true);c.assertTrue(data.getMana()==mana,"Rejected second control does not charge mana");
        c.assertFalse(SpellEngine.cast(p,Spell.VINES),"Switching to card cannot bypass the native spell's cooldown");
        data.getPlayerCooldowns().removeCooldown(freeze.getSpellId());data.getPlayerCooldowns().removeCooldown(vines.getSpellId());
        c.assertFalse(mirror.attemptInitiateCast(ItemStack.EMPTY,1,p.level(),p,CastSource.SPELLBOOK,true,"mainhand"),"Mirror cannot bypass the persisted group even with no individual cooldown entries");
        var loaded=ControlCooldown.load(ControlCooldown.get(p.server).save(new net.minecraft.nbt.CompoundTag(),p.registryAccess()),p.registryAccess());
        c.assertTrue(loaded.save(new net.minecraft.nbt.CompoundTag(),p.registryAccess()).getList("Cooldowns",10).size()>0,"Pool survives serialization");
        c.runAtTickTime(310,()->{c.assertTrue(ControlCooldown.remaining(p)==0,"Pool expires after 15 seconds");vines.castSpell(p.level(),1,p,CastSource.SCROLL,true);c.assertTrue(ControlCooldown.remaining(p)==300,"Vines can then cast and restarts the same pool");c.assertFalse(freeze.checkPreCastConditions(p.level(),1,p,data),"Vines also locks Freeze in the opposite direction");cleanup(p);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="151-freeze-duration",timeoutTicks=135)
    public void actualFreezeFieldControlsForFiveSecondsThenReleases(GameTestHelper c){
        var p=player(c);var at=p.position().add(2,0,0);var victim=EntityType.COW.create(c.getLevel());victim.setPos(at);victim.setNoAi(true);victim.setNoGravity(true);c.getLevel().addFreshEntity(victim);
        var cp=new ChunkPos(victim.blockPosition());boolean forced=c.getLevel().setChunkForced(cp.x,cp.z,true);
        var fx=SpellEntity.create(c.getLevel(),Spell.FREEZE,p.getUUID(),at,at);fx.setIronSpell("royalespells:freeze",1,((RoyaleIronSpell)IronIntegration.spell(IronSpellProfile.FREEZE)).durationScale(1,p));c.getLevel().addFreshEntity(fx);
        c.runAtTickTime(95,()->c.assertTrue(victim.hasEffect(RoyaleSpells.FROZEN)&&victim.hasEffect(RoyaleSpells.STUN),"Freeze is still active near 5 seconds"));
        c.runAtTickTime(115,()->{c.assertFalse(victim.hasEffect(RoyaleSpells.FROZEN)||victim.hasEffect(RoyaleSpells.STUN),"Freeze ends and movement can resume");victim.discard();cleanup(p);if(forced)c.getLevel().setChunkForced(cp.x,cp.z,false);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="151-native-skeleton")
    public void nativeIronSkeletonGetsSpeedOrdersAndNoArrowKnockback(GameTestHelper c){
        var p=player(c);var skeleton=new io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton(c.getLevel(),p,false);skeleton.setPos(p.position().add(0,0,2));skeleton.setNoAi(true);c.getLevel().addFreshEntity(skeleton);
        c.assertTrue(Math.abs(skeleton.getAttributeValue(Attributes.MOVEMENT_SPEED)-skeleton.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()*1.25)<.0001,"Native summoned skeleton gains 25 percent speed");
        var victim=EntityType.COW.create(c.getLevel());victim.setPos(skeleton.position().add(1,0,0));victim.setNoAi(true);victim.setOnGround(true);c.getLevel().addFreshEntity(victim);
        skeleton.setTarget(victim);c.assertTrue(skeleton.getTarget()==null,"Native skeleton does not proactively select passive cattle");SummonOrders.order(p,victim);skeleton.tickCount=10;SummonOrders.tick(skeleton,p.getUUID());c.assertTrue(skeleton.getTarget()==victim,"Owner's attack order reaches native skeleton AI");
        var arrow=EntityType.ARROW.create(c.getLevel());arrow.setOwner(skeleton);victim.setDeltaMovement(Vec3.ZERO);victim.hurt(c.getLevel().damageSources().arrow(arrow,skeleton),2);
        c.assertTrue(victim.getDeltaMovement().lengthSqr()<1e-10,"Arrow damage from the native skeleton has no hurt impulse");skeleton.discard();victim.discard();cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="151-iron-impact")
    public void nativeRoyaleDamagePipelineKeepsDamageWithoutPushing(GameTestHelper c){
        var p=player(c);var victim=EntityType.COW.create(c.getLevel());var at=p.position().add(1,0,0);victim.setPos(at);victim.setOnGround(true);c.getLevel().addFreshEntity(victim);
        var fx=SpellEntity.create(c.getLevel(),Spell.ZAP,p.getUUID(),at,at);fx.setIronSpell("royalespells:zap",1,1);victim.setDeltaMovement(Vec3.ZERO);float hp=victim.getHealth();IronSpellSystem.damage(fx,victim,4);
        c.assertTrue(hp-victim.getHealth()==4&&victim.getDeltaMovement().lengthSqr()<1e-10,"Native magic damage remains 4 without knockback");
        c.assertTrue(ArmyMagic.spell().getSpellCooldown()==300,"Army base cooldown is exactly 15 seconds even with the old world's cached config");victim.discard();fx.discard();cleanup(p);c.succeed();
    }
}
