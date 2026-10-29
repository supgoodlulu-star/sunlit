package dev.sunlit.journal;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.concurrent.CompletableFuture;
import java.util.*;

/** Opt-in real integrated-server check; never compiled into the shipped mod. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class JournalInputChatCheck {
    private static int phase,ticks;
    private static final long START=System.currentTimeMillis();
    private static CompletableFuture<Void> pending;
    private static final Map<String,List<net.minecraft.network.chat.Component>> RECEIVED=new HashMap<>();
    private static final String[] TOKENS={"SUNLIT_CHAT_CHECK_A","SUNLIT_CHAT_CHECK_B","SUNLIT_CHAT_CHECK_C","SUNLIT_CHAT_CHECK_D"};
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("SUNLIT_INPUT_CHAT_CHECK: "+reason);}
    @SubscribeEvent public static void chat(ClientChatReceivedEvent event){
        if(!Boolean.getBoolean("sunlit.inputChatCheck"))return;
        for(String token:TOKENS)if(event.getMessage().getString().contains(token))RECEIVED.computeIfAbsent(token,k->new ArrayList<>()).add(event.getMessage().copy());
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void filter(net.minecraftforge.event.ServerChatEvent event){
        if(Boolean.getBoolean("sunlit.inputChatCheck")&&event.getRawText().equals(TOKENS[3]))event.setCanceled(true);
    }
    private static void server(Minecraft mc,String title){
        pending=new CompletableFuture<>();var id=mc.player.getUUID();
        mc.getSingleplayerServer().execute(()->{
            try{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var d=SunlitJournal.data(player);d.putString("owned","새싹농부|별빛셰프|벚꽃요정");
                if(title.isEmpty())d.remove("equipped");else d.putString("equipped",title);
                Integrations.nick(player,"햇살테스트",false);pending.complete(null);
            }catch(Throwable e){pending.completeExceptionally(e);}
        });
    }
    private static void profile(Minecraft mc)throws ReflectiveOperationException {
        require(mc.screen instanceof JournalScreen,"Server did not open profile");
        var field=JournalScreen.class.getDeclaredField("data");field.setAccessible(true);
        var data=(CompoundTag)field.get(mc.screen);
        require(data.getString("username").equals(mc.player.getGameProfile().getName()),"Profile was not a real server snapshot");
    }
    private static InputEvent.InteractionKeyMappingTriggered use(Minecraft mc){
        var event=new InputEvent.InteractionKeyMappingTriggered(1,mc.options.keyUse,InteractionHand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(event);return event;
    }
    private static void received(String token,String prefix,String title){
        var list=RECEIVED.getOrDefault(token,List.of());
        require(list.size()==1,"Expected one chat delivery for "+token+", got "+list.size());
        var component=list.get(0);require(component.getString().startsWith((title.isEmpty()?"":TitleIcons.glyph(title)+" ")+prefix),"Unexpected chat prefix: "+component.getString());
        if(!title.isEmpty()){
            final boolean[] found={false};
            component.visit((style,text)->{if(text.contains(title)&&style.getColor()!=null&&style.getColor().getValue()==TitleThemes.color(title,true))found[0]=true;return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
            require(found[0],"Title color missing after real network delivery");
            final boolean[] image={false};component.visit((style,text)->{if(text.contains(TitleIcons.glyph(title))&&style.getFont().equals(TitleIcons.FONT)&&style.getColor()!=null&&style.getColor().getValue()==0xFFFFFF)image[0]=true;return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
            require(image[0],"Title image missing after real network delivery");
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event)throws ReflectiveOperationException {
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.inputChatCheck"))return;
        require(System.currentTimeMillis()-START<180000,"Timed out at phase "+phase);
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        ticks++;
        switch(phase){
            case 0 -> {if(ticks<40)return;mc.setScreen(null);server(mc,"새싹농부");phase=1;ticks=0;}
            case 1 -> {
                if(!pending.isDone())return;pending.join();
                mc.getToasts().clear();
                mc.player.input.shiftKeyDown=false;mc.hitResult=BlockHitResult.miss(mc.player.getEyePosition(),Direction.UP,BlockPos.ZERO);
                require(!use(mc).isCanceled(),"Ordinary right click intercepted");
                mc.player.input.shiftKeyDown=true;mc.hitResult=new BlockHitResult(mc.player.position(),Direction.UP,mc.player.blockPosition(),false);
                require(!use(mc).isCanceled(),"Block interaction intercepted");
                mc.hitResult=new EntityHitResult(mc.player);require(!use(mc).isCanceled(),"Entity interaction intercepted");
                mc.hitResult=BlockHitResult.miss(mc.player.getEyePosition(),Direction.UP,BlockPos.ZERO);
                require(use(mc).isCanceled(),"Sneak + air use not intercepted");mc.player.input.shiftKeyDown=false;phase=2;ticks=0;
            }
            case 2 -> {
                if(!(mc.screen instanceof JournalScreen))return;profile(mc);
                if(ticks<15)return;
                Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),"input-profile-themed.png",mc.getMainRenderTarget(),c->{});
                mc.screen.onClose();phase=3;ticks=0;
            }
            case 3 -> {
                if(ticks<8)return;
                require(Arrays.stream(mc.options.keyMappings).anyMatch(k->k==JournalKeys.OPEN_PROFILE),"Open-profile binding not registered");
                var key=InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H);
                JournalKeys.OPEN_PROFILE.setKey(key);KeyMapping.resetMapping();KeyMapping.click(key);phase=4;ticks=0;
            }
            case 4 -> {if(!(mc.screen instanceof JournalScreen))return;profile(mc);mc.player.connection.sendChat(TOKENS[0]);phase=5;ticks=0;}
            case 5 -> {if(ticks<25)return;received(TOKENS[0],"[새싹농부] 햇살테스트: ","새싹농부");server(mc,"별빛셰프");phase=6;}
            case 6 -> {if(!pending.isDone())return;pending.join();mc.player.connection.sendChat(TOKENS[1]);phase=7;ticks=0;}
            case 7 -> {if(ticks<25)return;received(TOKENS[1],"[별빛셰프] 햇살테스트: ","별빛셰프");server(mc,"");phase=8;}
            case 8 -> {if(!pending.isDone())return;pending.join();mc.player.connection.sendChat(TOKENS[2]);phase=9;ticks=0;}
            case 9 -> {
                if(ticks<25)return;received(TOKENS[2],"<햇살테스트>","");
                server(mc,"새싹농부");phase=10;
            }
            case 10 -> {if(!pending.isDone())return;pending.join();mc.player.connection.sendChat(TOKENS[3]);phase=11;ticks=0;}
            case 11 -> {
                if(ticks<25)return;require(RECEIVED.getOrDefault(TOKENS[3],List.of()).isEmpty(),"Pre-cancelled chat was published");
                if(mc.screen!=null)mc.screen.onClose();
                com.mojang.logging.LogUtils.getLogger().info("SUNLIT_INPUT_CHAT_CHECK PASS: air gesture, block/entity exclusions, rebindable key, real profile snapshots, one chat delivery, nickname, title change/unequip, network colors, title images and prior-cancellation handling");
                phase=12;ticks=0;
            }
            case 12 -> {
                if(ticks<10)return;
                Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),"input-chat-themed.png",mc.getMainRenderTarget(),c->{});
                phase=13;ticks=0;
            }
            case 13 -> {if(ticks>=10)mc.stop();
            }
        }
    }
}
