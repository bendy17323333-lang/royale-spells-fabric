package dev.royalespells.mixin;

import dev.royalespells.CombatCompatibility;
import dev.royalespells.SpellEngine;
import dev.royalespells.entity.Summoned;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Narrow exception to Iron's generic monster-aggro guard; other listeners still receive the event. */
@Pseudo
@Mixin(targets="io.redspace.ironsspellbooks.player.ServerPlayerEvents",remap=false)
public abstract class IronTargetMixin {
    @Inject(method="onLivingChangeTarget",at=@At("HEAD"),cancellable=true,remap=false)
    private static void royaleExplicitEnemySummons(LivingChangeTargetEvent event,CallbackInfo ci) {
        var attacker=event.getEntity();var target=event.getNewAboutToBeSetTarget();
        if(!(attacker instanceof Summoned ours)||ours.ownerId()==null||target==null
            ||!(target instanceof IMagicSummon)||CombatCompatibility.ownerOf(target)==null
            ||SpellEngine.friendly(ours.ownerId(),target))return;
        // Keep every other condition from the same upstream listener intact.
        if(target.hasEffect(MobEffectRegistry.TRUE_INVISIBILITY))return;
        if(target.getType().is(ModTags.VILLAGE_ALLIES)&&attacker.getType().is(ModTags.VILLAGE_ALLIES))return;
        ci.cancel();
    }
}
