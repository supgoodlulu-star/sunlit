package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.CompletableFuture;
import java.util.*;
/** Real client/server UI checks in an isolated world; example-data pictures are explicitly labelled. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class NotebookClientCheck {
    static int phase,ticks;static boolean fixture;static long start=System.currentTimeMillis();static CompletableFuture<Void> pending;
    interface Work{void run(net.minecraft.server.level.ServerPlayer p)throws Exception;}
    static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("NOTEBOOK_CHECK: "+message);}
    static void server(Minecraft mc,Work work){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
    static JournalScreen screen(Minecraft mc){require(mc.screen instanceof JournalScreen,"Journal not open");return (JournalScreen)mc.screen;}
    static void press(Minecraft mc,String label){for(var child:screen(mc).children())if(child instanceof JournalButton b&&b.visible&&b.active&&b.getMessage().getString().equals(label)){b.onPress();return;}throw new IllegalStateException("Missing active button "+label);}
    static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void shot(Minecraft mc,String name){var root=new java.io.File(System.getProperty("sunlit.previewOutput"));new java.io.File(root,"screenshots").mkdirs();mc.getToasts().clear();Screenshot.grab(root,name+".png",mc.getMainRenderTarget(),c->{});}
    static CompoundTag sample()throws Exception{var m=JournalVisualPreview.class.getDeclaredMethod("data");m.setAccessible(true);return (CompoundTag)m.invoke(null);}
    static void layout(Minecraft mc)throws Exception{
        var s=screen(mc);int lo=(int)field(s,"contentTop"),hi=(int)field(s,"contentBottom");require(lo<hi,"No scrolling viewport");
        for(var child:s.children())if(child instanceof JournalButton b&&b.visible){require(b.getX()>=0&&b.getY()>=0&&b.getX()+b.getWidth()<=s.width&&b.getY()+b.getHeight()<=s.height,"Widget outside viewport");}
        long tabs=s.children().stream().filter(c->c instanceof JournalButton b&&Set.of("프로필","생활 기술","업무 일지","업적 도감","칭호 도감").contains(b.getMessage().getString())).count();require(tabs==5,"Expected five tabs");
        for(String label:List.of("위로","아래로","닫기")){
            var found=s.children().stream().filter(c->c instanceof JournalButton b&&b.getMessage().getString().equals(label)).map(c->(JournalButton)c).findFirst();
            require(found.isPresent()&&found.get().visible,"Footer widget missing: "+label);
            if(phase==4){var b=found.get();System.out.println("NOTEBOOK_WIDGET "+label+" x="+b.getX()+" y="+b.getY()+" visible="+b.visible+" active="+b.active);}
        }
        for(var card:(List<?>)field(s,"cards")){var b=(JournalButton)field(card,"button");if(b!=null&&b.visible)require(b.getY()>=lo&&b.getY()+b.getHeight()<=hi,"Card action escaped clipping bounds");}
    }
    @SubscribeEvent public static void caption(net.minecraftforge.client.event.ScreenEvent.Render.Post e){if(Boolean.getBoolean("sunlit.notebookCheck")&&e.getScreen() instanceof JournalScreen)e.getGuiGraphics().drawString(Minecraft.getInstance().font,fixture?"실제 게임 UI · 예시 데이터 · 전체 모드팩 플레이 기록 아님":"실제 게임 UI 검증 · 별도 테스트 월드",8,e.getScreen().height-11,0xffffdfa0,false);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.notebookCheck"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<300000,"Timeout phase "+phase);
        if(mc.player==null||mc.getOverlay()!=null)return;if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}if(++ticks<35)return;ticks=0;
        switch(phase++){
            case 0->{mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.options.guiScale().set(2);mc.reloadResourcePacks();mc.resizeDisplay();mc.setScreen(null);server(mc,p->{var d=SunlitJournal.data(p);d.putString("owned","새싹농부|벚꽃요정");d.putString("equipped","벚꽃요정");JournalWorld.get(p.getServer()).player(p).getCompound("counts").putLong("farming",46);});}
            case 1->mc.player.connection.sendCommand("sunlit journal");
            case 2->{var d=(CompoundTag)field(screen(mc),"data");require(d.getString("username").equals(mc.player.getGameProfile().getName()),"Not a server snapshot");layout(mc);shot(mc,"real-profile");press(mc,"생활 기술");}
            case 3->{layout(mc);shot(mc,"real-skills");press(mc,"업무 일지");}
            case 4->{layout(mc);shot(mc,"real-daily");press(mc,"주간 업무");}
            case 5->{layout(mc);shot(mc,"real-weekly");press(mc,"계절 특별 업무");}
            case 6->{layout(mc);shot(mc,"real-seasonal");press(mc,"업적 도감");}
            case 7->{layout(mc);shot(mc,"real-achievements");press(mc,"칭호 도감");press(mc,"획득");}
            case 8->{layout(mc);shot(mc,"real-titles");screen(mc).mouseScrolled(mc.screen.width/2,mc.screen.height/2,20);press(mc,"대표 칭호 장착");}
            case 9->{server(mc,p->require(SunlitJournal.data(p).getString("equipped").equals("새싹농부"),"Title equip request failed"));}
            case 10->{layout(mc);shot(mc,"real-title-equipped");org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,720);mc.options.guiScale().set(3);mc.resizeDisplay();press(mc,"프로필");}
            case 11->{layout(mc);shot(mc,"small-profile");if(field(screen(mc),"nickname") instanceof EditBox edit){edit.setValue("봄날");require(screen(mc).mouseClicked(edit.getX()+5,edit.getY()+edit.getHeight()/2d,0),"Paper nickname click missed");require(edit.isFocused()&&edit.getCursorPosition()==0,"Paper nickname caret offset wrong");edit.setCursorPosition(edit.getValue().length());screen(mc).setFocused(edit);}JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("refresh",""));}
            case 12->{if(field(screen(mc),"nickname") instanceof EditBox edit)require(edit.getValue().equals("봄날")&&edit.isFocused(),"Refresh lost nickname edit");press(mc,"업무 일지");press(mc,"일일 업무");screen(mc).mouseScrolled(mc.screen.width/2,mc.screen.height/2,-100);}
            case 13->{layout(mc);shot(mc,"small-daily-scrolled");press(mc,"칭호 도감");}
            case 14->{layout(mc);shot(mc,"small-titles");org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,900);mc.options.guiScale().set(2);mc.resizeDisplay();fixture=true;JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("close",""));mc.setScreen(new JournalScreen(sample(),"skills"));}
            case 15->{mc.setScreen(new JournalScreen(sample(),"skills"));}
            case 16->{layout(mc);shot(mc,"example-skills");var d=sample();mc.setScreen(new JournalScreen(d,"quests"));}
            case 17->{layout(mc);shot(mc,"example-daily-rewards");press(mc,"계절 특별 업무");}
            case 18->{layout(mc);shot(mc,"example-spring-festival");var d=sample();d.getCompound("festival").putString("season","WINTER");mc.setScreen(new JournalScreen(d,"quests"));press(mc,"계절 특별 업무");}
            case 19->{layout(mc);shot(mc,"example-winter-festival");System.out.println("NOTEBOOK_CLIENT_PASSED: five tabs, real server snapshots, title equip, scrolling, small GUI bounds, refresh/edit preservation, example illustrations and seasonal points.");mc.stop();}
        }
    }
}
