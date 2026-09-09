package dev.mineclash.zappies;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Raw Clash level values use one explicit conversion for both HP and damage. */
public final class ZappiesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue LEVEL,HIT_TICKS;
    public static final ModConfigSpec.DoubleValue SCALE;
    static{var b=new ModConfigSpec.Builder();
        LEVEL=b.comment("Clash Royale card level. Applied to newly deployed squads.").defineInRange("defaultLevel",11,3,16);
        SCALE=b.comment("Minecraft HP/damage = original card values * scale. 1/26 preserves MineClash's 4.5 damage at level 11; set 1 for literal card stats.").defineInRange("statScale",1.0/26,0.001,100);
        HIT_TICKS=b.comment("46 ticks = 2.3 seconds, September 2026 final balance announcement. Set 44 for the previous August balance.").defineInRange("hitIntervalTicks",46,20,200);
        SPEC=b.build();
    }
    public static int level(){return LEVEL.get();}
    public static int interval(){return HIT_TICKS.get();}
    public static double scale(){return SCALE.get();}
    public static final int[] HP={250,275,302,331,364,399,438,482,529,581,639,701,770,846};
    public static final int[] DAMAGE={55,61,67,73,80,88,97,107,117,129,142,155,171,188};
    public static int clampLevel(int value){return Math.clamp(value,3,16);}
}
