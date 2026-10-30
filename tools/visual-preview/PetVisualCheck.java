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
public final class PetVisualCheck {
 static int stage=-1,ticks;static java.util.concurrent.CompletableFuture<Void> loading;
 static class Hero extends Screen {
  private static final String[][] SETS={
   {"fox","dog","cat","capybara","red_panda","koala"},
   {"spring_fairy","spring_dragon","spring_haetae","spring_skywhale","spring_tiger","winter_dragon"},
   {"mushroom","flowerbud","pearl","baby_dragon","griffin","book"}};
  final int scene;
  Hero(){super(Component.literal("Sunlit · 입체 외형 확대 검증"));scene=Math.max(6,Math.min(9,stage));}
  @Override public void render(GuiGraphics g,int x,int y,float partial){
   g.fill(0,0,width,height,0xfff9e7c5);g.drawCenteredString(font,"Sunlit · 실제 게임 모델 / "+(scene==9?"뒷모습 검증":"세부 외형 확대"),width/2,12,0xff573c2a);
   int cw=(width-24)/3,ch=(height-48)/2;String[] set=SETS[scene==9?1:scene-6];
   for(int n=0;n<6;n++){int px=12+(n%3)*cw,py=35+(n/3)*ch;var s=PetCatalog.find(set[n]).orElseThrow();g.fill(px+3,py+3,px+cw-3,py+ch-3,0xffffefd3);
    PetRender.previewFit(g,s.id(),false,px+cw/2,py+ch-29,cw-20,ch-43,scene==9?-28:152);g.drawCenteredString(font,s.name(),px+cw/2,py+ch-18,0xff573c2a);
   }
  }
 }
 static class Gallery extends Screen {
  Gallery(){super(Component.literal("Sunlit pet models"));}
  @Override public void render(GuiGraphics g,int x,int y,float partial){
   g.fill(0,0,width,height,0xfff9e7c5);g.fill(5,5,width-5,height-5,0xffedd5ac);
   g.drawCenteredString(font,"Sunlit · 실제 게임 3D 모델 검증 / "+(stage>=3?"전용 장식 착용":"기본 외형"),width/2,12,0xff563b2c);
   int start=stage%3*20;int cw=(width-20)/5,ch=(height-45)/4;
   for(int n=0;n<20;n++){var s=PetCatalog.ALL.get(start+n);int px=10+(n%5)*cw,py=32+(n/5)*ch;g.fill(px+2,py+2,px+cw-2,py+ch-2,0xffffedce);PetRender.previewFit(g,s.id(),stage>=3,px+cw/2,py+ch-23,cw-14,ch-32,152);g.drawCenteredString(font,s.name(),px+cw/2,py+ch-15,0xff64432d);}
  }
 }
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.petVisualCheck"))return;var mc=Minecraft.getInstance();
  if(stage==-1){if(!(mc.screen instanceof TitleScreen))return;mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");loading=mc.reloadResourcePacks();stage=-2;return;}
  if(stage==-2){if(!loading.isDone())return;mc.options.guiScale().set(2);mc.resizeDisplay();stage=0;mc.setScreen(new Gallery());ticks=0;return;}
  if(mc.getOverlay()!=null)return;if(++ticks<30)return;
  String name=stage<6?"pets-"+(stage%3==0?"life":stage%3==1?"fantasy":"legendary")+(stage>=3?"-accessories":"-base"):"detail-"+(stage==6?"animals":stage==7?"legends":stage==8?"fantasy":"rear");
  Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation/pets")),name+".png",mc.getMainRenderTarget(),c->{});
  if(++stage>=10){mc.stop();return;}
  if(stage==6){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1600,1100);mc.resizeDisplay();}
  mc.setScreen(stage<6?new Gallery():new Hero());ticks=0;
 }
}
