// SPDX-License-Identifier: MIT
package dev.royalespells.mixin;

import dev.royalespells.iron.WitchTowerTemplates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

/** Uses the normal per-world structure cache and reload invalidation. Original
 * Iron IDs always take their original path. Explicit datapack overrides win. */
@Mixin(StructureTemplateManager.class)
public abstract class WitchTowerTemplateMixin {
    @Shadow private ResourceManager resourceManager;
    @Shadow protected abstract Optional<StructureTemplate> loadFromResource(ResourceLocation id);
    @Shadow public abstract StructureTemplate readStructure(CompoundTag data);
    @Inject(method="loadFromResource",at=@At("HEAD"),cancellable=true)
    private void royaleWitchTemplate(ResourceLocation id,CallbackInfoReturnable<Optional<StructureTemplate>> cir){
        if(!WitchTowerTemplates.owns(id)||!net.neoforged.fml.ModList.get().isLoaded("irons_spellbooks"))return;
        if(resourceManager.getResource(id.withPath("structure/"+id.getPath()+".nbt")).isPresent())return;
        cir.setReturnValue(loadFromResource(WitchTowerTemplates.source(id)).map(original ->
                readStructure(WitchTowerTemplates.adapt(original.save(new CompoundTag()),id))));
    }
}
