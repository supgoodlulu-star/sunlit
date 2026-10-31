package dev.sunlit.reforged;
import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import com.mojang.authlib.GameProfile;
import java.util.*;

@GameTestHolder("sunlit_reforged") @PrefixGameTestTemplate(false)
public final class FishingGameTests{
    static ServerPlayer player(GameTestHelper h){return new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"FishLive"));}
    static ItemStack rod(){var s=new ItemStack(Items.FISHING_ROD);var line=new Potential.Line(Potential.Option.RARE_FISH,Potential.Tier.LEGENDARY,100);new Potential(Potential.Tier.LEGENDARY,List.of(line,line,line)).apply(s);return s;}
    @GameTest(template="empty") public static void verifiedPoolThresholdAndWeatherConditions(GameTestHelper h){
        h.assertTrue(FishingPools.rareMaxWeight()==3,"Rare threshold not user-approved 3");var night=FishingPools.eligible("spring","Ocean",true,false,false);
        h.assertTrue(night.stream().anyMatch(f->f.id().equals("unusualfishmod:raw_spindlefish")&&f.weight()==3),"Verified rare fish missing");
        h.assertTrue(FishingPools.eligible("spring","Ocean",false,false,false).stream().noneMatch(f->f.id().equals("unusualfishmod:raw_spindlefish")),"Night-only fish allowed during day");
        h.assertTrue(FishingPools.eligible("spring","Ocean",false,false,false).stream().noneMatch(f->f.id().equals("aquaculture:atlantic_halibut")),"Rain-only fish allowed in clear weather");
        h.assertTrue(FishingPools.eligible("spring","Ocean",false,true,false).stream().anyMatch(f->f.id().equals("aquaculture:atlantic_halibut")),"Rain condition omitted");
        h.assertTrue(night.stream().noneMatch(f->f.id().equals("society:neptuna")),"Skill-locked special fish injected");h.succeed();
    }
    @GameTest(template="empty") public static void actualStardewTreasureChanceAndRewardDeduplicate(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishingTests")){h.succeed();return;}var p=player(h);var rod=rod();p.setItemInHand(InteractionHand.MAIN_HAND,rod);var hook=new FishingHook(p,h.getLevel(),0,0);
        Class<?> behavior=Class.forName("com.bonker.stardewfishing.common.FishBehavior");Object fish=behavior.getConstructor(int.class,float.class,float.class,float.class,int.class,int.class).newInstance(10,1.5f,.7f,.7f,55,10);
        Class<?> start=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameStartedEvent");var event=start.getConstructor(ServerPlayer.class,FishingHook.class,ItemStack.class,ItemStack.class,behavior,boolean.class).newInstance(p,hook,rod,new ItemStack(Items.COD),fish,false);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post((net.minecraftforge.eventbus.api.Event)event);double base=((Number)Class.forName("com.bonker.stardewfishing.SFConfig").getMethod("getTreasureChestChance").invoke(null)).doubleValue();double bonus=((Number)start.getMethod("getTreasureChanceBonus").invoke(event)).doubleValue();h.assertTrue(Math.abs(base+bonus-1)<.000001,"Treasure chance did not clamp safely at 100 percent");
        Class<?> ended=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameEndedEvent");var finish=ended.getConstructor(ServerPlayer.class,FishingHook.class,ItemStack.class,boolean.class,double.class,boolean.class).newInstance(p,hook,rod,true,1.0,true);
        double chance=Rules.get().rewardRates[2];Rules.get().rewardRates[2]=100;try{net.minecraftforge.common.MinecraftForge.EVENT_BUS.post((net.minecraftforge.eventbus.api.Event)finish);var repeat=ended.getConstructor(ServerPlayer.class,FishingHook.class,ItemStack.class,boolean.class,double.class,boolean.class).newInstance(p,hook,rod,true,1.0,true);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post((net.minecraftforge.eventbus.api.Event)repeat);h.assertTrue(p.getInventory().countItem(SunlitReforged.SUSPICIOUS.get())==1,"Stardew chest reward repeated");}finally{Rules.get().rewardRates[2]=chance;hook.discard();}h.succeed();
    }
    @SuppressWarnings("unchecked") @GameTest(template="empty") public static void storedCatchRerollPreservesExtrasAndOnlyRollsOnce(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishingTests")){h.succeed();return;}var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,rod());var hook=new FishingHook(p,h.getLevel(),0,0);var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,3,2));hook.setPos(pos.getX(),pos.getY(),pos.getZ());h.getLevel().setDayTime(13000);h.getLevel().setWeatherParameters(6000,0,false,false);
        var helper=Class.forName("sereneseasons.api.season.SeasonHelper");var method=helper.getMethod("getSeasonState",net.minecraft.world.level.Level.class);var state=method.invoke(null,h.getLevel());String season=method.getReturnType().getMethod("getSeason").invoke(state).toString();
        var biome=h.getLevel().getBiome(pos);String region=biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN)||biome.is(net.minecraft.tags.BiomeTags.IS_BEACH)?"Ocean":biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)?"River":"Fresh";
        var pool=FishingPools.eligible(season,region,true,false,false);var source=pool.stream().filter(f->f.weight()>3&&net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation(f.id()))).findFirst().orElseThrow();
        var rewards=(Optional<ArrayList<ItemStack>>)Class.forName("com.bonker.stardewfishing.common.FishingHookLogic").getMethod("getStoredRewards",FishingHook.class).invoke(null,hook);var list=rewards.orElseThrow();list.add(new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(source.id())),2));list.add(new ItemStack(Items.DIAMOND));StardewBridge.reroll(p);
        String result=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString();h.assertTrue(pool.stream().anyMatch(f->f.weight()<=3&&f.id().equals(result)),"Rare catch ignored current season/weather/region");h.assertTrue(list.stream().mapToInt(ItemStack::getCount).sum()==3&&list.stream().anyMatch(s->s.is(Items.DIAMOND)),"Existing catches or other rewards lost");
        list.set(0,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(source.id()))));StardewBridge.reroll(p);h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString().equals(source.id()),"Duplicate start re-rolled same hook");hook.discard();h.succeed();
    }
}
