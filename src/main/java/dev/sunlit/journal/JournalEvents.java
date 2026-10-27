package dev.sunlit.journal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public final class JournalEvents {
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){
        if(e.getEntity() instanceof ServerPlayer p){if(SkyGarden.inside(p))SkyGarden.leave(p);var w=JournalWorld.get(p.getServer());w.refreshTasks(p);w.updateFestival(p);w.achievements(p);}
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%40!=0)return;
        var w=JournalWorld.get(p.getServer());w.refreshTasks(p);w.updateFestival(p);w.achievements(p);
        if(JournalNetwork.isWatching(p))JournalNetwork.send(p,false,"");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void place(BlockEvent.EntityPlaceEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getPlacedBlock().is(Tags.Blocks.ORES))JournalWorld.get(p.getServer()).placedOre(p,e.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void harvest(BlockEvent.BreakEvent e){
        if(!(e.getPlayer() instanceof ServerPlayer p))return;
        var w=JournalWorld.get(p.getServer());
        if(e.getState().getBlock() instanceof CropBlock c&&c.isMaxAge(e.getState()))w.activity(p,0,1,net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(e.getState().getBlock()).toString());
        if(e.getState().is(Tags.Blocks.ORES)&&!w.consumePlacedOre(p,e.getPos()))w.activity(p,2,1,net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(e.getState().getBlock()).toString());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void fish(ItemFishedEvent e){
        if(e.getEntity() instanceof ServerPlayer p){
            for(var stack:e.getDrops())if(stack.is(ItemTags.FISHES))
                JournalWorld.get(p.getServer()).activity(p,1,stack.getCount(),net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
    }
    @SubscribeEvent public void smelt(PlayerEvent.ItemSmeltedEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getSmelting().isEdible())JournalWorld.get(p.getServer()).activity(p,3,e.getSmelting().getCount(),net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getSmelting().getItem()).toString());
    }
    @SubscribeEvent public void craft(PlayerEvent.ItemCraftedEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getCrafting().isEdible())JournalWorld.get(p.getServer()).activity(p,3,e.getCrafting().getCount(),net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getCrafting().getItem()).toString());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void breed(BabyEntitySpawnEvent e){
        if(e.getCausedByPlayer() instanceof ServerPlayer p&&e.getChild()!=null)JournalWorld.get(p.getServer()).activity(p,4,1,net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getChild().getType()).toString());
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)JournalNetwork.forget(p);}
}
