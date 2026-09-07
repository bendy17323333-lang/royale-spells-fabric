package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.ArmySkeleton;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** A single shared summon path for the native scroll, inscribed spell and enchanted horn. */
public final class EvolvedArmySpell extends RoyaleIronSpell {
    public static final int LIFETIME=20*60*10;
    public EvolvedArmySpell(){super(IronSpellProfile.SKELETON_ARMY_EVOLUTION);getDefaultConfig().setAllowCrafting(false);}
    @Override public int getSpellCooldown(){return 300;}
    @Override public boolean allowLooting(){return false;}
    @Override public boolean checkPreCastConditions(Level level,int spellLevel,LivingEntity caster,MagicData data){
        if(!super.checkPreCastConditions(level,spellLevel,caster,data)||!(level instanceof ServerLevel world))return false;
        var ledger=ArmyLedger.get(world.getServer());long now=ArmyLedger.now(world.getServer());
        if(ledger.active(caster.getUUID(),now))return fail(caster,"message.royalespells.army_alive");
        if(ledger.remaining(caster.getUUID(),now)>0||data.getPlayerCooldowns().isOnCooldown(this))return fail(caster,"message.royalespells.army_cooldown");
        return !placements(world,caster).isEmpty()||fail(caster,"message.royalespells.deployment_blocked");
    }
    private List<Vec3> placements(ServerLevel world,LivingEntity caster){
        var center=SpellEngine.ground(world,target(caster));var list=new ArrayList<Vec3>();
        Vec3 forward=SpellEngine.horizontal(caster.getViewVector(1)),side=forward.cross(new Vec3(0,1,0));
        for(int i=0;i<16;i++){
            Vec3 offset=ArmyFormation.offset(i,forward);
            Vec3 at=SpellEngine.ground(world,center.add(offset));var pos=net.minecraft.core.BlockPos.containing(at);
            if(!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||caster instanceof net.minecraft.world.entity.player.Player p&&!p.mayInteract(world,pos)
                ||!world.noCollision(AABB.ofSize(at.add(0,.8,0),.48,1.6,.48)))return List.of();
            list.add(at);
        }return list;
    }
    @Override public void castSpell(Level level,int spellLevel,ServerPlayer player,CastSource source,boolean triggerCooldown){
        if(!checkPreCastConditions(level,spellLevel,player,MagicData.getPlayerMagicData(player)))return;
        // Our persisted shared timer is authoritative even for one-use scrolls and creative casting.
        super.castSpell(level,spellLevel,player,source,false);
    }
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data){
        if(!(level instanceof ServerLevel world)||!checkPreCastConditions(level,spellLevel,caster,data))return;
        var locations=placements(world,caster);if(locations.size()!=16)return;
        UUID army=UUID.randomUUID();var units=new ArrayList<ArmySkeleton>();float power=power(spellLevel,caster);
        for(int i=0;i<16;i++){
            var unit=RoyaleSpells.ARMY_SKELETON.create(world);if(unit==null)return;
            unit.enlist(caster.getUUID(),army,i==0,4*power,1.8f*power,LIFETIME);
            unit.moveTo(locations.get(i),caster.getYRot(),0);unit.setYBodyRot(caster.getYRot());units.add(unit);
        }
        long now=ArmyLedger.now(world.getServer());int cooldown=caster instanceof ServerPlayer player?io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(this,player,CastSource.SPELLBOOK):getSpellCooldown();
        var ledger=ArmyLedger.get(world.getServer());ledger.start(caster.getUUID(),army,units.getFirst().getUUID(),now+LIFETIME,now+cooldown);
        for(var unit:units)if(!world.addFreshEntity(unit)){
            ledger.finish(caster.getUUID(),army);units.forEach(Entity::discard);return;
        }
        for(var unit:units)IronSpellSystem.summon(unit,caster.getUUID(),getSpellId(),spellLevel);
        dev.royalespells.entity.EvolutionBurst.deploy(world,locations.get(8),2.1f);
        data.getPlayerCooldowns().addCooldown(this,cooldown);
        if(caster instanceof ServerPlayer player)data.getPlayerCooldowns().syncToPlayer(player);
        world.playSound(null,source==CastSource.NONE?caster.blockPosition():units.getFirst().blockPosition(),ArmySounds.DEPLOY,SoundSource.PLAYERS,.9f,1);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster){return List.of(Component.translatable("ui.royalespells.army_count"),Component.translatable("ui.royalespells.army_general"),Component.translatable("ui.royalespells.army_shared"));}
}
