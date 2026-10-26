package dev.sunlit.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.Arrays;

/** One theme per saved title: darker ink on paper, brighter ink in the chat overlay. */
public final class TitleThemes {
    private TitleThemes() {}
    private static final int[][] PAPER={
        {0x426C39,0x376B46,0x88621F,0x776B27},
        {0x326F7D,0x28617F,0x37599B,0x3F5689},
        {0x635B74,0x6A5589,0x56519B,0x8A6525},
        {0x8C5B38,0x925238,0x67508F,0x8E4656},
        {0x667338,0x55713A,0x39765B,0x3A6B47},
        {0x836124,0x7C6A2D,0x906323,0x865022}
    };
    private static final int[][] CHAT={
        {0x9BDC86,0x78D594,0xF1D66F,0xD9DC7B},
        {0x8CDAE4,0x77CAEF,0x91B9FF,0xABC5FF},
        {0xC3B9DD,0xCAA5ED,0xB5ADFF,0xF2CF75},
        {0xF4C18D,0xFFAF85,0xC7ABF7,0xF4A6BA},
        {0xD3DF8E,0xB6D986,0x8EDBB3,0x9DDAA6},
        {0xF1D589,0xE4DC8D,0xFFCF6D,0xF2B97A}
    };
    public static boolean known(String title){return Arrays.asList(SunlitJournal.TITLES).contains(title);}
    public static int color(String title,boolean chat){
        int i=Arrays.asList(SunlitJournal.TITLES).indexOf(title);
        if(i>=0&&i<24)return (chat?CHAT:PAPER)[i/4][i%4];
        return switch(title){
            case "구름 위의 손님" -> chat?0xB7E8FF:0x3B6B87;
            case "하늘 정원의 수호자" -> chat?0xE3CBFF:0x71538B;
            case "사계요정" -> chat?0xE6BCFF:0x815293;
            case "벚꽃요정" -> chat?0xFFADCF:0x98506D;
            case "별빛요정" -> chat?0xAAC8FF:0x475D94;
            case "풍요요정" -> chat?0xF3B579:0x925432;
            case "눈꽃요정" -> chat?0xB7EEFF:0x376E81;
            default -> chat?0xFFFFFF:0x3F3028;
        };
    }
    public static MutableComponent label(String title,boolean chat){return Component.literal(title).withStyle(s->s.withColor(color(title,chat)));}
    public static MutableComponent badge(String title){return Component.literal("["+title+"]").withStyle(s->s.withColor(color(title,true)));}
}
