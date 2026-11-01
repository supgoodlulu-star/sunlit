package dev.sunlit.reforged;

import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import java.util.*;
import static dev.sunlit.reforged.Potential.*;
import dev.sunlit.reforged.Potential.Tier;

@GameTestHolder("sunlit_reforged") @PrefixGameTestTemplate(false)
public final class ModpackEquipmentGameTests {
    static boolean enabled(){return Boolean.getBoolean("sunlit.modpackEquipmentTests");}
    static ItemStack item(String id){var key=new ResourceLocation(id);if(!BuiltInRegistries.ITEM.containsKey(key))throw new IllegalStateException("Missing actual pack item "+id);return new ItemStack(BuiltInRegistries.ITEM.get(key));}
    static Map<String,Gear> targets(){
        var out=new LinkedHashMap<String,Gear>();
        for(String metal:List.of("neptunium"))for(String type:List.of("sword","axe","pickaxe","shovel","hoe"))out.put("aquaculture:"+metal+"_"+type,category(type));
        for(String metal:List.of("manasteel","elementium"))for(String type:List.of("sword","axe","pickaxe","shovel","hoe"))out.put("botania:"+metal+"_"+(metal.equals("manasteel")&&type.equals("pickaxe")?"pick":type),category(type));
        for(String type:List.of("sword","axe","pickaxe","shovel","hoe","knife"))out.put("oreganized:electrum_"+type,category(type));
        for(String type:List.of("terra_sword","star_sword","thunder_sword","terra_axe"))out.put("botania:"+type,Gear.WEAPON);
        out.put("botania:terra_pick",Gear.PICKAXE);
        for(String type:List.of("galaxy_sword","meowmageddon","dragontooth_axe"))out.put("society:"+type,Gear.WEAPON);
        for(String metal:List.of("diamond","netherite")){
            out.put("farmersdelight:"+metal+"_knife",Gear.WEAPON);
            for(String type:List.of("hammer","reinforced_hammer","impact_hammer","reinforced_impact_hammer","destructor_hammer"))out.put("justhammers:"+metal+"_"+type,Gear.PICKAXE);
        }
        for(String metal:List.of("iron","gold","diamond","neptunium"))out.put("aquaculture:"+metal+"_fishing_rod",Gear.ROD);
        out.put("netherdepthsupgrade:lava_fishing_rod",Gear.ROD);
        for(String prefix:List.of("aquaculture:neptunium_","botania:terrasteel_"))for(String part:List.of("helmet","chestplate","leggings","boots"))out.put(prefix+part,Gear.ARMOR);
        return out;
    }
    static Gear category(String type){return switch(type){case "pickaxe"->Gear.PICKAXE;case "shovel"->Gear.SHOVEL;case "hoe"->Gear.HOE;default->Gear.WEAPON;};}
    static CompoundTag otherData(ItemStack item){var n=item.save(new CompoundTag());n.getCompound("tag").remove(Potential.KEY);return n;}
    @GameTest(template="empty") public static void actualPackEquipmentHasCorrectPools(GameTestHelper h){
        if(!enabled()){h.succeed();return;}
        for(var entry:targets().entrySet()){
            var s=item(entry.getKey());h.assertTrue(Potential.gear(s).orElse(null)==entry.getValue(),"Wrong category: "+entry.getKey()+" actual="+Potential.gear(s));
            var rolled=Potential.roll(Optional.empty(),Potential.gear(s).orElseThrow(),Cube.SUSPICIOUS,RandomSource.create(17),Rules.defaults());
            h.assertTrue(rolled.lines().stream().allMatch(l->l.option().gear.contains(entry.getValue())),"Wrong pool for "+entry.getKey());
        }
        System.out.println("MODPACK_EQUIPMENT_TARGETS "+targets());h.succeed();
    }
    @GameTest(template="empty") public static void lowTierToolsAndUtilityItemsStayExcluded(GameTestHelper h){
        if(!enabled()){h.succeed();return;}
        for(String id:List.of("minecraft:iron_pickaxe","minecraft:golden_pickaxe","minecraft:stone_axe","minecraft:iron_sword","minecraft:bow","minecraft:stick","farmersdelight:iron_knife","justhammers:iron_hammer","justhammers:gold_hammer","justhammers:gold_destructor_hammer","society:princess_hairbrush","botania:glass_pickaxe"))h.assertTrue(Potential.gear(item(id)).isEmpty(),"Unexpected lower/utility item: "+id);
        System.out.println("PACK_GOLD_LEVELS vanilla="+((TieredItem)item("minecraft:golden_pickaxe").getItem()).getTier().getLevel()+" hammer="+((TieredItem)item("justhammers:gold_hammer").getItem()).getTier().getLevel());
        var p=ReforgedGameTests.player(h);var m=new StationMenu(1,p.getInventory(),p);m.work.setItem(0,item("minecraft:iron_pickaxe"));m.work.setItem(1,new ItemStack(SunlitReforged.SUSPICIOUS.get(),2));h.assertTrue(!m.roll()&&m.work.getItem(1).getCount()==2,"Rejected item consumed dreamdrops");h.succeed();
    }
    @GameTest(template="empty") public static void actualPackItemsRollAndPreserveForeignData(GameTestHelper h){
        if(!enabled()){h.succeed();return;}
        var p=ReforgedGameTests.player(h);var m=new StationMenu(1,p.getInventory(),p);
        for(var entry:targets().entrySet()){
            var s=item(entry.getKey());s.setHoverName(net.minecraft.network.chat.Component.literal("외부 이름 유지"));s.setDamageValue(7);s.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,2);s.getOrCreateTag().putString("PackLevelingProbe","original");var before=otherData(s);
            h.assertTrue(m.getSlot(0).mayPlace(s),"Server slot rejected "+entry.getKey());m.work.setItem(0,s);m.work.setItem(1,new ItemStack(SunlitReforged.SUSPICIOUS.get(),2));
            h.assertTrue(m.roll()&&!m.pending()&&m.work.getItem(1).getCount()==1,"Server roll failed: "+entry.getKey());h.assertTrue(Potential.get(s).isPresent()&&before.equals(otherData(s)),"Foreign data changed on "+entry.getKey());
            var restored=ItemStack.of(s.save(new CompoundTag()));h.assertTrue(Potential.gear(restored).orElse(null)==entry.getValue()&&Potential.get(restored).equals(Potential.get(s)),"Item persistence broken: "+entry.getKey());
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void actualRodsUseNativeFishingAndKeepTackle(GameTestHelper h)throws Exception{
        if(!enabled()){h.succeed();return;}
        for(var entry:targets().entrySet())if(entry.getValue()==Gear.ROD){
            var s=item(entry.getKey());if(entry.getKey().startsWith("aquaculture:")){
                var handler=(net.minecraftforge.items.IItemHandlerModifiable)s.getItem().getClass().getMethod("getHandler",ItemStack.class).invoke(null,s);handler.setStackInSlot(0,item("aquaculture:iron_hook"));
            }
            var before=otherData(s);new Potential(Tier.LEGENDARY,List.of(new Line(Option.RARE_FISH,Tier.LEGENDARY,40),new Line(Option.TREASURE_CHANCE,Tier.LEGENDARY,50),new Line(Option.FISHING_BAR,Tier.LEGENDARY,100))).apply(s);
            var event=FishingPotentialGameTests.event(h,s);int original=(int)FishingPotentialGameTests.number(event,"getBarSize");double chest=FishingPotentialGameTests.number(event,"getTreasureChanceBonus");double base=((Number)Class.forName("com.bonker.stardewfishing.SFConfig").getMethod("getTreasureChestChance").invoke(null)).doubleValue();FishingPotentialGameTests.post(event);
            h.assertTrue(FishingPotentialGameTests.number(event,"getBarSize")==Math.min(142,original*2)&&Math.abs(FishingPotentialGameTests.number(event,"getTreasureChanceBonus")-(Math.min(1,base+chest+.5)-base))<1e-8&&StardewBridge.rareBonus(s)==40,"Native fishing effects failed: "+entry.getKey());
            h.assertTrue(before.equals(otherData(s)),"Tackle/rod metadata changed: "+entry.getKey());
            var restored=ItemStack.of(s.save(new CompoundTag()));h.assertTrue(before.equals(otherData(restored))&&Effects.sum(restored,Option.FISHING_BAR)==100,"Rod capability/save roundtrip changed: "+entry.getKey());
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void customWeaponMasterRecoveryKeepsLinesAndEffects(GameTestHelper h){
        if(!enabled()){h.succeed();return;}
        var p=ReforgedGameTests.player(h);var s=item("society:galaxy_sword");var l=new Line(Option.ATTACK_FLAT,Tier.UNIQUE,3);new Potential(Tier.UNIQUE,List.of(l,l,l)).apply(s);s.getOrCreateTag().putString("ForeignPrefix","preserve");var old=Potential.get(s).orElseThrow();var before=otherData(s);var m=new StationMenu(1,p.getInventory(),p);m.work.setItem(0,s);m.work.setItem(1,new ItemStack(SunlitReforged.MASTER.get(),2));
        h.assertTrue(m.roll()&&m.pending(),"Custom weapon comparison failed");var restored=new StationMenu(2,p.getInventory(),p);h.assertTrue(restored.choose(true,restored.saved.getCompound("pending").getString("rollId")),"Pending custom weapon failed recovery");var result=restored.work.getItem(0);h.assertTrue(before.equals(otherData(result))&&Potential.get(result).orElseThrow().lines().equals(old.lines())&&Effects.sum(result,Option.ATTACK_FLAT)==9,"Custom weapon lost old data/effects");h.succeed();
    }
}
