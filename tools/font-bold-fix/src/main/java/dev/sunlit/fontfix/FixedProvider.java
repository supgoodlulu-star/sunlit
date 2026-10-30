package dev.sunlit.fontfix;
import com.mojang.blaze3d.font.*;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Owns precisely the provider returned by the targeted bitmap definition. */
public final class FixedProvider implements GlyphProvider {
    private final GlyphProvider original;
    private final ConcurrentHashMap<Integer,GlyphInfo> cache=new ConcurrentHashMap<>();
    public FixedProvider(GlyphProvider original){this.original=original;}
    @Override public GlyphInfo getGlyph(int codepoint){return cache.computeIfAbsent(codepoint,c->{GlyphInfo glyph=original.getGlyph(c);return glyph==null?null:new FixedGlyph(glyph);});}
    @Override public IntSet getSupportedGlyphs(){return original.getSupportedGlyphs();}
    @Override public void close(){cache.clear();original.close();}
    public record FixedGlyph(GlyphInfo original) implements GlyphInfo {
        @Override public float getAdvance(){return original.getAdvance();}
        @Override public float getBoldOffset(){return FontFix.BOLD_OFFSET.get().floatValue();}
        @Override public float getShadowOffset(){return original.getShadowOffset();}
        @Override public BakedGlyph bake(Function<SheetGlyphInfo,BakedGlyph> baker){return original.bake(baker);}
    }
}
