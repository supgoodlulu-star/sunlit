package dev.sunlit.journal;

import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Test-world-only real equip/chat packet check, excluded from the release JAR. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class ChatTierCheck {
    private static int phase,index,ticks;
    private static long start;
    private static CompletableFuture<Void> pending;
    private static final Map<String,Integer> received=new HashMap<>();
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("CHAT_TIER_CHECK: "+reason);}
    private static String token(String title){return "단계 확인 · "+title;}
    @SubscribeEvent public static void chat(ClientChatReceivedEvent e){
        if(!Boolean.getBoolean("sunlit.chatTierCheck"))return;
        String plain=e.getMessage().getString();
        for(String title:SunlitJournal.TITLES)if(plain.endsWith(token(title))){
            require(plain.startsWith(TitleIcons.glyph(title)+" ["+title+"] 햇살테스트: "),"Wrong delivered title/nickname prefix");
            final boolean[] image={false},color={false};
            e.getMessage().visit((style,text)->{
                if(text.contains(TitleIcons.glyph(title))&&style.getFont().equals(TitleIcons.FONT)&&style.getColor()!=null&&style.getColor().getValue()==0xFFFFFF)image[0]=true;
                if(text.contains("["+title+"]")&&style.getColor()!=null&&style.getColor().getValue()==TitleThemes.color(title,true))color[0]=true;
                return Optional.empty();
            },Style.EMPTY);
            require(image[0]&&color[0],"Image font or title color missing after network delivery");received.merge(title,1,Integer::sum);
        }
        if(plain.endsWith("칭호 해제 확인")){require(plain.startsWith("<햇살테스트>"),"Untitled nickname handler changed");require(plain.codePoints().noneMatch(c->c>=0xE200&&c<0xE21D),"Unequip retained artwork");received.merge("none",1,Integer::sum);}
    }
    @SubscribeEvent public static void label(net.minecraftforge.client.event.ScreenEvent.Render.Post e){
        if(Boolean.getBoolean("sunlit.chatTierCheck")&&e.getScreen() instanceof ChatScreen)e.getGuiGraphics().drawString(Minecraft.getInstance().font,"채팅 배지 검증 · 전용 테스트 월드 · 실제 장착/채팅 패킷",8,8,0xFFFFE7A0,false);
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.chatTierCheck"))return;
        if(start==0)start=System.currentTimeMillis();require(System.currentTimeMillis()-start<300000,"Timeout at "+phase+"/"+index);
        var mc=Minecraft.getInstance();
        if(mc.player==null){if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.active&&b.getMessage().getString().equals("진행")){b.onPress();break;}return;}
        if(mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;++ticks;mc.getToasts().clear();
        switch(phase){
            case 0 -> {if(ticks<30)return;mc.options.guiScale().set(3);mc.options.chatScale().set(1.0);mc.options.chatOpacity().set(1.0);mc.options.chatHeightFocused().set(1.0);mc.resizeDisplay();
                pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);SunlitJournal.saveOwned(p,new LinkedHashSet<>(Arrays.asList(SunlitJournal.TITLES)));SunlitJournal.data(p).remove("equipped");Integrations.nick(p,"햇살테스트",false);pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});phase=1;ticks=0;}
            case 1 -> {if(!pending.isDone())return;pending.join();mc.setScreen(new ChatScreen(""));JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("equip",SunlitJournal.TITLES[index]));phase=2;ticks=0;}
            case 2 -> {if(ticks<10)return;mc.player.connection.sendChat(token(SunlitJournal.TITLES[index]));phase=3;ticks=0;}
            case 3 -> {if(ticks<20)return;String title=SunlitJournal.TITLES[index];require(received.getOrDefault(title,0)==1,"Missing or duplicate chat for "+title);index++;
                if(index==29||index<=24&&index%4==0){phase=4;ticks=0;}else {phase=1;ticks=0;}}
            case 4 -> {if(ticks<12)return;String name=index==29?"seasons":new String[]{"farming","fishing","mining","cooking","ranching","wealth"}[index/4-1];shot(mc,"chat-tiers-"+name);
                if(index<29){phase=1;ticks=0;}else{JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("equip",SunlitJournal.TITLES[28]));phase=5;ticks=0;}}
            case 5 -> {if(ticks<10)return;mc.player.connection.sendChat("칭호 해제 확인");phase=6;ticks=0;}
            case 6 -> {if(ticks<20)return;require(received.getOrDefault("none",0)==1,"Unequipped delivery missing/duplicated");shot(mc,"chat-tiers-unequipped");mc.options.guiScale().set(2);mc.resizeDisplay();phase=7;ticks=0;}
            case 7 -> {if(ticks<15)return;shot(mc,"chat-tiers-scale2");com.mojang.logging.LogUtils.getLogger().info("CHAT_TIER_CHECK PASS: all 29 title images via actual equip and chat packets, 6 families x 4 tiers plus 5 seasonal titles, white bitmap icons and themed text, one delivery, unequip, GUI scales 2/3");phase=8;ticks=0;}
            case 8 -> {if(ticks>10)mc.stop();}
        }
    }
}
