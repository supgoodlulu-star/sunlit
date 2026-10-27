package dev.sunlit.journal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.nbt.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.CompletableFuture;
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,value=Dist.CLIENT)
public class JournalVisualPreview {
    private static int stage=-1,ticks;
    private static CompletableFuture<Void> reloading;
    private static final String[] PAGES={"skills","quests","achievements","achievements","profile","quests","achievements","skills","achievements","quests","quests","quests","skills","quests","quests"};
    private static CompoundTag data(){
        var d=new CompoundTag();d.putString("username","Sunlit");d.putString("nickname","햇살");d.putBoolean("nickAvailable",true);
        var titles=new CompoundTag();titles.putString("owned","새싹농부|꼬마낚시꾼|벚꽃요정");titles.putString("equipped","새싹농부");d.put("titles",titles);
        var sd=new CompoundTag();sd.putBoolean("available",true);var list=new ListTag();
        for(String name:new String[]{"exploration","life","books","farming","fishing","mining","cooking","husbandry"}){var c=new CompoundTag();c.putString("id","society:"+name);c.putBoolean("unlocked",true);c.putBoolean("hasXp",!java.util.Set.of("exploration","life","books").contains(name));c.putInt("level",7);c.putInt("xp",42);c.putInt("required",100);var names=new ListTag();for(int j=0;j<(java.util.Set.of("exploration","life","books").contains(name)?0:4);j++)names.add(StringTag.valueOf("fixture_"+j));c.put("skills",names);list.add(c);}sd.put("categories",list);d.put("skillsData",sd);
        for(String period:new String[]{"daily","weekly"}){
            var tasks=new CompoundTag();tasks.putString("date","2026-10-27");boolean daily=period.equals("daily");
            int[] kinds=daily?new int[]{0,1,2,3,4}:new int[]{2,3,4};String[] ids={"minecraft:wheat","minecraft:cod","minecraft:iron_ore","minecraft:bread","minecraft:cow"};
            for(int i=0;i<kinds.length;i++){int kind=kinds[i];var t=new CompoundTag();t.putInt("kind",kind);t.putString("subject",ids[kind]);t.putInt("target",6);t.putInt("progress",i==1?6:2);t.put("payout",TaskRewards.plan(kind,!daily,0));tasks.put(""+i,t);}d.put(period,tasks);
        }
        var f=new CompoundTag();f.putBoolean("available",true);f.putString("season","SPRING");f.putLong("progress",250);f.putLong("goal",1000);d.put("festival",f);
        var counts=new CompoundTag();counts.putLong("farming",46);counts.putLong("mining",123);d.put("counts",counts);return d;
    }
    private static void press(JournalScreen screen,String label){
        for(var child:screen.children())if(child instanceof JournalButton b&&b.getMessage().getString().equals(label)){b.onPress();return;}
        throw new IllegalStateException("Missing preview button: "+label);
    }
    private static CompoundTag stageData(){
        var d=data();
        if(stage==11)d.remove("festival");
        if(stage==13){var t=d.getCompound("daily").getCompound("0");t.putInt("kind",0);t.putString("subject","minecraft:carrots");}
        return d;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("sunlit.visualPreview"))return;
        var mc=Minecraft.getInstance();
        if(stage==-1){if(!(mc.screen instanceof TitleScreen))return;mc.options.languageCode="ko_kr";mc.getLanguageManager().setSelected("ko_kr");reloading=mc.reloadResourcePacks();stage=-2;return;}
        if(stage==-2){if(!reloading.isDone())return;
            if(java.util.Set.of(SkillCategoryIcons.select("society:exploration","탐험"),SkillCategoryIcons.select("society:life","생활 기술"),SkillCategoryIcons.select("society:books","서적")).size()!=3)throw new IllegalStateException("Category icons collide");stage=0;ticks=0;mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new JournalScreen(data(),PAGES[stage]));return;}
        if(stage>=PAGES.length)return;
        if(mc.getOverlay()!=null){ticks=0;return;}
        if(++ticks<40)return;
        Screenshot.grab(new java.io.File(System.getProperty("sunlit.previewOutput","../validation")),"cards-"+stage+"-"+PAGES[stage]+".png",mc.getMainRenderTarget(),c->{});
        if(++stage>=PAGES.length){mc.stop();return;}ticks=0;
        if(stage==7||stage==9){mc.options.guiScale().set(2);mc.resizeDisplay();}
        if(stage==14){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,720);mc.options.guiScale().set(3);mc.resizeDisplay();}
        if(stage==5){mc.options.guiScale().set(3);mc.resizeDisplay();}
        var screen=new JournalScreen(stageData(),PAGES[stage]);mc.setScreen(screen);
        if(stage==9){press(screen,"주간 업무");screen.update(stageData());}
        if(stage==10||stage==11)press(screen,"계절 특별 업무");
        if(stage==12)screen.mouseScrolled(screen.width/2,screen.height/2,-10);
        if(stage==7){for(var child:screen.children())if(child instanceof JournalButton b&&b.getMessage().getString().equals("기술 펼치기")){b.onPress();break;}}
        if(stage==3||stage==6||stage==8){int pw=Math.min(480,screen.width-12),ph=Math.min(400,screen.height-12),left=(screen.width-pw)/2,top=(screen.height-ph)/2,w=(pw-28)/8;int filter=stage==3?7:stage==8?6:3;screen.mouseClicked(left+14+filter*w+5,top+68,0);}
    }
}
