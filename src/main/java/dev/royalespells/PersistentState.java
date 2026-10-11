package dev.royalespells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
/** Private per-entity state. Fabric has no NeoForge persistent-data extension. */
public interface PersistentState {
    CompoundTag royalePersistentData();
    static CompoundTag of(Entity entity){return ((PersistentState)entity).royalePersistentData();}
}
