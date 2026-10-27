package dev.sunlit.journal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in client acceptance, real packet handling and disposable test-world state only. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class FantasyCollectionCheck {
    private static int phase,ticks,theme;private static long start;
    private static CompletableFuture<Void> pending;
    private static void require(boolean b,String message){if(!b)throw new IllegalStateException("FANTASY_CHECK: "+message);}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> run){
        pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{run.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable e){pending.completeExceptionally(e);}});
    }
    private static void press(FantasyGachaScreen screen,String label){
        for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(label)){require(b.active,"Disabled "+label);b.onPress();return;}throw new IllegalStateException("Missing "+label);
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.fantasyCheck"))return;
        if(start==0)start=System.currentTimeMillis();require(System.currentTimeMillis()-start<240000,"Timeout "+phase);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;ticks++;
        switch(phase){
            case 0 -> {
                mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.resizeDisplay();pending=mc.reloadResourcePacks();phase=1;ticks=0;
            }
            case 1 -> {
                if(!pending.isDone()||ticks<30)return;pending.join();
                for(var s:FantasyGacha.POOL){
                    var stack=FantasyGachaScreen.preview(s.id());var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                    require(model.getParticleIcon().contents().name().toString().equals("sunlit_journal_core:item/fantasy_"+s.theme()),"Wrong model "+s.id());
                    require(model.getQuads(null,null,net.minecraft.util.RandomSource.create()).size()>200,"Empty OBJ "+s.id());
                }
                server(mc,p->{p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(p.getServer().overworld(),0.5,140,0.5,0,15);p.getAbilities().flying=true;p.onUpdateAbilities();p.getServer().overworld().setDayTime(6000);p.getServer().overworld().setWeatherParameters(0,6000,false,false);p.getInventory().clearContent();JournalWorld.get(p.getServer()).player(p).remove("fantasyGacha");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));p.getInventory().add(new ItemStack(SkyContent.TICKET.get(),64));p.containerMenu.broadcastChanges();});phase=2;ticks=0;
            }
            case 2 -> {
                if(!pending.isDone()||ticks<10)return;pending.join();JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("open","gacha"));phase=3;ticks=0;
            }
            case 3 -> {
                if(!(mc.screen instanceof FantasyGachaScreen screen)||ticks<25)return;mc.getToasts().clear();shot(mc,"fantasy-gacha-initial");press(screen,"1회 뽑기 · 뽑기권 1장");phase=4;ticks=0;
            }
            case 4 -> {
                if(ticks<55)return;shot(mc,"fantasy-gacha-single");server(mc,p->{require(FantasyGacha.tickets(p)==63,"Single cost");require(FantasyGacha.state(p).getLong("sequence")==1,"Single reward");});phase=5;ticks=0;
            }
            case 5 -> {
                if(!pending.isDone()||ticks<10)return;pending.join();var screen=(FantasyGachaScreen)mc.screen;
                // Use transformed mouse input rather than invoking the draw callback for the batch.
                float scale=Math.min((screen.width-8)/960f,(screen.height-8)/600f);float ox=(screen.width-960*scale)/2,oy=(screen.height-600*scale)/2;
                require(screen.mouseClicked(ox+565*scale,oy+570*scale,0),"Scaled click missed ten draw");phase=6;ticks=0;
            }
            case 6 -> {
                if(ticks<80)return;shot(mc,"fantasy-gacha-ten");server(mc,p->{require(FantasyGacha.tickets(p)==53,"Batch cost");require(FantasyGacha.state(p).getList("results",Tag.TAG_COMPOUND).size()==10,"Batch results");});phase=7;ticks=0;
            }
            case 7 -> {
                if(!pending.isDone()||ticks<10)return;pending.join();press((FantasyGachaScreen)mc.screen,"곡괭이");phase=8;ticks=0;
            }
            case 8 -> {
                if(ticks<10)return;shot(mc,String.format("fantasy-theme-%02d",theme));
                if(++theme<12){press((FantasyGachaScreen)mc.screen,"다음 테마");ticks=0;return;}
                press((FantasyGachaScreen)mc.screen,"확률·이용 안내");phase=9;ticks=0;
            }
            case 9 -> {
                if(ticks<10)return;shot(mc,"fantasy-gacha-rates");mc.screen.mouseClicked(1,1,0);
                mc.options.guiScale().set(3);mc.resizeDisplay();phase=10;ticks=0;
            }
            case 10 -> {
                if(ticks<15)return;shot(mc,"fantasy-gacha-scale3");
                server(mc,p->{var state=FantasyGacha.state(p);state.putInt("dust",5);state.getCompound("owned").remove("heaven_relic_pickaxe");JournalNetwork.send(p,false,"");});phase=11;ticks=0;
            }
            case 11 -> {
                if(!pending.isDone()||ticks<15)return;pending.join();press((FantasyGachaScreen)mc.screen,"별가루 5개로 교환");phase=12;ticks=0;
            }
            case 12 -> {
                if(ticks<15)return;press((FantasyGachaScreen)mc.screen,"손에 든 장비에 스킨 적용");phase=13;ticks=0;
            }
            case 13 -> {
                if(ticks<20)return;var stack=mc.player.getMainHandItem();require(stack.hasTag()&&stack.getTag().getString(FantasySkins.KEY).equals("heaven_relic_pickaxe"),"Applied marker not synced");
                shot(mc,"fantasy-gacha-equipped");server(mc,p->{FantasyGacha.deliver(p,new ItemStack(SkyContent.FRAGMENT.get(),10));JournalNetwork.send(p,false,"");});phase=16;ticks=0;
            }
            case 16 -> {
                if(!pending.isDone()||ticks<15)return;pending.join();press((FantasyGachaScreen)mc.screen,"조각 10개 → 뽑기권 1장");phase=17;ticks=0;
            }
            case 17 -> {
                if(ticks<15)return;server(mc,p->{require(FantasyGacha.tickets(p)==54&&FantasyGacha.fragments(p)==0,"Fragment exchange packet count: tickets="+FantasyGacha.tickets(p)+", fragments="+FantasyGacha.fragments(p));});phase=18;ticks=0;
            }
            case 18 -> {if(!pending.isDone()||ticks<10)return;pending.join();shot(mc,"fantasy-gacha-fragments");mc.screen.onClose();phase=14;ticks=0;}
            case 14 -> {
                if(ticks<10)return;mc.player.setXRot(20);shot(mc,"fantasy-held-pickaxe");
                com.mojang.logging.LogUtils.getLogger().info("FANTASY_CHECK PASS: all 60 baked OBJ models, actual 1/10 draw packets and scaled click, 12 theme previews, rates, GUI scales, duplicate exchange, 10-fragment exchange and equipped S2C sync");phase=15;ticks=0;
            }
            case 15 -> {if(ticks>5)mc.stop();}
        }
    }
}
