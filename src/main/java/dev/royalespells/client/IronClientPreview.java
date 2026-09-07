package dev.royalespells.client;

import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.world.entity.player.Player;

/** Isolated from the standalone client's class loading path. */
public final class IronClientPreview {
    public static RoyaleIronSpell selected(Player player) {
        AbstractSpell spell=preparedSpell(player);
        if(spell instanceof MirrorIronSpell && !MirrorIronSpell.clientHistory.spellId().isEmpty())spell=SpellRegistry.getSpell(MirrorIronSpell.clientHistory.spellId());
        return spell instanceof RoyaleIronSpell ours && !(ours instanceof MirrorIronSpell)?ours:null;
    }
    public static AbstractSpell preparedSpell(Player player) {
        if(player==null)return null;
        AbstractSpell spell=null;
        for(var stack:java.util.List.of(player.getMainHandItem(),player.getOffhandItem())) {
            if(ArmyMagic.horn(stack))return ArmyMagic.spell();
            boolean weapon=ISpellContainer.isSpellContainer(stack) && !ISpellContainer.get(stack).mustEquip();
            if(!(stack.getItem() instanceof Scroll) && !stack.has(io.redspace.ironsspellbooks.registries.ComponentRegistry.CASTING_IMPLEMENT) && !weapon)continue;
            if(ISpellContainer.isSpellContainer(stack)) {
                var container=ISpellContainer.get(stack);
                if(!container.isEmpty())spell=container.getSpellAtIndex(0).getSpell();
            } else {
                var manager=ClientMagicData.getSpellSelectionManager();
                if(manager!=null)spell=manager.getSelectedSpellData().getSpell();
            }
            // Follow the casting hand, including a selected native spell. Do not fall
            // through to an unrelated book spell just because it is one of ours.
            break;
        }
        return spell;
    }
    private IronClientPreview(){}
}
