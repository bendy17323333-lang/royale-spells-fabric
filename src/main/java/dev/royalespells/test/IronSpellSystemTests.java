package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
import java.util.function.Consumer;

/** Integration tests exercise the real Iron's menus, registry, casting manager and event pipeline. */
@PrefixGameTestTemplate(false)
public class IronSpellSystemTests {
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static ItemStack ink(SpellRarity rarity){return new ItemStack(InkItem.getInkForRarity(rarity));}
    private static ServerPlayer player(GameTestHelper c) {
        var player=TestPlayers.create(c);var p=c.absolutePos(new BlockPos(2,15,2));
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++) {
            c.getLevel().setBlockAndUpdate(p.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<6;y++)c.getLevel().setBlockAndUpdate(p.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        player.setPos(Vec3.atBottomCenterOf(p));player.setXRot(60);player.setYRot(0);return player;
    }
    private static void cleanup(ServerPlayer player) {
        for(var e:com.google.common.collect.ImmutableList.copyOf(player.serverLevel().getAllEntities()))if(e instanceof SpellEntity effect && player.getUUID().equals(effect.ownerId)
            || e instanceof Summoned summon && player.getUUID().equals(summon.ownerId()))e.discard();
        player.server.getPlayerList().remove(player);player.discard();
    }
    private static SpellData data(ItemStack stack){return ISpellContainer.get(stack).getSpellAtIndex(0);}
    private static void finish(ServerPlayer player,int limit) {
        var manager=new MagicManager();
        for(int i=0;i<limit && MagicData.getPlayerMagicData(player).isCasting();i++)manager.tick(player.serverLevel());
    }
    private static Mob dummy(GameTestHelper c,Vec3 at) {
        var mob=EntityType.PIG.create(c.getLevel());mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);c.getLevel().addFreshEntity(mob);return mob;
    }
    private static boolean begin(ServerPlayer p,AbstractSpell spell,int level,CastSource source) {
        return spell.attemptInitiateCast(ItemStack.EMPTY,level,p.level(),p,source,true,"mainhand");
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-registry")
    public void all29SpellsHaveNativeConfigAndLootPools(GameTestHelper c) {
        var pool=new SpellFilter().getApplicableSpells();int count=0;
        for(var profile:IronSpellProfile.values()) {
            var spell=IronIntegration.spell(profile);
            c.assertTrue(SpellRegistry.getSpell(RoyaleSpells.id(profile.id()))==spell,"Native registry: "+profile);
            c.assertTrue(spell.isEnabled() && (profile==IronSpellProfile.SKELETON_ARMY_EVOLUTION?!spell.allowCrafting() && !pool.contains(spell):spell.allowCrafting() && pool.contains(spell)),"Crafting and real random loot pool: "+profile);
            c.assertTrue(spell.getMaxLevel()==profile.maxLevel() && spell.getRarity(1)==profile.rarity,"Config and minimum rarity: "+profile);
            c.assertTrue(SpellRegistry.getSpellsForSchool(spell.getSchoolType()).contains(spell),"School focus discovery: "+profile);
            c.assertTrue(profile==IronSpellProfile.SKELETON_ARMY_EVOLUTION?!new SpellFilter(spell.getSchoolType()).getApplicableSpells().contains(spell):new SpellFilter(spell.getSchoolType()).getApplicableSpells().contains(spell),"School-specific loot discovery: "+profile);
            for(int level=1;level<=spell.getMaxLevel();level++) {
                var scroll=IronIntegration.scroll(profile,level);var value=data(scroll);
                c.assertTrue(value.getSpell()==spell && value.getLevel()==level,"Native scroll stores exact level: "+profile+" "+level);
                c.assertTrue(value.getRarity().getValue()>=profile.rarity.getValue(),"Rarity never drops below minimum");
            }
            count++;
        }
        c.assertTrue(count==31,"All 31 native profiles, including the ritual army and Inferno Dragon, are registered");c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-forge",timeoutTicks=160)
    public void forgeEverySpellWithItsNativeInkAndFocus(GameTestHelper c) {
        var p=player(c);var at=p.blockPosition().offset(4,0,0);
        c.getLevel().setBlockAndUpdate(at,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("irons_spellbooks:scroll_forge")).defaultBlockState());
        var menu=new ScrollForgeMenu(10,p.getInventory(),c.getLevel().getBlockEntity(at));
        for(var profile:IronSpellProfile.values()) {
            var spell=IronIntegration.spell(profile);if(!spell.allowCrafting())continue;
            var focus=BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(stack->spell.getSchoolType().isFocus(stack)).findFirst().orElseThrow();
            for(var rarity:SpellRarity.values()) {
                int level=spell.getMinLevelForRarity(rarity);if(level<1 || level>spell.getMaxLevel())continue;
                menu.getInkSlot().set(ink(rarity));menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER));menu.getFocusSlot().set(focus.copy());menu.setRecipeSpell(spell);
                var result=menu.getResultSlot().getItem();c.assertFalse(result.isEmpty(),"Real forge output: "+profile+" "+rarity);
                c.assertTrue(data(result).getSpell()==spell && data(result).getLevel()==level,"Forge result level follows ink rarity");
                menu.getResultSlot().onTake(p,result.copy());
                c.assertTrue(menu.getInkSlot().getItem().isEmpty() && menu.getBlankScrollSlot().getItem().isEmpty() && menu.getFocusSlot().getItem().isEmpty(),"Forge consumes all three materials");
            }
        }
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-inscription")
    public void inscribeAndExtractEveryRoyalSpell(GameTestHelper c) {
        var p=player(c);var menu=new InscriptionTableMenu(11,p.getInventory(),ContainerLevelAccess.NULL);
        for(var profile:IronSpellProfile.values()) {
            var spell=IronIntegration.spell(profile);int level=spell.getMaxLevel();
            menu.getSpellBookSlot().set(item("irons_spellbooks:iron_spell_book"));menu.getScrollSlot().set(IronIntegration.scroll(profile,level));
            menu.clickMenuButton(p,0);menu.clickMenuButton(p,-1);
            var book=menu.getSpellBookSlot().getItem();c.assertTrue(data(book).getSpell()==spell && data(book).getLevel()==level,"Real inscription retains level: "+profile);
            c.assertTrue(menu.getScrollSlot().getItem().isEmpty(),"Inscription consumes the scroll");
            menu.setSelectedSpell(0);var extracted=menu.getResultSlot().getItem().copy();
            c.assertTrue(data(extracted).getSpell()==spell && data(extracted).getLevel()==level,"Extraction result: "+profile);
            menu.getResultSlot().onTake(p,extracted);
            c.assertTrue(ISpellContainer.get(book).isEmpty(),"Extracted spell removed from book; no duplication");
        }
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-anvil")
    public void upgradeEveryScrollThroughEveryLevel(GameTestHelper c) {
        var p=player(c);var menu=new ArcaneAnvilMenu(12,p.getInventory(),ContainerLevelAccess.NULL);
        for(var profile:IronSpellProfile.values()) {
            var spell=IronIntegration.spell(profile);
            for(int level=1;level<spell.getMaxLevel();level++) {
                menu.getSlot(0).set(IronIntegration.scroll(profile,level));menu.getSlot(1).set(ink(spell.getRarity(level+1)));menu.createResult();
                var output=menu.getSlot(2).getItem();
                c.assertFalse(output.isEmpty(),"Native arcane anvil upgrade: "+profile+" "+level);
                c.assertTrue(data(output).getLevel()==level+1 && data(output).getSpell()==spell,"Upgrade is exactly one level");
            }
            menu.getSlot(0).set(IronIntegration.scroll(profile,spell.getMaxLevel()));menu.getSlot(1).set(ink(SpellRarity.LEGENDARY));menu.createResult();
            c.assertTrue(menu.getSlot(2).getItem().isEmpty(),"Native maximum level stops upgrades: "+profile);
        }
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-imbue")
    public void nativeWeaponImbuementRetainsAllSpellLevels(GameTestHelper c) {
        var p=player(c);var menu=new ArcaneAnvilMenu(13,p.getInventory(),ContainerLevelAccess.NULL);
        for(var profile:IronSpellProfile.values()) {
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));menu.getSlot(1).set(IronIntegration.scroll(profile,Math.min(2,profile.maxLevel())));menu.createResult();
            var output=menu.getSlot(2).getItem();c.assertFalse(output.isEmpty(),"Native sword imbuement: "+profile);
            c.assertTrue(data(output).getSpell()==IronIntegration.spell(profile) && data(output).getLevel()==Math.min(2,profile.maxLevel()),"Imbued spell and level retained");
        }
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-casting")
    public void spellbookConsumesManaAndUsesNativeCooldown(GameTestHelper c) {
        var p=player(c);p.setGameMode(GameType.SURVIVAL);var magic=MagicData.getPlayerMagicData(p);magic.setMana(100);
        var spell=IronIntegration.spell(IronSpellProfile.ZAP);
        c.assertTrue(begin(p,spell,1,CastSource.SPELLBOOK),"Book casting begins through native lifecycle");finish(p,5);
        c.assertTrue(magic.getMana()<=81 && magic.getMana()>=80,"Only native Zap mana cost is charged: "+magic.getMana());
        c.assertTrue(magic.getPlayerCooldowns().isOnCooldown(spell),"Native spell cooldown is active");
        c.assertFalse(begin(p,spell,1,CastSource.SPELLBOOK),"Cooldown rejects immediate reuse");
        c.assertTrue(p.serverLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(40),e->p.getUUID().equals(e.ownerId)&&e.ironSpellId().equals(spell.getSpellId())).size()==1,"Native cast creates one attributed spell effect");
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-scroll")
    public void consumableScrollUsesNoManaAndNoCooldown(GameTestHelper c) {
        var p=player(c);p.setGameMode(GameType.SURVIVAL);var magic=MagicData.getPlayerMagicData(p);magic.setMana(0);
        var scroll=IronIntegration.scroll(IronSpellProfile.ZAP,2);p.setItemInHand(InteractionHand.MAIN_HAND,scroll);
        scroll.use(p.level(),p,InteractionHand.MAIN_HAND);finish(p,5);
        c.assertTrue(scroll.isEmpty(),"Native one-use scroll is consumed");
        c.assertTrue(magic.getMana()<=1,"Scroll does not require or consume mana");
        c.assertFalse(magic.getPlayerCooldowns().isOnCooldown(IronIntegration.spell(IronSpellProfile.ZAP)),"Scroll uses native cooldown exemption");
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-permissions")
    public void insufficientManaStunAndCancelledCastHaveNoEffects(GameTestHelper c) {
        var p=player(c);p.setGameMode(GameType.SURVIVAL);var magic=MagicData.getPlayerMagicData(p);var spell=IronIntegration.spell(IronSpellProfile.ZAP);magic.setMana(0);
        c.assertFalse(begin(p,spell,1,CastSource.SPELLBOOK),"No mana cannot cast");magic.setMana(100);
        SpellEngine.stun(p,20);c.assertFalse(begin(p,spell,1,CastSource.SPELLBOOK),"Royal stun prevents native casts");p.removeEffect(RoyaleSpells.STUN);
        Consumer<SpellPreCastEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,cancel);
        try {c.assertFalse(begin(p,spell,1,CastSource.SPELLBOOK),"Other mods can cancel casting");}
        finally {NeoForge.EVENT_BUS.unregister(cancel);}
        c.assertTrue(magic.getMana()==100 && !magic.getPlayerCooldowns().hasCooldownsActive(),"Failed casts charge nothing");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-damage")
    public void schoolPowerResistanceAndDamageEventsAffectActualHits(GameTestHelper c) {
        var p=player(c);var spell=(RoyaleIronSpell)IronIntegration.spell(IronSpellProfile.ZAP);
        p.getAttribute(AttributeRegistry.SPELL_POWER).setBaseValue(1.5);p.getAttribute(AttributeRegistry.LIGHTNING_SPELL_POWER).setBaseValue(2);
        c.assertTrue(Math.abs(spell.power(1,p)-3)<.001,"Both native equipment power attributes multiply spell effects");
        var at=p.position().add(3,0,0);var target=dummy(c,at);
        var fx=SpellEntity.create(p.serverLevel(),Spell.ZAP,p.getUUID(),at,at);fx.setIronSpell(spell.getSpellId(),1,1);fx.setPower(spell.power(1,p));
        IronSpellSystem.damage(fx,target,4*fx.power());float normal=100-target.getHealth();c.assertTrue(normal>0,"Damage applies");
        target.setHealth(100);target.invulnerableTime=0;target.getAttribute(AttributeRegistry.LIGHTNING_MAGIC_RESIST).setBaseValue(2);IronSpellSystem.damage(fx,target,4*fx.power());
        c.assertTrue(100-target.getHealth()<normal,"Native school resistance reduces actual damage");
        Consumer<SpellDamageEvent> cancel=e->{if(e.getEntity()==target)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,cancel);
        try {float hp=target.getHealth();IronSpellSystem.damage(fx,target,100);c.assertTrue(hp==target.getHealth(),"SpellDamageEvent cancellation is respected");}
        finally {NeoForge.EVENT_BUS.unregister(cancel);}
        target.discard();cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-summons")
    public void summonsRaiseNativeEventsAndUseSummonDamageEquipment(GameTestHelper c) {
        var p=player(c);var at=p.position().add(3,0,0);var count=new int[1];
        Consumer<SpellSummonEvent> listener=e->{if(e.getCaster()==p)count[0]++;};NeoForge.EVENT_BUS.addListener(listener);
        Mob skeleton;
        try {skeleton=SpellEngine.summon(p.serverLevel(),p.getUUID(),at,"skeleton",false);IronSpellSystem.summon(skeleton,p.getUUID(),"royalespells:graveyard",3);}
        finally {NeoForge.EVENT_BUS.unregister(listener);}
        c.assertTrue(count[0]==1,"Exactly one native summon event");skeleton.setNoAi(true);var target=dummy(c,at.add(1,0,0));
        p.getAttribute(AttributeRegistry.SUMMON_DAMAGE).setBaseValue(1);skeleton.doHurtTarget(target);float base=100-target.getHealth();target.setHealth(100);
        p.getAttribute(AttributeRegistry.SUMMON_DAMAGE).setBaseValue(2);skeleton.doHurtTarget(target);
        c.assertTrue(Math.abs((100-target.getHealth())-base*2)<.01,"Summon damage equipment affects real skeleton attacks");target.discard();cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-mirror")
    public void mirrorCopiesRoyalSpellOneLevelHigherWithoutElixirOrRecursion(GameTestHelper c) {
        var p=player(c);p.setGameMode(GameType.SURVIVAL);var magic=MagicData.getPlayerMagicData(p);magic.setMana(250);
        var zap=IronIntegration.spell(IronSpellProfile.ZAP);var mirror=IronIntegration.spell(IronSpellProfile.MIRROR);
        c.assertFalse(begin(p,mirror,1,CastSource.SPELLBOOK),"Mirror needs a successful source cast");
        c.assertTrue(begin(p,zap,1,CastSource.SPELLBOOK),"Original cast");finish(p,5);float mana=magic.getMana();
        c.assertTrue(begin(p,mirror,1,CastSource.SPELLBOOK),"Mirror can repeat a source on cooldown");finish(p,5);
        c.assertTrue(magic.getMana()>=mana-52 && magic.getMana()<=mana-51,"Mirror charges level-2 Zap plus surcharge once: "+mana+" -> "+magic.getMana());
        c.assertTrue(MirrorIronSpell.history(p).getLevel()==1,"Mirrored result does not overwrite original source level");
        c.assertTrue(magic.getPlayerCooldowns().isOnCooldown(mirror),"Mirror has its own native cooldown");
        c.assertTrue(p.serverLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(40),e->p.getUUID().equals(e.ownerId)&&e.ironLevel()==2).size()==1,"Mirrored level reaches the real effect");
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-native-mirror")
    public void mirrorDelegatesNativeIronProjectileAndChannelledLifecycle(GameTestHelper c) {
        var p=player(c);var nativeSpell=SpellRegistry.FIREBOLT_SPELL.get();var mirror=IronIntegration.spell(IronSpellProfile.MIRROR);
        c.assertTrue(begin(p,nativeSpell,2,CastSource.SPELLBOOK),"Native Iron's original cast");finish(p,120);
        c.assertTrue(begin(p,mirror,1,CastSource.SPELLBOOK),"Mirror delegates native spell");
        var magic=MagicData.getPlayerMagicData(p);c.assertTrue(magic.getCastingSpellId().equals(nativeSpell.getSpellId()) && magic.getCastingSpellLevel()==3,"Original native cast lifecycle at +1");finish(p,120);
        var electrocute=SpellRegistry.getSpell("irons_spellbooks:electrocute");
        c.assertTrue(begin(p,electrocute,1,CastSource.SPELLBOOK),"Original continuous spell");finish(p,200);
        c.assertTrue(begin(p,mirror,1,CastSource.SPELLBOOK),"Mirror delegates continuous spell");
        c.assertTrue(magic.getCastingSpell().getSpell().getCastType()==CastType.CONTINUOUS && magic.getCastingSpellLevel()==2,"Continuous casting is preserved, not one onCast call");finish(p,200);
        c.assertFalse(magic.isCasting(),"Mirrored channel finishes normally");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-all-casts")
    public void everyRoyalSpellUsesNativeCastingAndPersistsItsOrigin(GameTestHelper c) {
        var p=player(c);var magic=MagicData.getPlayerMagicData(p);
        for(var profile:IronSpellProfile.values()) {
            if(profile==IronSpellProfile.MIRROR || profile==IronSpellProfile.SKELETON_ARMY_EVOLUTION)continue;
            // Each case is an independent cast; shared-pool timing has its own regression test.
            ControlCooldown.clear(p);
            var spell=IronIntegration.spell(profile);c.assertTrue(begin(p,spell,2,CastSource.SPELLBOOK),"Native cast starts: "+profile);finish(p,120);
            if(profile==IronSpellProfile.BARBARIAN_HUT) {
                c.assertTrue(p.serverLevel().getEntitiesOfClass(RoyaleUnit.class,p.getBoundingBox().inflate(40),e->e.getPersistentData().getString("RoyaleIronSpell").equals(spell.getSpellId())).size()==1,"Native building deploys");
            } else if(profile==IronSpellProfile.INFERNO_DRAGON) {
                var dragons=p.serverLevel().getEntitiesOfClass(InfernoDragon.class,p.getBoundingBox().inflate(40),e->p.getUUID().equals(e.ownerId()));
                c.assertTrue(dragons.size()==1&&dragons.getFirst().getPersistentData().getString("RoyaleIronSpell").equals(spell.getSpellId())&&dragons.getFirst().getPersistentData().getInt("RoyaleIronLevel")==2,"Native dragon cast preserves level and attribution");
                dragons.forEach(Entity::discard);
            } else {
                var effects=p.serverLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(40),e->p.getUUID().equals(e.ownerId)&&e.ironSpellId().equals(spell.getSpellId()));
                c.assertTrue(effects.size()==(profile==IronSpellProfile.GOBLIN_BARREL_EVOLUTION?2:1),"Correct attributed effect count: "+profile);
                var original=effects.getFirst();var nbt=new net.minecraft.nbt.CompoundTag();original.saveWithoutId(nbt);var copy=RoyaleSpells.SPELL.create(p.level());copy.load(nbt);
                c.assertTrue(copy.ironLevel()==2 && copy.ironSpellId().equals(spell.getSpellId()) && copy.power()==original.power() && copy.duration()==original.duration(),"NBT retains native scaling and source: "+profile);
                effects.forEach(Entity::discard);
            }
        }
        magic.getPlayerRecasts().removeAll(io.redspace.ironsspellbooks.capabilities.magic.RecastResult.COMMAND);cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-recasts")
    public void mirrorRetainsAllNativeRecastsAndPaysOnce(GameTestHelper c) {
        var p=player(c);var magic=MagicData.getPlayerMagicData(p);var barrage=SpellRegistry.getSpell("irons_spellbooks:flaming_barrage");
        for(int i=0;i<5;i++){c.assertTrue(begin(p,barrage,1,CastSource.SPELLBOOK),"Original native recast "+i);finish(p,5);}
        p.setGameMode(GameType.SURVIVAL);p.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(500);p.getAttribute(AttributeRegistry.MANA_REGEN).setBaseValue(0);magic.setMana(500);
        var mirror=IronIntegration.spell(IronSpellProfile.MIRROR);
        for(int i=0;i<5;i++){c.assertTrue(begin(p,mirror,1,CastSource.SPELLBOOK),"Mirrored native recast "+i);finish(p,5);}
        float expected=500-barrage.getManaCost(2)-mirror.getManaCost(1);
        c.assertTrue(Math.abs(magic.getMana()-expected)<.01,"Five shots cost the upgraded original plus surcharge once, without tick-phase regeneration: "+magic.getMana()+" expected="+expected);
        c.assertFalse(magic.getPlayerRecasts().hasRecastForSpell(barrage),"All mirrored shots are consumed");
        c.assertTrue(MirrorIronSpell.history(p).getLevel()==1,"Recasts do not recursively increase source level");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-hero")
    public void heroAbilityUsesNativeRecastWithoutSpawningAnotherHero(GameTestHelper c) {
        var p=player(c);var spell=IronIntegration.spell(IronSpellProfile.BARBARIAN_BARREL_HERO);var magic=MagicData.getPlayerMagicData(p);
        c.assertTrue(begin(p,spell,1,CastSource.SPELLBOOK),"Deploy hero");finish(p,80);
        c.assertTrue(magic.getPlayerRecasts().getRemainingRecastsForSpell(spell)==1,"One native ability recast");
        var roll=p.serverLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(40),e->p.getUUID().equals(e.ownerId)).getFirst();
        for(int i=0;i<24;i++)roll.tick();
        var heroes=p.serverLevel().getEntitiesOfClass(AllyZombie.class,p.getBoundingBox().inflate(40),e->e.hero&&p.getUUID().equals(e.ownerId()));
        c.assertTrue(heroes.size()==1,"Exactly one hero deployed");var hero=heroes.getFirst();hero.setHealth(4);
        c.assertTrue(begin(p,spell,1,CastSource.SPELLBOOK),"Native recast activates rolling ability");finish(p,80);
        var ability=p.serverLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(40),e->e.reroll&&p.getUUID().equals(e.ownerId)).getFirst();
        for(int i=0;i<24;i++)ability.tick();
        c.assertTrue(hero.getHealth()>4 && !magic.getPlayerRecasts().hasRecastForSpell(spell),"Ability heals hero and consumes recast");
        c.assertTrue(p.serverLevel().getEntitiesOfClass(AllyZombie.class,p.getBoundingBox().inflate(40),e->e.hero&&p.getUUID().equals(e.ownerId())).size()==1,"Ability cannot duplicate hero");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-counterspell")
    public void nativeCounterspellHonorsCancellationAndDispelsRoyalSummons(GameTestHelper c) {
        var p=player(c);p.setXRot(0);var summon=SpellEngine.summon(p.serverLevel(),p.getUUID(),p.position().add(0,0,4),"barbarian",false);summon.setNoAi(true);
        var counter=SpellRegistry.getSpell("irons_spellbooks:counterspell");var magic=MagicData.getPlayerMagicData(p);
        Consumer<CounterSpellEvent> cancel=e->{if(e.caster==p)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,cancel);
        try {counter.onCast(p.level(),1,p,CastSource.COMMAND,magic);c.assertFalse(summon.isRemoved(),"Other mods may cancel dispelling");}
        finally {NeoForge.EVENT_BUS.unregister(cancel);}
        counter.onCast(p.level(),1,p,CastSource.COMMAND,magic);c.assertTrue(summon.isRemoved(),"Real native Counterspell ray removes Royal summon");
        c.assertTrue(RoyaleSpells.RAGED.value() instanceof io.redspace.ironsspellbooks.effect.MagicMobEffect,"Royal status participates in native dispelling");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-interruption")
    public void nonElectricHardStunInterruptsNativeChannellingAndMirrorRespectsLearning(GameTestHelper c) {
        var p=player(c);var magic=MagicData.getPlayerMagicData(p);var electrocute=SpellRegistry.getSpell("irons_spellbooks:electrocute");
        c.assertTrue(begin(p,electrocute,1,CastSource.SPELLBOOK),"Begin native channel");
        c.assertTrue(magic.isCasting(),"Channel is actually active");SpellEngine.stun(p,10);
        c.assertFalse(magic.isCasting(),"Royal stun cancels the native channel immediately");p.removeEffect(RoyaleSpells.STUN);
        var locked=SpellRegistry.REGISTRY.stream().filter(AbstractSpell::requiresLearning).findFirst().orElseThrow();
        var history=new net.minecraft.nbt.CompoundTag();history.putString("Id",locked.getSpellId());history.putInt("Level",1);p.getPersistentData().put("RoyaleIronMirrorHistory",history);p.setGameMode(GameType.SURVIVAL);
        c.assertFalse(begin(p,IronIntegration.spell(IronSpellProfile.MIRROR),1,CastSource.SPELLBOOK),"Mirror cannot bypass native learning requirements");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-electrical-pause")
    public void electricityPausesActualPlayerChannelAndResumesWithoutNewCast(GameTestHelper c) {
        var p=player(c);var data=MagicData.getPlayerMagicData(p);var spell=SpellRegistry.getSpell("irons_spellbooks:electrocute");
        p.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(1000);data.setMana(1000);
        c.assertTrue(begin(p,spell,1,CastSource.SPELLBOOK),"Begin an actual native channel");
        var manager=new MagicManager();manager.tick(p.serverLevel());int remaining=data.getCastDurationRemaining();float mana=data.getMana();
        c.assertTrue(data.isCasting()&&remaining>50,"The funded channel has really progressed before shock");
        SpellEngine.electricStun(p,10);
        c.assertTrue(dev.royalespells.pause.ElectricPause.active(p),"Electrical action pause effect is active on the caster");
        for(int i=0;i<10;i++)manager.tick(p.serverLevel());
        c.assertTrue(data.isCasting()&&data.getCastDurationRemaining()==remaining,"Channel survives: casting="+data.isCasting()+", remaining="+data.getCastDurationRemaining()+", before="+remaining+", paused="+dev.royalespells.pause.ElectricPause.active(p));
        c.assertTrue(data.getMana()>=mana,"A held channel does not emit repeated damage or spend channel mana");
        c.assertTrue(data.getCastingSpellId().equals(spell.getSpellId()),"Same spell, no cancellation/restart");
        p.removeEffect(RoyaleSpells.ELECTRICAL_STUN);manager.tick(p.serverLevel());
        c.assertTrue(data.isCasting()&&data.getCastDurationRemaining()==remaining-1,"Resume the next original casting tick");
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-electrical-start")
    public void electricalStunBlocksStartingANewNativeSpell(GameTestHelper c) {
        var p=player(c);SpellEngine.electricStun(p,10);
        c.assertFalse(begin(p,SpellRegistry.FIREBALL_SPELL.get(),1,CastSource.SPELLBOOK),"No fresh attack during electrical pause");
        p.removeEffect(RoyaleSpells.ELECTRICAL_STUN);
        c.assertTrue(begin(p,SpellRegistry.FIREBALL_SPELL.get(),1,CastSource.SPELLBOOK),"The same spell is available when shock ends");
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-native-clone")
    public void cloneCopiesOwnedIronSummonsWithOneHealthAndNoRecursion(GameTestHelper c) {
        var p=player(c);var original=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("irons_spellbooks:summoned_zombie")).create(p.level());
        original.setPos(p.position().add(2,0,0));p.serverLevel().addFreshEntity(original);io.redspace.ironsspellbooks.capabilities.magic.SummonManager.setOwner(original,p);
        var clone=NativeClones.copy(original,p.getUUID(),1.12f,"royalespells:clone",2);
        c.assertTrue(clone!=null && clone.getType()==original.getType() && clone.getMaxHealth()==1 && clone.getHealth()==1,"Native model and one-health clone");
        c.assertTrue(p.getUUID().equals(CombatCompatibility.ownerOf(clone)) && clone.hasEffect(RoyaleSpells.CLONED),"Native ownership and cyan clone marker");
        c.assertTrue(NativeClones.copy(clone,p.getUUID(),1,"royalespells:clone",1)==null,"No recursive cloning");
        c.assertTrue(NativeClones.copy(original,UUID.randomUUID(),1,"royalespells:clone",1)==null,"Cannot steal enemy summons");
        clone.removeEffect(RoyaleSpells.CLONED);NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(clone));
        c.assertTrue(clone.isRemoved(),"Dispelled clone expires");original.discard();cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="iron-system-command-mirror")
    public void administrativeMirrorUsesTheOriginalLifecycle(GameTestHelper c) {
        var p=player(c);var original=SpellRegistry.FIREBOLT_SPELL.get();
        c.assertTrue(begin(p,original,2,CastSource.SPELLBOOK),"Seed a successful spell");finish(p,120);
        IronIntegration.spell(IronSpellProfile.MIRROR).castSpell(p.level(),1,p,CastSource.COMMAND,false);
        var magic=MagicData.getPlayerMagicData(p);
        c.assertTrue(magic.getCastingSpellId().equals(original.getSpellId()) && magic.getCastingSpellLevel()==3,"Command Mirror delegates at +1");
        finish(p,120);c.assertFalse(magic.isCasting(),"Command Mirror finishes normally");cleanup(p);c.succeed();
    }
}
