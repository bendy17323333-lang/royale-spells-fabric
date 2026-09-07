package dev.royalespells.mixin;
import dev.royalespells.client.IronCardUi;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay",remap=false)
public abstract class IronSpellWheelUiMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void royale$begin(GuiGraphics gui,DeltaTracker delta,CallbackInfo ci){IronCardUi.begin(IronCardUi.Surface.WHEEL);}
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void royale$icon(GuiGraphics gui,ResourceLocation texture,int x,int y,float u,float v,int w,int h,int tw,int th){IronCardUi.icon(IronCardUi.Surface.WHEEL,gui,texture,x,y,u,v,w,h,tw,th);}
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void royale$frame(GuiGraphics gui,ResourceLocation texture,int x,int y,int u,int v,int w,int h){IronCardUi.small(IronCardUi.Surface.WHEEL,gui,texture,x,y,u,v,w,h);}
}
