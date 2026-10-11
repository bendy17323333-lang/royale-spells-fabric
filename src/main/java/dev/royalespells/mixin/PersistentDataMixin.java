package dev.royalespells.mixin;
import dev.royalespells.PersistentState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class PersistentDataMixin implements PersistentState {
    @Unique private CompoundTag royaleData=new CompoundTag();
    public CompoundTag royalePersistentData(){return royaleData;}
    @Inject(method="saveWithoutId",at=@At("RETURN"))
    private void saveRoyale(CompoundTag tag,CallbackInfoReturnable<CompoundTag> cir){if(!royaleData.isEmpty())tag.put("RoyaleData",royaleData.copy());}
    @Inject(method="load",at=@At("TAIL"))
    private void loadRoyale(CompoundTag tag,CallbackInfo ci){royaleData=tag.getCompound("RoyaleData").copy();}
}
