package dev.royalespells.pause.mixin;
import dev.royalespells.pause.ElectricPause;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.player.ClientMagicData",remap=false)
public abstract class PauseIronClientMixin {
    @Inject(method="handleCastDuration",at=@At("HEAD"),cancellable=true,remap=false)
    private static void holdClientCastTimer(CallbackInfo ci){var p=Minecraft.getInstance().player;if(p!=null&&ElectricPause.active(p))ci.cancel();}
}
