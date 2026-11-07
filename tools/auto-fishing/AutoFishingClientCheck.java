package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class AutoFishingClientCheck {
    static int phase,ticks,cycles,miniTicks,aquaStartWins,aquaStartCasts,fullInventoryCasts,fullInventoryWins;static long fullInventoryStartedAt;static long start=System.currentTimeMillis();static CompletableFuture<Void> pending;
    static final AtomicInteger casts=new AtomicInteger(),wins=new AtomicInteger(),chests=new AtomicInteger(),rewards=new AtomicInteger();
    static volatile UUID playerId;static Object previousScreen;static boolean miniShot,treasureShot;static final List<Object> packets=new ArrayList<>();
    interface Work{void run(ServerPlayer p)throws Exception;}
    static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException("AUTO_FISHING_CLIENT: "+msg);}
    static Object get(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void server(Minecraft mc,Work work){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
    static void key(){MinecraftForge.EVENT_BUS.post(new InputEvent.Key(GLFW.GLFW_KEY_F8,0,GLFW.GLFW_PRESS,0));}
    static void shot(Minecraft mc,String name){var out=new java.io.File(System.getProperty("sunlit.previewOutput"));new java.io.File(out,"screenshots").mkdirs();mc.getToasts().clear();Screenshot.grab(out,name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void spawned(net.minecraftforge.event.entity.EntityJoinLevelEvent e){if(Boolean.getBoolean("sunlit.autoFishingCheck")&&!e.getLevel().isClientSide&&e.getEntity() instanceof FishingHook hook&&hook.getPlayerOwner()!=null&&hook.getPlayerOwner().getUUID().equals(playerId))casts.incrementAndGet();}
    @SuppressWarnings({"rawtypes","unchecked"}) static void events()throws Exception{
        var type=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameStartedEvent");
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,(Class)type,(Consumer<Event>)e->{try{
            // QA only: force a chest to test collection, never a success or progress value.
            type.getMethod("setForcedTreasureChest",boolean.class).invoke(e,true);
            System.out.println("AUTO_FISHING_QA_NATIVE_START bar="+type.getMethod("getBarSize").invoke(e)+" line="+type.getMethod("getLineStrength").invoke(e));
        }catch(Exception x){throw new RuntimeException(x);}});
        var end=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameEndedEvent");MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,(Class)end,(Consumer<Event>)e->{try{if((Boolean)end.getMethod("wasSuccessful").invoke(e)){wins.incrementAndGet();if((Boolean)end.getMethod("gotChest").invoke(e))chests.incrementAndGet();}}catch(Exception x){throw new RuntimeException(x);}});
        var modify=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameModifyRewardsEvent");MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,(Class)modify,(Consumer<Event>)e->{try{for(var s:(List<ItemStack>)modify.getMethod("getRewards").invoke(e))if(FishSizes.fish(s)){require(FishSizes.size(s).isPresent(),"Real catch missing fish-size data");rewards.addAndGet(s.getCount());}}catch(Exception x){throw new RuntimeException(x);}});
    }
    static void simulation(Minecraft mc)throws Exception{
        int success=0,treasure=0;for(int packetIndex=0;packetIndex<packets.size();packetIndex++)for(int seed=0;seed<5;seed++){
            var packet=packets.get(packetIndex);var s=(Screen)Class.forName(AutoFishingStardew.SCREEN).getConstructor(packet.getClass()).newInstance(packet);s.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
            var game=get(s,"minigame");((Random)get(game,"random")).setSeed(seed);var adapter=new AutoFishingStardew(s);String status="";int elapsed=0;
            while(elapsed++<3000){adapter.tick();s.tick();status=get(s,"status").toString();if(status.equals("SUCCESS")||status.equals("FAILURE"))break;}
            boolean won=status.equals("SUCCESS"),box=(Boolean)game.getClass().getMethod("gotChest").invoke(game);if(won)success++;if(won&&box)treasure++;
            System.out.println("AUTO_FISHING_NATIVE_SIM packet="+packetIndex+" movementSeed="+seed+" result="+status+" chest="+box+" ticks="+elapsed);
        }
        require(success>=packets.size()*3,"Native input controller too unreliable: "+success);require(treasure>0,"Native controller never collected a treasure chest");System.out.println("AUTO_FISHING_NATIVE_SIM_PASSED wins="+success+" chests="+treasure+" games="+packets.size()*5);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.autoFishingCheck"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<600000,"Timeout phase="+phase+" casts="+casts+" wins="+wins+" chests="+chests+" status="+AutoFishingClient.status());
        if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button b&&(b.getMessage().getString().equals("Proceed")||b.getMessage().getString().equals("진행"))){b.onPress();return;}
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}ticks++;
        if(phase==2||phase==9){
            if(AutoFishingStardew.isScreen(mc.screen)){
                if(previousScreen!=mc.screen){previousScreen=mc.screen;cycles++;miniTicks=0;}miniTicks++;
                if(!miniShot&&miniTicks>30){shot(mc,"auto-fishing-native-minigame");miniShot=true;}
                if(!treasureShot&&AutoFishingClient.status().equals("treasure")){shot(mc,"auto-fishing-treasure-tracking");treasureShot=true;}
            }
            if((phase==2?wins.get()>=2&&chests.get()>=1&&casts.get()>=3:wins.get()>aquaStartWins&&casts.get()>=aquaStartCasts+2)&&mc.screen==null){boolean aqua=phase==9;key();require(!AutoFishingClient.enabled(),"Hotkey did not stop fishing");shot(mc,aqua?"auto-fishing-neptunium-rewards":"auto-fishing-stopped-after-rewards");phase=aqua?10:3;ticks=0;}
            return;
        }
        if(phase==12){
            require(AutoFishingClient.enabled(),"Full inventory stopped automatic fishing");
            if(wins.get()>fullInventoryWins&&casts.get()>=fullInventoryCasts+2&&mc.screen==null){
                shot(mc,"auto-fishing-full-inventory-continues");key();server(mc,p->{require(p.getInventory().getFreeSlot()<0,"Full-inventory scenario lost its setup");var drops=p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(40)).stream().filter(i->FishSizes.size(i.getItem()).isPresent()&&i.getItem().getTag().getCompound(FishSizes.KEY).getLong("caughtAt")>=fullInventoryStartedAt).toList();require(!drops.isEmpty(),"Full-inventory fish were not dropped normally");System.out.println("AUTO_FISHING_FULL_INVENTORY_PASSED casts="+(casts.get()-fullInventoryCasts)+" wins="+(wins.get()-fullInventoryWins)+" fishDrops="+drops.size());for(int i=1;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);p.containerMenu.broadcastChanges();});phase=13;ticks=0;
            }return;
        }
        if(ticks<25)return;ticks=0;
        switch(phase++){
            case 0->{events();mc.options.guiScale().set(2);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.reloadResourcePacks();mc.resizeDisplay();mc.setScreen(null);JournalKeys.AUTO_FISHING.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8));KeyMapping.resetMapping();server(mc,p->{
                playerId=p.getUUID();SunlitJournal.data(p).remove("fishRecords");p.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,p.getServer());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.teleportTo(.5,65,.5);p.setYRot(-90);p.setXRot(30);p.connection.teleport(.5,65,.5,-90,30);
                for(int x=-3;x<=18;x++)for(int z=-9;z<=9;z++)for(int y=60;y<=67;y++){var block=y==60?Blocks.STONE:y<=64&&x>=2&&x<=17&&Math.abs(z)<=8?Blocks.WATER:y<=64?Blocks.STONE:Blocks.AIR;p.serverLevel().setBlock(new BlockPos(x,y,z),block.defaultBlockState(),2);}
                var rod=new ItemStack(Items.FISHING_ROD);rod.enchant(net.minecraft.world.item.enchantment.Enchantments.FISHING_SPEED,3);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,rod);p.getInventory().selected=0;p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(60000,0,false,false);
                var behaviors=Class.forName("com.bonker.stardewfishing.server.data.FishBehaviorReloadListener");var behavior=Class.forName("com.bonker.stardewfishing.common.FishBehavior");var type=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameStartedEvent");var packet=Class.forName("com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket");
                for(String id:List.of("minecraft:cod","minecraft:salmon","aquaculture:minnow","aquaculture:tuna"))for(int width:new int[]{36,72}){var fish=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(id)));var hook=new FishingHook(p,p.serverLevel(),0,0);var ev=type.getConstructor(ServerPlayer.class,FishingHook.class,ItemStack.class,ItemStack.class,behavior,boolean.class).newInstance(p,hook,rod,fish,behaviors.getMethod("getBehavior",ItemStack.class).invoke(null,fish),false);type.getMethod("setBarSize",int.class).invoke(ev,width);packets.add(packet.getConstructor(type,boolean.class,boolean.class).newInstance(ev,true,false));hook.discard();}p.fishing=null;p.containerMenu.broadcastChanges();
            });}
            case 1->{simulation(mc);key();require(AutoFishingClient.enabled(),"Bound hotkey did not enable fishing");key();require(!AutoFishingClient.enabled(),"Bound hotkey did not disable fishing");key();shot(mc,"auto-fishing-enabled");}
            case 3->{require(!AutoFishingClient.enabled(),"Auto fishing restarted itself");key();server(mc,p->{p.getInventory().selected=1;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(1));});}
            case 4->{require(!AutoFishingClient.enabled(),"Rod/slot change did not stop fishing");server(mc,p->{p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));});}
            case 5->{key();require(AutoFishingClient.enabled(),"Cannot enable after rod restore");mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
            case 6->{require(!AutoFishingClient.enabled(),"Inventory did not stop automation");mc.setScreen(null);server(mc,p->{require(rewards.get()>=2,"Server awarded no fish");require(p.getInventory().items.stream().anyMatch(s->FishSizes.size(s).isPresent()),"Caught fish did not reach the actual player inventory");require(p.getMainHandItem().getDamageValue()>0,"Native rod durability was bypassed");require(!SunlitJournal.data(p).getCompound("fishRecords").isEmpty(),"Real catches did not update maximum-size records");System.out.println("AUTO_FISHING_END_TO_END_PASSED casts="+casts+" wins="+wins+" chests="+chests+" fishRewards="+rewards+" durability="+p.getMainHandItem().getDamageValue());});}
            case 7->{server(mc,p->{if(p.fishing!=null)p.fishing.discard();var rod=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation("aquaculture:neptunium_fishing_rod")));rod.enchant(net.minecraft.world.item.enchantment.Enchantments.FISHING_SPEED,3);rod.getOrCreateTag().putString("AutoFishingForeignProbe","keep");var potential=rod.getOrCreateTagElement("sunlit_reforged");potential.putInt("schemaVersion",2);potential.putString("potentialTier","LEGENDARY");var lines=new net.minecraft.nbt.ListTag();String[] ids={"FISHING_BAR","TREASURE_CHANCE","LIFE_XP"};double[] values={100,50,20};for(int i=0;i<3;i++){var line=new net.minecraft.nbt.CompoundTag();line.putString("optionId",ids[i]);line.putString("lineTier","LEGENDARY");line.putDouble("value",values[i]);lines.add(line);}potential.put("lines",lines);p.getInventory().selected=0;p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,rod);p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.containerMenu.broadcastChanges();});}
            case 8->{for(String id:List.of("minecraft:fishing_rod","aquaculture:iron_fishing_rod","aquaculture:gold_fishing_rod","aquaculture:diamond_fishing_rod","aquaculture:neptunium_fishing_rod","netherdepthsupgrade:lava_fishing_rod"))require(AutoFishingClient.rod(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(id)))),"Pack rod not supported "+id);aquaStartWins=wins.get();aquaStartCasts=casts.get();key();require(AutoFishingClient.enabled(),"Modded rod did not enable");}
            case 10->{server(mc,p->{require(p.getMainHandItem().getTag().getString("AutoFishingForeignProbe").equals("keep")&&p.getMainHandItem().getTag().getCompound("sunlit_reforged").getList("lines",10).size()==3,"Modded rod foreign NBT/potentials changed");require(p.getMainHandItem().getDamageValue()>0,"Modded rod durability bypassed");System.out.println("AUTO_FISHING_MODDED_ROD_PASSED wins="+(wins.get()-aquaStartWins)+" casts="+(casts.get()-aquaStartCasts));if(p.fishing!=null)p.fishing.discard();for(int i=1;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.containerMenu.broadcastChanges();});}
            case 11->{fullInventoryCasts=casts.get();fullInventoryWins=wins.get();fullInventoryStartedAt=System.currentTimeMillis();key();require(AutoFishingClient.enabled(),"Full-inventory check setup failed");}
            case 13->{System.out.println("AUTO_FISHING_CLIENT_PASSED: vanilla and neptunium casts, real bites, input-driven minigames, treasure, recast, fish sizes, durability, unchanged potential/foreign NBT, F8 toggle, rod/slot, inventory-screen stop and continued casting with full inventory.");mc.stop();}
        }
    }
}
