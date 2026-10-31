package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class NicknameShadowCheck {
    static int phase,ticks;static boolean inspect;static int inspected;static long start=System.currentTimeMillis();
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("NICKNAME_SHADOW_CHECK: "+why);}
    static EditBox edit()throws Exception{var s=Minecraft.getInstance().screen;require(s instanceof JournalScreen,"Profile not open");var f=JournalScreen.class.getDeclaredField("nickname");f.setAccessible(true);var e=(EditBox)f.get(s);require(e!=null,"No nickname input");return e;}
    static void shot(String name){var mc=Minecraft.getInstance();var root=new java.io.File(System.getProperty("sunlit.previewOutput"));new java.io.File(root,"screenshots").mkdirs();mc.getToasts().clear();Screenshot.grab(root,name+".png",mc.getMainRenderTarget(),c->{});}
    static final class Recorder extends GuiGraphics{
        final GuiGraphics target;int draws,shadows;final java.util.List<Integer> starts=new java.util.ArrayList<>(), widths=new java.util.ArrayList<>();
        Recorder(GuiGraphics g){super(Minecraft.getInstance(),g.bufferSource());target=g;}
        @Override public int drawString(Font f,FormattedCharSequence s,int x,int y,int color,boolean shadow){draws++;if(shadow)shadows++;starts.add(x);int end=target.drawString(f,s,x,y,color,shadow);widths.add(end-x);return end;}
        @Override public int drawString(Font f,Component s,int x,int y,int color,boolean shadow){draws++;if(shadow)shadows++;return target.drawString(f,s,x,y,color,shadow);}
        @Override public int drawString(Font f,String s,int x,int y,int color,boolean shadow){draws++;if(shadow)shadows++;return target.drawString(f,s,x,y,color,shadow);}
        @Override public void fill(int a,int b,int c,int d,int color){target.fill(a,b,c,d,color);}
        @Override public void fill(net.minecraft.client.renderer.RenderType r,int a,int b,int c,int d,int color){target.fill(r,a,b,c,d,color);}
        @Override public com.mojang.blaze3d.vertex.PoseStack pose(){return target.pose();}
    }
    @SubscribeEvent public static void rendered(net.minecraftforge.client.event.ScreenEvent.Render.Post e)throws Exception{
        if(!Boolean.getBoolean("sunlit.nicknameShadowCheck")||!(e.getScreen() instanceof JournalScreen))return;
        if(inspect){var r=new Recorder(e.getGuiGraphics());edit().renderWidget(r,-1,-1,0);require(r.draws>0&&r.shadows==0,"Input rendered shadow: "+r.shadows);if(r.starts.size()>=2)require(r.starts.get(1)==r.starts.get(0)+r.widths.get(0),"Caret split text overlapped or left a gap");inspected++;inspect=false;System.out.println("NICKNAME_NO_SHADOW_PASS draws="+r.draws);}
        e.getGuiGraphics().drawString(Minecraft.getInstance().font,"실제 게임 검증 · 라디오 볼펜 + 보정 모드 · 닉네임 입력 그림자 제거",6,e.getScreen().height-11,0xffffdfa0,false);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.nicknameShadowCheck"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<240000,"Timeout");if(mc.player==null||mc.getOverlay()!=null)return;if(++ticks<35)return;ticks=0;
        switch(phase++){
            case 0->{require(mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation("sunlit_font","textures/font/page_00.png")).isPresent(),"User font pack missing");var m=Font.class.getDeclaredMethod("getFontSet",net.minecraft.resources.ResourceLocation.class);m.setAccessible(true);var set=(net.minecraft.client.gui.font.FontSet)m.invoke(mc.font,new net.minecraft.resources.ResourceLocation("minecraft","default"));var glyph=set.getGlyphInfo('가',false);require(glyph.getClass().getName().contains("FixedGlyph")&&Math.abs(glyph.getBoldOffset()-.25)<.001,"Font correction mod not active");mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(null);mc.player.connection.sendCommand("sunlit journal");}
            case 1->{edit().setValue("예서 룰루 Sunlit 0123");edit().setFocused(false);inspect=true;}
            case 2->{shot("nickname-normal");var t=edit();require(mc.screen.mouseClicked(t.getX()+5,t.getY()+10,0),"Click missed");require(t.getCursorPosition()==0,"Caret did not align with text");t.setCursorPosition(2);inspect=true;}
            case 3->{shot("nickname-focused-middle");edit().setHighlightPos(0);inspect=true;}
            case 4->{shot("nickname-selection");var t=edit();t.setValue("가나다");t.setFocused(true);t.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT,0,0);t.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE,0,0);require(t.getValue().equals("가다"),"Native cursor/backspace broke");t.setValue("선릿밸리 닉네임 가독성 확인 0123456789");org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,720);mc.options.guiScale().set(3);mc.resizeDisplay();inspect=true;}
            case 5->{shot("nickname-small-long");JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("refresh",""));}
            case 6->{require(edit().getValue().equals("선릿밸리 닉네임 가독성 확인 0123456789"),"Refresh lost editing text");inspect=true;}
            case 7->{shot("nickname-small-refreshed");require(inspected>=5,"Render checks missing");System.out.println("NICKNAME_CLIENT_PASSED: real bitmap font and bold fix loaded; input shadow disabled; focused/unfocused/selection/long text, native caret/backspace, small GUI and refresh checked.");mc.stop();}
        }
    }
}
