package dev.sunlit.journal;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Authoritative world data: all mutations happen on the Minecraft server thread. */
public final class JournalWorld extends SavedData {
    public static final String[] ACTIVITIES={"farming","fishing","mining","cooking","ranching"};
    public static final String[] NAMES={"농사","낚시","채광","요리","목축"};
    public static final long[] MILESTONES={10,100,1000,10000};
    public static final long[] SUN_TARGETS={1000,100000,10000000,100000000};
    private final CompoundTag root;
    public JournalWorld(){root=new CompoundTag();}
    public JournalWorld(CompoundTag tag){root=tag.copy();}
    public static JournalWorld get(MinecraftServer s){
        return s.overworld().getDataStorage().computeIfAbsent(JournalWorld::new,JournalWorld::new,"sunlit_journal_alpha4");
    }
    @Override public CompoundTag save(CompoundTag tag){tag.merge(root);return tag;}
    private CompoundTag child(CompoundTag parent,String key){
        if(!parent.contains(key,Tag.TAG_COMPOUND))parent.put(key,new CompoundTag());
        return parent.getCompound(key);
    }
    public CompoundTag player(UUID uuid){return child(child(root,"players"),uuid.toString());}
    public CompoundTag player(ServerPlayer p){return player(p.getUUID());}
    public void refreshTasks(ServerPlayer p){
        CompoundTag d=player(p);
        LocalDate day=LocalDate.now(Clock.systemUTC());
        String[] periods={day.toString(),day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()};
        for(int period=0;period<2;period++){
            String key=period==0?"daily":"weekly";
            CompoundTag tasks=child(d,key);
            // Clock rollback must not reopen an earlier reward period.
            if(tasks.getString("date").isEmpty() || tasks.getString("date").compareTo(periods[period])<0){
                CompoundTag fresh=new CompoundTag();fresh.putString("date",periods[period]);
                List<Integer> pool=new ArrayList<>(List.of(0,1,2,3,4));Collections.shuffle(pool);
                for(int i=0;i<3;i++){
                    CompoundTag task=new CompoundTag();task.putInt("kind",pool.get(i));
                    TaskTargets.assign(task,period,true);
                    task.putInt("reward",period==0?2:8);fresh.put(Integer.toString(i),task);
                }
                d.put(key,fresh);setDirty();
            }
            // Migrate only untouched legacy tasks; preserve earned progress and claimed rewards.
            for(int i=0;i<3;i++){
                CompoundTag task=d.getCompound(key).getCompound(""+i);
                if((!task.contains("subject")||TaskTargets.excluded(task.getString("subject")))&&task.getInt("progress")==0&&!task.getBoolean("claimed")){
                    TaskTargets.assign(task,period,false);setDirty();
                }
            }
        }
    }
    public boolean claim(ServerPlayer p,String key,int index){
        if((!key.equals("daily")&&!key.equals("weekly"))||index<0||index>2)return false;
        refreshTasks(p);CompoundTag task=player(p).getCompound(key).getCompound(""+index);
        if(task.getInt("target")<=0||task.getInt("progress")<task.getInt("target")||task.getBoolean("claimed"))return false;
        task.putBoolean("claimed",true);setDirty();
        reward(p,task.getInt("reward"));return true;
    }
    public static void reward(ServerPlayer p,int amount){
        // Inventory overflow is delivered as an owned item by vanilla's drop path.
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,amount);
        if(!p.getInventory().add(stack))p.drop(stack,false);
        p.giveExperiencePoints(amount*10);
    }
    public void activity(ServerPlayer p,int kind,int amount){activity(p,kind,amount,"");}
    public void activity(ServerPlayer p,int kind,int amount,String subject){
        if(kind<0||kind>=5||amount<=0||p.isCreative()||p.isSpectator())return;
        refreshTasks(p);CompoundTag d=player(p),counts=child(d,"counts");
        long old=counts.getLong(ACTIVITIES[kind]);
        counts.putLong(ACTIVITIES[kind],old>Long.MAX_VALUE-amount?Long.MAX_VALUE:old+amount);
        for(String period:List.of("daily","weekly"))for(int i=0;i<3;i++){
            CompoundTag t=d.getCompound(period).getCompound(""+i);
            if(TaskTargets.matches(t,kind,subject))t.putInt("progress",(int)Math.min((long)t.getInt("target"),(long)t.getInt("progress")+amount));
        }
        updateFestival(p);
        contribute(p,amount);
        setDirty();achievements(p);
    }
    void contribute(ServerPlayer p,int amount){
        if(root.contains("festival",Tag.TAG_COMPOUND)){
            CompoundTag f=root.getCompound("festival");
            if(f.getBoolean("available")&&!f.getBoolean("complete")){
                CompoundTag people=child(f,"contributions");String id=p.getUUID().toString();
                people.putLong(id,people.getLong(id)+amount);
                f.putLong("progress",Math.min(f.getLong("goal"),f.getLong("progress")+amount));
                if(f.getLong("progress")>=f.getLong("goal")){
                    f.putBoolean("complete",true);
                    for(String who:people.getAllKeys()){
                        if(people.getLong(who)<=0)continue;
                        CompoundTag pd=player(UUID.fromString(who));
                        child(pd,"seasons").putBoolean(f.getString("season"),true);
                        pd.putInt("festivalRewards",pd.getInt("festivalRewards")+1);
                    }
                }
            }
        }
        setDirty();
    }
    public void updateFestival(ServerPlayer p){
        Optional<Integrations.Season> state=Integrations.season(p);
        if(state.isEmpty()){
            if(root.contains("festival",Tag.TAG_COMPOUND))root.getCompound("festival").putBoolean("available",false);
            return;
        }
        setSeason(state.get());
    }
    void setSeason(Integrations.Season s){
        CompoundTag f=child(root,"festival");
        if(!f.getString("season").equals(s.name())||s.cycleTicks()<f.getInt("lastCycleTick")){
            CompoundTag fresh=new CompoundTag();fresh.putString("season",s.name());
            fresh.putLong("sequence",f.getLong("sequence")+1);fresh.putLong("goal",JournalConfig.FESTIVAL_GOAL.get());
            root.put("festival",fresh);f=fresh;setDirty();
        }
        if(!f.getBoolean("available"))setDirty();
        f.putBoolean("available",true);f.putInt("lastCycleTick",s.cycleTicks());setDirty();
    }
    public void achievements(ServerPlayer p){
        CompoundTag d=player(p),counts=child(d,"counts");
        for(int k=0;k<5;k++)for(int tier=0;tier<4;tier++)
            if(counts.getLong(ACTIVITIES[k])>=MILESTONES[tier])grant(p,SunlitJournal.TITLES[k*4+tier]);
        var balance=Integrations.balance(p);
        balance.ifPresent(b->{for(int i=0;i<4;i++)if(b.spurs()/b.perSun()>=SUN_TARGETS[i])grant(p,SunlitJournal.TITLES[20+i]);});
        CompoundTag seasons=child(d,"seasons");int n=0;
        for(String season:List.of("SPRING","SUMMER","AUTUMN","WINTER"))if(seasons.getBoolean(season)){
            grant(p,seasonTitle(season));n++;
        }
        if(n==4)grant(p,"사계요정");
        int rewards=d.getInt("festivalRewards");
        if(rewards>0){d.putInt("festivalRewards",0);setDirty();for(int i=0;i<rewards;i++)reward(p,16);}
    }
    public static String seasonTitle(String s){return switch(s){case "SPRING"->"벚꽃요정";case "SUMMER"->"별빛요정";case "AUTUMN"->"풍요요정";case "WINTER"->"눈꽃요정";default->"";};}
    public static String festivalName(String s){return switch(s){case "SPRING"->"벚꽃 축제";case "SUMMER"->"별빛 해변 축제";case "AUTUMN"->"풍요의 수확제";case "WINTER"->"눈꽃 축제";default->"계절 축제";};}
    public static void grant(ServerPlayer p,String title){
        Set<String> owned=SunlitJournal.owned(p);
        if(owned.add(title)){
            SunlitJournal.saveOwned(p,owned);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[모험가의 수첩] 칭호 획득: ").append(TitleThemes.badge(title)));
        }
    }
    public CompoundTag snapshot(ServerPlayer p){
        refreshTasks(p);updateFestival(p);achievements(p);
        CompoundTag out=player(p).copy();out.putString("username",p.getGameProfile().getName());
        out.put("titles",SunlitJournal.data(p).copy());
        try{out.putString("nickname",Integrations.nick(p));out.putBoolean("nickAvailable",true);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError e){Integrations.failure("nickname",e);}
        var balance=Integrations.balance(p);
        balance.ifPresent(b->{out.putLong("spurs",b.spurs());out.putLong("perSun",b.perSun());out.putBoolean("balanceAvailable",true);});
        out.put("festival",root.getCompound("festival").copy());out.put("skillsData",Integrations.skills(p));
        return out;
    }
    /** Placed ore exclusion persists over restarts; dimensions prevent coordinate collisions. */
    public void placedOre(ServerPlayer p,net.minecraft.core.BlockPos pos){
        child(root,"placedOre").putBoolean(p.level().dimension().location()+":"+pos.asLong(),true);setDirty();
    }
    public boolean consumePlacedOre(ServerPlayer p,net.minecraft.core.BlockPos pos){
        CompoundTag positions=child(root,"placedOre");String key=p.level().dimension().location()+":"+pos.asLong();
        if(!positions.contains(key))return false;positions.remove(key);setDirty();return true;
    }
}
