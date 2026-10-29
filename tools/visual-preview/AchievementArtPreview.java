package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.nbt.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.CompletableFuture;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class AchievementArtPreview {
    private static int stage=-1,ticks;
    private static CompletableFuture<Void> loading;
    private static void align(Minecraft mc)throws ReflectiveOperationException {
        if(stage%2==0)return;
        var screen=(JournalScreen)mc.screen;var cf=JournalScreen.class.getDeclaredField("cards");cf.setAccessible(true);
        for(var card:(java.util.List<?>)cf.get(screen)){
            var tf=card.getClass().getDeclaredField("title");tf.setAccessible(true);if(!tf.get(card).equals("사계요정"))continue;
            var yf=card.getClass().getDeclaredField("y");yf.setAccessible(true);var sf=JournalScreen.class.getDeclaredField("scroll");sf.setAccessible(true);sf.setDouble(screen,yf.getInt(card));
            screen.mouseScrolled(screen.width/2,screen.height/2,0);break;
        }
    }
    private static void show(Minecraft mc)throws ReflectiveOperationException {
        var d=new CompoundTag();d.putBoolean("balanceAvailable",true);d.putLong("perSun",1);d.putLong("spurs",500);
        var seasons=new CompoundTag();seasons.putBoolean("SPRING",true);seasons.putBoolean("SUMMER",true);if(stage==3){seasons.putBoolean("AUTUMN",true);seasons.putBoolean("WINTER",true);var titles=new CompoundTag();titles.putString("owned","사계요정");d.put("titles",titles);}d.put("seasons",seasons);
        var screen=new JournalScreen(d,"achievements");mc.setScreen(screen);
        for(var child:screen.children())if(child instanceof JournalButton b&&b.getMessage().getString().equals(stage%2==0?"자산":"축제")){b.onPress();break;}
        align(mc);
    }
    @SubscribeEvent public static void label(net.minecraftforge.client.event.ScreenEvent.Render.Post e){
        if(Boolean.getBoolean("sunlit.achievementArtPreview")&&e.getScreen() instanceof JournalScreen)e.getGuiGraphics().drawString(Minecraft.getInstance().font,"업적 그림 UI 검증 · 예시 데이터 · 전체 모드팩 플레이 화면 아님",8,e.getScreen().height-12,0xFFFFE7A0,false);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws ReflectiveOperationException {
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.achievementArtPreview"))return;
        var mc=Minecraft.getInstance();
        if(stage==-1){if(!(mc.screen instanceof TitleScreen))return;mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");loading=mc.reloadResourcePacks();stage=-2;return;}
        if(stage==-2){if(!loading.isDone())return;mc.options.guiScale().set(2);mc.resizeDisplay();stage=0;show(mc);ticks=0;return;}
        if(mc.getOverlay()!=null)return;++ticks;if(ticks==10)align(mc);if(ticks<30)return;
        Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),"achievement-"+(stage%2==0?"wealth":"four-seasons")+(stage<2?"-normal":"-small")+".png",mc.getMainRenderTarget(),c->{});
        if(++stage==4){mc.stop();return;}
        if(stage==2){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,720);mc.options.guiScale().set(3);mc.resizeDisplay();}
        show(mc);ticks=0;
    }
}
