package dev.royalespells.mixin;

import java.util.*;
import io.redspace.ironsspellbooks.api.spells.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets="io.redspace.ironsspellbooks.loot.SpellFilter",remap=false)
public interface IronLootCacheAccess {
    @Accessor("SPELLS_FOR_SCHOOL") static Map<SchoolType,List<AbstractSpell>> royaleLoot(){throw new AssertionError();}
    @Accessor("SPELLS_FOR_SCHOOL_FORCED") static Map<SchoolType,List<AbstractSpell>> royaleForcedLoot(){throw new AssertionError();}
}
