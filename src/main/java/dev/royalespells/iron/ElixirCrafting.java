package dev.royalespells.iron;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class ElixirCrafting {
    public static boolean accepts(AbstractSpell spell){return spell instanceof RoyaleIronSpell;}
    public static ItemStack evolve(ItemStack input,Player player) {
        if(!(input.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll) || !ISpellContainer.isSpellContainer(input))return ItemStack.EMPTY;
        var base=ISpellContainer.get(input).getSpellAtIndex(0);
        if(!(base.getSpell() instanceof RoyaleIronSpell royal))return ItemStack.EMPTY;
        var target=switch(royal.profile){
            case ZAP -> IronSpellProfile.ZAP_EVOLUTION;
            case GIANT_SNOWBALL -> IronSpellProfile.GIANT_SNOWBALL_EVOLUTION;
            case GOBLIN_BARREL -> IronSpellProfile.GOBLIN_BARREL_EVOLUTION;
            default -> null;
        };
        if(target==null)return ItemStack.EMPTY;
        var evolved=IronIntegration.spell(target);
        if(!evolved.isEnabled() || !evolved.allowCrafting() || !evolved.canBeCraftedBy(player))return ItemStack.EMPTY;
        // Preserve progress toward maximum when a common card has more levels than its evolution.
        int max=base.getSpell().getMaxLevel();
        int level=1+Math.round((Math.min(max,Math.max(1,base.getLevel()))-1f)/Math.max(1,max-1)*(evolved.getMaxLevel()-1));
        var output=input.copyWithCount(1);ISpellContainer.createScrollContainer(evolved,level,output);return output;
    }
    private ElixirCrafting(){}
}
