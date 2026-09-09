package dev.royalespells.iron;

import dev.royalespells.RoyaleSpells;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Delegates to the original spell lifecycle, including channelled spells and recasts. */
public final class MirrorIronSpell extends RoyaleIronSpell {
    private static final String HISTORY="RoyaleIronMirrorHistory";
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    public static MirrorHistoryPayload clientHistory=new MirrorHistoryPayload("",0);
    private static final class Session {
        final AbstractSpell target;final int targetLevel,mirrorLevel;boolean paid;
        Session(AbstractSpell target,int targetLevel,int mirrorLevel){this.target=target;this.targetLevel=targetLevel;this.mirrorLevel=mirrorLevel;}
    }
    public MirrorIronSpell(){super(IronSpellProfile.MIRROR);}
    public static void install() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,MirrorIronSpell::castEvent);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event)->ACTIVE.entrySet().removeIf(entry->{
            var player=event.getServer().getPlayerList().getPlayer(entry.getKey());if(player==null)return true;
            var data=MagicData.getPlayerMagicData(player);
            return !data.isCasting() && !data.getPlayerRecasts().hasRecastForSpell(entry.getValue().target.getSpellId());
        }));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->ACTIVE.clear());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player){var last=history(player);sync(player,last==SpellData.EMPTY?"":last.getSpell().getSpellId(),last.getLevel());}
        });
    }
    public static SpellData history(Player player) {
        var tag=player.getPersistentData().getCompound(HISTORY);var id=tag.getString("Id");
        if(id.isEmpty())return SpellData.EMPTY;
        var spell=SpellRegistry.getSpell(id);
        return spell==SpellRegistry.none() || spell instanceof MirrorIronSpell?SpellData.EMPTY:new SpellData(spell,Math.max(1,tag.getInt("Level")));
    }
    private static void castEvent(SpellOnCastEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var spell=SpellRegistry.getSpell(event.getSpellId());if(spell==SpellRegistry.none() || spell instanceof MirrorIronSpell)return;
        var session=ACTIVE.get(player.getUUID());
        if(session!=null && session.target==spell) {
            if(!session.paid) {
                var mirror=IronIntegration.spell(IronSpellProfile.MIRROR);
                event.setManaCost(event.getManaCost()+mirror.getManaCost(session.mirrorLevel));
                session.paid=true;
                if(event.getCastSource().respectsCooldown() && !(player.isCreative() && !ServerConfigs.CREATIVE_COOLDOWN.get()))
                    new MagicManager().addCooldown(player,mirror,event.getCastSource());
            }
            return; // A mirrored copy never becomes the next source, preventing unbounded level stacking.
        }
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Id",spell.getSpellId());tag.putInt("Level",event.getSpellLevel());
        player.getPersistentData().put(HISTORY,tag);
        sync(player,spell.getSpellId(),event.getSpellLevel());
    }
    private static void sync(ServerPlayer player,String id,int level){net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new MirrorHistoryPayload(id,level));}
    @Override public boolean attemptInitiateCast(ItemStack stack,int mirrorLevel,Level world,Player player,CastSource source,boolean cooldown,String slot) {
        if(!(player instanceof ServerPlayer serverPlayer) || world.isClientSide)return false;
        var data=MagicData.getPlayerMagicData(player);
        if(data.isCasting()){Utils.serverSideCancelCast(serverPlayer);return false;}
        if((player.hasEffect(RoyaleSpells.STUN)||dev.royalespells.pause.ElectricPause.active(player)) || player.hasEffect(RoyaleSpells.FROZEN))return fail(player,"message.royalespells.iron_stunned");
        var previous=history(player);if(previous==SpellData.EMPTY)return fail(player,"message.royalespells.iron_no_mirror");
        AbstractSpell target=previous.getSpell();
        if(!isEnabled() || !target.isEnabled() || target.requiresLearning() && !target.isLearned(player))return fail(player,"message.royalespells.iron_mirror_locked");
        var existing=ACTIVE.get(player.getUUID());
        boolean ownRecast=existing!=null && existing.target==target && data.getPlayerRecasts().hasRecastForSpell(target.getSpellId());
        if(!ownRecast) {
            var result=super.canBeCastedBy(mirrorLevel,source,data,player);
            if(!result.isSuccess()){if(result.message!=null)player.displayClientMessage(result.message,true);return false;}
            if(data.getPlayerRecasts().hasRecastForSpell(target.getSpellId()))return fail(player,"message.royalespells.iron_finish_recast");
        }
        int targetLevel=ownRecast?existing.targetLevel:Math.min(255,previous.getLevel()+1);
        if(source.consumesMana() && !ownRecast && !(player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get())
            && data.getMana()<target.getManaCost(targetLevel)+getManaCost(mirrorLevel))return fail(player,"message.royalespells.iron_mirror_mana");
        if(NeoForge.EVENT_BUS.post(new SpellPreCastEvent(player,getSpellId(),mirrorLevel,getSchoolType(),source)).isCanceled())return false;
        // Temporarily suppress only the source's cooldown during its normal permission check.
        // All learning, target, scroll, event and original override checks still run.
        var cooldowns=data.getPlayerCooldowns();var old=cooldowns.getSpellCooldowns().remove(target.getSpellId());
        var session=ownRecast?existing:new Session(target,targetLevel,mirrorLevel);ACTIVE.put(player.getUUID(),session);
        boolean started=false;
        try {started=target.attemptInitiateCast(stack,targetLevel,world,player,source,cooldown,slot);if(started && !ownRecast)dev.royalespells.SpellSounds.play(world,player.position(),dev.royalespells.Spell.MIRROR,"deploy");return started;}
        finally {
            if(old!=null)cooldowns.getSpellCooldowns().put(target.getSpellId(),old);
            if(!started && !ownRecast)ACTIVE.remove(player.getUUID());
        }
    }
    @Override public void castSpell(Level world,int level,ServerPlayer player,CastSource source,boolean cooldown) {
        // Administrative casts also enter the copied spell's lifecycle; they must not spawn an empty MIRROR effect.
        attemptInitiateCast(ItemStack.EMPTY,level,world,player,source,cooldown,"mainhand");
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster) {
        return List.of(Component.translatable("ui.royalespells.mirror_all"),Component.translatable("ui.royalespells.mirror_mana",getManaCost(level)),Component.translatable("ui.royalespells.mirror_native"));
    }
}
