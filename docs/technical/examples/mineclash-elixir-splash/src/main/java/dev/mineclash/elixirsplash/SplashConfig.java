// SPDX-License-Identifier: MIT
package dev.mineclash.elixirsplash;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SplashConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MAX_BURSTS;
    public static final ModConfigSpec.IntValue DISTANCE;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Replace only MineClash's final white death poof with elixir.").define("enabled", true);
        MAX_BURSTS = b.comment("Hard cap. Older splashes retire first during mass deaths.").defineInRange("maxBursts", 24, 1, 64);
        DISTANCE = b.comment("Maximum camera distance in blocks.").defineInRange("renderDistance", 48, 8, 96);
        SPEC = b.build();
    }
    private SplashConfig() {}
}
