package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native legendary Fire summon: scrolls, scribing, elixir ink, mana and Mirror. */
public final class InfernoDragonSpell extends RoyaleIronSpell {
    public InfernoDragonSpell(){super(IronSpellProfile.INFERNO_DRAGON);}
    @Override public boolean checkPreCastConditions(Level level,int rank,LivingEntity caster,MagicData data){
        if(!super.checkPreCastConditions(level,rank,caster,data)||!(level instanceof ServerLevel world))return false;
        Vec3 at=SpellEngine.ground(world,target(caster));
        if(caster instanceof Player player&&!player.mayInteract(world,BlockPos.containing(at)))return fail(caster,"message.royalespells.deployment_blocked");
        int owned=0,total=0;for(var e:world.getAllEntities())if(e instanceof Summoned s){total++;if(caster.getUUID().equals(s.ownerId()))owned++;}
        if(total>=256||owned>=SpellEngine.MAX_UNITS_PER_OWNER||SpellEngine.dragonPlacement(world,at)==null)return fail(caster,"message.royalespells.deployment_blocked");
        return true;
    }
    @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData data){
        if(!(level instanceof ServerLevel world)||!checkPreCastConditions(level,rank,caster,data))return;
        var dragon=SpellEngine.summon(world,caster.getUUID(),SpellEngine.ground(world,target(caster)),"inferno_dragon",false);
        if(dragon==null)return;
        dragon.setYRot(caster.getYRot());dragon.setYBodyRot(caster.getYRot());dragon.setYHeadRot(caster.getYRot());
        SpellEngine.empower(dragon,power(rank,caster));IronIntegration.summoned(dragon,caster.getUUID(),getSpellId(),rank);
    }
    public static boolean damage(InfernoDragon dragon,LivingEntity target,float amount){
        var caster=CombatCompatibility.resolve((ServerLevel)dragon.level(),dragon.ownerId());
        if(caster instanceof LivingEntity living&&living.getAttributes().hasAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE))
            amount*=Math.max(0,(float)living.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE));
        var spell=IronIntegration.spell(IronSpellProfile.INFERNO_DRAGON);
        // Attribute the school damage to the caster once; summon-damage modifiers
        // are applied here, avoiding the generic melee summon listener's multiplier.
        var source=spell.getDamageSource(dragon,caster==null?dragon:caster);
        return io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(target,amount,source);
    }
    @Override public List<MutableComponent> getUniqueInfo(int rank,LivingEntity caster){
        float p=power(rank,caster);
        float summon=caster!=null&&caster.getAttributes().hasAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE)
            ?Math.max(0,(float)caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE)):1;
        float damage=p*summon; // Summon equipment increases damage, not maximum health.
        return List.of(Component.translatable("ui.royalespells.inferno_health",String.format(Locale.ROOT,"%.1f",InfernoDragon.BASE_HEALTH*p)),
            Component.translatable("ui.royalespells.inferno_damage",String.format(Locale.ROOT,"%.2f",damage),String.format(Locale.ROOT,"%.2f",InfernoDragon.tierDamage(41)*damage),String.format(Locale.ROOT,"%.2f",InfernoDragon.tierDamage(81)*damage)),
            Component.translatable("ui.royalespells.inferno_rules"));
    }
}
