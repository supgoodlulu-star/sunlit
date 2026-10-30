package dev.sunlit.journal;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;
import java.util.concurrent.*;

/** Disposable real-client QA; example scores are not production records. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public final class HallClientCheck {
    static volatile boolean worldReady;
    static int phase,ticks;static CompletableFuture<Void> pending;static long start=System.currentTimeMillis();
    static void require(boolean b,String m){if(!b)throw new IllegalStateException("HALL_CLIENT: "+m);}
    interface Work{void run(net.minecraft.server.level.ServerPlayer p)throws Exception;}
    static void server(Minecraft mc,Work action){pending=new CompletableFuture<>();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{action.run(mc.getSingleplayerServer().getPlayerList().getPlayer(id));pending.complete(null);}catch(Throwable t){pending.completeExceptionally(t);}});}
    static void shot(Minecraft mc,String name){mc.getToasts().clear();Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput")),name+".png",mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.hallCheck"))return;var mc=Minecraft.getInstance();require(System.currentTimeMillis()-start<240000,"timeout phase "+phase);
        if(mc.screen!=null)for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals("진행")){b.onPress();return;}
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
        if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
        if(++ticks<40)return;ticks=0;
        switch(phase++){
            case 0->{mc.options.guiScale().set(2);mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");mc.reloadResourcePacks();mc.resizeDisplay();mc.setScreen(null);server(mc,p->{
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getInventory().clearContent();p.teleportTo(0.5,81,-8.5);p.setYRot(0);p.setXRot(0);var level=p.serverLevel();level.getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                for(int x=-12;x<=12;x++)for(int z=-22;z<=8;z++){level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.GRASS_BLOCK.defaultBlockState());for(int y=81;y<=90;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                var w=HallWorld.get(p.getServer());var n=w.save(new CompoundTag());var people=HallWorld.child(n,"people");String[] names={"룰루","예서","햇살","단풍","초롱","모카","새봄","은별","나루","소담"};for(int i=0;i<10;i++){UUID id=i==0?p.getUUID():new UUID(0,100+i);var who=HallWorld.child(people,id.toString());who.putString("name",i==0?p.getGameProfile().getName():"Example"+i);who.putString("nick",names[i]);who.putInt("achievements",20-i);}
                n.put("scores",new CompoundTag());var seeded=new HallWorld(n);for(int i=0;i<10;i++)seeded.add(i==0?p.getUUID():new UUID(0,100+i),"tasks",50-i*3);level.getDataStorage().set("sunlit_hall_of_fame",seeded);
                for(int x=-2;x<=2;x++)for(int z=-10;z<=-6;z++)for(int y=81;y<=82;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE_BRICKS.defaultBlockState());p.teleportTo(.5,83,-8.5);p.setYRot(180);
                int result=p.getServer().getCommands().getDispatcher().execute("명예의 전당 업무",p.createCommandSourceStack().withPermission(2));require(result==1,"placement command failed");p.teleportTo(.5,83,-14.5);p.setXRot(0);
            });}
            case 1->{mc.player.setYRot(0);mc.player.setXRot(0);mc.gui.getChat().clearMessages(true);mc.setScreen(null);mc.options.hideGui=false;}
            case 2->{shot(mc,"winner-only-example-records");mc.options.hideGui=false;mc.player.connection.sendCommand("명예의전당");}
            case 3->{require(mc.screen instanceof HallScreen,"public command did not open screen");shot(mc,"monthly-leaderboard-example-records");server(mc,p->{var w=HallWorld.get(p.getServer());w.add(new UUID(0,109),"tasks",100);});}
            case 4->{require(mc.screen instanceof HallScreen,"live screen lost");if(!((HallScreen)mc.screen).snapshot().getList("rows",10).getCompound(0).getUUID("id").equals(new UUID(0,109))){phase=4;return;}server(mc,p->{var list=p.serverLevel().getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(32));require(list.size()==1,"incorrect figure count");worldReady=list.stream().anyMatch(f->f.getCustomName()!=null&&f.getCustomName().getString().startsWith("1위 · 소담"));});}
            case 5->{if(!worldReady){phase=4;return;}shot(mc,"live-rank-change-example-records");mc.setScreen(null);server(mc,p->{var list=p.serverLevel().getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(32));list.get(0).mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);});}
            case 6->{require(mc.screen instanceof HallScreen,"figure interaction did not open screen");mc.options.guiScale().set(4);mc.resizeDisplay();}
            case 7->{shot(mc,"small-gui-example-records");mc.screen.onClose();}
            case 8->{require(!(mc.screen instanceof HallScreen),"close failed");mc.options.guiScale().set(2);mc.resizeDisplay();server(mc,p->JournalNetwork.send(p,true,"profile"));}
            case 9->{require(mc.screen instanceof JournalScreen,"profile failed");shot(mc,"profile-hall-entry");for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals("명예의 전당")){b.onPress();return;}throw new IllegalStateException("Profile hall button missing");}
            case 10->{require(mc.screen instanceof HallScreen,"profile button failed");System.out.println("HALL_CLIENT_PASSED: public command, single winner at exact command position, live records, figure interaction, small GUI, close and journal profile. Example QA scores only.");mc.stop();}
        }
    }
}
