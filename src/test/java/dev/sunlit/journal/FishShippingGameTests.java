package dev.sunlit.journal;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(SunlitJournal.MODID) @PrefixGameTestTemplate(false)
public final class FishShippingGameTests {
    @GameTest(template="empty") public static void originalKubeJsShippingQualitySkillsPreviewAndRemoval(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.fishSizeIntegration")){h.succeed();return;}
        var manager=Class.forName("dev.latvian.mods.kubejs.KubeJS").getMethod("getStartupScriptManager").invoke(null);var type=manager.getClass();var context=type.getField("context").get(manager);var scope=type.getField("topLevelScope").get(manager);
        String script="""
        (function() {
          var oldTrades = global.trades;
          var expect = (ok, text) => { if (!ok) throw new Error(text); };
          try {
            global.trades = new Map();
            global.trades.set('minecraft:cod', {value:100, multiplier:'shippingbin:fish_sell_multiplier'});
            global.trades.set('aquaculture:bluegill', {value:100, multiplier:'shippingbin:fish_sell_multiplier'});
            global.trades.set('minecraft:diamond', {value:500, multiplier:'shippingbin:fish_sell_multiplier'});
            var attrs = [{Name:'shippingbin:fish_sell_multiplier', Base:2}];
            var sized = (band, count) => Item.of('minecraft:cod', count, '{quality_food:{quality:2},sunlit_fish_size:{schemaVersion:1,fishId:"minecraft:cod",size:"'+band+'",lengthTenthsCm:1200,catcher:[I;1,2,3,4],catchId:[I;5,6,7,8]}}');
            var inv = (items) => ({getStackInSlot:(i)=>items[i],setStackInSlot:(i,s)=>{items[i]=Item.of(s);}});
            var bands = ['SMALL','NORMAL','LARGE','GIANT','COLOSSAL'];
            var values = [300,300,390,600,900];
            for(var i=0;i<bands.length;i++) {
              var items=[sized(bands[i],1)];
              var result=global.processShippingBinInventory(inv(items),1,attrs,[],false,true);
              expect(result.calculatedValue===values[i], 'Wrong quality+skill+size price '+bands[i]+': '+result.calculatedValue);
              expect(items[0].count===1, 'Simulated preview consumed a fish');
            }
            var mixed=[sized('GIANT',2),Item.of('minecraft:dirt'),Item.of('minecraft:diamond',2)];
            var result=global.processShippingBinInventory(inv(mixed),3,attrs,[],true,false);
            expect(result.calculatedValue===3200, 'Mixed fish/normal item price changed');
            expect(String(result.removedItems)==='0,2', 'Original removal slots not mapped back');
            expect(mixed[0].count===2, 'Deferred-removal bin lost inventory');
            var direct=[sized('COLOSSAL',1),Item.of('minecraft:dirt')];
            var sold=global.processShippingBinInventory(inv(direct),2,attrs,[],false,false);
            expect(sold.calculatedValue===900 && direct[0].isEmpty() && !direct[1].isEmpty(), 'Smart-bin removal/unsellable handling changed');
            var oldFish=[Item.of('minecraft:cod')];
            expect(global.processShippingBinInventory(inv(oldFish),1,attrs,[],false,true).calculatedValue===200, 'Legacy fish price changed');
            var blue=Item.of('aquaculture:bluegill',1,'{quality_food:{quality:1},sunlit_fish_size:{schemaVersion:1,fishId:"aquaculture:bluegill",size:"COLOSSAL",lengthTenthsCm:1200,catcher:[I;1,2,3,4],catchId:[I;5,6,7,8]}}');
            expect(global.processShippingBinInventory(inv([blue]),1,attrs,['bluegill_meridian'],false,true).calculatedValue===4998, 'Original species-specific stage bonus changed');
            return true;
          } finally { global.trades=oldTrades; }
        })()
        """;
        var result=context.getClass().getMethod("evaluateString",Class.forName("dev.latvian.mods.rhino.Scriptable"),String.class,String.class,int.class,Object.class).invoke(context,scope,script,"SunlitShippingGameTest",1,null);
        h.assertTrue(Boolean.TRUE.equals(result),"Original shipping calculator integration did not pass");h.succeed();
    }
}
