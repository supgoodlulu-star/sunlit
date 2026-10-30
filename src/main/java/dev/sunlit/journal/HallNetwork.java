package dev.sunlit.journal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.*;

public final class HallNetwork {
    static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(SunlitJournal.MODID,"hall"),()->"1","1"::equals,"1"::equals);
    record View(String season,String category,int page){}
    record Snapshot(CompoundTag data,boolean open){}
    record Watch(View view,int heartbeat,int sent){}
    static final Map<UUID,Watch> WATCH=new HashMap<>();
    static final Map<UUID,Integer> LAST=new HashMap<>();
    public static void register(){
        CHANNEL.messageBuilder(View.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUtf(m.season,16);b.writeUtf(m.category,256);b.writeInt(m.page);}).decoder(b->new View(b.readUtf(16),b.readUtf(256),b.readInt())).consumerMainThread((m,c)->{
            var p=c.get().getSender();if(p==null)return;
            if(m.category.equals("close")){WATCH.remove(p.getUUID());return;}
            int now=p.getServer().getTickCount();Integer last=LAST.get(p.getUUID());if(last!=null&&now-last<5)return;LAST.put(p.getUUID(),now);
            WATCH.put(p.getUUID(),new Watch(m,now,now));send(p,m,true);
        }).add();
        CHANNEL.messageBuilder(Snapshot.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeNbt(m.data);b.writeBoolean(m.open);}).decoder(b->new Snapshot(Objects.requireNonNullElseGet(b.readNbt(),CompoundTag::new),b.readBoolean())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->HallScreen.receive(m.data,m.open))).add();
    }
    public static void request(String season,String category,int page){CHANNEL.sendToServer(new View(season,category,page));}
    static void send(ServerPlayer p,View v,boolean open){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(HallWorld.get(p.getServer()).snapshot(v.season,v.category,v.page),open));}
    public static void open(ServerPlayer p,String category){var v=new View("",category,0);int now=p.getServer().getTickCount();WATCH.put(p.getUUID(),new Watch(v,now,now));send(p,v,true);}
    static void tick(ServerPlayer p){var w=WATCH.get(p.getUUID());if(w==null)return;int now=p.getServer().getTickCount();if(now-w.heartbeat>1200){WATCH.remove(p.getUUID());return;}if(now-w.sent>=40){send(p,w.view,false);WATCH.put(p.getUUID(),new Watch(w.view,w.heartbeat,now));}}
    static void forget(ServerPlayer p){WATCH.remove(p.getUUID());LAST.remove(p.getUUID());}
}
