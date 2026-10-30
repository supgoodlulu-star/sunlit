package dev.sunlit.journal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class HallEvents {
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)HallWorld.get(p.getServer()).observe(p);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END){var s=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(s!=null){HallWorld.get(s).season();if(s.getTickCount()%20==0)for(var p:s.getPlayerList().getPlayers()){HallWorld.get(s).observe(p);HallNetwork.tick(p);}}}}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){HallWorld.get(p.getServer()).observe(p);HallNetwork.forget(p);}}
    @SubscribeEvent public void stop(ServerStoppedEvent e){HallNetwork.WATCH.clear();HallNetwork.LAST.clear();}
}
