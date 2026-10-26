package dev.sunlit.journal;
import net.minecraftforge.common.ForgeConfigSpec;
public final class JournalConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.IntValue FESTIVAL_GOAL;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> TASK_TARGETS;
    static {
        var b = new ForgeConfigSpec.Builder();
        DEBUG = b.comment("Log integration failures without player secrets").define("debug", false);
        FESTIVAL_GOAL = b.comment("Shared activity units per season").defineInRange("festivalGoal", 1000, 1, 1000000000);
        TASK_TARGETS = b.comment("Concrete targets: activity|namespace:id|daily quantity|weekly quantity.",
            "Activities: farming (mature CropBlock), fishing (fish item), mining (natural ore block), cooking (edible output), ranching (baby entity).",
            "Modded blocks, items and animals may be added. Restart the server after editing. Existing tasks keep their targets.")
            .defineListAllowEmpty("taskTargets", TaskTargets.DEFAULTS, TaskTargets::valid);
        SPEC = b.build();
    }
}
