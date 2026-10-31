package dev.sunlit.reforged;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.nbt.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.util.*;
import java.util.concurrent.*;

@Mod.EventBusSubscriber(modid=SunlitReforged.ID,value=Dist.CLIENT)
public final class ReforgedClientCheck{
    static int phase,ticks;static long start=System.currentTimeMillis();static CompletableFuture<Void> pending;static double oldChance;static double hoverX=-1,hoverY;
    static final List<Component> messages=new ArrayList<>();
    interface Work{void run(net.minecraft.server.level.ServerPlayer p)throws Exception;}
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("REFORGED_CLIENT: "+why);}
    static void server(Minecraft mc,Work work){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
    static void click(Minecraft mc,String name){for(var child:mc.screen.children())if(child instanceof Button b&&(b.getMessage().getString().equals(name)||(name.equals("수상한 큐브")&&b.getMessage().getString().startsWith("수상한 ")))){require(b.active,"Inactive button "+name);b.onPress();return;}throw new IllegalStateException("No button "+name);}
    static void shot(Minecraft mc,String name){mc.getToasts().clear();Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput")),name+".png",mc.getMainRenderTarget(),c->{});}
    static StationMenu menu(Minecraft mc){require(mc.player.containerMenu instanceof StationMenu,"Not a station menu phase "+phase);return (StationMenu)mc.player.containerMenu;}
    static List<Potential.Line> original(){return List.of(new Potential.Line(Potential.Option.ATTACK_PERCENT,Potential.Tier.UNIQUE,6),new Potential.Line(Potential.Option.ATTACK_PERCENT,Potential.Tier.UNIQUE,6),new Potential.Line(Potential.Option.LIFESTEAL,Potential.Tier.UNIQUE,3));}
    @SubscribeEvent public static void message(ClientChatReceivedEvent e){if(Boolean.getBoolean("sunlit.reforgedCheck"))messages.add(e.getMessage().copy());}
    static void pin(Minecraft mc){if(hoverX<0||!(mc.screen instanceof ChatScreen))return;try{for(String n:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(n);f.setAccessible(true);f.setDouble(mc.mouseHandler,n.equals("xpos")?hoverX:hoverY);}}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
    static void hover(Minecraft mc){for(int y=mc.getWindow().getGuiScaledHeight()-43;y>20;y--)for(int x=1;x<Math.min(430,mc.getWindow().getGuiScaledWidth());x++){var style=mc.gui.getChat().getClickedComponentStyleAt(x,y);if(style!=null&&style.getHoverEvent()!=null&&style.getHoverEvent().getAction()==HoverEvent.Action.SHOW_ITEM){hoverX=x*(double)mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth();hoverY=y*(double)mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight();pin(mc);return;}}throw new IllegalStateException("No chat hover target");}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.reforgedCheck"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<360000,"Timeout phase "+phase);
        if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof Button b&&(b.getMessage().getString().equals("진행")||b.getMessage().getString().equals("Proceed"))){b.onPress();return;}
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;pin(mc);
        if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
        if(++ticks<40)return;ticks=0;
        switch(phase++){
            case 0->{mc.options.guiScale().set(2);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.reloadResourcePacks();mc.resizeDisplay();mc.setScreen(null);server(mc,p->{
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setHoverName(Component.literal("햇살의 검"));sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);sword.getOrCreateTag().putString("ForeignPrefixProbe","unchanged");new Potential(Potential.Tier.UNIQUE,original()).apply(sword);
                p.getInventory().setItem(0,sword);p.getInventory().setItem(1,new ItemStack(SunlitReforged.MASTER.get(),3));p.getInventory().setItem(2,new ItemStack(SunlitReforged.SUSPICIOUS.get(),3));p.getInventory().setItem(3,new ItemStack(SunlitReforged.ARTISAN.get(),3));p.getInventory().selected=0;
                var bank=Class.forName("dev.ithundxr.createnumismatics.Numismatics").getField("BANK").get(null);var type=Class.forName("dev.ithundxr.createnumismatics.content.backend.BankAccount$Type");var account=bank.getClass().getMethod("getOrCreateAccount",UUID.class,type).invoke(bank,p.getUUID(),type.getField("PLAYER").get(null));account.getClass().getMethod("setBalance",int.class).invoke(account,20000*4096);
                var station=Ledger.get(p.getServer()).station(p.getUUID());station.remove("pending");station.remove("equipment");station.remove("cubes");Ledger.get(p.getServer()).setDirty();oldChance=Rules.get().upgrades[2][2];Rules.get().upgrades[2][2]=100;p.serverLevel().setDayTime(6000);
            });}
            case 1->mc.player.connection.sendCommand("재련");
            case 2->{var m=menu(mc);mc.gameMode.handleInventoryMouseClick(m.containerId,29,0,ClickType.QUICK_MOVE,mc.player);mc.gameMode.handleInventoryMouseClick(m.containerId,30,0,ClickType.QUICK_MOVE,mc.player);}
            case 3->{require(Potential.get(menu(mc).work.getItem(0)).orElseThrow().tier()==Potential.Tier.UNIQUE,"Equipment not moved by click packets");shot(mc,"potential-current-unique");click(mc,"큐브 사용");}
            case 4->{var m=menu(mc);require(m.pending(),"Master did not enter comparison");require(Potential.get(m.work.getItem(0)).orElseThrow().tier()==Potential.Tier.LEGENDARY,"Promotion not committed");shot(mc,"master-comparison");mc.gameMode.handleInventoryMouseClick(m.containerId,0,0,ClickType.SWAP,mc.player);}
            case 5->{require(menu(mc).pending()&&!menu(mc).work.getItem(0).isEmpty(),"Locked equipment moved");mc.screen.onClose();}
            case 6->mc.player.connection.sendCommand("재련");
            case 7->{require(menu(mc).pending(),"Pending comparison lost on reopen");shot(mc,"pending-restored");click(mc,"기존 줄 유지");}
            case 8->{var m=menu(mc);require(!m.pending(),"Choice not resolved");var result=Potential.get(m.work.getItem(0)).orElseThrow();require(result.tier()==Potential.Tier.LEGENDARY&&result.lines().equals(original()),"Old lines or promoted tier not retained");shot(mc,"legendary-with-old-unique-lines");mc.screen.onClose();server(mc,p->Rules.get().upgrades[2][2]=oldChance);}
            case 9->{server(mc,p->{require(p.getInventory().countItem(Items.DIAMOND_SWORD)==1,"Gear duplicated/lost");require(p.getInventory().countItem(SunlitReforged.MASTER.get())==2,"Cube count wrong");require(p.getMainHandItem().is(Items.DIAMOND_SWORD),"Restored equipment not in selected slot");});}
            case 10->{messages.clear();mc.gui.getChat().clearMessages(true);mc.player.connection.sendCommand("자랑하기");mc.setScreen(new ChatScreen(""));}
            case 11->{var msg=messages.stream().filter(c->c.getString().contains("아이템을 자랑합니다")).findFirst().orElseThrow();final int[] count={0};msg.visit((style,text)->{var hover=style.getHoverEvent();if(hover!=null&&hover.getAction()==HoverEvent.Action.SHOW_ITEM){var stack=hover.getValue(HoverEvent.Action.SHOW_ITEM).getItemStack();var pot=Potential.get(stack).orElseThrow();require(pot.tier()==Potential.Tier.LEGENDARY&&pot.lines().equals(original()),"Chat hover potential lost");var tooltip=stack.getTooltipLines(mc.player,TooltipFlag.Default.NORMAL).stream().map(Component::getString).toList();require(tooltip.stream().anyMatch(s->s.contains("레전드리"))&&tooltip.stream().filter(s->s.contains("[유니크]")).count()==3,"Client tooltip did not include potentials");count[0]++;}return Optional.empty();},Style.EMPTY);require(count[0]>0,"No actual item hover");hover(mc);}
            case 12->{shot(mc,"journal-item-showcase-hover");hoverX=-1;mc.setScreen(null);mc.player.connection.sendCommand("재련");}
            case 13->{require(menu(mc).work.getItem(0).isEmpty(),"Previous gear not returned");click(mc,"수상한 큐브");click(mc,"수상한 큐브");}
            case 14->{server(mc,p->{require(Economy.view(p).getLong("spurs")==19500L*4096,"Duplicate UI purchase debit");require(p.getInventory().countItem(SunlitReforged.SUSPICIOUS.get())==4,"UI cube delivery wrong");});}
            case 15->{shot(mc,"shop-after-one-purchase");mc.options.guiScale().set(4);mc.resizeDisplay();}
            case 16->{shot(mc,"small-gui-station");mc.screen.onClose();mc.options.guiScale().set(2);mc.resizeDisplay();System.out.println("REFORGED_CLIENT_PASSED: real menu/slot/roll/choice packets, promoted old lines, locked equipment, reopen recovery, real Numismatics purchase/replay, Journal SHOW_ITEM hover and small GUI.");mc.stop();}
        }
    }
}
