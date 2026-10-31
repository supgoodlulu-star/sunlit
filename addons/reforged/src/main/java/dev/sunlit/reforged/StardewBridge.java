package dev.sunlit.reforged;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.common.MinecraftForge;
import java.util.*;
import java.util.function.Consumer;

/** Exact Stardew Fishing 3.7 events and stored-reward API, inspected from the 4.1.5A distribution. */
public final class StardewBridge{
    public static boolean installed;
    @SuppressWarnings({"rawtypes","unchecked"}) public static void register(){
        try{
            var started=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameStartedEvent");var ended=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameEndedEvent");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,(Class)started,(Consumer<Event>)StardewBridge::start);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,(Class)ended,(Consumer<Event>)StardewBridge::end);installed=true;
        }catch(ClassNotFoundException absent){installed=false;}
    }
    public static ItemStack activeRod(ServerPlayer p){
        try{var hand=(InteractionHand)Class.forName("com.bonker.stardewfishing.proxy.ItemUtils").getMethod("getRodHand",Player.class).invoke(null,p);return hand==null?ItemStack.EMPTY:p.getItemInHand(hand);}catch(ReflectiveOperationException|RuntimeException e){return p.getMainHandItem();}
    }
    static void start(Event event){
        try{var c=event.getClass();var p=(ServerPlayer)c.getMethod("getPlayer").invoke(event);if(!Activity.eligible(p))return;var rod=(ItemStack)c.getMethod("getFishingRod").invoke(event);double bonus=Effects.sum(rod,Potential.Option.RARE_FISH)/100;if(bonus<=0)return;
            double base=((Number)Class.forName("com.bonker.stardewfishing.SFConfig").getMethod("getTreasureChestChance").invoke(null)).doubleValue();double existing=((Number)c.getMethod("getTreasureChanceBonus").invoke(event)).doubleValue();double total=Math.max(0,Math.min(1,base+existing+bonus));c.getMethod("setTreasureChanceBonus",double.class).invoke(event,total-base);
        }catch(ReflectiveOperationException|RuntimeException e){SunlitReforged.LOG.warn("Stardew treasure bonus unavailable",e);}
    }
    static void end(Event event){
        try{var c=event.getClass();var p=(ServerPlayer)c.getMethod("getPlayer").invoke(event);if(!Activity.eligible(p)||!(Boolean)c.getMethod("wasSuccessful").invoke(event)||!(Boolean)c.getMethod("gotChest").invoke(event))return;
            var hook=(FishingHook)c.getMethod("getHook").invoke(event);if(!Activity.fishingOnce(hook,"cube"))return;
            if(p.getRandom().nextDouble()<Rules.get().rewardRates[2]/100)Activity.give(p,SunlitReforged.SUSPICIOUS.get());
        }catch(ReflectiveOperationException|RuntimeException e){SunlitReforged.LOG.warn("Stardew treasure reward unavailable",e);}
    }
    @SuppressWarnings("unchecked") public static void reroll(ServerPlayer p){
        if(!installed||p.fishing==null||!Activity.eligible(p)||!FishingPools.ready()||!Rules.get().rareFishingTable.isBlank())return;
        double bonus=Effects.sum(activeRod(p),Potential.Option.RARE_FISH);if(bonus<=0)return;
        try{
            var hook=p.fishing;
            var cap=(net.minecraftforge.common.capabilities.Capability<Object>)Class.forName("com.bonker.stardewfishing.common.FishingHookLogic$CapProvider").getField("CAP").get(null);
            var logic=hook.getCapability(cap).resolve();if(logic.isEmpty()||logic.get().getClass().getField("event").get(logic.get())!=null)return;
            var pos=BlockPos.containing(hook.position());var biome=p.serverLevel().getBiome(pos);String region=biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_BEACH)?"Ocean":biome.is(BiomeTags.IS_RIVER)?"River":"Fresh";
            var helper=Class.forName("sereneseasons.api.season.SeasonHelper");var method=helper.getMethod("getSeasonState",net.minecraft.world.level.Level.class);var state=method.invoke(null,p.serverLevel());
            String season=method.getReturnType().getMethod("getSeason").invoke(state).toString().toLowerCase(Locale.ROOT);
            var pool=FishingPools.eligible(season,region,Math.floorMod(p.serverLevel().getDayTime(),24000)>=13000,p.serverLevel().isRaining(),p.serverLevel().isThundering());
            var stored=(Optional<ArrayList<ItemStack>>)Class.forName("com.bonker.stardewfishing.common.FishingHookLogic").getMethod("getStoredRewards",FishingHook.class).invoke(null,hook);if(stored.isEmpty())return;var rewards=stored.get();
            for(int i=0;i<rewards.size();i++){
                var old=rewards.get(i);String id=BuiltInRegistries.ITEM.getKey(old.getItem()).toString();var source=pool.stream().filter(f->f.id().equals(id)).findFirst();
                // Unlisted/skill-locked special fish and already-rare fish are never overwritten.
                if(source.isEmpty())continue;if(source.get().weight()<=FishingPools.rareMaxWeight())return;
                if(!Activity.fishingOnce(hook,"rare_roll")||p.getRandom().nextDouble()>=Effects.chance(bonus))return;
                var selected=FishingPools.choose(pool,p.getRandom());if(selected.isEmpty())return;var item=BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(selected.get().id()));
                rewards.set(i,new ItemStack(item,1));if(old.getCount()>1)rewards.add(i+1,old.copyWithCount(old.getCount()-1));
                SunlitReforged.debug("rare fish player={} original={} selected={} season={} region={}",p.getUUID(),id,selected.get().id(),season,region);return;
            }
        }catch(ReflectiveOperationException|RuntimeException e){SunlitReforged.debug("Stardew rare reroll unavailable: {}",e.toString());}
    }
}
