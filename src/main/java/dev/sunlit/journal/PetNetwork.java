package dev.sunlit.journal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import java.util.*;

public final class PetNetwork {
 private record Session(String mode,BlockPos pos,String dimension,long expires){}
 private static final Map<UUID,Session> SESSIONS=new HashMap<>();
 private static final Map<UUID,Long> LAST=new HashMap<>();
 public record Request(String action,String value){static void encode(Request m,FriendlyByteBuf b){b.writeUtf(m.action,24);b.writeUtf(m.value,80);}static Request decode(FriendlyByteBuf b){return new Request(b.readUtf(24),b.readUtf(80));}}
 public record Snapshot(CompoundTag data,boolean open){static void encode(Snapshot m,FriendlyByteBuf b){b.writeNbt(m.data);b.writeBoolean(m.open);}static Snapshot decode(FriendlyByteBuf b){var d=b.readNbt();return new Snapshot(d==null?new CompoundTag():d,b.readBoolean());}}
 public static void register(){
  JournalNetwork.CHANNEL.messageBuilder(Request.class,2,NetworkDirection.PLAY_TO_SERVER).encoder(Request::encode).decoder(Request::decode).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)handle(p,m);}).add();
  JournalNetwork.CHANNEL.messageBuilder(Snapshot.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::encode).decoder(Snapshot::decode).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->PetScreen.receive(m))).add();
 }
 public static void forget(ServerPlayer p){SESSIONS.remove(p.getUUID());LAST.remove(p.getUUID());}
 public static void clear(){SESSIONS.clear();LAST.clear();}
 public static void openCounter(ServerPlayer p,String mode,BlockPos pos){SESSIONS.put(p.getUUID(),new Session(mode,pos.immutable(),p.level().dimension().location().toString(),p.getServer().getTickCount()+1200L));send(p,mode,true);}
 private static boolean counter(ServerPlayer p,String mode){
  var s=SESSIONS.get(p.getUUID());if(s==null||!s.mode.equals(mode)||s.expires<p.getServer().getTickCount()||!s.dimension.equals(p.level().dimension().location().toString())||p.distanceToSqr(s.pos.getX()+.5,s.pos.getY()+.5,s.pos.getZ()+.5)>64)return false;
  return p.level().getBlockState(s.pos).is(mode.equals("draw")?PetContent.VENDING.get():PetContent.SHOP.get());
 }
 static void handle(ServerPlayer p,Request m){
  long now=p.getServer().getTickCount();Long last=LAST.put(p.getUUID(),now);if(last!=null&&now-last<4)return;
  if(m.action.equals("open")){SESSIONS.remove(p.getUUID());send(p,"vault",true);return;}
  var w=PetWorld.get(p);String mode="vault";
  switch(m.action){
   case "draw"->{if(!counter(p,"draw"))return;w.draw(p);mode="draw";}
   case "buy"->{if(!counter(p,"shop"))return;w.buy(p,m.value);mode="shop";}
   case "recall"->w.recall(p);
   case "summon","withdraw","accessory"->{UUID id;try{id=UUID.fromString(m.value);}catch(IllegalArgumentException ex){return;}switch(m.action){case "summon"->w.summon(p,id);case "withdraw"->w.withdraw(p,id);default->w.accessory(p,id);}}
   default->{return;}
  }
  send(p,mode,false);
 }
 public static void send(ServerPlayer p,String mode,boolean open){JournalNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(PetWorld.get(p).snapshot(p,mode),open));}
}
