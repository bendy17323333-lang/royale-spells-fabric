package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import dev.royalespells.elixir.*;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public class ArmyTests {
    private static final Map<UUID,List<ChunkPos>> FORCED=new HashMap<>();
    private ServerPlayer player(GameTestHelper c){
        var p=TestPlayers.create(c);var a=c.absolutePos(new BlockPos(8,14,8));
        var origin=new ChunkPos(a);var forced=new ArrayList<ChunkPos>();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var cp=new ChunkPos(origin.x+x,origin.z+z);if(c.getLevel().setChunkForced(cp.x,cp.z,true))forced.add(cp);}FORCED.put(p.getUUID(),forced);
        for(int x=-10;x<=10;x++)for(int z=-10;z<=16;z++){
            c.getLevel().setBlockAndUpdate(a.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<5;y++)c.getLevel().setBlockAndUpdate(a.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(a));p.setYRot(0);p.setXRot(35);p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(300);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();return p;
    }
    private void cleanup(ServerPlayer p){for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof Summoned s&&p.getUUID().equals(s.ownerId()))e.discard();for(var cp:FORCED.getOrDefault(p.getUUID(),List.of()))p.serverLevel().setChunkForced(cp.x,cp.z,false);FORCED.remove(p.getUUID());p.server.getPlayerList().remove(p);p.discard();}
    private BlockPos pool(GameTestHelper c){var at=c.absolutePos(new BlockPos(5,14,5));TowerPools.basin(c.getLevel(),at,new BoundingBox(at.getX()-3,at.getY()-2,at.getZ()-3,at.getX()+3,at.getY()+4,at.getZ()+3),true);return at;}
    private ItemEntity drop(GameTestHelper c,BlockPos at,ItemStack stack){var i=new ItemEntity(c.getLevel(),at.getX()+.5,at.getY()+.9,at.getZ()+.5,stack);c.getLevel().addFreshEntity(i);return i;}
    private List<ArmySkeleton> army(ServerPlayer p){return p.serverLevel().getEntitiesOfClass(ArmySkeleton.class,p.getBoundingBox().inflate(48),e->p.getUUID().equals(e.ownerId()));}
    @GameTest(template="empty",templateNamespace="royalespells",batch="beta-army-arrival")
    public void nativeArmyCreatesOneDeploymentBurstAndRejectedRepeatDoesNotReplayIt(GameTestHelper c){
        var p=player(c);var area=p.getBoundingBox().inflate(48);var old=new HashSet<UUID>();
        c.getLevel().getEntitiesOfClass(EvolutionBurst.class,area).forEach(e->old.add(e.getUUID()));
        var data=MagicData.getPlayerMagicData(p);data.setMana(300);ArmyMagic.spell().castSpell(p.level(),1,p,CastSource.SPELLBOOK,true);
        var arrivals=c.getLevel().getEntitiesOfClass(EvolutionBurst.class,area,e->!old.contains(e.getUUID()));
        c.assertTrue(army(p).size()==16&&arrivals.size()==1,"Native spellbook summons one army and one arrival effect");
        float mana=data.getMana();ArmyMagic.spell().castSpell(p.level(),1,p,CastSource.SCROLL,true);
        c.assertTrue(c.getLevel().getEntitiesOfClass(EvolutionBurst.class,area,e->!old.contains(e.getUUID())).size()==1&&data.getMana()==mana,"Rejected repeat neither replays VFX nor spends mana");
        arrivals.forEach(Entity::discard);cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-input")
    public void everyNativeAndGraveyardLevelConvertsButUnrelatedScrollsNeverDo(GameTestHelper c){
        var nativeSpell=SpellRegistry.RAISE_DEAD_SPELL.get();for(int level=1;level<=nativeSpell.getMaxLevel();level++){var scroll=new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(nativeSpell,level,scroll);c.assertTrue(ArmyMagic.skeletonScroll(scroll),"Native skeleton summon level "+level);}
        for(int level=1;level<=IronSpellProfile.GRAVEYARD.maxLevel();level++)c.assertTrue(ArmyMagic.skeletonScroll(IronIntegration.scroll(IronSpellProfile.GRAVEYARD,level)),"Graveyard level "+level);
        for(var profile:IronSpellProfile.values())if(profile!=IronSpellProfile.GRAVEYARD)c.assertFalse(ArmyMagic.skeletonScroll(IronIntegration.scroll(profile,1)),"Reject unrelated spell "+profile);
        c.assertFalse(ArmyMagic.skeletonScroll(new ItemStack(Items.PAPER)),"Paper is not a scroll");c.assertFalse(ArmyMagic.spell().allowCrafting()||ArmyMagic.spell().allowLooting(),"New spell is ritual-only");c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-natural",timeoutTicks=180)
    public void naturalSourceProvenanceSurvivesTicksButNotBucketTransport(GameTestHelper c){
        var at=pool(c);c.assertTrue(DarkPoolBlock.natural(c.getLevel().getBlockState(at)),"Generated source is natural");
        c.runAtTickTime(100,()->{
            var state=c.getLevel().getBlockState(at);c.assertTrue(DarkPoolBlock.natural(state),"Natural marker survives actual fluid ticks");
            var bucket=((net.minecraft.world.level.block.BucketPickup)state.getBlock()).pickupBlock(null,c.getLevel(),at,state);c.assertTrue(bucket.is(ElixirContent.DARK.bucket.get()),"Natural source can be harvested");
            ElixirContent.DARK.bucket.get().emptyContents(null,c.getLevel(),at,null);c.assertFalse(DarkPoolBlock.natural(c.getLevel().getBlockState(at)),"Transported oil loses natural provenance");
            var item=drop(c,at,IronIntegration.scroll(IronSpellProfile.GRAVEYARD,1));c.assertFalse(RitualEntity.begin(item,null,at,0),"Player-placed oil cannot convert");item.discard();c.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-ritual",timeoutTicks=200)
    public void actualDroppedScrollRitualProducesExactlyOneNativeEvolvedScroll(GameTestHelper c){
        var at=pool(c);drop(c,at,IronIntegration.scroll(IronSpellProfile.GRAVEYARD,3));
        c.runAtTickTime(30,()->c.assertTrue(c.getLevel().getEntitiesOfClass(RitualEntity.class,new AABB(at).inflate(4)).size()==1,"Dropped scroll starts the actual server ritual"));
        c.runAtTickTime(175,()->{var drops=c.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4));c.assertTrue(drops.size()==1&&ArmyMagic.armyScroll(drops.getFirst().getItem()),"One input becomes one native scroll");c.assertTrue(ArmyMagic.scrollData(drops.getFirst().getItem()).getLevel()==1,"Ritual's unique spell is level one");drops.forEach(Entity::discard);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-refund",timeoutTicks=80)
    public void interruptedAndReloadedRitualReturnsOnlyItsOriginalInputs(GameTestHelper c){
        var at=pool(c);var horn=drop(c,at,new ItemStack(Items.GOAT_HORN));var scroll=drop(c,at,ArmyMagic.evolvedScroll());c.assertTrue(RitualEntity.begin(horn,scroll,at,1),"Fusion starts");
        var old=c.getLevel().getEntitiesOfClass(RitualEntity.class,new AABB(at).inflate(4)).getFirst();var saved=new CompoundTag();old.saveWithoutId(saved);old.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        var reloaded=RoyaleSpells.RITUAL.create(c.getLevel());reloaded.load(saved);c.getLevel().addFreshEntity(reloaded);c.getLevel().setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        c.runAtTickTime(20,()->{var drops=c.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4));c.assertTrue(drops.size()==2&&drops.stream().anyMatch(i->i.getItem().is(Items.GOAT_HORN)&&!ArmyMagic.horn(i.getItem()))&&drops.stream().anyMatch(i->ArmyMagic.armyScroll(i.getItem())),"Abort refunds exactly one horn and one scroll after reload");drops.forEach(Entity::discard);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-fusion",timeoutTicks=200)
    public void onlyEvolvedArmyScrollCanEnchantAHornAndKeepsItsCustomName(GameTestHelper c){
        var at=pool(c);var hornStack=new ItemStack(Items.GOAT_HORN);hornStack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Gerry"));drop(c,at,hornStack);var bad=drop(c,at,IronIntegration.scroll(IronSpellProfile.FIREBALL,1));
        c.runAtTickTime(25,()->{c.assertTrue(c.getLevel().getEntitiesOfClass(RitualEntity.class,new AABB(at).inflate(4)).isEmpty(),"Unrelated spell and horn do not fuse");bad.discard();drop(c,at,ArmyMagic.evolvedScroll());});
        c.runAtTickTime(195,()->{var drops=c.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4));c.assertTrue(drops.size()==1&&ArmyMagic.horn(drops.getFirst().getItem()),"One scroll consumed, one enchanted vanilla goat horn returned");var result=drops.getFirst().getItem();c.assertTrue(result.getHoverName().getString().equals("Gerry")&&result.get(DataComponents.INSTRUMENT).value().soundEvent().value()==ArmySounds.DEPLOY,"Name kept, instrument uses original deployment horn");drops.forEach(Entity::discard);c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-horn",timeoutTicks=80)
    public void hornConsumesNoManaAndSharesArmyLimitAndCooldownWithScrolls(GameTestHelper c){
        var p=player(c);var data=MagicData.getPlayerMagicData(p);data.setMana(0);p.setItemInHand(InteractionHand.OFF_HAND,ArmyMagic.enchantedHorn(new ItemStack(Items.GOAT_HORN)));
        var event=new PlayerInteractEvent.RightClickItem(p,InteractionHand.OFF_HAND);NeoForge.EVENT_BUS.post(event);
        c.assertTrue(event.isCanceled()&&event.getCancellationResult()==InteractionResult.CONSUME,"Actual horn use accepted in off hand");c.assertTrue(data.getMana()==0&&army(p).size()==16,"Horn summons 15+1 with zero mana");
        c.assertTrue(data.getPlayerCooldowns().isOnCooldown(ArmyMagic.spell())&&ArmyLedger.get(p.server).remaining(p.getUUID(),ArmyLedger.now(p.server))>0,"Native HUD and persisted timer share cooldown");
        c.assertFalse(ArmyMagic.spell().attemptInitiateCast(ArmyMagic.evolvedScroll(),1,p.level(),p,CastSource.SCROLL,true,"mainhand"),"Scroll cannot bypass a horn's active army");
        army(p).stream().filter(ArmySkeleton::general).findFirst().orElseThrow().discard();
        c.assertFalse(ArmyMagic.spell().checkPreCastConditions(p.level(),1,p,data),"General death does not clear shared cooldown");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-spell",timeoutTicks=80)
    public void standaloneScrollAndSpellbookUseTheSameSummonAndTimer(GameTestHelper c){
        var p=player(c);var data=MagicData.getPlayerMagicData(p);data.setMana(300);ArmyMagic.spell().castSpell(p.level(),1,p,CastSource.SPELLBOOK,true);
        c.assertTrue(army(p).size()==16&&Math.abs(data.getMana()-175)<.01,"Book spell charges its 125 mana exactly once");
        p.setItemInHand(InteractionHand.MAIN_HAND,ArmyMagic.enchantedHorn(new ItemStack(Items.GOAT_HORN)));var horn=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.post(horn);c.assertTrue(horn.getCancellationResult()==InteractionResult.FAIL&&army(p).size()==16,"Horn cannot bypass a book cast");
        cleanup(p);var q=player(c);var d=MagicData.getPlayerMagicData(q);d.setMana(0);ArmyMagic.spell().castSpell(q.level(),1,q,CastSource.SCROLL,true);c.assertTrue(army(q).size()==16&&d.getMana()==0,"One-use native scroll casts independently without a horn");cleanup(q);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-ghost",timeoutTicks=240)
    public void shieldGhostsAndGeneralDeathFollowTheOriginalCounterplay(GameTestHelper c){
        var p=player(c);ArmyMagic.spell().onCast(p.level(),1,p,CastSource.NONE,MagicData.getPlayerMagicData(p));var list=army(p);c.assertTrue(list.size()==16,"Formation spawned");var g=list.stream().filter(ArmySkeleton::general).findFirst().orElseThrow();var s=list.stream().filter(e->!e.general()).findFirst().orElseThrow();
        list.forEach(e->{e.setNoAi(true);e.setNoGravity(true);});s.hurt(p.damageSources().generic(),100);c.assertTrue(s.isAlive()&&s.ghost()&&!s.isAttackable(),"Lethal attack transforms a supported skeleton into an untargetable ghost");c.assertFalse(s.hurt(p.damageSources().generic(),1000),"Ghost is indestructible");
        float hp=g.getHealth();g.hurt(p.damageSources().generic(),100);c.assertTrue(g.isAlive()&&g.getHealth()==hp&&g.shield()==0,"Shield absorbs its entire breaking hit without overflow");g.hurt(p.damageSources().generic(),100);c.assertFalse(ArmyLedger.get(p.server).active(p.getUUID(),ArmyLedger.now(p.server)),"General death releases the army lease");
        c.succeedWhen(()->{c.assertTrue(g.isRemoved()&&s.isRemoved(),"General and spectral soldier finish their death/dissolve");c.assertTrue(list.stream().filter(e->e!=g&&e!=s).allMatch(e->e.isAlive()&&e.dissolve()==0&&!e.supported()),"All fourteen living soldiers survive without ghost support");cleanup(p);});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-ledger")
    public void armyLeaseAndCooldownSurviveSerializationAndEntityUnloading(GameTestHelper c){
        var p=player(c);ArmyMagic.spell().onCast(p.level(),1,p,CastSource.NONE,MagicData.getPlayerMagicData(p));var g=army(p).stream().filter(ArmySkeleton::general).findFirst().orElseThrow();var ledger=ArmyLedger.get(p.server);long now=ArmyLedger.now(p.server);var copy=ArmyLedger.load(ledger.save(new CompoundTag(),p.registryAccess()),p.registryAccess());
        c.assertTrue(copy.active(p.getUUID(),now)&&copy.remaining(p.getUUID(),now)>0,"Reload retains active army and cooldown");g.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);c.assertTrue(ledger.active(p.getUUID(),now),"Unloaded general is still an active army");ledger.finish(p.getUUID(),g.armyId());c.assertFalse(ledger.active(p.getUUID(),now),"Explicit removal releases army");c.assertTrue(ledger.remaining(p.getUUID(),now)>0,"Removal retains cooldown");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-two-owners")
    public void twoPlayersMayEachOwnAnArmyButCooldownExpiryCannotDuplicateOne(GameTestHelper c){
        var p=player(c);ArmyMagic.spell().onCast(p.level(),1,p,CastSource.NONE,MagicData.getPlayerMagicData(p));
        var q=player(c);q.setPos(q.position().add(7,0,0));ArmyMagic.spell().onCast(q.level(),1,q,CastSource.NONE,MagicData.getPlayerMagicData(q));
        c.assertTrue(army(p).size()==16&&army(q).size()==16,"Army cap belongs to the owner, not the entire battlefield");
        var ledger=ArmyLedger.get(p.server);var entry=ledger.entry(p.getUUID());ledger.start(p.getUUID(),entry.army(),entry.general(),entry.expires(),0);MagicData.getPlayerMagicData(p).getPlayerCooldowns().removeCooldown(ArmyMagic.spell().getSpellId());
        c.assertFalse(ArmyMagic.spell().checkPreCastConditions(p.level(),1,p,MagicData.getPlayerMagicData(p)),"An active army remains a lock even after cooldown ends");cleanup(q);cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="army-attack",timeoutTicks=260)
    public void generalUsesAWindupThenOneDamagingStaffThrust(GameTestHelper c){
        var p=player(c);ArmyMagic.spell().onCast(p.level(),1,p,CastSource.NONE,MagicData.getPlayerMagicData(p));var list=army(p);list.forEach(e->e.setNoAi(true));var general=list.stream().filter(ArmySkeleton::general).findFirst().orElseThrow();
        var victim=EntityType.HUSK.create(p.level());victim.setNoAi(true);victim.moveTo(general.position().add(0,0,1.3));victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);c.getLevel().addFreshEntity(victim);int[] start={-1};
        c.succeedWhen(()->{
            c.assertTrue(general.age()>=20,"Wait for the actual entity's deployment animation");
            if(start[0]<0){c.assertTrue(general.beginStrike(victim),"Staff attack begins");start[0]=general.age();}
            int elapsed=general.age()-start[0];if(elapsed<5)c.assertTrue(victim.getHealth()==100,"Windup cannot damage early");
            c.assertTrue(elapsed>=20,"Wait for the complete attack");c.assertTrue(Math.abs(victim.getHealth()-97)<.02,"One thrust inflicts its converted 3 damage exactly once");
            victim.discard();cleanup(p);
        });
    }
}
