package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import dev.royalespells.elixir.DarkPoolBlock;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.*;

public final class ArmyMagic {
    public static final String HORN="RoyaleSkeletonArmyHorn";
    public static EvolvedArmySpell spell(){return (EvolvedArmySpell)IronIntegration.spell(IronSpellProfile.SKELETON_ARMY_EVOLUTION);}
    public static SpellData scrollData(ItemStack stack){return stack.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll&&ISpellContainer.isSpellContainer(stack)?ISpellContainer.get(stack).getSpellAtIndex(0):SpellData.EMPTY;}
    public static boolean skeletonScroll(ItemStack stack){var d=scrollData(stack);if(d==SpellData.EMPTY)return false;return Set.of("irons_spellbooks:raise_dead","royalespells:graveyard").contains(d.getSpell().getSpellId());}
    public static boolean armyScroll(ItemStack stack){var d=scrollData(stack);return d!=SpellData.EMPTY&&d.getSpell()==spell();}
    public static boolean horn(ItemStack stack){return stack.is(Items.GOAT_HORN)&&stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBoolean(HORN);}
    public static ItemStack enchantedHorn(ItemStack original){
        var horn=original.copyWithCount(1);var tag=horn.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean(HORN,true);horn.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        horn.set(DataComponents.INSTRUMENT,ArmySounds.INSTRUMENT);horn.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);return horn;
    }
    public static ItemStack evolvedScroll(){return IronIntegration.scroll(IronSpellProfile.SKELETON_ARMY_EVOLUTION,1);}
    public static BlockPos naturalSource(ItemEntity item){
        var at=item.blockPosition();for(var pos:List.of(at,at.below()))if(DarkPoolBlock.natural(item.level().getBlockState(pos))&&item.getY()<pos.getY()+1.25)return pos;return null;
    }
    private static boolean ready(ItemEntity item){return item.isAlive()&&item.level().getGameTime()>=item.getPersistentData().getLong("RoyaleRitualGrace");}
    public static void itemTick(ItemEntity item){
        if(item.level().isClientSide||item.tickCount%10!=0||!ready(item))return;
        var pos=naturalSource(item);if(pos==null)return;
        var stack=item.getItem();
        if(skeletonScroll(stack))RitualEntity.begin(item,null,pos,0);
        else if(stack.is(Items.GOAT_HORN)&&!horn(stack)){
            var matching=item.level().getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(2.5),other->other!=item&&ready(other)&&armyScroll(other.getItem())&&naturalSource(other)!=null);
            matching.stream().min(Comparator.comparingDouble(item::distanceToSqr)).ifPresent(scroll->RitualEntity.begin(item,scroll,pos,1));
        }
    }
    private static void use(PlayerInteractEvent.RightClickItem event){
        if(!horn(event.getItemStack()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);
        if(event.getLevel().isClientSide){event.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var data=MagicData.getPlayerMagicData(player);var spell=spell();
        if(data.isCasting()||!spell.isEnabled()||!spell.canBeCastedBy(1,CastSource.NONE,data,player).isSuccess()||!spell.checkPreCastConditions(player.level(),1,player,data))return;
        var pre=new io.redspace.ironsspellbooks.api.events.SpellPreCastEvent(player,spell.getSpellId(),1,spell.getSchoolType(),CastSource.NONE);if(NeoForge.EVENT_BUS.post(pre).isCanceled())return;
        spell.castSpell(player.level(),1,player,CastSource.NONE,false);
        if(!ArmyLedger.get(player.server).active(player.getUUID(),ArmyLedger.now(player.server)))return;
        player.startUsingItem(event.getHand());player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(Items.GOAT_HORN));
        player.level().gameEvent(net.minecraft.world.level.gameevent.GameEvent.INSTRUMENT_PLAY,player.position(),net.minecraft.world.level.gameevent.GameEvent.Context.of(player));
        event.setCancellationResult(InteractionResult.CONSUME);
    }
    public static void install(){
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post event)->{if(event.getEntity() instanceof ItemEntity item)itemTick(item);});
        NeoForge.EVENT_BUS.addListener(ArmyMagic::use);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event)->{if(horn(event.getItemStack())){event.getToolTip().add(Component.translatable("item.royalespells.army_horn.effect").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));event.getToolTip().add(Component.translatable("ui.royalespells.army_shared").withStyle(net.minecraft.ChatFormatting.GRAY));}});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event)->{if(event.getNewAboutToBeSetTarget() instanceof ArmySkeleton skeleton&&skeleton.ghost())event.setNewAboutToBeSetTarget(null);});
    }
    private ArmyMagic(){}
}
