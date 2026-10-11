package dev.royalespells;
import dev.royalespells.entity.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import java.util.*;
public final class RoyaleSpells implements ModInitializer {
    public static final String MOD_ID="royalespells";
    public static ResourceLocation id(String path){return new ResourceLocation(MOD_ID,path);}
    public static final Map<Spell,SpellItem> ITEMS=new EnumMap<>(Spell.class);
    public static final Map<TroopCard,TroopItem> TROOP_ITEMS=new EnumMap<>(TroopCard.class);
    public static Item PREVIOUS_SCENE,NEXT_SCENE,NEUTRAL_ARMY_EGG;
    private static MobEffect effect(String id,MobEffectCategory kind,int color){return Registry.register(BuiltInRegistries.MOB_EFFECT,id(id),new MobEffect(kind,color){});}
    public static final MobEffect STUN=effect("stun",MobEffectCategory.HARMFUL,0x92CAFF),ELECTRICAL_STUN=effect("electrical_stun",MobEffectCategory.HARMFUL,0x8BE5FF),
        RAGED=effect("rage",MobEffectCategory.BENEFICIAL,0xCC50ED).addAttributeModifier(Attributes.ATTACK_SPEED,"e9786d48-7cfe-4ddf-8dbd-b373e65a431d",.35,AttributeModifier.Operation.MULTIPLY_TOTAL),
        FROZEN=effect("frozen",MobEffectCategory.HARMFUL,0x9ADFFF),ROOTED=effect("rooted",MobEffectCategory.HARMFUL,0x438724),
        CLONED=effect("cloned",MobEffectCategory.NEUTRAL,0x14CCFF),SNOWBOUND=effect("snowbound",MobEffectCategory.HARMFUL,0x438FFF),
        CARD_SLOW=effect("card_slow",MobEffectCategory.HARMFUL,0x81BCDE).addAttributeModifier(Attributes.MOVEMENT_SPEED,"7bba2d5d-c805-455b-890d-a053c11500a1",-.05,AttributeModifier.Operation.MULTIPLY_TOTAL),
        CARD_SPEED=effect("card_speed",MobEffectCategory.BENEFICIAL,0xCC50ED).addAttributeModifier(Attributes.MOVEMENT_SPEED,"bd6c8491-66fc-49fb-ab49-ac90c03e00a2",.35,AttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final SimpleParticleType SPARK=FabricParticleTypes.simple(),GRAVE_MOTE=FabricParticleTypes.simple();
    public static final net.minecraft.sounds.SoundEvent GRAVEYARD_DEPLOY=net.minecraft.sounds.SoundEvent.createFixedRangeEvent(id("graveyard_deploy"),SpellSounds.RANGE);
    private static <T extends Entity> EntityType<T> type(String name,EntityType.EntityFactory<T> factory,float width,float height,MobCategory group){return Registry.register(BuiltInRegistries.ENTITY_TYPE,id(name),EntityType.Builder.of(factory,group).sized(width,height).clientTrackingRange(96).updateInterval(1).build(id(name).toString()));}
    public static final EntityType<SpellEntity> SPELL=type("spell",SpellEntity::new,.1f,.1f,MobCategory.MISC);
    public static final EntityType<AllyZombie> ZOMBIE=type("baby_zombie",AllyZombie::new,.6f,1.95f,MobCategory.CREATURE),BARBARIAN=type("barbarian",AllyZombie::new,.6f,1.95f,MobCategory.CREATURE),RECRUIT=type("royal_recruit",AllyZombie::new,.6f,1.95f,MobCategory.CREATURE);
    public static final EntityType<AllySkeleton> SKELETON=type("graveyard_skeleton",AllySkeleton::new,.48f,1.4f,MobCategory.CREATURE);
    public static final EntityType<ArmySkeleton> ARMY_SKELETON=type("army_skeleton",ArmySkeleton::new,.48f,1.4f,MobCategory.CREATURE);
    public static final EntityType<ElementalSpirit> ELEMENTAL_SPIRIT=type("elemental_spirit",ElementalSpirit::new,.6f,.8f,MobCategory.CREATURE);
    public static final EntityType<InfernoDragon> INFERNO_DRAGON=type("inferno_dragon",InfernoDragon::new,1.15f,1.65f,MobCategory.CREATURE);
    public static final EntityType<RoyalWitch> WITCH=type("witch",RoyalWitch::new,.65f,1.95f,MobCategory.CREATURE);
    public static final EntityType<RoyalWitch> EVOLVED_WITCH=type("witch_evolution",(type,world)->{var w=new RoyalWitch(type,world);w.setEvolved(true);return w;},.65f,1.95f,MobCategory.CREATURE);
    public static net.minecraft.world.item.Item WITCH_EGG,EVOLVED_WITCH_EGG;
    public static final EntityType<WitchBolt> WITCH_BOLT=type("witch_bolt",WitchBolt::new,.15f,.15f,MobCategory.MISC);
    public static final EntityType<SpiritArc> SPIRIT_ARC=type("spirit_chain_arc",SpiritArc::new,.1f,.1f,MobCategory.MISC);
    public static final EntityType<EvolutionBurst> EVOLUTION_BURST=type("evolution_deployment",EvolutionBurst::new,.1f,.1f,MobCategory.MISC);
    public static final EntityType<RoyaleUnit> BARBARIAN_HUT=type("barbarian_hut",RoyaleUnit::new,3.2f,3.4f,MobCategory.CREATURE);
    @Override public void onInitialize(){
        for(var spell:Spell.values())ITEMS.put(spell,Registry.register(BuiltInRegistries.ITEM,id(spell.id()),new SpellItem(spell)));
        for(var card:TroopCard.values())TROOP_ITEMS.put(card,Registry.register(BuiltInRegistries.ITEM,id(card.id()),new TroopItem(card)));
        PREVIOUS_SCENE=Registry.register(BuiltInRegistries.ITEM,id("previous_scene"),new SceneControlItem(-1));
        NEXT_SCENE=Registry.register(BuiltInRegistries.ITEM,id("next_scene"),new SceneControlItem(1));
        NEUTRAL_ARMY_EGG=Registry.register(BuiltInRegistries.ITEM,id("neutral_skeleton_army_spawn_egg"),new dev.royalespells.army.NeutralArmyEgg());
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,id("spell_spark"),SPARK);Registry.register(BuiltInRegistries.PARTICLE_TYPE,id("grave_mote"),GRAVE_MOTE);
        Registry.register(BuiltInRegistries.SOUND_EVENT,id("graveyard_deploy"),GRAVEYARD_DEPLOY);UnitSounds.initialize();SpellSounds.initialize();WitchSounds.register();dev.royalespells.army.ArmySounds.register();
        FabricDefaultAttributeRegistry.register(ZOMBIE,AllyZombie.createAttributes());FabricDefaultAttributeRegistry.register(BARBARIAN,AllyZombie.createAttributes());FabricDefaultAttributeRegistry.register(RECRUIT,AllyZombie.createAttributes());
        FabricDefaultAttributeRegistry.register(SKELETON,AllySkeleton.createAttributes().add(Attributes.ATTACK_DAMAGE,3));FabricDefaultAttributeRegistry.register(ARMY_SKELETON,AllySkeleton.createAttributes().add(Attributes.ATTACK_DAMAGE,3).add(Attributes.MAX_HEALTH,3));
        FabricDefaultAttributeRegistry.register(WITCH,RoyalWitch.attributes());
        FabricDefaultAttributeRegistry.register(EVOLVED_WITCH,RoyalWitch.attributes());
        WITCH_EGG=Registry.register(BuiltInRegistries.ITEM,id("witch_spawn_egg"),new net.minecraft.world.item.SpawnEggItem(WITCH,0x3E216E,0xDBC078,new Item.Properties()));
        EVOLVED_WITCH_EGG=Registry.register(BuiltInRegistries.ITEM,id("witch_evolution_spawn_egg"),new net.minecraft.world.item.SpawnEggItem(EVOLVED_WITCH,0x281447,0xF08CF0,new Item.Properties()));
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS).register(entries->{entries.accept(WITCH_EGG);entries.accept(EVOLVED_WITCH_EGG);});
        FabricDefaultAttributeRegistry.register(ELEMENTAL_SPIRIT,ElementalSpirit.attributes());FabricDefaultAttributeRegistry.register(INFERNO_DRAGON,InfernoDragon.attributes());FabricDefaultAttributeRegistry.register(BARBARIAN_HUT,RoyaleUnit.attributes());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("spells"),FabricItemGroup.builder().title(Component.translatable("itemGroup.royalespells.spells")).icon(()->new ItemStack(ITEMS.get(Spell.ZAP_EVOLUTION))).displayItems((ctx,entries)->{ITEMS.values().forEach(entries::accept);TROOP_ITEMS.values().forEach(entries::accept);entries.accept(NEUTRAL_ARMY_EGG);entries.accept(WITCH_EGG);entries.accept(EVOLVED_WITCH_EGG);}).build());
        ServerTickEvents.END_SERVER_TICK.register(server->{SpellEngine.tick(server);EarthquakeDestruction.tickAll(server);SpellMotion.tick(server);});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{SpellEngine.clear();EarthquakeDestruction.clear();SpellMotion.clear();});
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,damage)->{SpellEngine.onDeath(entity);RoyalWitch.onSkeletonDeath(entity);});
        SummonOrders.install();ControlCooldown.install();ShowcaseMap.install();
        CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment)->dispatcher.register(Commands.literal("royalespells").requires(s->s.hasPermission(2))
            .then(Commands.literal("give").executes(ctx->{var player=ctx.getSource().getPlayerOrException();for(var i:ITEMS.values())player.addItem(new ItemStack(i));for(var i:TROOP_ITEMS.values())player.addItem(new ItemStack(i));return ITEMS.size()+TROOP_ITEMS.size();})
                .then(Commands.argument("card",com.mojang.brigadier.arguments.StringArgumentType.word()).suggests((ctx,suggestions)->{ITEMS.keySet().forEach(c->suggestions.suggest(c.id()));TROOP_ITEMS.keySet().forEach(c->suggestions.suggest(c.id()));return suggestions.buildFuture();}).executes(ctx->{String wanted=com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"card");for(var c:ITEMS.entrySet())if(c.getKey().id().equals(wanted)){ctx.getSource().getPlayerOrException().addItem(new ItemStack(c.getValue()));return 1;}for(var c:TROOP_ITEMS.entrySet())if(c.getKey().id().equals(wanted)){ctx.getSource().getPlayerOrException().addItem(new ItemStack(c.getValue()));return 1;}return 0;})))
            .then(Commands.literal("refill").executes(ctx->{SpellEngine.refill(ctx.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("clear").executes(ctx->{var player=ctx.getSource().getPlayerOrException();for(var e:com.google.common.collect.ImmutableList.copyOf(player.serverLevel().getAllEntities()))if(e instanceof Summoned s&&player.getUUID().equals(s.ownerId())||e instanceof SpellEntity effect&&player.getUUID().equals(effect.ownerId))e.discard();return 1;}))));
    }
}
