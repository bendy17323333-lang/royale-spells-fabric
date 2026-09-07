package dev.royalespells;

import net.minecraft.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public class SpellItem extends Item {
    public final Spell spell;
    public SpellItem(Spell spell) { super(new Item.Properties().stacksTo(1).rarity(spell.evolved()?Rarity.EPIC:Rarity.RARE));this.spell=spell; }
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player user,InteractionHand hand) {
        ItemStack stack=user.getItemInHand(hand);
        if(user.hasEffect(RoyaleSpells.STUN) || !IronSpellSystem.allowCard(user)) return InteractionResultHolder.fail(stack);
        if(!world.isClientSide && !SpellEngine.cast((ServerPlayer)user,spell)) return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
    @Override public boolean isFoil(ItemStack stack) { return spell.evolved() || super.isFoil(stack); }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> tooltip,TooltipFlag type) {
        tooltip.add(Component.translatable("tooltip.royalespells.cost",spell.cost).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("spell.royalespells."+spell.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.royalespells.use").withStyle(ChatFormatting.DARK_GRAY));
        if(IronSpellSystem.loaded)tooltip.add(Component.translatable("message.royalespells.use_iron_scroll").withStyle(ChatFormatting.GOLD));
        if(spell.evolved()) tooltip.add(Component.translatable("tooltip.royalespells.evolved").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
