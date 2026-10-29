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
public final class SkillTreeIconPreview {
    private static int stage=-1,ticks;
    private static CompletableFuture<Void> loading;
    private static void show(Minecraft mc){
        var d=new CompoundTag();var sd=new CompoundTag();sd.putBoolean("available",true);d.put("skillsData",sd);
        mc.setScreen(new JournalScreen(d,"skills"));
        mc.screen.mouseScrolled(mc.screen.width/2,mc.screen.height/2,-100);
    }
    @SubscribeEvent public static void label(net.minecraftforge.client.event.ScreenEvent.Render.Post e){
        if(Boolean.getBoolean("sunlit.skillTreeIconPreview")&&e.getScreen() instanceof JournalScreen)e.getGuiGraphics().drawString(Minecraft.getInstance().font,"아이콘 UI 검증 · 예시 데이터 · 전체 모드팩 플레이 화면 아님",8,e.getScreen().height-12,0xFFFFE7A0,false);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.skillTreeIconPreview"))return;
        var mc=Minecraft.getInstance();
        if(stage==-1){if(!(mc.screen instanceof TitleScreen))return;mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");loading=mc.reloadResourcePacks();stage=-2;return;}
        if(stage==-2){if(!loading.isDone())return;mc.options.guiScale().set(2);mc.resizeDisplay();show(mc);stage=0;ticks=0;return;}
        if(mc.getOverlay()!=null)return;
        ++ticks;
        if(stage==1&&ticks==10)mc.screen.mouseScrolled(mc.screen.width/2,mc.screen.height/2,-100);
        if(ticks<30)return;
        Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),"skill-tree-"+(stage==0?"normal":"small")+".png",mc.getMainRenderTarget(),c->{});
        if(stage==1){mc.stop();stage=2;return;}
        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,720);mc.options.guiScale().set(3);mc.resizeDisplay();show(mc);stage=1;ticks=0;
    }
}
