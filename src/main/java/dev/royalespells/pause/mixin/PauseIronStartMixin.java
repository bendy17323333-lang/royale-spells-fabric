package dev.royalespells.pause.mixin;
import dev.royalespells.pause.ElectricPause;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.api.spells.AbstractSpell",remap=false)
public abstract class PauseIronStartMixin {
    @Inject(method="attemptInitiateCast",at=@At("HEAD"),cancellable=true,remap=false)
    private void holdNewCast(ItemStack stack,int level,Level world,Player player,CastSource source,boolean sound,String slot,CallbackInfoReturnable<Boolean> cir){
        if(ElectricPause.active(player))cir.setReturnValue(false);
    }
}
