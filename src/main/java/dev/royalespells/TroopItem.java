package dev.royalespells;
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
public final class TroopItem extends Item {
    public final TroopCard card;
    public TroopItem(TroopCard card){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.card=card;}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player.hasEffect(RoyaleSpells.STUN) || !world.isClientSide && !SpellEngine.deploy((ServerPlayer)player,card))return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag type) {
        lines.add(Component.translatable("tooltip.royalespells.cost",card.cost).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("troop.royalespells."+card.id()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.royalespells.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
