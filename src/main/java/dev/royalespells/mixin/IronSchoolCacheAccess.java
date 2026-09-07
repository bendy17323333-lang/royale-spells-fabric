package dev.royalespells.mixin;

import java.util.*;
import io.redspace.ironsspellbooks.api.spells.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets="io.redspace.ironsspellbooks.api.registry.SpellRegistry",remap=false)
public interface IronSchoolCacheAccess {
    @Accessor("SCHOOLS_TO_SPELLS") static Map<SchoolType,List<AbstractSpell>> royaleSchools(){throw new AssertionError();}
}
