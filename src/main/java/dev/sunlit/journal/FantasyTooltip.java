package dev.sunlit.journal;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class FantasyTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        if(!event.getItemStack().hasTag())return;
        var skin=FantasyGacha.find(event.getItemStack().getTag().getString(FantasySkins.KEY));
        if(skin!=null)event.getToolTip().add(Component.literal("외형 · "+skin.name()+" ["+skin.rarityName()+"]").withStyle(style->style.withColor(skin.color()&0xFFFFFF)));
    }
}
