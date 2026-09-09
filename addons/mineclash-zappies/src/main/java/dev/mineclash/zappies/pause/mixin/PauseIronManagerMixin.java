package dev.mineclash.zappies.pause.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.mineclash.zappies.pause.ElectricPause;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
/** Skip only the casting branch. Mana regeneration, cooldowns and recasts still tick. */
@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.capabilities.magic.MagicManager",remap=false)
public abstract class PauseIronManagerMixin {
    @ModifyExpressionValue(method="lambda$tick$0",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/api/magic/MagicData;isCasting()Z",ordinal=0),remap=false)
    private boolean pauseWithoutCancel(boolean casting,boolean regen,Player player){return casting&&!ElectricPause.active(player);}
}
