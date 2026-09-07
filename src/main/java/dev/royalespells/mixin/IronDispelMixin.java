package dev.royalespells.mixin;

import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={SpellEntity.class,AllyZombie.class,AllySkeleton.class,RoyaleUnit.class,ElementalSpirit.class},remap=false)
public abstract class IronDispelMixin implements AntiMagicSusceptible {
    @Override public void onAntiMagic(MagicData data){((Entity)(Object)this).discard();}
}
