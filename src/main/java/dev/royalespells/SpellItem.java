package dev.royalespells;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.List;

public class SpellItem extends Item {
    public final Spell spell;
    public SpellItem(Spell spell) { super(new Item.Settings().maxCount(1).rarity(spell.evolved()?Rarity.EPIC:Rarity.RARE));this.spell=spell; }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        ItemStack stack=user.getStackInHand(hand);
        if(user.hasStatusEffect(RoyaleSpells.STUN)) return TypedActionResult.fail(stack);
        if(!world.isClient && !SpellEngine.cast((ServerPlayerEntity)user,spell)) return TypedActionResult.fail(stack);
        return TypedActionResult.success(stack,world.isClient);
    }
    @Override public boolean hasGlint(ItemStack stack) { return spell.evolved() || super.hasGlint(stack); }
    @Override public void appendTooltip(ItemStack stack,World world,List<Text> tooltip,TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.royalespells.cost",spell.cost).formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("spell.royalespells."+spell.id()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.royalespells.use").formatted(Formatting.DARK_GRAY));
        if(spell.evolved()) tooltip.add(Text.translatable("tooltip.royalespells.evolved").formatted(Formatting.LIGHT_PURPLE));
    }
}
