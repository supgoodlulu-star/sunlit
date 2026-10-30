package dev.sunlit.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.Optional;

/** Decorates equipped titles and item shares; ordinary chat keeps its existing handler. */
public final class JournalChat {
    static Optional<String> equipped(ServerPlayer player){
        String title=SunlitJournal.data(player).getString("equipped");
        return TitleThemes.known(title)&&SunlitJournal.owned(player).contains(title)?Optional.of(title):Optional.empty();
    }
    static Component nickname(ServerPlayer player){
        try{
            String nick=Integrations.nick(player);
            if(!nick.isBlank()){
                Object formatted=Integrations.call(Class.forName("com.ggserver.servernick.NickFormat"),"toComponent",new Class<?>[]{String.class},nick);
                if(formatted instanceof Component component)return component.copy();
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){Integrations.failure("chatNickname",e);}
        return player.getDisplayName().copy();
    }
    static Optional<Component> format(ServerPlayer player,Component message){
        return equipped(player).map(title->Component.empty().append(TitleIcons.icon(title)).append(" ").append(TitleThemes.badge(title)).append(" ")
            .append(nickname(player)).append(Component.literal(": ").withStyle(s->s.withColor(0xFFFFFF))).append(message.copy()));
    }
    // Server Nick 1.2.0 cancels and broadcasts at NORMAL. Run before it so a titled or item-sharing
    // message is sent once. Already-cancelled events are deliberately not received.
    @SubscribeEvent(priority=EventPriority.HIGH)
    public void chat(ServerChatEvent event){
        var server=event.getPlayer().getServer();
        if(server==null||server.isStopped())return;
        // Forge can fire this event from the asynchronous chat-decorator worker.
        // Read player NBT and publish on the server thread, before dispatch continues.
        if(server.isSameThread())publish(event);
        else server.submit(()->publish(event)).join();
    }
    private void publish(ServerChatEvent event){
        var player=event.getPlayer();Component body=event.getMessage();
        boolean itemShare=ItemShowcase.containsToken(body);
        if(itemShare){
            if(player.getMainHandItem().isEmpty()){
                event.setCanceled(true);player.sendSystemMessage(ItemShowcase.emptyHand());return;
            }
            body=ItemShowcase.expand(body,player.getMainHandItem()).orElse(body);
        }
        Component message=format(player,body).orElse(null);
        if(message==null&&itemShare)message=Component.empty().append(nickname(player)).append(": ").append(body);
        if(message!=null){
            event.setCanceled(true);
            player.getServer().getPlayerList().broadcastSystemMessage(message,false);
        }
    }
}
