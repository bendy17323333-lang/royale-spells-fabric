package dev.royalespells;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import dev.royalespells.entity.*;
import java.util.*;

public final class RoyaleSpells implements ModInitializer {
    public static final String MOD_ID="royalespells";
    public static final Map<Spell,SpellItem> ITEMS=new EnumMap<>(Spell.class);
    public static final Map<TroopCard,TroopItem> TROOP_ITEMS=new EnumMap<>(TroopCard.class);
    public static final Item PREVIOUS_SCENE=new SceneControlItem(-1),NEXT_SCENE=new SceneControlItem(1);
    public static Identifier id(String path) { return Identifier.of(MOD_ID,path); }
    public static final RegistryEntry<StatusEffect> STUN=Registry.registerReference(Registries.STATUS_EFFECT,id("stun"),new StatusEffect(StatusEffectCategory.HARMFUL,0x92CAFF) {});
    public static final RegistryEntry<StatusEffect> RAGED=Registry.registerReference(Registries.STATUS_EFFECT,id("rage"),new StatusEffect(StatusEffectCategory.BENEFICIAL,0xCC50ED) {}.addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED,id("rage_attack_speed"),0.35,net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final SimpleParticleType SPARK=FabricParticleTypes.simple();
    public static final RegistryEntry<StatusEffect> FROZEN=Registry.registerReference(Registries.STATUS_EFFECT,id("frozen"),new StatusEffect(StatusEffectCategory.HARMFUL,0x9ADFFF) {});
    public static final RegistryEntry<StatusEffect> ROOTED=Registry.registerReference(Registries.STATUS_EFFECT,id("rooted"),new StatusEffect(StatusEffectCategory.HARMFUL,0x438724) {});
    public static final net.minecraft.sound.SoundEvent GRAVEYARD_DEPLOY=Registry.register(Registries.SOUND_EVENT,id("graveyard_deploy"),net.minecraft.sound.SoundEvent.of(id("graveyard_deploy")));
    public static final EntityType<SpellEntity> SPELL=Registry.register(Registries.ENTITY_TYPE,id("spell"),
        FabricEntityTypeBuilder.<SpellEntity>create(SpawnGroup.MISC,SpellEntity::new)
            .dimensions(EntityDimensions.fixed(0.1f,0.1f)).trackRangeBlocks(96).trackedUpdateRate(1).build());
    public static final EntityType<AllyZombie> ZOMBIE=unit("baby_zombie",AllyZombie::new,0.6f,1.95f);
    public static final EntityType<AllyZombie> BARBARIAN=unit("barbarian",AllyZombie::new,0.6f,1.95f);
    public static final EntityType<AllyZombie> RECRUIT=unit("royal_recruit",AllyZombie::new,0.6f,1.95f);
    public static final EntityType<AllySkeleton> SKELETON=unit("graveyard_skeleton",AllySkeleton::new,0.6f,1.99f);
    public static final EntityType<RoyaleUnit> BARBARIAN_HUT=unit("barbarian_hut",RoyaleUnit::new,3.2f,3.4f);
    private static <T extends Entity> EntityType<T> unit(String name,EntityType.EntityFactory<T> factory,float w,float h) {
        return Registry.register(Registries.ENTITY_TYPE,id(name),FabricEntityTypeBuilder.create(SpawnGroup.CREATURE,factory)
            .dimensions(EntityDimensions.fixed(w,h)).trackRangeBlocks(80).build());
    }
    public void onInitialize() {
        Registry.register(Registries.PARTICLE_TYPE,id("spell_spark"),SPARK);
        for(Spell spell:Spell.values()) ITEMS.put(spell,Registry.register(Registries.ITEM,id(spell.id()),new SpellItem(spell)));
        for(TroopCard card:TroopCard.values())TROOP_ITEMS.put(card,Registry.register(Registries.ITEM,id(card.id()),new TroopItem(card)));
        Registry.register(Registries.ITEM,id("previous_scene"),PREVIOUS_SCENE);
        Registry.register(Registries.ITEM,id("next_scene"),NEXT_SCENE);
        ShowcaseMap.install();
        Registry.register(Registries.ITEM_GROUP,id("spells"),FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.royalespells.spells"))
            .icon(()->new ItemStack(ITEMS.get(Spell.ZAP_EVOLUTION)))
            .entries((ctx,entries)->{ITEMS.values().forEach(entries::add);TROOP_ITEMS.values().forEach(entries::add);}).build());
        FabricDefaultAttributeRegistry.register(ZOMBIE,AllyZombie.createZombieAttributes());
        FabricDefaultAttributeRegistry.register(BARBARIAN,AllyZombie.createZombieAttributes());
        FabricDefaultAttributeRegistry.register(RECRUIT,AllyZombie.createZombieAttributes());
        FabricDefaultAttributeRegistry.register(SKELETON,AllySkeleton.createAbstractSkeletonAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE,3));
        FabricDefaultAttributeRegistry.register(BARBARIAN_HUT,RoyaleUnit.attributes());
        UnitSounds.initialize();
        ServerTickEvents.END_SERVER_TICK.register(SpellEngine::tick);
        ServerTickEvents.END_SERVER_TICK.register(EarthquakeDestruction::tickAll);
        ServerTickEvents.END_SERVER_TICK.register(SpellMotion::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->SpellMotion.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server->EarthquakeDestruction.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server->SpellEngine.clear());
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->SpellEngine.onDeath(entity));
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(
            CommandManager.literal("royalespells").requires(source->source.hasPermissionLevel(2))
              .then(CommandManager.literal("give").executes(ctx->{
                  var player=ctx.getSource().getPlayerOrThrow();
                  for(var item:ITEMS.values()) player.giveItemStack(new ItemStack(item));
                  for(var item:TROOP_ITEMS.values()) player.giveItemStack(new ItemStack(item));
                  return ITEMS.size()+TROOP_ITEMS.size();
              }).then(CommandManager.argument("spell",StringArgumentType.word()).suggests((ctx,builder)->{
                  for(Spell spell:Spell.values()) builder.suggest(spell.id());for(TroopCard card:TroopCard.values())builder.suggest(card.id());return builder.buildFuture();
              }).executes(ctx->{
                  String id=StringArgumentType.getString(ctx,"spell");
                  for(TroopCard card:TroopCard.values())if(card.id().equals(id)){ctx.getSource().getPlayerOrThrow().giveItemStack(new ItemStack(TROOP_ITEMS.get(card)));return 1;}
                  for(Spell spell:Spell.values()) if(spell.id().equals(id)) {
                      ctx.getSource().getPlayerOrThrow().giveItemStack(new ItemStack(ITEMS.get(spell))); return 1;
                  }
                  ctx.getSource().sendError(Text.literal("Unknown spell: "+id)); return 0;
              })))
              .then(CommandManager.literal("refill").executes(ctx->{SpellEngine.refill(ctx.getSource().getPlayerOrThrow());return 1;}))
              .then(CommandManager.literal("scene").executes(ctx->{ShowcaseMap.switchScene(ctx.getSource().getPlayerOrThrow(),0);return 1;})
                  .then(CommandManager.literal("next").executes(ctx->{ShowcaseMap.switchScene(ctx.getSource().getPlayerOrThrow(),1);return 1;}))
                  .then(CommandManager.literal("previous").executes(ctx->{ShowcaseMap.switchScene(ctx.getSource().getPlayerOrThrow(),-1);return 1;})))
              .then(CommandManager.literal("clear").executes(ctx->{
                  var player=ctx.getSource().getPlayerOrThrow();
                  for(Entity e:player.getServerWorld().iterateEntities())
                      if((e instanceof Summoned s && player.getUuid().equals(s.ownerId())) ||
                         (e instanceof SpellEntity fx && player.getUuid().equals(fx.ownerId))) e.discard();
                  return 1;
              }))
        ));
    }
}
