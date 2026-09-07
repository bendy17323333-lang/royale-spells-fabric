package dev.royalespells.mixin;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LightningBolt.class)
public abstract class SilentLightningMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void royale$respectSilent(Level world,double x,double y,double z,SoundEvent sound,SoundSource category,float volume,float pitch,boolean delay) {
        if(!((LightningBolt)(Object)this).isSilent())world.playLocalSound(x,y,z,sound,category,volume,pitch,delay);
    }
}
