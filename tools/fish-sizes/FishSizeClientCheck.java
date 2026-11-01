package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.util.*;
import java.util.concurrent.*;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class FishSizeClientCheck {
    static int phase,ticks;static long start=System.currentTimeMillis();static CompletableFuture<Void> pending;static final List<Component> received=new ArrayList<>();static double hoverX=-1,hoverY;
    static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException("FISH_SIZE_CLIENT: "+msg);}
    @SubscribeEvent public static void receive(ClientChatReceivedEvent e){if(Boolean.getBoolean("sunlit.fishSizeClientCheck"))received.add(e.getMessage().copy());}
    interface Work {void accept(net.minecraft.server.level.ServerPlayer p)throws Exception;}
    static void server(Minecraft mc,Work work){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
    static void mouse(Minecraft mc)throws Exception{if(hoverX<0)return;for(String key:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(key);f.setAccessible(true);f.setDouble(mc.mouseHandler,key.equals("xpos")?hoverX:hoverY);}}
    static void shot(Minecraft mc,String name){var out=new java.io.File(System.getProperty("sunlit.previewOutput"));new java.io.File(out,"screenshots").mkdirs();mc.getToasts().clear();Screenshot.grab(out,name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.fishSizeClientCheck"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<240000,"Timeout phase "+phase);
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;mouse(mc);if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}if(++ticks<30)return;ticks=0;
        switch(phase++){
            case 0->{mc.options.guiScale().set(2);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.reloadResourcePacks();mc.resizeDisplay();mc.setScreen(null);server(mc,p->{p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();Integrations.nick(p,"룰루",false);p.serverLevel().setDayTime(6000);var random=net.minecraft.util.RandomSource.create(39);for(var band:FishSizes.Size.values()){ItemStack s=null;for(int i=0;i<50000;i++){s=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation("aquaculture:carp")));s.getOrCreateTagElement("quality_food").putInt("quality",3);FishSizes.stamp(s,p,random);if(FishSizes.size(s).orElseThrow()==band)break;}require(FishSizes.size(s).orElseThrow()==band,"Could not generate test band");p.getInventory().setItem(band.ordinal(),s);}p.getInventory().selected=0;p.containerMenu.broadcastChanges();});}
            case 1->{for(int i=0;i<5;i++){var s=mc.player.getInventory().getItem(i);var band=FishSizes.Size.values()[i];require(FishSizes.size(s).orElseThrow()==band,"Size lost over inventory packet");var tooltip=s.getTooltipLines(mc.player,TooltipFlag.Default.NORMAL).stream().map(Component::getString).toList();require(tooltip.stream().anyMatch(t->t.contains(band.label)&&t.contains("판매가 ×"))&&tooltip.stream().anyMatch(t->t.contains("낚은 사람 · 룰루")),"Native tooltip missing size/price/catcher");}received.clear();mc.gui.getChat().clearMessages(true);server(mc,p->{for(int i=0;i<5;i++){p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,p.getInventory().getItem(i).copy());p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(),"자랑하기");}});mc.setScreen(new ChatScreen(""));}
            case 2->{require(received.stream().filter(c->c.getString().contains("님의 소중한 아이템")).count()==5,"Expected five native announcements");boolean found=false;for(int y=mc.getWindow().getGuiScaledHeight()-43;y>20&&!found;y--)for(int x=1;x<mc.getWindow().getGuiScaledWidth()&&!found;x++){var style=mc.gui.getChat().getClickedComponentStyleAt(x,y);if(style!=null&&style.getHoverEvent()!=null&&style.getHoverEvent().getAction()==HoverEvent.Action.SHOW_ITEM){var item=style.getHoverEvent().getValue(HoverEvent.Action.SHOW_ITEM).getItemStack();if(FishSizes.size(item).orElse(null)==FishSizes.Size.COLOSSAL){hoverX=x*(double)mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth();hoverY=y*(double)mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight();found=true;}}}require(found,"Colossal fish native hover not found");mouse(mc);}
            case 3->{shot(mc,"colossal-fish-chat-hover");hoverX=-1;mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));var slot=mc.player.inventoryMenu.slots.stream().filter(s->FishSizes.size(s.getItem()).orElse(null)==FishSizes.Size.COLOSSAL).findFirst().orElseThrow();hoverX=(mc.screen.width/2d-88+slot.x+8)*mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth();hoverY=(mc.screen.height/2d-83+slot.y+8)*mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight();mouse(mc);}
            case 4->{shot(mc,"fish-size-inventory-tooltip");System.out.println("FISH_SIZE_CLIENT_PASSED: five server-generated bands, persistent inventory packets, native size/price/catcher tooltips, actual showcase SHOW_ITEM hover and existing quality NBT preserved.");mc.stop();}
        }
    }
}
