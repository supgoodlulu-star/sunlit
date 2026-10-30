package dev.sunlit.fontfix.mixin;
import com.mojang.blaze3d.font.GlyphProvider;
import dev.sunlit.fontfix.FixedProvider;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MCP and SRG method names verified against Forge 47.3.0 / Minecraft 1.20.1.
 * Dual explicit selectors avoid shipping a development-only refmap. */
@Mixin(BitmapProvider.Definition.class)
public abstract class BitmapDefinitionMixin {
    @Inject(method={"load","m_286048_"},at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void sunlit$wrapFont(ResourceManager resources,CallbackInfoReturnable<GlyphProvider> cir){
        var file=((BitmapProvider.Definition)(Object)this).file();
        if(file.getNamespace().equals("sunlit_font")&&file.getPath().matches("font/page_[0-9]{2}\\.png")&&cir.getReturnValue()!=null)
            cir.setReturnValue(new FixedProvider(cir.getReturnValue()));
    }
}
