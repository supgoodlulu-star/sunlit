package dev.sunlit.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class AutoFishingClient {
    private static boolean enabled;
    private static UUID playerId,pendingReel;
    private static Object level;
    private static Item rod;
    private static int slot,ticks,nextAction,waitingUntil;
    private static boolean awaitingHook;
    private static AutoFishingStardew minigame;
    private static String status="waiting";
    public static boolean enabled(){return enabled;}
    public static String status(){return status;}
    public static boolean rod(ItemStack stack){return !stack.isEmpty()&&(stack.getItem() instanceof FishingRodItem||stack.canPerformAction(ToolActions.FISHING_ROD_CAST));}
    public static void toggle(){
        var mc=Minecraft.getInstance();if(enabled){stop("manual");return;}
        if(mc.player==null||mc.level==null||mc.gameMode==null||!mc.player.isAlive()||mc.player.isSpectator()||mc.screen!=null&&!AutoFishingStardew.isScreen(mc.screen))return;
        if(!rod(mc.player.getMainHandItem())){notice("need_rod");return;}
        enabled=true;playerId=mc.player.getUUID();level=mc.level;rod=mc.player.getMainHandItem().getItem();slot=mc.player.getInventory().selected;
        awaitingHook=false;pendingReel=null;nextAction=ticks+5;status="waiting";notice("enabled");
    }
    public static void stop(String reason){
        boolean was=enabled;enabled=false;
        if(minigame!=null)try{minigame.release();}catch(ReflectiveOperationException ignored){}
        minigame=null;awaitingHook=false;pendingReel=null;playerId=null;level=null;rod=null;
        if(was)notice(reason);
    }
    private static void notice(String reason){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.translatable("sunlit_journal_core.autofish."+reason).withStyle(s->s.withColor(0xE3BD6B)),true);}
    @SubscribeEvent public static void key(InputEvent.Key e){
        if(e.getAction()==GLFW.GLFW_PRESS&&JournalKeys.AUTO_FISHING.matches(e.getKey(),e.getScanCode()))toggle();
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Pre e){
        if(e.getAction()==GLFW.GLFW_PRESS&&JournalKeys.AUTO_FISHING.matchesMouse(e.getButton())){var mc=Minecraft.getInstance();if(enabled||mc.screen==null||AutoFishingStardew.isScreen(mc.screen)){toggle();e.setCanceled(true);}}
    }
    @SubscribeEvent public static void manualUse(InputEvent.InteractionKeyMappingTriggered e){if(enabled&&e.isUseItem())stop("manual");}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.START)return;ticks++;if(!enabled)return;var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level!=level||!mc.player.getUUID().equals(playerId)||mc.getConnection()==null){stop("disconnected");return;}
        if(!mc.player.isAlive()||mc.player.isSpectator()){stop("unavailable");return;}
        if(mc.player.getInventory().selected!=slot||mc.player.getMainHandItem().getItem()!=rod||!rod(mc.player.getMainHandItem())){stop("rod_changed");return;}
        if(mc.screen!=null&&!AutoFishingStardew.isScreen(mc.screen)){stop("screen");return;}
        try{
            if(AutoFishingStardew.isScreen(mc.screen)){
                if(minigame==null||!minigame.owns(mc.screen))minigame=new AutoFishingStardew(mc.screen);
                status=minigame.tick();awaitingHook=false;return;
            }
            if(minigame!=null){minigame.release();minigame=null;pendingReel=null;nextAction=ticks+10;}
            var hook=mc.player.fishing;
            if(hook!=null&&!hook.isRemoved()){
                awaitingHook=false;status="waiting";
                if(hook.getHookedIn()!=null){stop("hooked_entity");return;}
                if(pendingReel!=null){status="reeling";if(ticks>waitingUntil)stop("timeout");return;}
                if(ticks>=nextAction&&biting(hook)){pendingReel=hook.getUUID();waitingUntil=ticks+100;status="reeling";useRod(mc);}
                return;
            }
            if(pendingReel!=null){pendingReel=null;nextAction=ticks+10;}
            status="casting";
            if(awaitingHook){if(ticks>waitingUntil)stop("timeout");return;}
            if(ticks>=nextAction&&mc.gameMode!=null){
                awaitingHook=true;waitingUntil=ticks+80;useRod(mc);nextAction=ticks+10;
            }
        }catch(ReflectiveOperationException|RuntimeException failed){stop("unsupported");com.mojang.logging.LogUtils.getLogger().warn("Sunlit auto fishing stopped: unsupported fishing API",failed);}
    }
    private static boolean biting(FishingHook hook)throws IllegalAccessException{
        return ObfuscationReflectionHelper.findField(FishingHook.class,"f_37099_").getBoolean(hook);
    }
    private static void useRod(Minecraft mc){var result=mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);if(result.shouldSwing())mc.player.swing(InteractionHand.MAIN_HAND);}
    private static void draw(net.minecraft.client.gui.GuiGraphics g){
        if(!enabled)return;var mc=Minecraft.getInstance();var label=Component.translatable("sunlit_journal_core.autofish.hud").append(" · ").append(Component.translatable("sunlit_journal_core.autofish.state."+status));
        int width=mc.getWindow().getGuiScaledWidth();int box=Math.min(mc.font.width(label)+12,Math.max(20,width-12));g.fill(6,6,6+box,23,0xdd39291e);g.drawString(mc.font,label,12,11,0xFFF0C2,false);
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){if(Minecraft.getInstance().screen==null)draw(e.getGuiGraphics());}
    @SubscribeEvent public static void screen(ScreenEvent.Render.Post e){draw(e.getGuiGraphics());}
}
