package dev.sunlit.reforged;
import net.minecraft.client.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.CompletableFuture;
/** QA only. Uses the actual Stardew Fishing event, network packet and native FishingScreen. */
@Mod.EventBusSubscriber(modid=SunlitReforged.ID,value=Dist.CLIENT)
public final class FishingBarPreview {
    static int stage,ticks;static boolean boosted,oldHud;static volatile int baseSize,actualSize;static CompletableFuture<Void> pending;static long start=System.currentTimeMillis();
    static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("FISHING_BAR_PREVIEW: "+text);}
    static Object get(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void set(Object o,String name,Object value)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static boolean nativeScreen(){var s=Minecraft.getInstance().screen;return s!=null&&s.getClass().getName().equals("com.bonker.stardewfishing.client.FishingScreen");}
    static void open(boolean plus){
        var mc=Minecraft.getInstance();pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(60000,0,false,false);
            if(p.fishing!=null)p.fishing.discard();var hook=new FishingHook(p,p.serverLevel(),0,0);hook.setPos(p.getX()+1,p.getY(),p.getZ()+1);p.serverLevel().addFreshEntity(hook);
            var behavior=Class.forName("com.bonker.stardewfishing.common.FishBehavior");var fish=behavior.getConstructor(int.class,float.class,float.class,float.class,int.class,int.class).newInstance(10,1.5f,.7f,.7f,55,10);
            var type=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameStartedEvent");var event=type.getConstructor(ServerPlayer.class,FishingHook.class,ItemStack.class,ItemStack.class,behavior,boolean.class).newInstance(p,hook,p.getMainHandItem(),new ItemStack(Items.COD),fish,false);
            baseSize=((Number)type.getMethod("getBarSize").invoke(event)).intValue();if(plus)type.getMethod("setBarSize",int.class).invoke(event,(int)Math.round(baseSize*1.4));actualSize=((Number)type.getMethod("getBarSize").invoke(event)).intValue();
            var packet=Class.forName("com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket").getConstructor(type,boolean.class,boolean.class).newInstance(event,false,false);
            Class.forName("com.bonker.stardewfishing.common.networking.SFNetworking").getMethod("sendToPlayer",ServerPlayer.class,Object.class).invoke(null,p,packet);
            boosted=plus;System.out.println("FISHING_BAR_SERVER base="+baseSize+" actual="+actualSize+" requested="+(plus?40:0)+"%");pending.complete(null);
        }catch(Throwable t){pending.completeExceptionally(t);}});
    }
    static void fixedPose()throws Exception{
        if(!nativeScreen())return;var s=Minecraft.getInstance().screen;var minigame=get(s,"minigame");
        var status=Class.forName("com.bonker.stardewfishing.client.FishingScreen$Status");set(s,"status",status.getField("MINIGAME").get(null));
        set(minigame,"points",60f);set(minigame,"bobberPos",55d);set(minigame,"fishPos",75d);set(minigame,"bobberVelocity",0d);set(minigame,"fishVelocity",0d);
        for(String name:new String[]{"bobberPos","fishPos","bobberAlpha","progressBar"}){var a=get(s,name);a.getClass().getMethod("freeze",float.class).invoke(a,switch(name){case "bobberPos"->55f;case "fishPos"->75f;case "bobberAlpha"->1f;default->.5f;});}
    }
    @SubscribeEvent public static void pre(net.minecraftforge.client.event.ScreenEvent.Render.Pre e)throws Exception{if(Boolean.getBoolean("sunlit.barPreview")){Minecraft.getInstance().getToasts().clear();fixedPose();}}
    @SubscribeEvent public static void caption(net.minecraftforge.client.event.ScreenEvent.Render.Post e){
        if(!Boolean.getBoolean("sunlit.barPreview")||!nativeScreen())return;var mc=Minecraft.getInstance();var g=e.getGuiGraphics();
        String label=boosted?"막대 크기 +40% · "+baseSize+" → "+actualSize+" px":"기본 막대 · "+actualSize+" px";
        g.fill(mc.screen.width/2-150,10,mc.screen.width/2+150,43,0xdd39291e);g.drawCenteredString(mc.font,label,mc.screen.width/2,17,0xffffe2ab);
        g.drawCenteredString(mc.font,"실제 Stardew Fishing 3.7 · 비교용 위치 고정",mc.screen.width/2,30,0xffffefcd);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(!Boolean.getBoolean("sunlit.barPreview"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<240000,"Timeout stage "+stage);if(mc.player==null||mc.getOverlay()!=null)return;fixedPose();if(e.phase!=TickEvent.Phase.END)return;
        if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
        if(++ticks<30)return;ticks=0;
        switch(stage++){
            case 0->{oldHud=mc.options.hideGui;mc.options.hideGui=true;mc.options.guiScale().set(3);mc.resizeDisplay();mc.setScreen(null);open(false);}
            case 1->{require(nativeScreen(),"Native minigame was not received");int value=((Number)get(mc.screen,"minigame").getClass().getMethod("getBarSize").invoke(get(mc.screen,"minigame"))).intValue();require(value==baseSize,"Baseline packet/client differs");shot("fishing-bar-default");open(true);}
            case 2->{require(nativeScreen(),"Native boosted minigame missing");int value=((Number)get(mc.screen,"minigame").getClass().getMethod("getBarSize").invoke(get(mc.screen,"minigame"))).intValue();require(value==actualSize&&actualSize==Math.round(baseSize*1.4),"Boosted packet/client size differs");shot("fishing-bar-plus40");}
            case 3->{mc.options.hideGui=oldHud;System.out.println("FISHING_BAR_PREVIEW_PASSED base="+baseSize+" boosted="+actualSize+" native server event -> packet -> FishingScreen; positions fixed for comparison only");mc.stop();}
        }
    }
    static void shot(String name){var mc=Minecraft.getInstance();var out=new java.io.File(System.getProperty("sunlit.previewOutput"));new java.io.File(out,"screenshots").mkdirs();mc.getToasts().clear();Screenshot.grab(out,name+".png",mc.getMainRenderTarget(),c->{});}
}
