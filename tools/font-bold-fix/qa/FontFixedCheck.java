package dev.sunlit.fontfix.qa;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid="sunlit_font_bold_fix",value=Dist.CLIENT)
public final class FontFixedCheck {
 static int phase,ticks;static long start=System.currentTimeMillis();
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.fontFixedCheck"))return;var mc=Minecraft.getInstance();if(System.currentTimeMillis()-start>150000)throw new IllegalStateException("Font QA timeout");if(mc.getOverlay()!=null||mc.screen==null)return;
  if(++ticks<60)return;ticks=0;
  if(phase++==0){mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Sample());}
  else if(phase==2){check(mc);mc.reloadResourcePacks();}
  else {check(mc);Screenshot.grab(new java.io.File(System.getProperty("sunlit.fontQaOutput","validation/fixed")),"bold-fixed.png",mc.getMainRenderTarget(),c->{});System.out.println("FONT_FIXED_CAPTURED");mc.stop();}
 }
 static void check(Minecraft mc){
  try{
   if(mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation("sunlit_font","textures/font/page_00.png")).isEmpty())throw new IllegalStateException("Test resource pack not loaded");
   var method=net.minecraft.client.gui.Font.class.getDeclaredMethod("getFontSet",net.minecraft.resources.ResourceLocation.class);method.setAccessible(true);
   var set=(net.minecraft.client.gui.font.FontSet)method.invoke(mc.font,new net.minecraft.resources.ResourceLocation("minecraft","default"));
   var glyph=set.getGlyphInfo('가',false);
   if(!(glyph instanceof dev.sunlit.fontfix.FixedProvider.FixedGlyph fixed))throw new IllegalStateException("Target font not wrapped: "+glyph.getClass().getName());
   if(Math.abs(glyph.getBoldOffset()-.25f)>.0001||glyph.getAdvance()!=fixed.original().getAdvance()||glyph.getShadowOffset()!=fixed.original().getShadowOffset())throw new IllegalStateException("Unexpected bold, normal advance or shadow offset");
   var uniform=(net.minecraft.client.gui.font.FontSet)method.invoke(mc.font,new net.minecraft.resources.ResourceLocation("minecraft","uniform"));
   if(uniform.getGlyphInfo('A',false) instanceof dev.sunlit.fontfix.FixedProvider.FixedGlyph)throw new IllegalStateException("Vanilla font modified");
   if(set.getGlyphInfo('$',false) instanceof dev.sunlit.fontfix.FixedProvider.FixedGlyph)throw new IllegalStateException("Fallback font modified");
   System.out.println("FONT_FIX_ASSERTIONS_PASSED: target offset .25, normal advance and shadow preserved, vanilla and fallback untouched");
  }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
 }
 static class Sample extends Screen{
  Sample(){super(Component.literal("Font comparison"));}
  public void render(GuiGraphics g,int mx,int my,float delta){
   g.fill(0,0,width,height,0xfff9edce);int x=24,y=30;
   g.drawString(font,"보정 적용 · 그림자 없음",x,y,0xff473025,false);
   String s="선릿밸리 채광 농사 업무 자산 0123456789";
   g.drawString(font,Component.literal("일반: "+s),x,y+35,0xff473025,false);
   g.drawString(font,Component.literal("볼드: "+s).withStyle(ChatFormatting.BOLD),x,y+65,0xff473025,false);
   g.fill(12,y+100,width-12,y+210,0xff27313a);
   g.drawString(font,Component.literal("일반: "+s),x,y+120,0xffffedce,false);
   g.drawString(font,Component.literal("볼드: "+s).withStyle(ChatFormatting.BOLD),x,y+150,0xffffedce,false);
   g.drawString(font,Component.literal("볼드 + 그림자: "+s).withStyle(ChatFormatting.BOLD),x,y+185,0xffffedce,true);
  }
 }
}
