package dev.sunlit.journal;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.time.*;
import java.util.*;

/** Separate additive save: never resets journal/player progress. All access on server thread. */
public final class HallWorld extends SavedData {
    static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    private final CompoundTag root;
    private final java.util.function.Supplier<Instant> now;
    public HallWorld(){this(new CompoundTag());}
    public HallWorld(CompoundTag n){this(n,Instant::now);}
    HallWorld(CompoundTag n,java.util.function.Supplier<Instant> now){root=n.copy();this.now=now;}
    public static HallWorld get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(HallWorld::new,HallWorld::new,"sunlit_hall_of_fame");}
    @Override public CompoundTag save(CompoundTag n){n.merge(root);return n;}
    static CompoundTag child(CompoundTag n,String k){if(!n.contains(k,10))n.put(k,new CompoundTag());return n.getCompound(k);}
    static String month(Instant now){return YearMonth.from(now.atZone(ZONE)).toString();}
    public String season(){return rotate(now.get());}
    String rotate(Instant now){
        String next=month(now),old=root.getString("season");
        // Clock rollback must not reopen or overwrite an archived season.
        if(old.isEmpty()||next.compareTo(old)>0){
            if(!old.isEmpty()){child(root,"archive").put(old,root.getCompound("scores").copy());child(root,"archivePeople").put(old,root.getCompound("people").copy());}
            root.putString("season",next);root.put("scores",new CompoundTag());setDirty();
        }
        return root.getString("season");
    }
    void add(UUID id,String category,long amount){
        season();if(amount<=0)return;
        CompoundTag row=child(child(root,"scores"),id.toString());long old=row.getLong(category);
        row.putLong(category,old>Long.MAX_VALUE-amount?Long.MAX_VALUE:old+amount);setDirty();
    }
    public void task(ServerPlayer p){if(!p.isCreative()&&!p.isSpectator())add(p.getUUID(),"tasks",1);}
    public void festival(ServerPlayer p,long amount){add(p.getUUID(),"festival",amount);}
    void sample(UUID id,String category,long total,boolean eligible){
        String current=season();CompoundTag last=child(child(root,"samples"),id.toString());String k="skill:"+category;
        // A per-category season avoids charging gains from offline months to a new season.
        if(last.contains(k,Tag.TAG_LONG)&&last.getString(k+":season").equals(current)&&eligible)
            add(id,k,Math.max(0,total-last.getLong(k)));
        last.putLong(k,total);last.putString(k+":season",current);setDirty();
    }
    void balance(UUID id,Optional<Integrations.Balance> value){
        season();if(value.isEmpty())return;var balance=value.get();if(balance.spurs()<0||balance.perSun()<=0)return;
        var person=child(child(root,"people"),id.toString());person.putLong("assets",balance.spurs());person.putLong("assetUnit",balance.perSun());person.putLong("assetChecked",now.get().getEpochSecond());setDirty();
    }
    public void refreshOfflineAssets(Set<UUID> online){
        for(String key:root.getCompound("people").getAllKeys())try{UUID id=UUID.fromString(key);if(!online.contains(id))balance(id,Integrations.balance(id));}catch(IllegalArgumentException ignored){}
    }
    public static String score(String category,long value,long unit){
        if(category.equals("assets")&&unit>0)return java.math.BigDecimal.valueOf(value).divide(java.math.BigDecimal.valueOf(unit),12,java.math.RoundingMode.DOWN).stripTrailingZeros().toPlainString()+" Sun";
        return String.format(java.util.Locale.ROOT,"%,d",value);
    }
    public void observe(ServerPlayer p){
        season();CompoundTag person=child(child(root,"people"),p.getUUID().toString());
        person.putString("name",p.getGameProfile().getName());
        var textures=p.getGameProfile().getProperties().get("textures");if(!textures.isEmpty()){var texture=textures.iterator().next();person.putString("texture",texture.getValue());if(texture.hasSignature())person.putString("signature",texture.getSignature());}
        String nick=p.getGameProfile().getName();try{String n=Integrations.nick(p);if(!n.isBlank())nick=net.minecraft.ChatFormatting.stripFormatting(n);}catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){}
        person.putString("nick",nick);person.putInt("achievements",SunlitJournal.owned(p).size());
        balance(p.getUUID(),Integrations.balance(p));
        var skills=Integrations.skillTotals(p);
        if(skills.getBoolean("available"))for(Tag tag:skills.getList("categories",10)){
            CompoundTag c=(CompoundTag)tag;if(c.getBoolean("hasTotal")){
                String id=c.getString("id");child(root,"categories").putString("skill:"+id,HallNames.skill(id));
                sample(p.getUUID(),id,c.getLong("total"),!p.isCreative()&&!p.isSpectator());
            }
        }
        setDirty();
    }
    CompoundTag person(UUID id){return root.getCompound("people").getCompound(id.toString()).copy();}
    public record Row(UUID id,String name,String nick,long score,int rank,long unit){}
    public List<Row> rows(String season,String category){
        String current=season();CompoundTag people=category.equals("achievements")||season.equals(current)?root.getCompound("people"):root.getCompound("archivePeople").getCompound(season);
        CompoundTag scores=(category.equals("achievements")||category.equals("assets"))?people:season.equals(current)?root.getCompound("scores"):root.getCompound("archive").getCompound(season);
        var entries=new ArrayList<Row>();
        for(String id:scores.getAllKeys())try{
            long score=scores.getCompound(id).getLong(category);if(category.equals("achievements"))score=scores.getCompound(id).getInt(category);
            if(score<=0)continue;CompoundTag who=people.getCompound(id);
            if(category.equals("assets")&&who.getLong("assetUnit")<=0)continue;
            entries.add(new Row(UUID.fromString(id),who.getString("name"),who.getString("nick"),score,0,who.getLong("assetUnit")));
        }catch(IllegalArgumentException ignored){}
        entries.sort(Comparator.comparingLong(Row::score).reversed().thenComparing(r->r.id().toString()));
        var out=new ArrayList<Row>();long previous=-1;int rank=0;
        for(int i=0;i<entries.size();i++){Row r=entries.get(i);if(r.score()!=previous)rank=i+1;previous=r.score();out.add(new Row(r.id(),r.name(),r.nick(),r.score(),rank,r.unit()));}
        return out;
    }
    public List<String> seasons(){season();var out=new ArrayList<>(root.getCompound("archive").getAllKeys());out.sort(Comparator.reverseOrder());out.add(0,root.getString("season"));return out;}
    void declareSkill(String id){child(root,"categories").putString("skill:"+id,HallNames.skill(id));setDirty();}
    public Map<String,String> categories(){var out=new LinkedHashMap<String,String>();out.put("tasks","업무 완료");out.put("festival","축제 기여");out.put("achievements","업적 · 누적");out.put("assets","자산 · 개인 계좌");var c=root.getCompound("categories");c.getAllKeys().stream().sorted().forEach(k->out.put(k,c.getString(k)+" 경험치"));return out;}
    public CompoundTag snapshot(String requested,String category,int page){
        var seasons=seasons();String selected=seasons.contains(requested)?requested:season();if(!categories().containsKey(category))category="tasks";
        var rows=rows(selected,category);int pages=Math.max(1,(rows.size()+7)/8);page=Math.max(0,Math.min(page,pages-1));
        CompoundTag out=new CompoundTag();out.putString("season",selected);out.putString("current",season());out.putString("category",category);out.putInt("page",page);out.putInt("pages",pages);out.putInt("total",rows.size());
        ListTag months=new ListTag();for(String s:seasons)months.add(StringTag.valueOf(s));out.put("seasons",months);
        CompoundTag cats=new CompoundTag();categories().forEach(cats::putString);out.put("categories",cats);
        ListTag list=new ListTag();for(Row r:rows.subList(page*8,Math.min(rows.size(),page*8+8))){CompoundTag n=new CompoundTag();n.putUUID("id",r.id());n.putString("name",r.name());n.putString("nick",r.nick());n.putLong("score",r.score());n.putString("displayScore",score(category,r.score(),r.unit()));n.putInt("rank",r.rank());list.add(n);}out.put("rows",list);if(category.equals("assets"))out.putString("note",rows.isEmpty()?"개인 계좌 잔액 기록이 없습니다. 계좌 연동을 확인해 주세요.":"마지막 확인한 개인 계좌 잔액 · 현금·공동 계좌 제외");return out;
    }
}
