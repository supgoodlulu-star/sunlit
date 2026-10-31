package dev.sunlit.journal.api;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;
/** Additive, non-cancellable notification. Existing Journal payouts are already committed. */
public final class JournalRewardEvent extends Event{
    private final ServerPlayer player;private final String kind,instanceId;
    public JournalRewardEvent(ServerPlayer player,String kind,String instanceId){this.player=player;this.kind=kind;this.instanceId=instanceId;}
    public ServerPlayer getPlayer(){return player;}
    public String getKind(){return kind;}
    public String getInstanceId(){return instanceId;}
    public static void publish(ServerPlayer p,String kind,String id){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new JournalRewardEvent(p,kind,id));}
}
