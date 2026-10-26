package dev.sunlit.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.concurrent.CompletableFuture;

/** Opt-in real-map acceptance check in a disposable copy of the GameTest world. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class SkyGardenCheck {
    private static int phase,ticks,readyTick;private static long start;
    private static CompletableFuture<Void> pending;
    private static void require(boolean yes,String reason){if(!yes)throw new IllegalStateException("SKY_GARDEN_CHECK: "+reason);}
    private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){
        pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable e){pending.completeExceptionally(e);}});
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.skyCheck"))return;
        if(start==0)start=System.currentTimeMillis();require(System.currentTimeMillis()-start<240000,"Timeout at "+phase);
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;ticks++;
        switch(phase){
            case 0 -> {if(ticks<30)return;mc.setScreen(null);mc.options.guiScale().set(2);mc.resizeDisplay();mc.getToasts().clear();server(mc,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SkyContent.COMPASS.get()));SkyGarden.player(p).remove("complete");SkyGarden.player(p).putInt("records",0);SkyGarden.player(p).putInt("step",0);p.containerMenu.broadcastChanges();});phase=1;}
            case 1 -> {if(!pending.isDone())return;pending.join();if(!mc.player.getMainHandItem().is(SkyContent.COMPASS.get()))return;mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=2;ticks=0;}
            case 2 -> {if(!mc.level.dimension().equals(SkyGarden.DIMENSION))return;if(ticks<30)return;server(mc,p->{require(SkyGarden.player(p).getBoolean("discovered"),"Real compass did not discover map");p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),48,180,55,138,25);});phase=3;ticks=0;}
            case 3 -> {if(!pending.isDone())return;pending.join();for(var pos:SkyGarden.RECORDS)if(!mc.level.hasChunkAt(pos))return;if(readyTick==0)readyTick=ticks;if(ticks-readyTick<140)return;mc.getToasts().clear();mc.gui.getChat().clearMessages(true);shot(mc,"sky-overview");server(mc,p->{p.teleportTo(p.serverLevel(),1,154,-6,180,15);});phase=4;ticks=0;}
            case 4 -> {if(!pending.isDone())return;pending.join();if(ticks<25)return;shot(mc,"sky-observatory");server(mc,p->{
                for(int i=0;i<3;i++){var pos=SkyGarden.RECORDS[i];p.teleportTo(p.serverLevel(),pos.getX()+.5,pos.getY()+1,pos.getZ()+2.5,0,0);SkyGarden.interact(p,pos,i);}
                for(int i=0;i<3;i++){var pos=SkyGarden.RUNES[i];p.teleportTo(p.serverLevel(),pos.getX()+.5,pos.getY()+1,pos.getZ()+2.5,0,0);SkyGarden.interact(p,pos,i+3);}
                require(SkyGarden.player(p).getBoolean("complete"),"Puzzle did not unlock personal reward");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.containerMenu.broadcastChanges();
            });phase=5;ticks=0;}
            case 5 -> {if(!pending.isDone())return;pending.join();if(!mc.player.getMainHandItem().is(Items.DIAMOND_SWORD))return;JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("skin","sword"));phase=6;ticks=0;}
            case 6 -> {if(ticks<20)return;var stack=mc.player.getMainHandItem();require(stack.hasTag()&&"sword".equals(stack.getTag().getString("SunlitSkySkin")),"Server skin not synced");
                var property=ItemProperties.getProperty(stack.getItem(),new ResourceLocation(SunlitJournal.MODID,"sky_skin"));require(property!=null&&property.call(stack,mc.level,mc.player,0)==1,"Skin predicate not registered");
                var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);require(model.getParticleIcon().contents().name().toString().equals("sunlit_journal_core:item/starlight_sword"),"Actual sword model did not resolve");
                mc.player.setXRot(20);shot(mc,"sky-sword-skin");server(mc,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));p.containerMenu.broadcastChanges();});phase=7;ticks=0;}
            case 7 -> {if(!pending.isDone())return;pending.join();if(!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return;JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("skin","pickaxe"));phase=8;ticks=0;}
            case 8 -> {if(ticks<20)return;var stack=mc.player.getMainHandItem();var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);require(model.getParticleIcon().contents().name().toString().equals("sunlit_journal_core:item/cloud_pickaxe"),"Actual pickaxe model did not resolve");shot(mc,"sky-pickaxe-skin");JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("open","secrets"));phase=9;ticks=0;}
            case 9 -> {if(!(mc.screen instanceof JournalScreen)||ticks<20)return;mc.screen.mouseScrolled(mc.screen.width/2,mc.screen.height/2,-60);phase=10;ticks=0;}
            case 10 -> {if(ticks<10)return;shot(mc,"sky-cosmetic-unlocks");mc.screen.onClose();server(mc,p->SkyGarden.leave(p));phase=11;ticks=0;}
            case 11 -> {if(!pending.isDone())return;pending.join();if(mc.level.dimension().equals(SkyGarden.DIMENSION))return;com.mojang.logging.LogUtils.getLogger().info("SKY_GARDEN_CHECK PASS: compass item use path, real map dimension/build/teleport, discovery, personal puzzle, both server-synced cosmetic models, journal and return");phase=12;ticks=0;}
            case 12 -> {if(ticks>=10)mc.stop();}
        }
    }
}
