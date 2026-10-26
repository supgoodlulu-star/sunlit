package dev.sunlit.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Small, rounded pastel buttons with the standard keyboard/narration behavior. */
final class JournalButton extends Button {
    private final boolean selected;
    JournalButton(String label,int x,int y,int width,int height,boolean selected,Runnable action) {
        super(x,y,width,height,Component.literal(label),b->action.run(),DEFAULT_NARRATION);
        this.selected=selected;
    }
    static void rounded(GuiGraphics g,int x,int y,int w,int h,int radius,int color) {
        if(w<=0||h<=0)return;
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        g.fill(x+r,y,x+w-r,y+h,color);
        for(int dy=0;dy<h;dy++) {
            int cy=dy<r?r-1-dy:dy>=h-r?dy-(h-r):0;
            int inset=r==0?0:r-(int)Math.sqrt(Math.max(0,r*r-cy*cy));
            g.fill(x+inset,y+dy,x+r,y+dy+1,color);
            g.fill(x+w-r,y+dy,x+w-inset,y+dy+1,color);
        }
    }
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float delta) {
        int border=active?0xFFD5B8AC:0xFFE2D9D1;
        int color=!active?0xFFEDE7DF:selected?0xFFCFE9DC:isHoveredOrFocused()?0xFFFFE0D3:0xFFFFF1E7;
        rounded(g,getX(),getY()+1,width,height,7,border);
        rounded(g,getX()+1,getY(),width-2,height-1,6,color);
        var font=Minecraft.getInstance().font;
        g.drawCenteredString(font,getMessage(),getX()+width/2,getY()+(height-8)/2,active?0xFF70544D:0xFFAAA096);
        if(isFocused())rounded(g,getX()+6,getY()+height-3,width-12,1,0,0xFF9EBCAF);
    }
}
