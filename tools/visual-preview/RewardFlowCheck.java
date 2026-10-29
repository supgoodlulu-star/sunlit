package dev.sunlit.journal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/** Pack item fixtures are test-only; this verifies packets and presentation, not the full modpack. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class RewardFlowCheck {
    private static int phase,ticks;private static long start;private static CompletableFuture<Void> pending;
    private static void require(boolean b,String reason){if(!b)throw new IllegalStateException("REWARD_CHECK: "+reason);}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){
        pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable e){pending.completeExceptionally(e);}});
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),name+".png",mc.getMainRenderTarget(),c->{});}
    private static void press(JournalScreen screen,String label){for(var c:screen.children())if(c instanceof JournalButton b&&b.getMessage().getString().equals(label)){require(b.active,"Disabled "+label);b.onPress();return;}throw new IllegalStateException("Missing button "+label);}
    @SubscribeEvent public static void previewLabel(net.minecraftforge.client.event.ScreenEvent.Render.Post e){
        if(Boolean.getBoolean("sunlit.rewardCheck")&&e.getScreen() instanceof JournalScreen)e.getGuiGraphics().drawString(Minecraft.getInstance().font,"개발 검증 화면 · 연동 아이템 아이콘은 대체 리소스",8,e.getScreen().height-12,0xFFFFE7A0,false);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.rewardCheck"))return;
        if(start==0)start=System.currentTimeMillis();require(System.currentTimeMillis()-start<180000,"Timeout "+phase);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;mc.getToasts().clear();ticks++;
        switch(phase){
            case 0 -> {mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.resizeDisplay();pending=mc.reloadResourcePacks();phase=1;ticks=0;}
            case 1 -> {if(!pending.isDone()||ticks<30)return;pending.join();server(mc,p->{
                p.getInventory().clearContent();var w=JournalWorld.get(p.getServer());w.player(p).remove("daily");w.player(p).remove("weekly");w.refreshTasks(p);
                var daily=w.player(p).getCompound("daily");
                for(int i:new int[]{1,3,4}){var t=daily.getCompound(""+i);t.putInt("progress",t.getInt("target"));}
                daily.getCompound("0").put("payout",TaskRewards.plan(0,false,0));
                var weekly=w.player(p).getCompound("weekly");var mine=weekly.getCompound("0");mine.putInt("kind",2);mine.putString("subject","minecraft:iron_ore");mine.putInt("target",32);mine.putInt("progress",32);mine.put("payout",TaskRewards.plan(2,true,2));
                JournalNetwork.send(p,true,"quests");
            });phase=2;ticks=0;}
            case 2 -> {if(!pending.isDone()||!(mc.screen instanceof JournalScreen)||ticks<25)return;pending.join();shot(mc,"rewards-daily");
                server(mc,p->{require(p.getInventory().countItem(SkyContent.FRAGMENT.get())==0,"Removed bonus issued fragments");var t=JournalWorld.get(p.getServer()).player(p).getCompound("daily").getCompound("4");require(!t.getBoolean("claimed"),"Unexpected claim");});phase=3;ticks=0;}
            case 3 -> {if(!pending.isDone()||ticks<10)return;pending.join();JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("claim","daily:4"));phase=4;ticks=0;}
            case 4 -> {if(ticks<20)return;server(mc,p->{var t=JournalWorld.get(p.getServer()).player(p).getCompound("daily").getCompound("4");require(t.getBoolean("claimed"),"Fifth-slot packet rejected");require(p.getInventory().countItem(TaskRewards.stack(TaskRewards.COIN,1).getItem())==2,"Daily coin count");});phase=5;ticks=0;}
            case 5 -> {if(!pending.isDone()||ticks<10)return;pending.join();press((JournalScreen)mc.screen,"주간 업무");phase=6;ticks=0;}
            case 6 -> {if(ticks<15)return;shot(mc,"rewards-weekly");JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("claim","weekly:0"));phase=7;ticks=0;}
            case 7 -> {if(ticks<20)return;server(mc,p->{require(p.getInventory().countItem(Items.IRON_INGOT)==96,"Weekly material count");require(p.getInventory().countItem(TaskRewards.stack(TaskRewards.COIN,1).getItem())==8,"Weekly currency count");});phase=8;ticks=0;}
            case 8 -> {if(!pending.isDone()||ticks<10)return;pending.join();shot(mc,"rewards-claimed");press((JournalScreen)mc.screen,"일일 업무");mc.options.guiScale().set(3);mc.resizeDisplay();phase=9;ticks=0;}
            case 9 -> {if(ticks<15)return;shot(mc,"rewards-small-scale");mc.screen.mouseScrolled(mc.screen.width/2,mc.screen.height/2,-12);phase=10;ticks=0;}
            case 10 -> {if(ticks<15)return;shot(mc,"rewards-bottom");
                com.mojang.logging.LogUtils.getLogger().info("REWARD_CHECK PASS: 5 daily tasks, no retired ticket bonus, actual fifth-slot and weekly claim packets, coins/material/96-item sync, task summary, GUI scales 2/3; pack-item placeholders used in this test only");mc.screen.onClose();phase=11;ticks=0;}
            case 11 -> {if(ticks<8)return;mc.options.guiScale().set(2);mc.resizeDisplay();server(mc,p->{SunlitJournal.saveOwned(p,new java.util.LinkedHashSet<>(java.util.List.of("새싹농부","꼬마낚시꾼")));JournalNetwork.send(p,true,"titles");});phase=12;ticks=0;}
            case 12 -> {if(!pending.isDone()||ticks<20||!(mc.screen instanceof JournalScreen))return;pending.join();press((JournalScreen)mc.screen,"획득");press((JournalScreen)mc.screen,"대표 칭호 장착");phase=13;ticks=0;}
            case 13 -> {if(ticks<20)return;server(mc,p->{require(SunlitJournal.data(p).getString("equipped").equals("새싹농부"),"Title equip packet did not persist");});phase=14;ticks=0;}
            case 14 -> {if(!pending.isDone()||ticks<10)return;pending.join();shot(mc,"titles-equipped");press((JournalScreen)mc.screen,"미획득");for(var c:mc.screen.children())if(c instanceof JournalButton b&&b.getMessage().getString().equals("미획득")&&b.getWidth()==110)require(!b.active,"Unowned title equip enabled");phase=18;ticks=0;}
            case 15 -> {if(ticks<20)return;server(mc,p->{require(SunlitJournal.data(p).getString("equipped").isEmpty(),"Title unequip packet failed");JournalNetwork.send(p,true,"profile");});phase=16;ticks=0;}
            case 16 -> {if(!pending.isDone()||ticks<20)return;pending.join();shot(mc,"profile-live");com.mojang.logging.LogUtils.getLogger().info("TITLE_CHECK PASS: independent tab, owned/unowned filters, equip/unequip server packets");phase=17;ticks=0;}
            case 18 -> {if(ticks<15)return;shot(mc,"titles-unowned");press((JournalScreen)mc.screen,"전체");press((JournalScreen)mc.screen,"칭호 해제");phase=15;ticks=0;}
            case 17 -> {if(ticks>5)mc.stop();}
        }
    }
}
