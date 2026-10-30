package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;
import java.util.concurrent.*;

/** Opt-in real client/server packet flow on a disposable test world. Never shipped. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class FoxIntegrationCheck {
 static int stage,ticks;static UUID pet;static CompletableFuture<Void> pending;static long start=System.currentTimeMillis();
 static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("FOX_INTEGRATION_CHECK: "+why);}
 interface Work {void run(net.minecraft.server.level.ServerPlayer p)throws Exception;}
 static void server(Minecraft mc,Work work){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
 static void click(Minecraft mc,String text){for(var c:mc.screen.children())if(c instanceof Button b&&b.getMessage().getString().equals(text)){b.onPress();return;}throw new IllegalStateException("Missing button: "+text);}
 static void selectPet(Minecraft mc)throws Exception {
  require(mc.screen instanceof PetScreen,"Vault missing");var f=PetScreen.class.getDeclaredField("data");f.setAccessible(true);var data=(CompoundTag)f.get(mc.screen);var list=data.getList("pets",Tag.TAG_COMPOUND);var sorted=new ArrayList<CompoundTag>();for(var t:list)sorted.add((CompoundTag)t);sorted.sort(Comparator.comparing(t->t.getString("id")));int at=-1;for(int i=0;i<sorted.size();i++)if(sorted.get(i).getString("id").equals(pet.toString()))at=i;require(at>=0,"Individual not in vault");var sf=PetScreen.class.getDeclaredField("selected");sf.setAccessible(true);sf.setInt(mc.screen,at);mc.screen.resize(mc,mc.screen.width,mc.screen.height);
 }
 static void shot(Minecraft mc,String name){mc.getToasts().clear();Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput")),"fox-"+name+".png",mc.getMainRenderTarget(),c->{});}
 static void pad(net.minecraft.server.level.ServerLevel level){for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());for(int y=100;y<106;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.foxIntegrationCheck"))return;var mc=Minecraft.getInstance();
  if(System.currentTimeMillis()-start>240000)throw new IllegalStateException("Pet integration check timed out stage "+stage);
  if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals("진행")){b.onPress();return;}
  if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
  if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
  if(++ticks<35)return;ticks=0;
  switch(stage++){
   case 0->{mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.options.guiScale().set(2);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.reloadResourcePacks();mc.resizeDisplay();server(mc,p->{pad(p.serverLevel());p.teleportTo(p.serverLevel(),0,100,-1,0,20);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();var w=PetWorld.get(p);w.recall(p);PetWorld.child(w.player(p.getUUID()),"unlocks").remove("fox");var stack=w.mint("fox");pet=stack.getTag().getUUID("pet");p.getInventory().setItem(0,stack);p.getInventory().selected=0;p.getInventory().setItem(1,TaskRewards.stack("numismatics:sun",30));p.getInventory().setItem(2,new ItemStack(PetContent.TICKET.get(),1));p.level().setBlockAndUpdate(new BlockPos(2,100,1),PetContent.VENDING.get().defaultBlockState());p.level().setBlockAndUpdate(new BlockPos(3,100,1),PetContent.SHOP.get().defaultBlockState());p.serverLevel().setDayTime(6000);});}
   case 1->{mc.setScreen(null);shot(mc,"pet-doll-held");mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
   case 2->{server(mc,p->{require(PetWorld.get(p).owns(p,pet),"Doll right-click did not register");require(p.getInventory().getItem(0).isEmpty(),"Doll not consumed");});JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("open","profile"));}
   case 3->{require(mc.screen instanceof JournalScreen,"Profile missing");shot(mc,"pet-profile-entry");click(mc,"펫 보관함");}
   case 4->{selectPet(mc);click(mc,"이 친구 소환하기");}
   case 5->{shot(mc,"pet-vault-summoned");server(mc,p->{var found=p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner()));require(found.size()==1,"Not exactly one companion");require(found.get(0).individual().equals(pet),"Wrong individual");});mc.setScreen(null);mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(3.5,100.5,1.5),Direction.NORTH,new BlockPos(3,100,1),false));}
   case 6->{require(mc.screen instanceof PetScreen,"Shop block did not open");var sf=PetScreen.class.getDeclaredField("selected");sf.setAccessible(true);sf.setInt(mc.screen,0);mc.screen.resize(mc,mc.screen.width,mc.screen.height);shot(mc,"pet-shop-preview");click(mc,"구매 · 이리듐 10개");}
   case 7->{server(mc,p->{require(PetWorld.get(p).unlocked(p,"fox"),"Shop buy failed");require(p.getInventory().countItem(TaskRewards.stack("numismatics:sun",1).getItem())==20,"Wrong coin charge");});PetScreen.request("open","");}
   case 8->{selectPet(mc);click(mc,"장식 착용");}
   case 9->{server(mc,p->{var list=p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner()));require(list.size()==1&&list.get(0).accessory(),"Accessory not synced");list.get(0).getNavigation().stop();list.get(0).moveTo(0,100,2,180,0);p.teleportTo(p.serverLevel(),0,100,-1,0,20);});mc.setScreen(null);}
   case 10->{var list=mc.level.getEntitiesOfClass(PetEntity.class,mc.player.getBoundingBox().inflate(8));require(!list.isEmpty(),"Companion not tracked by client");shot(mc,"pet-world-idle");mc.gameMode.interact(mc.player,list.get(0),InteractionHand.MAIN_HAND);ticks=25;}
   case 11->{var list=mc.level.getEntitiesOfClass(PetEntity.class,mc.player.getBoundingBox().inflate(8));require(list.stream().anyMatch(PetEntity::petting),"Right-click did not pet/smile");shot(mc,"pet-world-petting");PetScreen.request("open","");}
   case 12->{selectPet(mc);click(mc,"인형으로 꺼내기");}
   case 13->{require(mc.screen instanceof ConfirmScreen,"Withdrawal confirmation missing");click(mc,"예");}
   case 14->{server(mc,p->{require(!PetWorld.get(p).owns(p,pet),"Withdraw did not remove registration");require(p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner())).isEmpty(),"Withdraw left an entity");require(PetWorld.get(p).unlocked(p,"fox"),"Withdraw removed unlock");require(p.getInventory().items.stream().anyMatch(s->s.is(PetContent.DOLL.get())&&s.getTag().getUUID("pet").equals(pet)),"Withdraw lost doll");});shot(mc,"pet-vault-withdrawn");mc.setScreen(null);mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,100.5,1.5),Direction.NORTH,new BlockPos(2,100,1),false));}
   case 15->{require(mc.screen instanceof PetScreen,"Vending block did not open");click(mc,"한 장으로 뽑기");}
   case 16->{shot(mc,"pet-vending-single-draw");server(mc,p->{require(p.getInventory().countItem(PetContent.TICKET.get())==0,"Single draw not exactly one ticket");require(p.getInventory().countItem(PetContent.DOLL.get())==2,"Doll not delivered");var w=PetWorld.get(p);var stack=p.getInventory().items.stream().filter(s->s.is(PetContent.DOLL.get())&&s.getTag().getUUID("pet").equals(pet)).findFirst().orElseThrow();require(w.register(p,stack),"Re-register failed");require(w.summon(p,pet),"Re-summon failed");});mc.setScreen(null);}
   case 17->{mc.setScreen(null);server(mc,p->{var w=PetWorld.get(p);PetEvents.remove(p);PetEvents.ensure(p);require(p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner())).size()==1,"Wanted summon not restored");p.teleportTo(p.serverLevel(),6,100,-1,0,20);});}
   case 18->{server(mc,p->{var list=p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner()));require(list.size()==1&&list.get(0).getX()>1.5,"Fox did not follow moving owner");});shot(mc,"following");}
   case 19->{server(mc,p->{var nether=p.getServer().getLevel(Level.NETHER);pad(nether);p.teleportTo(nether,0,100,-1,0,20);});}
   case 20->{server(mc,p->{require(p.level().dimension()==Level.NETHER,"Dimension transition failed");require(p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner())).size()==1,"Dimension transition lost pet");p.teleportTo(p.getServer().overworld(),0,100,-1,0,20);});}
   case 21->{server(mc,p->{var w=PetWorld.get(p);w.recall(p);PetEvents.ensure(p);require(p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner())).isEmpty(),"Manual recall auto-resummoned");System.out.println("FOX_INTEGRATION_CHECK_PASSED: real doll packets, vault, one fox, accessory price and sync, petting, withdrawal, single draw, following, dimension, manual recall");});PetScreen.request("open","");}
   case 22->{org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,720);mc.options.guiScale().set(3);mc.resizeDisplay();}
   case 23->{shot(mc,"pet-vault-small-gui");mc.stop();}
  }
 }
}
