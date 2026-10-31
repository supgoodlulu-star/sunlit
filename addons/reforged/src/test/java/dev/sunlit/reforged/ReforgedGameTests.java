package dev.sunlit.reforged;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import com.mojang.authlib.GameProfile;
import static dev.sunlit.reforged.Potential.*;
import dev.sunlit.reforged.Potential.Tier;

@GameTestHolder("sunlit_reforged")
@PrefixGameTestTemplate(false)
public final class ReforgedGameTests{
    static ServerPlayer player(GameTestHelper h){return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ReforgedTest"));}
    static Potential pot(Tier tier,Option option,double value){return new Potential(tier,List.of(new Line(option,tier,value),new Line(option,tier,value),new Line(option,tier,value)));}
    @GameTest(template="empty") public static void firstUseAllCubesCreatesRareThreeLines(GameTestHelper h){
        for(var cube:Cube.values()){var result=Potential.roll(Optional.empty(),Gear.WEAPON,cube,RandomSource.create(1),Rules.defaults());h.assertTrue(result.tier()==Tier.RARE&&result.lines().size()==3&&result.lines().stream().allMatch(l->l.tier()==Tier.RARE),"Initial use promoted or wrong line tier");}h.succeed();
    }
    @GameTest(template="empty") public static void gearWhitelistAndExactNineteenOptions(GameTestHelper h){
        h.assertTrue(Option.values().length==19,"Wrong option count");for(var item:List.of(Items.IRON_SWORD,Items.GOLDEN_PICKAXE,Items.WOODEN_HOE,Items.SHIELD,Items.BOW,Items.STONE_AXE))h.assertTrue(Potential.gear(new ItemStack(item)).isEmpty(),"Unapproved item allowed");
        h.assertTrue(Potential.gear(new ItemStack(Items.DIAMOND_AXE)).orElseThrow()==Gear.WEAPON,"Axe is not a weapon");h.assertTrue(Potential.gear(new ItemStack(Items.FISHING_ROD)).orElseThrow()==Gear.ROD,"Rod not supported");h.succeed();
    }
    @GameTest(template="empty") public static void duplicateLinesAndGearPools(GameTestHelper h){
        Rules r=Rules.defaults();Arrays.fill(r.weights,0);r.weights[Option.ATTACK_PERCENT.ordinal()]=1;
        var p=Potential.roll(Optional.empty(),Gear.WEAPON,Cube.MASTER,RandomSource.create(2),r);h.assertTrue(p.lines().stream().allMatch(l->l.option()==Option.ATTACK_PERCENT),"Three duplicate lines disallowed");
        for(var gear:Gear.values())for(int i=0;i<100;i++){var result=Potential.roll(Optional.of(pot(Tier.LEGENDARY,Option.DURABILITY,8)),gear,Cube.SUSPICIOUS,RandomSource.create(i),Rules.defaults());h.assertTrue(result.lines().stream().allMatch(l->l.option().gear.contains(gear)),"Wrong gear pool");}
        h.succeed();
    }
    @GameTest(template="empty") public static void tiersNeverDropAndCubeCeilings(GameTestHelper h){
        for(var cube:Cube.values())for(var tier:Tier.values())for(int i=0;i<400;i++){var old=pot(tier,Option.DURABILITY,8);var result=Potential.roll(Optional.of(old),Gear.WEAPON,cube,RandomSource.create(i),Rules.defaults());
            h.assertTrue(result.tier().ordinal()>=tier.ordinal(),"Tier dropped");h.assertTrue(result.lines().get(0).tier()==result.tier(),"First line differs from rolled tier");
            for(int j=1;j<3;j++)h.assertTrue(result.lines().get(j).tier()==result.tier()||(result.tier().ordinal()>0&&result.lines().get(j).tier().ordinal()==result.tier().ordinal()-1),"Invalid secondary line tier");
            if(cube==Cube.SUSPICIOUS&&tier.ordinal()>=1||cube==Cube.ARTISAN&&tier.ordinal()>=2)h.assertTrue(result.tier()==tier,"Cube exceeded promotion ceiling");
        }h.succeed();
    }
    @GameTest(template="empty") public static void masterKeepsOldLinesButPromotedGlobalTierAndRestoresPending(GameTestHelper h){
        ServerPlayer p=player(h);var m=new StationMenu(1,p.getInventory(),p);var gear=new ItemStack(Items.DIAMOND_SWORD);var original=pot(Tier.UNIQUE,Option.ATTACK_PERCENT,6);original.apply(gear);gear.getOrCreateTag().putString("ForeignPrefixProbe","unchanged");m.work.setItem(0,gear);m.work.setItem(1,new ItemStack(SunlitReforged.MASTER.get(),2));
        double chance=Rules.get().upgrades[2][2];Rules.get().upgrades[2][2]=100;
        try{int nonce=m.revision();h.assertTrue(m.act(nonce,"roll",0,0,""),"Master roll failed");h.assertTrue(!m.act(nonce,"roll",0,0,""),"Duplicate request consumed another cube");h.assertTrue(m.work.getItem(1).getCount()==1,"Wrong cube count");h.assertTrue(!m.getSlot(0).mayPickup(p)&&m.quickMoveStack(p,0).isEmpty(),"Pending item movable");
            var snapshot=Ledger.get(p.getServer()).save(new CompoundTag());var restored=new Ledger(snapshot).station(p.getUUID());h.assertTrue(restored.contains("pending",10)&&restored.getCompound("equipment").getCompound("tag").getCompound(Potential.KEY).getString("potentialTier").equals("LEGENDARY"),"Pending result or promotion not saved");
            var resumed=new StationMenu(2,p.getInventory(),p);String roll=resumed.saved.getCompound("pending").getString("rollId");h.assertTrue(resumed.choose(true,roll),"Could not choose old");var result=Potential.get(resumed.work.getItem(0)).orElseThrow();h.assertTrue(result.tier()==Tier.LEGENDARY&&result.lines().equals(original.lines()),"Old lines mutated or promotion lost");h.assertTrue(resumed.work.getItem(0).getTag().getString("ForeignPrefixProbe").equals("unchanged"),"Foreign data changed");h.assertTrue(!resumed.choose(false,roll),"Result applied twice");
        }finally{Rules.get().upgrades[2][2]=chance;}h.succeed();
    }
    @GameTest(template="empty") public static void attributeSwapDoesNotStackAndProbabilityIsSafe(GameTestHelper h){
        var p=player(h);var s=new ItemStack(Items.DIAMOND_SWORD);pot(Tier.RARE,Option.ATTACK_FLAT,1).apply(s);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,s);
        Effects.reconcile(p);double first=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);for(int i=0;i<20;i++)Effects.reconcile(p);h.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==first,"Attribute repeatedly stacked");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);Effects.reconcile(p);h.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)<first,"Modifier leaked after unequip");
        h.assertTrue(Effects.chance(400)==1&&Effects.chance(-10)==0,"Probability unsafe");h.succeed();
    }
    @GameTest(template="empty") public static void actualDurabilityMixinPreventsDamage(GameTestHelper h){
        var p=player(h);var stack=new ItemStack(Items.DIAMOND_PICKAXE);pot(Tier.LEGENDARY,Option.DURABILITY,100).apply(stack);for(int i=0;i<20;i++)stack.hurt(10,RandomSource.create(i),p);h.assertTrue(stack.getDamageValue()==0,"Production durability mixin not applied");h.succeed();
    }
    @GameTest(template="empty") public static void rewardLedgerAndNBTCodecSurviveRestart(GameTestHelper h){
        UUID owner=UUID.randomUUID();var ledger=new Ledger();h.assertTrue(ledger.reward(owner,"daily:2026-10-31:0"),"First reward missing");var restored=new Ledger(ledger.save(new CompoundTag()));h.assertTrue(!restored.reward(owner,"daily:2026-10-31:0"),"Reward replay after restart");
        var s=new ItemStack(Items.NETHERITE_CHESTPLATE);pot(Tier.EPIC,Option.HEALTH_FLAT,2).apply(s);s.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION,4);s.setDamageValue(15);var decoded=ItemStack.of(s.save(new CompoundTag()));h.assertTrue(ItemStack.matches(s,decoded)&&Potential.get(s).equals(Potential.get(decoded)),"Item data lost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void probabilitySimulationMatchesConfiguredUpgrade(GameTestHelper h){
        var r=Rules.defaults();var rng=RandomSource.create(0x51a71);int n=50000,up=0,upper=0,secondary=0;var old=pot(Tier.RARE,Option.ATTACK_FLAT,1);
        for(int i=0;i<n;i++){var result=Potential.roll(Optional.of(old),Gear.WEAPON,Cube.SUSPICIOUS,rng,r);if(result.tier()==Tier.EPIC){up++;for(int j=1;j<3;j++){secondary++;if(result.lines().get(j).tier()==Tier.EPIC)upper++;}}}
        h.assertTrue(Math.abs(up/(double)n-.08)<.008,"Upgrade probability biased");h.assertTrue(Math.abs(upper/(double)secondary-.20)<.035,"Secondary grade biased");h.succeed();
    }
    @GameTest(template="empty") public static void firstMasterConsumesOneWithoutChoiceAndBadGearDoesNotConsume(GameTestHelper h){
        var p=player(h);var m=new StationMenu(1,p.getInventory(),p);m.work.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));m.work.setItem(1,new ItemStack(SunlitReforged.MASTER.get(),2));
        h.assertTrue(m.roll()&&!m.pending()&&m.work.getItem(1).getCount()==1&&Potential.get(m.work.getItem(0)).orElseThrow().tier()==Tier.RARE,"First master use wrong");
        m.work.setItem(0,new ItemStack(Items.IRON_PICKAXE));h.assertTrue(!m.roll()&&m.work.getItem(1).getCount()==1,"Unsupported item consumed cube");h.succeed();
    }
    @GameTest(template="empty") public static void vanillaNetheriteUpgradePreservesAllNBT(GameTestHelper h){
        var source=new ItemStack(Items.DIAMOND_SWORD);pot(Tier.UNIQUE,Option.ATTACK_FLAT,3).apply(source);source.getOrCreateTag().putString("ForeignPrefixProbe","opaque");source.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
        var input=new net.minecraft.world.SimpleContainer(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),source,new ItemStack(Items.NETHERITE_INGOT));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING,input,h.getLevel()).orElseThrow();
        var output=recipe.assemble(input,h.getLevel().registryAccess());h.assertTrue(output.is(Items.NETHERITE_SWORD)&&output.getTag().equals(source.getTag()),"Vanilla upgrade lost NBT");h.succeed();
    }
    @GameTest(template="empty") public static void xpFractionsAndGrantTimeSkillsHook(GameTestHelper h)throws Exception{
        var p=player(h);int sum=0;for(int i=0;i<100;i++)sum+=Skills.calculate(p,"test:small",1,i,2);
        h.assertTrue(sum==102,"Small XP grants lost fractional bonus");h.assertTrue(Skills.calculate(p,"test:limit",10,Integer.MAX_VALUE-2,100)==2,"Upstream int XP overflow");
        if(Boolean.getBoolean("sunlit.integrationTests")){
            var skills=Class.forName("net.puffish.skillsmod.SkillsMod");var handler=Arrays.stream(skills.getDeclaredMethods()).filter(m->m.getName().contains("sunlit$xp")).findFirst().orElseThrow();handler.setAccessible(true);
            var actual=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"SkillsLive"));var tool=new ItemStack(Items.DIAMOND_PICKAXE);pot(Tier.RARE,Option.LIFE_XP,2).apply(tool);actual.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
            var catClass=Class.forName("net.puffish.skillsmod.config.CategoryConfig");var constructor=catClass.getConstructors()[0];Object[] args=new Object[constructor.getParameterCount()];args[0]=new net.minecraft.resources.ResourceLocation("society","mining");args[args.length-1]=Optional.empty();var category=constructor.newInstance(args);
            var dataClass=Class.forName("net.puffish.skillsmod.server.data.CategoryData");var data=dataClass.getMethod("read",CompoundTag.class).invoke(null,new CompoundTag());var instance=skills.getMethod("getInstance").invoke(null);
            h.assertTrue(((Number)handler.invoke(instance,100,actual,category,null,data,100)).intValue()==106,"Real Skills grant hook did not add exactly 6 percent");
        }h.succeed();
    }
    @GameTest(template="empty") public static void realNumismaticsPurchaseAndReplaySafety(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.economyTests")){h.succeed();return;}
        var p=player(h);var bank=Class.forName("dev.ithundxr.createnumismatics.Numismatics").getField("BANK").get(null);var type=Class.forName("dev.ithundxr.createnumismatics.content.backend.BankAccount$Type");
        Object account=bank.getClass().getMethod("getOrCreateAccount",UUID.class,type).invoke(bank,p.getUUID(),type.getField("PLAYER").get(null));account.getClass().getMethod("setBalance",int.class).invoke(account,4096*20000);
        long initial=Economy.view(p).getLong("spurs");h.assertTrue(!Economy.buy(p,3,1)&&Economy.view(p).getLong("spurs")==initial,"Removed product accepted or charged");var menu=new StationMenu(1,p.getInventory(),p);int nonce=menu.revision();long before=Economy.view(p).getLong("spurs");h.assertTrue(menu.act(nonce,"buy",0,1,""),"Real bank purchase failed");h.assertTrue(!menu.act(nonce,"buy",0,1,""),"Purchase request replay accepted");
        h.assertTrue(before-Economy.view(p).getLong("spurs")==500L,"Wrong debit");h.assertTrue(p.getInventory().countItem(SunlitReforged.SUSPICIOUS.get())==1,"Cube not delivered exactly once");
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));long full=Economy.view(p).getLong("spurs");h.assertTrue(!Economy.buy(p,0,1)&&Economy.view(p).getLong("spurs")==full,"Full inventory charged account");
        p.getInventory().clearContent();account.getClass().getMethod("setBalance",int.class).invoke(account,4096);
        h.assertTrue(Economy.view(p).getLong("unit")==1&&Economy.buy(p,0,1)&&Economy.view(p).getLong("spurs")==3596,"One iridium must buy a 500 SUN dreamdrop and leave 3596 SUN");
        h.assertTrue(Economy.buy(p,1,1)&&Economy.view(p).getLong("spurs")==1096&&!Economy.buy(p,2,1)&&Economy.view(p).getLong("spurs")==1096,"2500/10000 total-value SUN pricing wrong");
        if(Boolean.getBoolean("sunlit.journalTests")){var result=(Optional<?>)Class.forName("dev.sunlit.journal.Integrations").getMethod("balance",UUID.class).invoke(null,p.getUUID());var v=result.orElseThrow();h.assertTrue(((Number)v.getClass().getMethod("perSun").invoke(v)).longValue()==1&&((Number)v.getClass().getMethod("spurs").invoke(v)).longValue()==1096,"Journal and shop SUN units differ");}
        p.getInventory().clearContent();account.getClass().getMethod("setBalance",int.class).invoke(account,1);h.assertTrue(!Economy.buy(p,0,1)&&Economy.view(p).getLong("spurs")==1,"Insufficient balance was wiped");h.succeed();
    }

    @GameTest(template="empty") public static void actualJournalEventBridgeAwardsOnce(GameTestHelper h)throws Exception{
        if(!Boolean.getBoolean("sunlit.journalTests")){h.succeed();return;}
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"ReforgedLive"));var type=Class.forName("dev.sunlit.journal.api.JournalRewardEvent");var constructor=type.getConstructor(ServerPlayer.class,String.class,String.class);
        double chance=Rules.get().rewardRates[3];Rules.get().rewardRates[3]=100;
        try{net.minecraftforge.common.MinecraftForge.EVENT_BUS.post((net.minecraftforge.eventbus.api.Event)constructor.newInstance(p,"daily","test:once"));net.minecraftforge.common.MinecraftForge.EVENT_BUS.post((net.minecraftforge.eventbus.api.Event)constructor.newInstance(p,"daily","test:once"));h.assertTrue(p.getInventory().countItem(SunlitReforged.SUSPICIOUS.get())==1,"Journal event replay or bridge missing");}
        finally{Rules.get().rewardRates[3]=chance;}h.succeed();
    }
    @GameTest(template="empty") public static void actualVanillaTreasureTableCanAwardCube(GameTestHelper h){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"ReforgedLive"));var hook=new net.minecraft.world.entity.projectile.FishingHook(p,h.getLevel(),0,0);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p.position()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD)).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,hook).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.FISHING);
        double chance=Rules.get().rewardRates[2];Rules.get().rewardRates[2]=100;
        try{var table=p.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("minecraft","gameplay/fishing/treasure"));var loot=table.getRandomItems(params);h.assertTrue(loot.stream().anyMatch(s->s.is(SunlitReforged.SUSPICIOUS.get())),"Actual treasure table was not traced");}finally{Rules.get().rewardRates[2]=chance;hook.discard();}h.succeed();
    }

}
