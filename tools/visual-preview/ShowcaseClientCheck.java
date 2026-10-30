package dev.sunlit.journal;

import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.util.*;
import java.util.concurrent.*;

/** Real command/chat packets and vanilla chat hover, in an isolated QA save. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class ShowcaseClientCheck {
 static int phase,ticks;static CompletableFuture<Void> pending;static long start=System.currentTimeMillis();
 static double hoverX=-1,hoverY;
 static final List<Component> received=new ArrayList<>();static ItemStack expected;
 interface Work{void run(net.minecraft.server.level.ServerPlayer p)throws Exception;}
 static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("SHOWCASE_CLIENT: "+why);}
 static void server(Minecraft mc,Work work){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
 @SubscribeEvent public static void receive(ClientChatReceivedEvent e){if(Boolean.getBoolean("sunlit.showcaseCheck"))received.add(e.getMessage().copy());}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void cancel(ServerChatEvent e){if(Boolean.getBoolean("sunlit.showcaseCheck")&&e.getRawText().startsWith("취소된 자랑"))e.setCanceled(true);}
 static Component message(String key){var matches=received.stream().filter(c->c.getString().contains(key)).toList();require(matches.size()==1,"Expected one delivery for "+key+", got "+matches.size());return matches.get(0);}
 static void checkHover(Component message){final int[] found={0};message.visit((s,t)->{var hover=s.getHoverEvent();if(hover!=null&&hover.getAction()==HoverEvent.Action.SHOW_ITEM){require(ItemStack.matches(hover.getValue(HoverEvent.Action.SHOW_ITEM).getItemStack(),expected),"Hover item/NBT mismatch");found[0]++;}return Optional.empty();},Style.EMPTY);require(found[0]>0,"No SHOW_ITEM after network delivery");}
 static void pinMouse(Minecraft mc){
  if(hoverX<0||!(mc.screen instanceof ChatScreen))return;
  try{for(String name:new String[]{"xpos","ypos"}){var f=MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,name.equals("xpos")?hoverX:hoverY);}}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
 }
 static void hover(Minecraft mc){
  for(int y=mc.getWindow().getGuiScaledHeight()-43;y>20;y--)for(int x=1;x<Math.min(430,mc.getWindow().getGuiScaledWidth());x++){
   var style=mc.gui.getChat().getClickedComponentStyleAt(x,y);
   if(style!=null&&style.getHoverEvent()!=null&&style.getHoverEvent().getAction()==HoverEvent.Action.SHOW_ITEM){hoverX=x*(double)mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth();hoverY=y*(double)mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight();org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),hoverX,hoverY);pinMouse(mc);return;}
  }throw new IllegalStateException("No visible chat hover target");
 }
 static void shot(Minecraft mc,String name){mc.getToasts().clear();Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput")),name+".png",mc.getMainRenderTarget(),c->{});}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.showcaseCheck"))return;var mc=Minecraft.getInstance();
  require(System.currentTimeMillis()-start<240000,"Timeout phase "+phase);
  if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals("진행")){b.onPress();return;}
  if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
  pinMouse(mc);
  if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
  if(++ticks<20)return;ticks=0;
  switch(phase++){
   case 0->{mc.options.guiScale().set(2);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.options.advancedItemTooltips=true;mc.reloadResourcePacks();mc.resizeDisplay();mc.setScreen(null);server(mc,p->{p.getInventory().clearContent();var stack=new ItemStack(Items.DIAMOND_PICKAXE);stack.enchant(Enchantments.BLOCK_FORTUNE,3);stack.enchant(Enchantments.UNBREAKING,3);stack.setDamageValue(37);var lore=new ListTag();lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("햇살 가득한 날에 캔 첫 다이아몬드").withStyle(net.minecraft.ChatFormatting.GOLD))));stack.getOrCreateTagElement("display").put("Lore",lore);p.setItemInHand(InteractionHand.MAIN_HAND,stack);expected=stack.copy();SunlitJournal.data(p).remove("equipped");Integrations.nick(p,"룰루",false);p.serverLevel().setDayTime(6000);});}
   case 1->{received.clear();mc.gui.getChat().clearMessages(true);mc.player.connection.sendCommand("자랑하기");mc.setScreen(new ChatScreen(""));}
   case 2->{var c=message("아이템을 자랑합니다!");require(c.getString().startsWith("✿ 룰루님이"),"Nickname not used by command");checkHover(c);hover(mc);}
   case 3->{shot(mc,"command-item-hover");server(mc,p->{var stack=new ItemStack(Items.TORCH,32);p.setItemInHand(InteractionHand.MAIN_HAND,stack);expected=stack.copy();Integrations.nick(p,"예서",false);});}
   case 4->{received.clear();mc.gui.getChat().clearMessages(true);mc.player.connection.sendChat("쨔안~ [아이템]");mc.setScreen(new ChatScreen(""));}
   case 5->{var c=message("쨔안~");require(c.getString().startsWith("예서:"),"Nickname not used by placeholder");require(!c.getString().contains("[아이템]")&&c.getString().contains("32"),"Placeholder/count missing");checkHover(c);hover(mc);}
   case 6->{shot(mc,"chat-placeholder-hover");server(mc,p->{var stack=new ItemStack(Items.DIAMOND_SWORD);stack.setHoverName(Component.literal("예서의 봄빛 검"));stack.enchant(Enchantments.SHARPNESS,5);p.setItemInHand(InteractionHand.MAIN_HAND,stack);expected=stack.copy();var d=SunlitJournal.data(p);d.putString("owned","새싹농부");d.putString("equipped","새싹농부");});}
   case 7->{received.clear();mc.gui.getChat().clearMessages(true);mc.player.connection.sendChat("칭호 확인 [아이템]");mc.setScreen(new ChatScreen(""));}
   case 8->{var c=message("칭호 확인");require(c.getString().startsWith(TitleIcons.glyph("새싹농부")),"Title art lost");checkHover(c);hover(mc);}
   case 9->{shot(mc,"title-and-custom-item-hover");server(mc,p->{p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.EMERALD));});}
   case 10->{received.clear();mc.player.connection.sendCommand("자랑하기");mc.player.connection.sendChat("빈손 확인 [아이템]");}
   case 11->{require(received.stream().filter(c->c.getString().contains("손에 아이템을 든 뒤")).count()==2,"Empty hand feedback missing");require(received.stream().noneMatch(c->c.getString().contains("빈손 확인")),"Empty item share broadcast");mc.player.connection.sendChat("취소된 자랑 [아이템]");mc.player.connection.sendChat("평소 채팅 확인");}
   case 12->{message("평소 채팅 확인");require(received.stream().noneMatch(c->c.getString().contains("취소된 자랑")),"Cancelled chat was republished");System.out.println("SHOWCASE_CLIENT_PASSED: real command and chat packets, one delivery, item/count/NBT hovers, nickname and title art, empty hand, cancellation and normal chat.");mc.stop();}
  }
 }
}
