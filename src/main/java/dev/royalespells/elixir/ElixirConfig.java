package dev.royalespells.elixir;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
public final class ElixirConfig {
    private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue ELIXIR_CHANCE=B.comment("Chance per new Iron's pyromancer tower; 0 disables, 1 guarantees if suitable terrain exists.").defineInRange("elixirPoolChance",.65,0,1);
    public static final ModConfigSpec.DoubleValue DARK_CHANCE=B.comment("Chance for a finite dark elixir basin in the original tower basement.").defineInRange("darkElixirPoolChance",.35,0,1);
    public static final ModConfigSpec SPEC=B.build();
    public static void install(){ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON,SPEC,"royalespells-worldgen.toml");}
    private ElixirConfig(){}
}
