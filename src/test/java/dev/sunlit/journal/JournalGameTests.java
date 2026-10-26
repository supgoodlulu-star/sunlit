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
        int before=p.getInventory().countItem(Items.EMERALD);
        h.assertTrue(w.claim(p,"daily",0),"Completed task rejected");
        int after=p.getInventory().countItem(Items.EMERALD);
        h.assertTrue(after-before==task.getInt("reward"),"Wrong reward amount");
        h.assertFalse(w.claim(p,"daily",0),"Duplicate reward accepted");
        h.assertFalse(w.claim(p,"daily",-1),"Negative index accepted");
        h.assertFalse(w.claim(p,"weekly",3),"Out-of-range index accepted");
        h.assertFalse(w.claim(p,"other",0),"Unknown period accepted");
        h.assertTrue(p.getInventory().countItem(Items.EMERALD)==after,"Invalid claims changed inventory");
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
}
