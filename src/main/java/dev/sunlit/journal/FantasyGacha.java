package dev.sunlit.journal;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Shared, versioned catalogue; all economic mutations run on the authoritative server thread. */
public final class FantasyGacha {
    public record Skin(String id,String name,String theme,String themeName,String collection,String kind,String rarity,int weight,int index,String description) {
        public String rarityName(){return switch(rarity){case "legendary"->"전설";case "epic"->"영웅";case "rare"->"희귀";default->"일반";};}
        public int color(){return switch(rarity){case "legendary"->0xFFFFCD81;case "epic"->0xFFD2A4F5;case "rare"->0xFF8EC7F5;default->0xFFD9C9BB;};}
    }
    public static final List<Skin> POOL;
    public static final int TOTAL_WEIGHT;
    private static final Map<String,Skin> BY_ID=new HashMap<>();
    static {
        try(var stream=Objects.requireNonNull(FantasyGacha.class.getResourceAsStream("/data/sunlit_journal_core/fantasy_skins.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            POOL=List.of(new Gson().fromJson(reader,Skin[].class));
            for(var s:POOL){if(s.weight()<=0||s.index()<=0||BY_ID.put(s.id(),s)!=null)throw new IllegalStateException("Invalid catalogue");}
            TOTAL_WEIGHT=POOL.stream().mapToInt(Skin::weight).sum();
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static Skin find(String id){return BY_ID.get(id);}
    static CompoundTag state(ServerPlayer p){
        var d=JournalWorld.get(p.getServer()).player(p);
        if(!d.contains("fantasyGacha",Tag.TAG_COMPOUND))d.put("fantasyGacha",new CompoundTag());
        return d.getCompound("fantasyGacha");
    }
    public static boolean owns(ServerPlayer p,String id){return state(p).getCompound("owned").getBoolean(id);}
    private static int count(ServerPlayer p,net.minecraft.world.item.Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))n+=s.getCount();}return n;}
    public static int tickets(ServerPlayer p){return count(p,SkyContent.TICKET.get());}
    public static int fragments(ServerPlayer p){return count(p,SkyContent.FRAGMENT.get());}
    public static CompoundTag snapshot(ServerPlayer p){var out=state(p).copy();out.putInt("tickets",tickets(p));out.putInt("fragments",fragments(p));return out;}
    public static boolean craftTicket(ServerPlayer p){
        if(fragments(p)<10)return false;
        int remaining=10;
        for(int i=0;i<p.getInventory().getContainerSize()&&remaining>0;i++){var stack=p.getInventory().getItem(i);if(stack.is(SkyContent.FRAGMENT.get())){int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;}}
        deliver(p,new net.minecraft.world.item.ItemStack(SkyContent.TICKET.get()));
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();return true;
    }
    public static void deliver(ServerPlayer p,net.minecraft.world.item.ItemStack stack){
        if(!p.getInventory().add(stack)){var drop=p.drop(stack,false);if(drop!=null)drop.setTarget(p.getUUID());}
    }
    public static Skin sample(int roll){
        if(roll<0||roll>=TOTAL_WEIGHT)throw new IllegalArgumentException("roll");
        for(var s:POOL){roll-=s.weight();if(roll<0)return s;}throw new IllegalStateException();
    }
    public static boolean draw(ServerPlayer p,int count){
        if(count!=1&&count!=10)return false;
        if(tickets(p)<count){p.sendSystemMessage(Component.literal("[별빛 공방] 뽑기권이 부족해요."));return false;}
        ListTag results=new ListTag();var state=state(p);var owned=state.getCompound("owned").copy();int dust=state.getInt("dust");
        for(int i=0;i<count;i++){
            var s=sample(p.getRandom().nextInt(TOTAL_WEIGHT));boolean duplicate=owned.getBoolean(s.id());
            CompoundTag result=new CompoundTag();result.putString("id",s.id());result.putBoolean("duplicate",duplicate);results.add(result);
            if(duplicate)dust++;owned.putBoolean(s.id(),true);
        }
        int remaining=count;
        for(int i=0;i<p.getInventory().getContainerSize()&&remaining>0;i++){var s=p.getInventory().getItem(i);if(s.is(SkyContent.TICKET.get())){int n=Math.min(remaining,s.getCount());s.shrink(n);remaining-=n;}}
        state.put("owned",owned);state.putInt("dust",dust);state.put("results",results);state.putLong("sequence",state.getLong("sequence")+1);
        JournalWorld.get(p.getServer()).setDirty();p.getInventory().setChanged();p.containerMenu.broadcastChanges();return true;
    }
    public static boolean exchange(ServerPlayer p,String id){
        if(find(id)==null)return false;var d=state(p);
        if(d.getInt("dust")<5||owns(p,id))return false;
        var owned=d.getCompound("owned");owned.putBoolean(id,true);d.put("owned",owned);d.putInt("dust",d.getInt("dust")-5);
        JournalWorld.get(p.getServer()).setDirty();return true;
    }
}
