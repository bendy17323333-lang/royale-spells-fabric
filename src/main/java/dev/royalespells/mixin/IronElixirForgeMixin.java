package dev.royalespells.mixin;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu",remap=false)
public abstract class IronElixirForgeMixin {
    @Shadow @Final private Slot inkSlot;
    @Shadow @Final private Slot blankScrollSlot;
    @Shadow @Final private Slot focusSlot;
    @Shadow @Final private Slot resultSlot;
    @Shadow private AbstractSpell spellRecipeSelection;
    @Unique private Player royalePlayer;
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",at=@At("RETURN"))
    private void royalePlayer(int id,Inventory inv,BlockEntity entity,CallbackInfo ci){royalePlayer=inv.player;}
    @Inject(method="setupResultSlot",at=@At("HEAD"),cancellable=true)
    private void royaleInk(AbstractSpell spell,CallbackInfo ci) {
        if(!(inkSlot.getItem().getItem() instanceof ElixirInkItem ink))return;
        ci.cancel();ItemStack result=ItemStack.EMPTY;
        if(ElixirCrafting.accepts(spell) && spell.isEnabled() && spell.allowCrafting() && royalePlayer!=null && spell.canBeCraftedBy(royalePlayer)
            && blankScrollSlot.getItem().is(Items.PAPER) && !inkSlot.getItem().isEmpty() && !focusSlot.getItem().isEmpty()
            && spell.getMinRarity()<=ink.grade && SchoolRegistry.getSchoolsFromFocus(focusSlot.getItem()).contains(spell.getSchoolType())) {
            int level=spell.getMinLevelForRarity(ink.getRarity());
            if(level>=spell.getMinLevel() && level<=spell.getMaxLevel())result=IronIntegration.scroll(((RoyaleIronSpell)spell).profile,level);
        }
        if(result.isEmpty())spellRecipeSelection=SpellRegistry.none();
        if(!ItemStack.matches(result,resultSlot.getItem()))resultSlot.set(result);
    }
}
