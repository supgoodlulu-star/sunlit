package dev.sunlit.journal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.*;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class PetReconnectCheck {
 static int ticks;static boolean done,stopping;static CompletableFuture<Void> pending;static long start=System.currentTimeMillis();
 static void require(boolean b,String s){if(!b)throw new IllegalStateException("PET_RECONNECT: "+s);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(stopping||e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.petReconnectCheck"))return;var mc=Minecraft.getInstance();
  if(System.currentTimeMillis()-start>180000)throw new IllegalStateException("Reconnect check timed out");
  if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals("진행")){b.onPress();return;}
  if(mc.player==null||mc.getSingleplayerServer()==null)return;
  if(pending!=null){if(!pending.isDone())return;pending.join();stopping=true;System.out.println("PET_RECONNECT_CHECK_PASSED phase="+System.getProperty("sunlit.reconnectPhase"));mc.stop();return;}
  if(done||++ticks<70)return;done=true;pending=new CompletableFuture<>();var id=mc.player.getUUID();
  mc.getSingleplayerServer().execute(()->{try{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var w=PetWorld.get(p);var d=w.player(id);int phase=Integer.getInteger("sunlit.reconnectPhase",0);
   if(phase==0){var doll=w.mint("winter_fairy");var pet=doll.getTag().getUUID("pet");require(w.register(p,doll),"prepare registration");require(w.summon(p,pet),"prepare summon");d.putUUID("reconnectTest",pet);w.setDirty();}
   else if(phase==1){require(d.hasUUID("reconnectTest"),"test ID not saved");var pet=d.getUUID("reconnectTest");require(d.hasUUID("active")&&d.getUUID("active").equals(pet),"desired pet lost on logout");var list=p.serverLevel().getEntities(PetContent.PET.get(),v->id.equals(v.owner()));require(list.size()==1&&list.get(0).individual().equals(pet),"auto summon after reconnect");w.recall(p);}
   else {require(!d.hasUUID("active"),"manual recall was not saved");require(p.serverLevel().getEntities(PetContent.PET.get(),v->id.equals(v.owner())).isEmpty(),"manual recall resummoned after reconnect");d.remove("reconnectTest");w.setDirty();}
   pending.complete(null);
  }catch(Throwable ex){pending.completeExceptionally(ex);}});
 }
}
