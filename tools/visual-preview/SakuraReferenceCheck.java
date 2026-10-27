package dev.sunlit.journal;

import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import java.util.concurrent.CompletableFuture;

/** Test-only presentation of actual baked models; not an illustration or a shipping screen. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class SakuraReferenceCheck {
    private static int phase,ticks;private static long start;private static CompletableFuture<Void> pending;
    private static void require(boolean b,String message){if(!b)throw new IllegalStateException("REFERENCE_CHECK: "+message);}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> run){
        pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{run.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable e){pending.completeExceptionally(e);}});
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.referenceCheck"))return;
        if(start==0)start=System.currentTimeMillis();require(System.currentTimeMillis()-start<180000,"Timeout "+phase);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;mc.getToasts().clear();ticks++;
        switch(phase){
            case 0 -> {mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.resizeDisplay();pending=mc.reloadResourcePacks();phase=1;ticks=0;}
            case 1 -> {
                if(!pending.isDone()||ticks<25)return;pending.join();
                for(String id:new String[]{"sakura_sword","sakura_pickaxe"}){
                    var model=mc.getItemRenderer().getModel(FantasyGachaScreen.preview(id),mc.level,mc.player,0);var quads=model.getQuads(null,null,net.minecraft.util.RandomSource.create());require(quads.size()>200,"Empty model");
                    for(var q:quads)require(q.getSprite().contents().name().toString().startsWith("sunlit_journal_core:item/fantasy_materials/sakura/reference_"),"Old or missing material");
                }
                mc.setScreen(new Comparison());phase=2;ticks=0;
            }
            case 2 -> {if(ticks<20)return;shot(mc,"sakura-reference-pair");server(mc,p->{
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(p.getServer().overworld(),.5,140,.5,0,15);p.getAbilities().flying=true;p.onUpdateAbilities();p.getServer().overworld().setDayTime(6000);p.getServer().overworld().setWeatherParameters(0,6000,false,false);
                var state=FantasyGacha.state(p);var owned=state.getCompound("owned");owned.putBoolean("sakura_sword",true);owned.putBoolean("sakura_pickaxe",true);state.put("owned",owned);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.containerMenu.broadcastChanges();
            });mc.setScreen(null);phase=3;ticks=0;}
            case 3 -> {if(!pending.isDone()||ticks<10)return;pending.join();JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("fantasySkin","sakura_sword"));phase=4;ticks=0;}
            case 4 -> {if(ticks<20)return;require(mc.player.getMainHandItem().hasTag()&&mc.player.getMainHandItem().getTag().getString(FantasySkins.KEY).equals("sakura_sword"),"Sword apply not synced");shot(mc,"sakura-reference-held-sword");server(mc,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));p.containerMenu.broadcastChanges();});phase=5;ticks=0;}
            case 5 -> {if(!pending.isDone()||ticks<10)return;pending.join();JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("fantasySkin","sakura_pickaxe"));phase=6;ticks=0;}
            case 6 -> {if(ticks<20)return;require(mc.player.getMainHandItem().hasTag()&&mc.player.getMainHandItem().getTag().getString(FantasySkins.KEY).equals("sakura_pickaxe"),"Pickaxe apply not synced");shot(mc,"sakura-reference-held-pickaxe");com.mojang.logging.LogUtils.getLogger().info("REFERENCE_CHECK PASS: two actual baked models, new material bindings, both server-authorized skins and client inventory sync, comparison and held-item captures");phase=7;ticks=0;}
            case 7 -> {if(ticks>5)mc.stop();}
        }
    }
    private static final class Comparison extends Screen {
        Comparison(){super(Component.literal("벚꽃 세트 · 레퍼런스 맞춤 샘플"));}
        @Override public void render(GuiGraphics g,int mx,int my,float delta){
            g.fill(0,0,width,height,0xFFF5ECE4);
            g.drawString(font,"벚꽃 세트 · 실제 Minecraft 3D 모델",22,18,0xFF765A69,false);
            g.drawString(font,"2개만 수정한 미리보기 · 일러스트가 아닌 게임 렌더링",22,34,0xFF8F7180,false);
            int panel=(width-54)/2;
            for(int i=0;i<2;i++){
                int x=18+i*(panel+18);JournalButton.rounded(g,x,58,panel,height-100,16,0xFFECCFD7);JournalButton.rounded(g,x+1,59,panel-2,height-102,15,0xFFFFF7F2);
                String id=i==0?"sakura_sword":"sakura_pickaxe";
                float scale=Math.min((panel-30)/10f,(height-160)/12f);
                g.pose().pushPose();g.pose().translate(x+(panel-16*scale)/2,20,0);g.pose().scale(scale,scale,scale);g.renderItem(FantasyGachaScreen.preview(id),0,0);g.pose().popPose();
                g.drawString(font,i==0?"벚꽃 검":"벚꽃 곡괭이",x+16,height-73,0xFF805A6F,false);
            }
            g.drawString(font,"얇은 금테 · 크림색 그립 · 꽃과 진주 · 연분홍 곡선",22,height-24,0xFF805A6F,false);
        }
        @Override public boolean isPauseScreen(){return false;}
    }
}
