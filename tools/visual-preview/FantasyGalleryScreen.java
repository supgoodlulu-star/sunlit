package dev.sunlit.journal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Test-only actual baked-model contact sheet; no concept illustrations. */
final class FantasyGalleryScreen extends Screen {
    private final int kind;
    private static final String[] NAMES={"검","곡괭이","도끼","삽","괭이"};
    FantasyGalleryScreen(int kind){super(Component.literal("테마별 입체 조형"));this.kind=kind;}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xFF171C2C);
        g.drawString(font,"Sunlit Fantasy · "+NAMES[kind]+" · 실제 게임용 3D 모델",18,12,0xFFFFEDD1,false);
        int cw=(width-28)/4,ch=(height-42)/3;
        for(int i=0;i<12;i++){
            int x=12+(i%4)*cw,y=32+(i/4)*ch;
            JournalButton.rounded(g,x,y,cw-5,ch-5,8,0xFF30374B);
            var skin=FantasyGacha.POOL.get(i*5+kind);
            float size=Math.min((cw-28)/16f,(ch-27)/16f);
            g.pose().pushPose();g.pose().translate(x+(cw-16*size)/2,y+4,0);g.pose().scale(size,size,size);
            g.renderItem(FantasyGachaScreen.preview(skin.id()),0,0);g.pose().popPose();
            g.drawString(font,skin.themeName(),x+9,y+ch-21,skin.color(),false);
        }
    }
    @Override public boolean isPauseScreen(){return false;}
}
