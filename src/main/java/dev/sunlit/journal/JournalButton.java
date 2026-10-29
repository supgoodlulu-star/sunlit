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
        // Stepped corners instead of antialiased curves: pixel notebook geometry.
        int r=Math.min(2,Math.min(w,h)/2);
        g.fill(x+r,y,x+w-r,y+h,color);g.fill(x,y+r,x+w,y+h-r,color);
    }
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float delta) {
        int border=active?0xFF795127:0xFFC3B4A1;
        int color=!active?0xFFE0D5C6:selected?0xFF769748:isHoveredOrFocused()?0xFFE7C17E:0xFFF2D59A;
        rounded(g,getX(),getY()+1,width,height,7,border);
        rounded(g,getX()+1,getY(),width-2,height-1,6,color);
        var font=Minecraft.getInstance().font;
        String label=font.plainSubstrByWidth(getMessage().getString(),Math.max(1,width-12));
        g.drawString(font,label,getX()+(width-font.width(label))/2,getY()+(height-font.lineHeight)/2,!active?0xFF756354:selected?0xFFFFF9DD:0xFF3F3028,false);
        if(isFocused())rounded(g,getX()+6,getY()+height-3,width-12,1,0,0xFF9EBCAF);
    }
}
