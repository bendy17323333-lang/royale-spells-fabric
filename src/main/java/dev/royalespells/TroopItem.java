package dev.royalespells;
import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.*;
import net.minecraft.text.Text;
import net.minecraft.client.item.TooltipContext;
import java.util.List;
public final class TroopItem extends Item {
    public final TroopCard card;
    public TroopItem(TroopCard card){super(new Settings().maxCount(1).rarity(Rarity.RARE));this.card=card;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        var stack=player.getStackInHand(hand);
        if(player.hasStatusEffect(RoyaleSpells.STUN) || !world.isClient && !SpellEngine.deploy((ServerPlayerEntity)player,card))return TypedActionResult.fail(stack);
        return TypedActionResult.success(stack,world.isClient);
    }
    @Override public void appendTooltip(ItemStack stack,World world,List<Text> lines,TooltipContext context) {
        lines.add(Text.translatable("tooltip.royalespells.cost",card.cost).formatted(Formatting.LIGHT_PURPLE));
        lines.add(Text.translatable("troop.royalespells."+card.id()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.royalespells.use").formatted(Formatting.DARK_GRAY));
    }
}
