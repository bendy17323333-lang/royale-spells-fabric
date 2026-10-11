package dev.royalespells.pause;
import dev.royalespells.RoyaleSpells;
import net.minecraft.world.entity.LivingEntity;
/** Electrical stun pauses existing actions without cancelling their windups. */
public final class ElectricPause {
    public static boolean active(LivingEntity e){return e.isAlive()&&(e.level().isClientSide?e instanceof PauseState s&&s.electricallyPaused():e.hasEffect(RoyaleSpells.ELECTRICAL_STUN));}
    private ElectricPause(){}
}
