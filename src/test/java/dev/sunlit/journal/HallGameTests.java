package dev.sunlit.journal;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.nbt.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(SunlitJournal.MODID)
@PrefixGameTestTemplate(false)
public class HallGameTests {
    static UUID id(int n){return new UUID(0,n);}
    static HallWorld world(AtomicReference<Instant> time){return new HallWorld(new CompoundTag(),time::get);}
    static AtomicReference<Instant> time(){return new AtomicReference<>(Instant.parse("2026-10-31T14:59:59Z"));}
    @GameTest(template="empty") public static void seoulRolloverArchivesAndSurvivesRestart(GameTestHelper h){
        var t=time();var w=world(t);w.add(id(1),"tasks",7);t.set(Instant.parse("2026-10-31T15:00:00Z"));
        h.assertTrue(w.season().equals("2026-11"),"Not Seoul midnight");h.assertTrue(w.rows("2026-10","tasks").get(0).score()==7,"Archive lost");h.assertTrue(w.rows("2026-11","tasks").isEmpty(),"New season carried old score");
        w.add(id(1),"tasks",2);var restored=new HallWorld(w.save(new CompoundTag()),t::get);h.assertTrue(restored.rows("2026-10","tasks").get(0).score()==7&&restored.rows("2026-11","tasks").get(0).score()==2,"Restart lost records");
        t.set(Instant.parse("2026-10-31T14:00:00Z"));h.assertTrue(restored.season().equals("2026-11"),"Clock rollback reopened archive");h.succeed();
    }
    @GameTest(template="empty") public static void archivedNamesStayFixedAndAchievementsAreCumulative(GameTestHelper h){
        var t=time();CompoundTag n=new CompoundTag();var who=HallWorld.child(HallWorld.child(n,"people"),id(1).toString());who.putString("name","Tester");who.putString("nick","옛이름");who.putInt("achievements",5);var w=new HallWorld(n,t::get);w.add(id(1),"tasks",3);
        t.set(Instant.parse("2026-10-31T15:00:00Z"));w.season();n=w.save(new CompoundTag());n.getCompound("people").getCompound(id(1).toString()).putString("nick","새이름");w=new HallWorld(n,t::get);
        h.assertTrue(w.rows("2026-10","tasks").get(0).nick().equals("옛이름"),"Archived nickname changed");h.assertTrue(w.rows(w.season(),"achievements").get(0).score()==5,"Monthly reset erased cumulative achievements");h.succeed();
    }
    @GameTest(template="empty") public static void xpUsesBaselineSkipsOfflineMonthsAndCreative(GameTestHelper h){
        var t=time();var w=world(t);w.sample(id(1),"farm",1000,true);w.sample(id(1),"farm",1040,true);w.sample(id(1),"farm",1100,false);w.sample(id(1),"farm",1110,true);
        h.assertTrue(w.rows(w.season(),"skill:farm").get(0).score()==50,"Retroactive or creative XP counted");
        t.set(Instant.parse("2027-01-01T00:00:00Z"));w.sample(id(1),"farm",2000,true);h.assertTrue(w.rows(w.season(),"skill:farm").isEmpty(),"Offline XP entered new season");w.sample(id(1),"farm",2020,true);h.assertTrue(w.rows(w.season(),"skill:farm").get(0).score()==20,"New XP lost");h.succeed();
    }
    @GameTest(template="empty") public static void tiesPaginationAndOverflow(GameTestHelper h){
        var w=world(time());for(int i=1;i<=20;i++)w.add(id(i),"tasks",i<=2?100:30-i);
        var rows=w.rows(w.season(),"tasks");h.assertTrue(rows.get(0).rank()==1&&rows.get(1).rank()==1&&rows.get(2).rank()==3,"Ties not competition ranked");
        var page=w.snapshot("bad","bad",Integer.MAX_VALUE);h.assertTrue(page.getInt("page")==2&&page.getList("rows",10).size()==4,"Invalid request pagination unbounded");
        w.add(id(1),"tasks",Long.MAX_VALUE);w.add(id(1),"tasks",1);h.assertTrue(w.rows(w.season(),"tasks").get(0).score()==Long.MAX_VALUE,"Score overflowed");h.succeed();
    }
    @GameTest(template="empty") public static void completedTaskCountedOnceWithoutClaim(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HallTaskTest"));var w=JournalWorld.get(p.getServer());w.refreshTasks(p);
        var tasks=w.player(p).getCompound("daily");for(int i=0;i<5;i++){var task=tasks.getCompound(""+i);task.putString("subject","minecraft:wheat");task.putInt("kind",0);task.putInt("target",i==0?1:9999);task.putInt("progress",0);}
        var hall=HallWorld.get(p.getServer());long before=hall.rows(hall.season(),"tasks").stream().filter(r->r.id().equals(p.getUUID())).mapToLong(HallWorld.Row::score).sum();w.activity(p,0,1,"minecraft:wheat");w.activity(p,0,1,"minecraft:wheat");
        long after=hall.rows(hall.season(),"tasks").stream().filter(r->r.id().equals(p.getUUID())).mapToLong(HallWorld.Row::score).sum();h.assertTrue(after==before+1,"Completion counted twice or required claim");h.succeed();
    }
    @GameTest(template="empty") public static void festivalCountsOnlyRemainingGoal(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HallFestival"));var hall=HallWorld.get(p.getServer());hall.observe(p);
        CompoundTag data=new CompoundTag(),f=HallWorld.child(data,"festival");f.putBoolean("available",true);f.putLong("goal",10);f.putLong("progress",8);f.putString("season","SPRING");var journal=new JournalWorld(data);
        journal.contribute(p,5);journal.contribute(p,10);var row=hall.rows(hall.season(),"festival").stream().filter(r->r.id().equals(p.getUUID())).findFirst().orElseThrow();h.assertTrue(row.score()==2,"Overflow or post-completion contribution counted");h.succeed();
    }
    @GameTest(template="empty") public static void noPetAndPublicViewOperatorPlacement(GameTestHelper h)throws Exception{
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HallPermission"));var dispatcher=p.getServer().getCommands().getDispatcher();
        var source=p.createCommandSourceStack().withPermission(0);var result=dispatcher.parse("sunlithall place tasks",source);h.assertTrue(!result.getExceptions().isEmpty()||result.getReader().canRead(),"Non-operator can install display");
        var entity=HallContent.FIGURE.get().create(h.getLevel());entity.configure("tasks",1);entity.moveTo(h.absolutePos(new net.minecraft.core.BlockPos(1,1,1)),0,0);h.getLevel().addFreshEntity(entity);
        CompoundTag n=new CompoundTag();entity.save(n);h.assertTrue(n.getString("HallCategory").equals("tasks")&&n.getInt("HallSlot")==1,"Display settings not persisted");h.assertTrue(!entity.hurt(h.getLevel().damageSources().generic(),10)&&!entity.isPushable(),"Display vulnerable");entity.discard();h.succeed();
    }
    @GameTest(template="empty") public static void winnerOnlyAtExactPositionWithoutChangingBlocks(GameTestHelper h)throws Exception{
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HallPosition"));var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));p.setPos(pos.getX()+.27,pos.getY()+.35,pos.getZ()+.68);p.setYRot(73);
        var level=h.getLevel();var marker=pos.below();level.setBlockAndUpdate(marker,net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState());
        var before=new java.util.HashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();for(var b:net.minecraft.core.BlockPos.betweenClosed(pos.offset(-5,-2,-5),pos.offset(5,4,5)))before.put(b.immutable(),level.getBlockState(b));
        var prior=level.getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(20)).stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        int result=p.getServer().getCommands().getDispatcher().execute("명예의 전당 채광",p.createCommandSourceStack().withPermission(2));
        var added=level.getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(20)).stream().filter(f->!prior.contains(f.getUUID())).toList();h.assertTrue(result==1&&added.size()==1,"Not exactly one figure");var figure=added.get(0);
        h.assertTrue(figure.position().distanceTo(p.position())<.000001&&figure.getYRot()==73,"Position or facing changed");CompoundTag n=new CompoundTag();figure.save(n);h.assertTrue(n.getString("HallCategory").equals("skill:society:mining")&&n.getInt("HallSlot")==0,"Incorrect category/rank");
        h.assertTrue(before.entrySet().stream().allMatch(e->level.getBlockState(e.getKey()).equals(e.getValue())),"Placement changed construction blocks");figure.discard();h.succeed();
    }
    @GameTest(template="empty") public static void koreanPlacementIsOperatorOnlyAndUnknownNamesDoNotSpawn(GameTestHelper h)throws Exception{
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HallAlias"));var d=p.getServer().getCommands().getDispatcher();
        for(String command:java.util.List.of("명예의전당 채광","명예의 전당 채광","sunlithall place tasks")){
            boolean rejected=false;try{d.execute(command,p.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){rejected=true;}h.assertTrue(rejected,"Unprivileged placement: "+command);
        }
        h.assertTrue(HallContent.resolve(HallWorld.get(p.getServer()),"목축").equals("skill:society:husbandry"),"Ranching alias wrong");
        h.assertTrue(d.execute("명예의전당 없는분야",p.createCommandSourceStack().withPermission(2))==0,"Unknown category accepted");h.succeed();
    }
    @GameTest(template="empty") public static void legacyRunnerUpFiguresAreRemovedButWinnerPersists(GameTestHelper h){
        var level=h.getLevel();var runner=HallContent.FIGURE.get().create(level);CompoundTag n=new CompoundTag();n.putString("HallCategory","tasks");n.putInt("HallSlot",1);runner.readAdditionalSaveData(n);runner.tick();h.assertTrue(runner.isRemoved(),"Legacy runner-up persisted");
        var winner=HallContent.FIGURE.get().create(level);winner.configure("tasks",0);winner.tick();h.assertTrue(!winner.isRemoved(),"Winner removed");winner.discard();h.succeed();
    }

}
