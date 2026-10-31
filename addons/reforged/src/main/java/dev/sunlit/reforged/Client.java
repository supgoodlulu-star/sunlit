package dev.sunlit.reforged;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
@Mod.EventBusSubscriber(modid=SunlitReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class Client{
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(SunlitReforged.STATION.get(),StationScreen::new));}
    public static void receive(CompoundTag n){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.player.containerMenu instanceof StationMenu m&&m.containerId==n.getInt("menuId")){m.clientState=n;if(mc.screen instanceof StationScreen s)s.update();}}
    @Mod.EventBusSubscriber(modid=SunlitReforged.ID,value=Dist.CLIENT)
    public static final class Tooltips{
        @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e){Potential.get(e.getItemStack()).ifPresent(p->{
            e.getToolTip().add(Component.literal("잠재능력 · "+p.tier().label).withStyle(s->s.withColor(p.tier().color)));
            for(var l:p.lines())e.getToolTip().add(Component.literal("  "+l.label()+" ["+l.tier().label+"]").withStyle(s->s.withColor(l.tier().color)));
        });}
    }
}
