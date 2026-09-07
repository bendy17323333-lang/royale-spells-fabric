package dev.royalespells.mixin;
import dev.royalespells.elixir.TowerPools;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(StructureStart.class)
public abstract class TowerPoolsMixin {
    @Inject(method="placeInChunk",at=@At("TAIL"))
    private void royalePools(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,CallbackInfo ci) {
        TowerPools.place((StructureStart)(Object)this,level,generator,clip);
    }
}
