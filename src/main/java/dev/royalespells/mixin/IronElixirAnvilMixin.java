package dev.royalespells.mixin;
import dev.royalespells.iron.*;
import dev.royalespells.elixir.ElixirContent;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu",remap=false)
public abstract class IronElixirAnvilMixin {
    @Inject(method="createResult",at=@At("TAIL"))
    private void royaleInkAndEvolution(CallbackInfo ci) {
        var menu=(ArcaneAnvilMenu)(Object)this;var input=menu.getSlot(0).getItem();var reagent=menu.getSlot(1).getItem();
        if(!(reagent.getItem() instanceof ElixirInkItem))return;
        if(!(input.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll) || !ISpellContainer.isSpellContainer(input)
            || !ElixirCrafting.accepts(ISpellContainer.get(input).getSpellAtIndex(0).getSpell())) {
            menu.getSlot(2).set(ItemStack.EMPTY);return;
        }
        if(reagent.is(ElixirContent.DARK_CONCENTRATE.get()) && io.redspace.ironsspellbooks.config.ServerConfigs.SCROLL_MERGING.get()) {
            var result=ElixirCrafting.evolve(input,((ItemCombinerPlayerAccess)menu).royalePlayer());
            if(!result.isEmpty())menu.getSlot(2).set(result);
        }
    }
}
