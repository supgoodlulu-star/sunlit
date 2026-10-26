package dev.sunlit.journal;
import net.minecraftforge.common.ForgeConfigSpec;
public final class JournalConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.IntValue FESTIVAL_GOAL;
    static {
        var b = new ForgeConfigSpec.Builder();
        DEBUG = b.comment("Log integration failures without player secrets").define("debug", false);
        FESTIVAL_GOAL = b.comment("Shared activity units per season").defineInRange("festivalGoal", 1000, 1, 1000000000);
        SPEC = b.build();
    }
}
