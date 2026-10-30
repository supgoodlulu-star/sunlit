package dev.sunlit.journal;
import net.minecraftforge.common.ForgeConfigSpec;
public final class PetConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.ConfigValue<String> IRIDIUM;
 public static final ForgeConfigSpec.DoubleValue TICKETS_PER_HOUR;
 static {var b=new ForgeConfigSpec.Builder();IRIDIUM=b.comment("Physical iridium coin registry ID. Never substitutes brass or task rewards.").define("iridiumCoin","numismatics:sun",s->s instanceof String v&&net.minecraft.resources.ResourceLocation.isValidResourceLocation(v));TICKETS_PER_HOUR=b.comment("Expected tickets per hour of measured active life activity, not guaranteed. 0 disables drops.").defineInRange("ticketsPerActiveHour",1d,0d,60d);SPEC=b.build();}
}
