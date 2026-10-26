package dev.sunlit.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class JournalInputs {
    private static long lastOpen=Long.MIN_VALUE;
    private static boolean canOpen(Minecraft mc){return mc.player!=null&&mc.level!=null&&mc.getConnection()!=null&&mc.screen==null&&!mc.player.isSpectator();}
    private static void open(Minecraft mc){
        long now=mc.level.getGameTime();
        if(lastOpen!=Long.MIN_VALUE&&now>=lastOpen&&now-lastOpen<6)return;
        lastOpen=now;
        JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request("open","profile"));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)lastOpen=Long.MIN_VALUE;
        while(JournalKeys.OPEN_PROFILE.consumeClick())if(canOpen(mc))open(mc);
    }
    @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered event){
        Minecraft mc=Minecraft.getInstance();
        if(!event.isUseItem()||event.getHand()!=InteractionHand.MAIN_HAND||!canOpen(mc)||!JournalClientConfig.SHIFT_AIR_OPEN.get())return;
        if(!mc.player.isShiftKeyDown()||mc.hitResult==null||mc.hitResult.getType()!=HitResult.Type.MISS)return;
        event.setCanceled(true);event.setSwingHand(false);open(mc);
    }
}
