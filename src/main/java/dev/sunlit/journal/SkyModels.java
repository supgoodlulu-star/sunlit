package dev.sunlit.journal;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SkyModels {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        for(String tier:new String[]{"wooden","stone","iron","golden","diamond","netherite"})for(String kind:new String[]{"sword","pickaxe"}){
            var item=BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft",tier+"_"+kind));
            ItemProperties.register(item,new ResourceLocation(SunlitJournal.MODID,"sky_skin"),(stack,level,entity,seed)->stack.hasTag()&&kind.equals(stack.getTag().getString("SunlitSkySkin"))?1:0);
        }
    });}
}
