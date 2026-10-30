package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class FoxModelPreview {
 static int stage=-1,ticks;static java.util.concurrent.CompletableFuture<Void> loading;
 static class Gallery extends Screen {
  final int scene;
  Gallery(){super(Component.literal("Sunlit · Blockbench 여우 실제 렌더링"));scene=Math.max(0,Math.min(1,stage));}
  @Override public void render(GuiGraphics g,int x,int y,float partial){
   g.fill(0,0,width,height,0xfff8e7c6);g.drawCenteredString(font,"Sunlit · 첨부 Blockbench 여우 / 실제 Minecraft 렌더링",width/2,12,0xff573b2a);
   int cw=(width-24)/2,ch=(height-46)/2;
   String[] normal={"정면","옆에서 본 모습","뒷모습","사선에서 본 모습"};String[] states={"기본 표정","쓰다듬기 · 감은 눈","두루마리 가방 착용","이동 · 원본 앉은 자세의 가벼운 도약"};
   for(int n=0;n<4;n++){
    int px=12+(n%2)*cw,py=32+(n/2)*ch;g.fill(px+3,py+3,px+cw-3,py+ch-3,0xfffff1d9);
    float angle=scene==0?new float[]{180,90,0,152}[n]:n==2?32:152;
    PetRender.previewFit(g,"fox",scene==1&&n==2,px+cw/2,py+ch-25,cw-20,ch-38,angle,scene==1&&n==1,scene==1&&n==3?4:0,scene==1&&n==3);
    g.drawCenteredString(font,scene==0?normal[n]:states[n],px+cw/2,py+ch-16,0xff573b2a);
   }
  }
 }
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.foxModelPreview"))return;var mc=Minecraft.getInstance();
  if(stage==-1){if(!(mc.screen instanceof TitleScreen))return;mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");loading=mc.reloadResourcePacks();stage=-2;return;}
  if(stage==-2){if(!loading.isDone())return;mc.options.guiScale().set(2);mc.resizeDisplay();stage=0;mc.setScreen(new Gallery());ticks=0;return;}
  if(mc.getOverlay()!=null)return;if(++ticks<24)return;
  Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput")),stage==0?"fox-bb-angles.png":"fox-bb-states.png",mc.getMainRenderTarget(),c->{});
  if(++stage>=2){mc.stop();return;}mc.setScreen(new Gallery());ticks=0;
 }
}
