package dev.mineclash.zappies.mixin;
import dev.mineclash.zappies.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Reuse the existing MineClash egg including its original item art. */
@Mixin(SpawnEggItem.class)
public abstract class ZappyEggMixin {
    @Inject(method="useOn",at=@At("HEAD"),cancellable=true)
    private void threeAtOnce(UseOnContext c,CallbackInfoReturnable<InteractionResult> cir){
        if(!BuiltInRegistries.ITEM.getKey(c.getItemInHand().getItem()).toString().equals("mineclash:zappies_spawn_egg")||c.getLevel().getBlockState(c.getClickedPos()).is(Blocks.SPAWNER))return;
        if(c.getLevel().isClientSide){cir.setReturnValue(InteractionResult.SUCCESS);return;}
        var p=c.getPlayer();Vec3 at=Vec3.atCenterOf(c.getClickedPos()).add(0,.5,0);
        var group=ZappySquad.spawn((ServerLevel)c.getLevel(),at,p==null?0:p.getYRot(),p==null?null:p.getUUID(),ZappiesConfig.level());
        if(group.isEmpty()){if(p!=null)p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.zappiesaddon.no_space"),true);cir.setReturnValue(InteractionResult.FAIL);return;}
        if(p==null||!p.getAbilities().instabuild)c.getItemInHand().shrink(1);
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
