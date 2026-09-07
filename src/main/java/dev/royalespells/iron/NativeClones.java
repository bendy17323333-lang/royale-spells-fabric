package dev.royalespells.iron;

import dev.royalespells.*;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.common.NeoForge;
import java.util.UUID;

/** Native owned summons retain their own AI, model and ownership manager. */
public final class NativeClones {
    public static final String MARKER="RoyaleIronClone";
    public static Mob copy(LivingEntity original,UUID owner,float power,String spell,int level) {
        if(!(original instanceof Mob source) || !(source instanceof IMagicSummon) || !(source.level() instanceof ServerLevel world)
            || source.getPersistentData().getBoolean(MARKER) || !owner.equals(CombatCompatibility.ownerOf(source)))return null;
        var caster=CombatCompatibility.resolve(world,owner);if(caster==null || SummonManager.getSummons(caster).size()>=SpellEngine.MAX_UNITS_PER_OWNER)return null;
        if(!(source.getType().create(world) instanceof Mob clone))return null;
        var tag=new CompoundTag();source.saveWithoutId(tag);
        for(var key:new String[]{"UUID","Pos","Motion","Passengers","Leash","active_effects"})tag.remove(key);
        clone.load(tag);clone.setUUID(UUID.randomUUID());clone.setPos(source.position().add(.7,0,0));clone.removeAllEffects();
        clone.getAttribute(Attributes.MAX_HEALTH).removeModifiers();clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);clone.setHealth(1);clone.setAbsorptionAmount(0);
        var attack=clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if(attack!=null && power!=1)attack.addPermanentModifier(new AttributeModifier(RoyaleSpells.id("clone_power"),power-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        for(var slot:EquipmentSlot.values())clone.setDropChance(slot,0);
        clone.setCanPickUpLoot(false);clone.setPersistenceRequired();clone.getPersistentData().putBoolean(MARKER,true);
        clone.getPersistentData().putLong("RoyaleCloneExpires",world.getGameTime()+400);
        clone.addEffect(new MobEffectInstance(RoyaleSpells.CLONED,400,0,false,false,false));
        SummonManager.setOwner(clone,caster);
        if(!world.addFreshEntity(clone)){SummonManager.removeSummon(clone);return null;}
        SummonManager.setDuration(clone,400);IronSpellSystem.summon(clone,owner,spell,level);return clone;
    }
    public static void install() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post event)->{
            if(event.getEntity() instanceof LivingEntity living && !living.level().isClientSide && living.getPersistentData().getBoolean(MARKER)
                && (!living.hasEffect(RoyaleSpells.CLONED) || living.level().getGameTime()>=living.getPersistentData().getLong("RoyaleCloneExpires")))living.discard();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDropsEvent event)->{
            if(event.getEntity().getPersistentData().getBoolean(MARKER))event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent event)->{
            if(event.getEntity().getPersistentData().getBoolean(MARKER))event.setDroppedExperience(0);
        });
    }
    private NativeClones(){}
}
