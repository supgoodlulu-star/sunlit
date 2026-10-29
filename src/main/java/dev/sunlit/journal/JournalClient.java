package dev.sunlit.journal;
import net.minecraft.client.Minecraft;
/** Loaded only from the physical client network handler. */
public final class JournalClient {
    public static void receive(JournalNetwork.Snapshot msg){
        Minecraft mc=Minecraft.getInstance();
        if(msg.open())mc.setScreen(new JournalScreen(msg.data(),msg.page()));
        else if(mc.screen instanceof JournalScreen screen)screen.update(msg.data());
    }
}
