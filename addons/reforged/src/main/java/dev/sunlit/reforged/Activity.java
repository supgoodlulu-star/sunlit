package dev.sunlit.reforged;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;
public final class Activity{
    private static final Map<String,Long> ROLLED=new HashMap<>();
    private static final Map<ServerLevel,Set<BlockPos>> CLEANUP=new HashMap<>();
    static boolean eligible(ServerPlayer p){return !p.isCreative()&&!p.isSpectator()&&!(p instanceof FakePlayer);}
    public static boolean fishingOnce(net.minecraft.world.entity.projectile.FishingHook hook,String reason){String key="sunlit_reforged_"+reason;var data=hook.getPersistentData();if(data.getBoolean(key))return false;data.putBoolean(key,true);return true;}
    public static void give(ServerPlayer p,Item item){var stack=new ItemStack(item);if(!p.getInventory().add(stack))p.drop(stack,false);SunlitReforged.debug("reward player={} item={}",p.getUUID(),item);}
    static boolean once(ServerPlayer p,ServerLevel level,BlockPos pos){String key=level.dimension().location()+":"+pos.asLong();long tick=level.getGameTime();Long previous=ROLLED.put(key,tick);return previous==null||previous!=tick;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void place(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel level&&e.getPlacedBlock().is(Tags.Blocks.ORES))Ledger.get(level.getServer()).placed(level,e.getPos());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void multi(BlockEvent.EntityMultiPlaceEvent e){if(e.getLevel() instanceof ServerLevel level)for(var snap:e.getReplacedBlockSnapshots())if(level.getBlockState(snap.getPos()).is(Tags.Blocks.ORES))Ledger.get(level.getServer()).placed(level,snap.getPos());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void breakBlock(BlockEvent.BreakEvent e){if(e.getLevel() instanceof ServerLevel level&&Ledger.get(level.getServer()).isPlaced(level,e.getPos()))CLEANUP.computeIfAbsent(level,x->new HashSet<>()).add(e.getPos().immutable());}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        if(server.getTickCount()%20==0){ROLLED.clear();}
        CLEANUP.forEach((level,positions)->{for(var pos:positions)if(!level.getBlockState(pos).is(Tags.Blocks.ORES))Ledger.get(server).forget(level,pos);});CLEANUP.clear();
    }
    @SubscribeEvent public void stop(ServerStoppedEvent e){ROLLED.clear();CLEANUP.clear();}
}
