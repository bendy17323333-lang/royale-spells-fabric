package dev.mineclash.zappies.pause;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
/** Render-only local time. Offsets are per entity, never per renderer or model.
 * Accumulating the held interval makes an attack resume instead of jumping ahead. */
public final class PauseClock {
    private static final Map<LivingEntity,Clock> CLOCKS=new WeakHashMap<>();
    private static final class Clock { double offset,held;boolean paused; }
    public static double sample(LivingEntity e,float partial){
        var clock=CLOCKS.computeIfAbsent(e,k->new Clock());
        double now=e.tickCount+partial;
        boolean paused=ElectricPause.active(e);
        if(paused&&!clock.paused)clock.held=now;
        if(!paused&&clock.paused)clock.offset+=Math.max(0,now-clock.held);
        clock.paused=paused;
        return (paused?clock.held:now)-clock.offset;
    }
    private PauseClock(){}
}
