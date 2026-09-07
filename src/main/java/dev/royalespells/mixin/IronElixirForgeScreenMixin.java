package dev.royalespells.mixin;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen",remap=false)
public abstract class IronElixirForgeScreenMixin {
    @ModifyVariable(method="generateSpellList",at=@At("STORE"),ordinal=0)
    private List<AbstractSpell> royaleOnly(List<AbstractSpell> spells) {
        return ((ScrollForgeScreen)(Object)this).getMenu().getInkSlot().getItem().getItem() instanceof ElixirInkItem
            ?spells.stream().filter(ElixirCrafting::accepts).toList():spells;
    }
}
