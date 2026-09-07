package dev.royalespells;

import dev.royalespells.entity.SpellEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import java.util.UUID;

/** Optional boundary: no Iron's types appear in a standalone class signature. */
public final class IronSpellSystem {
    public static boolean loaded;
    public static void install(IEventBus bus) {
        loaded=ModList.get().isLoaded("irons_spellbooks");
        if(loaded)dev.royalespells.iron.IronIntegration.install(bus);
    }
    public static net.minecraft.world.item.Item elixirInk(boolean dark,int grade){return loaded?dev.royalespells.iron.IronIntegration.elixirInk(dark,grade):new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties());}
    public static boolean allowCard(Player player) {
        if(!loaded || player.isCreative() || player.level() instanceof ServerLevel world && ShowcaseMap.enabled(world))return true;
        if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("message.royalespells.use_iron_scroll"),true);
        return false;
    }
    public static void damage(SpellEntity effect,LivingEntity target,float amount) {
        CombatImpact.withoutKnockback(target,()->{
            if(loaded && !effect.ironSpellId().isEmpty())dev.royalespells.iron.IronIntegration.damage(effect,target,amount);
            else SpellEngine.hit((ServerLevel)effect.level(),effect.ownerId,target,amount);
            return null;
        });
    }
    public static void heal(SpellEntity effect,LivingEntity target,float amount) {
        if(loaded && !effect.ironSpellId().isEmpty())dev.royalespells.iron.IronIntegration.heal(effect,target,amount);
        else target.heal(amount);
    }
    public static void summon(Mob mob,UUID owner,String spell,int level) {
        if(loaded && mob!=null && !spell.isEmpty())dev.royalespells.iron.IronIntegration.summoned(mob,owner,spell,level);
    }
    public static net.minecraft.world.effect.MobEffect effect(net.minecraft.world.effect.MobEffectCategory category,int color) {
        return loaded?dev.royalespells.iron.IronIntegration.effect(category,color):new net.minecraft.world.effect.MobEffect(category,color){};
    }
    public static void interrupt(LivingEntity target) {
        if(loaded)dev.royalespells.iron.IronIntegration.interrupt(target);
    }
    public static void cloneNative(LivingEntity source,UUID owner,float power,String spell,int level) {
        if(loaded)dev.royalespells.iron.NativeClones.copy(source,owner,power,spell,level);
    }
    private IronSpellSystem() {}
}
