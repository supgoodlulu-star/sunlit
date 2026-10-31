package dev.sunlit.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Small, rounded pastel buttons with the standard keyboard/narration behavior. */
final class JournalButton extends Button {
    private final boolean selected;
    private boolean notebook,bookmark;
    JournalButton notebook(boolean bookmark){this.notebook=true;this.bookmark=bookmark;return this;}
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
        if(notebook){
            int x=getX(),y=getY();boolean hover=isHoveredOrFocused();
            int fill=!active?0xFFCEBCA0:selected?0xFF64863F:bookmark?(hover?0xFF876039:0xFF60442E):(hover?0xFFE8C480:0xFFF0D39D);
            rounded(g,x,y+1,width,height-1,2,0xFF4C3422);
            rounded(g,x+1,y,width-2,height-1,2,0xFFC89A52);
            rounded(g,x+2,y+2,width-4,height-4,1,fill);
            g.fill(x+4,y+3,x+width-4,y+4,selected?0xFFAAC47A:bookmark?0xFFAC844B:0xFFFFE7B5);
            if(bookmark){g.fill(x+4,y+height-3,x+width-4,y+height-2,0xFFAE894E);g.fill(x+width/2-2,y+height-2,x+width/2+2,y+height,0xFFC89A52);}
            var font=Minecraft.getInstance().font;String label=font.plainSubstrByWidth(getMessage().getString(),Math.max(1,width-10));
            int ink=!active?0xFF78664F:selected||bookmark?0xFFFFEBC5:0xFF49321F;
            g.drawString(font,label,x+(width-font.width(label))/2,y+(height-font.lineHeight)/2,ink,false);
            if(isFocused())g.fill(x+4,y+height-4,x+width-4,y+height-3,0xFFB7D18D);
            return;
        }
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
