package dev.sunlit.reforged;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
public final class Network{
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(SunlitReforged.ID,"station"),()->"1","1"::equals,"1"::equals);
    public record Action(int menu,int revision,String action,int product,int quantity,String rollId){}
    public record View(CompoundTag state){}
    public static void register(){
        CHANNEL.messageBuilder(Action.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeInt(m.menu);b.writeInt(m.revision);b.writeUtf(m.action,16);b.writeInt(m.product);b.writeInt(m.quantity);b.writeUtf(m.rollId,64);}).decoder(b->new Action(b.readInt(),b.readInt(),b.readUtf(16),b.readInt(),b.readInt(),b.readUtf(64))).consumerMainThread((m,c)->{
            var p=c.get().getSender();if(p==null||!(p.containerMenu instanceof StationMenu menu)||menu.containerId!=m.menu||menu.revision()!=m.revision)return;
            boolean ok=menu.act(m.revision,m.action,m.product,m.quantity,m.rollId);
            if(!ok)p.sendSystemMessage(Component.literal("[선릿 재련] 처리하지 못했습니다. 장비·꿈방울·계좌·빈 공간을 확인해 주세요."));send(p);
        }).add();
        CHANNEL.messageBuilder(View.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeNbt(m.state)).decoder(b->new View(Objects.requireNonNullElseGet(b.readNbt(),CompoundTag::new))).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Client.receive(m.state))).add();
    }
    public static void send(ServerPlayer p){if(p.containerMenu instanceof StationMenu m)CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(m.snapshot()));}
}
