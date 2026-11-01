package dev.sunlit.reforged;
import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import com.google.gson.*;
import java.util.*;
import java.io.*;
import static dev.sunlit.reforged.Potential.*;
import dev.sunlit.reforged.Potential.Tier;
@GameTestHolder("sunlit_reforged") @PrefixGameTestTemplate(false)
public final class FishingPotentialGameTests {
    static ItemStack rod(Option option,double value){var rod=new ItemStack(Items.FISHING_ROD);var line=new Line(option,Tier.LEGENDARY,value);new Potential(Tier.LEGENDARY,List.of(line,new Line(Option.DURABILITY,Tier.RARE,0),new Line(Option.LIFE_XP,Tier.RARE,0))).apply(rod);return rod;}
    static ItemStack legacy(){var s=rod(Option.RARE_FISH,40);s.getTag().getCompound(Potential.KEY).putInt("schemaVersion",1);s.getOrCreateTag().putString("OtherMod","keep");return s;}
    @GameTest(template="empty") public static void legacyNbtKeepsCombinedBenefitWithoutRewritingItem(GameTestHelper h){
        var s=legacy();var original=s.save(new CompoundTag());var p=Potential.get(s).orElseThrow();
        h.assertTrue(p.lines().get(0).option()==Option.LEGACY_FISHING&&StardewBridge.rareBonus(s)==40&&StardewBridge.treasureBonus(s)==40&&Effects.sum(s,Option.FISHING_BAR)==0,"Legacy combined fishing benefit was lost");
        h.assertTrue(s.save(new CompoundTag()).equals(original),"Reading old NBT rewrote inventory");
        p.apply(s);h.assertTrue(s.getTag().getCompound(Potential.KEY).getInt("schemaVersion")==2&&Potential.get(s).orElseThrow().equals(p)&&s.getTag().getString("OtherMod").equals("keep"),"Legacy round trip/foreign NBT broken");
        h.assertTrue(StardewBridge.treasureBonus(rod(Option.RARE_FISH,40))==0&&StardewBridge.rareBonus(rod(Option.TREASURE_CHANCE,50))==0,"New split option still affects both rewards");h.succeed();
    }
    @GameTest(template="empty") public static void oldNineteenOptionPackGetsOnlyApprovedDefaults(GameTestHelper h)throws Exception{
        JsonObject root;try(var in=Rules.class.getResourceAsStream("/data/sunlit_reforged/potential_rules/default.json")){root=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        var old=new JsonArray();for(var entry:root.getAsJsonArray("options")){var o=entry.getAsJsonObject();if(!Set.of("treasure_chance","fishing_bar").contains(o.get("id").getAsString()))old.add(o);if(o.get("id").getAsString().equals("rare_fish")){o.addProperty("weight",7);o.getAsJsonArray("values").set(3,new JsonPrimitive(43));}}
        root.add("options",old);var r=Rules.parse(root);
        h.assertTrue(old.size()==19&&r.weights[Option.RARE_FISH.ordinal()]==7&&r.values[Option.RARE_FISH.ordinal()][3]==43,"Old custom values overwritten");
        h.assertTrue(Arrays.equals(r.values[Option.TREASURE_CHANCE.ordinal()],new double[]{10,20,30,50})&&Arrays.equals(r.values[Option.FISHING_BAR.ordinal()],new double[]{30,50,75,100}),"Approved defaults not loaded for old pack");
        var invalid=root.deepCopy();invalid.getAsJsonArray("options").remove(0);boolean rejected=false;try{Rules.parse(invalid);}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected,"Missing original option accepted");h.succeed();
    }
    @GameTest(template="empty") public static void rodPoolHasFiveOptionsAndLegacyNeverRerolls(GameTestHelper h){
        var r=Rules.defaults();var expected=Set.of(Option.RARE_FISH,Option.TREASURE_CHANCE,Option.FISHING_BAR,Option.LIFE_XP,Option.DURABILITY);var seen=new HashSet<Option>();r.weights[Option.LEGACY_FISHING.ordinal()]=1_000_000;
        for(int i=0;i<500;i++){var result=Potential.roll(Optional.empty(),Gear.ROD,Cube.MASTER,RandomSource.create(i),r);for(var line:result.lines()){h.assertTrue(expected.contains(line.option()),"Old hybrid rolled again");seen.add(line.option());}}
        h.assertTrue(seen.equals(expected),"Some rod options cannot roll");for(var gear:Gear.values())if(gear!=Gear.ROD)for(int i=0;i<50;i++)h.assertTrue(Potential.roll(Optional.empty(),gear,Cube.MASTER,RandomSource.create(i),r).lines().stream().noneMatch(l->Set.of(Option.RARE_FISH,Option.TREASURE_CHANCE,Option.FISHING_BAR,Option.LEGACY_FISHING).contains(l.option())),"Fishing option rolled on another gear");h.succeed();
    }
    static Object event(GameTestHelper h,ItemStack rod)throws Exception{
        var p=FishingGameTests.player(h);p.setItemInHand(InteractionHand.MAIN_HAND,rod);var hook=new FishingHook(p,h.getLevel(),0,0);var behavior=Class.forName("com.bonker.stardewfishing.common.FishBehavior");var fish=behavior.getConstructor(int.class,float.class,float.class,float.class,int.class,int.class).newInstance(10,1.5f,.7f,.7f,55,10);
        var type=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameStartedEvent");return type.getConstructor(ServerPlayer.class,FishingHook.class,ItemStack.class,ItemStack.class,behavior,boolean.class).newInstance(p,hook,rod,new ItemStack(Items.COD),fish,false);
    }
    static double number(Object o,String method)throws Exception{return ((Number)o.getClass().getMethod(method).invoke(o)).doubleValue();}
    static void post(Object o){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post((net.minecraftforge.eventbus.api.Event)o);}
    @GameTest(template="empty") public static void realEventSeparatesRareTreasureAndBar(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishingTests")){h.succeed();return;}
        var rare=event(h,rod(Option.RARE_FISH,40));double before=number(rare,"getTreasureChanceBonus"),size=number(rare,"getBarSize");post(rare);h.assertTrue(number(rare,"getTreasureChanceBonus")==before&&number(rare,"getBarSize")==size,"Rare fish option changed chest/bar");
        var treasure=event(h,rod(Option.TREASURE_CHANCE,30));before=number(treasure,"getTreasureChanceBonus");size=number(treasure,"getBarSize");double base=((Number)Class.forName("com.bonker.stardewfishing.SFConfig").getMethod("getTreasureChestChance").invoke(null)).doubleValue();post(treasure);h.assertTrue(Math.abs(number(treasure,"getTreasureChanceBonus")-(Math.min(1,base+before+.30)-base))<1e-8&&number(treasure,"getBarSize")==size,"Treasure probability or isolated effect wrong");
        var bar=event(h,rod(Option.FISHING_BAR,50));bar.getClass().getMethod("setBarSize",int.class).invoke(bar,64);before=number(bar,"getTreasureChanceBonus");post(bar);h.assertTrue(number(bar,"getBarSize")==96&&number(bar,"getTreasureChanceBonus")==before,"Bar failed to preserve/scale existing modifiers");
        var old=event(h,legacy());before=number(old,"getTreasureChanceBonus");post(old);h.assertTrue(Math.abs(number(old,"getTreasureChanceBonus")-(Math.min(1,base+before+.40)-base))<1e-8,"Old rod treasure effect changed");h.succeed();
    }
    @GameTest(template="empty") public static void tierBarSizesAndNativeLimit(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishingTests")){h.succeed();return;}
        double[] bonuses={30,50,75,100};int[] expected={47,54,63,72};for(int i=0;i<4;i++){var e=event(h,rod(Option.FISHING_BAR,bonuses[i]));e.getClass().getMethod("setBarSize",int.class).invoke(e,36);post(e);h.assertTrue(number(e,"getBarSize")==expected[i],"Wrong native bar size at tier "+i);}
        var s=rod(Option.FISHING_BAR,100);var l=new Line(Option.FISHING_BAR,Tier.LEGENDARY,100);new Potential(Tier.LEGENDARY,List.of(l,l,l)).apply(s);var e=event(h,s);post(e);h.assertTrue(number(e,"getBarSize")==142,"Native 142px limit was bypassed");h.succeed();
    }
    @GameTest(template="empty") public static void pendingOldMasterRodPreservesHybridLines(GameTestHelper h){
        if(!FishingPools.ready()){h.succeed();return;}var p=ReforgedGameTests.player(h);var rod=legacy();var n=rod.getTag().getCompound(Potential.KEY);n.putString("potentialTier","UNIQUE");for(var tag:n.getList("lines",10))((CompoundTag)tag).putString("lineTier","UNIQUE");var original=Potential.get(rod).orElseThrow();var m=new StationMenu(1,p.getInventory(),p);m.work.setItem(0,rod);m.work.setItem(1,new ItemStack(SunlitReforged.MASTER.get(),2));double prior=Rules.get().upgrades[2][2];Rules.get().upgrades[2][2]=100;
        try{h.assertTrue(m.roll()&&m.pending(),"Old rod master comparison failed");var restored=new StationMenu(2,p.getInventory(),p);h.assertTrue(restored.choose(true,restored.saved.getCompound("pending").getString("rollId")),"Old rod choice lost on recovery");var result=Potential.get(restored.work.getItem(0)).orElseThrow();h.assertTrue(result.tier()==Tier.LEGENDARY&&result.lines().equals(original.lines())&&StardewBridge.treasureBonus(restored.work.getItem(0))==40&&StardewBridge.rareBonus(restored.work.getItem(0))==40,"Keeping old rod changed its lines or combined effects");}finally{Rules.get().upgrades[2][2]=prior;}h.succeed();
    }
}
