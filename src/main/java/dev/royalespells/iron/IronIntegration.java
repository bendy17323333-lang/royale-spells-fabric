package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;
import java.util.function.Supplier;

public final class IronIntegration {
    private static final DeferredRegister<AbstractSpell> SPELLS=DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,RoyaleSpells.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,RoyaleSpells.MOD_ID);
    private static final Map<IronSpellProfile,Supplier<AbstractSpell>> REGISTERED=new EnumMap<>(IronSpellProfile.class);
    public static void install(IEventBus bus) {
        for(var profile:IronSpellProfile.values())REGISTERED.put(profile,SPELLS.register(profile.id(),()->profile==IronSpellProfile.MIRROR?new MirrorIronSpell():profile==IronSpellProfile.SKELETON_ARMY_EVOLUTION?new EvolvedArmySpell():new RoyaleIronSpell(profile)));
        TABS.register("iron_scrolls",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.royalespells.iron_scrolls"))
            .icon(()->scroll(IronSpellProfile.ZAP,1)).displayItems((context,output)->{
                SpiritSpells.creative(output);
                for(var profile:IronSpellProfile.values()){
                    var spell=spell(profile);
                    if(spell.isEnabled())for(int level=spell.getMinLevel();level<=spell.getMaxLevel();level++)output.accept(scroll(profile,level));
                }
            }).build());
        SPELLS.register(bus);TABS.register(bus);
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event)->
            event.registrar("1").playToClient(MirrorHistoryPayload.TYPE,MirrorHistoryPayload.CODEC,
                (payload,context)->context.enqueueWork(()->MirrorIronSpell.clientHistory=payload)));
        NeoForge.EVENT_BUS.addListener(IronIntegration::summonDamage);
        // Upstream caches can be populated before world JSON configuration is applied.
        // Rebuild discovery after configuration/sync, including changed schools and loot permissions.
        NeoForge.EVENT_BUS.addListener((io.redspace.ironsspellbooks.api.config.ModifyDefaultConfigValuesEvent event)->{
            dev.royalespells.mixin.IronSchoolCacheAccess.royaleSchools().clear();
            dev.royalespells.mixin.IronLootCacheAccess.royaleLoot().clear();
            dev.royalespells.mixin.IronLootCacheAccess.royaleForcedLoot().clear();
        });
        MirrorIronSpell.install();NativeClones.install();ArmyMagic.install();SpiritSpells.install(bus);
    }
    public static net.minecraft.world.item.Item elixirInk(boolean dark,int grade){return new ElixirInkItem(dark,grade);}
    public static AbstractSpell spell(IronSpellProfile profile){return REGISTERED.get(profile).get();}
    public static net.minecraft.world.effect.MobEffect effect(net.minecraft.world.effect.MobEffectCategory category,int color) {
        return new io.redspace.ironsspellbooks.effect.MagicMobEffect(category,color);
    }
    public static void interrupt(LivingEntity target) {
        if(target instanceof net.minecraft.server.level.ServerPlayer player && io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player).isCasting())
            io.redspace.ironsspellbooks.api.util.Utils.serverSideCancelCast(player,true);
    }
    public static ItemStack scroll(IronSpellProfile profile,int level) {
        var stack=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(spell(profile),level,stack);return stack;
    }
    public static void damage(SpellEntity effect,LivingEntity target,float amount) {
        if(!(effect.level() instanceof ServerLevel world) || !SpellEngine.enemy(effect.ownerId,target))return;
        if(target instanceof Player && !world.getServer().isPvpAllowed())return;
        var spell=SpellRegistry.getSpell(effect.ironSpellId());if(spell==SpellRegistry.none())return;
        var caster=CombatCompatibility.resolve(world,effect.ownerId);
        var damage=spell.getDamageSource(effect,caster==null?effect:caster);
        if(spell instanceof RoyaleIronSpell royal)amount*=royal.profile.damageMultiplier();
        // Normal spells participate in vanilla hit immunity. Graveyard skeletons retain
        // their explicitly designed, separately implemented rapid weak attacks.
        DamageSources.applyDamage(target,amount,damage);
    }
    public static void heal(SpellEntity effect,LivingEntity target,float amount) {
        var world=(ServerLevel)effect.level();var owner=CombatCompatibility.resolve(world,effect.ownerId);
        var spell=SpellRegistry.getSpell(effect.ironSpellId());
        if(owner instanceof LivingEntity caster)NeoForge.EVENT_BUS.post(new SpellHealEvent(caster,target,amount,spell.getSchoolType()));
        target.heal(amount);
    }
    public static void summoned(Mob creature,UUID owner,String spellId,int level) {
        if(!(creature.level() instanceof ServerLevel world))return;
        creature.getPersistentData().putString("RoyaleIronSpell",spellId);creature.getPersistentData().putInt("RoyaleIronLevel",level);
        // The visible sword must not add a second, hidden weapon damage bonus.
        if(creature instanceof AllyZombie)creature.getMainHandItem().set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        var caster=CombatCompatibility.resolve(world,owner);
        if(caster instanceof LivingEntity living)NeoForge.EVENT_BUS.post(new SpellSummonEvent<>(living,creature,ResourceLocation.parse(spellId),level));
    }
    private static void summonDamage(LivingIncomingDamageEvent event) {
        if(event.getSource().getEntity() instanceof LivingEntity attacker && attacker instanceof Summoned summon
            && attacker.getPersistentData().contains("RoyaleIronSpell") && attacker.level() instanceof ServerLevel world) {
            var owner=CombatCompatibility.resolve(world,summon.ownerId());
            if(owner instanceof LivingEntity caster && caster.getAttributes().hasAttribute(AttributeRegistry.SUMMON_DAMAGE))
                event.setAmount(event.getAmount()*(float)Math.max(0,caster.getAttributeValue(AttributeRegistry.SUMMON_DAMAGE)));
        }
    }
    private IronIntegration() {}
}
