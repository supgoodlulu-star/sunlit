package dev.sunlit.journal;

import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** Server-safe title artwork keys and bitmap-font glyphs. No changes to title ownership or tiers. */
public final class TitleIcons {
    private TitleIcons() {}
    public static final ResourceLocation FONT=new ResourceLocation(SunlitJournal.MODID,"chat_titles");
    private static final String[] FIELDS={"farming","fishing","mining","cooking","ranching","wealth"};
    private static final String[] SEASONS={"four_seasons","spring","summer","autumn","winter"};
    private static int index(String title){return Arrays.asList(SunlitJournal.TITLES).indexOf(title);}
    public static String asset(String title){int i=index(title);return i<0?"":i<24?FIELDS[i/4]+"_"+(i%4+1):SEASONS[i-24];}
    public static String glyph(String title){int i=index(title);return i<0?"":Character.toString(0xE200+i);}
    public static int tier(String title){int i=index(title);return i>=0&&i<24?i%4+1:0;}
    public static ResourceLocation texture(String title){return new ResourceLocation(SunlitJournal.MODID,"textures/gui/title_badges/"+asset(title)+".png");}
    public static MutableComponent icon(String title){
        String hint=title+(tier(title)>0?" · "+tier(title)+"단계":"");
        return Component.literal(glyph(title)).withStyle(s->s.withFont(FONT).withColor(0xFFFFFF).withBold(false).withItalic(false)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal(hint))));
    }
}
