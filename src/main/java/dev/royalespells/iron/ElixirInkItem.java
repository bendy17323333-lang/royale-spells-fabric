package dev.royalespells.iron;
import dev.royalespells.elixir.ElixirContent;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;
public final class ElixirInkItem extends InkItem {
    public final boolean dark;
    public final int grade;
    public ElixirInkItem(boolean dark,int grade) {
        super(SpellRarity.values()[grade],dark?ElixirContent.DARK.source:ElixirContent.ELIXIR.source,new Item.Properties().rarity(grade>=3?Rarity.EPIC:grade>=2?Rarity.RARE:Rarity.UNCOMMON));
        this.dark=dark;this.grade=grade;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.royalespells.elixir_ink",getRarity().getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.royalespells.elixir_concentrate").withStyle(ChatFormatting.GRAY));
        if(dark)lines.add(Component.translatable(grade==4?"tooltip.royalespells.dark_evolve":"tooltip.royalespells.dark_concentrate").withStyle(ChatFormatting.DARK_PURPLE));
    }
    @Override public boolean isFoil(ItemStack stack){return dark && grade==4;}
}
