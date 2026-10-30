package dev.sunlit.fontfix;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(FontFix.ID)
public final class FontFix {
    public static final String ID="sunlit_font_bold_fix";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue BOLD_OFFSET;
    static {
        var b=new ForgeConfigSpec.Builder();
        BOLD_OFFSET=b.comment("Synthetic bold offset for Sunlit Radio Ballpen bitmap glyphs, in GUI pixels. Vanilla is 1.0; 0 removes the bold displacement. Normal glyphs and shadows are unchanged.")
            .defineInRange("boldOffset",.25,0.0,1.0);
        SPEC=b.build();
    }
    public FontFix(){ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,SPEC);}
}
