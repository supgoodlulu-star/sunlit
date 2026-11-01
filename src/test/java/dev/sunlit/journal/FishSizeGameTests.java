package dev.sunlit.journal;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import java.util.*;
@GameTestHolder(SunlitJournal.MODID) @PrefixGameTestTemplate(false)
public final class FishSizeGameTests {
    public static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FishSizeTest"));}
    @GameTest(template="empty") public static void approvedBandsAndSaleMultipliers(GameTestHelper h){
        double[] rolls={0,.199999,.2,.799999,.8,.969999,.97,.997999,.998,.999999};FishSizes.Size[] expected={FishSizes.Size.SMALL,FishSizes.Size.SMALL,FishSizes.Size.NORMAL,FishSizes.Size.NORMAL,FishSizes.Size.LARGE,FishSizes.Size.LARGE,FishSizes.Size.GIANT,FishSizes.Size.GIANT,FishSizes.Size.COLOSSAL,FishSizes.Size.COLOSSAL};
        for(int i=0;i<rolls.length;i++)h.assertTrue(FishSizes.choose(rolls[i])==expected[i],"Wrong distribution boundary");
        h.assertTrue(Arrays.equals(Arrays.stream(FishSizes.Size.values()).mapToDouble(s->s.multiplier).toArray(),new double[]{1,1,1.3,2,3}),"Approved price multipliers changed");
        h.assertTrue(FishSizes.saleMultiplier(new ItemStack(Items.COD))==1,"Old fish devalued or retroactively resized");h.succeed();
    }
    @GameTest(template="empty") public static void vanillaFishingTableRunsRegisteredSizeModifier(GameTestHelper h){
        var p=player(h);var hook=new net.minecraft.world.entity.projectile.FishingHook(p,h.getLevel(),0,0);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,hook).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,hook.position()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD)).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.FISHING);
        var table=h.getLevel().getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("minecraft","gameplay/fishing/fish"));var rewards=table.getRandomItems(params);
        h.assertTrue(!rewards.isEmpty()&&rewards.stream().allMatch(s->FishSizes.fish(s)&&FishSizes.size(s).isPresent()),"Native fishing loot bypassed registered global size modifier");h.assertTrue(!SunlitJournal.data(p).contains("fishRecords"),"Rolling pending loot awarded a record");h.succeed();
    }
    @GameTest(template="empty") public static void catchRewardsGetPersistentSizesAndKeepQualityAndStackSemantics(GameTestHelper h){
        var p=player(h);var fish=new ItemStack(Items.COD,8);fish.setHoverName(net.minecraft.network.chat.Component.literal("이름 유지"));fish.getOrCreateTagElement("quality_food").putInt("quality",3);fish.getOrCreateTag().putString("OtherMod","preserve");var loot=new ArrayList<ItemStack>();for(int i=0;i<8;i++)loot.add(fish.copyWithCount(1));loot.add(new ItemStack(Items.DIAMOND,2));FishSizes.decorate(loot,p,RandomSource.create(6));
        h.assertTrue(loot.size()==9&&loot.stream().mapToInt(ItemStack::getCount).sum()==10,"Reward quantities changed");var unique=new HashSet<UUID>();
        for(int i=0;i<8;i++){var s=loot.get(i);h.assertTrue(s.getCount()==1&&FishSizes.size(s).isPresent()&&s.getTag().getCompound("quality_food").getInt("quality")==3&&s.getTag().getString("OtherMod").equals("preserve")&&s.getHoverName().getString().equals("이름 유지"),"Quality/name/foreign NBT lost");unique.add(s.getTag().getCompound(FishSizes.KEY).getUUID("catchId"));var restored=ItemStack.of(s.save(new CompoundTag()));h.assertTrue(ItemStack.matches(restored,s)&&FishSizes.saleMultiplier(restored)==FishSizes.saleMultiplier(s),"Save/reconnect changed size or price");}
        h.assertTrue(unique.size()==8&&FishSizes.size(loot.get(8)).isEmpty(),"Nonfish decorated or sizes shared per unit");var before=loot.stream().map(s->s.save(new CompoundTag())).toList();FishSizes.decorate(loot,p,RandomSource.create(999));h.assertTrue(before.equals(loot.stream().map(s->s.save(new CompoundTag())).toList()),"Same rewards rerolled on duplicate hook/reload");var batch=new ArrayList<>(List.of(new ItemStack(Items.SALMON,3)));FishSizes.decorate(batch,p,RandomSource.create(3));h.assertTrue(batch.size()==1&&batch.get(0).getCount()==3&&FishSizes.size(batch.get(0)).isPresent(),"Existing skill reward-group semantics changed");h.succeed();
    }
    @GameTest(template="empty") public static void lengthsRespectSpeciesAndBands(GameTestHelper h){
        var p=player(h);h.assertTrue(FishSizeProfiles.defaults().get("aquaculture:minnow")<FishSizeProfiles.defaults().get("aquaculture:tuna"),"Small and large species have same range");
        for(int i=0;i<500;i++){var s=new ItemStack(Items.COD);FishSizes.stamp(s,p,RandomSource.create(i));var band=FishSizes.size(s).orElseThrow();double length=s.getTag().getCompound(FishSizes.KEY).getInt("lengthTenthsCm")/10d,base=FishSizeProfiles.baseCm(s);h.assertTrue(length>=base*band.min&&length<base*band.max,"Length crossed displayed category boundary");}
        h.succeed();
    }
    @GameTest(template="empty") public static void recordsCommitOnlyAfterAwardAndDoNotRepeatOnTrade(GameTestHelper h){
        var p=player(h);var s=new ItemStack(Items.SALMON);FishSizes.stamp(s,p,RandomSource.create(8));h.assertTrue(!SunlitJournal.data(p).contains("fishRecords"),"Pending/failed minigame awarded a record");FishSizes.commit(s,p);int max=SunlitJournal.data(p).getCompound("fishRecords").getInt("minecraft:salmon");h.assertTrue(max==s.getTag().getCompound(FishSizes.KEY).getInt("lengthTenthsCm"),"Successful catch not recorded");FishSizes.commit(s,p);var other=player(h);FishSizes.commit(s,other);h.assertTrue(!SunlitJournal.data(other).contains("fishRecords")&&SunlitJournal.data(p).getCompound("fishRecords").getInt("minecraft:salmon")==max,"Trading/repeated delivery forged a record");h.succeed();
    }
    @GameTest(template="empty") public static void malformedAndOldDataStayUntouched(GameTestHelper h){
        var s=new ItemStack(Items.COD);s.getOrCreateTagElement(FishSizes.KEY).putInt("schemaVersion",99);var before=s.copy();FishSizes.stamp(s,player(h),RandomSource.create(5));h.assertTrue(ItemStack.matches(s,before)&&FishSizes.saleMultiplier(s)==1,"Unknown saved data overwritten or charged extra");var old=new ItemStack(Items.SALMON,64);h.assertTrue(!old.hasTag()&&FishSizes.saleMultiplier(old)==1&&!old.hasTag(),"Pricing mutated old fish");h.succeed();
    }
    @GameTest(template="empty") public static void qualityFoodApiKeepsSizesAndConversionsCannotCopyPrice(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishSizeIntegration")){h.succeed();return;}
        var s=new ItemStack(Items.COD);FishSizes.stamp(s,player(h),RandomSource.create(9));var before=s.getTag().getCompound(FishSizes.KEY).copy();var quality=Class.forName("de.cadentem.quality_food.core.Quality");var diamond=quality.getField("DIAMOND").get(null);
        Class.forName("de.cadentem.quality_food.util.QualityUtils").getMethod("applyQuality",ItemStack.class,quality).invoke(null,s,diamond);
        h.assertTrue(s.getTag().getCompound(FishSizes.KEY).equals(before)&&s.getTag().getCompound("quality_food").getInt("quality")==3,"Real Quality Food API overwrote size or lost quality");
        var cooked=new ItemStack(Items.COOKED_COD);cooked.setTag(s.getTag().copy());h.assertTrue(FishSizes.size(cooked).isEmpty()&&FishSizes.saleMultiplier(cooked)==1,"A processed item inherited raw-fish size markup");h.succeed();
    }
    @GameTest(template="empty") public static void actualStardewRewardListKeepsCountsAndDoesNotReroll(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishSizeIntegration")){h.succeed();return;}
        var p=player(h);var hook=new net.minecraft.world.entity.projectile.FishingHook(p,h.getLevel(),0,0);var rewards=new ArrayList<>(List.of(new ItemStack(Items.COD,3),new ItemStack(Items.EMERALD)));
        var type=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameModifyRewardsEvent");var event=(net.minecraftforge.eventbus.api.Event)type.getConstructor(net.minecraft.server.level.ServerPlayer.class,net.minecraft.world.entity.projectile.FishingHook.class,ItemStack.class,List.class).newInstance(p,hook,new ItemStack(Items.FISHING_ROD),rewards);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        h.assertTrue(rewards.size()==2&&rewards.get(0).getCount()==3&&rewards.stream().filter(FishSizes::fish).allMatch(s->FishSizes.size(s).isPresent()),"Actual Stardew API did not update the real reward list");var before=rewards.stream().map(s->s.save(new CompoundTag())).toList();var again=(net.minecraftforge.eventbus.api.Event)type.getConstructor(net.minecraft.server.level.ServerPlayer.class,net.minecraft.world.entity.projectile.FishingHook.class,ItemStack.class,List.class).newInstance(p,hook,new ItemStack(Items.FISHING_ROD),rewards);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(again);h.assertTrue(before.equals(rewards.stream().map(s->s.save(new CompoundTag())).toList()),"Stardew duplicate rewards rerolled");h.succeed();
    }
}
