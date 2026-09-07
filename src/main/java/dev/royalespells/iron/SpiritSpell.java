package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.entity.ElementalSpirit;
import dev.royalespells.spirit.SpiritElement;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** Staff-bound native summon. Shared cooldown prevents rotating four staffs for free bursts. */
public final class SpiritSpell extends AbstractSpell {
    public final SpiritElement element;private final DefaultConfig config;
    public SpiritSpell(SpiritElement element){this.element=element;config=new DefaultConfig().setSchoolResource(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",element.school)).setMinRarity(SpellRarity.RARE).setMaxLevel(1).setCooldownSeconds(6).setAllowCrafting(false).build();baseManaCost=element.mana;manaCostPerLevel=0;baseSpellPower=100;spellPowerPerLevel=15;castTime=12;}
    @Override public ResourceLocation getSpellResource(){return RoyaleSpells.id(element.spellId());}
    @Override public String getComponentId(){return "ironspell.royalespells."+element.spellId();}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return CastType.LONG;}
    @Override public boolean allowLooting(){return false;}
    @Override public Optional<SoundEvent> getCastFinishSound(){return Optional.empty();}
    private Vec3 placement(LivingEntity caster){return SpellEngine.ground(caster.level(),caster.position().add(SpellEngine.horizontal(caster.getViewVector(1)).scale(1.2)));}
    @Override public boolean checkPreCastConditions(Level level,int spellLevel,LivingEntity caster,MagicData data){
        if(!(level instanceof ServerLevel world)||!isEnabled()||caster.hasEffect(RoyaleSpells.STUN)||caster.hasEffect(RoyaleSpells.FROZEN))return false;
        if(caster instanceof ServerPlayer p&&SpiritSpells.remaining(p)>0)return false;
        Vec3 at=placement(caster);var pos=BlockPos.containing(at);
        if(!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||caster instanceof net.minecraft.world.entity.player.Player p&&!p.mayInteract(world,pos)||!world.noCollision(AABB.ofSize(at.add(0,.4,0),.6,.8,.6)))return false;
        int total=0,owned=0;for(var dimension:world.getServer().getAllLevels())for(var e:dimension.getAllEntities())if(e instanceof ElementalSpirit spirit&&spirit.isAlive()){if(dimension==world)total++;if(caster.getUUID().equals(spirit.ownerId()))owned++;}
        return total<96&&owned<6;
    }
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data){
        if(!(level instanceof ServerLevel world)||!checkPreCastConditions(level,spellLevel,caster,data))return;
        var spirit=RoyaleSpells.ELEMENTAL_SPIRIT.create(world);if(spirit==null)return;
        float power=net.minecraft.util.Mth.clamp(getSpellPower(spellLevel,caster)/100f,.05f,64);
        spirit.configure(caster.getUUID(),element,power);spirit.moveTo(placement(caster),caster.getYRot(),0);spirit.setYBodyRot(caster.getYRot());
        if(!world.addFreshEntity(spirit))return;
        IronSpellSystem.summon(spirit,caster.getUUID(),getSpellId(),spellLevel);
        if(caster instanceof ServerPlayer p)SpiritSpells.cooldown(p,Math.max(20,io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(this,p,source)));
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster){
        var info=new ArrayList<MutableComponent>();info.add(Component.translatable("ui.royalespells.spirit_rules"));info.add(Component.translatable("ui.royalespells.spirit_"+element.id()));
        float multiplier=caster!=null&&caster.getAttributes().hasAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE)?(float)caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SUMMON_DAMAGE):1;
        info.add(Component.translatable("ui.royalespells.spirit_damage",String.format(Locale.ROOT,"%.1f",element.damage*getSpellPower(level,caster)/100f*Math.max(0,multiplier))));
        if(element==dev.royalespells.spirit.SpiritElement.ELECTRO)info.add(Component.translatable("ui.royalespells.spirit_electro_timing"));
        info.add(Component.translatable("ui.royalespells.spirit_scaling"));return info;
    }
}
