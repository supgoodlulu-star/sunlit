package dev.sunlit.journal;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common Java types only: safe to register even on a dedicated server. */
public final class JournalClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHIFT_AIR_OPEN;
    static {
        var builder=new ForgeConfigSpec.Builder();
        SHIFT_AIR_OPEN=builder.comment("Open the profile with sneak + use while pointing at empty air. Block/entity interactions are not intercepted.")
            .define("shiftAirOpen",true);
        SPEC=builder.build();
    }
}
