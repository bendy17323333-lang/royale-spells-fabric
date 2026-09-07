package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.entity.ElementalSpirit;
import dev.royalespells.spirit.SpiritElement;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;
import java.util.function.Supplier;

/** Optional integration: neither items nor upstream spell types load in standalone mode. */
public final class SpiritSpells {
    private static final DeferredRegister<AbstractSpell> SPELLS=DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,RoyaleSpells.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,RoyaleSpells.MOD_ID);
    private static final Map<SpiritElement,Supplier<AbstractSpell>> SPELL_MAP=new EnumMap<>(SpiritElement.class);
    private static final Map<SpiritElement,Supplier<Item>> STAFF_MAP=new EnumMap<>(SpiritElement.class);
    private static final String COOLDOWN="RoyaleSpiritCooldown";
    public static void install(IEventBus bus){
        for(var kind:SpiritElement.values()){
            SPELL_MAP.put(kind,SPELLS.register(kind.spellId(),()->new SpiritSpell(kind)));
            STAFF_MAP.put(kind,ITEMS.register(kind.staffId(),()->new FurnaceStaffItem(kind)));
        }
        SPELLS.register(bus);ITEMS.register(bus);
        // Own staffs choose their bound element; the attempt still goes through the
        // native mana, learned-spell, casting, cancellation and event lifecycle.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,SpiritSpells::use);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer p)sync(p);});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e)->{if(e.getEntity() instanceof ServerPlayer p)sync(p);});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone e)->{if(e.getOriginal().getPersistentData().contains(COOLDOWN))e.getEntity().getPersistentData().putLong(COOLDOWN,e.getOriginal().getPersistentData().getLong(COOLDOWN));});
    }
    public static AbstractSpell spell(SpiritElement element){return SPELL_MAP.get(element).get();}
    public static Item staff(SpiritElement element){return STAFF_MAP.get(element).get();}
    public static void creative(CreativeModeTab.Output output){for(var kind:SpiritElement.values())output.accept(staff(kind).getDefaultInstance());}
    private static void use(PlayerInteractEvent.RightClickItem e){
        if(!(e.getItemStack().getItem() instanceof FurnaceStaffItem staff))return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.CONSUME);
        if(!(e.getEntity() instanceof ServerPlayer player))return;
        staff.prepare(e.getItemStack());var data=MagicData.getPlayerMagicData(player);
        if(data.isCasting()){io.redspace.ironsspellbooks.api.util.Utils.serverSideCancelCast(player,true);return;}
        var spell=spell(staff.element);
        if(!spell.attemptInitiateCast(e.getItemStack(),1,player.level(),player,CastSource.SWORD,true,e.getHand()==InteractionHand.MAIN_HAND?io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND:io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND))e.setCancellationResult(InteractionResult.FAIL);
    }
    public static long remaining(ServerPlayer p){return Math.max(0,p.getPersistentData().getLong(COOLDOWN)-p.server.overworld().getGameTime());}
    public static void cooldown(ServerPlayer p,int ticks){p.getPersistentData().putLong(COOLDOWN,p.server.overworld().getGameTime()+ticks);sync(p);}
    private static void sync(ServerPlayer p){int ticks=(int)Math.min(Integer.MAX_VALUE,remaining(p));if(ticks<=0)return;var data=MagicData.getPlayerMagicData(p);for(var e:SpiritElement.values())data.getPlayerCooldowns().addCooldown(spell(e),ticks);data.getPlayerCooldowns().syncToPlayer(p);}
    public static ItemStack attune(ItemStack input,ItemStack reagent){
        if(!(input.getItem() instanceof FurnaceStaffItem old))return ItemStack.EMPTY;
        for(var kind:SpiritElement.values())if(kind!=old.element&&reagent.is(kind.reagent())&&spell(kind).isEnabled()){
            var result=input.transmuteCopy(staff(kind),1);((FurnaceStaffItem)result.getItem()).prepare(result);return result;
        }
        return ItemStack.EMPTY;
    }
    public static void damage(ElementalSpirit spirit,LivingEntity target,float amount){
        var caster=CombatCompatibility.resolve((net.minecraft.server.level.ServerLevel)spirit.level(),spirit.ownerId());
        if(target instanceof net.minecraft.world.entity.player.Player p&&!p.getServer().isPvpAllowed())return;
        if(caster instanceof LivingEntity living&&living.getAttributes().hasAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE))amount*=Math.max(0,(float)living.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE));
        var source=spell(spirit.element()).getDamageSource(spirit,caster==null?spirit:caster);
        final float damage=amount;
        CombatImpact.withoutKnockback(target,()->io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(target,damage,source));
    }
    public static void heal(ElementalSpirit spirit,LivingEntity target,float amount){
        var caster=CombatCompatibility.resolve((net.minecraft.server.level.ServerLevel)spirit.level(),spirit.ownerId());
        if(caster instanceof LivingEntity living)NeoForge.EVENT_BUS.post(new io.redspace.ironsspellbooks.api.events.SpellHealEvent(living,target,amount,spell(spirit.element()).getSchoolType()));
        target.heal(amount);
    }
    private SpiritSpells(){}
}
