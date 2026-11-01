package dev.sunlit.journal;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.network.chat.Component;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class FishSizeTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){FishSizes.size(e.getItemStack()).ifPresent(size->{
        var n=e.getItemStack().getTag().getCompound(FishSizes.KEY);
        e.getToolTip().add(Component.literal("길이 "+FishSizes.length(e.getItemStack())+"cm · "+size.label+" · 판매가 ×"+java.math.BigDecimal.valueOf(size.multiplier).stripTrailingZeros().toPlainString()).withStyle(s->s.withColor(size.color)));
        e.getToolTip().add(Component.literal("낚은 사람 · "+n.getString("catcherName")).withStyle(s->s.withColor(0xB5A18A)));
    });}
}
