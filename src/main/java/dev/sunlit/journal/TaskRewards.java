package dev.sunlit.journal;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Agreed reward contract. Roll once during creation; resolve every item before charging the claim. */
public final class TaskRewards {
    public record Material(String id,String label,int daily) {}
    public record Display(String id,String label,int count) {}
    public static final String COIN="numismatics:cog";
    public static final List<List<Material>> POOLS=List.of(
        List.of(new Material("dew_drop_farmland_growth:low_quality_fertilizer","저등급 비료",12),new Material("dew_drop_farmland_growth:weak_fertilizer","약한 비료",12),new Material("dew_drop_farmland_growth:hydrating_fertilizer","보습 비료",14)),
        List.of(new Material("aquaculture:worm","지렁이",18),new Material("crabbersdelight:crab_trap_bait","게잡이 통발 미끼",14)),
        List.of(new Material("minecraft:coal","석탄",64),new Material("minecraft:copper_ingot","구리 주괴",64),new Material("minecraft:iron_ingot","철 주괴",64)),
        List.of(new Material("minecraft:sugar","설탕",18),new Material("meadow:alpine_salt","알프스 소금",18)),
        List.of(new Material("society:animal_feed","동물 먹이",18),new Material("society:animal_feed","동물 먹이",22),new Material("society:animal_feed","동물 먹이",26))
    );
    public static CompoundTag plan(int kind,boolean weekly,int choice){
        var m=POOLS.get(kind).get(choice);var out=new CompoundTag();out.putInt("version",1);
        out.putString("currency",COIN);out.putInt("coins",weekly?6:2);out.putInt("xp",weekly?80:20);
        out.putString("item",m.id());out.putString("label",m.label());out.putInt("count",weekly?(kind==2?96:m.daily()*3):m.daily());return out;
    }
    public static void assign(CompoundTag task,boolean weekly,RandomSource random){
        if(task.contains("payout",10))return;
        if(task.getBoolean("claimed")){var old=new CompoundTag();old.putBoolean("legacy",true);old.putInt("coins",task.getInt("reward"));old.putInt("xp",task.getInt("reward")*10);task.put("payout",old);return;}
        int kind=task.getInt("kind");task.put("payout",plan(kind,weekly,random.nextInt(POOLS.get(kind).size())));
    }
    public static ItemStack stack(String id,int count){
        var key=ResourceLocation.tryParse(id);if(key==null||!BuiltInRegistries.ITEM.containsKey(key))return ItemStack.EMPTY;
        var item=BuiltInRegistries.ITEM.get(key);return item==Items.AIR?ItemStack.EMPTY:new ItemStack(item,count);
    }
    public static List<Display> display(CompoundTag task){
        var r=task.getCompound("payout");
        if(r.getBoolean("legacy"))return List.of(new Display("minecraft:emerald","에메랄드",r.getInt("coins")),new Display("minecraft:experience_bottle","경험치",r.getInt("xp")));
        return List.of(new Display(r.getString("currency"),"황동 동전",r.getInt("coins")),new Display(r.getString("item"),r.getString("label"),r.getInt("count")),new Display("minecraft:experience_bottle","경험치",r.getInt("xp")));
    }
    /** No partial currency/XP or claim flag if one of the required pack items is unavailable. */
    public static boolean available(CompoundTag task){
        var r=task.getCompound("payout");return r.getInt("version")==1&&r.getInt("coins")>0&&r.getInt("count")>0&&!stack(r.getString("currency"),1).isEmpty()&&!stack(r.getString("item"),1).isEmpty();
    }
    public static void unavailable(ServerPlayer p){p.sendSystemMessage(Component.literal("[업무 일지] 보상 아이템을 준비하지 못했어요. 진행도는 보존되니 운영자에게 알려주세요."));}
    public static void deliver(ServerPlayer p,CompoundTag task){
        var r=task.getCompound("payout");give(p,r.getString("currency"),r.getInt("coins"));give(p,r.getString("item"),r.getInt("count"));p.giveExperiencePoints(r.getInt("xp"));
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();
    }
    private static void give(ServerPlayer p,String id,int amount){
        while(amount>0){var sample=stack(id,1);int n=Math.min(amount,sample.getMaxStackSize());sample.setCount(n);if(!p.getInventory().add(sample))p.drop(sample,false);amount-=n;}
    }
}
