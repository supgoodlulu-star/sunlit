package dev.sunlit.journal;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** TEST ONLY: substitutes for pack items unavailable in the cloud environment. Never packaged. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class RewardFixtureItems {
    @SubscribeEvent public static void register(RegisterEvent event){
        if(!Boolean.getBoolean("sunlit.rewardFixtures")||!event.getRegistryKey().equals(Registries.ITEM))return;
        event.<Item>register(Registries.ITEM,helper->{
            for(String id:new String[]{"numismatics:cog","dew_drop_farmland_growth:low_quality_fertilizer","dew_drop_farmland_growth:weak_fertilizer","dew_drop_farmland_growth:hydrating_fertilizer","aquaculture:worm","crabbersdelight:crab_trap_bait","meadow:alpine_salt","society:animal_feed"}){
                var key=new ResourceLocation(id);if(!BuiltInRegistries.ITEM.containsKey(key))helper.register(key,new Item(new Item.Properties()));
            }
        });
    }
}
