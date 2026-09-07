package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** The native Iron's casting lifecycle owns mana, cooldowns, cast events and item consumption. */
public class RoyaleIronSpell extends AbstractSpell {
    public final IronSpellProfile profile;
    private final DefaultConfig config;
    public RoyaleIronSpell(IronSpellProfile profile) {
        this.profile=profile;
        config=new DefaultConfig().setSchoolResource(profile.schoolId()).setMinRarity(profile.rarity)
            .setMaxLevel(profile.maxLevel()).setCooldownSeconds(profile.cooldown).setAllowCrafting(true).build();
        baseManaCost=profile.mana;manaCostPerLevel=profile.manaPerLevel;
        baseSpellPower=100;spellPowerPerLevel=profile.powerPerLevel();castTime=profile.castTicks;
    }
    @Override public ResourceLocation getSpellResource(){return RoyaleSpells.id(profile.id());}
    @Override public String getComponentId(){return "ironspell.royalespells."+profile.id();}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return castTime==0?CastType.INSTANT:CastType.LONG;}
    @Override public Optional<SoundEvent> getCastFinishSound(){return Optional.empty();}
    @Override public int getManaCost(int level){return Math.max(1,super.getManaCost(level));}
    @Override public int getSpellCooldown(){return control()?ControlCooldown.TICKS:super.getSpellCooldown();}
    private boolean control(){return profile==IronSpellProfile.FREEZE||profile==IronSpellProfile.VINES;}
    @Override public void castSpell(Level level,int spellLevel,net.minecraft.server.level.ServerPlayer player,CastSource source,boolean triggerCooldown){
        if(control()&&!checkPreCastConditions(level,spellLevel,player,MagicData.getPlayerMagicData(player)))return;
        super.castSpell(level,spellLevel,player,source,triggerCooldown&&!control());
    }
    @Override public int getRecastCount(int level,LivingEntity caster){return profile==IronSpellProfile.BARBARIAN_BARREL_HERO?2:0;}
    private AllyZombie hero(LivingEntity caster) {
        return caster.level().getEntitiesOfClass(AllyZombie.class,caster.getBoundingBox().inflate(40),
            e->e.hero && caster.getUUID().equals(e.ownerId()) && e.isAlive() && e.nextReroll<=caster.level().getGameTime()).stream().findFirst().orElse(null);
    }
    public float power(int level,LivingEntity caster){return Mth.clamp(getSpellPower(level,caster)/100f,.05f,64);}
    public float durationScale(int level,LivingEntity caster) {
        float equipment=Mth.clamp(power(level,caster)/(1+profile.powerPerLevel()*(level-1)/100f)-1,0,2);
        return switch(profile) {
            case FREEZE -> 1;
            case VINES -> Mth.clamp((1.5f+.12f*(level-1)+.2f*equipment)/2.5f,.5f,1);
            case RAGE -> Mth.clamp((6f+.5f*(level-1)+equipment)/6f,1,1.75f);
            case WARMTH -> Mth.clamp((5f+.5f*(level-1)+equipment)/5f,1,1.75f);
            default -> 1;
        };
    }
    public Vec3 target(LivingEntity caster) {
        if(caster.isShiftKeyDown() && (profile==IronSpellProfile.RAGE || profile==IronSpellProfile.HEAL || profile==IronSpellProfile.WARMTH || profile==IronSpellProfile.CLONE))return caster.position();
        if(caster instanceof Mob mob && mob.getTarget()!=null)return mob.getTarget().position();
        return SpellEngine.aim(caster,32);
    }
    @Override public boolean checkPreCastConditions(Level level,int spellLevel,LivingEntity caster,MagicData data) {
        if(!(level instanceof ServerLevel world) || !isEnabled())return false;
        if(control()&&caster instanceof net.minecraft.server.level.ServerPlayer player&&ControlCooldown.remaining(player)>0)return fail(caster,"message.royalespells.control_cooldown");
        if(caster.hasEffect(RoyaleSpells.STUN) || caster.hasEffect(RoyaleSpells.FROZEN))return fail(caster,"message.royalespells.iron_stunned");
        if(profile==IronSpellProfile.BARBARIAN_BARREL_HERO && data.getPlayerRecasts().hasRecastForSpell(getSpellId()) && hero(caster)==null)
            return fail(caster,"message.royalespells.no_hero");
        Vec3 at=target(caster);BlockPos block=BlockPos.containing(at);
        if(!world.hasChunkAt(block) || !world.getWorldBorder().isWithinBounds(block))return fail(caster,"message.royalespells.deployment_blocked");
        int active=0,owned=0;
        for(var entity:world.getAllEntities()) {
            if(entity instanceof SpellEntity)active++;
            if(entity instanceof Summoned summon && caster.getUUID().equals(summon.ownerId()))owned++;
        }
        if(active>=256)return fail(caster,"message.royalespells.limit");
        if(profile==IronSpellProfile.BARBARIAN_HUT) {
            at=SpellEngine.ground(world,at);
            if(owned>=SpellEngine.MAX_UNITS_PER_OWNER || caster instanceof Player player && !player.mayInteract(world,BlockPos.containing(at))
                || !world.noCollision(AABB.ofSize(at.add(0,1.7,0),3.2,3.4,3.2)))return fail(caster,"message.royalespells.deployment_blocked");
        }
        return true;
    }
    protected static boolean fail(LivingEntity caster,String message) {
        if(caster instanceof Player player)player.displayClientMessage(Component.translatable(message),true);
        return false;
    }
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data) {
        if(!(level instanceof ServerLevel world))return;
        if(control()&&!checkPreCastConditions(level,spellLevel,caster,data))return;
        float strength=power(spellLevel,caster);Vec3 target=target(caster);
        if(profile==IronSpellProfile.BARBARIAN_BARREL_HERO && data.getPlayerRecasts().hasRecastForSpell(getSpellId())) {
            var hero=hero(caster);
            if(hero!=null) {
                var effect=SpellEntity.create(world,profile.card(),caster.getUUID(),hero.position(),hero.position().add(SpellEngine.horizontal(caster.getViewVector(1)).scale(3)));
                effect.rerollId=hero.getUUID();effect.reroll=true;effect.setPower(strength);effect.setIronSpell(getSpellId(),spellLevel,1);
                world.addFreshEntity(effect);hero.nextReroll=world.getGameTime()+200;
            }
            super.onCast(level,spellLevel,caster,source,data);return;
        }
        if(profile==IronSpellProfile.BARBARIAN_HUT) {
            var hut=SpellEngine.summon(world,caster.getUUID(),SpellEngine.ground(world,target),"barbarian_hut",false);
            SpellEngine.empower(hut,strength);
            if(hut instanceof RoyaleUnit building)building.lockFacing(caster.getYRot());
            IronSpellSystem.summon(hut,caster.getUUID(),getSpellId(),spellLevel);
        } else {
            Spell card=profile.card();Vec3 start=caster.getEyePosition().subtract(0,.25,0);
            if(card.rolling()) {
                Vec3 forward=SpellEngine.horizontal(caster.getViewVector(1));
                start=caster.position().add(forward.scale(1.1));target=start.add(forward.scale(card==Spell.THE_LOG?10:5));
            }
            var effect=SpellEntity.create(world,card,caster.getUUID(),start,target);
            effect.setPower(strength);effect.setIronSpell(getSpellId(),spellLevel,durationScale(spellLevel,caster));
            boolean added=world.addFreshEntity(effect);
            if(added&&control()&&caster instanceof net.minecraft.server.level.ServerPlayer player)ControlCooldown.start(player);
            if(added && card==Spell.GOBLIN_BARREL_EVOLUTION) {
                Vec3 side=SpellEngine.horizontal(caster.getViewVector(1)).cross(new Vec3(0,1,0)).scale(5);
                var decoy=SpellEntity.create(world,card,caster.getUUID(),start,SpellEngine.ground(world,target.add(side)));
                decoy.setPower(strength);decoy.setIronSpell(getSpellId(),spellLevel,1);decoy.decoy=true;world.addFreshEntity(decoy);
            }
        }
        if(profile==IronSpellProfile.BARBARIAN_BARREL_HERO)
            data.getPlayerRecasts().addRecast(new io.redspace.ironsspellbooks.capabilities.magic.RecastInstance(getSpellId(),spellLevel,2,600,source,null),data);
        super.onCast(level,spellLevel,caster,source,data);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster) {
        var info=new ArrayList<MutableComponent>();
        info.add(Component.translatable("ui.royalespells.effect_power",Math.round(power(level,caster)*100)));
        float base=switch(profile){case ARROWS->9;case FIREBALL->12;case ZAP->4;case LIGHTNING->22;case ROCKET,PARTY_ROCKET->30;case POISON->16;case THE_LOG,ZAP_EVOLUTION->8;case TORNADO,GIANT_SNOWBALL,GIANT_SNOWBALL_EVOLUTION->4;case EARTHQUAKE->9;case BARBARIAN_BARREL,BARBARIAN_BARREL_HERO->6;case ROYAL_DELIVERY->10;case GOBLIN_CURSE->6;default->0;};
        if(base>0)info.add(Component.translatable("ui.royalespells.damage_budget",String.format(Locale.ROOT,"%.1f",base*power(level,caster))));
        if(profile==IronSpellProfile.VOID)info.add(Component.translatable("ui.royalespells.void_budget",String.format(Locale.ROOT,"%.1f",36*power(level,caster)),String.format(Locale.ROOT,"%.1f",16.2*power(level,caster)),String.format(Locale.ROOT,"%.1f",7.2*power(level,caster))));
        if(profile==IronSpellProfile.HEAL)info.add(Component.translatable("ui.royalespells.heal_budget",String.format(Locale.ROOT,"%.1f",6*power(level,caster))));
        if(profile.card()!=null)info.add(Component.translatable("ui.royalespells.radius",profile.card().radius));
        if(profile.durationScales())info.add(Component.translatable("ui.royalespells.duration",String.format(Locale.ROOT,"%.1f",profile.card().duration*durationScale(level,caster)/20f)));
        if(control())info.add(Component.translatable("ui.royalespells.control_shared"));
        if(profile==IronSpellProfile.BARBARIAN_BARREL_HERO)info.add(Component.translatable("ui.royalespells.hero_recast"));
        if(profile==IronSpellProfile.RAGE || profile==IronSpellProfile.HEAL || profile==IronSpellProfile.WARMTH || profile==IronSpellProfile.CLONE)info.add(Component.translatable("ui.royalespells.self_target"));
        return info;
    }
}
