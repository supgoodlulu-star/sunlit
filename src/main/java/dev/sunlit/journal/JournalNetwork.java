package dev.sunlit.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

public final class JournalNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(SunlitJournal.MODID,"journal"),()->"journal-10", "journal-10"::equals,"journal-10"::equals);
    private static final Map<UUID,Long> WATCHING=new HashMap<>(), LAST=new HashMap<>(), HEARTBEATS=new HashMap<>();
    private static final Set<String> PAGES=Set.of("profile","skills","quests","achievements","titles");
    public record Request(String action,String value){
        public static void encode(Request m,FriendlyByteBuf b){b.writeUtf(m.action,32);b.writeUtf(m.value,256);}
        public static Request decode(FriendlyByteBuf b){return new Request(b.readUtf(32),b.readUtf(256));}
    }
    public record Snapshot(CompoundTag data,boolean open,String page){
        public static void encode(Snapshot m,FriendlyByteBuf b){b.writeNbt(m.data);b.writeBoolean(m.open);b.writeUtf(m.page,32);}
        public static Snapshot decode(FriendlyByteBuf b){CompoundTag n=b.readNbt();return new Snapshot(n==null?new CompoundTag():n,b.readBoolean(),b.readUtf(32));}
    }
    public static void register(){
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Request::encode).decoder(Request::decode)
            .consumerMainThread((m,ctx)->{
                ServerPlayer p=ctx.get().getSender();if(p==null)return;
                if(m.action.equals("close")){WATCHING.remove(p.getUUID());return;}
                long now=p.getServer().getTickCount();
                if(m.action.equals("refresh")){
                    Long heartbeat=HEARTBEATS.get(p.getUUID());if(heartbeat!=null&&now-heartbeat<20)return;
                    HEARTBEATS.put(p.getUUID(),now);
                    if(WATCHING.containsKey(p.getUUID())){WATCHING.put(p.getUUID(),now);send(p,false,"");}
                    return;
                }
                Long previous=LAST.get(p.getUUID());if(previous!=null&&now-previous<4)return;
                LAST.put(p.getUUID(),now);
                try{
                    var w=JournalWorld.get(p.getServer());
                    switch(m.action){
                        case "open" -> {if(PAGES.contains(m.value)){send(p,true,m.value);return;}}
                        case "nick" -> Integrations.nick(p,m.value,false);
                        case "resetNick" -> Integrations.nick(p,"",true);
                        case "equip" -> {
                            if(SunlitJournal.owned(p).contains(m.value)){
                                var d=SunlitJournal.data(p);
                                if(d.getString("equipped").equals(m.value))d.remove("equipped");else d.putString("equipped",m.value);
                            }
                        }
                        case "claim" -> {
                            String[] args=m.value.split(":",-1);
                            if(args.length==2)w.claim(p,args[0],Integer.parseInt(args[1]));
                        }
                        case "skillsTree" -> {
                            var c=Class.forName("net.puffish.skillsmod.api.SkillsAPI");
                            Integrations.call(c,"openScreen",new Class<?>[]{ServerPlayer.class},p);return;
                        }
                        default -> {return;}
                    }
                    send(p,false,"");
                }catch(ReflectiveOperationException|RuntimeException|LinkageError e){
                    Integrations.failure("request:"+m.action,e);
                    p.sendSystemMessage(Component.literal("[모험가의 수첩] 요청을 처리하지 못했습니다. 입력 내용과 연동 모드를 확인해 주세요."));
                }
            }).add();
        CHANNEL.messageBuilder(Snapshot.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::encode).decoder(Snapshot::decode)
            .consumerMainThread((m,ctx)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->JournalClient.receive(m))).add();
    }
    public static boolean isWatching(ServerPlayer p){return WATCHING.containsKey(p.getUUID())&&p.getServer().getTickCount()-WATCHING.get(p.getUUID())<1200;}
    public static void forget(ServerPlayer p){WATCHING.remove(p.getUUID());LAST.remove(p.getUUID());HEARTBEATS.remove(p.getUUID());}
    public static void send(ServerPlayer p,boolean open,String page){
        if(open)WATCHING.put(p.getUUID(),(long)p.getServer().getTickCount());
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(JournalWorld.get(p.getServer()).snapshot(p),open,page));
    }
}
