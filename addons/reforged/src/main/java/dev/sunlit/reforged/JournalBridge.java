package dev.sunlit.reforged;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;
public final class JournalBridge{
    @SuppressWarnings({"unchecked","rawtypes"}) public static void register(){
        try{Class<?> type=Class.forName("dev.sunlit.journal.api.JournalRewardEvent");MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false,(Class)type,(Consumer<Event>)JournalBridge::reward);}
        catch(ClassNotFoundException absent){SunlitReforged.LOG.info("Journal reward bridge unavailable; Journal 0.8.11 bridge needed for task/festival cube bonuses");}
    }
    static void reward(Event e){
        try{var c=e.getClass();ServerPlayer p=(ServerPlayer)c.getMethod("getPlayer").invoke(e);String kind=(String)c.getMethod("getKind").invoke(e),id=(String)c.getMethod("getInstanceId").invoke(e);
            if(!Activity.eligible(p))return;int rate=switch(kind){case "daily"->3;case "weekly"->4;case "festival"->5;default->-1;};if(rate<0||!Ledger.get(p.getServer()).reward(p.getUUID(),kind+":"+id))return;
            if(p.getRandom().nextDouble()<Rules.get().rewardRates[rate]/100)Activity.give(p,SunlitReforged.cube(Potential.Cube.values()[rate-3]));
        }catch(ReflectiveOperationException|RuntimeException failure){SunlitReforged.LOG.error("Journal reward event failed",failure);}
    }
}
