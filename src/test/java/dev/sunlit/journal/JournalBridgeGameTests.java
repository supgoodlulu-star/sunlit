package dev.sunlit.journal;
import dev.sunlit.journal.api.JournalRewardEvent;
import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import java.util.*;
import java.util.function.Consumer;
@GameTestHolder(SunlitJournal.MODID)
@PrefixGameTestTemplate(false)
public final class JournalBridgeGameTests{
    @GameTest(template="empty") public static void taskBridgePublishesOncePerCompletion(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BridgeTasks"));var w=JournalWorld.get(p.getServer());w.refreshTasks(p);
        for(String period:List.of("daily","weekly"))for(int i=0;i<JournalWorld.taskCount(period);i++){var t=w.player(p).getCompound(period).getCompound(""+i);t.putInt("kind",1);t.putInt("target",9999);t.putInt("progress",0);t.putString("subject","minecraft:cod");}
        var task=w.player(p).getCompound("daily").getCompound("0");task.putInt("kind",0);task.putInt("target",1);task.putString("subject","minecraft:wheat");
        var seen=new ArrayList<JournalRewardEvent>();Consumer<JournalRewardEvent> listener=e->{if(e.getPlayer()==p)seen.add(e);};net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL,false,JournalRewardEvent.class,listener);
        try{w.activity(p,0,1,"minecraft:wheat");w.activity(p,0,1,"minecraft:wheat");h.assertTrue(seen.size()==1&&seen.get(0).getKind().equals("daily")&&!seen.get(0).getInstanceId().isBlank(),"Duplicate/missing task event");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);}h.succeed();
    }
    @GameTest(template="empty") public static void festivalBridgeFollowsExistingPayoutOnce(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BridgeFestival"));var w=new JournalWorld();w.setSeason(new Integrations.Season("SPRING",1,1000));
        var n=w.save(new net.minecraft.nbt.CompoundTag());n.getCompound("festival").putLong("goal",1);w=new JournalWorld(n);w.contribute(p,1);
        var seen=new ArrayList<JournalRewardEvent>();Consumer<JournalRewardEvent> listener=e->{if(e.getPlayer()==p)seen.add(e);};net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL,false,JournalRewardEvent.class,listener);
        try{w.achievements(p);w.achievements(p);h.assertTrue(seen.size()==1&&seen.get(0).getKind().equals("festival"),"Duplicate/missing personal festival event");h.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.EMERALD)==16,"Existing festival payout changed");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);}h.succeed();
    }
}
