package dev.royalespells;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.royalespells.entity.*;
import java.util.*;

@net.neoforged.fml.common.Mod(RoyaleSpells.MOD_ID)
public final class RoyaleSpells {
    public static final String MOD_ID="royalespells";
    public static final Map<Spell,SpellItem> ITEMS=new EnumMap<>(Spell.class);
    public static final Map<TroopCard,TroopItem> TROOP_ITEMS=new EnumMap<>(TroopCard.class);
    public static Item PREVIOUS_SCENE,NEXT_SCENE,NEUTRAL_ARMY_EGG;
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID,path); }
    public static Holder<MobEffect> STUN,RAGED,FROZEN,ROOTED,CLONED;
    public static final SimpleParticleType SPARK=new SimpleParticleType(false);
    public static final SimpleParticleType GRAVE_MOTE=new SimpleParticleType(false);
    public static final net.minecraft.sounds.SoundEvent GRAVEYARD_DEPLOY=net.minecraft.sounds.SoundEvent.createFixedRangeEvent(id("graveyard_deploy"),SpellSounds.RANGE);
    public static EntityType<SpellEntity> SPELL;
    public static EntityType<AllyZombie> ZOMBIE,BARBARIAN,RECRUIT;
    public static EntityType<AllySkeleton> SKELETON;
    public static EntityType<ArmySkeleton> ARMY_SKELETON;
    public static EntityType<ElementalSpirit> ELEMENTAL_SPIRIT;
    public static EntityType<SpiritArc> SPIRIT_ARC;
    public static EntityType<RitualEntity> RITUAL;
    public static EntityType<EvolutionBurst> EVOLUTION_BURST;
    public static EntityType<RoyaleUnit> BARBARIAN_HUT;
    private static <T extends Entity> EntityType<T> unit(String name,EntityType.EntityFactory<T> factory,float w,float h) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE,id(name),EntityType.Builder.of(factory,MobCategory.CREATURE)
            .sized(w,h).clientTrackingRange(80).updateInterval(1).build(id(name).toString()));
    }
    public RoyaleSpells(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener(this::register);
        bus.addListener(this::attributes);
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event)->{
            if(net.neoforged.fml.ModList.get().isLoaded("irons_spellbooks")) {
                event.register(dev.royalespells.test.IronCompatibilityTests.class);
                event.register(dev.royalespells.test.Iron151Tests.class);
                event.register(dev.royalespells.test.IronSpellSystemTests.class);event.register(dev.royalespells.test.ElixirTests.class);event.register(dev.royalespells.test.IronBalanceTests.class);event.register(dev.royalespells.test.ArmyTests.class);event.register(dev.royalespells.test.SpiritIronTests.class);
            }
            String report=System.getProperty("royalespells.gametestReport");
            if(report!=null)try {net.minecraft.gametest.framework.GlobalTestReporter.replaceWith(new net.minecraft.gametest.framework.JUnitLikeTestReporter(new java.io.File(report)));}
            catch(Exception e){throw new IllegalStateException("Cannot create game test report",e);}
        });
        var events=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->{
            var server=event.getServer();SpellEngine.tick(server);EarthquakeDestruction.tickAll(server);SpellMotion.tick(server);
        });
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{
            SpellMotion.clear();EarthquakeDestruction.clear();SpellEngine.clear();
        });
        events.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event)->SpellEngine.onDeath(event.getEntity()));
        events.addListener(this::commands);
        ShowcaseMap.install();CombatCompatibility.install();IronSpellSystem.install(bus);SummonOrders.install();ControlCooldown.install();dev.royalespells.elixir.ElixirContent.install(bus);
    }
    private void register(net.neoforged.neoforge.registries.RegisterEvent event) {
        var key=event.getRegistryKey();
        if(key.equals(net.minecraft.core.registries.Registries.MOB_EFFECT)) {
            STUN=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,id("stun"),IronSpellSystem.effect(MobEffectCategory.HARMFUL,0x92CAFF));
            RAGED=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,id("rage"),IronSpellSystem.effect(MobEffectCategory.BENEFICIAL,0xCC50ED).addAttributeModifier(Attributes.ATTACK_SPEED,id("rage_attack_speed"),0.35,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            FROZEN=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,id("frozen"),IronSpellSystem.effect(MobEffectCategory.HARMFUL,0x9ADFFF));
            ROOTED=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,id("rooted"),IronSpellSystem.effect(MobEffectCategory.HARMFUL,0x438724));
            CLONED=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,id("cloned"),IronSpellSystem.effect(MobEffectCategory.NEUTRAL,0x14CCFF));
        } else if(key.equals(net.minecraft.core.registries.Registries.ENTITY_TYPE)) {
            SPELL=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("spell"),EntityType.Builder.<SpellEntity>of(SpellEntity::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(96).updateInterval(1).build(id("spell").toString()));
            ZOMBIE=unit("baby_zombie",AllyZombie::new,.6f,1.95f);
            BARBARIAN=unit("barbarian",AllyZombie::new,.6f,1.95f);
            RECRUIT=unit("royal_recruit",AllyZombie::new,.6f,1.95f);
            SKELETON=unit("graveyard_skeleton",AllySkeleton::new,.48f,1.4f);
            ARMY_SKELETON=unit("army_skeleton",ArmySkeleton::new,.48f,1.4f);
            ELEMENTAL_SPIRIT=unit("elemental_spirit",ElementalSpirit::new,.6f,.8f);
            SPIRIT_ARC=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("spirit_chain_arc"),EntityType.Builder.<SpiritArc>of(SpiritArc::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(64).updateInterval(1).build(id("spirit_chain_arc").toString()));
            RITUAL=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("dark_elixir_ritual"),EntityType.Builder.<RitualEntity>of(RitualEntity::new,MobCategory.MISC).sized(.2f,.2f).clientTrackingRange(64).updateInterval(1).build(id("dark_elixir_ritual").toString()));
            EVOLUTION_BURST=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("evolution_deployment"),EntityType.Builder.<EvolutionBurst>of(EvolutionBurst::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(96).updateInterval(1).build(id("evolution_deployment").toString()));
            BARBARIAN_HUT=unit("barbarian_hut",RoyaleUnit::new,3.2f,3.4f);
        } else if(key.equals(net.minecraft.core.registries.Registries.ITEM)) {
            for(Spell spell:Spell.values())ITEMS.put(spell,Registry.register(BuiltInRegistries.ITEM,id(spell.id()),new SpellItem(spell)));
            for(TroopCard card:TroopCard.values())TROOP_ITEMS.put(card,Registry.register(BuiltInRegistries.ITEM,id(card.id()),new TroopItem(card)));
            PREVIOUS_SCENE=Registry.register(BuiltInRegistries.ITEM,id("previous_scene"),new SceneControlItem(-1));
            NEXT_SCENE=Registry.register(BuiltInRegistries.ITEM,id("next_scene"),new SceneControlItem(1));
            NEUTRAL_ARMY_EGG=Registry.register(BuiltInRegistries.ITEM,id("neutral_skeleton_army_spawn_egg"),new dev.royalespells.army.NeutralArmyEgg());
        } else if(key.equals(net.minecraft.core.registries.Registries.PARTICLE_TYPE)) {
            Registry.register(BuiltInRegistries.PARTICLE_TYPE,id("spell_spark"),SPARK);
            Registry.register(BuiltInRegistries.PARTICLE_TYPE,id("grave_mote"),GRAVE_MOTE);
        }
        else if(key.equals(net.minecraft.core.registries.Registries.SOUND_EVENT)) {
            Registry.register(BuiltInRegistries.SOUND_EVENT,id("graveyard_deploy"),GRAVEYARD_DEPLOY);UnitSounds.initialize();SpellSounds.initialize();dev.royalespells.army.ArmySounds.register();
        } else if(key.equals(net.minecraft.core.registries.Registries.INSTRUMENT)) {
            dev.royalespells.army.ArmySounds.INSTRUMENT=Registry.registerForHolder(BuiltInRegistries.INSTRUMENT,id("skeleton_army_horn"),new net.minecraft.world.item.Instrument(Holder.direct(dev.royalespells.army.ArmySounds.DEPLOY),40,32));
        } else if(key.equals(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB))
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("spells"),net.minecraft.world.item.CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.royalespells.spells")).icon(()->new ItemStack(ITEMS.get(Spell.ZAP_EVOLUTION)))
                .displayItems((ctx,entries)->{ITEMS.values().forEach(entries::accept);TROOP_ITEMS.values().forEach(entries::accept);entries.accept(NEUTRAL_ARMY_EGG);dev.royalespells.elixir.ElixirContent.creative(entries);}).build());
    }
    private void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ZOMBIE,AllyZombie.createAttributes().build());event.put(BARBARIAN,AllyZombie.createAttributes().build());
        event.put(RECRUIT,AllyZombie.createAttributes().build());event.put(SKELETON,AllySkeleton.createAttributes().add(Attributes.ATTACK_DAMAGE,3).build());
        event.put(BARBARIAN_HUT,RoyaleUnit.attributes().build());
        event.put(ARMY_SKELETON,AllySkeleton.createAttributes().add(Attributes.ATTACK_DAMAGE,1.8).add(Attributes.MAX_HEALTH,4).build());
        event.put(ELEMENTAL_SPIRIT,ElementalSpirit.attributes().build());
    }
    private void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("royalespells").requires(source->source.hasPermission(2))
              .then(Commands.literal("give").executes(ctx->{
                  var player=ctx.getSource().getPlayerOrException();
                  for(var item:ITEMS.values()) player.addItem(new ItemStack(item));
                  for(var item:TROOP_ITEMS.values()) player.addItem(new ItemStack(item));
                  return ITEMS.size()+TROOP_ITEMS.size();
              }).then(Commands.argument("spell",StringArgumentType.word()).suggests((ctx,builder)->{
                  for(Spell spell:Spell.values()) builder.suggest(spell.id());for(TroopCard card:TroopCard.values())builder.suggest(card.id());return builder.buildFuture();
              }).executes(ctx->{
                  String id=StringArgumentType.getString(ctx,"spell");
                  for(TroopCard card:TroopCard.values())if(card.id().equals(id)){ctx.getSource().getPlayerOrException().addItem(new ItemStack(TROOP_ITEMS.get(card)));return 1;}
                  for(Spell spell:Spell.values()) if(spell.id().equals(id)) {
                      ctx.getSource().getPlayerOrException().addItem(new ItemStack(ITEMS.get(spell))); return 1;
                  }
                  ctx.getSource().sendFailure(Component.literal("Unknown spell: "+id)); return 0;
              })))
              .then(Commands.literal("refill").executes(ctx->{SpellEngine.refill(ctx.getSource().getPlayerOrException());return 1;}))
              .then(Commands.literal("scene").executes(ctx->{ShowcaseMap.switchScene(ctx.getSource().getPlayerOrException(),0);return 1;})
                  .then(Commands.literal("next").executes(ctx->{ShowcaseMap.switchScene(ctx.getSource().getPlayerOrException(),1);return 1;}))
                  .then(Commands.literal("previous").executes(ctx->{ShowcaseMap.switchScene(ctx.getSource().getPlayerOrException(),-1);return 1;})))
              .then(Commands.literal("clear").executes(ctx->{
                  var player=ctx.getSource().getPlayerOrException();
                  for(Entity e:com.google.common.collect.ImmutableList.copyOf(player.serverLevel().getAllEntities()))
                      if((e instanceof Summoned s && player.getUUID().equals(s.ownerId())) ||
                         (e instanceof SpellEntity fx && player.getUUID().equals(fx.ownerId))) e.discard();
                  return 1;
              }))
        );
    }
}
