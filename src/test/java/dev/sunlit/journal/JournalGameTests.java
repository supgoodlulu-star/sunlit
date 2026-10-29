package dev.sunlit.journal;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import java.util.*;

@GameTestHolder(SunlitJournal.MODID)
@PrefixGameTestTemplate(false)
public class JournalGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SunlitTest"));
    }
    @GameTest(template="empty") public static void playerPersistence(GameTestHelper h){
        var a=UUID.randomUUID();var b=UUID.randomUUID();var w=new JournalWorld();
        w.player(a).putLong("privateCount",47);w.player(b).putLong("privateCount",3);
        var loaded=new JournalWorld(w.save(new CompoundTag()));
        h.assertTrue(loaded.player(a).getLong("privateCount")==47,"Player A data lost");
        h.assertTrue(loaded.player(b).getLong("privateCount")==3,"Player data mixed");
        loaded.player(a).putLong("privateCount",99);
        h.assertTrue(w.player(a).getLong("privateCount")==47,"Deserialization aliases original NBT");h.succeed();
    }
    @GameTest(template="empty") public static void duplicateAndInvalidClaims(GameTestHelper h){
        var p=player(h);var w=new JournalWorld();w.refreshTasks(p);
        var daily=w.player(p).getCompound("daily");var task=daily.getCompound("0");
        h.assertFalse(w.claim(p,"daily",0),"Incomplete task accepted");
        task.putInt("progress",task.getInt("target"));
        var coin=TaskRewards.stack(TaskRewards.COIN,1).getItem();
        int before=p.getInventory().countItem(coin);
        h.assertTrue(w.claim(p,"daily",0),"Completed task rejected");
        int after=p.getInventory().countItem(coin);
        h.assertTrue(after-before==2,"Wrong reward amount");
        h.assertFalse(w.claim(p,"daily",0),"Duplicate reward accepted");
        h.assertFalse(w.claim(p,"daily",-1),"Negative index accepted");
        h.assertFalse(w.claim(p,"weekly",3),"Out-of-range index accepted");
        h.assertFalse(w.claim(p,"other",0),"Unknown period accepted");
        h.assertTrue(p.getInventory().countItem(coin)==after,"Invalid claims changed inventory");
        var restored=new JournalWorld(w.save(new CompoundTag()));
        h.assertFalse(restored.claim(p,"daily",0),"Reload permitted duplicate reward");h.succeed();
    }
    @GameTest(template="empty") public static void calendarAndIsolation(GameTestHelper h){
        var a=player(h);var b=player(h);var w=new JournalWorld();
        w.refreshTasks(a);w.refreshTasks(b);
        var saved=w.player(a).getCompound("daily").copy();w.refreshTasks(a);
        h.assertTrue(saved.equals(w.player(a).getCompound("daily")),"Refresh rerolled current tasks");
        var future=w.player(a).getCompound("daily");future.putString("date","9999-12-31");future.getCompound("0").putBoolean("claimed",true);
        w.refreshTasks(a);
        h.assertTrue(w.player(a).getCompound("daily").getCompound("0").getBoolean("claimed"),"Clock rollback reopened reward");
        h.assertFalse(w.player(b).getCompound("daily").getCompound("0").getBoolean("claimed"),"Claim flag leaked to another UUID");
        var old=w.player(a).getCompound("weekly");old.putString("date","2000-01-01");old.getCompound("0").putBoolean("claimed",true);
        w.refreshTasks(a);h.assertFalse(w.player(a).getCompound("weekly").getCompound("0").getBoolean("claimed"),"New week did not reset flags");h.succeed();
    }
    @GameTest(template="empty") public static void activityAndLegacyTitles(GameTestHelper h){
        var a=player(h);var b=player(h);a.setGameMode(GameType.SURVIVAL);
        var w=new JournalWorld();w.refreshTasks(a);
        var task=w.player(a).getCompound("daily").getCompound("0");int kind=task.getInt("kind");
        w.activity(a,kind,10);
        h.assertTrue(w.player(a).getCompound("counts").getLong(JournalWorld.ACTIVITIES[kind])==10,"Activity not recorded");
        h.assertTrue(w.player(b).getCompound("counts").isEmpty(),"Activity credited wrong UUID");
        h.assertTrue(SunlitJournal.owned(a).contains(SunlitJournal.TITLES[kind*4]),"Milestone title not granted");
        var titles=SunlitJournal.data(a);titles.putString("equipped",SunlitJournal.TITLES[kind*4]);
        SunlitJournal.copyTitles(a,b);h.assertTrue(SunlitJournal.data(a).equals(SunlitJournal.data(b)),"Legacy NBT clone failed");
        SunlitJournal.data(b).remove("equipped");h.assertTrue(!SunlitJournal.data(a).getString("equipped").isEmpty(),"Clone aliases old NBT");
        a.setGameMode(GameType.CREATIVE);w.activity(a,kind,20);
        h.assertTrue(w.player(a).getCompound("counts").getLong(JournalWorld.ACTIVITIES[kind])==10,"Creative activity counted");h.succeed();
    }
    @GameTest(template="empty") public static void requestBoundaries(GameTestHelper h){
        var buf=new FriendlyByteBuf(Unpooled.buffer());
        try{
            var req=new JournalNetwork.Request("equip","사계요정");JournalNetwork.Request.encode(req,buf);
            h.assertTrue(req.equals(JournalNetwork.Request.decode(buf)),"Request Unicode round trip failed");
            boolean rejected=false;
            try{JournalNetwork.Request.encode(new JournalNetwork.Request("nick","x".repeat(257)),buf);}catch(RuntimeException e){rejected=true;}
            h.assertTrue(rejected,"Oversized nickname accepted by codec");
        }finally{buf.release();}h.succeed();
    }
    @GameTest(template="empty") public static void placedOrePersistence(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));var w=new JournalWorld();
        w.placedOre(p,pos);var restored=new JournalWorld(w.save(new CompoundTag()));
        h.assertTrue(restored.consumePlacedOre(p,pos),"Placed ore marker lost on reload");
        h.assertFalse(restored.consumePlacedOre(p,pos),"Marker consumed more than once");h.succeed();
    }


    @GameTest(template="empty") public static void festivalSharedRewardsAndSeasons(GameTestHelper h){
        var a=player(h);var b=player(h);var w=new JournalWorld();int seasonIndex=0;
        for(String season:List.of("SPRING","SUMMER","AUTUMN","WINTER")){
            w.setSeason(new Integrations.Season(season,++seasonIndex*100,400));
            var raw=w.save(new CompoundTag());raw.getCompound("festival").putLong("goal",3);
            w=new JournalWorld(raw);
            w.contribute(a,1);w.contribute(b,2);
            var completed=w.save(new CompoundTag()).getCompound("festival");
            h.assertTrue(completed.getBoolean("complete"),"Shared contributions did not complete festival");
            h.assertTrue(completed.getCompound("contributions").getLong(a.getUUID().toString())==1,"Personal contribution lost");
            h.assertTrue(completed.getCompound("contributions").getLong(b.getUUID().toString())==2,"Shared contribution attributed incorrectly");
            w.contribute(a,100); // Completed seasons cannot issue more rewards.
            h.assertTrue(w.player(a).getInt("festivalRewards")==1,"Duplicate festival reward queued");
            var reloaded=new JournalWorld(w.save(new CompoundTag()));
            reloaded.achievements(a);reloaded.achievements(b);
            h.assertTrue(SunlitJournal.owned(a).contains(JournalWorld.seasonTitle(season)),"Season title not granted after reload");
            h.assertTrue(reloaded.player(a).getInt("festivalRewards")==0,"Reward remained claimable");
            w=reloaded;
        }
        h.assertTrue(SunlitJournal.owned(a).contains("사계요정"),"Four-season title missing");
        h.assertTrue(SunlitJournal.owned(b).contains("사계요정"),"Second contributor missing four-season title");
        h.assertTrue(a.getInventory().countItem(Items.EMERALD)==64,"Festival reward count incorrect");h.succeed();
    }

    @GameTest(template="empty") public static void legacyPlayerSaveReload(GameTestHelper h){
        var old=player(h);var restored=player(h);
        SunlitJournal.data(old).putString("owned","새싹농부|사계요정");
        SunlitJournal.data(old).putString("equipped","사계요정");
        var serialized=old.saveWithoutId(new CompoundTag());restored.load(serialized);
        h.assertTrue(SunlitJournal.owned(restored).containsAll(Set.of("새싹농부","사계요정")),"Legacy titles lost on full player save/load");
        h.assertTrue(SunlitJournal.data(restored).getString("equipped").equals("사계요정"),"Equipped title lost on save/load");
        var command=h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("sunlit");
        h.assertTrue(command!=null,"Sunlit command not registered");
        for(String name:List.of("titles","equip","unequip","grant","revoke","journal"))
            h.assertTrue(command.getChild(name)!=null,"Missing legacy command: "+name);
        h.succeed();
    }
    @GameTest(template="empty") public static void concreteTaskMatchingAndReload(GameTestHelper h){
        var p=player(h);p.setGameMode(GameType.SURVIVAL);var w=new JournalWorld();w.refreshTasks(p);
        var task=w.player(p).getCompound("daily").getCompound("0");
        task.putInt("kind",2);task.putString("subject","minecraft:iron_ore");task.putInt("target",6);task.putInt("progress",0);
        w.activity(p,2,3,"minecraft:coal_ore");
        h.assertTrue(task.getInt("progress")==0,"Unrelated ore advanced iron task");
        w.activity(p,3,1,"minecraft:iron_ore");
        h.assertTrue(task.getInt("progress")==0,"Wrong activity advanced ore task");
        w.activity(p,2,2,"minecraft:iron_ore");
        h.assertTrue(task.getInt("progress")==2,"Matching ore did not advance task");
        var loaded=new JournalWorld(w.save(new CompoundTag()));
        loaded.activity(p,2,100,"minecraft:iron_ore");
        h.assertTrue(loaded.player(p).getCompound("daily").getCompound("0").getInt("progress")==6,"Reload lost target or exceeded cap");
        h.assertTrue(loaded.claim(p,"daily",0),"Specific task reward rejected");
        h.assertFalse(loaded.claim(p,"daily",0),"Specific task paid twice");h.succeed();
    }
    @GameTest(template="empty") public static void concreteCookingAndLegacyMigration(GameTestHelper h){
        var p=player(h);p.setGameMode(GameType.SURVIVAL);var w=new JournalWorld();w.refreshTasks(p);
        var tasks=w.player(p).getCompound("daily");
        var untouched=tasks.getCompound("0");untouched.remove("subject");untouched.putInt("progress",0);
        var started=tasks.getCompound("1");started.remove("subject");started.putInt("progress",2);
        var claimed=tasks.getCompound("2");claimed.remove("subject");claimed.putBoolean("claimed",true);
        w.refreshTasks(p);
        h.assertTrue(untouched.contains("subject"),"Untouched task did not receive concrete target");
        h.assertFalse(started.contains("subject"),"Migration changed a task with earned progress");
        h.assertTrue(started.getInt("progress")==2,"Migration erased progress");
        h.assertTrue(claimed.getBoolean("claimed"),"Migration reopened claimed reward");
        untouched.putInt("kind",3);untouched.putString("subject","minecraft:bread");untouched.putInt("target",6);
        w.activity(p,3,2,"minecraft:baked_potato");
        h.assertTrue(untouched.getInt("progress")==0,"Different food advanced bread task");
        w.activity(p,3,3,"minecraft:bread");
        h.assertTrue(untouched.getInt("progress")==3,"Bread did not advance cooking task");h.succeed();
    }
    @GameTest(template="empty") public static void oreEventUsesConcreteSubject(GameTestHelper h){
        var p=player(h);p.setGameMode(GameType.SURVIVAL);var w=JournalWorld.get(p.getServer());w.refreshTasks(p);
        var task=w.player(p).getCompound("daily").getCompound("0");
        task.putInt("kind",2);task.putString("subject","minecraft:iron_ore");task.putInt("target",6);task.putInt("progress",0);
        var events=new JournalEvents();var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        events.harvest(new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,net.minecraft.world.level.block.Blocks.COAL_ORE.defaultBlockState(),p));
        h.assertTrue(task.getInt("progress")==0,"Coal break event advanced iron task");
        events.harvest(new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(),p));
        h.assertTrue(task.getInt("progress")==1,"Iron break event missed task");
        w.placedOre(p,pos);
        events.harvest(new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(),p));
        h.assertTrue(task.getInt("progress")==1,"Placed iron ore advanced task");h.succeed();
    }

    @GameTest(template="empty") public static void disabledCropTasksPreserveEarnedProgress(GameTestHelper h){
        var p=player(h);var w=new JournalWorld();w.refreshTasks(p);
        var tasks=w.player(p).getCompound("daily");
        for(int i=0;i<3;i++){var t=tasks.getCompound(""+i);t.putInt("kind",0);t.putString("subject","minecraft:carrots");t.putInt("target",13);t.putInt("progress",i==1?2:0);t.putBoolean("claimed",i==2);}
        w.refreshTasks(p);
        h.assertFalse(tasks.getCompound("0").getString("subject").equals("minecraft:carrots"),"Disabled unstarted carrot task was not replaced");
        h.assertTrue(tasks.getCompound("0").getInt("target")==13,"Migration changed goal");
        h.assertTrue(tasks.getCompound("1").getString("subject").equals("minecraft:carrots")&&tasks.getCompound("1").getInt("progress")==2,"Migration changed earned progress");
        h.assertTrue(tasks.getCompound("2").getBoolean("claimed"),"Migration reopened claimed task");
        for(int i=0;i<20;i++)h.assertFalse(TaskTargets.choose(0).subject().equals("minecraft:carrots"),"Disabled crop was generated");h.succeed();
    }

    @GameTest(template="empty") public static void titleChatOwnershipAndStyles(GameTestHelper h){
        var p=player(h);var d=SunlitJournal.data(p);
        d.putString("equipped","새싹농부");
        h.assertTrue(JournalChat.format(p,net.minecraft.network.chat.Component.literal("안녕")).isEmpty(),"Unowned title appeared in chat");
        d.putString("owned","새싹농부|별빛셰프");
        var body=net.minecraft.network.chat.Component.literal("안녕하세요").withStyle(net.minecraft.ChatFormatting.AQUA);
        var line=JournalChat.format(p,body).orElseThrow();
        h.assertTrue(line.getString().equals("[새싹농부] "+p.getDisplayName().getString()+": 안녕하세요"),"Title/name/body order incorrect");
        h.assertTrue(line.getSiblings().get(0).getStyle().getColor().getValue()==TitleThemes.color("새싹농부",true),"Chat title color missing");
        h.assertTrue(line.getSiblings().get(line.getSiblings().size()-1).getStyle().equals(body.getStyle()),"Title theme leaked into message body");
        h.assertTrue(body.getString().equals("안녕하세요"),"Formatting mutated original message");
        d.putString("equipped","별빛셰프");
        h.assertTrue(JournalChat.format(p,body).orElseThrow().getString().startsWith("[별빛셰프]"),"New equipped title not used immediately");
        d.remove("equipped");h.assertTrue(JournalChat.format(p,body).isEmpty(),"Unequipped title still displayed");h.succeed();
    }

    @GameTest(template="empty") public static void retiredSkyCannotEnter(GameTestHelper h){
        var p=player(h);var home=p.position();SkyGarden.travel(p);
        h.assertFalse(SkyGarden.inside(p),"Retired map still accessible");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(SkyContent.COMPASS.get()));
        SkyContent.COMPASS.get().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.position().equals(home),"Legacy compass still teleports");h.succeed();
    }

    @GameTest(template="empty") public static void legacySkyReturnPreservesUnlocks(GameTestHelper h){
        var p=player(h);var state=SkyGarden.player(p);state.putBoolean("complete",true);state.putInt("records",7);
        var back=new CompoundTag();back.putString("dimension","minecraft:overworld");back.putDouble("x",p.getX());back.putDouble("y",p.getY());back.putDouble("z",p.getZ());state.put("return",back);
        var oldKey=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,new net.minecraft.resources.ResourceLocation(SunlitJournal.MODID,"sky_garden"));
        var old=p.getServer().getLevel(oldKey);h.assertTrue(old!=null,"Legacy dimension removed");
        p.teleportTo(old,.5,151,16.5,0,0);p.moveTo(.5,151,16.5,0,0);
        SkyGarden.interact(p,new net.minecraft.core.BlockPos(0,151,14),6);
        h.assertFalse(SkyGarden.inside(p),"Legacy return stone stranded player");
        h.assertTrue(SkyGarden.player(p).getBoolean("complete")&&SkyGarden.player(p).getInt("records")==7,"Upgrade reset personal rewards");h.succeed();
    }

    @GameTest(template="empty") public static void grandSkyReturnAfterCitadelUpgrade(GameTestHelper h){
        var p=player(h);var state=SkyGarden.player(p);state.putBoolean("complete",true);state.putInt("records",7);
        var back=new CompoundTag();back.putString("dimension","minecraft:overworld");back.putDouble("x",p.getX());back.putDouble("y",p.getY());back.putDouble("z",p.getZ());state.put("return",back);
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,new net.minecraft.resources.ResourceLocation(SunlitJournal.MODID,"sky_garden_grand"));
        var old=p.getServer().getLevel(key);h.assertTrue(old!=null,"Grand garden dimension was removed");
        p.teleportTo(old,.5,151,33.5,0,0);p.moveTo(.5,151,33.5,0,0);SkyGarden.interact(p,new net.minecraft.core.BlockPos(0,151,31),6);
        h.assertFalse(SkyGarden.inside(p),"Grand garden return stopped working");h.assertTrue(SkyGarden.player(p).getBoolean("complete"),"Grand garden upgrade erased unlock");h.succeed();
    }

    @net.minecraft.gametest.framework.GameTestGenerator
    public static Collection<net.minecraft.gametest.framework.TestFunction> integrationChecks(){
        if(!Boolean.getBoolean("sunlit.integrationTests"))return List.of();
        return List.of(
            integration("nicknameintegration",h->{
                var p=player(h);
                try{
                    Integrations.nick(p,"햇살",false);
                    h.assertTrue("햇살".equals(Integrations.nick(p)),"Server Nick read/write did not round trip");
                    Integrations.nick(p,"별빛",false);
                    h.assertTrue("별빛".equals(Integrations.nick(p)),"Second rename rejected");
                    boolean rejected=false;
                    try{Integrations.nick(p,"x".repeat(128),false);}catch(IllegalArgumentException e){rejected=true;}
                    h.assertTrue(rejected,"Nickname validation accepted an oversized input");
                    h.assertTrue("별빛".equals(Integrations.nick(p)),"Invalid input mutated nickname");
                    Integrations.nick(p,"",true);h.assertTrue(Integrations.nick(p).isEmpty(),"Reset did not clear nickname");
                }catch(ReflectiveOperationException e){throw new RuntimeException(e);}h.succeed();
            }),
            integration("titlechatnicknameintegration",h->{
                var p=player(h);var d=SunlitJournal.data(p);d.putString("owned","새싹농부");d.putString("equipped","새싹농부");
                try{
                    Integrations.nick(p,"햇살테스트",false);
                    var formatted=JournalChat.format(p,net.minecraft.network.chat.Component.literal("안녕!")).orElseThrow();
                    h.assertTrue(formatted.getString().equals("[새싹농부] 햇살테스트: 안녕!"),"Actual nickname not retained in themed chat");
                    var event=new net.minecraftforge.event.ServerChatEvent(p,"안녕!",net.minecraft.network.chat.Component.literal("안녕!"));
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                    h.assertTrue(event.isCanceled(),"Titled chat fell through to duplicate nickname handler");
                }catch(ReflectiveOperationException e){throw new RuntimeException(e);}h.succeed();
            }),
            integration("skillsapiintegration",h->{
                var p=player(h);var snapshot=Integrations.skills(p);
                h.assertTrue(snapshot.getBoolean("available"),"Installed Skills API was not readable");
                h.assertTrue(snapshot.contains("categories",Tag.TAG_LIST),"Missing actual category list");
                h.assertTrue(!snapshot.contains("error"),"Skills read reported an error");h.succeed();
            }),
            integration("patchoulibookregistration",h->{
                try{
                    var c=Class.forName("vazkii.patchouli.common.book.BookRegistry");
                    Object registry=c.getField("INSTANCE").get(null);
                    var books=(Map<?,?>)c.getField("books").get(registry);
                    h.assertTrue(books.containsKey(new net.minecraft.resources.ResourceLocation(SunlitJournal.MODID,"sunlit_journal")),"Patchouli did not register the journal book");
                }catch(ReflectiveOperationException e){throw new RuntimeException(e);}h.succeed();
            })
        );
    }
    private static net.minecraft.gametest.framework.TestFunction integration(String name,java.util.function.Consumer<GameTestHelper> test){
        return new net.minecraft.gametest.framework.TestFunction("sunlit_integrations",name,"sunlit_journal_core:empty",100,0L,true,test);
    }


    @GameTest(template="empty") public static void agreedRewardPools(GameTestHelper h){
        String[][] ids={{"dew_drop_farmland_growth:low_quality_fertilizer","dew_drop_farmland_growth:weak_fertilizer","dew_drop_farmland_growth:hydrating_fertilizer"},{"aquaculture:worm","crabbersdelight:crab_trap_bait"},{"minecraft:coal","minecraft:copper_ingot","minecraft:iron_ingot"},{"minecraft:sugar","meadow:alpine_salt"},{"society:animal_feed","society:animal_feed","society:animal_feed"}};
        int[][] counts={{12,12,14},{18,14},{64,64,64},{18,18},{18,22,26}};
        for(int kind=0;kind<5;kind++)for(int i=0;i<ids[kind].length;i++)for(boolean weekly:new boolean[]{false,true}){
            var plan=TaskRewards.plan(kind,weekly,i);
            h.assertTrue(plan.getString("item").equals(ids[kind][i]),"Reward ID differs from agreed pool");
            h.assertTrue(plan.getInt("count")== (weekly?(kind==2?96:counts[kind][i]*3):counts[kind][i]),"Reward count differs from agreement");
            h.assertTrue(plan.getInt("coins")== (weekly?6:2)&&plan.getInt("xp")== (weekly?80:20),"Money or XP incorrect");
        }h.succeed();
    }
    @GameTest(template="empty") public static void fiveFieldsStableRewardsWithoutBonus(GameTestHelper h){
        var p=player(h);p.getInventory().clearContent();var w=new JournalWorld();w.refreshTasks(p);var daily=w.player(p).getCompound("daily");var kinds=new java.util.HashSet<Integer>();
        for(int i=0;i<5;i++)kinds.add(daily.getCompound(""+i).getInt("kind"));h.assertTrue(kinds.size()==5,"Daily does not cover five fields");
        var initial=daily.copy();w.refreshTasks(p);h.assertTrue(initial.equals(daily),"Refresh rerolled reward");
        var loaded=new JournalWorld(w.save(new CompoundTag()));loaded.refreshTasks(p);h.assertTrue(initial.equals(loaded.player(p).getCompound("daily")),"Restart rerolled reward");
        for(int i:new int[]{1,3,4}){var task=daily.getCompound(""+i);task.putInt("progress",task.getInt("target"));}
        w.refreshTasks(p);h.assertTrue(p.getInventory().countItem(SkyContent.FRAGMENT.get())==0,"Removed bonus issued a fragment");
        for(int i:new int[]{0,2}){var task=daily.getCompound(""+i);task.putInt("progress",task.getInt("target"));}
        w.refreshTasks(p);h.assertTrue(p.getInventory().countItem(SkyContent.FRAGMENT.get())==0,"Removed bonus issued a fragment");
        h.assertTrue(w.claim(p,"daily",4),"Fifth task cannot be claimed");h.assertFalse(w.claim(p,"daily",5),"Sixth index accepted");h.succeed();
    }
    @GameTest(template="empty") public static void rewardMigrationPreservesProgress(GameTestHelper h){
        var p=player(h);var w=new JournalWorld();w.refreshTasks(p);var daily=w.player(p).getCompound("daily");daily.remove("3");daily.remove("4");daily.putBoolean("setBonusAwarded",true);
        for(int i=0;i<3;i++){daily.getCompound(""+i).remove("payout");daily.getCompound(""+i).putInt("reward",2);}
        daily.getCompound("0").putInt("progress",2);daily.getCompound("1").putBoolean("claimed",true);
        String target=daily.getCompound("0").getString("subject");w.refreshTasks(p);
        h.assertTrue(daily.getCompound("0").getInt("progress")==2&&daily.getCompound("0").getString("subject").equals(target),"Migration reset progress/target");
        h.assertTrue(daily.getCompound("1").getBoolean("claimed")&&daily.getCompound("1").getCompound("payout").getBoolean("legacy"),"Migration reopened old claim");
        h.assertTrue(daily.contains("4",10)&&daily.getBoolean("setBonusAwarded"),"Migration lost slot or bonus flag");
        var snapshot=daily.copy();w.refreshTasks(p);h.assertTrue(snapshot.equals(daily),"Migration repeated/rerolled");h.succeed();
    }
    @GameTest(template="empty") public static void missingRewardDoesNotPartiallyPay(GameTestHelper h){
        var p=player(h);p.getInventory().clearContent();var w=new JournalWorld();w.refreshTasks(p);var task=w.player(p).getCompound("daily").getCompound("0");task.putInt("progress",task.getInt("target"));
        task.getCompound("payout").putString("item","sunlit_missing:unavailable");var saved=task.copy();int xp=p.totalExperience;
        h.assertFalse(w.claim(p,"daily",0),"Unavailable pack item paid");h.assertFalse(task.getBoolean("claimed"),"Unavailable reward consumed claim");
        h.assertTrue(p.totalExperience==xp&&p.getInventory().isEmpty(),"Unavailable item allowed partial coins/XP");h.assertTrue(saved.equals(task),"Unavailable reward was rerolled");h.succeed();
    }
    @GameTest(template="empty") public static void weeklyMiningSplitsStacks(GameTestHelper h){
        var p=player(h);p.getInventory().clearContent();var w=new JournalWorld();w.refreshTasks(p);var task=w.player(p).getCompound("weekly").getCompound("0");
        task.putInt("kind",2);task.put("payout",TaskRewards.plan(2,true,2));task.putInt("progress",task.getInt("target"));int xp=p.totalExperience;
        h.assertTrue(w.claim(p,"weekly",0),"Weekly mining claim failed");h.assertTrue(p.getInventory().countItem(Items.IRON_INGOT)==96,"Weekly mining not 96");
        h.assertTrue(p.getInventory().countItem(TaskRewards.stack(TaskRewards.COIN,1).getItem())==6&&p.totalExperience==xp+80,"Weekly money/XP incorrect");
        for(var stack:p.getInventory().items)h.assertTrue(stack.getCount()<=stack.getMaxStackSize(),"Oversized inventory stack");
        h.assertFalse(w.claim(p,"weekly",0),"Weekly claim duplicated");h.succeed();
    }
    @GameTest(template="empty") public static void retiredDataAndItemsRemainInert(GameTestHelper h){
        var p=player(h);p.getInventory().clearContent();var w=JournalWorld.get(p.getServer());w.refreshTasks(p);
        var legacy=new CompoundTag();legacy.putInt("dust",37);var owned=new CompoundTag();owned.putBoolean("sakura_sword",true);legacy.put("owned",owned);w.player(p).put("fantasyGacha",legacy.copy());
        SunlitJournal.data(p).putString("owned","새싹농부|하늘 정원의 수호자");SunlitJournal.data(p).putString("equipped","하늘 정원의 수호자");
        for(String period:List.of("daily","weekly")){var tasks=w.player(p).getCompound(period);for(int n=0;n<JournalWorld.taskCount(period);n++){var t=tasks.getCompound(""+n);t.putInt("progress",t.getInt("target"));}}
        p.getInventory().add(new net.minecraft.world.item.ItemStack(SkyContent.TICKET.get(),4));p.getInventory().add(new net.minecraft.world.item.ItemStack(SkyContent.FRAGMENT.get(),12));
        var before=w.player(p).getCompound("daily").copy();w.refreshTasks(p);
        var snapshot=w.snapshot(p);h.assertFalse(snapshot.contains("fantasyGacha")||snapshot.contains("gacha")||snapshot.contains("skyGarden"),"Retired data leaked into client snapshot");
        var loaded=new JournalWorld(w.save(new CompoundTag()));loaded.refreshTasks(p);
        h.assertTrue(loaded.player(p).getCompound("fantasyGacha").equals(legacy),"Legacy ownership was erased");
        h.assertTrue(before.equals(loaded.player(p).getCompound("daily")),"Migration changed completed task payout/progress");
        h.assertTrue(p.getInventory().countItem(SkyContent.TICKET.get())==4&&p.getInventory().countItem(SkyContent.FRAGMENT.get())==12,"Removed bonus changed old inventory");
        h.assertTrue(snapshot.getCompound("titles").getString("equipped").isEmpty(),"Retired equipped title survived migration");
        h.assertTrue(snapshot.getCompound("titles").getString("owned").equals("새싹농부"),"Unrelated title lost or retired title retained");h.succeed();
    }
    @GameTest(template="empty") public static void profileUsesActualVanillaExperience(GameTestHelper h){
        var p=player(h);p.giveExperiencePoints(137);var snapshot=JournalWorld.get(p.getServer()).snapshot(p);
        h.assertTrue(snapshot.getInt("level")==p.experienceLevel&&snapshot.getFloat("levelProgress")==p.experienceProgress,"Profile XP was fabricated");
        h.assertTrue(snapshot.getInt("levelRequired")==p.getXpNeededForNextLevel(),"Wrong next-level requirement");h.succeed();
    }
    @GameTest(template="empty") public static void weeklyFieldsDistinctNoTicketBonus(GameTestHelper h){
        var p=player(h);p.getInventory().clearContent();var w=new JournalWorld();w.refreshTasks(p);var weekly=w.player(p).getCompound("weekly");var kinds=new HashSet<Integer>();
        for(int n=0;n<3;n++){var t=weekly.getCompound(""+n);kinds.add(t.getInt("kind"));t.putInt("progress",t.getInt("target"));}
        var before=weekly.copy();w.refreshTasks(p);var loaded=new JournalWorld(w.save(new CompoundTag()));loaded.refreshTasks(p);
        h.assertTrue(kinds.size()==3,"Weekly repeated a field");h.assertTrue(before.equals(loaded.player(p).getCompound("weekly")),"Reload changed weekly rewards");
        h.assertTrue(p.getInventory().isEmpty(),"Removed weekly bonus minted items");h.succeed();
    }

    @GameTest(template="empty") public static void removedSkyTitlesMigrateWithoutOtherLoss(GameTestHelper h){
        var p=player(h);var old=new CompoundTag();
        old.putString("owned","새싹농부|구름 위의 손님|사계요정|하늘 정원의 수호자|미래 칭호");old.putString("equipped","구름 위의 손님");old.putInt("customRecord",73);
        p.getPersistentData().put("SunlitJournalTitles",old);p.getPersistentData().putString("anotherMod","keep");
        new JournalEvents().login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
        var cleaned=p.getPersistentData().getCompound("SunlitJournalTitles");h.assertTrue(cleaned.getString("owned").equals("새싹농부|사계요정|미래 칭호"),"Upgrade removed unrelated titles or left sky titles");
        h.assertFalse(cleaned.contains("equipped"),"Retired equipped title retained");h.assertTrue(cleaned.getInt("customRecord")==73&&p.getPersistentData().getString("anotherMod").equals("keep"),"Upgrade touched unrelated NBT");
        var saved=cleaned.copy();SunlitJournal.data(p);h.assertTrue(saved.equals(SunlitJournal.data(p)),"Cleanup is not idempotent");
        h.assertTrue(JournalChat.format(p,net.minecraft.network.chat.Component.literal("test")).isEmpty(),"Retired prefix still appears in chat");
        var snapshot=JournalWorld.get(p.getServer()).snapshot(p).getCompound("titles");h.assertTrue(snapshot.equals(saved),"Snapshot resurrected or damaged title records");h.succeed();
    }
    @GameTest(template="empty") public static void removedSkyTitlesCannotBeRegranted(GameTestHelper h){
        var p=player(h);SunlitJournal.saveOwned(p,new LinkedHashSet<>(List.of("새싹농부","구름 위의 손님","하늘 정원의 수호자")));
        h.assertTrue(p.getPersistentData().getCompound("SunlitJournalTitles").getString("owned").equals("새싹농부"),"Save API persisted a retired title");
        JournalWorld.grant(p,"구름 위의 손님");JournalWorld.grant(p,"하늘 정원의 수호자");
        h.assertTrue(SunlitJournal.owned(p).equals(Set.of("새싹농부")),"Grant API restored retired title");
        h.assertFalse(SunlitJournal.isAllowedTitle("구름 위의 손님")||SunlitJournal.isAllowedTitle("하늘 정원의 수호자"),"Command grant registry still accepts retired titles");
        JournalWorld.grant(p,"사계요정");h.assertTrue(SunlitJournal.owned(p).contains("사계요정"),"Ordinary grant stopped working");h.succeed();
    }
    @GameTest(template="empty") public static void removedSkyTitlesDoNotReturnOnClone(GameTestHelper h){
        var original=player(h);var replacement=player(h);var old=new CompoundTag();old.putString("owned","하늘 정원의 수호자|벚꽃요정|구름 위의 손님");old.putString("equipped","벚꽃요정");old.putLong("extra",100);
        original.getPersistentData().put("SunlitJournalTitles",old);SunlitJournal.copyTitles(original,replacement);
        var copied=replacement.getPersistentData().getCompound("SunlitJournalTitles");h.assertTrue(copied.getString("owned").equals("벚꽃요정"),"Clone restored removed title");
        h.assertTrue(copied.getString("equipped").equals("벚꽃요정")&&copied.getLong("extra")==100,"Clone lost valid equip or unrelated record");
        var saved=copied.copy();replacement.getPersistentData().put("SunlitJournalTitles",saved.copy());h.assertTrue(SunlitJournal.data(replacement).equals(saved),"Reload changed migrated titles");
        h.assertTrue(original.getPersistentData().getCompound("SunlitJournalTitles").getString("owned").equals("벚꽃요정"),"Original retained retired records after clone");h.succeed();
    }

}
